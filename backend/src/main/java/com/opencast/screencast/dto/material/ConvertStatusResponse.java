package com.opencast.screencast.dto.material;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 转码状态响应
 */
@Data
@Schema(description = "转码状态响应")
public class ConvertStatusResponse {

    @Schema(description = "素材ID")
    private Long id;

    @Schema(description = "转码状态: 0无需 1转码中 2成功 3失败")
    private Integer convertStatus;

    @Schema(description = "转码进度(%)")
    private Integer convertProgress;

    @Schema(description = "转换后URL")
    private String convertedUrl;

    @Schema(description = "缩略图URL")
    private String thumbnailUrl;

    @Schema(description = "页数")
    private Integer pageCount;

    @Schema(description = "失败原因")
    private String failReason;
}
