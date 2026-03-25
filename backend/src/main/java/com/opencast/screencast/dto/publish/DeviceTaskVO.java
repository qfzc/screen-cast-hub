package com.opencast.screencast.dto.publish;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 设备播放任务VO（TV端使用）
 * 字段命名匹配 Android TV 端的 PublishTask 模型
 */
@Data
@Schema(description = "设备播放任务")
public class DeviceTaskVO {

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "批次ID")
    private String batchId;

    @Schema(description = "播放模式: SEQUENCE-顺序播放, RANDOM-随机播放")
    private String playMode;

    @Schema(description = "播放间隔(秒)")
    private Integer playInterval;

    @Schema(description = "自动播放")
    private Boolean autoPlay;

    @Schema(description = "循环播放")
    private Boolean loopPlay;

    @Schema(description = "任务状态")
    private Integer status;

    @Schema(description = "状态描述")
    private String statusDesc;

    @Schema(description = "素材列表")
    private List<TaskItemVO> items;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /**
     * 素材项VO
     */
    @Data
    @Schema(description = "素材项信息")
    public static class TaskItemVO {

        @Schema(description = "素材ID")
        private Long materialId;

        @Schema(description = "素材名称")
        private String materialName;

        @Schema(description = "素材类型: IMAGE, VIDEO, PDF")
        private String materialType;

        @Schema(description = "素材URL")
        private String materialUrl;

        @Schema(description = "缩略图URL")
        private String thumbnailUrl;

        @Schema(description = "播放顺序")
        private Integer sortOrder;

        @Schema(description = "播放时长(秒)")
        private Integer duration;

        @Schema(description = "平铺方式")
        private String fitMode;

        @Schema(description = "过渡效果")
        private String transition;

        @Schema(description = "页数")
        private Integer pageCount;

        @Schema(description = "素材文件MD5")
        private String materialMd5;
    }
}
