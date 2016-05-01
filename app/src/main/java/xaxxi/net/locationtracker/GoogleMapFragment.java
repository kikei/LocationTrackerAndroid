package net.xaxxi.locationtracker;

import java.util.Calendar;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.Location;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.LatLng;

import android.util.Log;

public class GoogleMapFragment
    extends SupportMapFragment
    implements OnMapReadyCallback {

    GoogleMap mMap;

    // When latest updateLocations was called
    Long mLastUpdated = 0L;
    Entry<Long, Location> mLastEntry = null;

    boolean mFollowLocation = true;
    
    Polyline mPresentLine = null; 
    Polyline mPastLine = null;
    Circle mPresentCircle = null;

    public static GoogleMapFragment newInstance() {
        return new GoogleMapFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        getMapAsync(this);        
        
        return v;
    }

    // Interface OnMapReadyCallback
    // https://developers.google.com/android/reference/com/google/android/gms/maps/OnMapReadyCallback
    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;

        // TODO: Show last position
                
        UiSettings settings = mMap.getUiSettings();
        settings.setAllGesturesEnabled(false);
        
        settings.setMyLocationButtonEnabled(false);
        settings.setZoomControlsEnabled(false);
        settings.setCompassEnabled(true);
        settings.setScrollGesturesEnabled(true);
        settings.setZoomGesturesEnabled(true);

        if (mPresentLine != null)
            mPresentLine.remove();

        PolylineOptions options = new PolylineOptions();
        options
            .width(4)
            .color(Color.RED)
            .geodesic(true);
        mPresentLine =
            mMap.addPolyline(options);

        SQLiteDatabase db =
            ((MainActivity)getActivity()).getReadOnlyDatabase();

        long today = getFirstTimeOfToday();
        TreeMap<Long, Location> locs =
            MainDbHelper.LocationsDatabase.getLaterEntries(db, today);

        android.util.Log.d("LocationTracker.GoogleMapFragment",
                           "now=" + Utilities.getTimeInMillis());
        android.util.Log.d("LocationTracker.GoogleMapFragment",
                           "today=" + today);
        android.util.Log.d("LocationTracker.GoogleMapFragment",
                           "locs size=" + locs.size());
        
        if (locs.size() > 0) {
            // Resume today's history
            mLastEntry = locs.lastEntry();
            LatLng pos1 =
                new LatLng(mLastEntry.getValue().getLatitude(),
                           mLastEntry.getValue().getLongitude());

            CameraPosition.Builder cam =
                new CameraPosition.Builder();
            cam.target(pos1);
            cam.zoom(14.0f);

            CameraUpdate camera =
                CameraUpdateFactory.newCameraPosition(cam.build());
            mMap.animateCamera(camera);
            mFollowLocation = false;

            List<LatLng> points = mPresentLine.getPoints();
            
            for (Entry<Long, Location> loc : locs.entrySet()) {
                LatLng p =
                    new LatLng(loc.getValue().getLatitude(),
                               loc.getValue().getLongitude());
                points.add(p);
            }
            
            mPresentLine.setPoints(points);
        }
    }

    public void goCurrentLocation() {
        if (mLastEntry == null || mMap == null) return;
                    
        LatLng pos1 =
            new LatLng(mLastEntry.getValue().getLatitude(),
                       mLastEntry.getValue().getLongitude());
        
        CameraPosition.Builder cam = new CameraPosition.Builder();
        cam.target(pos1);
        cam.zoom(mMap.getCameraPosition().zoom);

        CameraUpdate camera =
            CameraUpdateFactory.newCameraPosition(cam.build());
        mMap.animateCamera(camera);
    }

    public void updateLocations(TreeMap<Long, Location> locations) {
        if (locations.size() > 0)
            mLastEntry = locations.lastEntry();

        android.util.Log.d("LocationTracker.GoogleMapFragment",
                           "locations size=" + locations.size() +
                           ", mLastUpdated=" + mLastUpdated);

        // Ignore all location until onMapReady is called.
        if (mMap == null) return;

        SortedMap<Long, Location> locs = locations.tailMap(mLastUpdated, false);

        android.util.Log.d("LocationTracker.GoogleMapFragment",
                           "updateLocations locs.size()=" + locs.size() +
                           ", mLastUpdated=" + mLastUpdated);

        if (locs.size() > 0) {
            long lastTime = locs.lastKey();

            // Update last updated time only when view is updated
            mLastUpdated = lastTime;

            Location lastLocation = locs.get(lastTime);
            LatLng lastPos = new LatLng(lastLocation.getLatitude(),
                                        lastLocation.getLongitude());
            if (mFollowLocation) {
                CameraPosition.Builder cam = new CameraPosition.Builder();
                cam.target(lastPos);
                cam.zoom(14.0f);

                CameraUpdate camera =
                    CameraUpdateFactory.newCameraPosition(cam.build());
                mMap.animateCamera(camera);
                mFollowLocation = false;
            }
            
            List<LatLng> points = mPresentLine.getPoints();
            for (Entry<Long, Location> entry : locs.entrySet()) {
                Location location = entry.getValue();
                points.add(new LatLng(location.getLatitude(),
                                      location.getLongitude()));
            }
            android.util.Log.d("LocationTracker.GoogleMapFragment",
                               "updateLocations" +
                               " points.size()=" + points.size());
            mPresentLine.setPoints(points);
        }
    }

    public void updateTemporaryLocation(long time, Location location) {

        // Ignore all location until onMapReady is called.
        if (mMap == null) return;

        LatLng pos = new LatLng(location.getLatitude(),
                                location.getLongitude());
        if (mFollowLocation) {
            CameraPosition.Builder cam = new CameraPosition.Builder();
            cam.target(pos);
            cam.zoom(14.0f);

            CameraUpdate camera =
                CameraUpdateFactory.newCameraPosition(cam.build());
            mMap.animateCamera(camera);
            mFollowLocation = false;
        }
            
        if (location.hasAccuracy()) {
            setPresentCircle(pos, location.getAccuracy());
        } else {
            mPresentCircle.setVisible(false);
        }
    }

    private void setPresentCircle(LatLng pos, double accuracy) {
        if (mPresentCircle == null) {
            android.util.Log.d("LocationTracker.GoogleMapFragment",
                               "accuracy initialized: " +
                               accuracy);
            CircleOptions options = new CircleOptions();
            options
                .strokeColor(Color.RED)
                .fillColor(Color.argb(64, 255, 0, 0))
                .strokeWidth(1.0f)
                .center(pos)
                .radius(accuracy);
            mPresentCircle = mMap.addCircle(options);
        } else {
            android.util.Log.d("LocationTracker.GoogleMapFragment",
                               "accuracy updated: " +
                               accuracy);
            mPresentCircle.setVisible(true);
            mPresentCircle.setCenter(pos);
            mPresentCircle.setRadius(accuracy);
        }
    }

    public void showPresentLine() {
        if (mMap == null) return;
        
        if (mPresentLine != null)
            mPresentLine.setVisible(true);
        if (mPresentCircle != null)
            mPresentCircle.setVisible(true);
        if (mPastLine != null)
            mPastLine.setVisible(false);
    }

    public void showPastLine() {
        if (mMap == null) return;
        
        if (mPresentLine != null)
            mPresentLine.setVisible(false);
        if (mPresentCircle != null)
            mPresentCircle.setVisible(false);
        if (mPastLine != null)
            mPastLine.setVisible(true);
    }
    
    public void showPastLine(TreeMap<Long, Location> locations) {
        android.util.Log.d("LocationTracker.GoogleMapFragment",
                           "locations size=" + locations.size());

        // Ignore all location until onMapReady is called.
        if (mMap == null) return;

        if (locations.size() == 0) return;

        if (mPresentLine != null)
            mPresentLine.setVisible(false);
        if (mPresentCircle != null)
            mPresentCircle.setVisible(false);
        if (mPastLine != null)
            mPastLine.remove();

        Location lastLocation = locations.get(locations.lastKey());
        LatLng lastPos = new LatLng(lastLocation.getLatitude(),
                                    lastLocation.getLongitude());
        
        CameraPosition.Builder cam = new CameraPosition.Builder();
        cam.target(lastPos);
        cam.zoom(14.0f);

        CameraUpdate camera =
            CameraUpdateFactory.newCameraPosition(cam.build());
        mMap.animateCamera(camera);
                
        PolylineOptions options = new PolylineOptions();
        options
            .width(4)
            .color(Color.BLUE)
            .geodesic(true);
        mPastLine = mMap.addPolyline(options);

        List<LatLng> points = mPastLine.getPoints();

        for (Entry<Long, Location> entry : locations.entrySet()) {
            Location location = entry.getValue();
            points.add(new LatLng(location.getLatitude(),
                                  location.getLongitude()));
        }

        mPastLine.setPoints(points);
    }
    
    // Utility
    private long getFirstTimeOfToday() {
        Calendar c = Calendar.getInstance();
        c.set(c.get(Calendar.YEAR),
              c.get(Calendar.MONTH),
              c.get(Calendar.DATE),
              0, 0, 0);
        return Utilities.getTimeInMillis(c);
    }
}
