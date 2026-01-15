package com.example.demo.dto.response;

import java.util.List;

public class FriendMatchingResponse {
    private List<MatchedUserResponse> matches;

    public FriendMatchingResponse() {
    }

    public FriendMatchingResponse(List<MatchedUserResponse> matches) {
        this.matches = matches;
    }

    public List<MatchedUserResponse> getMatches() {
        return matches;
    }

    public void setMatches(List<MatchedUserResponse> matches) {
        this.matches = matches;
    }
}
