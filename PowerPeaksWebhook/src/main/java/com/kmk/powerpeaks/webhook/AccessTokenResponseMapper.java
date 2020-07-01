package com.kmk.powerpeaks.webhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kmk.powerpeaks.webhook.model.AccessTokenResponse;

import java.io.IOException;
import java.util.concurrent.CompletionException;

public class AccessTokenResponseMapper extends ObjectMapper {

    public String prettyWriteValueAsString(Object value) {
        try {
            return super.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new CompletionException(e);
        }
    }

    public AccessTokenResponse readAccessTokenResponse(String content) {
        TypeReference<AccessTokenResponse> type = new TypeReference<> () {};
        try {
            return super.readValue(content, type);
        } catch (IOException e) {
            throw new CompletionException(e);
        }
    }
}
