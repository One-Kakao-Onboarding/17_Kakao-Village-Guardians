package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.request.ProfileRequest;
import com.example.demo.dto.response.ProfileResponse;
import com.example.demo.service.ProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/profiles")
public class ProfileController {

    private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);
    private final ProfileService profileService;

    @Autowired
    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProfileResponse>>> getUserProfiles() {
        logger.info("GET /api/v1/profiles - Fetching user's profiles");
        List<ProfileResponse> profiles = profileService.getUserProfiles();
        return ResponseEntity.ok(ApiResponse.success(profiles));
    }

    @GetMapping("/{profileId}")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(@PathVariable Long profileId) {
        logger.info("GET /api/v1/profiles/{} - Fetching profile", profileId);
        ProfileResponse profile = profileService.getProfile(profileId);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> createProfile(@RequestBody ProfileRequest request) {
        logger.info("POST /api/v1/profiles - Creating new profile");
        ProfileResponse profile = profileService.createProfile(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(profile));
    }

    @PutMapping("/{profileId}")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @PathVariable Long profileId,
            @RequestBody ProfileRequest request) {
        logger.info("PUT /api/v1/profiles/{} - Updating profile", profileId);
        ProfileResponse profile = profileService.updateProfile(profileId, request);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @DeleteMapping("/{profileId}")
    public ResponseEntity<ApiResponse<Void>> deleteProfile(@PathVariable Long profileId) {
        logger.info("DELETE /api/v1/profiles/{} - Deleting profile", profileId);
        profileService.deleteProfile(profileId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
