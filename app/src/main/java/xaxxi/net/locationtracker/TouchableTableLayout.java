package net.xaxxi.locationtracker;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.TableLayout;

public class TouchableTableLayout extends TableLayout {

    public TouchableTableLayout(Context context) {
        super(context);
    }

    public TouchableTableLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        android.util.Log.d("LocationTracker.TouchableTableLayout", "intercept");
        // super.onInterceptTouchEvent(event);
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        android.util.Log.d("LocationTracker.TouchableTableLayout", "touch");
        // super.onTouchEvent(event);
        return false;
    }
}
