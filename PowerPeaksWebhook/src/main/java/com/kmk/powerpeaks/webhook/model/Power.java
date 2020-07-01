package com.kmk.powerpeaks.webhook.model;

import java.util.List;

public class Power {

    private Long activityId;
    private List<Integer> powerStream;

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public List<Integer> getPowerStream() {
        return powerStream;
    }

    public void setPowerStream(List<Integer> powerStream) {
        this.powerStream = powerStream;
    }


    @Override
    public String toString() {
        return "Power{" +
                "activityId=" + activityId +
                ", powerStream=" + powerStream +
                '}';
    }
}
