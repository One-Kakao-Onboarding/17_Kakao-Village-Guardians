package com.example.demo.dto.response;

import java.util.List;

public class FriendMatchingDetailResponse {
    private List<ChatRoomRecommendation> recommendations;

    public FriendMatchingDetailResponse() {
    }

    public FriendMatchingDetailResponse(List<ChatRoomRecommendation> recommendations) {
        this.recommendations = recommendations;
    }

    public List<ChatRoomRecommendation> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<ChatRoomRecommendation> recommendations) {
        this.recommendations = recommendations;
    }

    public static class ChatRoomRecommendation {
        private Long chatRoomId;
        private String chatRoomName;
        private Integer matchScore;
        private String matchReason;

        public ChatRoomRecommendation() {
        }

        public ChatRoomRecommendation(Long chatRoomId, String chatRoomName, Integer matchScore, String matchReason) {
            this.chatRoomId = chatRoomId;
            this.chatRoomName = chatRoomName;
            this.matchScore = matchScore;
            this.matchReason = matchReason;
        }

        public Long getChatRoomId() {
            return chatRoomId;
        }

        public void setChatRoomId(Long chatRoomId) {
            this.chatRoomId = chatRoomId;
        }

        public String getChatRoomName() {
            return chatRoomName;
        }

        public void setChatRoomName(String chatRoomName) {
            this.chatRoomName = chatRoomName;
        }

        public Integer getMatchScore() {
            return matchScore;
        }

        public void setMatchScore(Integer matchScore) {
            this.matchScore = matchScore;
        }

        public String getMatchReason() {
            return matchReason;
        }

        public void setMatchReason(String matchReason) {
            this.matchReason = matchReason;
        }
    }
}
