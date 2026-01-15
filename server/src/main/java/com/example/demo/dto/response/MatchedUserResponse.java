package com.example.demo.dto.response;

public class MatchedUserResponse {
    private Long userId;
    private String name;
    private String avatar;
    private Double compatibilityScore;
    private String matchReason;

    public MatchedUserResponse() {
    }

    public MatchedUserResponse(Long userId, String name, String avatar, Double compatibilityScore, String matchReason) {
        this.userId = userId;
        this.name = name;
        this.avatar = avatar;
        this.compatibilityScore = compatibilityScore;
        this.matchReason = matchReason;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Double getCompatibilityScore() {
        return compatibilityScore;
    }

    public void setCompatibilityScore(Double compatibilityScore) {
        this.compatibilityScore = compatibilityScore;
    }

    public String getMatchReason() {
        return matchReason;
    }

    public void setMatchReason(String matchReason) {
        this.matchReason = matchReason;
    }
}
