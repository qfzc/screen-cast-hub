package com.opencast.screencast.dto.device;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

/**
 * 设备播放列表响应
 */
@Data
@Schema(description = "设备播放列表")
public class DevicePlaylistVO {
    
    @Schema(description = "设备ID")
    private Long deviceId;
    
    @Schema(description = "播放模式")
    private String playMode;
    
    @Schema(description = "播放间隔(秒)")
    private Integer playInterval;
    
    @Schema(description = "是否循环播放")
    private Boolean loopPlay;
    
    @Schema(description = "播放项列表")
    private List<PlaylistItemVO> items;
    
    @Data
    @Schema(description = "播放项")
    public static class PlaylistItemVO {
        
        @Schema(description = "播放项ID")
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
        
        @Schema(description = "排序序号")
        private Integer sortOrder;
        
        @Schema(description = "平铺方式")
        private String fitMode;
        
        @Schema(description = "播放时长(秒)")
        private Integer duration;
        
        @Schema(description = "过渡效果")
        private String transition;
    }
}
