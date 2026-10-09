package com.recommendation;

import com.recommendation.client.TmdbApiClient;
import com.recommendation.engine.ContentBasedMovieRecommender;
import com.recommendation.exception.MovieNotFoundException;
import com.recommendation.model.Movie;
import com.recommendation.model.MovieRecommendation;
import com.recommendation.model.SearchHistory;
import com.recommendation.repository.SearchHistoryJdbcRepository;
import com.recommendation.service.MovieRecommendationResult;
import com.recommendation.service.MovieRecommendationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive Automated JUnit 5 Test Suite for the AI-Based Movie Recommendation System.
 * Tests:
 * 1. Algorithm mathematical accuracy (Genre Jaccard, Overview Tokens, Cast/Director)
 * 2. Weighted scoring and ranking order
 * 3. Target movie exclusion
 * 4. Higher-rating preference on comparable similarity
 * 5. Edge cases & boundary safety (null/empty inputs, missing metadata)
 * 6. Custom Exception handling (MovieNotFoundException)
 * 7. Multithreaded Candidate Gathering
 * 8. JDBC Data Access & Transactional Search Logging
 */
@SpringBootTest
class MovieRecommendationTests {

    @Autowired
    private MovieRecommendationService recommendationService;

    @Autowired
    private SearchHistoryJdbcRepository jdbcRepository;

    private ContentBasedMovieRecommender recommender;
    private TmdbApiClient client;
    private List<Movie> catalog;

    @BeforeEach
    void setUp() {
        recommender = new ContentBasedMovieRecommender();
        client = new TmdbApiClient("");
        catalog = client.getFallbackDataset();
    }

    @Test
    @DisplayName("1. Spring Context & Injected Beans Verification")
    void testContextLoads() {
        assertNotNull(recommendationService, "MovieRecommendationService must be injected");
        assertNotNull(jdbcRepository, "SearchHistoryJdbcRepository must be injected");
    }

    @Test
    @DisplayName("2. Genuine Content-Based Recommendations for 'Inception'")
    void testRecommendationsForInception() {
        MovieRecommendationResult result = recommendationService.getRecommendationsForMovie("Inception", 10);

        assertNotNull(result);
        assertNotNull(result.getTargetMovie());
        assertEquals("Inception", result.getTargetMovie().getTitle());

        List<MovieRecommendation> recs = result.getRecommendations();
        assertFalse(recs.isEmpty(), "Should return recommendations for Inception");
        assertTrue(recs.size() <= 10, "Should respect requested Top-N limit of 10");

        // Top recommendations for Inception should be Sci-Fi / Thriller / Christopher Nolan movies
        // such as Interstellar, Tenet, The Prestige, The Matrix
        boolean hasSciFiOrNolan = recs.stream().anyMatch(r ->
                r.getMovie().getGenres().contains("Science Fiction") ||
                "Christopher Nolan".equalsIgnoreCase(r.getMovie().getDirector())
        );
        assertTrue(hasSciFiOrNolan, "Recommendations for Inception must contain Sci-Fi or director-related films");

        // Verify descending order of final composite scores
        for (int i = 0; i < recs.size() - 1; i++) {
            assertTrue(recs.get(i).getFinalScore() >= recs.get(i + 1).getFinalScore(),
                    "Recommendations must be sorted by FinalScore descending");
        }
    }

    @Test
    @DisplayName("3. Target Movie Strict Exclusion Verification")
    void testTargetMovieExclusion() {
        Movie target = client.searchMovie("Inception");
        List<MovieRecommendation> recs = recommender.recommendSimilar(target, catalog, 10);

        for (MovieRecommendation rec : recs) {
            assertNotEquals(target.getId(), rec.getMovie().getId(),
                    "Target movie ID must not appear in recommendations list");
            assertFalse(target.getTitle().equalsIgnoreCase(rec.getMovie().getTitle()),
                    "Target movie title must not appear in recommendations list");
        }
    }

    @Test
    @DisplayName("4. Higher-Rated Movies Favored When Content Similarity is Equal")
    void testHigherRatedMoviesFavoredOnTies() {
        Movie target = new Movie(1001, "Target Film", "Exciting space action", "2020", 2020,
                Arrays.asList("Action", "Sci-Fi"), null, null, "Director A", 7.0, 1000, "");

        Movie candidateLowerRated = new Movie(1002, "Candidate Low", "Exciting space action", "2021", 2021,
                Arrays.asList("Action", "Sci-Fi"), null, null, "Director A", 6.0, 1000, "");

        Movie candidateHigherRated = new Movie(1003, "Candidate High", "Exciting space action", "2021", 2021,
                Arrays.asList("Action", "Sci-Fi"), null, null, "Director A", 9.0, 1000, "");

        List<Movie> candidatePool = Arrays.asList(candidateLowerRated, candidateHigherRated);
        List<MovieRecommendation> recs = recommender.recommendSimilar(target, candidatePool, 2);

        assertEquals(2, recs.size());
        assertEquals("Candidate High", recs.get(0).getMovie().getTitle(),
                "Higher rated movie must be ranked #1 when content similarity is identical");
        assertEquals("Candidate Low", recs.get(1).getMovie().getTitle(),
                "Lower rated movie must be ranked #2");
        assertTrue(recs.get(0).getFinalScore() > recs.get(1).getFinalScore(),
                "Candidate with higher rating must achieve higher composite FinalScore");
    }

    @Test
    @DisplayName("5. Mathematical Genre Jaccard Similarity Calculation")
    void testGenreSimilarityJaccard() {
        Set<String> setA = new HashSet<>(Arrays.asList("action", "science fiction", "adventure"));
        Set<String> setB = new HashSet<>(Arrays.asList("action", "science fiction", "drama"));

        // Intersection = 2 (action, science fiction), Union = 4 (action, science fiction, adventure, drama)
        // Jaccard = 2 / 4 = 0.50
        double sim = recommender.calculateGenreSimilarity(setA, setB);
        assertEquals(0.50, sim, 0.001, "Jaccard similarity for 2/4 overlap must be exactly 0.50");

        // Completely disjoint sets -> 0.0
        Set<String> setC = new HashSet<>(Arrays.asList("romance", "comedy"));
        double disjointSim = recommender.calculateGenreSimilarity(setA, setC);
        assertEquals(0.0, disjointSim, 0.001, "Disjoint genre sets must yield 0.0 similarity");

        // Identical sets -> 1.0
        double identicalSim = recommender.calculateGenreSimilarity(setA, setA);
        assertEquals(1.0, identicalSim, 0.001, "Identical genre sets must yield 1.0 similarity");
    }

    @Test
    @DisplayName("6. Plot Overview & Keyword Token Similarity Calculation")
    void testOverviewTextSimilarity() {
        Movie movieA = new Movie(1, "Film A", "A thrilling space exploration through a mysterious wormhole.", "2020", 2020,
                Collections.emptyList(), Arrays.asList("space", "wormhole"), Collections.emptyList(), "", 8.0, 100, "");

        Movie movieB = new Movie(2, "Film B", "Astronauts on a daring space mission journey through a giant wormhole.", "2021", 2021,
                Collections.emptyList(), Arrays.asList("space", "wormhole"), Collections.emptyList(), "", 8.0, 100, "");

        double textSim = recommender.calculateOverviewSimilarity(movieA, movieB);
        assertTrue(textSim > 0.3, "Movies with shared space & wormhole keywords must have positive overview similarity");

        Movie movieUnrelated = new Movie(3, "Film C", "A high school teenager plans a wild comedy graduation party.", "2022", 2022,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), "", 7.0, 100, "");

        double unrelatedSim = recommender.calculateOverviewSimilarity(movieA, movieUnrelated);
        assertEquals(0.0, unrelatedSim, 0.05, "Unrelated movies should yield negligible overview similarity");
    }

    @Test
    @DisplayName("7. Cast & Director Continuity Similarity Calculation")
    void testCastDirectorSimilarity() {
        Movie nolanFilmA = new Movie(1, "Film A", "", "2010", 2010,
                Collections.emptyList(), Collections.emptyList(), Arrays.asList("Christian Bale", "Michael Caine"),
                "Christopher Nolan", 8.0, 100, "");

        Movie nolanFilmB = new Movie(2, "Film B", "", "2012", 2012,
                Collections.emptyList(), Collections.emptyList(), Arrays.asList("Christian Bale", "Michael Caine", "Tom Hardy"),
                "Christopher Nolan", 8.0, 100, "");

        double castDirSim = recommender.calculateCastDirectorSimilarity(nolanFilmA, nolanFilmB);
        // Shared Director (0.60) + Shared Cast (0.40) = 1.00
        assertTrue(castDirSim >= 0.90, "Same director and lead actors must achieve high cast/director similarity");
    }

    @Test
    @DisplayName("8. Custom Exception on Invalid / Non-Existent Movie Name")
    void testInvalidMovieNameThrowsException() {
        assertThrows(MovieNotFoundException.class, () -> {
            recommendationService.getRecommendationsForMovie("ThisMovieDoesNotExistAtAll_XYZ_9999", 5);
        }, "Searching for a non-existent movie title must throw MovieNotFoundException");

        assertThrows(MovieNotFoundException.class, () -> {
            recommendationService.getRecommendationsForMovie("", 5);
        }, "Searching for an empty title must throw MovieNotFoundException");
    }

    @Test
    @DisplayName("9. Robust Edge Case & Null Input Safety")
    void testEdgeCasesAndNullHandling() {
        // Null inputs
        List<MovieRecommendation> nullRecs = recommender.recommendSimilar(null, catalog, 5);
        assertTrue(nullRecs.isEmpty(), "Null target movie must return empty list without crash");

        List<MovieRecommendation> emptyPoolRecs = recommender.recommendSimilar(catalog.get(0), Collections.emptyList(), 5);
        assertTrue(emptyPoolRecs.isEmpty(), "Empty candidate pool must return empty list");

        List<MovieRecommendation> zeroNRecs = recommender.recommendSimilar(catalog.get(0), catalog, 0);
        assertTrue(zeroNRecs.isEmpty(), "TopN <= 0 must safely return empty list");
    }

    @Test
    @DisplayName("10. JDBC Transactional Search History Logging")
    void testJdbcTransactionLogging() {
        Movie target = client.searchMovie("The Dark Knight");
        List<MovieRecommendation> recs = recommender.recommendSimilar(target, catalog, 5);

        long initialCount = jdbcRepository.countSearches();

        // Perform transactional save
        long generatedId = jdbcRepository.saveSearchWithRecommendations("The Dark Knight", target, recs);
        assertTrue(generatedId > 0, "JDBC Transaction should return positive generated history ID");

        long newCount = jdbcRepository.countSearches();
        assertTrue(newCount > initialCount, "Search history count should increase after transaction commit");

        List<SearchHistory> recent = jdbcRepository.getRecentSearches(20);
        assertFalse(recent.isEmpty());
        SearchHistory latest = recent.get(0);
        assertEquals("The Dark Knight", latest.getTargetMovieTitle());
        assertEquals(recs.size(), latest.getResultCount());
    }

    @Test
    @DisplayName("11. Test Latest Movies Categories, Regional Filtering, Pagination & Caching")
    void testLatestMoviesCategoriesAndPagination() {
        // 1. Test Now Playing Category
        com.recommendation.model.MoviePageResult nowPlaying = client.fetchMoviesByCategory("now_playing", 1, "");
        assertNotNull(nowPlaying, "Now playing result must not be null");
        assertFalse(nowPlaying.getMovies().isEmpty(), "Now playing should return movies");
        assertEquals(1, nowPlaying.getPage());
        assertTrue(nowPlaying.getTotalPages() >= 1);
        assertEquals("now_playing", nowPlaying.getCategory());

        // 2. Test Upcoming Category with Region=IN
        com.recommendation.model.MoviePageResult upcomingIN = client.fetchMoviesByCategory("upcoming", 1, "IN");
        assertNotNull(upcomingIN);
        assertFalse(upcomingIN.getMovies().isEmpty());
        assertEquals("upcoming", upcomingIN.getCategory());
        assertEquals("IN", upcomingIN.getRegion());

        // 3. Test Trending Category
        com.recommendation.model.MoviePageResult trending = client.fetchMoviesByCategory("trending", 1, "");
        assertNotNull(trending);
        assertFalse(trending.getMovies().isEmpty());
        assertEquals("trending", trending.getCategory());

        // 4. Test Recently Released (Discover) Category
        com.recommendation.model.MoviePageResult discover = client.fetchMoviesByCategory("recently_released", 1, "IN");
        assertNotNull(discover);
        assertFalse(discover.getMovies().isEmpty());
        assertEquals("recently_released", discover.getCategory());

        // 5. Test In-Memory Caching (Second request must hit cache)
        com.recommendation.model.MoviePageResult cachedResult = client.fetchMoviesByCategory("trending", 1, "");
        assertNotNull(cachedResult);
        assertTrue(cachedResult.isFromCache(), "Second call within TTL should be served from in-memory cache");

        // 6. Test Movie Attributes & Genre Resolution
        Movie sampleMovie = nowPlaying.getMovies().get(0);
        assertTrue(sampleMovie.getTmdbId() > 0, "Movie must have valid TMDB ID");
        assertNotNull(sampleMovie.getTitle(), "Movie must have title");
        assertNotNull(sampleMovie.getPosterUrl(), "Movie must have poster URL");
        assertNotNull(sampleMovie.getGenres(), "Movie should have parsed genres list");

        // 7. Test Pagination navigation helper methods
        assertTrue(nowPlaying.getPage() >= 1);
        assertFalse(nowPlaying.hasPrevious(), "Page 1 should not have previous page");
    }

    @Test
    @DisplayName("12. Multi-Language Support, Display Names & Language Filtering")
    void testMultiLanguageSupportAndFiltering() {
        // 1. Test Movie Language Display Name mappings
        Movie hindiMovie = new Movie(101, "Sholay", "Classic Hindi Action", "1975", 1975,
                List.of("Action"), List.of(), List.of(), "Ramesh Sippy", 8.2, 500, "", "hi");
        assertEquals("hi", hindiMovie.getOriginalLanguage());
        assertEquals("Hindi", hindiMovie.getLanguageDisplayName());

        Movie tamilMovie = new Movie(102, "Vikram", "Action Thriller", "2022", 2022,
                List.of("Action"), List.of(), List.of(), "Lokesh Kanagaraj", 8.3, 800, "", "ta");
        assertEquals("ta", tamilMovie.getOriginalLanguage());
        assertEquals("Tamil", tamilMovie.getLanguageDisplayName());

        Movie teluguMovie = new Movie(103, "RRR", "Action Drama", "2022", 2022,
                List.of("Action"), List.of(), List.of(), "S.S. Rajamouli", 8.5, 2000, "", "te");
        assertEquals("te", teluguMovie.getOriginalLanguage());
        assertEquals("Telugu", teluguMovie.getLanguageDisplayName());

        Movie koreanMovie = new Movie(104, "Parasite", "Drama Thriller", "2019", 2019,
                List.of("Drama"), List.of(), List.of(), "Bong Joon-ho", 8.5, 15000, "", "ko");
        assertEquals("ko", koreanMovie.getOriginalLanguage());
        assertEquals("Korean", koreanMovie.getLanguageDisplayName());

        Movie japaneseMovie = new Movie(105, "Spirited Away", "Anime Fantasy", "2001", 2001,
                List.of("Animation"), List.of(), List.of(), "Hayao Miyazaki", 8.5, 16000, "", "ja");
        assertEquals("ja", japaneseMovie.getOriginalLanguage());
        assertEquals("Japanese", japaneseMovie.getLanguageDisplayName());

        Movie englishMovie = new Movie(106, "Inception", "Sci-Fi Thriller", "2010", 2010,
                List.of("Sci-Fi"), List.of(), List.of(), "Christopher Nolan", 8.4, 35000, "", "en");
        assertEquals("en", englishMovie.getOriginalLanguage());
        assertEquals("English", englishMovie.getLanguageDisplayName());

        // 2. Test Multi-Language Category Fetching via Client and Service
        com.recommendation.model.MoviePageResult allLangResult = client.fetchMoviesByCategory("now_playing", 1, "", "");
        assertNotNull(allLangResult);
        assertNotNull(allLangResult.getMovies());
        assertEquals("", allLangResult.getLanguage(), "Default language must be empty (All Languages)");

        // 3. Test Language-Filtered Fetching
        com.recommendation.model.MoviePageResult hindiResult = client.fetchMoviesByCategory("trending", 1, "", "hi");
        assertNotNull(hindiResult);
        assertEquals("hi", hindiResult.getLanguage());

        // 4. Test Service Layer getLatestMovies with language parameter
        com.recommendation.model.MoviePageResult serviceResult = recommendationService.getLatestMovies("recently_released", 1, "IN", "hi");
        assertNotNull(serviceResult);
        assertEquals("hi", serviceResult.getLanguage());
        assertEquals("IN", serviceResult.getRegion());
    }
}
