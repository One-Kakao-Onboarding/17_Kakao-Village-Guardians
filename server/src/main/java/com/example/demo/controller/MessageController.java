package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.request.AddReactionRequest;
import com.example.demo.dto.request.SendMessageRequest;
import com.example.demo.dto.response.MessageResponse;
import com.example.demo.service.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class MessageController {

    private static final Logger logger = LoggerFactory.getLogger(MessageController.class);
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/chatrooms/{roomId}/messages")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getMessages(@PathVariable Long roomId) {
        logger.info("GET /api/v1/chatrooms/{}/messages - Fetching messages", roomId);
        List<MessageResponse> messages = messageService.getMessages(roomId);
        logger.info("Retrieved {} messages for chatRoom: {}", messages.size(), roomId);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    @PostMapping("/chatrooms/{roomId}/messages")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @PathVariable Long roomId,
            @RequestBody SendMessageRequest request) {
        logger.info("POST /api/v1/chatrooms/{}/messages - Sending message", roomId);
        MessageResponse message = messageService.sendMessage(roomId, request);
        logger.info("Message created with ID: {}", message.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(message));
    }

    @PostMapping("/chatrooms/{roomId}/read")
    public ResponseEntity<ApiResponse<Void>> markMessagesAsRead(@PathVariable Long roomId) {
        logger.info("POST /api/v1/chatrooms/{}/read - Marking messages as read", roomId);
        messageService.markMessagesAsRead(roomId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/messages/{messageId}/reactions")
    public ResponseEntity<ApiResponse<MessageResponse>> addReaction(
            @PathVariable Long messageId,
            @RequestBody AddReactionRequest request) {
        logger.info("POST /api/v1/messages/{}/reactions - Adding reaction: {}", messageId, request.getEmoji());
        MessageResponse message = messageService.addReaction(messageId, request.getEmoji());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(message));
    }

    @GetMapping("/chatrooms/{roomId}/messages/poll")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> pollMessages(
            @PathVariable Long roomId,
            @RequestParam String since) {
        logger.info("GET /api/v1/chatrooms/{}/messages/poll - Polling for new messages since {}", roomId, since);
        LocalDateTime sinceTime = LocalDateTime.parse(since);
        List<MessageResponse> newMessages = messageService.pollNewMessages(roomId, sinceTime);
        logger.info("Found {} new messages", newMessages.size());
        return ResponseEntity.ok(ApiResponse.success(newMessages));
    }
}
