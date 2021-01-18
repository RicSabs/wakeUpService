package com.example.wakeupdetection;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;

/**
 * Singleton class for observing and delivering swiping events on any view
 */
public class SwipeGestureDetector implements GestureDetector.OnGestureListener {

    private static SwipeGestureDetector swipeGestureDetector;
    private GestureDetector gestureDetector;
    private SwipeListener swipeListener;

    public SwipeGestureDetector(Context context) {
        gestureDetector = new GestureDetector(context, this);
    }

    public static SwipeGestureDetector getInstance() {
        if (swipeGestureDetector == null) {
            swipeGestureDetector = new SwipeGestureDetector(TestApp.getInstance());
        }
        return swipeGestureDetector;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        if (swipeListener != null) {
            swipeListener.onTap();
        }
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }


    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        return gestureDetector.onTouchEvent(motionEvent);
    }

    public void configure(SwipeListener swipeListener) {
        this.swipeListener = swipeListener;
    }

    /**
     * interface to deliver swiping events to the expected view
     */
    public interface SwipeListener {
        void onTap();
    }

}
