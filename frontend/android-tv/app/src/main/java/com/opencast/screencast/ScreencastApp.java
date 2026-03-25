package com.opencast.screencast;

import android.app.Application;
import android.content.SharedPreferences;

import com.opencast.screencast.network.RetrofitClient;

public class ScreencastApp extends Application {
    private static ScreencastApp instance;
    private SharedPreferences preferences;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        preferences = getSharedPreferences("screencast", MODE_PRIVATE);
        RetrofitClient.init(getServerAddress());
    }

    public static ScreencastApp getInstance() {
        return instance;
    }

    public String getServerAddress() {
        return preferences.getString("server_address", "http://192.168.5.153:8080");
    }

    public void setServerAddress(String address) {
        preferences.edit().putString("server_address", address).apply();
        RetrofitClient.init(address);
    }

    public String getDeviceToken() {
        return preferences.getString("device_token", null);
    }

    public void setDeviceToken(String token) {
        preferences.edit().putString("device_token", token).apply();
    }

    public boolean isDeviceBound() {
        return getDeviceToken() != null && getBindStatus();
    }

    public boolean getBindStatus() {
        return preferences.getBoolean("bind_status", false);
    }

    public void setBindStatus(boolean bound) {
        preferences.edit().putBoolean("bind_status", bound).apply();
    }

    public void clearBindStatus() {
        preferences.edit().remove("bind_status").apply();
    }

    public SharedPreferences getPreferences() {
        return preferences;
    }

    // 任务持久化
    private static final String KEY_LAST_TASK = "last_task_json";

    public void saveLastTask(String taskJson) {
        preferences.edit().putString(KEY_LAST_TASK, taskJson).apply();
    }

    public String getLastTask() {
        return preferences.getString(KEY_LAST_TASK, null);
    }

    public void clearLastTask() {
        preferences.edit().remove(KEY_LAST_TASK).apply();
    }
}
