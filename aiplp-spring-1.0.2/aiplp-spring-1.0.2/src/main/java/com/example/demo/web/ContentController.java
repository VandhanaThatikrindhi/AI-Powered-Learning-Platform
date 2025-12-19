package com.example.demo.web;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

@Controller
@RequestMapping("/view")
public class ContentController {
    private static final Logger logger = LoggerFactory.getLogger(ContentController.class);
    private final ResourceLoader resourceLoader;

    public ContentController(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @GetMapping("/content/{userId}/{courseId}/{activityId}")
    public String showContent(
            @PathVariable String userId,
            @PathVariable String courseId,
            @PathVariable String activityId,
            Model model) {
        
        logger.debug("Received request for content - userId: {}, courseId: {}, activityId: {}", 
                userId, courseId, activityId);
        
        try {
            // First try the static/data/data path
            String resourcePath = String.format("classpath:static/data/data/learning_paths/content/%s/%s/%s/content.json",
                    userId, courseId, activityId);
            Resource resource = resourceLoader.getResource(resourcePath);
            
            // If not found, try to fetch from localhost
            if (!resource.exists()) {
                // Prepare the directory for saving
                File contentDir = new File("src/main/resources/static/data/data/learning_paths/content/" + 
                        userId + "/" + courseId + "/" + activityId);
                contentDir.mkdirs();
                
                // Construct the URL for fetching content
                String contentUrl = String.format("http://localhost:8000/course_content/%s/%s/%s", 
                        userId, courseId, activityId);
                
                // Prepare the file path for saving
                File contentFile = new File(contentDir, "content.json");
                
                try {
                    // Download the content
                    URL url = new URL(contentUrl);
                    try (
                        InputStream inputStream = url.openStream();
                        ReadableByteChannel readableByteChannel = Channels.newChannel(inputStream);
                        FileOutputStream fileOutputStream = new FileOutputStream(contentFile)
                    ) {
                        fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
                        
                        logger.info("Successfully downloaded content for activity: {}", activityId);
                    }
                    
                    // Reload the resource
                    resource = resourceLoader.getResource("file:" + contentFile.getAbsolutePath());
                } catch (Exception e) {
                    logger.error("Failed to fetch content from localhost for activity: " + activityId, e);
                    model.addAttribute("error", "Unable to retrieve content. Please try again later.");
                    return "error";
                }
            }
            
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            logger.info("Successfully loaded content for activity: {}", activityId);
            
            // Add attributes to the model
            model.addAttribute("contentPath", String.format("%s/%s/%s", userId, courseId, activityId));
            model.addAttribute("content", content);
            model.addAttribute("activityId", activityId);
            model.addAttribute("rawJsonUrl", String.format("/data/data/learning_paths/content/%s/%s/%s/content.json", 
                    userId, courseId, activityId));
            
            return "content";
            
        } catch (IOException e) {
            logger.error("Error reading content for activity: " + activityId, e);
            model.addAttribute("error", "There was an error loading this content. Please try again later.");
            return "error";
        }
    }
}
