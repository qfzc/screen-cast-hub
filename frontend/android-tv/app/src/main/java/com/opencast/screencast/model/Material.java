package com.opencast.screencast.model;

import com.google.gson.annotations.SerializedName;

public class Material {
    @SerializedName("id")
    private Long id;

    @SerializedName("name")
    private String name;

    @SerializedName("type")
    private String type; // IMAGE, VIDEO, PDF

    @SerializedName("url")
    private String url;

    @SerializedName("thumbnailUrl")
    private String thumbnailUrl;

    @SerializedName("fileSize")
    private Long fileSize;

    @SerializedName("duration")
    private Integer duration; // for video, in seconds

    @SerializedName("conversionStatus")
    private String conversionStatus;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("transition")
    private String transition; // 过渡效果: NONE, FADE, SLIDE, CUBE, FLIP, REVEAL

    @SerializedName("fitMode")
    private String fitMode; // 适配模式: FILL, FIT, ORIGINAL, STRETCH

    @SerializedName("sortOrder")
    private Integer sortOrder; // 播放顺序

    @SerializedName("pageCount")
    private Integer pageCount;

    @SerializedName("md5")
    private String md5; // 文件MD5，用于缓存校验

    // 本地缓存路径（不参与序列化）
    private transient String localPath;

    // 获取 MD5
    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getConversionStatus() {
        return conversionStatus;
    }

    public void setConversionStatus(String conversionStatus) {
        this.conversionStatus = conversionStatus;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getTransition() {
        return transition;
    }

    public void setTransition(String transition) {
        this.transition = transition;
    }

    public String getFitMode() {
        return fitMode;
    }

    public void setFitMode(String fitMode) {
        this.fitMode = fitMode;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public void setPageCount(Integer pageCount) {
        this.pageCount = pageCount;
    }

    public boolean isImage() {
        return "IMAGE".equalsIgnoreCase(type);
    }

    public boolean isVideo() {
        return "VIDEO".equalsIgnoreCase(type);
    }

    public boolean isPdf() {
        return "PDF".equalsIgnoreCase(type);
    }
}
