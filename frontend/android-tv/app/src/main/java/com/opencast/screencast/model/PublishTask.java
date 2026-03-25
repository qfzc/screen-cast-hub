package com.opencast.screencast.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 发布任务模型
 */
public class PublishTask {

    @SerializedName(value = "taskId", alternate = {"id"})
    private Long taskId;

    @SerializedName("batchId")
    private String batchId;

    @SerializedName("deviceId")
    private Long deviceId;

    @SerializedName("name")
    private String name;

    @SerializedName("userId")
    private Long userId;

    @SerializedName("playMode")
    private String playMode; // SEQUENCE, RANDOM

    @SerializedName("playInterval")
    private Integer playInterval; // seconds between items

    @SerializedName("loopPlay")
    private Boolean loopPlay;

    @SerializedName("autoPlay")
    private Boolean autoPlay;

    @SerializedName("status")
    private Integer status; // 0待发布 1已发布 2播放中 3已完成 4已取消

    @SerializedName("scheduledAt")
    private String scheduledAt; // ISO 8601

    @SerializedName("publishedAt")
    private String publishedAt; // ISO 8601

    @SerializedName("startedAt")
    private String startedAt; // ISO 8601

    @SerializedName("completedAt")
    private String completedAt; // ISO 8601

    @SerializedName(value = "items", alternate = {"materials"})
    private List<PublishTaskItem> items;

    // Getters and Setters
    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getPlayMode() {
        return playMode;
    }

    public void setPlayMode(String playMode) {
        this.playMode = playMode;
    }

    public Integer getPlayInterval() {
        return playInterval;
    }

    public void setPlayInterval(Integer playInterval) {
        this.playInterval = playInterval;
    }

    public Boolean getLoopPlay() {
        return loopPlay;
    }

    public void setLoopPlay(Boolean loopPlay) {
        this.loopPlay = loopPlay;
    }

    public Boolean getAutoPlay() {
        return autoPlay;
    }

    public void setAutoPlay(Boolean autoPlay) {
        this.autoPlay = autoPlay;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(String scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public String getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(String publishedAt) {
        this.publishedAt = publishedAt;
    }

    public String getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(String startedAt) {
        this.startedAt = startedAt;
    }

    public String getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(String completedAt) {
        this.completedAt = completedAt;
    }

    public List<PublishTaskItem> getItems() {
        return items;
    }

    public void setItems(List<PublishTaskItem> items) {
        this.items = items;
    }
}
