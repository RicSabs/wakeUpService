package com.example.wakeupdetection;

import android.app.Application;

public class TestApp extends Application {
    private static TestApp testApp;
    public boolean wakeServiceActive = false;
    public boolean isTappedToTurnScreenOff = false;

    @Override
    public void onCreate() {
        super.onCreate();
        testApp = this;
    }

    public static TestApp getInstance() {
        return testApp;
    }
}
