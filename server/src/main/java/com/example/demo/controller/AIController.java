package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.request.*;
import com.example.demo.dto.response.*;
import com.example.demo.entity.Message;
import com.example.demo.entity.Profile;
import com.example.demo.entity.User;
import com.example.demo.repository.MessageRepository;
import com.example.demo.repository.ProfileRepository;
import com.example.demo.service.AIService;
import com.example.demo.service.UserService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ai")
public class AIController {

    private static final Logger logger = LoggerFactory.getLogger(AIController.class);
    private final AIService aiService;
    private final ProfileRepository profileRepository;
    private final MessageRepository messageRepository;
    private final UserService userService;

    @Autowired
    public AIController(AIService aiService, ProfileRepository profileRepository, MessageRepository messageRepository, UserService userService) {
        this.aiService = aiService;
        this.profileRepository = profileRepository;
        this.messageRepository = messageRepository;
        this.userService = userService;
    }

    @PostMapping("/transform")
    public ResponseEntity<ApiResponse<TransformTextResponse>> transformText(@RequestBody TransformTextRequest request) {
        logger.info("POST /api/v1/ai/transform - Transforming text with formality: {}, profileId: {}, roomId: {}",
                   request.getFormalityLevel(), request.getProfileId(), request.getRoomId());

        User currentUser = userService.getCurrentUser();
        Double formalityLevel = request.getFormalityLevel();
        String personaId = request.getPersonaId();

        // profileId 또는 roomId로부터 프로필 찾기
        Profile targetProfile = null;

        if (request.getProfileId() != null && !request.getProfileId().isEmpty()) {
            // profileId가 명시된 경우
            try {
                Long profileIdLong = Long.valueOf(request.getProfileId());
                targetProfile = profileRepository.findById(profileIdLong).orElse(null);
                if (targetProfile != null && !targetProfile.getUser().getId().equals(currentUser.getId())) {
                    targetProfile = null; // 다른 사용자의 프로필은 무시
                }
            } catch (Exception e) {
                logger.error("Failed to get profile by profileId: {}", request.getProfileId(), e);
            }
        } else if (request.getRoomId() != null) {
            // roomId로부터 자동으로 프로필 찾기
            targetProfile = findProfileByChatRoomId(currentUser.getId(), request.getRoomId());
            if (targetProfile != null) {
                logger.info("Auto-detected profile {} for chatRoom {}", targetProfile.getId(), request.getRoomId());
            }
        }

        // 프로필이 있으면 해당 프로필의 persona 사용
        if (targetProfile != null) {
            String defaultPersona = targetProfile.getDefaultPersona();
            formalityLevel = convertPersonaToFormalityLevel(defaultPersona);
            personaId = defaultPersona;
            logger.info("Using profile {} (name: {}, persona: {}, formalityLevel: {})",
                       targetProfile.getId(), targetProfile.getName(), defaultPersona, formalityLevel);
        }

        // Validate formalityLevel
        if (formalityLevel != null && (formalityLevel < 0 || formalityLevel > 100)) {
            throw new IllegalArgumentException("격식 수준은 0-100 사이여야 합니다.");
        }

        TransformTextResponse response = aiService.transformText(
                request.getText(),
                formalityLevel,
                request.getRelationship(),
                personaId
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/emotion-guard")
    public ResponseEntity<ApiResponse<EmotionGuardDetailResponse>> checkEmotionGuard(@RequestBody EmotionGuardRequest request) {
        logger.info("POST /api/v1/ai/emotion-guard - Checking emotion guard, chatRoomId: {}, profileId: {}",
                   request.getChatRoomId(), request.getProfileId());

        User currentUser = userService.getCurrentUser();
        Profile targetProfile = null;

        // profileId 또는 chatRoomId로부터 프로필 찾기
        if (request.getProfileId() != null && !request.getProfileId().isEmpty()) {
            try {
                Long profileIdLong = Long.valueOf(request.getProfileId());
                targetProfile = profileRepository.findById(profileIdLong).orElse(null);
                if (targetProfile != null && !targetProfile.getUser().getId().equals(currentUser.getId())) {
                    targetProfile = null;
                }
            } catch (Exception e) {
                logger.error("Failed to get profile by profileId: {}", request.getProfileId(), e);
            }
        } else if (request.getChatRoomId() != null) {
            targetProfile = findProfileByChatRoomId(currentUser.getId(), request.getChatRoomId());
            if (targetProfile != null) {
                logger.info("Auto-detected profile {} for chatRoom {}", targetProfile.getId(), request.getChatRoomId());
            }
        }

        String personaId = targetProfile != null ? targetProfile.getDefaultPersona() : null;
        if (targetProfile != null) {
            logger.info("Using profile {} (name: {}, persona: {})",
                       targetProfile.getId(), targetProfile.getName(), personaId);
        }

        EmotionGuardDetailResponse response = aiService.checkEmotionGuard(request.getText(), personaId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/reaction-suggest")
    public ResponseEntity<ApiResponse<ReactionSuggestResponse>> suggestReactions(@RequestBody ReactionSuggestRequest request) {
        logger.info("POST /api/v1/ai/reaction-suggest - Suggesting reactions for message: {}, profileId: {}, chatRoomId: {}",
                   request.getMessage(), request.getProfileId(), request.getChatRoomId());

        User currentUser = userService.getCurrentUser();
        Double formalityLevel = request.getFormalityLevel();
        String relationship = request.getRelationship();

        // profileId 또는 chatRoomId로부터 프로필 찾기
        Profile targetProfile = null;

        if (request.getProfileId() != null && !request.getProfileId().isEmpty()) {
            // profileId가 명시된 경우
            try {
                Long profileIdLong = Long.valueOf(request.getProfileId());
                targetProfile = profileRepository.findById(profileIdLong).orElse(null);
                if (targetProfile != null && !targetProfile.getUser().getId().equals(currentUser.getId())) {
                    targetProfile = null; // 다른 사용자의 프로필은 무시
                }
            } catch (Exception e) {
                logger.error("Failed to get profile by profileId: {}", request.getProfileId(), e);
            }
        } else if (request.getChatRoomId() != null) {
            // chatRoomId로부터 자동으로 프로필 찾기
            targetProfile = findProfileByChatRoomId(currentUser.getId(), request.getChatRoomId());
            if (targetProfile != null) {
                logger.info("Auto-detected profile {} for chatRoom {}", targetProfile.getId(), request.getChatRoomId());
            }
        }

        // 프로필이 있으면 해당 프로필의 persona 사용
        String personaId = null;
        if (targetProfile != null) {
            personaId = targetProfile.getDefaultPersona();
            formalityLevel = convertPersonaToFormalityLevel(personaId);
            logger.info("Using profile {} (name: {}, persona: {}, formalityLevel: {})",
                       targetProfile.getId(), targetProfile.getName(), personaId, formalityLevel);
        }

        // 대화 히스토리 가져오기 (최대 100개)
        List<Message> conversationHistory = new ArrayList<>();
        if (request.getChatRoomId() != null) {
            List<Message> allMessages = messageRepository.findByChatRoomIdOrderByTimestampAsc(request.getChatRoomId());
            // 최근 100개만 가져오기
            int startIndex = Math.max(0, allMessages.size() - 100);
            conversationHistory = allMessages.subList(startIndex, allMessages.size());
            logger.info("Loaded {} messages from conversation history for reaction suggestions", conversationHistory.size());
        }

        ReactionSuggestResponse response = aiService.suggestReactions(
                request.getMessage(),
                relationship,
                formalityLevel,
                personaId,
                conversationHistory,
                currentUser
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/friend-matching")
    public ResponseEntity<ApiResponse<FriendMatchingDetailResponse>> findFriendMatches(@RequestBody FriendMatchingRequest request) {
        logger.info("POST /api/v1/ai/friend-matching - Finding chat room matches for profile: {}", request.getProfileName());
        FriendMatchingDetailResponse response = aiService.findFriendMatches(
                request.getProfileName(),
                request.getPersonaId(),
                request.getChatRoomIds()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * defaultPersona를 formalityLevel(0-100)로 변환
     */
    private Double convertPersonaToFormalityLevel(String persona) {
        if (persona == null) {
            return 50.0;
        }

        switch (persona) {
            case "very-formal":
                return 90.0;
            case "formal":
                return 70.0;
            case "casual-polite":
                return 50.0;
            case "casual":
                return 30.0;
            case "very-casual":
                return 10.0;
            default:
                logger.warn("Unknown persona: {}, using default formalityLevel: 50.0", persona);
                return 50.0;
        }
    }

    /**
     * 특정 사용자가 특정 채팅방을 linkedChatRoomIds에 포함한 프로필을 찾음
     */
    private Profile findProfileByChatRoomId(Long userId, Long chatRoomId) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Profile> profiles = profileRepository.findByUserId(userId);
            for (Profile profile : profiles) {
                if (profile.getLinkedChatRoomIds() != null && !profile.getLinkedChatRoomIds().trim().isEmpty()) {
                    List<Long> chatRoomIds = mapper.readValue(
                            profile.getLinkedChatRoomIds(),
                            new TypeReference<List<Long>>() {}
                    );
                    if (chatRoomIds.contains(chatRoomId)) {
                        return profile;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to find profile by chatRoomId: {}", chatRoomId, e);
        }
        return null;
    }
}
