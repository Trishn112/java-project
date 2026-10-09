package com.recommendation;

import com.recommendation.client.TmdbApiClient;
import com.recommendation.engine.ContentBasedMovieRecommender;
import com.recommendation.model.Movie;
import com.recommendation.model.MovieRecommendation;

import java.util.List;

/**
 * Console Demonstration class for the AI-Based Movie Recommendation System.
 * Demonstrates:
 * 1. Content-Based Recommendation Algorithm in Pure Java
 * 2. Genre Jaccard Similarity (50% weight)
 * 3. Plot Overview & Keyword Token Similarity (30% weight)
 * 4. Cast & Director Continuity Similarity (20% weight)
 * 5. Composite Normalized Rating weighting: FinalScore = (0.70 × ContentSim) + (0.30 × NormalizedRating)
 * 6. Target Movie Strict Exclusion
 * 7. Java OOP Principles: Encapsulation, Inheritance, Abstraction, Polymorphism, Collections & Generics.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("   AI-BASED MOVIE RECOMMENDATION SYSTEM - ACADEMIC CONSOLE DEMONSTRATION        ");
        System.out.println("================================================================================\n");

        TmdbApiClient client = new TmdbApiClient("");
        ContentBasedMovieRecommender recommender = new ContentBasedMovieRecommender();
        List<Movie> catalog = client.getFallbackDataset();

        System.out.println("Loaded Catalog: " + catalog.size() + " real movies with TMDB metadata.\n");

        // Test 1: Inception (Sci-Fi / Action / Thriller - Directed by Christopher Nolan)
        String searchTarget = "Inception";
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println("TEST 1: SEARCHING FOR SIMILAR MOVIES TO: '" + searchTarget + "'");
        System.out.println("--------------------------------------------------------------------------------");

        Movie targetMovie = client.searchMovie(searchTarget);
        System.out.println("Found Reference Movie:");
        System.out.println("  - Title       : " + targetMovie.getTitle() + " (" + targetMovie.getReleaseYear() + ")");
        System.out.println("  - Genres      : " + targetMovie.getGenreString());
        System.out.println("  - Director    : " + targetMovie.getDirector());
        System.out.println("  - Top Cast    : " + targetMovie.getCastString());
        System.out.println("  - TMDB Rating : " + targetMovie.getRatingFormatted() + "\n");

        List<MovieRecommendation> recommendations = recommender.recommendSimilar(targetMovie, catalog, 5);

        System.out.println("TOP 5 SIMILAR RECOMMENDATIONS (Calculated by Java Algorithm):");
        displayRecommendations(recommendations);

        // Verification of Target Movie Exclusion
        boolean containsSelf = recommendations.stream().anyMatch(r -> r.getMovie().getId() == targetMovie.getId());
        System.out.println("Verification Check: Target Movie 'Inception' Excluded from Results? " + (!containsSelf ? "PASSED (Yes)" : "FAILED (No)"));
        System.out.println();

        // Test 2: The Godfather (Drama / Crime - Directed by Francis Ford Coppola)
        String crimeTarget = "The Godfather";
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println("TEST 2: SEARCHING FOR SIMILAR MOVIES TO: '" + crimeTarget + "'");
        System.out.println("--------------------------------------------------------------------------------");

        Movie godfather = client.searchMovie(crimeTarget);
        List<MovieRecommendation> crimeRecs = recommender.recommendSimilar(godfather, catalog, 5);

        System.out.println("TOP 5 SIMILAR RECOMMENDATIONS FOR '" + godfather.getTitle() + "':");
        displayRecommendations(crimeRecs);

        System.out.println("================================================================================");
        System.out.println("               RECOMMENDATION DEMONSTRATION COMPLETED                           ");
        System.out.println("================================================================================");
    }

    private static void displayRecommendations(List<MovieRecommendation> recs) {
        int rank = 1;
        for (MovieRecommendation rec : recs) {
            Movie m = rec.getMovie();
            System.out.printf("#%d %-28s (%d) | TMDB: %-9s | Similarity: %-5s | Final Score: %.3f%n",
                    rank++,
                    m.getTitle(),
                    m.getReleaseYear(),
                    m.getRatingFormatted(),
                    rec.getSimilarityPercentageFormatted(),
                    rec.getFinalScore()
            );
            System.out.printf("   [Breakdown: Genre=%.0f%% | Overview=%.0f%% | Cast/Dir=%.0f%% | NormRating=%.2f]%n",
                    rec.getGenreSimilarity() * 100.0,
                    rec.getOverviewSimilarity() * 100.0,
                    rec.getCastDirectorSimilarity() * 100.0,
                    rec.getNormalizedRating()
            );
        }
        System.out.println();
    }
}
