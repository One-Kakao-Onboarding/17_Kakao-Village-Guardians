package com.example.demo.dto.request;

import java.util.List;

public class FriendMatchingRequest {
    private String profileName;
    private String personaId;
    private List<Long> chatRoomIds;

    public FriendMatchingRequest() {
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public String getPersonaId() {
        return personaId;
    }

    public void setPersonaId(String personaId) {
        this.personaId = personaId;
    }

    public List<Long> getChatRoomIds() {
        return chatRoomIds;
    }

    public void setChatRoomIds(List<Long> chatRoomIds) {
        this.chatRoomIds = chatRoomIds;
    }
}
