package com.example.wakeupdetection;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private final Handler handler = new Handler();
    private TextView text;
    private final BroadcastReceiver screenFlagReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals(Constants.FLAG_KEEP_SCREEN_ON)) {
                handler.post(() -> {
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    handler.post(() -> text.setText(R.string.tap_to_turn_screen_off));
                });
            }
        }
    };
    private Intent serviceIntent;
    private final SwipeGestureDetector.SwipeListener swipeListener = () -> {
        startWakeUpDetection();

        // Turn screen off by removing the flag
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        handler.post(() -> text.setText(R.string.screen_is_turning_off));
        TestApp.getInstance().setTappedToTurnScreenOff(true);

        Log.d("WakeUpDetection", "Tapped to turn screen off");

    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        text = findViewById(R.id.text);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        IntentFilter intentfilter = new IntentFilter();
        intentfilter.addAction(Constants.FLAG_KEEP_SCREEN_ON);
        LocalBroadcastManager.getInstance(TestApp.getInstance()).registerReceiver(screenFlagReceiver, intentfilter);

        serviceIntent = new Intent(this, TestService.class);
        serviceIntent.putExtra(Constants.INPUT_EXTRA, "Foreground Service Example in Android");

        // decrease the timeout permission if permission is granted
        if (checkSystemWritePermission()) {
            Settings.System.putInt(TestApp.getInstance().getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 1000);
        } else {
            openAndroidPermissionsMenu();
        }

        // start writing the logcat in a file if the permission is granted
        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PERMISSION_GRANTED) {
            saveLogcatInFile();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    Constants.WRITE_STORAGE_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Constants.WRITE_STORAGE_REQUEST_CODE && grantResults[0] == PERMISSION_GRANTED) {
            saveLogcatInFile();
        }
    }

    private void saveLogcatInFile() {
        String filePath = Environment.getExternalStorageDirectory() + "/logcat.txt";

        try {
            Runtime.getRuntime().exec(new String[]{"logcat", "-f", filePath});
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        SwipeGestureDetector.getInstance().configure(swipeListener);
    }

    private boolean checkSystemWritePermission() {
        return Settings.System.canWrite(this);
    }

    private void openAndroidPermissionsMenu() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, Constants.WRITE_SETTINGS_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.WRITE_SETTINGS_REQUEST_CODE && checkSystemWritePermission()) {
            Settings.System.putInt(TestApp.getInstance().getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 1000);
        }
    }

    /**
     * Start wake up detection service if it is not running
     */
    public void startWakeUpDetection() {
        if (!TestApp.getInstance().isWakeServiceActive()) {
            startService(serviceIntent);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (SwipeGestureDetector.getInstance().onTouchEvent(ev)) {
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        SwipeGestureDetector.getInstance().configure(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        LocalBroadcastManager.getInstance(TestApp.getInstance()).unregisterReceiver(screenFlagReceiver);
        stopService(serviceIntent);
    }
}
