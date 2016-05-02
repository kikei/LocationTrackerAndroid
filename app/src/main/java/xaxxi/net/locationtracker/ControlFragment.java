package net.xaxxi.locationtracker;

import java.util.Collection;
import java.util.TreeMap;
import java.util.Map.Entry;

import android.app.Activity;

import android.location.GpsSatellite;
import android.location.Location;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import android.util.Log;

public class ControlFragment extends Fragment {

    Activity mActivity;

    TextView mGpsTextView;
    TextView mSatsTextView;
    TextView mLocsTextView;
    TextView mFixedLocationTextView;
    TextView mTemporaryLocationTextView;
    TextView mUpdateIntervalView;
    TextView mUpdateLevelView;
    TextView mLocationTrackerStatusView;
    TextView mClockTextView;
    TextView mAttiTextView;

    ListView listView1;
    ArrayAdapter<String> listAdapter1;
    // ListView satsListView;
    // ArrayAdapter<String> satsListAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceSate) {

        View v = inflater.inflate(R.layout.fragment_control, container, false);

        mGpsTextView = (TextView)v.findViewById(R.id.gps_text_view);
        updateGpsTextView();

        mSatsTextView = (TextView)v.findViewById(R.id.satellites_text_view);
        updateSatellitesView();
        
        mLocsTextView = (TextView)v.findViewById(R.id.locations_text_view);
        mFixedLocationTextView =
            (TextView)v.findViewById(R.id.fixed_location_view);
        mTemporaryLocationTextView =
            (TextView)v.findViewById(R.id.temporary_location_view);
        mUpdateIntervalView =
            (TextView)v.findViewById(R.id.update_interval_view);
        mUpdateLevelView =
            (TextView)v.findViewById(R.id.update_level_view);
        mLocationTrackerStatusView =
            (TextView)v.findViewById(R.id.location_tracker_status_view);
        mClockTextView = (TextView)v.findViewById(R.id.clock_text_view);
        mAttiTextView = (TextView)v.findViewById(R.id.attitude_text_view);

        mSatsTextView.setText("Satellites: 0/0");
        mLocsTextView.setText("Locations: 0");
        mFixedLocationTextView.setText("Fixed Location: -");
        mTemporaryLocationTextView.setText("Current Location: -");
        mUpdateIntervalView.setText("Update interval: -");
        mUpdateLevelView.setText("Update level: -");
        updateLocationTrackerStatus();
        mClockTextView.setText("22h10m52s tracking now");
        mAttiTextView.setText("Attitude: -");
        
        listView1 = (ListView)v.findViewById(R.id.listView1);
        listAdapter1 = 
            new ArrayAdapter<String>(v.getContext(),
                                     android.R.layout.simple_list_item_1);
        listView1.setAdapter(listAdapter1);

        // satsListView = (ListView)v.findViewById(R.id.satsListView);
        // satsListAdapter =
        //     new ArrayAdapter<String>(v.getContext(),
        //                              android.R.layout.simple_list_item_1);
        // satsListView.setAdapter(satsListAdapter);
        
        createList1();

        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
    }

    @Override
    public void onStart() {
        super.onStart();
        listAdapter1.add("onStart");
    }

    @Override
    public void onStop() {
        super.onStop();
        listAdapter1.add("onStop");
    }

    private void createList1() {
        final ArrayAdapter<String> ladapter1 = listAdapter1;
        
        AdapterView.OnItemClickListener clickListener = 
            new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    ListView lv = (ListView)parent;
                    String item = (String)lv.getItemAtPosition(position);
                    ladapter1.add(item + Integer.toString(position));
                }
            };
        AdapterView.OnItemSelectedListener selectedListener = 
            new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view,
                                           int position, long id) {
                    ListView lv = (ListView)parent;
                    String item = (String)lv.getItemAtPosition(position);
                    ladapter1.add(item + "s");
                };
                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            };
        listView1.setOnItemClickListener(clickListener);
        listView1.setOnItemSelectedListener(selectedListener);

        ladapter1.add("hello");
    }

    public void addMessage(String s) {
        if (listAdapter1 != null) {
            Log.d("ControlFragment", "addMessage(" + s + ")");
            listAdapter1.add(s);
        }
    }


    public void updateLocations(TreeMap<Long, Location> locations) {
        // Ignore until TextView is prepared
        if (mLocsTextView != null)
            mLocsTextView.setText("Locations: " + locations.size());

        if (mFixedLocationTextView != null && locations.size() > 0) {
            Entry<Long, Location> entry = locations.lastEntry();
            Location loc = entry.getValue();
            String str = Earth.degreeToDMS(loc.getLatitude(),
                                           loc.getLongitude());
            mFixedLocationTextView.setText("Fixed Location: " + str);
        }
    }

    public void updateTemporaryLocation(long time, Location location) {
        if (mTemporaryLocationTextView != null) {
            String str = Earth.degreeToDMS(location.getLatitude(),
                                           location.getLongitude());
            mTemporaryLocationTextView.setText("Temporary Location: " + str);
        }
    }

    public void updateUpdateInterval(int interval) {
        if (mUpdateIntervalView != null) {
            String str = String.format("%2d", interval / 1000);
            mUpdateIntervalView.setText("Update interval: " + str);
        }
    }

    public void updateUpdateLevel(String level) {
        if (mUpdateLevelView != null)
            mUpdateLevelView.setText("Update level: " + level);
    }

    public void updateLocationTrackerStatus() {
        if (mLocationTrackerStatusView != null)
            mLocationTrackerStatusView.setText("LocationTracker status: -");
    }
    
    public void updateLocationTrackerStatus(String status) {
        if (mLocationTrackerStatusView != null)
            mLocationTrackerStatusView.setText("LocationTracker status: " +
                                               status);
    }

    private void updateSatellitesView() {
        if (mSatsTextView != null)
            mSatsTextView.setText("Satellites: -");
    }

    public void setSatellites(Iterable<GpsSatellite> sats) {
        // Ignore until TextView is prepared
        if (mSatsTextView == null) return;
        
        int satN = 0;
        int satUsed = 0;
        
        for (GpsSatellite sat : sats) {
            ++satN;
            if (sat.usedInFix()) ++satUsed;
        }
        mSatsTextView.setText("Satellites: " + satUsed + "/" + satN);
    }
    
    // public void setSatellites(Iterable<GpsSatellite> sats) {
    //     int satN = 0;
    //     int satUsed = 0;
    //     List<String> lst = new ArrayList<String>();
    //     for (GpsSatellite sat : sats) {
    //         ++satN;
    //         // String s = "";
    //         if (sat.usedInFix()) {
    //             ++satUsed;
    //             // s += "+";
    //         } else {
    //             // s += "-";
    //         }
    //         s += "%2d: %07.1f %09.1f %04.1f";
    //         s = String.format(s,
    //                           sat.getPrn(),
    //                           sat.getAzimuth(),
    //                           sat.getElevation(),
    //                           sat.getSnr());
    //         lst.add(s);
    //     }
    //     if (satsListAdapter != null) {
    //         satsListAdapter.clear();
    //         satsListAdapter.addAll(lst);
    //     }
    //     // mSatsTextView.setText("");
    // }

    private void updateGpsTextView() {
        if (mGpsTextView != null)
            mGpsTextView.setText("GPS: -");
    }

    public void notifyGpsActive(boolean active) {
        if (mGpsTextView != null)
            mGpsTextView.setText("GPS: " + (active ? "On" : "Off"));
    }

    private int degi(float rad) {
        int d = Math.round(rad / (float)Math.PI * 180f);
        d = (d + 360) % 360;
        return d;
    }

    public void setAttitude(float azumith, float pitch, float roll) {
        // Ignore until TextView is prepared
        if (mAttiTextView == null) return;

        mAttiTextView.setText("azumith=" + degi(azumith) + 
                              ", pitch=" + degi(pitch) +
                              ", roll="  + degi(roll));
    }
}
