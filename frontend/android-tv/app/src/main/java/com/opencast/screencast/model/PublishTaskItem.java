package com.opencast.screencast.model;

import com.google.gson.annotations.SerializedName;

/**
 * 发布任务明细模型
 */
public class PublishTaskItem {

    @SerializedName("id")
    private Long id;

    @SerializedName("taskId")
    private Long taskId;

    @SerializedName("materialId")
    private Long materialId;

    @SerializedName("materialName")
    private String materialName;

    @SerializedName("materialType")
    private String materialType; // IMAGE, VIDEO, PDF

    @SerializedName("materialUrl")
    private String materialUrl;

    @SerializedName("thumbnailUrl")
    private String thumbnailUrl;

    @SerializedName("sortOrder")
    private Integer sortOrder;

    @SerializedName("duration")
    private Integer duration; // seconds (for images/PDF)

    @SerializedName("fitMode")
    private String fitMode; // FILL, FIT, ORIGINAL, STRETCH

    @SerializedName("transition")
    private String transition; // NONE, FADE, SLIDE, CUBE, FLIP, REVEAL

    @SerializedName("status")
    private Integer status; // 0待播放 1播放中 2已完成

    @SerializedName("playCount")
    private Integer playCount;

    @SerializedName("pageCount")
    private Integer pageCount;

    @SerializedName("materialMd5")
    private String materialMd5;

    public String getMaterialMd5() {
        return materialMd5;
    }

    public void setMaterialMd5(String materialMd5) {
        this.materialMd5 = materialMd5;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public String getMaterialName() {
        return materialName;
    }

    public void setMaterialName(String materialName) {
        this.materialName = materialName;
    }

    public String getMaterialType() {
        return materialType;
    }

    public void setMaterialType(String materialType) {
        this.materialType = materialType;
    }

    public String getMaterialUrl() {
        return materialUrl;
    }

    public void setMaterialUrl(String materialUrl) {
        this.materialUrl = materialUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getFitMode() {
        return fitMode;
    }

    public void setFitMode(String fitMode) {
        this.fitMode = fitMode;
    }

    public String getTransition() {
        return transition;
    }

    public void setTransition(String transition) {
        this.transition = transition;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getPlayCount() {
        return playCount;
    }

    public void setPlayCount(Integer playCount) {
        this.playCount = playCount;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public void setPageCount(Integer pageCount) {
        this.pageCount = pageCount;
    }

    /**
     * 转换为 Material 对象
     */
    public Material toMaterial() {
        Material material = new Material();
        material.setId(this.materialId);
        material.setName(this.materialName);
        material.setType(this.materialType);
        material.setUrl(this.materialUrl);
        material.setThumbnailUrl(this.thumbnailUrl);
        material.setFitMode(this.fitMode);
        material.setDuration(this.duration);
        material.setTransition(this.transition);
        material.setSortOrder(this.sortOrder);
        material.setPageCount(this.pageCount);
        material.setMd5(this.materialMd5);
        return material;
    }
}
