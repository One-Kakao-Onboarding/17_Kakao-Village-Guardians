package com.example.demo.dto.response;

import java.time.LocalDateTime;

public class MemberResponse {
    private Long id;
    private UserResponse user;
    private Long lastReadMessageId;
    private LocalDateTime joinedAt;

    public MemberResponse() {
    }

    public MemberResponse(Long id, UserResponse user, Long lastReadMessageId, LocalDateTime joinedAt) {
        this.id = id;
        this.user = user;
        this.lastReadMessageId = lastReadMessageId;
        this.joinedAt = joinedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserResponse getUser() {
        return user;
    }

    public void setUser(UserResponse user) {
        this.user = user;
    }

    public Long getLastReadMessageId() {
        return lastReadMessageId;
    }

    public void setLastReadMessageId(Long lastReadMessageId) {
        this.lastReadMessageId = lastReadMessageId;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }
}
