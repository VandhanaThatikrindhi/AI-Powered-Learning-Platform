package com.example.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CourseRecommendationFetchService {

    private static final Logger logger = LoggerFactory.getLogger(CourseRecommendationFetchService.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private static final String LOG_FILE_NAME = "suggest.log";

    @Value("${app.log.directory:#{systemProperties['user.dir']}}")
    private String logDirectory;
    
    @Value("${app.recommendation.api.url:http://localhost:8000}")
    private String recommendationApiUrl;

    @Autowired
    public CourseRecommendationFetchService(RestTemplate restTemplate, ObjectMapper objectMapper, ResourceLoader resourceLoader) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
    }

    private void logToFile(String message) {
        try {
            // Use provided log directory or default to current working directory
            File logFile = new File(logDirectory, LOG_FILE_NAME);
            
            // Ensure log directory exists
            logFile.getParentFile().mkdirs();

            try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                writer.println(String.format("[%s] %s", timestamp, message));
            }
        } catch (IOException e) {
            // Fallback logging
            logger.error("Failed to write to log file", e);
        }
    }
    
    /**
     * Gets the absolute path to the recommendation file for a user
     * @param userId User ID
     * @param createDirs Whether to create directories if they don't exist
     * @return File object pointing to the recommendation file
     */
    public File getRecommendationFile(Long userId, boolean createDirs) {
        // Get the absolute path to the resources directory
        String filename = "suggest_courses_" + userId + ".json";
        
        try {
            // First try to get the resources directory in the filesystem
            File resourcesDir = new File("src/main/resources/static/data/data/");
            
            // If we're running from a JAR, use a different approach
            if (!resourcesDir.exists()) {
                try {
                    resourcesDir = ResourceUtils.getFile("classpath:static/data/data/");
                } catch (Exception e) {
                    // If that fails, use the application directory
                    resourcesDir = new File(System.getProperty("user.dir"), "data");
                }
            }
            
            // Ensure the directory exists if requested
            if (createDirs && !resourcesDir.exists()) {
                boolean created = resourcesDir.mkdirs();
                if (created) {
                    logToFile("Created directory: " + resourcesDir.getAbsolutePath());
                }
            }
            
            return new File(resourcesDir, filename);
        } catch (Exception e) {
            logger.error("Error determining recommendation file path", e);
            // Fallback to temp directory
            return new File(System.getProperty("java.io.tmpdir"), filename);
        }
    }

    public boolean fetchAndSaveCourseRecommendations(Long userId) {
        logToFile(String.format("Starting course recommendation fetch for user %d", userId));

        try {
            // Get the recommendation file
            File saveFile = getRecommendationFile(userId, true);
            
            // Check if recommendations file already exists
            if (saveFile.exists()) {
                long fileAge = System.currentTimeMillis() - saveFile.lastModified();
                // If file is less than 24 hours old, use existing file
                if (fileAge < 24 * 60 * 60 * 1000) {
                    logToFile(String.format("Existing recommendations file found for user %d. Skipping fetch.", userId));
                    return true;
                }
            }

            // Log the start of API call
            logToFile(String.format("Attempting to fetch recommendations from %s/suggest_courses/%d", recommendationApiUrl, userId));
            
            // Fetch recommendations from the external API
            String url = String.format("%s/suggest_courses/%d", recommendationApiUrl, userId);
            Map<String, Object> recommendationResponse = objectMapper.convertValue(
                restTemplate.getForObject(url, Map.class),
                objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class)
            );

            if (recommendationResponse == null) {
                logToFile(String.format("No course recommendations found for user %d", userId));
                logger.warn("No course recommendations found for user {}", userId);
                return false;
            }

            // Extract recommendations with proper type safety
            List<Map<String, Object>> recommendations = objectMapper.convertValue(
                recommendationResponse.get("recommendations"),
                objectMapper.getTypeFactory().constructCollectionType(List.class, 
                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class))
            );
            
            if (recommendations == null || recommendations.isEmpty()) {
                logToFile(String.format("No course recommendations found for user %d", userId));
                logger.warn("No course recommendations found for user {}", userId);
                return false;
            }

            // Log course IDs
            List<Integer> courseIds = recommendations.stream()
                .map(rec -> (Integer) rec.get("id"))
                .collect(Collectors.toList());
            
            logToFile(String.format("Fetched %d course recommendations for user %d. Course IDs: %s", 
                recommendations.size(), userId, courseIds));

            // Write recommendations to file
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(saveFile, recommendationResponse);
            
            // Also save to alternate locations to ensure availability
            saveToAlternateLocations(saveFile, userId, recommendationResponse);

            // Log successful save
            logToFile(String.format("Successfully saved course recommendations for user %d to %s", userId, saveFile.getAbsolutePath()));
            logger.info("Successfully saved course recommendations for user {} to {}", userId, saveFile.getAbsolutePath());
            
            return true;

        } catch (Exception e) {
            // Log detailed error information
            String errorMessage = String.format("Error fetching or saving course recommendations for user %d: %s", 
                                                userId, e.getMessage());
            logToFile(errorMessage);
            logger.error(errorMessage, e);
            return false;
        } finally {
            logToFile(String.format("Completed recommendation fetch process for user %d", userId));
        }
    }
    
    /**
     * Saves the recommendation data to alternate locations to ensure it's accessible
     * from both development and runtime environments
     */
    private void saveToAlternateLocations(File primaryFile, Long userId, Map<String, Object> data) {
        try {
            // Also save to the target/classes directory to make it available at runtime
            String filename = "suggest_courses_" + userId + ".json";
            
            // Save to target/classes/static/data/data
            saveToLocation("target/classes/static/data/data", filename, data);
            
            // Save to target/classes/data/data as a fallback
            saveToLocation("target/classes/data/data", filename, data);
            
            // If we're in a JAR, also try to save to the extracted location
            try {
                // Try to use resourceLoader to get classpath resources
                Resource classPathResource = resourceLoader.getResource("classpath:");
                if (classPathResource.exists() && classPathResource.isFile()) {
                    File classPathDir = classPathResource.getFile();
                    Path staticDataDir = Paths.get(classPathDir.getAbsolutePath(), "static", "data", "data");
                    Files.createDirectories(staticDataDir);
                    Files.copy(primaryFile.toPath(), staticDataDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
                    logger.debug("Saved to classpath location: {}", staticDataDir.resolve(filename));
                }
            } catch (Exception e) {
                logger.debug("Could not save to classpath location (expected in JAR): {}", e.getMessage());
            }
            
        } catch (Exception e) {
            logger.warn("Failed to save recommendation data to alternate locations: {}", e.getMessage());
        }
    }
    
    /**
     * Saves data to a specific location
     */
    private void saveToLocation(String directory, String filename, Map<String, Object> data) {
        try {
            Path dirPath = Paths.get(directory);
            Files.createDirectories(dirPath);
            File file = dirPath.resolve(filename).toFile();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, data);
            logger.debug("Saved recommendation data to {}", file.getAbsolutePath());
        } catch (Exception e) {
            logger.debug("Failed to save to {}: {}", directory, e.getMessage());
        }
    }
}
