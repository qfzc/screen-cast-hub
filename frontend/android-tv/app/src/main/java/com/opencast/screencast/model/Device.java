package com.opencast.screencast.model;

import com.google.gson.annotations.SerializedName;

public class Device {
    @SerializedName("id")
    private Long id;

    @SerializedName("deviceToken")
    private String deviceToken;

    @SerializedName("name")
    private String name;

    @SerializedName("bindCode")
    private String bindCode;

    @SerializedName("status")
    private Integer status;

    @SerializedName("lastHeartbeat")
    private String lastHeartbeat;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBindCode() {
        return bindCode;
    }

    public void setBindCode(String bindCode) {
        this.bindCode = bindCode;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void setLastHeartbeat(String lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }
}
