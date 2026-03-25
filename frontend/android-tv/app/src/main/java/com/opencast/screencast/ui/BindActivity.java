package com.opencast.screencast.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.opencast.screencast.R;
import com.opencast.screencast.ScreencastApp;
import com.opencast.screencast.model.ApiResponse;
import com.opencast.screencast.network.ApiService;
import com.opencast.screencast.network.RetrofitClient;

import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BindActivity extends AppCompatActivity {
    private static final String TAG = "BindActivity";
    private static final int POLL_INTERVAL = 3000; // 3 seconds

    private ImageView qrCodeView;
    private TextView bindCodeText;
    private TextView statusText;

    private String deviceToken;
    private String bindCode;
    private Handler pollHandler = new Handler(Looper.getMainLooper());
    private boolean isBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bind);

        hideSystemUI();
        initViews();
        generateBindCode();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    private void hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Window window = getWindow();
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    private void initViews() {
        qrCodeView = findViewById(R.id.qr_code_view);
        bindCodeText = findViewById(R.id.bind_code_text);
        statusText = findViewById(R.id.status_text);
    }

    private void generateBindCode() {
        // 生成或获取设备唯一标识
        deviceToken = ScreencastApp.getInstance().getDeviceToken();
        if (deviceToken == null) {
            deviceToken = UUID.randomUUID().toString();
            ScreencastApp.getInstance().setDeviceToken(deviceToken);
        }

        // 获取设备信息
        String deviceName = Build.MODEL;
        String model = Build.MODEL;
        String osVersion = Build.VERSION.RELEASE;
        String appVersion = "1.0.0"; // 可以从 PackageInfo 获取

        ApiService apiService = RetrofitClient.getApiService();
        ApiService.BindCodeRequest request = new ApiService.BindCodeRequest(
                deviceToken, deviceName, model, osVersion, appVersion
        );

        apiService.generateBindCode(request).enqueue(new Callback<ApiResponse<ApiService.BindCodeResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<ApiService.BindCodeResponse>> call,
                                   Response<ApiResponse<ApiService.BindCodeResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiService.BindCodeResponse data = response.body().getData();
                    if (data != null) {
                        bindCode = data.getBindCode();
                        displayQrCode(data.getQrCodeUrl());
                        startPollingBindStatus();
                    }
                } else {
                    showError("生成绑定码失败");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<ApiService.BindCodeResponse>> call, Throwable t) {
                Log.e(TAG, "Generate bind code failed", t);
                showError("网络错误: " + t.getMessage());
            }
        });
    }

    private void displayQrCode(String qrCodeUrl) {
        try {
            // 使用服务端返回的URL生成二维码
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(
                    qrCodeUrl,
                    BarcodeFormat.QR_CODE,
                    400,
                    400
            );
            qrCodeView.setImageBitmap(bitmap);
            bindCodeText.setText(getString(R.string.binding_code, bindCode));
            statusText.setText(R.string.scan_qr_to_bind);
        } catch (Exception e) {
            Log.e(TAG, "Failed to generate QR code", e);
            showError("生成二维码失败");
        }
    }

    private void startPollingBindStatus() {
        pollHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isBound) return;
                checkBindStatus();
                pollHandler.postDelayed(this, POLL_INTERVAL);
            }
        }, POLL_INTERVAL);
    }

    private void checkBindStatus() {
        if (deviceToken == null) return;

        ApiService apiService = RetrofitClient.getApiService();
        apiService.checkBindStatus(deviceToken).enqueue(new Callback<ApiResponse<ApiService.BindStatusResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<ApiService.BindStatusResponse>> call,
                                   Response<ApiResponse<ApiService.BindStatusResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiService.BindStatusResponse status = response.body().getData();
                    if (status != null && status.isBound()) {
                        isBound = true;
                        onDeviceBound();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<ApiService.BindStatusResponse>> call, Throwable t) {
                Log.e(TAG, "Check bind status failed", t);
            }
        });
    }

    private void onDeviceBound() {
        // 保存绑定状态
        ScreencastApp.getInstance().setBindStatus(true);

        runOnUiThread(() -> {
            statusText.setText(R.string.device_bound);
            Toast.makeText(this, R.string.device_bound, Toast.LENGTH_SHORT).show();

            // Navigate to main activity
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }, 1500);
        });
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            statusText.setText(message);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pollHandler.removeCallbacksAndMessages(null);
    }
}
