package com.recommendation.recommendation;

import com.recommendation.model.Item;
import com.recommendation.model.Recommendation;
import com.recommendation.model.User;
import java.util.List;

/**
 * Interface defining the contract for any recommender component in the system.
 * Demonstrates the OOP principle of INTERFACES / ABSTRACTION.
 */
public interface Recommender {

    /**
     * Recommends items for a specified user from a candidate item pool.
     *
     * @param user  The target user
     * @param items Available items
     * @return List of recommendations
     */
    List<Recommendation> recommend(
        User user,
        List<Item> items
    );
}
