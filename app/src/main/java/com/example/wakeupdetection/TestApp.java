package com.example.wakeupdetection;

import android.app.Application;

public class TestApp extends Application {
    private static TestApp testApp;
    private boolean wakeServiceActive = false;
    private boolean isTappedToTurnScreenOff = false;

    public static TestApp getInstance() {
        return testApp;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        testApp = this;
    }

    public boolean isWakeServiceActive() {
        return wakeServiceActive;
    }

    public void setWakeServiceActive(boolean wakeServiceActive) {
        this.wakeServiceActive = wakeServiceActive;
    }

    public boolean isTappedToTurnScreenOff() {
        return isTappedToTurnScreenOff;
    }

    public void setTappedToTurnScreenOff(boolean tappedToTurnScreenOff) {
        isTappedToTurnScreenOff = tappedToTurnScreenOff;
    }
}
