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

    private final int WRITE_SETTINGS_REQUEST_CODE = 1111;
    private final int WRITE_STORAGE_REQUEST_CODE = 2134;

    private TextView text;
    private final Handler handler = new Handler();
    private Intent serviceIntent;

    private final BroadcastReceiver screenFlagReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals("ADD_WINDOW_FLAG_KEEP_SCREEN_ON")) {// add back window flags
                handler.post(() -> {
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    handler.post(() -> text.setText("Tap to turn off screen"));
                });
            }
        }
    };

    private final SwipeGestureDetector.SwipeListener swipeListener = () -> {
        startWakeUpDetection();

        // Turn screen off by removing flag
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        handler.post(() -> text.setText("Turning screen off"));

        Log.d("WakeUpDetection", "Tapped to turn screen off");

    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        text = findViewById(R.id.text);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        IntentFilter intentfilter = new IntentFilter();
        intentfilter.addAction("ADD_WINDOW_FLAG_KEEP_SCREEN_ON");
        LocalBroadcastManager.getInstance(TestApp.getInstance()).registerReceiver(screenFlagReceiver, intentfilter);

        serviceIntent = new Intent(this, TestService.class);
        serviceIntent.putExtra("inputExtra", "Foreground Service Example in Android");

        if(checkSystemWritePermission()) {
            Settings.System.putInt(TestApp.getInstance().getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 1000);
        } else {
            openAndroidPermissionsMenu();
        }
        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PERMISSION_GRANTED) {
            logInFile();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    WRITE_STORAGE_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == WRITE_STORAGE_REQUEST_CODE && grantResults[0] == PERMISSION_GRANTED) {
            logInFile();
        }
    }

    private void logInFile(){
        String filePath = Environment.getExternalStorageDirectory() + "/logcat.txt";

        try {
            Runtime.getRuntime().exec(new String[]{"logcat", "-f", filePath});
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        SwipeGestureDetector.getInstance().configure(swipeListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        SwipeGestureDetector.getInstance().configure(null);
    }

    private boolean checkSystemWritePermission() {
        boolean retVal;
        retVal = Settings.System.canWrite(this);
        return retVal;
    }

    private void openAndroidPermissionsMenu() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, WRITE_SETTINGS_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == WRITE_SETTINGS_REQUEST_CODE) {
            if (checkSystemWritePermission()) {
                Settings.System.putInt(TestApp.getInstance().getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 1000);
            }
        }
    }

    /**
     * Start wake up detection service if it is not running
     */
    public void startWakeUpDetection() {
        if (!TestApp.getInstance().wakeServiceActive) {
            startForegroundService(serviceIntent);
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
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(TestApp.getInstance()).unregisterReceiver(screenFlagReceiver);
        stopService(serviceIntent);
    }
}
