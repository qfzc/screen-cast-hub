package com.opencast.screencast.dto.publish;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 发布记录VO
 */
@Data
@Schema(description = "发布记录")
public class PublishRecordVO {

    @Schema(description = "任务ID")
    private Long id;

    @Schema(description = "批次ID")
    private String batchId;

    @Schema(description = "设备ID")
    private Long deviceId;

    @Schema(description = "设备名称")
    private String deviceName;

    @Schema(description = "任务名称")
    private String name;

    @Schema(description = "任务状态")
    private Integer status;

    @Schema(description = "任务状态描述")
    private String statusDesc;

    @Schema(description = "素材数量")
    private Integer itemCount;

    @Schema(description = "发布时间")
    private LocalDateTime publishedAt;

    @Schema(description = "完成时间")
    private LocalDateTime completedAt;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
