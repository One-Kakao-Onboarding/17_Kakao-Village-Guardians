package com.example.demo.dto.response;

import java.time.LocalDateTime;

public class ReactionResponse {
    private Long id;
    private UserResponse user;
    private String emoji;
    private LocalDateTime createdAt;

    public ReactionResponse() {
    }

    public ReactionResponse(Long id, UserResponse user, String emoji, LocalDateTime createdAt) {
        this.id = id;
        this.user = user;
        this.emoji = emoji;
        this.createdAt = createdAt;
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

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
