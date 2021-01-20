package com.example.wakeupdetection;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import static java.lang.Math.abs;

public class TestService extends Service implements SensorEventListener {

    private float lPercent = 50.0f;
    private static final String CHANNEL_ID = "ForegroundServiceChannel";
    private static final int NOTIFICATION_ID = 12345;

    private SensorManager sensorManager;
    private Notification notification;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sensorManager = (SensorManager) getApplicationContext().getSystemService(Context.SENSOR_SERVICE);

        sensorManager.registerListener(TestService.this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);

        TestApp.getInstance().wakeServiceActive = true;

        Intent notificationIntent = new Intent(this, TestService.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        createNotificationChannel();

        notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Foreground Service")
                .setContentText("text")
                .setSmallIcon(R.drawable.empty_dot)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();
        startForeground(NOTIFICATION_ID, notification);
        //return START_STICKY;
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            if (shouldTurnOnScreen(event.values.clone()[0])) {
                turnOnScreen();
            }
        }
    }

    private boolean shouldTurnOnScreen(float sensorData) {
        if (sensorData >= 0.9) {
            // Some calculations (simplified for this project)
            lPercent = ((100 / 2.1f) * (sensorData - 0.9f));
            lPercent = 50 - abs(lPercent / 2);
        }

        return lPercent < 10;
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_HIGH);
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(serviceChannel);
    }

    private void turnOnScreen() {
        PowerManager.WakeLock wl;
        PowerManager pm = (PowerManager) TestApp.getInstance().getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
                    | PowerManager.ACQUIRE_CAUSES_WAKEUP
                    | PowerManager.ON_AFTER_RELEASE, "TestService:");
            wl.acquire();
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("ADD_WINDOW_FLAG_KEEP_SCREEN_ON"));

            wl.release();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("Service", "onDestroy");
        if (sensorManager != null) {
            sensorManager.unregisterListener(TestService.this);
        }
        TestApp.getInstance().wakeServiceActive = false;
    }
}


