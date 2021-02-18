package com.example.wakeupdetection;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.Arrays;

import static android.content.Intent.ACTION_SCREEN_OFF;
import static android.content.Intent.ACTION_SCREEN_ON;
import static java.lang.Math.abs;

public class TestService extends Service implements SensorEventListener {

    private SensorManager sensorManager;

    private PowerManager pm;
    private PowerManager.WakeLock partialWakeLock;

    private final BroadcastReceiver screenOnReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                if (ACTION_SCREEN_ON.equals(action)) {
                    if (partialWakeLock != null && partialWakeLock.isHeld()) {
                        partialWakeLock.release();
                    }

                    if (sensorManager != null) {
                        sensorManager.unregisterListener(TestService.this);
                    }
                } else if(action.equals(ACTION_SCREEN_OFF)){
                    if (TestApp.getInstance().isTappedToTurnScreenOff) {
                        if (partialWakeLock != null && !partialWakeLock.isHeld()) {
                            partialWakeLock.acquire();
                        }

                        if (sensorManager != null) {
                            sensorManager.registerListener(TestService.this,
                                    sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
                        }
                    }
                }
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sensorManager = (SensorManager) getApplicationContext().getSystemService(Context.SENSOR_SERVICE);

        //sensorManager.registerListener(TestService.this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
        pm = (PowerManager) TestApp.getInstance().getSystemService(Context.POWER_SERVICE);
        partialWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TestService:");
        TestApp.getInstance().wakeServiceActive = true;

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_SCREEN_OFF);
        intentFilter.addAction(ACTION_SCREEN_ON);
        registerReceiver(screenOnReceiver, intentFilter);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            Log.d("WakeUpDetection", "received data: " + Arrays.toString(event.values.clone()));
            if (shouldTurnOnScreen(event.values.clone()[0])) {
                turnOnScreen();
            }
        }
    }

    private boolean shouldTurnOnScreen(float sensorData) {
        float lPercent = 50.0f;
        if (sensorData >= 0.9) {
            // Some calculations (simplified for this project)
            lPercent = ((100 / 2.1f) * (sensorData - 0.9f));
            lPercent = 50 - abs(lPercent / 2);
        }

        return lPercent < 10;
    }

    private void turnOnScreen() {
        if (pm != null) {
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
                    | PowerManager.ACQUIRE_CAUSES_WAKEUP
                    | PowerManager.ON_AFTER_RELEASE, "TestService:");
            wl.acquire();
            TestApp.getInstance().isTappedToTurnScreenOff = false;
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

        if (partialWakeLock != null && partialWakeLock.isHeld()) {
            partialWakeLock.release();
        }

        unregisterReceiver(screenOnReceiver);

        TestApp.getInstance().wakeServiceActive = false;
    }
}


