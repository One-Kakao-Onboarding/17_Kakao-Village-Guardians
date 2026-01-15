package com.example.demo.dto.response;

public class EmotionGuardDetailResponse {
    private Boolean isAggressive;
    private String aggressionType;
    private Double aggressionScore;
    private String suggestedText;
    private String warningMessage;

    public EmotionGuardDetailResponse() {
    }

    public EmotionGuardDetailResponse(Boolean isAggressive, String aggressionType, Double aggressionScore,
                                      String suggestedText, String warningMessage) {
        this.isAggressive = isAggressive;
        this.aggressionType = aggressionType;
        this.aggressionScore = aggressionScore;
        this.suggestedText = suggestedText;
        this.warningMessage = warningMessage;
    }

    public Boolean getIsAggressive() {
        return isAggressive;
    }

    public void setIsAggressive(Boolean isAggressive) {
        this.isAggressive = isAggressive;
    }

    public String getAggressionType() {
        return aggressionType;
    }

    public void setAggressionType(String aggressionType) {
        this.aggressionType = aggressionType;
    }

    public Double getAggressionScore() {
        return aggressionScore;
    }

    public void setAggressionScore(Double aggressionScore) {
        this.aggressionScore = aggressionScore;
    }

    public String getSuggestedText() {
        return suggestedText;
    }

    public void setSuggestedText(String suggestedText) {
        this.suggestedText = suggestedText;
    }

    public String getWarningMessage() {
        return warningMessage;
    }

    public void setWarningMessage(String warningMessage) {
        this.warningMessage = warningMessage;
    }
}
