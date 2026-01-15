package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.request.UpdateUserRequest;
import com.example.demo.dto.response.UserResponse;
import com.example.demo.entity.User;
import com.example.demo.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        logger.info("GET /api/v1/users/me - Fetching current user");
        User currentUser = userService.getCurrentUser();
        UserResponse response = new UserResponse(
                currentUser.getId(),
                currentUser.getLdap(),
                currentUser.getName(),
                currentUser.getAvatar()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateCurrentUser(@RequestBody UpdateUserRequest request) {
        logger.info("PUT /api/v1/users/me - Updating current user profile");
        User updatedUser = userService.updateCurrentUser(request);
        UserResponse response = new UserResponse(
                updatedUser.getId(),
                updatedUser.getLdap(),
                updatedUser.getName(),
                updatedUser.getAvatar()
        );
        logger.info("User profile updated successfully: {}", updatedUser.getLdap());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(@PathVariable Long id, @RequestBody User userDetails) {
        User updatedUser = userService.updateUser(id, userDetails);
        UserResponse response = new UserResponse(
                updatedUser.getId(),
                updatedUser.getLdap(),
                updatedUser.getName(),
                updatedUser.getAvatar()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
