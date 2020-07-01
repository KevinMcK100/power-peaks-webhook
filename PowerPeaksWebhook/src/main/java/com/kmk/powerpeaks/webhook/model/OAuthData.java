package com.kmk.powerpeaks.webhook.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OAuthData {

    @JsonProperty("athlete_id")
    private String athleteId;

    @JsonProperty("client_id")
    private String clientId;

    @JsonProperty("client_secret")
    private String clientSecret;

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("expires_at")
    private long expiresAt;

    public String getAthleteId() {
        return athleteId;
    }

    public void setAthleteId(String athleteId) {
        this.athleteId = athleteId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }

    @Override
    public String toString(){
        return
                "OAuthData{" +
                        "athleteId = '" + athleteId + '\'' +
                        ", clientId = '" + clientId + '\'' +
                        ", clientSecret = '" + clientSecret + '\'' +
                        ", accessToken = '" + accessToken + '\'' +
                        ", refreshToken = '" + refreshToken + '\'' +
                        ", expiresAt = '" + expiresAt + '\'' +
                        "}";
    }
}
