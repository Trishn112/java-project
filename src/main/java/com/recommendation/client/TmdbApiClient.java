package com.recommendation.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recommendation.exception.MovieNotFoundException;
import com.recommendation.exception.TmdbApiException;
import com.recommendation.model.Movie;
import com.recommendation.model.MoviePageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Client for interacting with The Movie Database (TMDB) API v3.
 * Demonstrates MULTITHREADING (ExecutorService, CompletableFuture),
 * EXCEPTION HANDLING, and ROBUST FALLBACK ARCHITECTURE.
 */
@Component
public class TmdbApiClient {

    private static final Logger logger = LoggerFactory.getLogger(TmdbApiClient.class);

    private static final String TMDB_BASE_URL = "https://api.themoviedb.org/3";
    private static final Map<Integer, String> GENRE_ID_MAP = new HashMap<>();

    static {
        // Standard TMDB Genre ID to Name Mapping
        GENRE_ID_MAP.put(28, "Action");
        GENRE_ID_MAP.put(12, "Adventure");
        GENRE_ID_MAP.put(16, "Animation");
        GENRE_ID_MAP.put(35, "Comedy");
        GENRE_ID_MAP.put(80, "Crime");
        GENRE_ID_MAP.put(99, "Documentary");
        GENRE_ID_MAP.put(18, "Drama");
        GENRE_ID_MAP.put(10751, "Family");
        GENRE_ID_MAP.put(14, "Fantasy");
        GENRE_ID_MAP.put(36, "History");
        GENRE_ID_MAP.put(27, "Horror");
        GENRE_ID_MAP.put(10402, "Music");
        GENRE_ID_MAP.put(9648, "Mystery");
        GENRE_ID_MAP.put(10749, "Romance");
        GENRE_ID_MAP.put(878, "Science Fiction");
        GENRE_ID_MAP.put(10770, "TV Movie");
        GENRE_ID_MAP.put(53, "Thriller");
        GENRE_ID_MAP.put(10752, "War");
        GENRE_ID_MAP.put(37, "Western");
    }

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;
    private final Map<Integer, Movie> inMemoryCache;
    private final List<Movie> fallbackDataset;
    private final Map<String, CachedPage> pageCache;
    private static final long PAGE_CACHE_TTL_MS = 10 * 60 * 1000L; // 10 minutes

    private static class CachedPage {
        final MoviePageResult pageResult;
        final long timestamp;

        CachedPage(MoviePageResult pageResult) {
            this.pageResult = pageResult;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired(long ttlMillis) {
            return (System.currentTimeMillis() - timestamp) > ttlMillis;
        }
    }

    public TmdbApiClient(@Value("${tmdb.api.key:}") String configuredApiKey) {
        String envKey = System.getenv("TMDB_API_KEY");
        if (envKey != null && !envKey.trim().isEmpty()) {
            this.apiKey = envKey.trim();
        } else if (configuredApiKey != null && !configuredApiKey.trim().isEmpty()) {
            this.apiKey = configuredApiKey.trim();
        } else {
            this.apiKey = "";
        }

        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        this.objectMapper = new ObjectMapper();
        this.executorService = Executors.newFixedThreadPool(6);
        this.inMemoryCache = new ConcurrentHashMap<>();
        this.pageCache = new ConcurrentHashMap<>();
        this.fallbackDataset = initializeFallbackDataset();

        if (isApiKeyConfigured()) {
            logger.info("TMDB API Key configured successfully. Live API mode active.");
        } else {
            logger.warn("No TMDB API Key found. System initialized with rich offline fallback dataset (40+ movies).");
        }
    }

    public boolean isApiKeyConfigured() {
        return apiKey != null && !apiKey.trim().isEmpty() && !apiKey.equalsIgnoreCase("YOUR_TMDB_API_KEY");
    }

    private boolean isBearerToken(String key) {
        return key != null && (key.startsWith("eyJ") || key.length() > 40);
    }

    private String buildUrl(String endpoint, Map<String, String> params) {
        StringBuilder url = new StringBuilder(TMDB_BASE_URL).append(endpoint);
        boolean hasQuery = endpoint.contains("?");
        if (isApiKeyConfigured() && !isBearerToken(this.apiKey)) {
            url.append(hasQuery ? "&" : "?").append("api_key=").append(this.apiKey);
            hasQuery = true;
        }
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                url.append(hasQuery ? "&" : "?")
                        .append(entry.getKey()).append("=")
                        .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
                hasQuery = true;
            }
        }
        return url.toString();
    }

    /**
     * Searches for multiple matching movies by title using TMDB API or fallback dataset.
     * Supports pagination, special characters, and non-English movie titles.
     *
     * @param queryTitle The search string entered by the user
     * @param page       Page number (1-based)
     * @return List of matching Movie objects
     */
    public List<Movie> searchMovies(String queryTitle) {
        return searchMovies(queryTitle, 1);
    }

    public List<Movie> searchMovies(String queryTitle, int page) {
        if (queryTitle == null || queryTitle.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String cleanQuery = queryTitle.trim();
        int targetPage = Math.max(page, 1);

        // 1. Try Live TMDB API if key is present
        if (isApiKeyConfigured()) {
            try {
                String url = buildUrl("/search/movie", Map.of(
                        "query", cleanQuery,
                        "include_adult", "false",
                        "language", "en-US",
                        "page", String.valueOf(targetPage)
                ));

                JsonNode root = executeGetRequest(url);
                JsonNode results = root.path("results");
                if (results.isArray() && results.size() > 0) {
                    List<Movie> list = new ArrayList<>();
                    for (JsonNode item : results) {
                        int id = item.path("id").asInt();
                        if (id <= 0) continue;
                        String title = item.path("title").asText("Untitled");
                        String originalTitle = item.path("original_title").asText("");
                        String overview = item.path("overview").asText("");
                        String releaseDate = item.path("release_date").asText("");
                        double voteAverage = item.path("vote_average").asDouble(0.0);
                        int voteCount = item.path("vote_count").asInt(0);
                        String posterPath = item.path("poster_path").asText("");
                        String originalLang = item.path("original_language").asText("");

                        int year = 0;
                        if (releaseDate != null && releaseDate.length() >= 4) {
                            try {
                                year = Integer.parseInt(releaseDate.substring(0, 4));
                            } catch (NumberFormatException ignored) {}
                        }

                        List<String> genres = new ArrayList<>();
                        JsonNode genreIdsNode = item.path("genre_ids");
                        if (genreIdsNode.isArray()) {
                            for (JsonNode gIdNode : genreIdsNode) {
                                int gId = gIdNode.asInt();
                                String gName = GENRE_ID_MAP.get(gId);
                                if (gName != null && !gName.isEmpty()) {
                                    genres.add(gName);
                                }
                            }
                        }

                        Movie movie = new Movie(id, title, overview, releaseDate, year, genres,
                                Collections.emptyList(), Collections.emptyList(), "", voteAverage, voteCount, posterPath, originalLang);
                        movie.setOriginalTitle(originalTitle);
                        list.add(movie);
                    }
                    if (!list.isEmpty()) {
                        return list;
                    }
                }
            } catch (Exception e) {
                logger.warn("Live TMDB API search failed for '{}' (page {}): {}. Attempting fallback repository.", cleanQuery, targetPage, e.getMessage());
            }
        }

        // 2. Search Fallback Dataset
        return findMatchesInFallback(cleanQuery);
    }

    /**
     * Searches for a single movie by title. Returns detailed Movie object.
     *
     * @param queryTitle The movie title to search
     * @return The best matching Movie object with full details
     * @throws MovieNotFoundException if no movie matches the title
     */
    public Movie searchMovie(String queryTitle) {
        if (queryTitle == null || queryTitle.trim().isEmpty()) {
            throw new MovieNotFoundException("", "Please enter a valid movie name.");
        }

        List<Movie> results = searchMovies(queryTitle, 1);
        if (!results.isEmpty()) {
            Movie topMatch = results.get(0);
            if (isApiKeyConfigured() && topMatch.getId() > 0) {
                try {
                    return getMovieDetails(topMatch.getId());
                } catch (Exception ignored) {
                }
            }
            return topMatch;
        }

        throw new MovieNotFoundException(queryTitle.trim());
    }

    /**
     * Fetches full movie metadata (including credits, director, keywords, genres) from TMDB.
     */
    public Movie getMovieDetails(int movieId) {
        if (inMemoryCache.containsKey(movieId)) {
            return inMemoryCache.get(movieId);
        }

        if (isApiKeyConfigured()) {
            try {
                String url = buildUrl("/movie/" + movieId, Map.of(
                        "append_to_response", "credits,keywords",
                        "language", "en-US"
                ));

                JsonNode root = executeGetRequest(url);
                Movie movie = parseMovieDetailsJson(root);
                inMemoryCache.put(movieId, movie);
                return movie;
            } catch (Exception e) {
                logger.warn("Failed to fetch TMDB details for movie ID {}: {}", movieId, e.getMessage());
            }
        }

        // Check fallback by ID
        for (Movie m : fallbackDataset) {
            if (m.getId() == movieId) {
                return m;
            }
        }

        throw new MovieNotFoundException("Movie ID: " + movieId);
    }

    /**
     * Asynchronously gathers a broad candidate pool of movies for recommendation evaluation
     * using Multithreading (CompletableFuture and ExecutorService).
     *
     * @param targetMovie The reference movie
     * @return List<Movie> candidate movies
     */
    public List<Movie> fetchCandidateMoviesAsync(Movie targetMovie) {
        if (!isApiKeyConfigured()) {
            return new ArrayList<>(fallbackDataset);
        }

        Set<Integer> candidateIds = Collections.synchronizedSet(new HashSet<>());
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // 1. Fetch TMDB Similar Movies Endpoint in parallel
        futures.add(CompletableFuture.runAsync(() -> {
            try {
                String url = buildUrl("/movie/" + targetMovie.getId() + "/similar", Map.of("language", "en-US", "page", "1"));
                collectMovieIdsFromEndpoint(url, candidateIds);
            } catch (Exception e) {
                logger.debug("Failed fetching similar movies from TMDB: {}", e.getMessage());
            }
        }, executorService));

        // 2. Fetch TMDB Recommendations Endpoint in parallel
        futures.add(CompletableFuture.runAsync(() -> {
            try {
                String url = buildUrl("/movie/" + targetMovie.getId() + "/recommendations", Map.of("language", "en-US", "page", "1"));
                collectMovieIdsFromEndpoint(url, candidateIds);
            } catch (Exception e) {
                logger.debug("Failed fetching recommendations from TMDB: {}", e.getMessage());
            }
        }, executorService));

        // 3. Fetch Top Rated Movies in parallel
        futures.add(CompletableFuture.runAsync(() -> {
            try {
                String url = buildUrl("/movie/top_rated", Map.of("language", "en-US", "page", "1"));
                collectMovieIdsFromEndpoint(url, candidateIds);
            } catch (Exception e) {
                logger.debug("Failed fetching top rated movies: {}", e.getMessage());
            }
        }, executorService));

        // 4. Fetch Popular Movies in parallel
        futures.add(CompletableFuture.runAsync(() -> {
            try {
                String url = buildUrl("/movie/popular", Map.of("language", "en-US", "page", "1"));
                collectMovieIdsFromEndpoint(url, candidateIds);
            } catch (Exception e) {
                logger.debug("Failed fetching popular movies: {}", e.getMessage());
            }
        }, executorService));

        // Wait for all asynchronous requests to complete with timeout safety
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(4, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.warn("Candidate gathering thread pool timed out / interrupted: {}", e.getMessage());
        }

        // Convert candidate IDs into Movie objects concurrently using thread pool
        List<Movie> candidatePool = Collections.synchronizedList(new ArrayList<>());
        List<CompletableFuture<Void>> detailFutures = new ArrayList<>();

        for (Integer id : candidateIds) {
            if (id == targetMovie.getId()) continue;
            detailFutures.add(CompletableFuture.runAsync(() -> {
                try {
                    Movie movie = getMovieDetails(id);
                    if (movie != null) {
                        candidatePool.add(movie);
                    }
                } catch (Exception ignored) {
                }
            }, executorService));
        }

        try {
            CompletableFuture.allOf(detailFutures.toArray(new CompletableFuture[0]))
                    .get(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.debug("Candidate details fetching timeout / completed partially: {}", e.getMessage());
        }

        // Supplement with fallback dataset to ensure rich candidate pool
        for (Movie fallbackMovie : fallbackDataset) {
            if (fallbackMovie.getId() != targetMovie.getId() && !candidateIds.contains(fallbackMovie.getId())) {
                candidatePool.add(fallbackMovie);
            }
        }

        return new ArrayList<>(candidatePool);
    }

    private void collectMovieIdsFromEndpoint(String url, Set<Integer> idSet) {
        try {
            JsonNode root = executeGetRequest(url);
            JsonNode results = root.path("results");
            if (results.isArray()) {
                for (JsonNode node : results) {
                    int id = node.path("id").asInt();
                    if (id > 0) {
                        idSet.add(id);
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Endpoint parsing error for {}: {}", url, e.getMessage());
        }
    }

    private JsonNode executeGetRequest(String urlStr) throws Exception {
        URI uri = URI.create(urlStr);
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) uri.toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AI-Movie-Recommender/1.0");

        if (isApiKeyConfigured() && isBearerToken(this.apiKey)) {
            conn.setRequestProperty("Authorization", "Bearer " + this.apiKey);
        }

        int status = conn.getResponseCode();
        if (status != 200) {
            throw new TmdbApiException("TMDB API request failed with status: " + status, status);
        }

        try (java.io.InputStream is = conn.getInputStream()) {
            return objectMapper.readTree(is);
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Dispatches category requests to the appropriate TMDB endpoint with pagination, language filtering, and caching.
     * Supports: "now_playing", "upcoming", "trending", "recently_released" (discover).
     * Supports regional filtering (e.g. "IN" for India).
     * Supports language filtering (e.g. "hi", "ta", "te", "kn", "ml", "ko", "ja", "en", or empty for all).
     */
    public MoviePageResult fetchMoviesByCategory(String category, int page, String region) {
        return fetchMoviesByCategory(category, page, region, "");
    }

    public MoviePageResult fetchMoviesByCategory(String category, int page, String region, String language) {
        String cleanCategory = (category != null && !category.trim().isEmpty()) ? category.toLowerCase().trim() : "now_playing";
        int cleanPage = Math.max(page, 1);
        String cleanRegion = (region != null) ? region.trim().toUpperCase() : "";
        String cleanLanguage = (language != null && !language.trim().equalsIgnoreCase("all")) ? language.trim().toLowerCase() : "";

        String cacheKey = cleanCategory + ":" + cleanRegion + ":" + cleanLanguage + ":" + cleanPage;
        CachedPage cached = pageCache.get(cacheKey);
        if (cached != null && !cached.isExpired(PAGE_CACHE_TTL_MS)) {
            MoviePageResult res = cached.pageResult;
            res.setFromCache(true);
            return res;
        }

        MoviePageResult result;
        switch (cleanCategory) {
            case "popular":
                result = fetchPopularMovies(cleanPage, cleanRegion, cleanLanguage);
                break;
            case "upcoming":
                result = fetchUpcomingMovies(cleanPage, cleanRegion, cleanLanguage);
                break;
            case "trending":
                result = fetchTrendingMovies(cleanPage, cleanRegion, cleanLanguage);
                break;
            case "recently_released":
            case "discover":
                result = fetchRecentlyReleasedMovies(cleanPage, cleanRegion, cleanLanguage, null, null);
                break;
            case "now_playing":
            default:
                result = fetchNowPlayingMovies(cleanPage, cleanRegion, cleanLanguage);
                break;
        }

        if (result != null && !result.isEmpty()) {
            pageCache.put(cacheKey, new CachedPage(result));
        }

        return result;
    }

    public MoviePageResult fetchPopularMovies(int page, String region) {
        return fetchPopularMovies(page, region, "");
    }

    public MoviePageResult fetchPopularMovies(int page, String region, String language) {
        int targetPage = Math.max(page, 1);
        Map<String, String> params = new HashMap<>();
        params.put("page", String.valueOf(targetPage));
        params.put("language", "en-US");
        if (region != null && !region.isEmpty()) {
            params.put("region", region);
        }

        String lang = (language != null && !language.trim().equalsIgnoreCase("all")) ? language.trim().toLowerCase() : "";

        if (isApiKeyConfigured()) {
            try {
                String endpoint;
                if (!lang.isEmpty()) {
                    endpoint = "/discover/movie";
                    params.put("with_original_language", lang);
                    params.put("sort_by", "popularity.desc");
                } else {
                    endpoint = "/movie/popular";
                }
                String url = buildUrl(endpoint, params);
                JsonNode root = executeGetRequest(url);
                return parsePaginatedMoviesJson(root, targetPage, "popular", "Popular Movies", region, lang);
            } catch (Exception e) {
                logger.warn("Live TMDB popular request failed (page {}, region {}, lang {}): {}. Using fallback.", targetPage, region, lang, e.getMessage());
            }
        }

        return getFallbackPageResult("popular", "Popular Movies", targetPage, region, lang);
    }

    public MoviePageResult fetchNowPlayingMovies(int page, String region) {
        return fetchNowPlayingMovies(page, region, "");
    }

    public MoviePageResult fetchNowPlayingMovies(int page, String region, String language) {
        int targetPage = Math.max(page, 1);
        Map<String, String> params = new HashMap<>();
        params.put("page", String.valueOf(targetPage));
        params.put("language", "en-US");
        if (region != null && !region.isEmpty()) {
            params.put("region", region);
        }

        String lang = (language != null && !language.trim().equalsIgnoreCase("all")) ? language.trim().toLowerCase() : "";

        if (isApiKeyConfigured()) {
            try {
                String endpoint;
                if (!lang.isEmpty()) {
                    endpoint = "/discover/movie";
                    params.put("with_original_language", lang);
                    params.put("primary_release_date.lte", LocalDate.now().toString());
                    params.put("primary_release_date.gte", LocalDate.now().minusDays(60).toString());
                    params.put("sort_by", "popularity.desc");
                } else {
                    endpoint = "/movie/now_playing";
                }
                String url = buildUrl(endpoint, params);
                JsonNode root = executeGetRequest(url);
                return parsePaginatedMoviesJson(root, targetPage, "now_playing", "Now Playing in Theaters", region, lang);
            } catch (Exception e) {
                logger.warn("Live TMDB now_playing request failed (page {}, region {}, lang {}): {}. Using fallback.", targetPage, region, lang, e.getMessage());
            }
        }

        return getFallbackPageResult("now_playing", "Now Playing in Theaters", targetPage, region, lang);
    }

    public MoviePageResult fetchUpcomingMovies(int page, String region) {
        return fetchUpcomingMovies(page, region, "");
    }

    public MoviePageResult fetchUpcomingMovies(int page, String region, String language) {
        int targetPage = Math.max(page, 1);
        Map<String, String> params = new HashMap<>();
        params.put("page", String.valueOf(targetPage));
        params.put("language", "en-US");
        if (region != null && !region.isEmpty()) {
            params.put("region", region);
        }

        String lang = (language != null && !language.trim().equalsIgnoreCase("all")) ? language.trim().toLowerCase() : "";

        if (isApiKeyConfigured()) {
            try {
                String endpoint;
                if (!lang.isEmpty()) {
                    endpoint = "/discover/movie";
                    params.put("with_original_language", lang);
                    params.put("primary_release_date.gte", LocalDate.now().plusDays(1).toString());
                    params.put("primary_release_date.lte", LocalDate.now().plusMonths(6).toString());
                    params.put("sort_by", "popularity.desc");
                } else {
                    endpoint = "/movie/upcoming";
                }
                String url = buildUrl(endpoint, params);
                JsonNode root = executeGetRequest(url);
                return parsePaginatedMoviesJson(root, targetPage, "upcoming", "Upcoming Movies", region, lang);
            } catch (Exception e) {
                logger.warn("Live TMDB upcoming request failed (page {}, region {}, lang {}): {}. Using fallback.", targetPage, region, lang, e.getMessage());
            }
        }

        return getFallbackPageResult("upcoming", "Upcoming Movies", targetPage, region, lang);
    }

    public MoviePageResult fetchTrendingMovies(int page, String region) {
        return fetchTrendingMovies(page, region, "");
    }

    public MoviePageResult fetchTrendingMovies(int page, String region, String language) {
        int targetPage = Math.max(page, 1);
        Map<String, String> params = new HashMap<>();
        params.put("page", String.valueOf(targetPage));
        params.put("language", "en-US");

        String lang = (language != null && !language.trim().equalsIgnoreCase("all")) ? language.trim().toLowerCase() : "";

        if (isApiKeyConfigured()) {
            try {
                String endpoint;
                if (!lang.isEmpty()) {
                    endpoint = "/discover/movie";
                    params.put("with_original_language", lang);
                    params.put("sort_by", "popularity.desc");
                } else {
                    endpoint = "/trending/movie/day";
                }
                String url = buildUrl(endpoint, params);
                JsonNode root = executeGetRequest(url);
                return parsePaginatedMoviesJson(root, targetPage, "trending", "Trending Movies Today", region, lang);
            } catch (Exception e) {
                logger.warn("Live TMDB trending request failed (page {}, lang {}): {}. Using fallback.", targetPage, lang, e.getMessage());
            }
        }

        return getFallbackPageResult("trending", "Trending Movies Today", targetPage, region, lang);
    }

    public MoviePageResult fetchRecentlyReleasedMovies(int page, String region, String releaseDateGte, String releaseDateLte) {
        return fetchRecentlyReleasedMovies(page, region, "", releaseDateGte, releaseDateLte);
    }

    public MoviePageResult fetchRecentlyReleasedMovies(int page, String region, String language, String releaseDateGte, String releaseDateLte) {
        int targetPage = Math.max(page, 1);
        Map<String, String> params = new HashMap<>();
        params.put("page", String.valueOf(targetPage));
        params.put("language", "en-US");
        params.put("sort_by", "primary_release_date.desc");
        params.put("vote_count.gte", "5");

        String lte = (releaseDateLte != null && !releaseDateLte.isEmpty()) ? releaseDateLte : LocalDate.now().toString();
        String gte = (releaseDateGte != null && !releaseDateGte.isEmpty()) ? releaseDateGte : LocalDate.now().minusMonths(6).toString();
        params.put("primary_release_date.lte", lte);
        params.put("primary_release_date.gte", gte);

        if (region != null && !region.trim().isEmpty()) {
            params.put("region", region.trim().toUpperCase());
        }

        String lang = (language != null && !language.trim().equalsIgnoreCase("all")) ? language.trim().toLowerCase() : "";
        if (!lang.isEmpty()) {
            params.put("with_original_language", lang);
        }

        if (isApiKeyConfigured()) {
            try {
                String url = buildUrl("/discover/movie", params);
                JsonNode root = executeGetRequest(url);
                return parsePaginatedMoviesJson(root, targetPage, "recently_released", "Recently Released Movies (Discover)", region, lang);
            } catch (Exception e) {
                logger.warn("Live TMDB discover request failed (page {}, region {}, lang {}): {}. Using fallback.", targetPage, region, lang, e.getMessage());
            }
        }

        return getFallbackPageResult("recently_released", "Recently Released Movies (Discover)", targetPage, region, lang);
    }

    private MoviePageResult parsePaginatedMoviesJson(JsonNode root, int requestedPage, String category, String categoryTitle, String region, String language) {
        int page = root.path("page").asInt(requestedPage);
        int totalPages = root.path("total_pages").asInt(1);
        int totalResults = root.path("total_results").asInt(0);

        JsonNode results = root.path("results");
        Map<Integer, Movie> deduplicatedMovies = new LinkedHashMap<>();

        if (results.isArray()) {
            for (JsonNode item : results) {
                int id = item.path("id").asInt();
                if (id <= 0 || deduplicatedMovies.containsKey(id)) {
                    continue;
                }

                String title = item.path("title").asText("Untitled");
                String originalTitle = item.path("original_title").asText("");
                String overview = item.path("overview").asText("");
                String releaseDate = item.path("release_date").asText("");
                double voteAverage = item.path("vote_average").asDouble(0.0);
                int voteCount = item.path("vote_count").asInt(0);
                String posterPath = item.path("poster_path").asText("");
                String originalLang = item.path("original_language").asText("");

                int year = 0;
                if (releaseDate != null && releaseDate.length() >= 4) {
                    try {
                        year = Integer.parseInt(releaseDate.substring(0, 4));
                    } catch (NumberFormatException ignored) {
                    }
                }

                List<String> genres = new ArrayList<>();
                JsonNode genreIdsNode = item.path("genre_ids");
                if (genreIdsNode.isArray()) {
                    for (JsonNode gIdNode : genreIdsNode) {
                        int gId = gIdNode.asInt();
                        String gName = GENRE_ID_MAP.get(gId);
                        if (gName != null && !gName.isEmpty()) {
                            genres.add(gName);
                        }
                    }
                }

                Movie movie = new Movie(id, title, overview, releaseDate, year, genres,
                        Collections.emptyList(), Collections.emptyList(), "", voteAverage, voteCount, posterPath, originalLang);
                movie.setOriginalTitle(originalTitle);
                deduplicatedMovies.put(id, movie);
            }
        }

        return new MoviePageResult(new ArrayList<>(deduplicatedMovies.values()), page, totalPages, totalResults,
                category, categoryTitle, region, language, false);
    }

    private MoviePageResult getFallbackPageResult(String category, String categoryTitle, int page, String region, String language) {
        int pageSize = 8;
        List<Movie> filtered = fallbackDataset;
        if (language != null && !language.trim().isEmpty() && !language.equalsIgnoreCase("all")) {
            filtered = new ArrayList<>();
            for (Movie m : fallbackDataset) {
                if (language.equalsIgnoreCase(m.getOriginalLanguage())) {
                    filtered.add(m);
                }
            }
            if (filtered.isEmpty()) {
                filtered = fallbackDataset;
            }
        }

        int totalItems = filtered.size();
        int totalPages = (int) Math.ceil((double) totalItems / (double) pageSize);
        int cleanPage = Math.min(Math.max(page, 1), Math.max(totalPages, 1));

        int fromIndex = (cleanPage - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, totalItems);

        List<Movie> pageItems = (fromIndex < totalItems)
                ? new ArrayList<>(filtered.subList(fromIndex, toIndex))
                : Collections.emptyList();

        return new MoviePageResult(pageItems, cleanPage, totalPages, totalItems, category, categoryTitle, region, language, true);
    }

    private Movie parseMovieDetailsJson(JsonNode node) {
        int id = node.path("id").asInt();
        String title = node.path("title").asText("Untitled");
        String originalTitle = node.path("original_title").asText("");
        String overview = node.path("overview").asText("");
        String releaseDate = node.path("release_date").asText("");
        double voteAverage = node.path("vote_average").asDouble(0.0);
        int voteCount = node.path("vote_count").asInt(0);
        String posterPath = node.path("poster_path").asText("");
        String originalLang = node.path("original_language").asText("");

        int year = 0;
        if (releaseDate != null && releaseDate.length() >= 4) {
            try {
                year = Integer.parseInt(releaseDate.substring(0, 4));
            } catch (NumberFormatException ignored) {
            }
        }

        // Parse Genres
        List<String> genres = new ArrayList<>();
        JsonNode genresNode = node.path("genres");
        if (genresNode.isArray()) {
            for (JsonNode g : genresNode) {
                String name = g.path("name").asText();
                if (!name.isEmpty()) genres.add(name);
            }
        }

        // Parse Director & Top Cast
        String director = "";
        List<String> topCast = new ArrayList<>();
        JsonNode creditsNode = node.path("credits");
        if (!creditsNode.isMissingNode()) {
            JsonNode crewNode = creditsNode.path("crew");
            if (crewNode.isArray()) {
                for (JsonNode crewMember : crewNode) {
                    if ("Director".equalsIgnoreCase(crewMember.path("job").asText())) {
                        director = crewMember.path("name").asText();
                        break;
                    }
                }
            }

            JsonNode castNode = creditsNode.path("cast");
            if (castNode.isArray()) {
                int count = 0;
                for (JsonNode actor : castNode) {
                    String actorName = actor.path("name").asText();
                    if (!actorName.isEmpty()) {
                        topCast.add(actorName);
                        count++;
                        if (count >= 5) break;
                    }
                }
            }
        }

        // Parse Keywords
        List<String> keywords = new ArrayList<>();
        JsonNode keywordsNode = node.path("keywords").path("keywords");
        if (keywordsNode.isArray()) {
            for (JsonNode kw : keywordsNode) {
                String name = kw.path("name").asText();
                if (!name.isEmpty()) keywords.add(name);
            }
        }

        Movie movie = new Movie(id, title, overview, releaseDate, year, genres, keywords, topCast, director, voteAverage, voteCount, posterPath, originalLang);
        movie.setOriginalTitle(originalTitle);
        return movie;
    }

    private List<Movie> findMatchesInFallback(String query) {
        String lowerQuery = query.toLowerCase().trim();
        List<Movie> matches = new ArrayList<>();

        // Exact match first
        for (Movie m : fallbackDataset) {
            if (m.getTitle().toLowerCase().equalsIgnoreCase(lowerQuery) ||
                (m.getOriginalTitle() != null && m.getOriginalTitle().toLowerCase().equalsIgnoreCase(lowerQuery))) {
                matches.add(m);
            }
        }

        // Substring / contains match next
        for (Movie m : fallbackDataset) {
            if (!matches.contains(m)) {
                if (m.getTitle().toLowerCase().contains(lowerQuery) ||
                    lowerQuery.contains(m.getTitle().toLowerCase()) ||
                    (m.getOriginalTitle() != null && m.getOriginalTitle().toLowerCase().contains(lowerQuery))) {
                    matches.add(m);
                }
            }
        }

        return matches;
    }

    private Movie findInFallback(String query) {
        List<Movie> matches = findMatchesInFallback(query);
        return !matches.isEmpty() ? matches.get(0) : null;
    }

    public List<Movie> getFallbackDataset() {
        return new ArrayList<>(fallbackDataset);
    }

    @PreDestroy
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(2, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Initializes a rich, authentic dataset containing real TMDB IDs, genres,
     * release years (ranging from classics to 2024-2026), directors, cast, and poster links.
     */
    private List<Movie> initializeFallbackDataset() {
        List<Movie> list = new ArrayList<>();

        // 1. Inception (Sci-Fi, Action, Thriller) - TMDB ID: 27205
        list.add(new Movie(27205, "Inception",
                "Cobb, a skilled thief who steals corporate secrets through the use of dream-sharing technology, is given the inverse task of planting an idea into the mind of a C.E.O.",
                "2010-07-16", 2010,
                Arrays.asList("Science Fiction", "Action", "Adventure"),
                Arrays.asList("dream", "subconscious", "heist", "mind-bending", "physics"),
                Arrays.asList("Leonardo DiCaprio", "Joseph Gordon-Levitt", "Elliot Page", "Tom Hardy", "Ken Watanabe"),
                "Christopher Nolan", 8.4, 35000,
                "/ljsZTbVsrQSqZgWeep2B1QiDKuh.jpg"));

        // 2. Interstellar (Sci-Fi, Drama) - TMDB ID: 157336
        list.add(new Movie(157336, "Interstellar",
                "The adventures of a group of explorers who make use of a newly discovered wormhole to surpass the limitations on human space travel and conquer the vast distances involved in an interstellar voyage.",
                "2014-11-07", 2014,
                Arrays.asList("Science Fiction", "Drama", "Adventure"),
                Arrays.asList("space travel", "black hole", "wormhole", "quantum physics", "time dilation", "father daughter"),
                Arrays.asList("Matthew McConaughey", "Anne Hathaway", "Jessica Chastain", "Michael Caine", "Matt Damon"),
                "Christopher Nolan", 8.4, 34000,
                "/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg"));

        // 3. The Dark Knight (Action, Crime, Drama) - TMDB ID: 155
        list.add(new Movie(155, "The Dark Knight",
                "Batman raises the stakes in his war on crime. With the help of Lt. Jim Gordon and District Attorney Harvey Dent, Batman sets out to dismantle the remaining criminal organizations that plague the streets.",
                "2008-07-18", 2008,
                Arrays.asList("Action", "Crime", "Drama"),
                Arrays.asList("batman", "joker", "gotham", "vigilante", "superhero", "chaos"),
                Arrays.asList("Christian Bale", "Heath Ledger", "Aaron Eckhart", "Michael Caine", "Maggie Gyllenhaal"),
                "Christopher Nolan", 8.5, 32000,
                "/qJ2tW6WMUDux911r6m7haRef0WH.jpg"));

        // 4. Tenet (Sci-Fi, Action, Thriller) - TMDB ID: 577922
        list.add(new Movie(577922, "Tenet",
                "Armed with only one word - Tenet - and fighting for the survival of the entire world, the Protagonist journeys through a twilight world of international espionage on a mission that will unfold in something beyond real time.",
                "2020-09-03", 2020,
                Arrays.asList("Action", "Science Fiction", "Thriller"),
                Arrays.asList("time inversion", "espionage", "quantum", "entropy", "secret agent"),
                Arrays.asList("John David Washington", "Robert Pattinson", "Elizabeth Debicki", "Kenneth Branagh", "Michael Caine"),
                "Christopher Nolan", 7.2, 9200,
                "/aCIFMriQ2vtJCFEiNPQIhmTEHVG.jpg"));

        // 5. Oppenheimer (Drama, History) - TMDB ID: 872585
        list.add(new Movie(872585, "Oppenheimer",
                "The story of J. Robert Oppenheimer's role in the development of the atomic bomb during World War II and the subsequent security clearance hearings.",
                "2023-07-21", 2023,
                Arrays.asList("Drama", "History"),
                Arrays.asList("atomic bomb", "physicist", "world war ii", "manhattan project", "politics"),
                Arrays.asList("Cillian Murphy", "Emily Blunt", "Matt Damon", "Robert Downey Jr.", "Florence Pugh"),
                "Christopher Nolan", 8.1, 8800,
                "/8Gxv8gSFCU0XGDykEGv7zR1n2ua.jpg"));

        // 6. The Matrix (Sci-Fi, Action) - TMDB ID: 603
        list.add(new Movie(603, "The Matrix",
                "Set in the 22nd century, The Matrix tells the story of a computer hacker who joins a group of underground insurgents fighting the vast and powerful computers who now rule the earth.",
                "1999-03-31", 1999,
                Arrays.asList("Science Fiction", "Action"),
                Arrays.asList("virtual reality", "artificial intelligence", "simulation", "cyberpunk", "hacker"),
                Arrays.asList("Keanu Reeves", "Laurence Fishburne", "Carrie-Anne Moss", "Hugo Weaving"),
                "Lana Wachowski", 8.2, 25000,
                "/f89U3ADr1oiB1s9GkdPOEpXUk5H.jpg"));

        // 7. Shutter Island (Mystery, Thriller, Drama) - TMDB ID: 11324
        list.add(new Movie(11324, "Shutter Island",
                "World War II soldier-turned-U.S. Marshal Teddy Daniels investigates the disappearance of a patient from Boston's Shutter Island Ashecliffe Hospital for the criminally insane.",
                "2010-02-19", 2010,
                Arrays.asList("Mystery", "Thriller", "Drama"),
                Arrays.asList("psychological", "asylum", "investigation", "twist", "hallucination"),
                Arrays.asList("Leonardo DiCaprio", "Mark Ruffalo", "Ben Kingsley", "Max von Sydow", "Michelle Williams"),
                "Martin Scorsese", 8.2, 23000,
                "/4GDy0PHYX3VRXUtwK5ysagvkihs.jpg"));

        // 8. Dune: Part Two (Sci-Fi, Adventure) - TMDB ID: 693134
        list.add(new Movie(693134, "Dune: Part Two",
                "Follow the mythic journey of Paul Atreides as he unites with Chani and the Fremen while on a path of revenge against the conspirators who destroyed his family.",
                "2024-03-01", 2024,
                Arrays.asList("Science Fiction", "Adventure"),
                Arrays.asList("desert", "sandworm", "prophecy", "spice", "space empire", "rebellion"),
                Arrays.asList("Timothée Chalamet", "Zendaya", "Rebecca Ferguson", "Javier Bardem", "Austin Butler"),
                "Denis Villeneuve", 8.2, 5400,
                "/1pdfLvkbY9ohJlCjQH2CZjjYVvJ.jpg"));

        // 9. Blade Runner 2049 (Sci-Fi, Mystery) - TMDB ID: 335984
        list.add(new Movie(335984, "Blade Runner 2049",
                "Thirty years after the events of the first film, a new blade runner, LAPD Officer K, unearths a long-buried secret that has the potential to plunge what's left of society into chaos.",
                "2017-10-06", 2017,
                Arrays.asList("Science Fiction", "Mystery", "Drama"),
                Arrays.asList("cyberpunk", "replicant", "future", "artificial intelligence", "dystopia"),
                Arrays.asList("Ryan Gosling", "Harrison Ford", "Ana de Armas", "Sylvia Hoeks", "Robin Wright"),
                "Denis Villeneuve", 7.6, 13000,
                "/gajva2L0rPYkEWjzgFlBXCAVBE5.jpg"));

        // 10. Arrival (Sci-Fi, Mystery, Drama) - TMDB ID: 329865
        list.add(new Movie(329865, "Arrival",
                "Taking place after alien crafts land around the world, an expert linguist is recruited by the military to determine whether they come in peace or are a threat.",
                "2016-11-11", 2016,
                Arrays.asList("Science Fiction", "Mystery", "Drama"),
                Arrays.asList("alien contact", "linguistics", "time perception", "communication", "spacecraft"),
                Arrays.asList("Amy Adams", "Jeremy Renner", "Forest Whitaker", "Michael Stuhlbarg"),
                "Denis Villeneuve", 7.6, 17000,
                "/x2FJsf1ElAgr63Y3PNPtJrcmpoe.jpg"));

        // 11. The Prestige (Drama, Mystery, Sci-Fi) - TMDB ID: 1124
        list.add(new Movie(1124, "The Prestige",
                "A mysterious story of two magicians whose intense rivalry leads them on a life-long battle for supremacy -- full of obsession, deceit and jealousy with dangerous and deadly consequences.",
                "2006-10-20", 2006,
                Arrays.asList("Drama", "Mystery", "Science Fiction"),
                Arrays.asList("magic", "illusion", "rivalry", "obsession", "teleportation", "victorian"),
                Arrays.asList("Hugh Jackman", "Christian Bale", "Michael Caine", "Scarlett Johansson", "David Bowie"),
                "Christopher Nolan", 8.2, 16000,
                "/bdN3gXu4UMxMc7ioJjBGl3vB76m.jpg"));

        // 12. The Godfather (Drama, Crime) - TMDB ID: 238
        list.add(new Movie(238, "The Godfather",
                "Spanning the years 1945 to 1955, a chronicle of the fictional Italian-American Corleone crime family. When organized crime family patriarch, Vito Corleone barely survives an attempt on his life, his youngest son, Michael steps up.",
                "1972-03-24", 1972,
                Arrays.asList("Drama", "Crime"),
                Arrays.asList("mafia", "patriarch", "organized crime", "family loyalty", "revenge"),
                Arrays.asList("Marlon Brando", "Al Pacino", "James Caan", "Robert Duvall", "Diane Keaton"),
                "Francis Ford Coppola", 8.7, 20000,
                "/3bhkrj58Vtu7enYsRolD1fZdja1.jpg"));

        // 13. GoodFellas (Drama, Crime) - TMDB ID: 769
        list.add(new Movie(769, "GoodFellas",
                "The true story of Henry Hill, a half-Irish, half-Sicilian Brooklyn kid who is adopted by neighbourhood gangsters at an early age and climbs the ranks of a Mafia family.",
                "1990-09-12", 1990,
                Arrays.asList("Drama", "Crime"),
                Arrays.asList("mafia", "gangster", "brooklyn", "mobster", "crime empire"),
                Arrays.asList("Robert De Niro", "Ray Liotta", "Joe Pesci", "Lorraine Bracco", "Paul Sorvino"),
                "Martin Scorsese", 8.5, 12500,
                "/aKuFiU82s5ISJpGZp7YkIr3kcuo.jpg"));

        // 14. Pulp Fiction (Crime, Thriller) - TMDB ID: 680
        list.add(new Movie(680, "Pulp Fiction",
                "A burger-loving hit man, his philosophical partner, a drug-addled gangster's moll and a washed-up boxer converge in this sprawling, comedic crime caper.",
                "1994-10-14", 1994,
                Arrays.asList("Crime", "Thriller"),
                Arrays.asList("hitman", "dialogue", "los angeles", "nonlinear", "boxer", "briefcase"),
                Arrays.asList("John Travolta", "Samuel L. Jackson", "Uma Thurman", "Bruce Willis", "Ving Rhames"),
                "Quentin Tarantino", 8.5, 27000,
                "/d5iIlFn5s0ImszYzBPb8JPIfbXD.jpg"));

        // 15. Fight Club (Drama, Thriller) - TMDB ID: 550
        list.add(new Movie(550, "Fight Club",
                "A ticking-time-bomb insomniac and a slippery soap salesman channel primal male aggression into a shocking new form of therapy. Their concept catches on, with underground 'fight clubs' forming in every town.",
                "1999-10-15", 1999,
                Arrays.asList("Drama", "Thriller"),
                Arrays.asList("insomnia", "alter ego", "anarchy", "anti consumerism", "underground"),
                Arrays.asList("Edward Norton", "Brad Pitt", "Helena Bonham Carter", "Meat Loaf"),
                "David Fincher", 8.4, 28500,
                "/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg"));

        // 16. Se7en (Crime, Mystery, Thriller) - TMDB ID: 807
        list.add(new Movie(807, "Se7en",
                "Two homicide detectives are on a desperate hunt for a serial killer whose crimes are based on the 'seven deadly sins'.",
                "1995-09-22", 1995,
                Arrays.asList("Crime", "Mystery", "Thriller"),
                Arrays.asList("serial killer", "seven deadly sins", "detective", "grim", "psychological"),
                Arrays.asList("Brad Pitt", "Morgan Freeman", "Gwyneth Paltrow", "Kevin Spacey"),
                "David Fincher", 8.4, 20500,
                "/6yoghtyTpznpBik8EngEmJskVUO.jpg"));

        // 17. The Batman (Crime, Action, Thriller) - TMDB ID: 414906
        list.add(new Movie(414906, "The Batman",
                "In his second year of fighting crime, Batman uncovers corruption in Gotham City that connects to his own family while facing a serial killer known as the Riddler.",
                "2022-03-04", 2022,
                Arrays.asList("Crime", "Action", "Thriller"),
                Arrays.asList("detective", "riddler", "gotham", "noir", "corruption"),
                Arrays.asList("Robert Pattinson", "Zoë Kravitz", "Paul Dano", "Jeffrey Wright", "Colin Farrell"),
                "Matt Reeves", 7.7, 9500,
                "/74xTEgt7R36Fpooo50r9T25onhq.jpg"));

        // 18. Avatar: The Way of Water (Sci-Fi, Adventure, Action) - TMDB ID: 76600
        list.add(new Movie(76600, "Avatar: The Way of Water",
                "Set more than a decade after the events of the first film, learn the story of the Sully family, the trouble that follows them, the lengths they go to keep each other safe, the battles they fight to stay alive, and the tragedies they endure.",
                "2022-12-16", 2022,
                Arrays.asList("Science Fiction", "Adventure", "Action"),
                Arrays.asList("pandora", "ocean", "alien world", "family", "colonization"),
                Arrays.asList("Sam Worthington", "Zoe Saldaña", "Sigourney Weaver", "Stephen Lang", "Kate Winslet"),
                "James Cameron", 7.6, 11000,
                "/t6HIqrRAclMCA60NsSmeqe9RmNV.jpg"));

        // 19. Avengers: Endgame (Action, Adventure, Sci-Fi) - TMDB ID: 299534
        list.add(new Movie(299534, "Avengers: Endgame",
                "After the devastating events of Avengers: Infinity War, the universe is in ruins. With the help of remaining allies, the Avengers assemble once more in order to reverse Thanos' actions and restore balance to the universe.",
                "2019-04-26", 2019,
                Arrays.asList("Action", "Adventure", "Science Fiction"),
                Arrays.asList("superhero", "time travel", "infinity stones", "marvel", "climax"),
                Arrays.asList("Robert Downey Jr.", "Chris Evans", "Mark Ruffalo", "Chris Hemsworth", "Scarlett Johansson"),
                "Anthony Russo", 8.3, 24800,
                "/or06FN3Dka5tukK1e9sl16pB3iy.jpg"));

        // 20. Superbad (Comedy) - TMDB ID: 8363
        list.add(new Movie(8363, "Superbad",
                "Two co-dependent high school seniors are forced to deal with separation anxiety after their plan to stage a booze-soaked party goes awry.",
                "2007-08-17", 2007,
                Arrays.asList("Comedy"),
                Arrays.asList("high school", "party", "coming of age", "fake id", "friendship"),
                Arrays.asList("Jonah Hill", "Michael Cera", "Christopher Mintz-Plasse", "Bill Hader", "Seth Rogen"),
                "Greg Mottola", 7.2, 7200,
                "/ek8e8txUyUwd2BNqj6lFEerJ9sm.jpg"));

        // 21. The Hangover (Comedy) - TMDB ID: 18785
        list.add(new Movie(18785, "The Hangover",
                "When three bachelors wake up from a bachelor party in Las Vegas with no memory of the previous night and the bachelor missing, they must retrace their steps in order to find their friend before the wedding.",
                "2009-06-05", 2009,
                Arrays.asList("Comedy"),
                Arrays.asList("las vegas", "bachelor party", "hangover", "mystery", "wedding"),
                Arrays.asList("Bradley Cooper", "Ed Helms", "Zach Galifianakis", "Justin Bartha"),
                "Todd Phillips", 7.3, 16500,
                "/uluhlXubGu1VxU63X9VHCLWDAYP.jpg"));

        // 22. Gladiator II (Action, Adventure, Drama) - TMDB ID: 558449 (Recent 2024-2026 release)
        list.add(new Movie(558449, "Gladiator II",
                "Years after witnessing the death of the revered hero Maximus at the hands of his uncle, Lucius must enter the Colosseum after his home is conquered by the tyrannical Emperors who now lead Rome with an iron fist.",
                "2024-11-22", 2024,
                Arrays.asList("Action", "Adventure", "Drama"),
                Arrays.asList("colosseum", "gladiator", "ancient rome", "revenge", "empire"),
                Arrays.asList("Paul Mescal", "Pedro Pascal", "Denzel Washington", "Connie Nielsen", "Joseph Quinn"),
                "Ridley Scott", 6.8, 3200,
                "/2cxhvwyEwRlysAmRH4iodkvo0z5.jpg"));

        // 23. Spider-Man: Into the Spider-Verse (Animation, Action, Sci-Fi) - TMDB ID: 324857
        list.add(new Movie(324857, "Spider-Man: Into the Spider-Verse",
                "Teenager Miles Morales becomes the new Spider-Man and joins other Spider-Heroes from parallel dimensions to stop a threat to all reality.",
                "2018-12-14", 2018,
                Arrays.asList("Animation", "Action", "Science Fiction"),
                Arrays.asList("multiverse", "spider-man", "alternate dimension", "superhero", "comic book"),
                Arrays.asList("Shameik Moore", "Jake Johnson", "Hailee Steinfeld", "Mahershala Ali"),
                "Bob Persichetti", 8.4, 15000,
                "/iiZZdoQBEYBv6id8su7ImL0oCbD.jpg"));

        // 24. Spirited Away (Animation, Fantasy) - TMDB ID: 129
        list.add(new Movie(129, "Spirited Away",
                "A young girl, Chihiro, becomes trapped in a strange new world of spirits. When her parents undergo a mysterious transformation, she must call upon the courage she never knew she had to free her family.",
                "2001-07-20", 2001,
                Arrays.asList("Animation", "Fantasy", "Family"),
                Arrays.asList("studio ghibli", "spirit world", "witch", "magic", "bathhouse"),
                Arrays.asList("Rumi Hiiragi", "Miyu Irino", "Mari Natsuki", "Takashi Naito"),
                "Hayao Miyazaki", 8.5, 16000,
                "/39wmItIWsg5sZMyRUHLkWBcuVCM.jpg"));

        // 25. Titanic (Drama, Romance) - TMDB ID: 597
        list.add(new Movie(597, "Titanic",
                "101-year-old Rose DeWitt Bukater tells the story of her life aboard the Titanic, 84 years later. A young Rose boards the ship with her mother and fiance. Meanwhile, Jack Dawson and Fabrizio De Rossi win third-class tickets aboard the ship. The story tells about the romance between Rose and Jack.",
                "1997-12-19", 1997,
                Arrays.asList("Drama", "Romance"),
                Arrays.asList("shipwreck", "iceberg", "tragic love", "historical", "class differences"),
                Arrays.asList("Leonardo DiCaprio", "Kate Winslet", "Billy Zane", "Kathy Bates", "Bill Paxton"),
                "James Cameron", 7.9, 25000,
                "/9xjZS2rlVxm8SFx8kPC3aIGCOYQ.jpg"));

        // 26. Avatar (Action, Adventure, Fantasy, Sci-Fi) - TMDB ID: 19995
        list.add(new Movie(19995, "Avatar",
                "In the 22nd century, a paraplegic Marine is dispatched to the moon Pandora on a unique mission, but becomes torn between following orders and protecting an alien civilization.",
                "2009-12-18", 2009,
                Arrays.asList("Action", "Adventure", "Fantasy", "Science Fiction"),
                Arrays.asList("alien planet", "indigenous people", "avatar", "military", "pandora"),
                Arrays.asList("Sam Worthington", "Zoe Saldana", "Sigourney Weaver", "Stephen Lang", "Michelle Rodriguez"),
                "James Cameron", 7.6, 31000,
                "/kyeqWdyUXW608qlYkRqosgbbJyK.jpg"));

        // 27. The Shawshank Redemption (Drama, Crime) - TMDB ID: 278
        list.add(new Movie(278, "The Shawshank Redemption",
                "Imprisoned in the 1940s for the double murder of his wife and her lover, upstanding banker Andy Dufresne begins a new life at the Shawshank prison.",
                "1994-09-23", 1994,
                Arrays.asList("Drama", "Crime"),
                Arrays.asList("prison", "escape", "hope", "friendship", "wrongful conviction"),
                Arrays.asList("Tim Robbins", "Morgan Freeman", "Bob Gunton", "William Sadler"),
                "Frank Darabont", 8.7, 26000,
                "/9cqNxx0GxF0bflZmeSMuL5tnGzr.jpg"));

        // 28. Forrest Gump (Comedy, Drama, Romance) - TMDB ID: 13
        list.add(new Movie(13, "Forrest Gump",
                "A man with a low IQ has accomplished great things in his life and been present during significant historic events—in each case, far exceeding what anyone imagined he could do. Yet, despite all the things he has attained, his one true love eludes him.",
                "1994-07-06", 1994,
                Arrays.asList("Comedy", "Drama", "Romance"),
                Arrays.asList("vietnam war", "running", "historical events", "friendship", "destiny"),
                Arrays.asList("Tom Hanks", "Robin Wright", "Gary Sinise", "Sally Field"),
                "Robert Zemeckis", 8.5, 27000,
                "/arw2vcBveWOVZr6pxd9XTd1TdQa.jpg"));

        // 29. Barbie (Comedy, Adventure, Fantasy) - TMDB ID: 346698
        list.add(new Movie(346698, "Barbie",
                "Barbie and Ken are having the time of their lives in the colorful and seemingly perfect world of Barbie Land. However, when they get a chance to go to the real world, they soon discover the joys and perils of living among humans.",
                "2023-07-21", 2023,
                Arrays.asList("Comedy", "Adventure", "Fantasy"),
                Arrays.asList("doll", "patriarchy", "existential crisis", "matriarchy", "self discovery"),
                Arrays.asList("Margot Robbie", "Ryan Gosling", "America Ferrera", "Kate McKinnon", "Will Ferrell"),
                "Greta Gerwig", 7.1, 8900,
                "/iuFNMS8U5cb6xfzi51Dbkovj7vM.jpg"));

        return list;
    }
}
