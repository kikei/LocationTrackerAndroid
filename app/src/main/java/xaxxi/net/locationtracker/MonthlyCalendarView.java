package net.xaxxi.locationtracker;

import java.util.Calendar;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.text.format.DateUtils;
import android.util.AttributeSet;

import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;

import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class MonthlyCalendarView extends LinearLayout {

    public interface OnDateSelectedListener {
        public void onDateSelected(Calendar calendar);
    }

    public interface OnMonthChangedListener {
        public void onMonthChanged(MonthlyCalendarView view, Calendar calendar);
    }

    Calendar mCalendar;

    int mTextColor;
    TextView mMonthView;
    TableLayout mDaysTable;
    TextView[] mDaysTextList;
    Context mContext;

    OnDateSelectedListener mDateSelectedListener;
    OnMonthChangedListener mMonthChangedListener;

    protected static final String[] weekdays = new String[] {
        DateUtils.getDayOfWeekString(Calendar.SUNDAY,
                                     DateUtils.LENGTH_MEDIUM), 
        DateUtils.getDayOfWeekString(Calendar.MONDAY,
                                     DateUtils.LENGTH_MEDIUM), 
        DateUtils.getDayOfWeekString(Calendar.TUESDAY,
                                     DateUtils.LENGTH_MEDIUM), 
        DateUtils.getDayOfWeekString(Calendar.WEDNESDAY,
                                     DateUtils.LENGTH_MEDIUM), 
        DateUtils.getDayOfWeekString(Calendar.THURSDAY,
                                     DateUtils.LENGTH_MEDIUM), 
        DateUtils.getDayOfWeekString(Calendar.FRIDAY,
                                     DateUtils.LENGTH_MEDIUM), 
        DateUtils.getDayOfWeekString(Calendar.SATURDAY,
                                     DateUtils.LENGTH_MEDIUM)
    };
    
    public MonthlyCalendarView(Context context) {
        super(context);
        mContext = context;
        initializeView(context, null);
    }

    public MonthlyCalendarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        initializeView(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        android.util.Log.d("LocationTracker.MonthlyCalendarView", "intercept");
        // super.onInterceptTouchEvent(event);
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        android.util.Log.d("LocationTracker.MonthlyCalendarView", "touch");
        // super.onTouchEvent(event);
        return false;
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // android.util.Log.d("LocationTracker.MonthlyCaledarView", "onMeasure");

        WindowManager wm =
            (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        Point p = new Point();
        display.getSize(p);

        setMeasuredDimension(p.x, p.y);
    }

    public void setCalendar(final Calendar calendar) {
        
        int year  = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) - Calendar.JANUARY + 1;
        
        if (mCalendar != null) {
            // Skip if calendar not updated
            int y = mCalendar.get(Calendar.YEAR);
            int m = mCalendar.get(Calendar.MONTH) - Calendar.JANUARY + 1;
            if (year == y && month == m) return;
        }
        
        mCalendar = (Calendar)calendar.clone();
        
        mCalendar.set(year, month - 1 + Calendar.JANUARY, 1);
        int week = mCalendar.get(Calendar.DAY_OF_WEEK);

        mMonthView.setText(Integer.toString(year) + "/" +
                           Integer.toString(month));

        final int N = daysInMonth(year, month);
        for (int i = 0; i < week - Calendar.SUNDAY; i++) {
            TextView v = mDaysTextList[7+i];
            v.setText("");
            v.setBackgroundColor(Color.argb(0, 0, 0, 0));
        }
        for (int i = week - Calendar.SUNDAY, d = 0; d < N; i++, d++) {
            TextView v = mDaysTextList[7+i];
            v.setText(Integer.toString(d+1));
            // v.setBackgroundResource(R.layout.shape_background_day_calendar);
            v.setBackgroundColor(Color.argb(0, 0, 0, 0));
            v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String dstr = ((TextView)v).getText().toString();
                        android.util.Log.d("LocationTracker." +
                                           "MonthlyCalendarView",
                                           dstr);
                        int d = -1;
                        try {
                            d = Integer.parseInt(dstr);
                        } catch (NumberFormatException e) { }
                        
                        if (d > 0) {
                            Calendar cal = (Calendar)mCalendar.clone();
                            cal.set(Calendar.DATE, d);
                            if (mDateSelectedListener != null)
                                mDateSelectedListener.onDateSelected(cal);
                        }
                    }
                });
        }
        for (int i = week - Calendar.SUNDAY + N;
             i < mDaysTextList.length - 7; i++) {
            TextView v = mDaysTextList[7+i];
            v.setText("");
            v.setBackgroundColor(Color.argb(0, 0, 0, 0));
        }
        if (mMonthChangedListener != null)
            mMonthChangedListener.onMonthChanged(this, mCalendar);
    }

    private void initializeView(Context context, AttributeSet attrs) {
        if (attrs != null) {
        }

        // LinearLayout settings
        setOrientation(LinearLayout.VERTICAL);
        setGravity(Gravity.CENTER);
        // setBackgroundColor(mBackgroundColor);

        mMonthView = new TextView(context);
        mMonthView.setGravity(Gravity.CENTER);
        mMonthView.setTextSize(40f);

        mDaysTable = new TouchableTableLayout(context);
        mDaysTable.setGravity(Gravity.CENTER);
        mDaysTable.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
                                                    LayoutParams.FILL_PARENT));
        
        mDaysTextList = new TextView[7*7];

        { // Show weekday names
            TableRow row = new TableRow(context);
            for (int d = 0; d < 7; d++) {
                TextView v = new TextView(context);
                v.setText(weekdays[d]);
                v.setGravity(Gravity.CENTER);
                v.setTextSize(30f);
                row.addView(v);
                mDaysTextList[d] = v;
            }
            mDaysTable.addView(row);
        }
        
        for (int w = 0; w < 6; w++) {
            TableRow row = new TableRow(context);
            for (int d = 0; d < 7; d++) {
                TextView v = new TextView(context);
                v.setGravity(Gravity.CENTER);
                v.setTextSize(30f);
                row.addView(v);
                mDaysTextList[7+w*7+d] = v;
            }
            mDaysTable.addView(row);
        }
        for (int d = 0; d < 7; d++) {
            mDaysTable.setColumnStretchable(d, true);
        }
        mDaysTable.setStretchAllColumns(true);

        addView(mMonthView);
        addView(mDaysTable);
    }

    public void setTextColor(int color) {
        mTextColor = color;
        for (int i = 0; i < mDaysTextList.length; i++)
            mDaysTextList[i].setTextColor(color);
    }

    public void setOnDateSelectedListener(OnDateSelectedListener listener) {
        mDateSelectedListener = listener;            
    }

    public void setOnMonthChangedListener(OnMonthChangedListener listener) {
        mMonthChangedListener = listener;
    }

    public void setDayBackgroundColor(int date, int color) {
        Calendar cal = (Calendar)(mCalendar.clone());
        
        cal.set(Calendar.DATE, 1);
        int week = cal.get(Calendar.DAY_OF_WEEK);
        
        TextView v = mDaysTextList[7 + week - Calendar.SUNDAY + date - 1];
        v.setBackgroundColor(color);
    }

    private int daysInMonth(int year, int month) {
        switch(month) {
        case 1 : return 31;
        case 3 : return 31;
        case 5 : return 31;
        case 7 : return 31;
        case 8 : return 31;
        case 10: return 31;
        case 12: return 31;
        case 4 : return 30;
        case 6 : return 30;
        case 9 : return 30;
        case 11: return 30;
        case 2 : return ((year % 4) == 0 &&
                         (year % 100) != 0 ||
                         (year % 400) == 0) ? 29 : 28;
        }
        return 0;
    }
}
