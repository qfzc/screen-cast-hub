package com.opencast.screencast.dto.material;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 素材上传响应
 */
@Data
@Schema(description = "素材上传响应")
public class MaterialUploadResponse {

    @Schema(description = "素材ID")
    private Long id;

    @Schema(description = "素材名称")
    private String name;

    @Schema(description = "类型: image/video/pdf/ppt")
    private String type;

    @Schema(description = "原始文件URL")
    private String originalUrl;

    @Schema(description = "文件大小(字节)")
    private Long fileSize;

    @Schema(description = "转码状态: 0无需 1转码中 2成功 3失败")
    private Integer convertStatus;

    @Schema(description = "是否需要转码")
    private Boolean needConvert;
}
