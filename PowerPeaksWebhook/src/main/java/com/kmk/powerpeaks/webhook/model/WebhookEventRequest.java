package com.kmk.powerpeaks.webhook.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;

public class WebhookEventRequest {

    @JsonProperty("object_type")
    private String objectType;

    @JsonProperty("object_id")
    private Long objectId;

    @JsonProperty("aspect_type")
    private String aspectType;

    private HashMap<String, String> updates;

    @JsonProperty("owner_id")
    private Long ownerId;

    @JsonProperty("subscription_id")
    private Integer subscriptionId;

    @JsonProperty("event_time")
    private Long eventTime;

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public Long getObjectId() {
        return objectId;
    }

    public void setObjectId(Long objectId) {
        this.objectId = objectId;
    }

    public String getAspectType() {
        return aspectType;
    }

    public void setAspectType(String aspectType) {
        this.aspectType = aspectType;
    }

    public HashMap<String, String> getUpdates() {
        return updates;
    }

    public void setUpdates(HashMap<String, String> updates) {
        this.updates = updates;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public Integer getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(Integer subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public Long getEventTime() {
        return eventTime;
    }

    public void setEventTime(Long eventTime) {
        this.eventTime = eventTime;
    }

    @Override
    public String toString() {
        return "WebhookEventRequest{" +
                "objectType='" + objectType + '\'' +
                ", objectId=" + objectId +
                ", aspectType='" + aspectType + '\'' +
                ", updates=" + updates +
                ", ownerId=" + ownerId +
                ", subscriptionId=" + subscriptionId +
                ", eventTime=" + eventTime +
                '}';
    }
}
