package com.example.demo.service;

import com.example.demo.model.User;
import com.example.demo.model.CourseProgress;
import com.example.demo.repository.CourseProgressRepository;
import com.example.demo.dto.CourseRecommendation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class RecommendationService {
    private static final Logger logger = LoggerFactory.getLogger(RecommendationService.class);

    @Autowired
    private UserService userService;

    @Autowired
    private CourseProgressRepository courseProgressRepository;
    
    @Autowired
    private CourseRecommendationService courseRecommendationService;

    public boolean isCompletedCourse(User user, String courseId) {
        if (courseId == null) return false;
        Optional<CourseProgress> progress = courseProgressRepository.findByUserIdAndCourseId(user.getId(), courseId);
        return progress.isPresent() && progress.get().getProgressPercentage() >= 100;
    }

    public List<CourseRecommendation> getRecommendationsForUser(User user) {
        try {
            // First ensure we have recommendations for this user
            if (!courseRecommendationService.ensureRecommendations(user.getId())) {
                logger.warn("Failed to ensure recommendations for user {}", user.getId());
                return Collections.emptyList();
            }
            
            // Get the recommendations from the service
            List<Map<String, Object>> recommendationsData = courseRecommendationService.getRecommendedCourses(user.getId());
            
            if (recommendationsData.isEmpty()) {
                logger.warn("No recommendations found for user {}", user.getId());
                return Collections.emptyList();
            }

            // Get user's current courses as integers
            Set<Long> userCourses = new HashSet<>();
            if (user.getCourseSelected1() != null) userCourses.add(Long.parseLong(user.getCourseSelected1()));
            if (user.getCourseSelected2() != null) userCourses.add(Long.parseLong(user.getCourseSelected2()));
            if (user.getCourseSelected3() != null) userCourses.add(Long.parseLong(user.getCourseSelected3()));

            // Convert to CourseRecommendation objects, excluding user's current courses
            List<CourseRecommendation> recommendations = new ArrayList<>();
            for (Map<String, Object> courseData : recommendationsData) {
                Long courseId = ((Number) courseData.get("id")).longValue();
                if (!userCourses.contains(courseId)) {
                    recommendations.add(new CourseRecommendation(
                        courseId,
                        (String) courseData.get("title"),
                        (String) courseData.get("description"),
                        ((Number) courseData.get("confidence_score")).doubleValue()
                    ));
                }
            }

            logger.info("Returning {} filtered recommendations for user {}", recommendations.size(), user.getId());
            return recommendations;
        } catch (Exception e) {
            logger.error("Error getting recommendations for user {}: {}", user.getId(), e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Forces a refresh of recommendations for a user
     * @param user The user
     * @return true if refresh was successful
     */
    public boolean refreshRecommendations(User user) {
        return courseRecommendationService.refreshRecommendations(user.getId());
    }

    @Transactional
    public boolean assignCourseToUser(User user, Long courseId, Boolean replaceCompleted) {
        String courseIdStr = courseId.toString();
        
        // If replacing completed course
        if (Boolean.TRUE.equals(replaceCompleted)) {
            // Check each slot for a completed course
            if (isCompletedCourse(user, user.getCourseSelected1())) {
                user.setCourseSelected1(courseIdStr);
                user.setCourse1Progress(0);
                userService.updateUser(user);
                return true;
            }
            if (isCompletedCourse(user, user.getCourseSelected2())) {
                user.setCourseSelected2(courseIdStr);
                user.setCourse2Progress(0);
                userService.updateUser(user);
                return true;
            }
            if (isCompletedCourse(user, user.getCourseSelected3())) {
                user.setCourseSelected3(courseIdStr);
                user.setCourse3Progress(0);
                userService.updateUser(user);
                return true;
            }
            return false;
        }
        
        // If not replacing, find first empty slot
        if (user.getCourseSelected1() == null) {
            user.setCourseSelected1(courseIdStr);
            user.setCourse1Progress(0);
        } else if (user.getCourseSelected2() == null) {
            user.setCourseSelected2(courseIdStr);
            user.setCourse2Progress(0);
        } else if (user.getCourseSelected3() == null) {
            user.setCourseSelected3(courseIdStr);
            user.setCourse3Progress(0);
        } else {
            return false; // No empty slots
        }

        userService.updateUser(user);
        return true;
    }
}
