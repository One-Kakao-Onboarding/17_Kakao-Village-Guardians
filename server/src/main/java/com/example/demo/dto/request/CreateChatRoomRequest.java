package com.example.demo.dto.request;

public class CreateChatRoomRequest {
    private String friendLdap;
    private Double formalityLevel;
    private String relationship;
    private String profileId;

    public CreateChatRoomRequest() {
    }

    public String getFriendLdap() {
        return friendLdap;
    }

    public void setFriendLdap(String friendLdap) {
        this.friendLdap = friendLdap;
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

    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }
}
