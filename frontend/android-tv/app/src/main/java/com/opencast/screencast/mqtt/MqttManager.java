package com.opencast.screencast.mqtt;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.opencast.screencast.model.PublishTask;

import info.mqtt.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MqttManager implements MqttCallback {
    private static final String TAG = "MqttManager";

    private final Context context;
    private final String serverUri;
    private MqttAndroidClient mqttClient;
    private final Gson gson = new Gson();
    private OnTaskReceivedListener taskListener;
    private String deviceToken;

    public interface OnTaskReceivedListener {
        void onTaskReceived(PublishTask task);
    }

    public MqttManager(Context context, String serverUri) {
        this.context = context;
        this.serverUri = serverUri;
    }

    public void connect(String deviceToken, OnTaskReceivedListener listener) {
        Log.d(TAG, "=== MQTT CONNECT CALLED ===");
        Log.d(TAG, "Server URI: " + serverUri);
        Log.d(TAG, "Device Token: " + deviceToken);

        this.deviceToken = deviceToken;
        this.taskListener = listener;

        String clientId = "device_" + deviceToken;
        Log.d(TAG, "Client ID: " + clientId);

        mqttClient = new MqttAndroidClient(context, serverUri, clientId);
        mqttClient.setCallback(this);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(30);
        options.setKeepAliveInterval(60);

        Log.d(TAG, "Connect options: autoReconnect=true, cleanSession=true, timeout=30, keepAlive=60");

        Log.d(TAG, "About to call mqttClient.connect()...");
        mqttClient.connect(options, null, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                Log.d(TAG, "=== MQTT CONNECT SUCCESS ===");
                Log.d(TAG, "Connected to: " + serverUri);
                subscribeToTopics();
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                Log.e(TAG, "=== MQTT CONNECT FAILED ===");
                Log.e(TAG, "Server URI: " + serverUri);
                Log.e(TAG, "Exception type: " + exception.getClass().getSimpleName());
                Log.e(TAG, "Exception message: " + exception.getMessage());
                if (exception.getCause() != null) {
                    Log.e(TAG, "Caused by: " + exception.getCause().getMessage());
                }
                exception.printStackTrace();
            }
        });
        Log.d(TAG, "mqttClient.connect() called, waiting for callback...");
    }

    private void subscribeToTopics() {
        Log.d(TAG, "=== SUBSCRIBE TO TOPICS ===");
        String commandTopic = "device/" + deviceToken + "/command";
        String taskTopic = "device/" + deviceToken + "/task";
        String batchTaskTopic = "device/" + deviceToken + "/batch-task";

        Log.d(TAG, "Topics to subscribe:");
        Log.d(TAG, "  - " + commandTopic);
        Log.d(TAG, "  - " + taskTopic);
        Log.d(TAG, "  - " + batchTaskTopic);

        mqttClient.subscribe(commandTopic, 1, null, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                Log.d(TAG, "Subscribed to: " + commandTopic);
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                Log.e(TAG, "Failed to subscribe to: " + commandTopic, exception);
            }
        });

        mqttClient.subscribe(taskTopic, 1, null, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                Log.d(TAG, "Subscribed to: " + taskTopic);
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                Log.e(TAG, "Failed to subscribe to: " + taskTopic, exception);
            }
        });

        mqttClient.subscribe(batchTaskTopic, 1, null, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                Log.d(TAG, "Subscribed to: " + batchTaskTopic);
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                Log.e(TAG, "Failed to subscribe to: " + batchTaskTopic, exception);
            }
        });

        Log.d(TAG, "All subscription requests sent, waiting for callbacks...");
    }

    public void disconnect() {
        if (mqttClient != null) {
            mqttClient.disconnect(null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "Disconnected successfully");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "Disconnect failed", exception);
                }
            });
        }
    }

    public boolean isConnected() {
        return mqttClient != null && mqttClient.isConnected();
    }

    // MqttCallback implementations
    @Override
    public void connectionLost(Throwable cause) {
        Log.w(TAG, "=== MQTT CONNECTION LOST ===");
        if (cause != null) {
            Log.w(TAG, "Cause: " + cause.getMessage());
            Log.w(TAG, "Exception type: " + cause.getClass().getSimpleName());
            cause.printStackTrace();
        } else {
            Log.w(TAG, "Cause: unknown (null)");
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        Log.d(TAG, "=== MQTT MESSAGE RECEIVED ===");
        Log.d(TAG, "Topic: " + topic);
        String payload = new String(message.getPayload());
        Log.d(TAG, "Payload length: " + payload.length());
        Log.d(TAG, "Payload: " + payload);
        Log.d(TAG, "QoS: " + message.getQos());
        Log.d(TAG, "Retained: " + message.isRetained());

        if (topic.endsWith("/task") || topic.endsWith("/batch-task")) {
            Log.d(TAG, "Processing as task message");
            handleTaskMessage(payload);
        } else if (topic.endsWith("/command")) {
            Log.d(TAG, "Processing as command message");
            handleCommandMessage(payload);
        } else {
            Log.w(TAG, "Unknown topic, ignoring message");
        }
    }

    private void handleTaskMessage(String payload) {
        try {
            Log.d(TAG, "Parsing task JSON...");
            PublishTask task = gson.fromJson(payload, PublishTask.class);
            if (task != null) {
                int itemCount = task.getItems() != null ? task.getItems().size() : 0;
                Log.d(TAG, "Task parsed successfully - ID: " + task.getTaskId() + ", items=" + itemCount);
                if (taskListener != null) {
                    taskListener.onTaskReceived(task);
                } else {
                    Log.w(TAG, "No task listener registered");
                }
            } else {
                Log.e(TAG, "Parsed task is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse task message: " + e.getMessage(), e);
            Log.e(TAG, "Original payload: " + payload);
        }
    }

    private void handleCommandMessage(String payload) {
        // Handle commands like: pause, resume, refresh
        Log.d(TAG, "Command received: " + payload);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Not used for subscribing
    }

    public void publishStatus(String status) {
        String statusTopic = "device/" + deviceToken + "/status";
        MqttMessage message = new MqttMessage(status.getBytes());
        message.setQos(1);
        mqttClient.publish(statusTopic, message, null, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                Log.d(TAG, "Status published successfully");
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                Log.e(TAG, "Failed to publish status", exception);
            }
        });
    }
}
