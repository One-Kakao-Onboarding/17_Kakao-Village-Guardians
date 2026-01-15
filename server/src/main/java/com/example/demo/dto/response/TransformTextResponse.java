package com.example.demo.dto.response;

import java.util.List;

public class TransformTextResponse {
    private String originalText;
    private String transformedText;
    private Double formalityLevel;
    private String appliedPersona;
    private List<ChangeDetail> changes;
    private Boolean shouldSuggest;
    private String suggestionReason;

    public TransformTextResponse() {
    }

    public String getOriginalText() {
        return originalText;
    }

    public void setOriginalText(String originalText) {
        this.originalText = originalText;
    }

    public String getTransformedText() {
        return transformedText;
    }

    public void setTransformedText(String transformedText) {
        this.transformedText = transformedText;
    }

    public Double getFormalityLevel() {
        return formalityLevel;
    }

    public void setFormalityLevel(Double formalityLevel) {
        this.formalityLevel = formalityLevel;
    }

    public String getAppliedPersona() {
        return appliedPersona;
    }

    public void setAppliedPersona(String appliedPersona) {
        this.appliedPersona = appliedPersona;
    }

    public List<ChangeDetail> getChanges() {
        return changes;
    }

    public void setChanges(List<ChangeDetail> changes) {
        this.changes = changes;
    }

    public Boolean getShouldSuggest() {
        return shouldSuggest;
    }

    public void setShouldSuggest(Boolean shouldSuggest) {
        this.shouldSuggest = shouldSuggest;
    }

    public String getSuggestionReason() {
        return suggestionReason;
    }

    public void setSuggestionReason(String suggestionReason) {
        this.suggestionReason = suggestionReason;
    }

    public static class ChangeDetail {
        private String type;
        private String description;

        public ChangeDetail() {
        }

        public ChangeDetail(String type, String description) {
            this.type = type;
            this.description = description;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
