package com.recommendation.engine;

import com.recommendation.model.Movie;
import com.recommendation.model.MovieRecommendation;
import java.util.List;

/**
 * Interface defining the abstract contract for movie recommendation algorithms.
 * Demonstrates the OOP principles of ABSTRACTION and INTERFACES.
 */
public interface MovieRecommender {

    /**
     * Generates a ranked list of similar movie recommendations for a target movie.
     *
     * @param targetMovie   The reference movie selected by the user
     * @param candidatePool The collection of candidate movies to evaluate
     * @param topN          The maximum number of top recommendations to return
     * @return Ranked list of MovieRecommendation objects sorted by descending score
     */
    List<MovieRecommendation> recommendSimilar(Movie targetMovie, List<Movie> candidatePool, int topN);
}
