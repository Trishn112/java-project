package com.recommendation.controller;

import com.recommendation.exception.MovieNotFoundException;
import com.recommendation.exception.TmdbApiException;
import com.recommendation.model.Movie;
import com.recommendation.model.MoviePageResult;
import com.recommendation.model.SearchHistory;
import com.recommendation.service.MovieRecommendationResult;
import com.recommendation.service.MovieRecommendationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Spring MVC Web Controller providing a clean, single-page interface
 * for the AI-Based Movie Recommendation System and TMDB Discovery Feeds.
 * Demonstrates MVC ARCHITECTURE, EXCEPTION HANDLING, and REST API CONSUMPTION.
 */
@Controller
public class MovieWebController {

    private final MovieRecommendationService recommendationService;

    public MovieWebController(MovieRecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    /**
     * Single-page movie recommendation dashboard, latest movies feed, language filtering,
     * movie candidate matching, and recommendation handler.
     *
     * @param movieName Movie title search term (optional)
     * @param tmdbId    Specific TMDB ID selected by user (optional)
     * @param category  Latest category ("now_playing", "popular", "upcoming", "trending", "recently_released")
     * @param page      1-based page number for pagination
     * @param region    Regional filter code (e.g. "IN" for India, or empty for Global)
     * @param lang      Original language filter code (e.g. "hi", "ta", "te", "kn", "ml", "ko", "ja", "en", or empty for All)
     * @param model     Spring MVC Model
     * @return HTML template name ("index")
     */
    @GetMapping({"/", "/search", "/latest", "/recommend"})
    public String index(@RequestParam(value = "movieName", required = false) String movieName,
                        @RequestParam(value = "tmdbId", required = false) Integer tmdbId,
                        @RequestParam(value = "category", defaultValue = "now_playing") String category,
                        @RequestParam(value = "page", defaultValue = "1") int page,
                        @RequestParam(value = "region", defaultValue = "") String region,
                        @RequestParam(value = "lang", defaultValue = "") String lang,
                        Model model) {

        // Retrieve recent searches from JDBC database
        try {
            List<SearchHistory> recentSearches = recommendationService.getRecentSearches(6);
            model.addAttribute("recentSearches", recentSearches);
        } catch (Exception e) {
            model.addAttribute("recentSearches", List.of());
        }

        model.addAttribute("isLiveApiActive", recommendationService.getTmdbApiClient().isApiKeyConfigured());

        // Retrieve paginated latest movies from TMDB API / Discover Feeds
        String activeCategory = (category != null && !category.trim().isEmpty()) ? category.trim().toLowerCase() : "now_playing";
        int activePage = Math.max(page, 1);
        String activeRegion = (region != null) ? region.trim().toUpperCase() : "";
        String activeLang = (lang != null && !lang.trim().equalsIgnoreCase("all")) ? lang.trim().toLowerCase() : "";

        MoviePageResult latestMovies = recommendationService.getLatestMovies(activeCategory, activePage, activeRegion, activeLang);
        model.addAttribute("latestMovies", latestMovies);
        model.addAttribute("selectedCategory", activeCategory);
        model.addAttribute("selectedRegion", activeRegion);
        model.addAttribute("selectedLanguage", activeLang);
        model.addAttribute("currentPage", activePage);

        // Case 1: Specific TMDB Movie ID clicked/selected by user
        if (tmdbId != null && tmdbId > 0) {
            model.addAttribute("hasSearched", true);
            try {
                MovieRecommendationResult result = recommendationService.getRecommendationsByTmdbId(tmdbId, 10);
                model.addAttribute("targetMovie", result.getTargetMovie());
                model.addAttribute("searchedMovieName", result.getTargetMovie().getTitle());
                model.addAttribute("recommendations", result.getRecommendations());
                model.addAttribute("dataSource", result.getDataSource());
                model.addAttribute("executionTimeMs", result.getExecutionTimeMs());
                model.addAttribute("resultCount", result.getResultCount());

                // If movieName query is present, keep the candidate list visible for context
                if (movieName != null && !movieName.trim().isEmpty()) {
                    List<Movie> matches = recommendationService.searchMovies(movieName.trim());
                    model.addAttribute("matchingMovies", matches);
                }
                return "index";
            } catch (MovieNotFoundException e) {
                model.addAttribute("errorMessage", e.getMessage());
                model.addAttribute("recommendations", List.of());
                return "index";
            } catch (Exception e) {
                model.addAttribute("errorMessage", "Error generating recommendations: " + e.getMessage());
                model.addAttribute("recommendations", List.of());
                return "index";
            }
        }

        // Case 2: Landing page without search query
        if (movieName == null || movieName.trim().isEmpty()) {
            model.addAttribute("hasSearched", false);
            return "index";
        }

        // Case 3: User submitted movie title search
        String cleanMovieName = movieName.trim();
        model.addAttribute("searchedMovieName", cleanMovieName);
        model.addAttribute("hasSearched", true);

        try {
            // Find all matching candidates from TMDB
            List<Movie> matchingCandidates = recommendationService.searchMovies(cleanMovieName);
            if (matchingCandidates.isEmpty()) {
                throw new MovieNotFoundException(cleanMovieName, "No movies found matching \"" + cleanMovieName + "\". Please check the spelling or try another title.");
            }

            model.addAttribute("matchingMovies", matchingCandidates);

            // Compute AI recommendations for the top matched candidate
            Movie topCandidate = matchingCandidates.get(0);
            MovieRecommendationResult result = (topCandidate.getTmdbId() > 0)
                    ? recommendationService.getRecommendationsByTmdbId(topCandidate.getTmdbId(), 10)
                    : recommendationService.getRecommendationsForMovie(cleanMovieName, 10);

            model.addAttribute("targetMovie", result.getTargetMovie());
            model.addAttribute("recommendations", result.getRecommendations());
            model.addAttribute("dataSource", result.getDataSource());
            model.addAttribute("executionTimeMs", result.getExecutionTimeMs());
            model.addAttribute("resultCount", result.getResultCount());

        } catch (MovieNotFoundException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("recommendations", List.of());
            model.addAttribute("matchingMovies", List.of());
        } catch (TmdbApiException e) {
            model.addAttribute("errorMessage", "TMDB API Error: " + e.getMessage() + ". Please check API credentials or network connection.");
            model.addAttribute("recommendations", List.of());
        } catch (Exception e) {
            model.addAttribute("errorMessage", "An unexpected error occurred while processing movie search: " + e.getMessage());
            model.addAttribute("recommendations", List.of());
        }

        return "index";
    }
}
