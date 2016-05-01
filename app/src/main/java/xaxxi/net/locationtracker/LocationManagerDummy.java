package net.xaxxi.locationtracker;

import android.os.AsyncTask;
import android.os.Handler;

import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;

import java.util.Timer;
import java.util.TimerTask;

/*
 * Dummy Loction generator
 */
public class LocationManagerDummy {

    public static final double START_LATITUDE  = 35.6075;
    public static final double START_LONGITUDE = 139.688;
    /*
    public static final double START_LATITUDE  = 38.2550;
    public static final double START_LONGITUDE = 140.889;
    */

    Handler mHandler;

    int mTimeInterval;
    long mLastTime;
    Location mLastLocation;

    public LocationManagerDummy(Handler handler) {
        mHandler = handler;
    }

    public void requestLocationUpdates(String provider,
                                       int timeInterval,
                                       int distanceInterval,
                                       final LocationListener listener) {
        mTimeInterval = timeInterval;
        mLastTime = 0L;
        
        mLastLocation = new Location(provider);
        mLastLocation.setLatitude(START_LATITUDE);
        mLastLocation.setLongitude(START_LONGITUDE);
        mLastLocation.setAccuracy(20);

        Timer timer = new Timer();

        TimerTask doAsyncTask = new TimerTask() {
                @Override
                public void run() {
                    mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    long now = Utilities.getTimeInMillis();
                        
                                    double lat = mLastLocation.getLatitude();
                                    double lng = mLastLocation.getLongitude();
                                
                                    lat += 0.0001 * Math.random();
                                    lng += 0.0001 * Math.random();
                                    mLastLocation.setLatitude(lat);
                                    mLastLocation.setLongitude(lng);

                                    listener.onLocationChanged(mLastLocation);

                                    mLastTime = now;
                                } catch (Exception e) {
                                }
                            }
                        });
                }
            };
        timer.schedule(doAsyncTask, 0, mTimeInterval);
    }

}
