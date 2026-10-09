package com.recommendation.recommendation;

import com.recommendation.model.Item;
import com.recommendation.model.Recommendation;
import com.recommendation.model.User;
import java.util.List;

/**
 * Abstract class representing a generic recommendation engine.
 * Demonstrates the OOP principle of ABSTRACTION by defining the contract
 * for generating recommendations without specifying the exact algorithm.
 */
public abstract class RecommendationEngine {

    /**
     * Abstract method to generate a list of recommendations for a given user.
     *
     * @param user  The target user
     * @param items List of available items
     * @return List of generated Recommendation objects
     */
    public abstract List<Recommendation> generateRecommendations(
        User user,
        List<Item> items
    );
}
