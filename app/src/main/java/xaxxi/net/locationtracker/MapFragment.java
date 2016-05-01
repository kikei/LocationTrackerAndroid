package net.xaxxi.locationtracker;

import java.util.Calendar;
import java.util.TreeMap;

import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MapFragment
    extends Fragment
    implements MonthlyCalendarView.OnDateSelectedListener
{

    enum DisplayMode { Past, Present }

    TextView mDisplayModeText;
    GoogleMapFragment mMapFragment;
    Button mCalendarButton;
    Button mLocationButton;
    
    DisplayMode mDisplayMode = DisplayMode.Present;
    Calendar mPastCalendar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        android.util.Log.d("LocationTracker.MapFragment", "onCreateView");
        
        View v = inflater.inflate(R.layout.fragment_map, container, false);

        mMapFragment = GoogleMapFragment.newInstance();
            
        FragmentManager fmanager = getChildFragmentManager();
        FragmentTransaction transaction = fmanager.beginTransaction();
        transaction.add(R.id.fragment_map, mMapFragment);
        transaction.commit();

        mDisplayModeText = (TextView)v.findViewById(R.id.text_display_mode);
        mDisplayModeText.setText("Now");
        mDisplayModeText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mDisplayMode == DisplayMode.Present) {
                        if (mPastCalendar != null) {
                            mDisplayMode = DisplayMode.Past;
                            mMapFragment.showPastLine();
                            String datestr = showPastDate(mPastCalendar);
                            mDisplayModeText.setText(datestr);
                        }
                    } else {
                        mDisplayMode = DisplayMode.Present;
                        mMapFragment.showPresentLine();
                        mDisplayModeText.setText("Now");
                    }
                }
            });

        Button mCalendarButton =
            (Button)v.findViewById(R.id.button_calendar);
        Button mLocationButton =
            (Button)v.findViewById(R.id.button_current_location);

        mCalendarButton.setText("Cal");
        mCalendarButton.setOnClickListener(calendarButtonClickListener);

        mLocationButton.setText("Cur");
        mLocationButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mDisplayMode = DisplayMode.Present;
                    mDisplayModeText.setText("Now");
                    mMapFragment.showPresentLine();
                    mMapFragment.goCurrentLocation();
                }
            });

        return v;
    }

    @Override
    public void onDateSelected(final Calendar calendar) {
        android.util.Log.d("LocationTracker.MapFragment",
                           "date=" + calendar.get(Calendar.DATE) +
                           "time=" + Utilities.getTimeInMillis(calendar));

        // Ignore when today or future date clicked
        Calendar now = Calendar.getInstance();
        now.add(Calendar.DATE, -1);
        if (now.before(calendar)) return;

        if (mMapFragment == null) return;

        final long time = Utilities.getTimeInMillis(calendar);
        
        SQLiteDatabase db =
            ((MainActivity)getActivity()).getReadOnlyDatabase();
        
        TreeMap<Long, Location> locations =
            MainDbHelper.LocationsDatabase.getDayEntries(db, time);
        // Ignore when no matched locations exists
        if (locations.size() == 0) return;
                    
        android.util.Log.d("LocationTracker.MapFragment",
                           locations.size() + " locations");

        mDisplayMode = DisplayMode.Past;
        mPastCalendar = (Calendar)(calendar.clone());
                        
        mMapFragment.showPastLine(locations);

        String datestr = showPastDate(mPastCalendar);
        mDisplayModeText.setText(datestr);

        // Close dialog only if locations obtained
        String tag =
            CalendarDialogFragment.class.getSimpleName();
        FragmentManager manager =
            getActivity().getSupportFragmentManager();
        Fragment prev = manager.findFragmentByTag(tag);
        if (prev != null)
            ((CalendarDialogFragment)prev).dismiss();
        
    }

    public void updateLocations(TreeMap<Long, Location> locations) {
        if (mMapFragment == null) return;
        if (mDisplayMode == DisplayMode.Past) return;
        mMapFragment.updateLocations(locations);
    }

    public void updateTemporaryLocation(long time, Location location) {
        if (mMapFragment == null) return;
        if (mDisplayMode == DisplayMode.Past) return;        
        mMapFragment.updateTemporaryLocation(time, location);
    }

    View.OnClickListener calendarButtonClickListener =
        new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                SQLiteDatabase db =
                    ((MainActivity)getActivity()).getReadOnlyDatabase();
                String tag = CalendarDialogFragment.class.getSimpleName();
                FragmentManager manager =
                    getActivity().getSupportFragmentManager();
                Fragment prev = manager.findFragmentByTag(tag);
                if (prev != null) return;
                
                CalendarDialogFragment dialog =
                    CalendarDialogFragment.newInstance();
                dialog.setTargetFragment(MapFragment.this, 0);
                dialog.setDatabaseInstance(db);
                dialog.show(manager, tag);
            }
        };


    private String showPastDate(Calendar calendar) {
        int year  = calendar.get(Calendar.YEAR);
        int month =
            calendar.get(Calendar.MONTH) - Calendar.JANUARY + 1;
        int date  = calendar.get(Calendar.DATE);
        String datestr = "" + year + "-" + month + "-" + date;
        return datestr;
    }

}
