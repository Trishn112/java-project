package com.recommendation.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a Movie entity stored in MySQL and populated with TMDB metadata.
 * Demonstrates ENCAPSULATION, GENERICS, and COLLECTIONS (List, Set).
 */
public class Movie {

    private int movieId;          // Database Primary Key (movie_id)
    private int tmdbId;           // Unique TMDB External ID (tmdb_id)
    private String title;
    private String overview;
    private String releaseDate;
    private int releaseYear;
    private List<String> genres;
    private Set<String> genreSet;
    private List<String> keywords;
    private List<String> topCast;
    private String director;
    private double voteAverage;   // Mapped to decimal rating in DB
    private int voteCount;
    private String posterPath;
    private String posterUrl;
    private String originalLanguage;
    private String originalTitle;

    public static final String TMDB_POSTER_BASE_URL = "https://image.tmdb.org/t/p/w500";
    public static final String DEFAULT_POSTER_URL = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=500&q=80";

    public Movie() {
        this.genres = new ArrayList<>();
        this.genreSet = new HashSet<>();
        this.keywords = new ArrayList<>();
        this.topCast = new ArrayList<>();
        this.originalLanguage = "";
        this.originalTitle = "";
    }

    public Movie(int tmdbId, String title, String overview, String releaseDate, int releaseYear,
                 List<String> genres, List<String> keywords, List<String> topCast,
                 String director, double voteAverage, int voteCount, String posterPath) {
        this(tmdbId, title, overview, releaseDate, releaseYear, genres, keywords, topCast, director, voteAverage, voteCount, posterPath, "");
    }

    public Movie(int tmdbId, String title, String overview, String releaseDate, int releaseYear,
                 List<String> genres, List<String> keywords, List<String> topCast,
                 String director, double voteAverage, int voteCount, String posterPath, String originalLanguage) {
        this.tmdbId = tmdbId;
        this.movieId = 0; // Assigned upon database persistence
        this.title = title;
        this.overview = overview != null ? overview : "";
        this.releaseDate = releaseDate != null ? releaseDate : "";
        this.releaseYear = releaseYear;
        setGenres(genres);
        setKeywords(keywords);
        setTopCast(topCast);
        this.director = director != null ? director : "";
        this.voteAverage = voteAverage;
        this.voteCount = voteCount;
        this.posterPath = posterPath;
        this.posterUrl = computePosterUrl(posterPath);
        this.originalLanguage = originalLanguage != null ? originalLanguage : "";
    }

    public Movie(int movieId, int tmdbId, String title, String overview, String releaseDate, int releaseYear,
                 List<String> genres, List<String> keywords, List<String> topCast,
                 String director, double voteAverage, int voteCount, String posterPath) {
        this(tmdbId, title, overview, releaseDate, releaseYear, genres, keywords, topCast, director, voteAverage, voteCount, posterPath, "");
        this.movieId = movieId;
    }

    public Movie(int movieId, int tmdbId, String title, String overview, String releaseDate, int releaseYear,
                 List<String> genres, List<String> keywords, List<String> topCast,
                 String director, double voteAverage, int voteCount, String posterPath, String originalLanguage) {
        this(tmdbId, title, overview, releaseDate, releaseYear, genres, keywords, topCast, director, voteAverage, voteCount, posterPath, originalLanguage);
        this.movieId = movieId;
    }

    private String computePosterUrl(String path) {
        if (path == null || path.trim().isEmpty()) {
            return DEFAULT_POSTER_URL;
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        return TMDB_POSTER_BASE_URL + (path.startsWith("/") ? path : "/" + path);
    }

    // Getters and Setters demonstrating Encapsulation

    public int getMovieId() {
        return movieId;
    }

    public void setMovieId(int movieId) {
        this.movieId = movieId;
    }

    public int getTmdbId() {
        return tmdbId;
    }

    public void setTmdbId(int tmdbId) {
        this.tmdbId = tmdbId;
    }

    /**
     * Backward-compatible getter for ID. Returns tmdbId if set, otherwise movieId.
     */
    public int getId() {
        return tmdbId > 0 ? tmdbId : movieId;
    }

    public void setId(int id) {
        this.tmdbId = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
        if (releaseDate != null && releaseDate.length() >= 4) {
            try {
                this.releaseYear = Integer.parseInt(releaseDate.substring(0, 4));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    public int getReleaseYear() {
        return releaseYear;
    }

    public void setReleaseYear(int releaseYear) {
        this.releaseYear = releaseYear;
    }

    public List<String> getGenres() {
        return new ArrayList<>(genres);
    }

    public void setGenres(List<String> genres) {
        this.genres = genres != null ? new ArrayList<>(genres) : new ArrayList<>();
        this.genreSet = new HashSet<>();
        for (String g : this.genres) {
            if (g != null && !g.trim().isEmpty()) {
                this.genreSet.add(g.trim().toLowerCase());
            }
        }
    }

    public void setGenreString(String genreStr) {
        if (genreStr == null || genreStr.trim().isEmpty()) {
            setGenres(Collections.emptyList());
            return;
        }
        String[] parts = genreStr.split("[,•|]");
        List<String> list = new ArrayList<>();
        for (String p : parts) {
            if (!p.trim().isEmpty()) {
                list.add(p.trim());
            }
        }
        setGenres(list);
    }

    public Set<String> getGenreSet() {
        return Collections.unmodifiableSet(genreSet);
    }

    public List<String> getKeywords() {
        return new ArrayList<>(keywords);
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords != null ? new ArrayList<>(keywords) : new ArrayList<>();
    }

    public List<String> getTopCast() {
        return new ArrayList<>(topCast);
    }

    public void setTopCast(List<String> topCast) {
        this.topCast = topCast != null ? new ArrayList<>(topCast) : new ArrayList<>();
    }

    public String getDirector() {
        return director;
    }

    public void setDirector(String director) {
        this.director = director;
    }

    public double getVoteAverage() {
        return voteAverage;
    }

    public void setVoteAverage(double voteAverage) {
        this.voteAverage = voteAverage;
    }

    public double getRating() {
        return voteAverage;
    }

    public void setRating(double rating) {
        this.voteAverage = rating;
    }

    public int getVoteCount() {
        return voteCount;
    }

    public void setVoteCount(int voteCount) {
        this.voteCount = voteCount;
    }

    public String getPosterPath() {
        return posterPath;
    }

    public void setPosterPath(String posterPath) {
        this.posterPath = posterPath;
        this.posterUrl = computePosterUrl(posterPath);
    }

    public String getPosterUrl() {
        return posterUrl != null ? posterUrl : DEFAULT_POSTER_URL;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public String getGenreString() {
        if (genres == null || genres.isEmpty()) {
            return "General";
        }
        return String.join(" • ", genres);
    }

    public String getRatingFormatted() {
        return String.format("%.1f / 10", voteAverage);
    }

    public String getCastString() {
        if (topCast == null || topCast.isEmpty()) {
            return "Cast not listed";
        }
        return String.join(", ", topCast);
    }

    public String getOriginalTitle() {
        return originalTitle != null ? originalTitle : "";
    }

    public void setOriginalTitle(String originalTitle) {
        this.originalTitle = originalTitle != null ? originalTitle : "";
    }

    public boolean hasDistinctOriginalTitle() {
        return originalTitle != null && !originalTitle.trim().isEmpty() &&
                !originalTitle.trim().equalsIgnoreCase(title != null ? title.trim() : "");
    }

    public String getOriginalLanguage() {
        return originalLanguage != null ? originalLanguage : "";
    }

    public void setOriginalLanguage(String originalLanguage) {
        this.originalLanguage = originalLanguage != null ? originalLanguage : "";
    }

    public String getLanguageDisplayName() {
        if (originalLanguage == null || originalLanguage.trim().isEmpty()) {
            return "English";
        }
        String code = originalLanguage.trim().toLowerCase();
        switch (code) {
            case "en": return "English";
            case "hi": return "Hindi";
            case "ta": return "Tamil";
            case "te": return "Telugu";
            case "kn": return "Kannada";
            case "ml": return "Malayalam";
            case "ko": return "Korean";
            case "ja": return "Japanese";
            case "es": return "Spanish";
            case "fr": return "French";
            case "de": return "German";
            case "zh": case "cn": return "Chinese";
            case "it": return "Italian";
            case "pt": return "Portuguese";
            case "ru": return "Russian";
            case "ar": return "Arabic";
            case "bn": return "Bengali";
            case "mr": return "Marathi";
            case "pa": return "Punjabi";
            case "tr": return "Turkish";
            default:
                try {
                    String display = java.util.Locale.forLanguageTag(code).getDisplayLanguage(java.util.Locale.ENGLISH);
                    if (display != null && !display.isEmpty()) {
                        return display;
                    }
                } catch (Exception ignored) {
                }
                return code.toUpperCase();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Movie movie = (Movie) o;
        if (tmdbId > 0 && movie.tmdbId > 0) {
            return tmdbId == movie.tmdbId;
        }
        if (movieId > 0 && movie.movieId > 0) {
            return movieId == movie.movieId;
        }
        return Objects.equals(title != null ? title.toLowerCase() : null, movie.title != null ? movie.title.toLowerCase() : null) &&
                releaseYear == movie.releaseYear;
    }

    @Override
    public int hashCode() {
        return tmdbId > 0 ? Objects.hash(tmdbId) : Objects.hash(title != null ? title.toLowerCase() : "", releaseYear);
    }

    @Override
    public String toString() {
        return "Movie{" +
                "movieId=" + movieId +
                ", tmdbId=" + tmdbId +
                ", title='" + title + '\'' +
                ", year=" + releaseYear +
                ", genres=" + genres +
                ", rating=" + voteAverage +
                '}';
    }
}
