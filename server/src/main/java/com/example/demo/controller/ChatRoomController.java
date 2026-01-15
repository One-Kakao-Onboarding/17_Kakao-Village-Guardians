package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.request.CreateChatRoomRequest;
import com.example.demo.dto.response.ChatRoomResponse;
import com.example.demo.dto.response.MessageOnlyResponse;
import com.example.demo.service.ChatRoomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chatrooms")
public class ChatRoomController {

    private static final Logger logger = LoggerFactory.getLogger(ChatRoomController.class);
    private final ChatRoomService chatRoomService;

    @Autowired
    public ChatRoomController(ChatRoomService chatRoomService) {
        this.chatRoomService = chatRoomService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ChatRoomResponse>>> getUserChatRooms(
            @RequestParam(required = false) String profileId) {
        logger.info("GET /api/v1/chatrooms - Fetching user's chat rooms (profileId: {})", profileId);
        List<ChatRoomResponse> chatRooms = chatRoomService.getUserChatRooms(profileId);
        logger.info("Retrieved {} chat rooms", chatRooms.size());
        return ResponseEntity.ok(ApiResponse.success(chatRooms));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ChatRoomResponse>> createChatRoom(@RequestBody CreateChatRoomRequest request) {
        logger.info("POST /api/v1/chatrooms - Creating chat room with friend: {}", request.getFriendLdap());
        ChatRoomResponse chatRoom = chatRoomService.createChatRoom(request);
        logger.info("Chat room created with ID: {}", chatRoom.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(chatRoom));
    }

    @GetMapping("/updates")
    public ResponseEntity<ApiResponse<List<ChatRoomResponse>>> getChatRoomUpdates(
            @RequestParam(required = false) String profileId) {
        logger.info("GET /api/v1/chatrooms/updates - Fetching chat room updates (profileId: {})", profileId);
        List<ChatRoomResponse> chatRooms = chatRoomService.getUserChatRooms(profileId);
        logger.info("Retrieved {} chat room updates", chatRooms.size());
        return ResponseEntity.ok(ApiResponse.success(chatRooms));
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<ApiResponse<ChatRoomResponse>> getChatRoomDetail(@PathVariable Long roomId) {
        logger.info("GET /api/v1/chatrooms/{} - Fetching chat room detail", roomId);
        ChatRoomResponse chatRoom = chatRoomService.getChatRoomDetail(roomId);
        return ResponseEntity.ok(ApiResponse.success(chatRoom));
    }

    @PutMapping("/{roomId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long roomId,
            @RequestParam(required = false) Long messageId) {
        if (messageId != null) {
            logger.info("PUT /api/v1/chatrooms/{}/read - Marking messages as read up to {}", roomId, messageId);
        } else {
            logger.info("PUT /api/v1/chatrooms/{}/read - Marking all messages as read", roomId);
        }
        chatRoomService.markAsRead(roomId, messageId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{roomId}")
    public ResponseEntity<ApiResponse<MessageOnlyResponse>> deleteChatRoom(@PathVariable Long roomId) {
        logger.info("DELETE /api/v1/chatrooms/{} - Deleting chat room", roomId);
        chatRoomService.deleteChatRoom(roomId);
        MessageOnlyResponse response = new MessageOnlyResponse("채팅방이 삭제되었습니다.");
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
