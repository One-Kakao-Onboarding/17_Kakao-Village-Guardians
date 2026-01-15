package com.example.demo.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public class MessageResponse {
    private Long id;
    private UserResponse sender;
    private String content;
    private String originalContent;
    private Boolean wasGuarded;
    private Boolean isEmoticon;
    private Long emoticonId;
    private LocalDateTime timestamp;
    private List<ReactionResponse> reactions;
    private Boolean isMine;
    private Boolean isRead;

    public MessageResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserResponse getSender() {
        return sender;
    }

    public void setSender(UserResponse sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getOriginalContent() {
        return originalContent;
    }

    public void setOriginalContent(String originalContent) {
        this.originalContent = originalContent;
    }

    public Boolean getWasGuarded() {
        return wasGuarded;
    }

    public void setWasGuarded(Boolean wasGuarded) {
        this.wasGuarded = wasGuarded;
    }

    public Boolean getIsEmoticon() {
        return isEmoticon;
    }

    public void setIsEmoticon(Boolean isEmoticon) {
        this.isEmoticon = isEmoticon;
    }

    public Long getEmoticonId() {
        return emoticonId;
    }

    public void setEmoticonId(Long emoticonId) {
        this.emoticonId = emoticonId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public List<ReactionResponse> getReactions() {
        return reactions;
    }

    public void setReactions(List<ReactionResponse> reactions) {
        this.reactions = reactions;
    }

    public Boolean getIsMine() {
        return isMine;
    }

    public void setIsMine(Boolean isMine) {
        this.isMine = isMine;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }
}
