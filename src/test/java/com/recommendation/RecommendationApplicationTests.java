package com.recommendation;

import com.recommendation.service.MovieRecommendationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Spring Boot Application Context Smoke Test.
 */
@SpringBootTest
class RecommendationApplicationTests {

    @Autowired
    private MovieRecommendationService movieRecommendationService;

    @Test
    void contextLoads() {
        assertNotNull(movieRecommendationService, "MovieRecommendationService should be successfully initialized and injected.");
    }
}
