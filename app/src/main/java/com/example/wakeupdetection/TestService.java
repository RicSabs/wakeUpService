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

public class TestService extends Service implements SensorEventListener {

    private static final String TAG = TestService.class.getSimpleName();
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
                } else if (action.equals(ACTION_SCREEN_OFF) && TestApp.getInstance().isTappedToTurnScreenOff()) {
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
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sensorManager = (SensorManager) getApplicationContext().getSystemService(Context.SENSOR_SERVICE);

        pm = (PowerManager) TestApp.getInstance().getSystemService(Context.POWER_SERVICE);
        partialWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        TestApp.getInstance().setWakeServiceActive(true);

        Log.d(TAG, "Extra: " + intent.getStringExtra(Constants.INPUT_EXTRA));

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_SCREEN_OFF);
        intentFilter.addAction(ACTION_SCREEN_ON);
        registerReceiver(screenOnReceiver, intentFilter);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            Log.d(TAG, "received data: " + Arrays.toString(event.values.clone()));
            if (isHeadTiltedFarEnough(event.values.clone()[0])) {
                turnScreenOn();
            }
        }
    }

    /**
     * Assuming the default position of the glass is parallel to the ground
     * a tilt to the left should be bigger than 2 m/s^2
     * @param tiltValue current sensor value for the roll
     * @return true if the head movement was far enough to trigger screen on
     */
    private boolean isHeadTiltedFarEnough(float tiltValue) {
        return tiltValue > 2.1f;
    }

    private void turnScreenOn() {
        if (pm != null) {
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
                    | PowerManager.ACQUIRE_CAUSES_WAKEUP
                    | PowerManager.ON_AFTER_RELEASE, TAG);
            wl.acquire();
            TestApp.getInstance().setTappedToTurnScreenOff(false);
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(Constants.FLAG_KEEP_SCREEN_ON));
            wl.release();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // not interesting in this use case
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        if (partialWakeLock != null && partialWakeLock.isHeld()) {
            partialWakeLock.release();
        }

        unregisterReceiver(screenOnReceiver);

        TestApp.getInstance().setWakeServiceActive(false);
    }
}


