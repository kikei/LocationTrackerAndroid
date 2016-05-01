package net.xaxxi.locationtracker;

import android.support.v4.view.ViewPager;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class MainViewPager extends ViewPager {

    public MainViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /*
     * Disable swipe action
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }

    /*
     * Disable swipe action
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return false;
    }
}
