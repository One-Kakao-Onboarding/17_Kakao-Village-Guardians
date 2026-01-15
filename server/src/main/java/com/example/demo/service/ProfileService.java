package com.example.demo.service;

import com.example.demo.dto.request.ProfileRequest;
import com.example.demo.dto.response.ProfileResponse;
import com.example.demo.entity.Profile;
import com.example.demo.entity.User;
import com.example.demo.exception.EntityNotFoundException;
import com.example.demo.exception.InvalidImageFormatException;
import com.example.demo.repository.ProfileRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProfileService {

    private static final Logger logger = LoggerFactory.getLogger(ProfileService.class);
    private static final List<String> SUPPORTED_IMAGE_FORMATS = Arrays.asList("png", "jpeg", "jpg", "gif");

    private final ProfileRepository profileRepository;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    @Autowired
    public ProfileService(ProfileRepository profileRepository,
                          UserService userService,
                          ObjectMapper objectMapper) {
        this.profileRepository = profileRepository;
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    public List<ProfileResponse> getUserProfiles() {
        User currentUser = userService.getCurrentUser();
        List<Profile> profiles = profileRepository.findByUserId(currentUser.getId());

        // Ensure "전체" (all) profile exists
        Profile defaultProfile = profiles.stream()
                .filter(Profile::getIsDefault)
                .findFirst()
                .orElse(null);

        if (defaultProfile == null) {
            // Create default "전체" profile if it doesn't exist
            defaultProfile = createDefaultProfile(currentUser);
            profiles.add(0, defaultProfile);
        }

        // Sort profiles: default profile first, then others
        profiles.sort((p1, p2) -> {
            if (p1.getIsDefault()) return -1;
            if (p2.getIsDefault()) return 1;
            return 0;
        });

        return profiles.stream()
                .map(this::toProfileResponse)
                .collect(Collectors.toList());
    }

    private Profile createDefaultProfile(User user) {
        Profile defaultProfile = new Profile(user, user.getName());
        defaultProfile.setDescription("기본 프로필");
        defaultProfile.setDefaultPersona("casual-polite");
        defaultProfile.setIsDefault(true);
        defaultProfile.setLinkedChatRoomIds(null);  // 기본 프로필은 다른 프로필에 속하지 않은 모든 채팅방 표시

        // 기본 프로필 아바타 설정 (사용자 이름 첫 글자)
        String avatarName = user.getName().substring(0, 1).toUpperCase();
        defaultProfile.setAvatar(String.format(
            "https://ui-avatars.com/api/?name=%s&size=200&background=4F46E5&color=ffffff&bold=true",
            avatarName
        ));

        return profileRepository.save(defaultProfile);
    }

    public ProfileResponse getProfile(Long profileId) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new EntityNotFoundException("Profile not found with id: " + profileId));

        User currentUser = userService.getCurrentUser();
        if (!profile.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("You can only access your own profiles");
        }

        return toProfileResponse(profile);
    }

    public ProfileResponse createProfile(ProfileRequest request) {
        User currentUser = userService.getCurrentUser();
        logger.info("Creating profile for user: {} (LDAP: {})", currentUser.getName(), currentUser.getLdap());

        Profile profile = new Profile(currentUser, request.getName());

        // Handle avatar
        if (request.getAvatar() != null && !request.getAvatar().trim().isEmpty()) {
            profile.setAvatar(request.getAvatar());
            logger.info("Using provided avatar for profile");
        }

        profile.setDescription(request.getDescription());
        profile.setDefaultPersona(request.getDefaultPersona());
        profile.setIsDefault(false);

        if (request.getAssignedFriends() != null) {
            try {
                // Convert String IDs to Long IDs for storage
                List<Long> linkedChatRoomIds = request.getAssignedFriends().stream()
                        .map(Long::valueOf)
                        .collect(Collectors.toList());
                String linkedChatRoomIdsJson = objectMapper.writeValueAsString(linkedChatRoomIds);
                profile.setLinkedChatRoomIds(linkedChatRoomIdsJson);

                // 새로 생성된 프로필에 채팅방을 할당하기 전에 저장
                Profile savedProfile = profileRepository.save(profile);

                // 다른 프로필에서 이 채팅방들을 제거 (중복 방지)
                removeFromOtherProfiles(currentUser.getId(), savedProfile.getId(), linkedChatRoomIds);

                logger.info("Profile created - ID: {}, Name: {}, User: {}, Assigned {} chat rooms",
                           savedProfile.getId(), savedProfile.getName(), currentUser.getId(), linkedChatRoomIds.size());

                return toProfileResponse(savedProfile);
            } catch (JsonProcessingException e) {
                logger.error("Failed to serialize assignedFriends", e);
            }
        }

        Profile savedProfile = profileRepository.save(profile);
        logger.info("Profile created - ID: {}, Name: {}, User: {}",
                    savedProfile.getId(), savedProfile.getName(), currentUser.getId());

        return toProfileResponse(savedProfile);
    }

    public ProfileResponse updateProfile(Long profileId, ProfileRequest request) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new EntityNotFoundException("Profile not found with id: " + profileId));

        User currentUser = userService.getCurrentUser();
        if (!profile.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("You can only update your own profiles");
        }

        logger.info("Updating profile {} for user: {} (LDAP: {})",
                    profileId, currentUser.getName(), currentUser.getLdap());

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            logger.info("Updating profile name from '{}' to '{}'", profile.getName(), request.getName());
            profile.setName(request.getName());
        }

        if (request.getAvatar() != null && !request.getAvatar().trim().isEmpty()) {
            logger.info("Updating profile avatar (length: {} characters)", request.getAvatar().length());
            profile.setAvatar(request.getAvatar());
        }

        if (request.getDescription() != null) {
            profile.setDescription(request.getDescription());
        }
        if (request.getDefaultPersona() != null) {
            profile.setDefaultPersona(request.getDefaultPersona());
        }
        if (request.getAssignedFriends() != null) {
            try {
                // Convert String IDs to Long IDs for storage
                List<Long> linkedChatRoomIds = request.getAssignedFriends().stream()
                        .map(Long::valueOf)
                        .collect(Collectors.toList());
                String linkedChatRoomIdsJson = objectMapper.writeValueAsString(linkedChatRoomIds);
                profile.setLinkedChatRoomIds(linkedChatRoomIdsJson);

                // 다른 프로필에서 이 채팅방들을 제거 (중복 방지)
                removeFromOtherProfiles(currentUser.getId(), profileId, linkedChatRoomIds);

                logger.info("Profile {} assigned {} chat rooms, removed from other profiles",
                           profileId, linkedChatRoomIds.size());
            } catch (JsonProcessingException e) {
                logger.error("Failed to serialize assignedFriends", e);
            }
        }

        Profile updatedProfile = profileRepository.save(profile);
        logger.info("Profile updated successfully - ID: {}", profileId);

        return toProfileResponse(updatedProfile);
    }

    public void deleteProfile(Long profileId) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new EntityNotFoundException("Profile not found with id: " + profileId));

        User currentUser = userService.getCurrentUser();
        if (!profile.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("You can only delete your own profiles");
        }

        // Prevent deletion of default "전체" profile
        if (profile.getIsDefault()) {
            throw new IllegalArgumentException("Cannot delete the default '전체' profile");
        }

        profileRepository.deleteById(profileId);
        logger.info("Profile deleted - ID: {}", profileId);
    }

    private ProfileResponse toProfileResponse(Profile profile) {
        ProfileResponse response = new ProfileResponse();
        response.setId(profile.getId());
        response.setUserId(profile.getUser().getId());
        response.setName(profile.getName());
        response.setAvatar(profile.getAvatar());
        response.setDescription(profile.getDescription());
        response.setDefaultPersona(profile.getDefaultPersona());
        response.setIsDefault(profile.getIsDefault());

        if (profile.getLinkedChatRoomIds() != null && !profile.getLinkedChatRoomIds().trim().isEmpty()) {
            try {
                List<Long> linkedChatRoomIds = objectMapper.readValue(
                        profile.getLinkedChatRoomIds(),
                        new TypeReference<List<Long>>() {}
                );
                // Convert Long IDs to String IDs for assignedFriends
                List<String> assignedFriends = linkedChatRoomIds.stream()
                        .map(String::valueOf)
                        .collect(Collectors.toList());
                response.setAssignedFriends(assignedFriends);
            } catch (JsonProcessingException e) {
                logger.error("Failed to parse linkedChatRoomIds JSON", e);
                response.setAssignedFriends(new ArrayList<>());
            }
        } else {
            response.setAssignedFriends(new ArrayList<>());
        }

        return response;
    }

    private void validateImageFormat(String avatar) {
        // 모든 이미지 형식 허용
        logger.debug("Image format validation skipped - all formats allowed");
    }

    /**
     * 다른 프로필에서 특정 채팅방들을 제거 (중복 방지)
     *
     * @param userId 사용자 ID
     * @param currentProfileId 현재 프로필 ID (이 프로필은 제외)
     * @param chatRoomIds 제거할 채팅방 ID 목록
     */
    private void removeFromOtherProfiles(Long userId, Long currentProfileId, List<Long> chatRoomIds) {
        if (chatRoomIds == null || chatRoomIds.isEmpty()) {
            return;
        }

        List<Profile> allProfiles = profileRepository.findByUserId(userId);

        for (Profile profile : allProfiles) {
            // 현재 프로필이거나 기본 프로필은 제외
            if (profile.getId().equals(currentProfileId) || profile.getIsDefault()) {
                continue;
            }

            // linkedChatRoomIds가 있는지 확인
            if (profile.getLinkedChatRoomIds() != null && !profile.getLinkedChatRoomIds().trim().isEmpty()) {
                try {
                    List<Long> linkedIds = objectMapper.readValue(
                            profile.getLinkedChatRoomIds(),
                            new TypeReference<List<Long>>() {}
                    );

                    // 제거할 채팅방 ID를 필터링
                    List<Long> updatedIds = linkedIds.stream()
                            .filter(id -> !chatRoomIds.contains(id))
                            .collect(Collectors.toList());

                    // 변경사항이 있으면 저장
                    if (updatedIds.size() != linkedIds.size()) {
                        String updatedJson = objectMapper.writeValueAsString(updatedIds);
                        profile.setLinkedChatRoomIds(updatedJson);
                        profileRepository.save(profile);

                        logger.info("Removed {} chat rooms from profile {} (remaining: {})",
                                   linkedIds.size() - updatedIds.size(), profile.getId(), updatedIds.size());
                    }
                } catch (JsonProcessingException e) {
                    logger.error("Failed to process linkedChatRoomIds for profile {}", profile.getId(), e);
                }
            }
        }
    }
}
