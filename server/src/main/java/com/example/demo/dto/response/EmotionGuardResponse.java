package com.example.demo.dto.response;

public class EmotionGuardResponse {
    private Boolean isAggressive;
    private Boolean isSarcastic;
    private String suggestion;

    public EmotionGuardResponse() {
    }

    public EmotionGuardResponse(Boolean isAggressive, Boolean isSarcastic, String suggestion) {
        this.isAggressive = isAggressive;
        this.isSarcastic = isSarcastic;
        this.suggestion = suggestion;
    }

    public Boolean getIsAggressive() {
        return isAggressive;
    }

    public void setIsAggressive(Boolean isAggressive) {
        this.isAggressive = isAggressive;
    }

    public Boolean getIsSarcastic() {
        return isSarcastic;
    }

    public void setIsSarcastic(Boolean isSarcastic) {
        this.isSarcastic = isSarcastic;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }
}
