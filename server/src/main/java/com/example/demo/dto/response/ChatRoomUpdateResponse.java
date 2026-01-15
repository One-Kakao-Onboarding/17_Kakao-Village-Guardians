package com.example.demo.dto.response;

import java.util.List;

public class ChatRoomUpdateResponse {
    private Long chatRoomId;
    private List<MessageResponse> newMessages;
    private Long unreadCount;

    public ChatRoomUpdateResponse() {
    }

    public ChatRoomUpdateResponse(Long chatRoomId, List<MessageResponse> newMessages, Long unreadCount) {
        this.chatRoomId = chatRoomId;
        this.newMessages = newMessages;
        this.unreadCount = unreadCount;
    }

    public Long getChatRoomId() {
        return chatRoomId;
    }

    public void setChatRoomId(Long chatRoomId) {
        this.chatRoomId = chatRoomId;
    }

    public List<MessageResponse> getNewMessages() {
        return newMessages;
    }

    public void setNewMessages(List<MessageResponse> newMessages) {
        this.newMessages = newMessages;
    }

    public Long getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(Long unreadCount) {
        this.unreadCount = unreadCount;
    }
}
