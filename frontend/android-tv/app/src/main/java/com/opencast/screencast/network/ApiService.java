package com.opencast.screencast.network;

import com.opencast.screencast.model.ApiResponse;
import com.opencast.screencast.model.Device;
import com.opencast.screencast.model.PublishTask;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    /**
     * 生成绑定码（TV端调用）
     */
    @POST("/api/v1/device/bind-code")
    Call<ApiResponse<BindCodeResponse>> generateBindCode(@Body BindCodeRequest request);

    /**
     * 查询绑定状态（TV端轮询）
     */
    @GET("/api/v1/device/bind-status")
    Call<ApiResponse<BindStatusResponse>> checkBindStatus(@Query("deviceToken") String deviceToken);

    /**
     * 设备心跳
     */
    @POST("/api/v1/device/heartbeat")
    Call<ApiResponse<Void>> heartbeat(@Body HeartbeatRequest request);

    /**
     * 获取设备播放任务（TV端调用）
     */
    @GET("/api/v1/publish/tasks")
    Call<ApiResponse<List<PublishTask>>> getDeviceTasks(@Query("deviceToken") String deviceToken);

    /**
     * 确认任务完成（TV端调用）
     */
    @POST("/api/v1/publish/task/{taskId}/complete")
    Call<ApiResponse<Void>> completeTask(@Path("taskId") Long taskId, @Body TaskCompleteRequest request);

    // Request classes
    class BindCodeRequest {
        private String deviceToken;
        private String deviceName;
        private String model;
        private String osVersion;
        private String appVersion;

        public BindCodeRequest(String deviceToken, String deviceName, String model, String osVersion, String appVersion) {
            this.deviceToken = deviceToken;
            this.deviceName = deviceName;
            this.model = model;
            this.osVersion = osVersion;
            this.appVersion = appVersion;
        }

        public String getDeviceToken() { return deviceToken; }
        public String getDeviceName() { return deviceName; }
        public String getModel() { return model; }
        public String getOsVersion() { return osVersion; }
        public String getAppVersion() { return appVersion; }
    }

    class HeartbeatRequest {
        private String deviceToken;
        private Long storageUsed;
        private Long storageTotal;

        public HeartbeatRequest(String deviceToken) {
            this.deviceToken = deviceToken;
        }

        public HeartbeatRequest(String deviceToken, Long storageUsed, Long storageTotal) {
            this.deviceToken = deviceToken;
            this.storageUsed = storageUsed;
            this.storageTotal = storageTotal;
        }

        public String getDeviceToken() { return deviceToken; }
        public Long getStorageUsed() { return storageUsed; }
        public Long getStorageTotal() { return storageTotal; }
    }

    class TaskCompleteRequest {
        private Integer playDuration;
        private Integer playCount;

        public TaskCompleteRequest(Integer playDuration, Integer playCount) {
            this.playDuration = playDuration;
            this.playCount = playCount;
        }

        public Integer getPlayDuration() { return playDuration; }
        public Integer getPlayCount() { return playCount; }
    }

    // Response classes
    class BindCodeResponse {
        private String bindCode;
        private String qrCodeUrl;
        private String expireAt;
        private Long deviceId;

        public String getBindCode() { return bindCode; }
        public String getQrCodeUrl() { return qrCodeUrl; }
        public String getExpireAt() { return expireAt; }
        public Long getDeviceId() { return deviceId; }
    }

    class BindStatusResponse {
        private Integer bindStatus;
        private Long userId;
        private String bindAt;

        public Integer getBindStatus() { return bindStatus; }
        public Long getUserId() { return userId; }
        public String getBindAt() { return bindAt; }

        public boolean isBound() {
            return bindStatus != null && bindStatus == 1;
        }
    }
}
