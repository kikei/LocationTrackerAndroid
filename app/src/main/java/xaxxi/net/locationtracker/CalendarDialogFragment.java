package net.xaxxi.locationtracker;

import java.util.Calendar;
import java.util.TreeMap;

import android.app.Dialog;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.Location;
import android.support.v4.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;

public class CalendarDialogFragment extends DialogFragment {

    SQLiteDatabase mDatabase;

    public static CalendarDialogFragment newInstance() {
        CalendarDialogFragment f = new CalendarDialogFragment();
        return f;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        CalendarView v =
            (CalendarView)inflater.inflate(R.layout.fragment_dialog_calendar,
                                           null, false);

        MonthlyCalendarView.OnDateSelectedListener onDateSelectedListener =
            (MonthlyCalendarView.OnDateSelectedListener)getTargetFragment();
        v.setOnDateSelectedListener(onDateSelectedListener);

        v.setOnMonthChangedListener(monthChangedListener);

        // Force calling OnMonthChangedListener
        // v.setCalendar(Calendar.getInstance());
        
        Dialog dialog = new Dialog(getActivity());
        dialog.setContentView(v);
        return dialog;
    }

    public void setDatabaseInstance(SQLiteDatabase db) {
        mDatabase = db;
    }

    MonthlyCalendarView.OnMonthChangedListener monthChangedListener =
        new MonthlyCalendarView.OnMonthChangedListener() {
            @Override
            public void onMonthChanged(MonthlyCalendarView view,
                                       Calendar calendar) {
                if (mDatabase == null) return;

                Calendar cal = (Calendar)(calendar.clone());
                
                int year  = cal.get(Calendar.YEAR);
                int month = cal.get(Calendar.MONTH) - Calendar.JANUARY + 1;
                int n = daysInMonth(year, month);

                for (int i = 0; i < n; i++) {
                    int day = i + 1;
                    cal.set(Calendar.DATE, day);
                    long time = Utilities.getTimeInMillis(cal);
                    TreeMap<Long, Location> locations =
                        MainDbHelper.LocationsDatabase.getDayEntries(mDatabase,
                                                                     time);
                    android.util.Log.d("LocationTracker.CalendarDialogFragment",
                                       "locations year=" + year +
                                       ", month=" + month +
                                       ", i=" + i +
                                       ", size=" + locations.size() +
                                       ", time=" + time);
                    
                    double blue = locations.size() / 128.0;
                    blue = blue < 1.0 ? blue : 1.0;
                    int color = Color.argb((int)(blue * 255),
                                           0,
                                           0,
                                           255);
                    view.setDayBackgroundColor(day, color);
                }
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

        };
}
