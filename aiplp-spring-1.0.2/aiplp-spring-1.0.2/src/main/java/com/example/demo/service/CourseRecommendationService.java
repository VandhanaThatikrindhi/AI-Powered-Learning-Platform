package com.example.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CourseRecommendationService {
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private final CourseRecommendationFetchService fetchService;
    private static final Logger logger = LoggerFactory.getLogger(CourseRecommendationService.class);
    
    @Autowired
    public CourseRecommendationService(ResourceLoader resourceLoader, CourseRecommendationFetchService fetchService) {
        this.objectMapper = new ObjectMapper();
        this.resourceLoader = resourceLoader;
        this.fetchService = fetchService;
    }
    
    /**
     * Checks if recommendations exist for a user
     * @param userId The user ID
     * @return true if recommendations exist, false otherwise
     */
    public boolean hasRecommendations(Long userId) {
        try {
            // Try to find the recommendations file
            Resource resource = findRecommendationResource(userId);
            return resource != null && resource.exists();
        } catch (Exception e) {
            logger.error("Error checking if recommendations exist for user {}: {}", userId, e.getMessage());
            return false;
        }
    }
    
    /**
     * Finds the recommendation resource using multiple fallback mechanisms
     * @param userId The user ID
     * @return The resource if found, null otherwise
     */
    private Resource findRecommendationResource(Long userId) {
        String filename = String.format("suggest_courses_%d.json", userId);
        
        // Try all possible locations
        String[] paths = {
            "classpath:static/data/data/" + filename,
            "classpath:data/data/" + filename,
            "file:src/main/resources/static/data/data/" + filename,
            "file:target/classes/static/data/data/" + filename,
            "file:target/classes/data/data/" + filename,
            "file:data/" + filename
        };
        
        for (String path : paths) {
            try {
                Resource resource = resourceLoader.getResource(path);
                if (resource.exists()) {
                    logger.debug("Found recommendation file at: {}", path);
                    return resource;
                }
            } catch (Exception e) {
                // Continue to next path
                logger.trace("Could not load from {}: {}", path, e.getMessage());
            }
        }
        
        // If not found in classpath resources, try direct file access
        try {
            File file = fetchService.getRecommendationFile(userId, false);
            if (file.exists()) {
                logger.debug("Found recommendation file at: {}", file.getAbsolutePath());
                return resourceLoader.getResource("file:" + file.getAbsolutePath());
            }
        } catch (Exception e) {
            logger.debug("Could not load from direct file: {}", e.getMessage());
        }
        
        logger.warn("Could not find recommendation file for user {}", userId);
        return null;
    }
    
    /**
     * Ensures recommendations are available for a user, fetching them if necessary
     * @param userId The user ID
     * @return true if recommendations are available, false otherwise
     */
    public boolean ensureRecommendations(Long userId) {
        if (hasRecommendations(userId)) {
            return true;
        }
        
        // If recommendations don't exist, fetch them
        return fetchService.fetchAndSaveCourseRecommendations(userId);
    }
    
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getRecommendedCourses(Long userId) {
        try {
            // Try to ensure recommendations exist
            if (!ensureRecommendations(userId)) {
                logger.warn("Failed to ensure recommendations for user {}", userId);
                return new ArrayList<>();
            }
            
            // Find the recommendation resource
            Resource resource = findRecommendationResource(userId);
            if (resource == null || !resource.exists()) {
                logger.warn("Recommendation file still not found for user {} after ensuring", userId);
                return new ArrayList<>();
            }
            
            // Read the JSON data
            InputStream inputStream = null;
            try {
                inputStream = resource.getInputStream();
            } catch (Exception e) {
                // If we can't get the input stream from the resource, try direct file access
                File file = fetchService.getRecommendationFile(userId, false);
                if (file.exists()) {
                    inputStream = new FileInputStream(file);
                }
            }
            
            if (inputStream == null) {
                logger.warn("Could not open input stream for recommendation file for user {}", userId);
                return new ArrayList<>();
            }
            
            // Read the JSON array of user recommendations
            Map<String, Object> userData = objectMapper.readValue(inputStream, Map.class);
            List<Map<String, Object>> recommendations = (List<Map<String, Object>>) userData.get("recommendations");
            
            if (recommendations == null || recommendations.isEmpty()) {
                logger.warn("No recommendations found in suggestions file for user {}", userId);
                return new ArrayList<>();
            }
            
            logger.info("Found {} recommendations for user {}", recommendations.size(), userId);
            return recommendations;
            
        } catch (Exception e) {
            logger.error("Error reading course suggestions file: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Forces a refresh of recommendations for a user
     * @param userId The user ID
     * @return true if refresh was successful, false otherwise
     */
    public boolean refreshRecommendations(Long userId) {
        try {
            // Delete any existing files first
            File file = fetchService.getRecommendationFile(userId, false);
            if (file.exists()) {
                file.delete();
            }
            
            // Try to delete from other locations
            try {
                String filename = "suggest_courses_" + userId + ".json";
                Files.deleteIfExists(Paths.get("src/main/resources/static/data/data/", filename));
                Files.deleteIfExists(Paths.get("target/classes/static/data/data/", filename));
                Files.deleteIfExists(Paths.get("target/classes/data/data/", filename));
            } catch (Exception e) {
                logger.debug("Error deleting old recommendation files: {}", e.getMessage());
            }
            
            // Fetch fresh recommendations
            return fetchService.fetchAndSaveCourseRecommendations(userId);
        } catch (Exception e) {
            logger.error("Error refreshing recommendations for user {}: {}", userId, e.getMessage());
            return false;
        }
    }
}
