package com.example.demo.dto.response;

import java.util.List;

public class ReactionSuggestResponse {
    private String emotion;
    private Double emotionScore;
    private List<String> suggestedEmojis;
    private List<SuggestedText> suggestedTexts;
    private List<QuickResponse> quickResponses;

    public ReactionSuggestResponse() {
    }

    public String getEmotion() {
        return emotion;
    }

    public void setEmotion(String emotion) {
        this.emotion = emotion;
    }

    public Double getEmotionScore() {
        return emotionScore;
    }

    public void setEmotionScore(Double emotionScore) {
        this.emotionScore = emotionScore;
    }

    public List<String> getSuggestedEmojis() {
        return suggestedEmojis;
    }

    public void setSuggestedEmojis(List<String> suggestedEmojis) {
        this.suggestedEmojis = suggestedEmojis;
    }

    public List<SuggestedText> getSuggestedTexts() {
        return suggestedTexts;
    }

    public void setSuggestedTexts(List<SuggestedText> suggestedTexts) {
        this.suggestedTexts = suggestedTexts;
    }

    public List<QuickResponse> getQuickResponses() {
        return quickResponses;
    }

    public void setQuickResponses(List<QuickResponse> quickResponses) {
        this.quickResponses = quickResponses;
    }

    public static class SuggestedText {
        private String text;
        private String type;

        public SuggestedText() {
        }

        public SuggestedText(String text, String type) {
            this.text = text;
            this.type = type;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    public static class QuickResponse {
        private String text;
        private String icon;

        public QuickResponse() {
        }

        public QuickResponse(String text, String icon) {
            this.text = text;
            this.icon = icon;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getIcon() {
            return icon;
        }

        public void setIcon(String icon) {
            this.icon = icon;
        }
    }
}
