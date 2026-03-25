package com.opencast.screencast.dto.material;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 素材VO
 */
@Data
@Schema(description = "素材信息")
public class MaterialVO {

    @Schema(description = "素材ID")
    private Long id;

    @Schema(description = "素材名称")
    private String name;

    @Schema(description = "原始文件名")
    private String originalName;

    @Schema(description = "类型: image/video/pdf/ppt")
    private String type;

    @Schema(description = "原始文件URL")
    private String originalUrl;

    @Schema(description = "转换后URL")
    private String convertedUrl;

    @Schema(description = "缩略图URL")
    private String thumbnailUrl;

    @Schema(description = "文件大小(字节)")
    private Long fileSize;

    @Schema(description = "时长(秒)")
    private Integer duration;

    @Schema(description = "页数")
    private Integer pageCount;

    @Schema(description = "转码状态: 0无需 1转码中 2成功 3失败")
    private Integer convertStatus;

    @Schema(description = "转码状态描述")
    private String convertStatusDesc;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "文件MD5")
    private String fileMd5;
}
