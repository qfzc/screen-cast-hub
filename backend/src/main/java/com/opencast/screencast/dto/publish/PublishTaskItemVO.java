package com.opencast.screencast.dto.publish;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 发布任务明细VO
 */
@Data
@Schema(description = "发布任务明细信息")
public class PublishTaskItemVO {

    @Schema(description = "明细ID")
    private Long id;

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

    @Schema(description = "平铺方式: FILL-填充, FIT-适应, ORIGINAL-原始, STRETCH-拉伸")
    private String fitMode;

    @Schema(description = "过渡效果: NONE-无, FADE-淡入淡出, SLIDE-滑动, CUBE-立方体")
    private String transition;

    @Schema(description = "状态: 0待播放 1播放中 2已完成")
    private Integer status;

    @Schema(description = "状态描述")
    private String statusDesc;

    @Schema(description = "播放次数")
    private Integer playCount;

    @Schema(description = "页数")
    private Integer pageCount;

    @Schema(description = "素材文件MD5")
    private String materialMd5;
}
