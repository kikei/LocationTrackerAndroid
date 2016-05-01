package net.xaxxi.locationtracker;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;

public class TouchableLinearLayout extends LinearLayout {

    public TouchableLinearLayout(Context context) {
        super(context);
    }

    public TouchableLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        android.util.Log.d("LocationTracker.TouchableLinearLayout", "intercept");
        // super.onInterceptTouchEvent(event);
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        android.util.Log.d("LocationTracker.TouchableLinearLayout", "touch");
        // super.onTouchEvent(event);
        return false;
    }
}
