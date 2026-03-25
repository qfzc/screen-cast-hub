package com.opencast.screencast.dto.device;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

/**
 * 设备播放列表请求
 */
@Data
@Schema(description = "设备播放列表请求")
public class DevicePlaylistRequest {
    
    @Schema(description = "播放模式: SEQUENCE-顺序, RANDOM-随机")
    private String playMode;
    
    @Schema(description = "播放间隔(秒)")
    private Integer playInterval;
    
    @Schema(description = "是否循环播放")
    private Boolean loopPlay;
    
    @Schema(description = "播放项列表")
    private List<PlaylistItemRequest> items;
    
    @Data
    @Schema(description = "播放项请求")
    public static class PlaylistItemRequest {
        
        @Schema(description = "素材ID")
        private Long materialId;
        
        @Schema(description = "排序序号")
        private Integer sortOrder;
        
        @Schema(description = "平铺方式: FILL, FIT, ORIGINAL, STRETCH")
        private String fitMode;
        
        @Schema(description = "播放时长(秒)")
        private Integer duration;
        
        @Schema(description = "过渡效果: NONE, FADE, SLIDE")
        private String transition;
    }
}
