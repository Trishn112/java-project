package com.recommendation.service;

import com.recommendation.model.Category;
import com.recommendation.model.Item;
import com.recommendation.model.Rating;
import com.recommendation.model.Recommendation;
import com.recommendation.model.User;
import com.recommendation.model.UserPreference;
import com.recommendation.recommendation.ContentBasedRecommender;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Spring Service bridging Web Controllers with the in-memory DataRepository
 * and ContentBasedRecommender AI engine.
 */
@Service
public class RecommendationService {

    private final DataRepository repository;
    private final ContentBasedRecommender recommender;

    public RecommendationService() {
        this.repository = new DataRepository();
        this.repository.initializeSampleData();
        this.recommender = new ContentBasedRecommender();
    }

    public List<User> getAllUsers() {
        return repository.getAllUsers();
    }

    public User getUserById(int id) {
        return repository.getUserById(id);
    }

    public List<Item> getAllItems() {
        return repository.getAllItems();
    }

    public Item getItemById(int id) {
        return repository.getItemById(id);
    }

    public List<Category> getAllCategories() {
        return repository.getAllCategories();
    }

    public Category getCategoryById(int id) {
        return repository.getCategoryById(id);
    }

    public List<UserPreference> getPreferencesForUser(int userId) {
        User user = repository.getUserById(userId);
        return repository.getPreferencesForUser(user);
    }

    public List<Rating> getRatingsForUser(int userId) {
        User user = repository.getUserById(userId);
        return repository.getRatingsForUser(user);
    }

    public List<Rating> getRatingsForItem(int itemId) {
        Item item = repository.getItemById(itemId);
        return repository.getRatingsForItem(item);
    }

    public Set<Integer> getRatedItemIdsForUser(int userId) {
        User user = repository.getUserById(userId);
        return repository.getRatedItemIdsForUser(user);
    }

    /**
     * Filters and searches catalog items based on keywords, category, and minimum rating.
     */
    public List<Item> searchAndFilterItems(String query, Integer categoryId, Double minRating, String sortBy) {
        List<Item> result = repository.getAllItems();

        if (query != null && !query.trim().isEmpty()) {
            String lowerQuery = query.trim().toLowerCase();
            result = result.stream()
                    .filter(item -> (item.getName() != null && item.getName().toLowerCase().contains(lowerQuery)) ||
                            (item.getDescription() != null && item.getDescription().toLowerCase().contains(lowerQuery)) ||
                            (item.getCategory() != null && item.getCategory().getName().toLowerCase().contains(lowerQuery)))
                    .collect(Collectors.toList());
        }

        if (categoryId != null && categoryId > 0) {
            result = result.stream()
                    .filter(item -> item.getCategory() != null && item.getCategory().getId() == categoryId)
                    .collect(Collectors.toList());
        }

        if (minRating != null && minRating > 0) {
            result = result.stream()
                    .filter(item -> item.getRating() >= minRating)
                    .collect(Collectors.toList());
        }

        if (sortBy != null) {
            switch (sortBy.toLowerCase()) {
                case "rating_desc":
                    result.sort(Comparator.comparingDouble(Item::getRating).reversed());
                    break;
                case "popularity_desc":
                    result.sort(Comparator.comparingInt(Item::getPopularity).reversed());
                    break;
                case "title_asc":
                    result.sort(Comparator.comparing(Item::getName));
                    break;
                default:
                    // default order
                    break;
            }
        }

        return result;
    }

    /**
     * Generates personalized recommendations for a user via the ContentBasedRecommender.
     */
    public List<Recommendation> getPersonalizedRecommendations(int userId, Integer categoryId, Double minRating, Integer topN) {
        User user = repository.getUserById(userId);
        if (user == null) {
            return Collections.emptyList();
        }

        List<Item> candidateItems = repository.getAllItems();
        List<UserPreference> preferences = repository.getPreferencesForUser(user);
        List<Rating> userRatings = repository.getRatingsForUser(user);

        // Core AI Recommendation Engine Execution (Excludes already rated items)
        List<Recommendation> recommendations = recommender.generateRecommendations(user, candidateItems, preferences, userRatings);

        // Optional post-filtering by category and minimum rating
        if (categoryId != null && categoryId > 0) {
            recommendations = recommendations.stream()
                    .filter(rec -> rec.getItem() != null && rec.getItem().getCategory() != null && rec.getItem().getCategory().getId() == categoryId)
                    .collect(Collectors.toList());
        }

        if (minRating != null && minRating > 0) {
            recommendations = recommendations.stream()
                    .filter(rec -> rec.getItem() != null && rec.getItem().getRating() >= minRating)
                    .collect(Collectors.toList());
        }

        if (topN != null && topN > 0 && recommendations.size() > topN) {
            recommendations = new ArrayList<>(recommendations.subList(0, topN));
        }

        return recommendations;
    }

    /**
     * Records a new user rating and dynamically recalculates item average rating.
     */
    public boolean addRating(int userId, int itemId, double score) {
        User user = repository.getUserById(userId);
        Item item = repository.getItemById(itemId);

        if (user == null || item == null || score < 1.0 || score > 5.0) {
            return false;
        }

        int nextRatingId = repository.getAllRatings().size() + 1;
        Rating rating = new Rating(nextRatingId, user, item, score);
        repository.addRating(rating);

        // Update item average rating based on all ratings in repository
        List<Rating> itemRatings = repository.getRatingsForItem(item);
        if (!itemRatings.isEmpty()) {
            double sum = 0.0;
            for (Rating r : itemRatings) {
                sum += r.getRating();
            }
            double newAvg = Math.round((sum / itemRatings.size()) * 10.0) / 10.0;
            item.setRating(newAvg);
            item.setPopularity(Math.min(item.getPopularity() + 2, 100)); // boost popularity on interaction
        }

        return true;
    }

    public DataRepository getRepository() {
        return repository;
    }

    public ContentBasedRecommender getRecommender() {
        return recommender;
    }
}
