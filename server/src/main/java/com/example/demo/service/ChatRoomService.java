package com.example.demo.service;

import com.example.demo.dto.request.CreateChatRoomRequest;
import com.example.demo.dto.response.*;
import com.example.demo.entity.ChatRoom;
import com.example.demo.entity.ChatRoomMember;
import com.example.demo.entity.Message;
import com.example.demo.entity.Profile;
import com.example.demo.entity.User;
import com.example.demo.exception.EntityNotFoundException;
import com.example.demo.repository.ChatRoomMemberRepository;
import com.example.demo.repository.ChatRoomRepository;
import com.example.demo.repository.MessageRepository;
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
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ChatRoomService {

    private static final Logger logger = LoggerFactory.getLogger(ChatRoomService.class);
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final MessageRepository messageRepository;
    private final ProfileRepository profileRepository;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    @Autowired
    public ChatRoomService(ChatRoomRepository chatRoomRepository,
                           ChatRoomMemberRepository chatRoomMemberRepository,
                           MessageRepository messageRepository,
                           ProfileRepository profileRepository,
                           UserService userService,
                           ObjectMapper objectMapper) {
        this.chatRoomRepository = chatRoomRepository;
        this.chatRoomMemberRepository = chatRoomMemberRepository;
        this.messageRepository = messageRepository;
        this.profileRepository = profileRepository;
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    public List<ChatRoomResponse> getUserChatRooms(String profileId) {
        User currentUser = userService.getCurrentUser();
        List<ChatRoom> chatRooms = chatRoomRepository.findByUserId(currentUser.getId());

        // profileId가 없거나 "all"이면 모든 채팅방 반환
        if (profileId != null && !profileId.equals("all")) {
            try {
                Long profileIdLong = Long.valueOf(profileId);
                Profile profile = profileRepository.findById(profileIdLong).orElse(null);

                if (profile != null) {
                    if (profile.getIsDefault()) {
                        // 기본 프로필: 다른 프로필에 속하지 않은 채팅방만 반환
                        List<Profile> allProfiles = profileRepository.findByUserId(currentUser.getId());
                        List<Long> assignedToOtherProfiles = new ArrayList<>();

                        for (Profile p : allProfiles) {
                            // 기본 프로필이 아니고, linkedChatRoomIds가 있는 경우
                            if (!p.getIsDefault() && p.getLinkedChatRoomIds() != null && !p.getLinkedChatRoomIds().trim().isEmpty()) {
                                List<Long> linkedIds = objectMapper.readValue(
                                        p.getLinkedChatRoomIds(),
                                        new TypeReference<List<Long>>() {}
                                );
                                assignedToOtherProfiles.addAll(linkedIds);
                            }
                        }

                        // 다른 프로필에 할당되지 않은 채팅방만 필터링
                        final List<Long> excludeIds = assignedToOtherProfiles;
                        chatRooms = chatRooms.stream()
                                .filter(room -> !excludeIds.contains(room.getId()))
                                .collect(Collectors.toList());

                        logger.debug("Default profile {}: showing {} chat rooms (excluding {} assigned to other profiles)",
                                profileId, chatRooms.size(), excludeIds.size());
                    } else {
                        // 일반 프로필: linkedChatRoomIds에 지정된 채팅방만 반환
                        if (profile.getLinkedChatRoomIds() != null && !profile.getLinkedChatRoomIds().trim().isEmpty()) {
                            List<Long> assignedChatRoomIds = objectMapper.readValue(
                                    profile.getLinkedChatRoomIds(),
                                    new TypeReference<List<Long>>() {}
                            );

                            chatRooms = chatRooms.stream()
                                    .filter(room -> assignedChatRoomIds.contains(room.getId()))
                                    .collect(Collectors.toList());

                            logger.debug("Profile {} (non-default): showing {} assigned chat rooms", profileId, chatRooms.size());
                        } else {
                            // linkedChatRoomIds가 없으면 빈 목록
                            chatRooms = new ArrayList<>();
                            logger.debug("Profile {} has no linkedChatRoomIds, showing 0 chat rooms", profileId);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to filter chat rooms by profileId: {}", profileId, e);
            }
        } else {
            logger.debug("No profileId or profileId=all, showing all chat rooms");
        }

        return chatRooms.stream()
                .map(room -> {
                    ChatRoomResponse response = toChatRoomResponse(room, currentUser);

                    ChatRoomMember currentMember = chatRoomMemberRepository
                            .findByChatRoomIdAndUserId(room.getId(), currentUser.getId())
                            .orElse(null);

                    if (currentMember != null) {
                        Long unreadCount = chatRoomMemberRepository.countUnreadMessages(
                                room.getId(),
                                currentMember.getLastReadMessageId(),
                                currentUser.getId()
                        );
                        response.setUnreadCount(unreadCount);
                        logger.debug("Chat room {} unread count: {} (lastReadMessageId: {})",
                                room.getId(), unreadCount, currentMember.getLastReadMessageId());
                    } else {
                        response.setUnreadCount(0L);
                    }

                    return response;
                })
                // 최신 메시지 순으로 정렬 (null은 맨 뒤로)
                .sorted(Comparator.comparing(
                        ChatRoomResponse::getLastMessageTime,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .collect(Collectors.toList());
    }

    public ChatRoomResponse getChatRoomDetail(Long chatRoomId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new EntityNotFoundException("ChatRoom not found with id: " + chatRoomId));

        User currentUser = userService.getCurrentUser();

        ChatRoomMember currentMember = chatRoomMemberRepository
                .findByChatRoomIdAndUserId(chatRoomId, currentUser.getId())
                .orElseThrow(() -> new EntityNotFoundException("User is not a member of this chat room"));

        ChatRoomResponse response = toChatRoomResponse(chatRoom, currentUser);

        List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoomId(chatRoomId);
        response.setMembers(members.stream()
                .map(this::toMemberResponse)
                .collect(Collectors.toList()));

        Long unreadCount = chatRoomMemberRepository.countUnreadMessages(
                chatRoomId,
                currentMember.getLastReadMessageId(),
                currentUser.getId()
        );
        response.setUnreadCount(unreadCount);
        logger.debug("Chat room {} detail - unread count: {} (lastReadMessageId: {})",
                chatRoomId, unreadCount, currentMember.getLastReadMessageId());

        return response;
    }

    public void deleteChatRoom(Long chatRoomId) {
        User currentUser = userService.getCurrentUser();

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new EntityNotFoundException("ChatRoom not found with id: " + chatRoomId));

        // 채팅방 멤버인지 확인
        chatRoomMemberRepository
                .findByChatRoomIdAndUserId(chatRoomId, currentUser.getId())
                .orElseThrow(() -> new EntityNotFoundException("User is not a member of this chat room"));

        logger.info("Deleting chat room {} by user: {} (LDAP: {})",
                chatRoomId, currentUser.getName(), currentUser.getLdap());

        // 1. 채팅방의 모든 메시지 삭제 (reactions도 함께 삭제됨)
        List<Message> messages = messageRepository.findByChatRoomIdOrderByTimestampAsc(chatRoomId);
        for (Message message : messages) {
            // Reaction 먼저 삭제
            messageRepository.delete(message);
        }

        // 2. 채팅방 멤버 삭제
        List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoomId(chatRoomId);
        chatRoomMemberRepository.deleteAll(members);

        // 3. 채팅방 삭제
        chatRoomRepository.delete(chatRoom);

        logger.info("Chat room {} successfully deleted", chatRoomId);
    }

    public void markAsRead(Long chatRoomId, Long messageId) {
        User currentUser = userService.getCurrentUser();

        ChatRoomMember member = chatRoomMemberRepository
                .findByChatRoomIdAndUserId(chatRoomId, currentUser.getId())
                .orElseThrow(() -> new EntityNotFoundException("User is not a member of this chat room"));

        // If messageId is null, mark all messages as read by using the latest message ID
        Long targetMessageId = messageId;
        if (targetMessageId == null) {
            Optional<Message> latestMessage = messageRepository.findFirstByChatRoomIdOrderByTimestampDesc(chatRoomId);
            if (latestMessage.isPresent()) {
                targetMessageId = latestMessage.get().getId();
                logger.info("No messageId provided, using latest message ID: {}", targetMessageId);
            } else {
                logger.info("No messages in room {}, skipping mark as read", chatRoomId);
                return; // No messages to mark as read
            }
        }

        member.setLastReadMessageId(targetMessageId);
        chatRoomMemberRepository.save(member);

        logger.info("User {} marked messages up to {} as read in room {}",
                currentUser.getId(), targetMessageId, chatRoomId);
    }

    public ChatRoomResponse createChatRoom(CreateChatRoomRequest request) {
        User currentUser = userService.getCurrentUser();

        // 친구 찾기 또는 생성
        User friend = userService.findOrCreateUserByLdap(request.getFriendLdap());

        if (friend.getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Cannot create chat room with yourself");
        }

        // 이미 두 사용자 간 1:1 채팅방이 있는지 확인
        List<ChatRoom> currentUserRooms = chatRoomRepository.findByUserId(currentUser.getId());
        for (ChatRoom room : currentUserRooms) {
            if (!room.getIsGroup()) {
                List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoomId(room.getId());
                if (members.size() == 2) {
                    boolean hasFriend = members.stream()
                            .anyMatch(m -> m.getUser().getId().equals(friend.getId()));
                    if (hasFriend) {
                        logger.info("Chat room already exists between {} and {}",
                                currentUser.getLdap(), friend.getLdap());
                        return getChatRoomDetail(room.getId());
                    }
                }
            }
        }

        // 새 채팅방 생성
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setName(friend.getName());
        chatRoom.setAvatar(friend.getAvatar());
        chatRoom.setIsGroup(false);

        // formalityLevel을 문자열로 변환 (informal, formal, casual 등)
        if (request.getFormalityLevel() != null) {
            String formalityLevel = convertFormalityLevel(request.getFormalityLevel());
            chatRoom.setFormalityLevel(formalityLevel);
        }

        chatRoom.setRelationship(request.getRelationship());

        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);
        logger.info("Created new chat room: {} between {} and {}",
                savedChatRoom.getId(), currentUser.getLdap(), friend.getLdap());

        // 두 사용자를 멤버로 추가
        ChatRoomMember member1 = new ChatRoomMember(savedChatRoom, currentUser);
        ChatRoomMember member2 = new ChatRoomMember(savedChatRoom, friend);

        chatRoomMemberRepository.save(member1);
        chatRoomMemberRepository.save(member2);

        // profileId 처리: 제공되지 않으면 기본 프로필 사용
        String targetProfileId = request.getProfileId();
        if (targetProfileId == null || targetProfileId.isEmpty()) {
            // 기본 프로필 찾기
            List<Profile> profiles = profileRepository.findByUserId(currentUser.getId());
            Profile defaultProfile = profiles.stream()
                    .filter(Profile::getIsDefault)
                    .findFirst()
                    .orElse(null);

            if (defaultProfile != null) {
                targetProfileId = defaultProfile.getId().toString();
                logger.info("No profileId provided, using default profile: {}", targetProfileId);
            }
        }

        // 프로필에 채팅방 매핑 추가
        if (targetProfileId != null && !targetProfileId.isEmpty()) {
            try {
                Long profileIdLong = Long.valueOf(targetProfileId);
                Profile profile = profileRepository.findById(profileIdLong).orElse(null);

                if (profile != null && profile.getUser().getId().equals(currentUser.getId())) {
                    List<Long> assignedChatRoomIds = new ArrayList<>();

                    // 기존 linkedChatRoomIds 가져오기
                    if (profile.getLinkedChatRoomIds() != null && !profile.getLinkedChatRoomIds().trim().isEmpty()) {
                        assignedChatRoomIds = objectMapper.readValue(
                                profile.getLinkedChatRoomIds(),
                                new TypeReference<List<Long>>() {}
                        );
                    }

                    // 새 채팅방 ID 추가
                    if (!assignedChatRoomIds.contains(savedChatRoom.getId())) {
                        assignedChatRoomIds.add(savedChatRoom.getId());
                        String updatedJson = objectMapper.writeValueAsString(assignedChatRoomIds);
                        profile.setLinkedChatRoomIds(updatedJson);
                        profileRepository.save(profile);

                        logger.info("Added chat room {} to profile {} ({})", savedChatRoom.getId(), profileIdLong,
                                   profile.getIsDefault() ? "default" : "custom");
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to add chat room to profile: {}", targetProfileId, e);
            }
        }

        // 응답 생성
        ChatRoomResponse response = toChatRoomResponse(savedChatRoom, currentUser);
        response.setUnreadCount(0L);
        response.setLastMessage(null);
        response.setLastMessageTime(null);

        return response;
    }

    private String convertFormalityLevel(Double level) {
        if (level == null) {
            return "informal";
        }
        if (level >= 80.0) {
            return "formal";
        } else if (level >= 50.0) {
            return "informal";
        } else {
            return "casual";
        }
    }

    private ChatRoomResponse toChatRoomResponse(ChatRoom chatRoom, User currentUser) {
        ChatRoomResponse response = new ChatRoomResponse();
        response.setId(chatRoom.getId());
        response.setIsGroup(chatRoom.getIsGroup());
        response.setFormalityLevel(chatRoom.getFormalityLevel());
        response.setRelationship(chatRoom.getRelationship());

        // 1:1 채팅방인 경우, 현재 사용자가 아닌 상대방의 이름과 아바타 표시
        if (!chatRoom.getIsGroup()) {
            List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoomId(chatRoom.getId());
            User otherUser = members.stream()
                    .map(ChatRoomMember::getUser)
                    .filter(user -> !user.getId().equals(currentUser.getId()))
                    .findFirst()
                    .orElse(null);

            if (otherUser != null) {
                response.setName(otherUser.getName());

                // 상대방이 이 채팅방을 어떤 프로필에 링크했는지 확인
                String avatar = otherUser.getAvatar(); // 기본값은 User의 아바타
                Profile linkedProfile = findProfileByChatRoomId(otherUser.getId(), chatRoom.getId());

                if (linkedProfile != null && linkedProfile.getAvatar() != null) {
                    avatar = linkedProfile.getAvatar();
                    logger.debug("Chat room {} - Using profile avatar (profileId: {}, profileName: {})",
                            chatRoom.getId(), linkedProfile.getId(), linkedProfile.getName());
                } else {
                    logger.debug("Chat room {} - Using user's default avatar (no profile linked)",
                            chatRoom.getId());
                }

                response.setAvatar(avatar);
                logger.debug("Chat room {} - Showing other user's name: {} (current user: {})",
                        chatRoom.getId(), otherUser.getName(), currentUser.getName());
            } else {
                // 상대방을 찾지 못한 경우 DB에 저장된 값 사용
                response.setName(chatRoom.getName());
                response.setAvatar(chatRoom.getAvatar());
            }
        } else {
            // 그룹 채팅방인 경우 DB에 저장된 이름과 아바타 사용
            response.setName(chatRoom.getName());
            response.setAvatar(chatRoom.getAvatar());
        }

        if (chatRoom.getKeywords() != null) {
            try {
                List<String> keywords = objectMapper.readValue(
                        chatRoom.getKeywords(),
                        new TypeReference<List<String>>() {}
                );
                response.setKeywords(keywords);
            } catch (JsonProcessingException e) {
                logger.error("Failed to parse keywords JSON", e);
                response.setKeywords(new ArrayList<>());
            }
        }

        // 마지막 메시지 정보 설정
        Optional<Message> lastMessage = messageRepository.findFirstByChatRoomIdOrderByTimestampDesc(chatRoom.getId());
        if (lastMessage.isPresent()) {
            response.setLastMessage(lastMessage.get().getContent());
            response.setLastMessageTime(lastMessage.get().getTimestamp());
        }

        return response;
    }

    private MemberResponse toMemberResponse(ChatRoomMember member) {
        User user = member.getUser();
        UserResponse userResponse = new UserResponse(
                user.getId(),
                user.getLdap(),
                user.getName(),
                user.getAvatar()
        );

        return new MemberResponse(
                member.getId(),
                userResponse,
                member.getLastReadMessageId(),
                member.getJoinedAt()
        );
    }

    /**
     * 특정 사용자가 특정 채팅방을 linkedChatRoomIds에 포함한 프로필을 찾음
     */
    private Profile findProfileByChatRoomId(Long userId, Long chatRoomId) {
        try {
            List<Profile> profiles = profileRepository.findByUserId(userId);
            for (Profile profile : profiles) {
                if (profile.getLinkedChatRoomIds() != null && !profile.getLinkedChatRoomIds().trim().isEmpty()) {
                    List<Long> linkedIds = objectMapper.readValue(
                            profile.getLinkedChatRoomIds(),
                            new TypeReference<List<Long>>() {}
                    );
                    if (linkedIds.contains(chatRoomId)) {
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
