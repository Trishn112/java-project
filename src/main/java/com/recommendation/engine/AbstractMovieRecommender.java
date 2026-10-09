package com.recommendation.engine;

import com.recommendation.model.Movie;
import com.recommendation.model.MovieRecommendation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Abstract base class providing common validation, normalization, exclusion,
 * and text-processing utilities for recommendation algorithms.
 * Demonstrates ABSTRACTION, INHERITANCE, and METHOD OVERRIDING.
 */
public abstract class AbstractMovieRecommender implements MovieRecommender {

    // Common English stop words for plot overview tokenization
    protected static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are",
            "as", "at", "be", "because", "been", "before", "being", "below", "between", "both", "but",
            "by", "can", "did", "do", "does", "doing", "don", "down", "during", "each", "few", "for",
            "from", "further", "had", "has", "have", "having", "he", "her", "here", "hers", "herself",
            "him", "himself", "his", "how", "i", "if", "in", "into", "is", "it", "its", "itself",
            "just", "me", "more", "most", "my", "myself", "no", "nor", "not", "now", "of", "off",
            "on", "once", "only", "or", "other", "our", "ours", "ourselves", "out", "over", "own",
            "s", "same", "she", "should", "so", "some", "such", "t", "than", "that", "the", "their",
            "theirs", "them", "themselves", "then", "there", "these", "they", "this", "those", "through",
            "to", "too", "under", "until", "up", "very", "was", "we", "were", "what", "when", "where",
            "which", "while", "who", "whom", "why", "will", "with", "would", "you", "your", "yours"
    ));

    /**
     * Checks if two movies represent the same entity to enforce target movie exclusion.
     *
     * @param target    The target movie searched by the user
     * @param candidate The candidate movie being evaluated
     * @return true if candidate is the same as target movie, false otherwise
     */
    protected boolean isSameMovie(Movie target, Movie candidate) {
        if (target == null || candidate == null) {
            return false;
        }
        if (target.getId() > 0 && candidate.getId() > 0 && target.getId() == candidate.getId()) {
            return true;
        }
        if (target.getTitle() != null && candidate.getTitle() != null) {
            boolean titleMatches = target.getTitle().trim().equalsIgnoreCase(candidate.getTitle().trim());
            boolean yearMatches = target.getReleaseYear() > 0 && candidate.getReleaseYear() > 0 &&
                    target.getReleaseYear() == candidate.getReleaseYear();
            return titleMatches && (yearMatches || target.getReleaseYear() == 0 || candidate.getReleaseYear() == 0);
        }
        return false;
    }

    /**
     * Normalizes a TMDB movie rating (0.0 to 10.0) into a [0.0 - 1.0] scale.
     *
     * @param voteAverage Raw rating out of 10
     * @return Normalized score between 0.0 and 1.0
     */
    protected double normalizeRating(double voteAverage) {
        if (voteAverage <= 0.0) {
            return 0.0;
        }
        return Math.min(Math.max(voteAverage / 10.0, 0.0), 1.0);
    }

    /**
     * Tokenizes a text string into a clean Set of non-stop-word lowercase keywords.
     * Demonstrates COLLECTIONS (Set, HashSet) and Generics.
     *
     * @param text Input text (e.g. movie overview or keywords)
     * @return Set<String> of clean lowercase tokens
     */
    protected Set<String> extractTokens(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> tokens = new HashSet<>();
        String[] words = text.toLowerCase().replaceAll("[^a-zA-Z0-9\\s]", " ").split("\\s+");
        for (String word : words) {
            String trimmed = word.trim();
            if (trimmed.length() > 2 && !STOP_WORDS.contains(trimmed)) {
                tokens.add(trimmed);
            }
        }
        return tokens;
    }

    /**
     * Safely bounds any score value to the [0.0, 1.0] interval.
     */
    protected double clampScore(double score) {
        if (Double.isNaN(score) || Double.isInfinite(score) || score < 0.0) {
            return 0.0;
        }
        return Math.min(score, 1.0);
    }
}
