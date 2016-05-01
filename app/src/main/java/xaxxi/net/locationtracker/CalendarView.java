package net.xaxxi.locationtracker;

import java.util.Calendar;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ViewFlipper;

public class CalendarView extends ScrollView {

    static final float THRESHOLD_TURNING_CALENDAR = 200f;

    int mBackgroundColor;
    int mTextColor;
    MonthlyCalendarView mCurrentCalendar;
    MonthlyCalendarView mNextCalendar;
    MonthlyCalendarView mPrevCalendar;
    LinearLayout mContainerView;
    Context mContext;

    public CalendarView(Context context) {
        super(context);
        mContext = context;
        initialize(context, null);
    }

    public CalendarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        initialize(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        WindowManager wm =
            (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        Point p = new Point();
        display.getSize(p);
        
        screenHeight = p.y;
        android.util.Log.d("LocationTracker.CaledarView",
                           "onMeasure: screenHeight=" + screenHeight);
        
        smoothScrollTo(0, (int)Math.round(screenHeight));
        setCalendar(mCalendar);
    }

    public void setCalendar(Calendar calendar) {
        mCalendar = calendar;
        
        mCurrentCalendar.setCalendar(calendar);
        calendar.add(Calendar.MONTH, -1);
        mPrevCalendar.setCalendar(calendar);
        calendar.add(Calendar.MONTH, 2);
        mNextCalendar.setCalendar(calendar);
        calendar.add(Calendar.MONTH, -1);
    }

    double screenHeight = 0.0;
    float touchY = -1f;
    int mCurrentPage = 1;
    Calendar mCalendar;
    
    int mScrollDestination;
    
    // Prevent task from running parallel
    boolean mIsScrolling = false;

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        // super.onTouchEvent(e);
        // android.util.Log.d("LocationTracker.CalendarView", "onTouchEvent");
        float y;
        switch (e.getAction()) {
        case MotionEvent.ACTION_DOWN:
            android.util.Log.d("LocationTracker.CalendarView", "DOWN");
            touchY = e.getY();
            break;
        case MotionEvent.ACTION_UP:
            android.util.Log.d("LocationTracker.CalendarView", "UP");
            y = e.getY();
            
            // This is for detect doubled ACTION_UP and ignoring it.
            if (touchY < 0) return true;

            if (!mIsScrolling) {
                if (y - touchY >= THRESHOLD_TURNING_CALENDAR) {
                    updatePage(0);
                } else if (y - touchY <= -THRESHOLD_TURNING_CALENDAR) {
                    Calendar now = Calendar.getInstance();
                    Calendar next = (Calendar)(mCalendar.clone());
                    next.add(Calendar.MONTH, 1);
                    if (now.after(next))
                        updatePage(2);
                    else 
                        smoothScrollTo(0, (int)Math.round(screenHeight));
                } else {
                    // Cancel and revert scrolling
                    smoothScrollTo(0, (int)Math.round(screenHeight));
                }
            }

            touchY = -1f;
            break;
        case MotionEvent.ACTION_MOVE:
            if (touchY < 0) return true;
            
            float dy = e.getY() - touchY;
            int scroll = (int)Math.round(screenHeight - dy);
            android.util.Log.d("LocationTracker.CalendarView",
                               "MOVE scroll=" + scroll);
            if (!mIsScrolling) scrollTo(0, scroll);
            break;
        case MotionEvent.ACTION_CANCEL:
            android.util.Log.d("LocationTracker.CalendarView", "CANCEL");
            break;
        }
        
        return true;
    }

    private void updatePage(int page) {
        mCurrentPage = page;
        mScrollDestination = (int)Math.round(screenHeight * mCurrentPage);
        smoothScrollTo(0, mScrollDestination);
        CalendarView.this.postDelayed(newTask(), 100);

        /*
        if (mIsScrolling) {
            android.util.Log.d("LocationTracker.CalendarView",
                               "mIsScrolling=true");
            if (mCurrentPage == 0)
                mCalendar.add(Calendar.MONTH, -1);
            else if (mCurrentPage == 2)
                mCalendar.add(Calendar.MONTH, 1);
            setCalendar(mCalendar);
        } else {
            CalendarView.this.postDelayed(newTask(), 100);
        }
        */
    }

    private Runnable newTask() {
        return new Runnable() {
            public void run() {
                mIsScrolling = true;
                
                int y = getScrollY();
                android.util.Log.d("LocationTracker.CalendarView",
                                   "scrollY=" + y +
                                   ", dest=" + mScrollDestination);
                if (y != mScrollDestination) {
                    CalendarView.this.postDelayed(newTask(), 100);
                } else {
                    // Update calendar page
                    if (mCurrentPage == 0)
                        mCalendar.add(Calendar.MONTH, -1);
                    else if (mCurrentPage == 2)
                        mCalendar.add(Calendar.MONTH, 1);
                    setCalendar(mCalendar);
                    
                    mCurrentPage = 1;
                    int scroll = (int)Math.round(screenHeight);
                    scrollTo(0, scroll);
                    mIsScrolling = false;
                }
            }
        };
    }

    private void initialize(Context context, AttributeSet attrs) {

        // mBackgroundColor = 0x000000;
        // mTextColor = 0x000000;
        
        if (attrs != null) {
            /*
            String backgroundColor =
                attrs.getAttributeValue(null, "background");
            try {
                mBackgroundColor = Color.parseColor(backgroundColor);
            } catch (IllegalArgumentException e) { }
            String textColor =
                attrs.getAttributeValue(null, "color");
            try {
                mTextColor = Color.parseColor(textColor);
            } catch (IllegalArgumentException e) { }
            */
        }

        // Flip ui<
        // mFlipper = new ViewFlipper(context);
        // mFlipper.setGravity(Gravity.CENTER);
        // setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
        //                                  LayoutParams.WRAP_CONTENT));

        mCurrentCalendar = new MonthlyCalendarView(context);
        // mCurrentCalendar.setBackgroundColor(mBackgroundColor);
        // mCurrentCalendar.setTextColor(mTextColor);
        
        mNextCalendar = new MonthlyCalendarView(context);
        // mNextCalendar.setBackgroundColor(mBackgroundColor);
        // mNextCalendar.setTextColor(mTextColor);

        mPrevCalendar = new MonthlyCalendarView(context);

        mCalendar = Calendar.getInstance();

        // Set temporary calendar until scrolling is done.
        // onMeasure causes setting correct calendar.
        Calendar c = Calendar.getInstance();
        c.set(1900, Calendar.JANUARY, 1);
        mCurrentCalendar.setCalendar(c);
        mPrevCalendar.setCalendar(c);
        mNextCalendar.setCalendar(c);

        mContainerView = new TouchableLinearLayout(context);
        mContainerView.setOrientation(LinearLayout.VERTICAL);
        mContainerView.setGravity(Gravity.CENTER);
        mContainerView.
            setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
                                             LayoutParams.FILL_PARENT));

        mContainerView.addView(mPrevCalendar);
        mContainerView.addView(mCurrentCalendar);
        mContainerView.addView(mNextCalendar);
        
        addView(mContainerView);
    }

    public void setOnDateSelectedListener(MonthlyCalendarView.
                                          OnDateSelectedListener listener) {
        mCurrentCalendar.setOnDateSelectedListener(listener);
    }

    public void setOnMonthChangedListener(MonthlyCalendarView.
                                          OnMonthChangedListener listener) {
        mCurrentCalendar.setOnMonthChangedListener(listener);
    }

}
