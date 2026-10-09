package com.recommendation.service;

import com.recommendation.model.Category;
import com.recommendation.model.Item;
import com.recommendation.model.Rating;
import com.recommendation.model.Recommendation;
import com.recommendation.model.User;
import com.recommendation.model.UserPreference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Repository class responsible for managing and querying in-memory application data.
 * Demonstrates:
 * 1. Java Collections Framework: List, ArrayList, Set, HashSet, Map, HashMap
 * 2. Generics: Strong compile-time type safety across all collections (e.g., List<Item>, Map<Category, Double>)
 * 3. Collection-based querying, filtering, aggregation, and grouping
 */
public class DataRepository {

    // ------------------------------------------------------------------------
    // Java Collections (List + ArrayList) holding entities with Generics
    // ------------------------------------------------------------------------
    private List<User> users;
    private List<Item> items;
    private List<Category> categories;
    private List<Rating> ratings;
    private List<UserPreference> preferences;
    private List<Recommendation> recommendations;

    /**
     * Default constructor initializing empty ArrayList collections for all entities.
     */
    public DataRepository() {
        this.users = new ArrayList<>();
        this.items = new ArrayList<>();
        this.categories = new ArrayList<>();
        this.ratings = new ArrayList<>();
        this.preferences = new ArrayList<>();
        this.recommendations = new ArrayList<>();
    }

    // ========================================================================
    // 1. LIST & ARRAYLIST OPERATIONS (CRUD, Access, Size, Boundary Checks)
    // ========================================================================

    // --- User Operations ---

    public void addUser(User user) {
        if (user != null && !users.contains(user)) {
            users.add(user);
        }
    }

    public User getUserById(int id) {
        for (User u : users) {
            if (u.getId() == id) {
                return u;
            }
        }
        return null;
    }

    public boolean removeUser(User user) {
        return users.remove(user);
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(users);
    }

    public int getUserCount() {
        return users.size();
    }

    public boolean isUsersEmpty() {
        return users.isEmpty();
    }

    // --- Item Operations ---

    public void addItem(Item item) {
        if (item != null && !items.contains(item)) {
            items.add(item);
        }
    }

    public Item getItemById(int id) {
        for (Item item : items) {
            if (item.getId() == id) {
                return item;
            }
        }
        return null;
    }

    public boolean removeItem(Item item) {
        return items.remove(item);
    }

    public List<Item> getAllItems() {
        return new ArrayList<>(items);
    }

    public int getItemCount() {
        return items.size();
    }

    public boolean isItemsEmpty() {
        return items.isEmpty();
    }

    // --- Category Operations ---

    public void addCategory(Category category) {
        if (category != null && !categories.contains(category)) {
            categories.add(category);
        }
    }

    public Category getCategoryById(int id) {
        for (Category cat : categories) {
            if (cat.getId() == id) {
                return cat;
            }
        }
        return null;
    }

    public Category getCategoryByName(String name) {
        if (name == null) return null;
        for (Category cat : categories) {
            if (name.equalsIgnoreCase(cat.getName())) {
                return cat;
            }
        }
        return null;
    }

    public List<Category> getAllCategories() {
        return new ArrayList<>(categories);
    }

    // --- Rating Operations ---

    public void addRating(Rating rating) {
        if (rating != null) {
            ratings.add(rating);
        }
    }

    public List<Rating> getAllRatings() {
        return new ArrayList<>(ratings);
    }

    public List<Rating> getRatingsForUser(User user) {
        List<Rating> userRatings = new ArrayList<>();
        if (user == null) return userRatings;

        for (Rating r : ratings) {
            if (r.getUser() != null && r.getUser().getId() == user.getId()) {
                userRatings.add(r);
            }
        }
        return userRatings;
    }

    public Set<Integer> getRatedItemIdsForUser(User user) {
        Set<Integer> ratedIds = new HashSet<>();
        if (user == null) return ratedIds;

        for (Rating r : ratings) {
            if (r.getUser() != null && r.getUser().getId() == user.getId() && r.getItem() != null) {
                ratedIds.add(r.getItem().getId());
            }
        }
        return ratedIds;
    }

    public List<Rating> getRatingsForItem(Item item) {
        List<Rating> itemRatings = new ArrayList<>();
        if (item == null) return itemRatings;

        for (Rating r : ratings) {
            if (r.getItem() != null && r.getItem().getId() == item.getId()) {
                itemRatings.add(r);
            }
        }
        return itemRatings;
    }

    // --- User Preference Operations ---

    public void addUserPreference(UserPreference preference) {
        if (preference != null) {
            preferences.add(preference);
        }
    }

    public List<UserPreference> getAllPreferences() {
        return new ArrayList<>(preferences);
    }

    public List<UserPreference> getPreferencesForUser(User user) {
        List<UserPreference> userPrefs = new ArrayList<>();
        if (user == null) return userPrefs;

        for (UserPreference up : preferences) {
            if (up.getUser() != null && up.getUser().getId() == user.getId()) {
                userPrefs.add(up);
            }
        }
        return userPrefs;
    }

    // --- Recommendation Storage Operations ---

    public void addRecommendation(Recommendation recommendation) {
        if (recommendation != null) {
            recommendations.add(recommendation);
        }
    }

    public void saveRecommendations(List<Recommendation> recs) {
        if (recs != null) {
            recommendations.addAll(recs);
        }
    }

    public List<Recommendation> getAllRecommendations() {
        return new ArrayList<>(recommendations);
    }

    public void clearRecommendations() {
        recommendations.clear();
    }

    // ========================================================================
    // 2. SET & HASHSET DEMONSTRATION (Eliminating Duplicates, Unique Sets)
    // ========================================================================

    /**
     * Extracts unique Category objects across all items in the repository.
     * Demonstrates Set/HashSet: Eliminates duplicate category instances automatically.
     *
     * @return Set<Category> containing distinct categories
     */
    public Set<Category> getUniqueCategories() {
        Set<Category> uniqueCategories = new HashSet<>();
        for (Item item : items) {
            if (item != null && item.getCategory() != null) {
                uniqueCategories.add(item.getCategory());
            }
        }
        return uniqueCategories;
    }

    /**
     * Extracts unique Category names as Strings.
     * Demonstrates Set<String> with HashSet for deduplicating category labels.
     *
     * @return Set<String> of unique category names
     */
    public Set<String> getUniqueCategoryNames() {
        Set<String> uniqueNames = new HashSet<>();
        for (Category cat : categories) {
            if (cat != null && cat.getName() != null) {
                uniqueNames.add(cat.getName());
            }
        }
        for (Item item : items) {
            if (item != null && item.getCategory() != null && item.getCategory().getName() != null) {
                uniqueNames.add(item.getCategory().getName());
            }
        }
        return uniqueNames;
    }

    /**
     * Retrieves unique categories preferred by a given user.
     * If multiple preference records reference the same category, HashSet prevents duplicates.
     *
     * @param user Target user
     * @return Set<Category> of unique categories preferred by the user
     */
    public Set<Category> getUniqueCategoriesPreferredByUser(User user) {
        Set<Category> preferredCategories = new HashSet<>();
        if (user == null) return preferredCategories;

        List<UserPreference> userPrefs = getPreferencesForUser(user);
        for (UserPreference pref : userPrefs) {
            if (pref != null && pref.getCategory() != null) {
                preferredCategories.add(pref.getCategory());
            }
        }
        return preferredCategories;
    }

    /**
     * Retrieves unique Item IDs that have received ratings.
     *
     * @return Set<Integer> of unique rated item IDs
     */
    public Set<Integer> getUniqueRatedItemIds() {
        Set<Integer> ratedIds = new HashSet<>();
        for (Rating rating : ratings) {
            if (rating != null && rating.getItem() != null) {
                ratedIds.add(rating.getItem().getId());
            }
        }
        return ratedIds;
    }

    // ========================================================================
    // 3. MAP & HASHMAP DEMONSTRATION (Key-Value Lookups, Indexing, Aggregation)
    // ========================================================================

    /**
     * Maps Category -> Preference Score for a given user.
     * Demonstrates Map<Category, Double> with HashMap:
     * Provides O(1) constant-time lookup when evaluating candidate items against user tastes.
     *
     * @param user Target user
     * @return Map<Category, Double> mapping Category to preference score
     */
    public Map<Category, Double> getUserPreferenceMap(User user) {
        Map<Category, Double> map = new HashMap<>();
        if (user == null) return map;

        List<UserPreference> userPrefs = getPreferencesForUser(user);
        for (UserPreference pref : userPrefs) {
            if (pref != null && pref.getCategory() != null) {
                map.put(pref.getCategory(), pref.getPreferenceScore());
            }
        }
        return map;
    }

    /**
     * Maps Category Name (String) -> Preference Score for a given user.
     * Demonstrates Map<String, Double> for category-name based scoring.
     *
     * @param user Target user
     * @return Map<String, Double> mapping category name to preference score
     */
    public Map<String, Double> getUserCategoryScoreMap(User user) {
        Map<String, Double> map = new HashMap<>();
        if (user == null) return map;

        List<UserPreference> userPrefs = getPreferencesForUser(user);
        for (UserPreference pref : userPrefs) {
            if (pref != null && pref.getCategory() != null && pref.getCategory().getName() != null) {
                map.put(pref.getCategory().getName().toLowerCase(), pref.getPreferenceScore());
            }
        }
        return map;
    }

    /**
     * Maps Item ID -> Recommendation Score.
     * Demonstrates Map<Integer, Double> for quick score lookup by item ID.
     *
     * @param recommendations List of recommendations
     * @return Map<Integer, Double> mapping Item ID to recommendation score
     */
    public Map<Integer, Double> getItemScoresMap(List<Recommendation> recommendations) {
        Map<Integer, Double> scoreMap = new HashMap<>();
        if (recommendations == null) return scoreMap;

        for (Recommendation rec : recommendations) {
            if (rec != null && rec.getItem() != null) {
                scoreMap.put(rec.getItem().getId(), rec.getScore());
            }
        }
        return scoreMap;
    }

    /**
     * Maps Item ID -> Number of Ratings received.
     * Demonstrates Map<Integer, Integer> aggregation.
     *
     * @return Map<Integer, Integer> mapping Item ID to rating count
     */
    public Map<Integer, Integer> getItemRatingCountMap() {
        Map<Integer, Integer> countMap = new HashMap<>();
        for (Rating rating : ratings) {
            if (rating != null && rating.getItem() != null) {
                int itemId = rating.getItem().getId();
                countMap.put(itemId, countMap.getOrDefault(itemId, 0) + 1);
            }
        }
        return countMap;
    }

    /**
     * Maps Item ID -> Computed Average Rating.
     * Demonstrates Map<Integer, Double> for aggregate calculations.
     *
     * @return Map<Integer, Double> mapping Item ID to average rating score
     */
    public Map<Integer, Double> getItemAverageRatingMap() {
        Map<Integer, Double> sumMap = new HashMap<>();
        Map<Integer, Integer> countMap = new HashMap<>();

        for (Rating rating : ratings) {
            if (rating != null && rating.getItem() != null) {
                int itemId = rating.getItem().getId();
                sumMap.put(itemId, sumMap.getOrDefault(itemId, 0.0) + rating.getRating());
                countMap.put(itemId, countMap.getOrDefault(itemId, 0) + 1);
            }
        }

        Map<Integer, Double> avgMap = new HashMap<>();
        for (Map.Entry<Integer, Double> entry : sumMap.entrySet()) {
            int itemId = entry.getKey();
            int count = countMap.get(itemId);
            avgMap.put(itemId, count > 0 ? entry.getValue() / count : 0.0);
        }
        return avgMap;
    }

    /**
     * Groups all items by their respective Category.
     * Demonstrates Map<Category, List<Item>> grouping pattern.
     *
     * @return Map<Category, List<Item>> grouping items by category
     */
    public Map<Category, List<Item>> getItemsGroupedByCategory() {
        Map<Category, List<Item>> grouped = new HashMap<>();
        for (Item item : items) {
            if (item != null && item.getCategory() != null) {
                Category cat = item.getCategory();
                grouped.computeIfAbsent(cat, k -> new ArrayList<>()).add(item);
            }
        }
        return grouped;
    }

    // ========================================================================
    // 4. COLLECTION FILTERING & DATA PROCESSING
    // ========================================================================

    /**
     * Filters items by Category using List traversal.
     *
     * @param category Category to filter by
     * @return List<Item> containing items in the specified category
     */
    public List<Item> filterItemsByCategory(Category category) {
        List<Item> filtered = new ArrayList<>();
        if (category == null) return filtered;

        for (Item item : items) {
            if (item != null && category.equals(item.getCategory())) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    /**
     * Filters items with a rating greater than or equal to minRating.
     *
     * @param minRating Minimum rating threshold
     * @return List<Item> containing items meeting the threshold
     */
    public List<Item> filterItemsByMinRating(double minRating) {
        List<Item> filtered = new ArrayList<>();
        for (Item item : items) {
            if (item != null && item.getRating() >= minRating) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    /**
     * Calculates the overall average rating across all items in the repository.
     *
     * @return double representing average rating, or 0.0 if empty
     */
    public double calculateOverallAverageRating() {
        if (items.isEmpty()) return 0.0;
        double sum = 0.0;
        int count = 0;
        for (Item item : items) {
            if (item != null) {
                sum += item.getRating();
                count++;
            }
        }
        return count > 0 ? sum / count : 0.0;
    }

    // ========================================================================
    // 5. SAMPLE DATA INITIALIZATION (Phase 2 Demo Dataset)
    // ========================================================================

    /**
     * Initializes a comprehensive sample dataset demonstrating:
     * - 4 Categories: Action, Comedy, Drama, Sci-Fi
     * - 8 Items with realistic ratings and popularity values
     * - 3 Users with diverse preference profiles
     * - UserPreferences mapping users to category affinities
     * - Ratings from users for various items
     */
    public void initializeSampleData() {
        // Clear any existing data
        users.clear();
        items.clear();
        categories.clear();
        ratings.clear();
        preferences.clear();
        recommendations.clear();

        // 1. Categories
        Category action = new Category(1, "Action");
        Category comedy = new Category(2, "Comedy");
        Category drama = new Category(3, "Drama");
        Category sciFi = new Category(4, "Sci-Fi");

        categories.add(action);
        categories.add(comedy);
        categories.add(drama);
        categories.add(sciFi);

        // 2. Items
        Item item1 = new Item(101, "The Dark Knight", "Gritty superhero action masterpiece", action, 4.9, 95);
        Item item2 = new Item(102, "Inception", "Mind-bending sci-fi subconscious heist", sciFi, 4.8, 90);
        Item item3 = new Item(103, "Interstellar", "Epic space exploration and quantum physics", sciFi, 4.7, 85);
        Item item4 = new Item(104, "Superbad", "Coming-of-age high school comedy", comedy, 4.3, 75);
        Item item5 = new Item(105, "The Hangover", "Wild bachelor party comedy in Las Vegas", comedy, 4.1, 70);
        Item item6 = new Item(106, "The Godfather", "Legendary mafia crime family drama", drama, 4.9, 98);
        Item item7 = new Item(107, "Forrest Gump", "Heartwarming life journey through modern history", drama, 4.8, 92);
        Item item8 = new Item(108, "Avengers: Endgame", "Epic climax to the superhero universe saga", action, 4.6, 88);

        items.add(item1);
        items.add(item2);
        items.add(item3);
        items.add(item4);
        items.add(item5);
        items.add(item6);
        items.add(item7);
        items.add(item8);

        // 3. Users
        // User 1: Action & Sci-Fi Enthusiast (John Doe)
        User user1 = new User(1, "John Doe", "john@example.com", "pass123", "USER");
        // User 2: Sci-Fi & Drama Fan (Sarah Connor)
        User user2 = new User(2, "Sarah Connor", "sarah@example.com", "pass456", "USER");
        // User 3: Comedy & Drama Lover (Michael Scott)
        User user3 = new User(3, "Michael Scott", "michael@example.com", "pass789", "USER");

        users.add(user1);
        users.add(user2);
        users.add(user3);

        // 4. User Preferences (Category Affinity on a 1.0 - 5.0 scale)
        // User 1 preferences (Loves Action & Sci-Fi)
        preferences.add(new UserPreference(1, user1, action, 5.0));
        preferences.add(new UserPreference(2, user1, sciFi, 4.5));
        preferences.add(new UserPreference(3, user1, drama, 2.5));
        preferences.add(new UserPreference(4, user1, comedy, 1.5));

        // User 2 preferences (Loves Sci-Fi & Drama)
        preferences.add(new UserPreference(5, user2, sciFi, 5.0));
        preferences.add(new UserPreference(6, user2, drama, 4.5));
        preferences.add(new UserPreference(7, user2, action, 2.0));
        preferences.add(new UserPreference(8, user2, comedy, 1.0));

        // User 3 preferences (Loves Comedy)
        preferences.add(new UserPreference(9, user3, comedy, 5.0));
        preferences.add(new UserPreference(10, user3, drama, 3.5));
        preferences.add(new UserPreference(11, user3, action, 2.0));
        preferences.add(new UserPreference(12, user3, sciFi, 1.0));

        // 5. Ratings
        ratings.add(new Rating(1, user1, item1, 5.0)); // John rated The Dark Knight 5.0
        ratings.add(new Rating(2, user1, item2, 4.5)); // John rated Inception 4.5
        ratings.add(new Rating(3, user2, item3, 5.0)); // Sarah rated Interstellar 5.0
        ratings.add(new Rating(4, user2, item6, 4.8)); // Sarah rated The Godfather 4.8
        ratings.add(new Rating(5, user3, item4, 5.0)); // Michael rated Superbad 5.0
        ratings.add(new Rating(6, user3, item5, 4.7)); // Michael rated The Hangover 4.7
    }
}
