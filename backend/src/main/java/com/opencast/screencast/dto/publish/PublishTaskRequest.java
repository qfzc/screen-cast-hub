package com.opencast.screencast.dto.publish;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 发布任务请求
 */
@Data
@Schema(description = "发布任务请求")
public class PublishTaskRequest {

    @NotEmpty(message = "设备ID不能为空")
    @Schema(description = "设备ID列表", required = true)
    private List<Long> deviceIds;

    @JsonAlias("items")
    @Schema(description = "素材项列表（兼容前端 items 字段）", required = true)
    private List<MaterialItemRequest> materials;

    @Schema(description = "素材ID列表（兼容旧版请求）")
    private List<Long> materialIds;

    @Schema(description = "任务名称")
    private String name;

    @Schema(description = "播放模式: SEQUENCE-顺序播放, RANDOM-随机播放")
    private String playMode;

    @Schema(description = "播放间隔(秒)")
    private Integer playInterval;

    @Schema(description = "定时发布时间")
    private LocalDateTime scheduledAt;

    @Schema(description = "自动播放")
    private Boolean autoPlay;

    @Schema(description = "循环播放")
    private Boolean loopPlay;

    @JsonIgnore
    public List<MaterialItemRequest> getEffectiveMaterials() {
        if (materials != null && !materials.isEmpty()) {
            return materials;
        }

        List<MaterialItemRequest> normalizedMaterials = new ArrayList<>();
        if (materialIds == null || materialIds.isEmpty()) {
            return normalizedMaterials;
        }

        for (int i = 0; i < materialIds.size(); i++) {
            Long materialId = materialIds.get(i);
            if (materialId == null) {
                continue;
            }

            MaterialItemRequest item = new MaterialItemRequest();
            item.setMaterialId(materialId);
            item.setSortOrder(i);
            normalizedMaterials.add(item);
        }
        return normalizedMaterials;
    }

    /**
     * 素材项请求
     */
    @Data
    @Schema(description = "素材项请求")
    public static class MaterialItemRequest {

        @NotNull(message = "素材ID不能为空")
        @Schema(description = "素材ID", required = true)
        private Long materialId;

        @Schema(description = "播放顺序")
        private Integer sortOrder;

        @Schema(description = "播放时长(秒)")
        private Integer duration;

        @Schema(description = "平铺方式: FILL-填充, FIT-适应, ORIGINAL-原始, STRETCH-拉伸")
        private String fitMode;

        @Schema(description = "过渡效果: NONE-无, FADE-淡入淡出, SLIDE-滑动, CUBE-立方体")
        private String transition;
    }
}
