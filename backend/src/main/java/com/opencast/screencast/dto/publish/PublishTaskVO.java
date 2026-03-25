package com.opencast.screencast.dto.publish;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 发布任务VO
 */
@Data
@Schema(description = "发布任务信息")
public class PublishTaskVO {

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "批次ID")
    private String batchId;

    @Schema(description = "任务名称")
    private String name;

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

    @Schema(description = "任务状态描述")
    private String statusDesc;

    @Schema(description = "设备ID")
    private Long deviceId;

    @Schema(description = "设备名称")
    private String deviceName;

    @Schema(description = "素材明细列表")
    private List<PublishTaskItemVO> items;

    @Schema(description = "素材数量")
    private Integer itemCount;

    @Schema(description = "发布时间")
    private LocalDateTime publishedAt;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
