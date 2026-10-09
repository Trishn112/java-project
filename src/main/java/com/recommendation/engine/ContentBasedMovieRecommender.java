package com.recommendation.engine;

import com.recommendation.model.Movie;
import com.recommendation.model.MovieRecommendation;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Concrete Content-Based Movie Recommendation Engine.
 *
 * Mathematical Algorithm & Scoring Architecture:
 * ------------------------------------------------------------------------------------------------
 * 1. Genre Similarity (Weight: 50% of Content Similarity):
 *    Calculated using Jaccard Similarity on Genre Sets:
 *    GenreSim(A, B) = |Genres_A ∩ Genres_B| / |Genres_A ∪ Genres_B|
 *
 * 2. Plot Overview & Keyword Similarity (Weight: 30% of Content Similarity):
 *    Tokenizes text overviews and keywords (removing stop-words) and computes Overlap Coefficient:
 *    TextSim(A, B) = |Tokens_A ∩ Tokens_B| / sqrt(|Tokens_A| × |Tokens_B|)
 *
 * 3. Cast & Director Similarity (Weight: 20% of Content Similarity):
 *    Director Match Bonus (0.60) + Cast Intersection Ratio (0.40):
 *    CastDirectorSim(A, B) = (DirectorMatch ? 0.60 : 0.0) + (0.40 × |Cast_A ∩ Cast_B| / min(|Cast_A|, |Cast_B|))
 *
 * 4. Composite Content Similarity:
 *    ContentSim(A, B) = (0.50 × GenreSim) + (0.30 × TextSim) + (0.20 × CastDirectorSim)
 *    (Weights dynamically rebalance if cast or overview metadata is unavailable).
 *
 * 5. Normalized Quality Rating:
 *    NormalizedRating(B) = voteAverage / 10.0
 *
 * 6. Final Recommendation Score:
 *    FinalScore = (0.70 × ContentSim) + (0.30 × NormalizedRating)
 *
 * 7. Candidate Ranking & Target Exclusion:
 *    - The searched movie A is strictly excluded from recommendations.
 *    - All candidates are sorted in descending order of FinalScore using Comparator.
 *    - Top N recommendations are returned (default: 10).
 * ------------------------------------------------------------------------------------------------
 */
@Component
public class ContentBasedMovieRecommender extends AbstractMovieRecommender {

    // Final Composite Scoring Weights (Sum = 1.00 / 100%)
    public static final double CONTENT_SIMILARITY_WEIGHT = 0.70; // 70% Content Match
    public static final double RATING_WEIGHT             = 0.30; // 30% Quality Rating

    // Content Feature Default Sub-Weights
    public static final double GENRE_WEIGHT              = 0.50; // 50% Genre affinity
    public static final double OVERVIEW_WEIGHT           = 0.30; // 30% Plot / Keyword semantics
    public static final double CAST_DIRECTOR_WEIGHT      = 0.20; // 20% Cast / Director continuity

    public ContentBasedMovieRecommender() {
        super();
    }

    /**
     * Calculates the Jaccard Similarity between two sets of genres.
     * Demonstrates COLLECTIONS (Set, HashSet) and Generics.
     *
     * @param genresA Genres of target movie
     * @param genresB Genres of candidate movie
     * @return Jaccard similarity score between 0.0 and 1.0
     */
    public double calculateGenreSimilarity(Set<String> genresA, Set<String> genresB) {
        if (genresA == null || genresB == null || genresA.isEmpty() || genresB.isEmpty()) {
            return 0.0;
        }

        Set<String> intersection = new HashSet<>(genresA);
        intersection.retainAll(genresB);

        Set<String> union = new HashSet<>(genresA);
        union.addAll(genresB);

        if (union.isEmpty()) {
            return 0.0;
        }

        return clampScore((double) intersection.size() / union.size());
    }

    /**
     * Calculates textual semantic similarity across plot overviews and keywords.
     *
     * @param targetMovie    Target reference movie
     * @param candidateMovie Candidate movie
     * @return Semantic text similarity score between 0.0 and 1.0
     */
    public double calculateOverviewSimilarity(Movie targetMovie, Movie candidateMovie) {
        if (targetMovie == null || candidateMovie == null) {
            return 0.0;
        }

        StringBuilder textA = new StringBuilder();
        if (targetMovie.getOverview() != null) textA.append(targetMovie.getOverview()).append(" ");
        if (targetMovie.getKeywords() != null) textA.append(String.join(" ", targetMovie.getKeywords()));

        StringBuilder textB = new StringBuilder();
        if (candidateMovie.getOverview() != null) textB.append(candidateMovie.getOverview()).append(" ");
        if (candidateMovie.getKeywords() != null) textB.append(String.join(" ", candidateMovie.getKeywords()));

        Set<String> tokensA = extractTokens(textA.toString());
        Set<String> tokensB = extractTokens(textB.toString());

        if (tokensA.isEmpty() || tokensB.isEmpty()) {
            return 0.0;
        }

        Set<String> intersection = new HashSet<>(tokensA);
        intersection.retainAll(tokensB);

        if (intersection.isEmpty()) {
            return 0.0;
        }

        double denominator = Math.sqrt((double) tokensA.size() * tokensB.size());
        if (denominator == 0.0) {
            return 0.0;
        }

        return clampScore(intersection.size() / denominator);
    }

    /**
     * Calculates similarity based on shared director and lead cast members.
     *
     * @param targetMovie    Target reference movie
     * @param candidateMovie Candidate movie
     * @return Cast and director similarity score between 0.0 and 1.0
     */
    public double calculateCastDirectorSimilarity(Movie targetMovie, Movie candidateMovie) {
        if (targetMovie == null || candidateMovie == null) {
            return 0.0;
        }

        double score = 0.0;

        // Director Match (60% of cast/director sub-score)
        String dirA = targetMovie.getDirector();
        String dirB = candidateMovie.getDirector();
        if (dirA != null && dirB != null && !dirA.trim().isEmpty() && !dirB.trim().isEmpty()) {
            if (dirA.trim().equalsIgnoreCase(dirB.trim())) {
                score += 0.60;
            }
        }

        // Top Cast Overlap (40% of cast/director sub-score)
        List<String> castA = targetMovie.getTopCast();
        List<String> castB = candidateMovie.getTopCast();
        if (castA != null && castB != null && !castA.isEmpty() && !castB.isEmpty()) {
            Set<String> setA = new HashSet<>();
            for (String actor : castA) if (actor != null) setA.add(actor.trim().toLowerCase());

            Set<String> setB = new HashSet<>();
            for (String actor : castB) if (actor != null) setB.add(actor.trim().toLowerCase());

            Set<String> commonActors = new HashSet<>(setA);
            commonActors.retainAll(setB);

            if (!commonActors.isEmpty()) {
                double castRatio = (double) commonActors.size() / Math.min(setA.size(), setB.size());
                score += (0.40 * Math.min(castRatio, 1.0));
            }
        }

        return clampScore(score);
    }

    /**
     * Computes the normalized content similarity score by weighting genre, overview, and cast/director.
     * Dynamically adapts weights if certain metadata fields are missing.
     *
     * @param genreSim        Genre similarity [0.0 - 1.0]
     * @param overviewSim     Overview/keyword similarity [0.0 - 1.0]
     * @param castDirectorSim Cast/director similarity [0.0 - 1.0]
     * @param hasOverview     Flag indicating if overview data was available
     * @param hasCastDirector Flag indicating if cast/director data was available
     * @return Normalized content similarity score [0.0 - 1.0]
     */
    public double calculateContentSimilarity(double genreSim, double overviewSim, double castDirectorSim,
                                            boolean hasOverview, boolean hasCastDirector) {
        double wGenre = GENRE_WEIGHT;
        double wOverview = hasOverview ? OVERVIEW_WEIGHT : 0.0;
        double wCast = hasCastDirector ? CAST_DIRECTOR_WEIGHT : 0.0;

        double totalWeight = wGenre + wOverview + wCast;
        if (totalWeight <= 0.0) {
            return clampScore(genreSim);
        }

        double normalizedContentSim = ((genreSim * wGenre) + (overviewSim * wOverview) + (castDirectorSim * wCast)) / totalWeight;
        return clampScore(normalizedContentSim);
    }

    /**
     * Computes the final composite recommendation score combining Content Similarity and TMDB Quality Rating.
     * Formula: FinalScore = (0.70 × ContentSim) + (0.30 × NormalizedRating)
     *
     * @param contentSimilarity Normalized content similarity [0.0 - 1.0]
     * @param normalizedRating  Normalized rating out of 10 [0.0 - 1.0]
     * @return Final composite ranking score [0.0 - 1.0]
     */
    public double calculateFinalScore(double contentSimilarity, double normalizedRating) {
        double score = (contentSimilarity * CONTENT_SIMILARITY_WEIGHT) + (normalizedRating * RATING_WEIGHT);
        return clampScore(score);
    }

    /**
     * Core recommendation pipeline implementing {@link MovieRecommender}.
     *
     * @param targetMovie   The reference movie searched by user
     * @param candidatePool Available candidate movies from TMDB / local cache
     * @param topN          Number of recommendations to return
     * @return Top-N ranked MovieRecommendation list sorted by final score descending
     */
    @Override
    public List<MovieRecommendation> recommendSimilar(Movie targetMovie, List<Movie> candidatePool, int topN) {
        if (targetMovie == null || candidatePool == null || candidatePool.isEmpty() || topN <= 0) {
            return Collections.emptyList();
        }

        List<MovieRecommendation> recommendations = new ArrayList<>();
        Set<Integer> processedMovieIds = new HashSet<>();

        Set<String> targetGenres = targetMovie.getGenreSet();
        boolean targetHasOverview = targetMovie.getOverview() != null && !targetMovie.getOverview().trim().isEmpty();
        boolean targetHasCast = (targetMovie.getTopCast() != null && !targetMovie.getTopCast().isEmpty()) ||
                (targetMovie.getDirector() != null && !targetMovie.getDirector().trim().isEmpty());

        for (Movie candidate : candidatePool) {
            if (candidate == null) {
                continue;
            }

            // 1. STRICT EXCLUSION: Do not recommend the target movie itself
            if (isSameMovie(targetMovie, candidate)) {
                continue;
            }

            // Deduplicate candidate movies by TMDB ID
            if (candidate.getId() > 0 && !processedMovieIds.add(candidate.getId())) {
                continue;
            }

            // 2. Compute Individual Component Similarities
            double genreSim = calculateGenreSimilarity(targetGenres, candidate.getGenreSet());
            double overviewSim = calculateOverviewSimilarity(targetMovie, candidate);
            double castDirSim = calculateCastDirectorSimilarity(targetMovie, candidate);

            boolean candHasOverview = candidate.getOverview() != null && !candidate.getOverview().trim().isEmpty();
            boolean candHasCast = (candidate.getTopCast() != null && !candidate.getTopCast().isEmpty()) ||
                    (candidate.getDirector() != null && !candidate.getDirector().trim().isEmpty());

            // 3. Compute Composite Content Similarity
            double contentSim = calculateContentSimilarity(
                    genreSim, overviewSim, castDirSim,
                    targetHasOverview && candHasOverview,
                    targetHasCast && candHasCast
            );

            // Filter out candidates with zero similarity
            if (contentSim <= 0.0 && genreSim <= 0.0) {
                continue;
            }

            // 4. Compute Normalized Quality Rating
            double normRating = normalizeRating(candidate.getVoteAverage());

            // 5. Compute Final Composite Score
            double finalScore = calculateFinalScore(contentSim, normRating);

            // 6. Build MovieRecommendation Object
            MovieRecommendation rec = new MovieRecommendation(
                    targetMovie, candidate, contentSim, normRating, finalScore,
                    genreSim, overviewSim, castDirSim
            );
            recommendations.add(rec);
        }

        // 7. Sort by FinalScore descending using Java Comparator
        // If final scores are tied, prefer higher quality ratings, then title
        recommendations.sort(
                Comparator.comparingDouble(MovieRecommendation::getFinalScore).reversed()
                        .thenComparing(r -> r.getMovie() != null ? r.getMovie().getVoteAverage() : 0.0, Comparator.reverseOrder())
                        .thenComparing(r -> r.getMovie() != null ? r.getMovie().getTitle() : "")
        );

        // 8. Extract Top-N recommendations with boundary safety
        int limit = Math.min(topN, recommendations.size());
        return new ArrayList<>(recommendations.subList(0, limit));
    }
}
