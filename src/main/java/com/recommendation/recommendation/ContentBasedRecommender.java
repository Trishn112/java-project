package com.recommendation.recommendation;

import com.recommendation.model.Category;
import com.recommendation.model.Item;
import com.recommendation.model.Rating;
import com.recommendation.model.Recommendation;
import com.recommendation.model.User;
import com.recommendation.model.UserPreference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Concrete implementation of a Content-Based Recommender for Phase 3.
 * Represents an intelligent, data-driven, weighted recommendation engine built in 100% Java.
 *
 * Architecture & OOP Principles:
 * 1. INHERITANCE: Extends {@link RecommendationEngine}.
 * 2. INTERFACE IMPLEMENTATION: Implements {@link Recommender}.
 * 3. METHOD MODULARITY: Encapsulates scoring logic into dedicated single-responsibility methods.
 * 4. COLLECTIONS & GENERICS: Employs List, ArrayList, Set, HashSet, Map, HashMap for efficient processing.
 *
 * Recommendation Scoring Model (Phase 3):
 * -----------------------------------------------------------------------------------------
 *  1. User Preference Score (Weight: 40%):
 *     - Normalized user affinity score for the candidate item's category: [0.0 - 1.0]
 *     - preferenceScore = userPreferenceScore / 5.0
 *
 *  2. Category Match Score (Weight: 20%):
 *     - Category matching factor indicating relevance to user's interest profile: [0.0 - 1.0]
 *     - categoryScore = categoryMatchScore / 5.0 (0.0 if unmatched)
 *
 *  3. Item Rating Score (Weight: 25%):
 *     - Normalized overall community quality rating of the item: [0.0 - 1.0]
 *     - ratingScore = itemRating / 5.0
 *
 *  4. Item Popularity Score (Weight: 15%):
 *     - Normalized community popularity / interaction counter: [0.0 - 1.0]
 *     - popularityScore = itemPopularity / 100.0
 *
 * Final Weighted Composite Score:
 *     finalScore = (preferenceScore * 0.40) + (categoryScore * 0.20) + (ratingScore * 0.25) + (popularityScore * 0.15)
 *
 * Previous Rating Exclusion:
 *     Items already rated by the target user are excluded from new recommendations
 *     using an efficient Set<Integer> lookup.
 * -----------------------------------------------------------------------------------------
 */
public class ContentBasedRecommender extends RecommendationEngine implements Recommender {

    // Scoring Weights (Sum = 1.00 / 100%)
    public static final double PREFERENCE_WEIGHT = 0.40;  // 40% User taste affinity
    public static final double CATEGORY_WEIGHT   = 0.20;  // 20% Category matching
    public static final double RATING_WEIGHT     = 0.25;  // 25% Item quality rating
    public static final double POPULARITY_WEIGHT = 0.15;  // 15% Item community popularity

    // Internal fallback preference and rating storage
    private Map<Category, Double> internalPreferenceMap;
    private Set<Integer> internalRatedItemIds;

    /**
     * Default constructor.
     */
    public ContentBasedRecommender() {
        this.internalPreferenceMap = new HashMap<>();
        this.internalRatedItemIds = new HashSet<>();
    }

    /**
     * Parameterized constructor accepting user preferences.
     *
     * @param preferences List of UserPreference records
     */
    public ContentBasedRecommender(List<UserPreference> preferences) {
        this();
        setPreferences(preferences);
    }

    /**
     * Parameterized constructor accepting preferences and previous ratings.
     *
     * @param preferences List of UserPreference records
     * @param ratings     List of Rating records
     */
    public ContentBasedRecommender(List<UserPreference> preferences, List<Rating> ratings, User targetUser) {
        this();
        setPreferences(preferences);
        setPreviousRatings(ratings, targetUser);
    }

    /**
     * Sets internal preferences from a List of UserPreference objects.
     *
     * @param preferences List of UserPreference
     */
    public void setPreferences(List<UserPreference> preferences) {
        this.internalPreferenceMap = new HashMap<>();
        if (preferences != null) {
            for (UserPreference pref : preferences) {
                if (pref != null && pref.getCategory() != null) {
                    this.internalPreferenceMap.put(pref.getCategory(), pref.getPreferenceScore());
                }
            }
        }
    }

    /**
     * Sets internal rated item IDs from previous user ratings.
     *
     * @param ratings    List of Rating records
     * @param targetUser The target user
     */
    public void setPreviousRatings(List<Rating> ratings, User targetUser) {
        this.internalRatedItemIds = getRatedItemIds(targetUser, ratings);
    }

    // ========================================================================
    // 1. DEDICATED SCORING & FILTERING METHODS
    // ========================================================================

    /**
     * Calculates the normalized User Preference Score [0.0 - 1.0] for an item's category.
     *
     * @param item          The candidate item
     * @param preferenceMap Map of Category to user affinity score (1.0 to 5.0)
     * @return Normalized score between 0.0 and 1.0
     */
    public double calculatePreferenceScore(Item item, Map<Category, Double> preferenceMap) {
        if (item == null || item.getCategory() == null || preferenceMap == null || preferenceMap.isEmpty()) {
            return 0.0;
        }
        Double rawPref = preferenceMap.get(item.getCategory());
        if (rawPref == null || rawPref <= 0.0) {
            return 0.0;
        }
        return Math.min(Math.max(rawPref / 5.0, 0.0), 1.0);
    }

    /**
     * Calculates the Category Match Score [0.0 - 1.0].
     * Evaluates whether the candidate item's category matches the user's positive preference profile.
     *
     * @param item          The candidate item
     * @param preferenceMap Map of Category to user affinity score
     * @return Category match score between 0.0 and 1.0
     */
    public double calculateCategoryScore(Item item, Map<Category, Double> preferenceMap) {
        if (item == null || item.getCategory() == null || preferenceMap == null || preferenceMap.isEmpty()) {
            return 0.0;
        }
        Double rawPref = preferenceMap.get(item.getCategory());
        if (rawPref == null || rawPref <= 0.0) {
            return 0.0;
        }
        // Match factor proportional to category affinity level
        return Math.min(Math.max(rawPref / 5.0, 0.0), 1.0);
    }

    /**
     * Calculates the normalized Item Rating Score [0.0 - 1.0].
     * Assumes a standard rating scale of 1.0 (min) to 5.0 (max).
     *
     * @param item The candidate item
     * @return Normalized score between 0.0 and 1.0
     */
    public double calculateRatingScore(Item item) {
        if (item == null) {
            return 0.0;
        }
        return Math.min(Math.max(item.getRating() / 5.0, 0.0), 1.0);
    }

    /**
     * Calculates the normalized Popularity Score [0.0 - 1.0].
     * Assumes a standard popularity range of 0 to 100.
     *
     * @param item The candidate item
     * @return Normalized score between 0.0 and 1.0
     */
    public double calculatePopularityScore(Item item) {
        if (item == null) {
            return 0.0;
        }
        return Math.min(Math.max(item.getPopularity() / 100.0, 0.0), 1.0);
    }

    /**
     * Computes the final weighted recommendation score from the 4 normalized components.
     * Formula:
     *   finalScore = (preferenceScore * 0.40) + (categoryScore * 0.20) + (ratingScore * 0.25) + (popularityScore * 0.15)
     *
     * @param preferenceScore Normalized preference score [0.0 - 1.0]
     * @param categoryScore   Normalized category match score [0.0 - 1.0]
     * @param ratingScore     Normalized rating score [0.0 - 1.0]
     * @param popularityScore Normalized popularity score [0.0 - 1.0]
     * @return Final composite recommendation score [0.0 - 1.0]
     */
    public double calculateFinalScore(double preferenceScore, double categoryScore, double ratingScore, double popularityScore) {
        double score = (preferenceScore * PREFERENCE_WEIGHT)
                + (categoryScore * CATEGORY_WEIGHT)
                + (ratingScore * RATING_WEIGHT)
                + (popularityScore * POPULARITY_WEIGHT);
        return Math.min(Math.max(score, 0.0), 1.0);
    }

    /**
     * Checks whether an item has already been rated by the user.
     *
     * @param item         The candidate item
     * @param ratedItemIds Set of item IDs already rated by the user
     * @return true if the item was already rated; false otherwise
     */
    public boolean isAlreadyRated(Item item, Set<Integer> ratedItemIds) {
        if (item == null || ratedItemIds == null) {
            return false;
        }
        return ratedItemIds.contains(item.getId());
    }

    /**
     * Extracts a Set of Item IDs that the specified user has already rated.
     *
     * @param user    Target user
     * @param ratings List of all ratings
     * @return Set<Integer> of rated item IDs
     */
    public Set<Integer> getRatedItemIds(User user, List<Rating> ratings) {
        Set<Integer> ratedIds = new HashSet<>();
        if (user == null || ratings == null) {
            return ratedIds;
        }
        for (Rating rating : ratings) {
            if (rating != null && rating.getUser() != null && rating.getItem() != null) {
                if (rating.getUser().getId() == user.getId()) {
                    ratedIds.add(rating.getItem().getId());
                }
            }
        }
        return ratedIds;
    }

    // ========================================================================
    // 2. MAIN RECOMMENDATION PIPELINE
    // ========================================================================

    /**
     * Full Phase 3 Recommendation Pipeline:
     * 1. Read User Preferences into Map<Category, Double>
     * 2. Read Previous Ratings into Set<Integer> ratedItemIds
     * 3. For each available candidate item:
     *      - Check if item is already rated -> Exclude if true
     *      - Calculate Preference Score
     *      - Calculate Category Match Score
     *      - Calculate Item Rating Score
     *      - Calculate Popularity Score
     *      - Calculate Final Composite Score
     *      - Create Recommendation Object with breakdown
     * 4. Sort recommendations by final score descending using Comparator
     * 5. Return sorted recommendations
     *
     * @param user         Target user
     * @param items        Available candidate items
     * @param preferences  List of UserPreference records
     * @param ratings      List of previous Rating records
     * @return List<Recommendation> sorted in descending order of score
     */
    public List<Recommendation> generateRecommendations(User user, List<Item> items,
                                                        List<UserPreference> preferences, List<Rating> ratings) {
        List<Recommendation> recommendations = new ArrayList<>();

        // Null and empty data safety
        if (user == null || items == null || items.isEmpty()) {
            return recommendations;
        }

        // 1. Build User Preference Map (O(1) lookup)
        Map<Category, Double> preferenceMap = new HashMap<>();
        if (preferences != null) {
            for (UserPreference pref : preferences) {
                if (pref != null && pref.getCategory() != null) {
                    preferenceMap.put(pref.getCategory(), pref.getPreferenceScore());
                }
            }
        }

        // 2. Build Set of Already Rated Item IDs (O(1) lookup)
        Set<Integer> ratedItemIds = getRatedItemIds(user, ratings);

        // 3. Process candidate items
        for (Item item : items) {
            if (item == null) {
                continue;
            }

            // Exclude items already rated by the user
            if (isAlreadyRated(item, ratedItemIds)) {
                continue;
            }

            // Calculate component scores
            double prefScore = calculatePreferenceScore(item, preferenceMap);
            double catScore  = calculateCategoryScore(item, preferenceMap);
            double rateScore = calculateRatingScore(item);
            double popScore  = calculatePopularityScore(item);

            // Calculate composite final score
            double finalScore = calculateFinalScore(prefScore, catScore, rateScore, popScore);

            // Create Recommendation object with granular breakdown
            Recommendation rec = new Recommendation(user, item, finalScore, prefScore, catScore, rateScore, popScore);
            recommendations.add(rec);
        }

        // 4. Sort by score in descending order using Comparator
        recommendations.sort(
                Comparator.comparingDouble(Recommendation::getScore).reversed()
                        .thenComparing(r -> r.getItem() != null ? r.getItem().getName() : "")
        );

        return recommendations;
    }

    /**
     * Generates recommendations using explicit preferences with no previous ratings.
     *
     * @param user        Target user
     * @param items       Available items
     * @param preferences User preferences
     * @return List of sorted recommendations
     */
    public List<Recommendation> generateRecommendations(User user, List<Item> items, List<UserPreference> preferences) {
        return generateRecommendations(user, items, preferences, Collections.emptyList());
    }

    /**
     * Generates recommendations using pre-mapped Category -> Preference Score map and rated item IDs.
     *
     * @param user            Target user
     * @param items           Available candidate items
     * @param userPrefMap     Map of Category to preference score
     * @param ratedItemIds    Set of already rated item IDs
     * @return List of sorted recommendations
     */
    public List<Recommendation> generateRecommendations(User user, List<Item> items,
                                                        Map<Category, Double> userPrefMap, Set<Integer> ratedItemIds) {
        List<Recommendation> recommendations = new ArrayList<>();
        if (user == null || items == null || items.isEmpty()) {
            return recommendations;
        }

        Map<Category, Double> activePrefs = userPrefMap != null ? userPrefMap : this.internalPreferenceMap;
        Set<Integer> activeRated = ratedItemIds != null ? ratedItemIds : this.internalRatedItemIds;

        for (Item item : items) {
            if (item == null || isAlreadyRated(item, activeRated)) {
                continue;
            }

            double prefScore = calculatePreferenceScore(item, activePrefs);
            double catScore  = calculateCategoryScore(item, activePrefs);
            double rateScore = calculateRatingScore(item);
            double popScore  = calculatePopularityScore(item);
            double finalScore = calculateFinalScore(prefScore, catScore, rateScore, popScore);

            recommendations.add(new Recommendation(user, item, finalScore, prefScore, catScore, rateScore, popScore));
        }

        recommendations.sort(
                Comparator.comparingDouble(Recommendation::getScore).reversed()
                        .thenComparing(r -> r.getItem() != null ? r.getItem().getName() : "")
        );

        return recommendations;
    }

    /**
     * Implements abstract method from {@link RecommendationEngine}.
     *
     * @param user  Target user
     * @param items Available items
     * @return List of recommendations
     */
    @Override
    public List<Recommendation> generateRecommendations(User user, List<Item> items) {
        return generateRecommendations(user, items, this.internalPreferenceMap, this.internalRatedItemIds);
    }

    /**
     * Implements interface contract from {@link Recommender}.
     *
     * @param user  Target user
     * @param items Available items
     * @return List of recommendations
     */
    @Override
    public List<Recommendation> recommend(User user, List<Item> items) {
        return generateRecommendations(user, items);
    }

    /**
     * Interface overload with explicit preferences and ratings.
     *
     * @param user        Target user
     * @param items       Available items
     * @param preferences User preferences
     * @param ratings     Previous ratings
     * @return List of recommendations
     */
    public List<Recommendation> recommend(User user, List<Item> items, List<UserPreference> preferences, List<Rating> ratings) {
        return generateRecommendations(user, items, preferences, ratings);
    }

    // ========================================================================
    // 3. TOP-N RECOMMENDATIONS
    // ========================================================================

    /**
     * Returns the Top-N recommendations with full preference and rating filtering.
     * Safely handles all boundary conditions (N <= 0, N > available items, empty data).
     *
     * @param user        Target user
     * @param items       Available candidate items
     * @param preferences User preferences
     * @param ratings     Previous ratings
     * @param topN        Maximum number of recommendations to return (e.g. 5)
     * @return List<Recommendation> containing at most topN recommendations
     */
    public List<Recommendation> recommendTopN(User user, List<Item> items,
                                              List<UserPreference> preferences, List<Rating> ratings, int topN) {
        if (topN <= 0) {
            return new ArrayList<>();
        }

        List<Recommendation> allRecs = generateRecommendations(user, items, preferences, ratings);
        if (allRecs.isEmpty()) {
            return allRecs;
        }

        int limit = Math.min(topN, allRecs.size());
        return new ArrayList<>(allRecs.subList(0, limit));
    }

    /**
     * Returns the Top-N recommendations with explicit preferences (no ratings).
     *
     * @param user        Target user
     * @param items       Available candidate items
     * @param preferences User preferences
     * @param topN        Maximum number of recommendations
     * @return List<Recommendation> of topN items
     */
    public List<Recommendation> recommendTopN(User user, List<Item> items, List<UserPreference> preferences, int topN) {
        return recommendTopN(user, items, preferences, Collections.emptyList(), topN);
    }

    /**
     * Returns the Top-N recommendations using internal state.
     *
     * @param user  Target user
     * @param items Available items
     * @param topN  Maximum number of recommendations
     * @return List<Recommendation> of topN items
     */
    public List<Recommendation> recommendTopN(User user, List<Item> items, int topN) {
        if (topN <= 0) {
            return new ArrayList<>();
        }

        List<Recommendation> allRecs = generateRecommendations(user, items);
        if (allRecs.isEmpty()) {
            return allRecs;
        }

        int limit = Math.min(topN, allRecs.size());
        return new ArrayList<>(allRecs.subList(0, limit));
    }
}
