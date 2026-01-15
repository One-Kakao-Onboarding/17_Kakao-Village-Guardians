package com.example.demo.service;

import com.example.demo.dto.request.SendMessageRequest;
import com.example.demo.dto.response.*;
import com.example.demo.entity.ChatRoom;
import com.example.demo.entity.ChatRoomMember;
import com.example.demo.entity.Message;
import com.example.demo.entity.Profile;
import com.example.demo.entity.MessageRead;
import com.example.demo.entity.Reaction;
import com.example.demo.entity.User;
import com.example.demo.exception.EntityNotFoundException;
import com.example.demo.repository.ChatRoomMemberRepository;
import com.example.demo.repository.ChatRoomRepository;
import com.example.demo.repository.MessageReadRepository;
import com.example.demo.repository.MessageRepository;
import com.example.demo.repository.ProfileRepository;
import com.example.demo.repository.ReactionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class MessageService {
    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);
    private final MessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ProfileRepository profileRepository;
    private final ReactionRepository reactionRepository;
    private final MessageReadRepository messageReadRepository;
    private final UserService userService;
    private final AIService aiService;

    @Autowired
    public MessageService(MessageRepository messageRepository,
                          ChatRoomRepository chatRoomRepository,
                          ChatRoomMemberRepository chatRoomMemberRepository,
                          ProfileRepository profileRepository,
                          ReactionRepository reactionRepository,
                          MessageReadRepository messageReadRepository,
                          UserService userService,
                          AIService aiService) {
        this.messageRepository = messageRepository;
        this.chatRoomRepository = chatRoomRepository;
        this.chatRoomMemberRepository = chatRoomMemberRepository;
        this.profileRepository = profileRepository;
        this.reactionRepository = reactionRepository;
        this.messageReadRepository = messageReadRepository;
        this.userService = userService;
        this.aiService = aiService;
    }

    public List<MessageResponse> getMessages(Long chatRoomId) {
        User currentUser = userService.getCurrentUser();
        logger.info("Retrieving messages for chatRoom: {} by user: {}", chatRoomId, currentUser.getLdap());

        // 채팅방 멤버십 확인
        ChatRoomMember member = chatRoomMemberRepository
                .findByChatRoomIdAndUserId(chatRoomId, currentUser.getId())
                .orElseThrow(() -> new EntityNotFoundException("User is not a member of this chat room"));

        logger.info("User {} is a member of chatRoom {}", currentUser.getLdap(), chatRoomId);

        List<Message> messages = messageRepository.findByChatRoomIdOrderByTimestampAsc(chatRoomId);
        logger.info("Retrieved {} messages for chatRoom: {}", messages.size(), chatRoomId);

        if (!messages.isEmpty()) {
            logger.info("First message: {} by {}", messages.get(0).getContent(), messages.get(0).getSender().getLdap());
            logger.info("Last message: {} by {}", messages.get(messages.size() - 1).getContent(), messages.get(messages.size() - 1).getSender().getLdap());
        }

        // 상대방이 읽은 메시지 ID 가져오기
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new EntityNotFoundException("ChatRoom not found"));
        User otherUser = getOtherUser(chatRoom, currentUser);
        List<Long> readMessageIds = messageReadRepository.findReadMessageIdsByChatRoomAndUser(chatRoomId, otherUser.getId());

        return messages.stream()
                .map(message -> {
                    MessageResponse response = toMessageResponse(message);
                    boolean isMine = message.getSender().getId().equals(currentUser.getId());
                    response.setIsMine(isMine);

                    // isRead 로직:
                    // - 내가 보낸 메시지: 상대방이 읽었는지 확인
                    // - 상대방이 보낸 메시지: 항상 true (조회 자체가 읽음을 의미)
                    if (isMine) {
                        response.setIsRead(readMessageIds.contains(message.getId()));
                    } else {
                        response.setIsRead(true);
                    }

                    return response;
                })
                .collect(Collectors.toList());
    }

    public MessageResponse sendMessage(Long chatRoomId, SendMessageRequest request) {
        User currentUser = userService.getCurrentUser();
        logger.info("Sending message to chatRoom: {} from user: {} (LDAP: {})", chatRoomId, currentUser.getName(), currentUser.getLdap());
        logger.info("Message content: '{}'", request.getContent());

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new EntityNotFoundException("ChatRoom not found with id: " + chatRoomId));

        // 채팅방 멤버십 확인
        chatRoomMemberRepository
                .findByChatRoomIdAndUserId(chatRoomId, currentUser.getId())
                .orElseThrow(() -> new EntityNotFoundException("User is not a member of this chat room"));

        logger.info("User {} is verified as member of chatRoom {}", currentUser.getLdap(), chatRoomId);

        // 프로필 찾기: profileId가 있으면 사용, 없으면 채팅방에 링크된 프로필 자동 검색
        Double formalityLevel = null;
        String personaId = null;
        String relationship = chatRoom.getRelationship();
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
        } else {
            // profileId가 없으면 chatRoomId로부터 자동 검색
            targetProfile = findProfileByChatRoomId(currentUser.getId(), chatRoomId);
            if (targetProfile != null) {
                logger.info("Auto-detected profile {} for chatRoom {}", targetProfile.getId(), chatRoomId);
            }
        }

        // 프로필에서 defaultPersona 가져오기
        if (targetProfile != null) {
            personaId = targetProfile.getDefaultPersona();
            formalityLevel = convertPersonaToFormalityLevel(personaId);
            logger.info("Using profile {} (name: {}, persona: {}, formalityLevel: {})",
                       targetProfile.getId(), targetProfile.getName(), personaId, formalityLevel);
        }

        // 프로필에서 가져오지 못한 경우 ChatRoom의 formalityLevel 사용
        if (formalityLevel == null && chatRoom.getFormalityLevel() != null) {
            formalityLevel = convertFormalityStringToInt(chatRoom.getFormalityLevel());
            logger.info("Using chatRoom formalityLevel: {}", formalityLevel);
        }

        String originalContent = request.getContent();
        String finalContent = originalContent;
        boolean wasGuarded = false;

        if (Boolean.TRUE.equals(request.getUseEmotionGuard())) {
            logger.info("Applying emotion guard...");
            EmotionGuardDetailResponse guardResponse = aiService.checkEmotionGuard(originalContent, personaId);
            if (Boolean.TRUE.equals(guardResponse.getIsAggressive())) {
                wasGuarded = true;
                if (guardResponse.getSuggestedText() != null) {
                    finalContent = guardResponse.getSuggestedText();
                    logger.info("Emotion guarded - Original: '{}', Modified: '{}'", originalContent, finalContent);
                }
            }
        }

        if (Boolean.TRUE.equals(request.getUseTransform()) && formalityLevel != null) {
            logger.info("Applying text transformation with formalityLevel: {}, personaId: {}, relationship: {}",
                    formalityLevel, personaId, relationship);
            TransformTextResponse transformResponse = aiService.transformText(finalContent, formalityLevel, relationship, personaId);
            finalContent = transformResponse.getTransformedText();
            logger.info("Text transformed - Original: '{}', Transformed: '{}', Applied persona: '{}'",
                    originalContent, finalContent, transformResponse.getAppliedPersona());
        }

        Message message = new Message(chatRoom, currentUser, finalContent);
        message.setOriginalContent(originalContent);
        message.setWasGuarded(wasGuarded);
        message.setIsEmoticon(Boolean.TRUE.equals(request.getIsEmoticon()));
        message.setEmoticonId(request.getEmoticonId());

        // profileId 저장 (상대방에게 어떤 프로필로 보냈는지 표시하기 위함)
        if (targetProfile != null) {
            message.setProfileId(targetProfile.getId());
            logger.info("Message sent with profileId: {}", targetProfile.getId());
        }

        Message savedMessage = messageRepository.save(message);
        logger.info("✓ Message saved successfully - ID: {}, ChatRoom: {}, Sender: {} ({}), Content: '{}'",
                savedMessage.getId(), chatRoomId, currentUser.getName(), currentUser.getLdap(), savedMessage.getContent());

        MessageResponse response = toMessageResponse(savedMessage);
        response.setIsMine(true); // Message is always from current user in sendMessage
        return response;
    }

    public MessageResponse addReaction(Long messageId, String emoji) {
        User currentUser = userService.getCurrentUser();
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found with id: " + messageId));

        Reaction reaction = new Reaction(message, currentUser, emoji);
        reactionRepository.save(reaction);

        logger.info("Reaction added - Message: {}, User: {}, Emoji: {}", messageId, currentUser.getId(), emoji);

        return toMessageResponse(message);
    }

    public List<MessageResponse> pollNewMessages(Long chatRoomId, LocalDateTime since) {
        User currentUser = userService.getCurrentUser();
        List<Message> newMessages = messageRepository.findNewMessages(chatRoomId, since);

        // 상대방이 읽은 메시지 ID 가져오기
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new EntityNotFoundException("ChatRoom not found"));
        User otherUser = getOtherUser(chatRoom, currentUser);
        List<Long> readMessageIds = messageReadRepository.findReadMessageIdsByChatRoomAndUser(chatRoomId, otherUser.getId());

        return newMessages.stream()
                .map(message -> {
                    MessageResponse response = toMessageResponse(message);
                    boolean isMine = message.getSender().getId().equals(currentUser.getId());
                    response.setIsMine(isMine);

                    // isRead 로직: 내가 보낸 메시지는 상대방이 읽었는지 확인, 상대방 메시지는 항상 true
                    if (isMine) {
                        response.setIsRead(readMessageIds.contains(message.getId()));
                    } else {
                        response.setIsRead(true);
                    }

                    return response;
                })
                .collect(Collectors.toList());
    }

    public void markMessagesAsRead(Long chatRoomId) {
        User currentUser = userService.getCurrentUser();
        logger.info("Marking messages as read for chatRoom: {} by user: {}", chatRoomId, currentUser.getLdap());

        // 채팅방 멤버십 확인
        chatRoomMemberRepository
                .findByChatRoomIdAndUserId(chatRoomId, currentUser.getId())
                .orElseThrow(() -> new EntityNotFoundException("User is not a member of this chat room"));

        // 해당 채팅방의 모든 메시지 가져오기
        List<Message> messages = messageRepository.findByChatRoomIdOrderByTimestampAsc(chatRoomId);

        // 현재 사용자가 아직 읽지 않은 메시지를 읽음으로 표시
        for (Message message : messages) {
            // 자기 자신이 보낸 메시지는 제외
            if (!message.getSender().getId().equals(currentUser.getId())) {
                // 이미 읽음 표시가 되어 있는지 확인
                if (!messageReadRepository.findByMessageIdAndUserId(message.getId(), currentUser.getId()).isPresent()) {
                    MessageRead messageRead = new MessageRead(message, currentUser);
                    messageReadRepository.save(messageRead);
                }
            }
        }

        logger.info("Marked {} messages as read in chatRoom: {}", messages.size(), chatRoomId);
    }

    private MessageResponse toMessageResponse(Message message) {
        MessageResponse response = new MessageResponse();
        response.setId(message.getId());

        User sender = message.getSender();
        User currentUser = userService.getCurrentUser();

        // 아바타 결정:
        // - 내가 보낸 메시지면 User의 기본 아바타
        // - 상대방이 보낸 메시지면 상대방이 이 채팅방을 링크한 프로필의 아바타
        String avatar = sender.getAvatar();

        if (!sender.getId().equals(currentUser.getId())) {
            // 상대방이 보낸 메시지인 경우, 상대방이 이 채팅방을 어떤 프로필에 링크했는지 확인
            Profile linkedProfile = findProfileByChatRoomId(sender.getId(), message.getChatRoom().getId());
            if (linkedProfile != null && linkedProfile.getAvatar() != null) {
                avatar = linkedProfile.getAvatar();
                logger.debug("Using profile avatar for message {} (profileId: {}, profileName: {})",
                        message.getId(), linkedProfile.getId(), linkedProfile.getName());
            }
        }

        // sender의 name은 항상 User의 원래 이름 (멀티프로필 이름이 아님)
        response.setSender(new UserResponse(
                sender.getId(),
                sender.getLdap(),
                sender.getName(),  // 항상 User의 원래 이름
                avatar             // 프로필 아바타 또는 User 기본 아바타
        ));

        response.setContent(message.getContent());
        response.setOriginalContent(message.getOriginalContent());
        response.setWasGuarded(message.getWasGuarded());
        response.setIsEmoticon(message.getIsEmoticon());
        response.setEmoticonId(message.getEmoticonId());
        response.setTimestamp(message.getTimestamp());

        List<Reaction> reactions = reactionRepository.findByMessageId(message.getId());
        response.setReactions(reactions.stream()
                .map(this::toReactionResponse)
                .collect(Collectors.toList()));

        return response;
    }

    private ReactionResponse toReactionResponse(Reaction reaction) {
        User user = reaction.getUser();
        UserResponse userResponse = new UserResponse(
                user.getId(),
                user.getLdap(),
                user.getName(),
                user.getAvatar()
        );
        return new ReactionResponse(
                reaction.getId(),
                userResponse,
                reaction.getEmoji(),
                reaction.getCreatedAt()
        );
    }

    /**
     * defaultPersona를 formalityLevel(0-100)로 변환
     */
    private Double convertPersonaToFormalityLevel(String persona) {
        if (persona == null) {
            return 50.0; // 기본값
        }

        switch (persona) {
            case "very-formal":
                return 90.0; // 80-100%
            case "formal":
                return 70.0; // 60-79%
            case "casual-polite":
                return 50.0; // 40-59%
            case "casual":
                return 30.0; // 20-39%
            case "very-casual":
                return 10.0; // 0-19%
            default:
                logger.warn("Unknown persona: {}, using default formalityLevel: 50.0", persona);
                return 50.0;
        }
    }

    /**
     * ChatRoom의 formalityLevel 문자열을 숫자로 변환
     */
    private Double convertFormalityStringToInt(String formalityLevel) {
        if (formalityLevel == null) {
            return 50.0;
        }

        switch (formalityLevel) {
            case "formal":
                return 70.0;
            case "informal":
                return 50.0;
            case "casual":
                return 30.0;
            default:
                return 50.0;
        }
    }

    /**
     * 채팅방에서 상대방 유저를 가져옴
     */
    private User getOtherUser(ChatRoom chatRoom, User currentUser) {
        List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoomId(chatRoom.getId());
        return members.stream()
                .map(ChatRoomMember::getUser)
                .filter(user -> !user.getId().equals(currentUser.getId()))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Other user not found in chat room"));
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
