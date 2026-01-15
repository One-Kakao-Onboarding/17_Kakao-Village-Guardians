package com.example.demo.dto.request;

public class TransformTextRequest {
    private String text;
    private String personaId;
    private Double formalityLevel;
    private String relationship;
    private Long roomId;
    private String profileId;

    public TransformTextRequest() {
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getPersonaId() {
        return personaId;
    }

    public void setPersonaId(String personaId) {
        this.personaId = personaId;
    }

    public Double getFormalityLevel() {
        return formalityLevel;
    }

    public void setFormalityLevel(Double formalityLevel) {
        this.formalityLevel = formalityLevel;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }
}
