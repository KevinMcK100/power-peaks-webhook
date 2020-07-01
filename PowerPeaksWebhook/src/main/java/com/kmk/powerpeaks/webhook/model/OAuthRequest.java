package com.kmk.powerpeaks.webhook.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OAuthRequest {

    private static final String REFRESH_TOKEN_GRANT_TYPE = "refresh_token";

    @JsonProperty("client_secret")
    private String clientSecret;

    @JsonProperty("client_id")
    private Long clientId;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("grant_type")
    private String grantType = REFRESH_TOKEN_GRANT_TYPE;

    public void setClientSecret(String clientSecret){
        this.clientSecret = clientSecret;
    }

    public String getClientSecret(){
        return clientSecret;
    }

    public void setClientId(Long clientId){
        this.clientId = clientId;
    }

    public Long getClientId(){
        return clientId;
    }

    public void setRefreshToken(String refreshToken){
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken(){
        return refreshToken;
    }

    public void setGrantType(String grantType){
        this.grantType = grantType;
    }

    public String getGrantType(){
        return grantType;
    }

    @Override
    public String toString(){
        return
                "AuthorisationRequest{" +
                        "refresh_token = '" + getRefreshToken() + '\'' +
                        ", grant_type = '" + getGrantType() + '\'' +
                        ", client_secret = '" + getClientSecret() + '\'' +
                        ", client_id = '" + getClientId() + '\'' +
                        "}";
    }
}
