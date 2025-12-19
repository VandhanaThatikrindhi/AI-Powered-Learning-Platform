package com.example.demo.web;

import com.example.demo.service.LearningPathVerificationService;
import com.example.demo.service.RecommendationService;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
public class LearningPathController {

    private final LearningPathVerificationService learningPathVerificationService;
    private final RecommendationService recommendationService;
    private final UserService userService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public LearningPathController(
        LearningPathVerificationService learningPathVerificationService,
        RecommendationService recommendationService,
        UserService userService
    ) {
        this.learningPathVerificationService = learningPathVerificationService;
        this.recommendationService = recommendationService;
        this.userService = userService;
    }

    @PostMapping("/verify-learning-paths")
    public ResponseEntity<Boolean> verifyLearningPaths(@RequestBody Map<String, Object> payload, Authentication authentication) {
        Long userId = Long.parseLong(payload.get("userId").toString());
        
        // Convert course IDs from strings to integers
        List<String> courseIdStrings = objectMapper.convertValue(payload.get("courseIds"),
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        List<Integer> courseIds = courseIdStrings.stream()
            .map(Integer::parseInt)
            .collect(Collectors.toList());

        // First, verify and fetch learning paths
        boolean allPathsExist = learningPathVerificationService.verifyAndFetchLearningPaths(userId);

        if (allPathsExist) {
            // If all paths exist, assign courses to the user
            String userEmail = authentication.getName();
            var user = userService.findByEmail(userEmail);

            // Assign each course to the user
            for (Integer courseId : courseIds) {
                recommendationService.assignCourseToUser(user, courseId.longValue(), false);
            }
        }

        return ResponseEntity.ok(allPathsExist);
    }
}
