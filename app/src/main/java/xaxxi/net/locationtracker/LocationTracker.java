package net.xaxxi.locationtracker;

import java.util.AbstractMap.SimpleEntry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.Map.Entry;

import android.content.Intent;

import android.location.Criteria;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import net.xaxxi.locationtracker.LocationTrackerBrain.UpdateLevel;
import net.xaxxi.locationtracker.MainMessage.MessageCode;

public class LocationTracker {
    private static final String TAG = "LocationTracker.LocationTracker";

    // public interface Listener {
    //     public void onLocationUpdated(TreeMap<Long, Location> locations);
    //     public void onTemporaryLocationUpdated(long time, Location location);
    //     /* Part of LocationListener */
    //     public void onProviderEnabled(String provider);
    //     public void onProviderDisabled(String provider);
    //     public void onStatusChanged(String provider, int status, Bundle extras);
    //     /* GpsStatus.Listener */
    //     public void onGpsStatusChanged(int event, GpsStatus status);
    // }

    // 最小時間間隔 [msec]
    // private static const int MIN_GPS_TIME_INTERVAL = 5 * 60 * 1000; 
    static final int MIN_GPS_TIME_INTERVAL = 60 * 1000;

    // 最小距離間隔 [m]
    // private static const int MIN_GPS_DISTANCE = 10; 
    static final int MIN_GPS_DISTANCE = 0; 

    Handler mHandler;
    
    LocationManager mLocationManager;

    // SensorManager mSensorManager;
    SensorOperator mSensorOperator;
    SensorOperator.Listener mSensorOperatorListener;

    // Switching flag to call given event listener
    // This will be useful to prevent it from calling unnecessary tasks.
    boolean mToCallListener;

    Model mModel;
    LocationTrackerClient mClient;
    LocationTrackerBrain mBrain;

    long mLastSynchronized;

    StatusManager mStatus;

    public LocationTracker(Model model,
                           LocationManager manager,
                           SensorOperator sensorOperator) {
        mModel = model;
        mLocationManager = manager;

        mSensorOperator = sensorOperator;
        if (mSensorOperator != null)
            mSensorOperator.setListener(sensorOperatorListener);

        mLastSynchronized = Utilities.getTimeInMillis();
        mToCallListener = true;
        mStatus = new StatusManager();
        mClient = LocationTrackerClient.getInstance(mModel);

        mBrain = new LocationTrackerBrain(mModel, brainListener);
    }
    
    /*
     * Start GPS and location tracking.
     */ 
    public void start() {

        if (mLocationManager == null) {
            android.util.Log.e("LocationManageer",
                               "BUG: LocationTracker not initialized");
        }

        if (mLocationManager == null) {
            android.util.Log.d("LocationTracker",
                               "mLocationManager is null");
            return;
        }
        
        final Criteria criteria = new Criteria();
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        criteria.setBearingRequired(false);  // Disable direction
        criteria.setSpeedRequired(false);    // Disable speed
        criteria.setAltitudeRequired(false); // Disable altitude

        final String provider = mLocationManager.getBestProvider(criteria, true);
        if (provider == null) {
            android.util.Log.d("LocationTracker", "TODO: no provider");
            return;
        }
        android.util.Log.d("LocationTracker", "Best provider is " + provider);

        if (mStatus.isStarted()) return;
        mStatus.start();

        final Location location =
            mLocationManager.getLastKnownLocation(provider);
        if (location == null) {
            android.util.Log.d("LocationTracker", "no last location");
        } else {
            locationListener.onLocationChanged(location);
        }

        UpdateLevel updateLevel = mBrain.getUpdateLevel();
        int updateInterval = mBrain.getUpdateInterval();
        startLocationUpdates(updateLevel, updateInterval);
        
        sendMessage(MessageCode.LOCATION_UPDATE_LEVEL_CHANGED,
                    updateLevel.toString());
        sendMessage(MessageCode.LOCATION_UPDATE_INTERVAL_CHANGED,
                    Integer.valueOf(updateInterval));
        
        // For debug
        /*
        LocationManagerDummy locationManagerDummy =
            new LocationManagerDummy(new Handler());
        locationManagerDummy.
            requestLocationUpdates(LocationManager.GPS_PROVIDER,
                                   10 * 1000, // 最小時間間隔
                                   MIN_GPS_DISTANCE, // 最小距離間隔
                                   locationListener);
        */
        // locationManagerDummy.addGpsStatusListener(gpsStatusListener);
    }

    public void stop() {
        stopLocationUpdates();
    }

    public void forceStop() {
        cancelLazySwitch();
        if (!mStatus.isRunning())
            stopLocationUpdates();
        mStatus.off();
    }

    public void unforceStop() {
        cancelLazySwitch();
        if (!mStatus.isRunning())
            startLocationUpdates();
    }

    public void setHandler(Handler handler) {
        mHandler = handler;
    }

    public void switchToCallListener(boolean todo) {
        mToCallListener = todo;
    }

    public void requestLocations() {
        if (mHandler == null || !mToCallListener) return;

        sendMessage(MessageCode.LOCATION_UPDATED, mBrain.getFixedLocations());

        Entry<Long, Location> ent = mBrain.getPresumedLocation();
        if (ent != null)
            sendMessage(MessageCode.LOCATION_TEMPORARILY_UPDATED, ent);

        sendMessage(MessageCode.LOCATION_UPDATE_LEVEL_CHANGED,
                    mBrain.getUpdateLevel().toString());
        sendMessage(MessageCode.LOCATION_UPDATE_INTERVAL_CHANGED,
                    Integer.valueOf(mBrain.getUpdateInterval()));

    }

    private void startLocationUpdates() {
        startLocationUpdates(mBrain.getUpdateLevel(),
                             mBrain.getUpdateInterval());
    }

    private void startLocationUpdates(UpdateLevel level, int interval) {
                                      
        android.util.Log.i("LocationTracker.LocationTracker",
                           "Requeted start LocationUpdates" +
                           ", running=" + mStatus.isRunning() +
                           ", level=" + level +
                           ", interval=" + interval);
        
        if (mStatus.isRunning()) stopLocationUpdates();

        if (level != UpdateLevel.Low)
            mStatus.run();
        else
            mStatus.runCalmly();

        if (!mStatus.isCalm())
            mSensorOperator.stop();
        else
            mSensorOperator.start();

        mLocationManager.
            requestLocationUpdates(LocationManager.GPS_PROVIDER,
                                   interval, // 最小時間間隔 [msec]
                                   MIN_GPS_DISTANCE, // 最小距離間隔 [m]
                                   locationListener);
        mLocationManager.addGpsStatusListener(gpsStatusListener);
    }

    private void stopLocationUpdates() {
        android.util.Log.d(TAG,
                           "stopLocationUpdates" +
                           ", running=" + mStatus.isRunning());
        
        if (mStatus.isRunning()) {
            mLocationManager.removeUpdates(locationListener);
            mLocationManager.removeGpsStatusListener(gpsStatusListener);
            mStatus.off();
        }
    }

    private void saveLocation(long time, Location location) {
        // Save location
        mModel.putLocationData(time, location);

        // Sync locations
        syncronizeLocations();
    }

    private void syncronizeLocations() { 
        Long interval = mModel.getSyncInterval();
        if (interval != null) {
            long now = Utilities.getTimeInMillis();
            if (now - mLastSynchronized > interval && mClient != null) {
                mLastSynchronized = now;
                mClient.synchronize();
            }
        }
    }

    private void sendMessage(int code, Object obj) {
        Message msg = Message.obtain();
        msg.what = code;
        msg.obj = obj;
        // android.util.Log.d(TAG, "sendMessage, code=" + code +
        //                    ", mToCallListener=" + mToCallListener +
        //                    ", handler=" + mHandler);
        if (mHandler != null && mToCallListener)
            mHandler.sendMessage(msg);
    }
    
    private LocationListener locationListener =
        new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                android.util.Log.d("LocationTracker.LocationListener",
                                   "onLocationChanged: location" + location);

                long now = Utilities.getTimeInMillis();
                mBrain.commitLocation(now, location);
            };
            
            @Override
            public void onProviderEnabled(String provider) {
                android.util.Log.d("LocationTracker.LocationTracker",
                                   "onProviderEnabled provider=" + provider);
                sendMessage(MessageCode.PROVIDER_ENABLED, provider);
            }
            @Override
            public void onProviderDisabled(String provider) {
                android.util.Log.d("LocationTracker.LocationTracker",
                                   "onProviderDisabled provider=" + provider);
                sendMessage(MessageCode.PROVIDER_DISABLED, provider);
            }
            @Override
            public void onStatusChanged(String provider,
                                        int status, Bundle extras) {
                android.util.Log.d("LocationTracker.LocationTracker",
                                   "onStatusChanged provider=" + provider +
                                   ", status=" + status);
                if (mBrain != null)
                    mBrain.commitGpsProviderStatusChanged(provider, 
                                                          status, extras);
                switch (status) {
                case LocationProvider.OUT_OF_SERVICE:
                    sendMessage(MessageCode.LOCATION_OUT_OF_SERVICE, provider);
                    break;
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    sendMessage(MessageCode.LOCATION_TEMPORARILY_UNAVAILABLE,
                                provider);
                    break;
                case LocationProvider.AVAILABLE:
                    sendMessage(MessageCode.LOCATION_AVAILABLE, provider);
                    break;
                }
            }
        };

    private GpsStatus.Listener gpsStatusListener =
        new GpsStatus.Listener() {
            @Override
            public void onGpsStatusChanged(int event) {
                GpsStatus status = mLocationManager.getGpsStatus(null);
                if (mBrain != null)
                    mBrain.commitGpsStatusChanged(event, status);

                // android.util.Log.d(TAG,
                //                    "onGpsStatusChanged event=" + event);
                
                switch (event) {
                case GpsStatus.GPS_EVENT_FIRST_FIX:
                    sendMessage(MessageCode.GPS_FIRST_FIX, status);
                    break;
                case GpsStatus.GPS_EVENT_STARTED:
                    sendMessage(MessageCode.GPS_STARTED, status);
                    break;
                case GpsStatus.GPS_EVENT_STOPPED:
                    sendMessage(MessageCode.GPS_STOPPED, status);
                    break;
                case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                    sendMessage(MessageCode.GPS_SATELLITE_STATUS, status);
                    break;
                }
            }
        };

    private LocationTrackerBrain.BrainListener brainListener =
        new LocationTrackerBrain.BrainListener() {
            @Override
            public void onLocationFixed(long time, Location location) {
                android.util.Log.d("LocationTracker.LocationTracker",
                                   "onLocationFixed" +
                                   " time=" + time + 
                                   ", location=" + location +
                                   ", mToCallListener=" + mToCallListener);
                                   
                // Collect and store fixed location
                sendMessage(MessageCode.LOCATION_UPDATED,
                            mBrain.getFixedLocations());

                saveLocation(time, location);
            }
            @Override
            public void onLocationPresumed(long time, Location location) {
                android.util.Log.d("LocationTracker.LocationTracker",
                                   "onLocationPresumed time=" + time +
                                   ", location=" + location);
                // Notify latest location for early response

                Entry<Long, Location> e = new SimpleEntry(time, location);
                sendMessage(MessageCode.LOCATION_TEMPORARILY_UPDATED, e);
            }
            @Override
            public void onRequestedChangeLocationUpdates
                (UpdateLevel updateLevel, int timeInterval) {
                android.util.Log.d("LocationTracker.LocationTracker",
                                   "onGpsTimeIntervalChanged");
                stopLocationUpdates();
                startLocationUpdates(updateLevel, timeInterval);
                sendMessage(MessageCode.LOCATION_UPDATE_INTERVAL_CHANGED,
                            Integer.valueOf(timeInterval));
                sendMessage(MessageCode.LOCATION_UPDATE_LEVEL_CHANGED,
                            updateLevel.toString());
            }
        };

    private SensorOperator.Listener sensorOperatorListener =
        new SensorOperator.Listener() {
            @Override
            public void onAttitudeUpdate(float azumith,
                                         float pitch,
                                         float roll) {
                sendMessage(MessageCode.ATTITUDE_UPDATE,
                            new float[] { azumith, pitch, roll });
                // Layed-flat
                if ((nearLt(pitch, 0.0, Math.PI / 36.0f) ||
                     nearLt(pitch, Math.PI / 2.0f, Math.PI / 36.0f)) &&
                    (nearLt(roll, 0.0, Math.PI / 36.0f) ||
                     nearLt(roll, Math.PI / 2.0f, Math.PI / 36.0f))) {
                    // attemptTo(false);
                } else {
                    attemptTo(true);
                }
            }
        };

    private boolean nearLt(double x, double c, double r) {
        return Math.abs(x - c) < r;
    }

    private Timer lazySwitch = null;

    private void cancelLazySwitch() {
        if (lazySwitch != null) {
            lazySwitch.cancel();
            lazySwitch = null;
        }
    }

    private void attemptTo(boolean toStart) {

        if ((mStatus.isCalm() && toStart) ||
            (mStatus.isSleeping() && !toStart)) {
            // Cancel an attempt
            if (lazySwitch != null) {
                lazySwitch.cancel();
                lazySwitch = null;
            }
        } else if ((mStatus.isCalm() && !toStart) ||
                   (mStatus.isSleeping() && toStart)) {
            // Start new attempt
            if (lazySwitch == null) {
                android.util.Log.d(TAG,
                                   "attemptTo" +
                                   ", toStart=" + toStart);
                lazySwitch = new Timer("gps_lazy_switch");
                lazySwitch.schedule(new LazySwitchTask(toStart), 10000L);
            }
        }

        // Log.d("LocationTrackerService.onAttitudeUpdate",
        //       "gpsRunning=" + gpsRunning +
        //       ", toStart=" + toStart +
        //       ", gpsSwitch=" + lazySwitch);
        /*
        if (gpsRunning == toStart) {
            if (lazySwitch != null) {
                lazySwitch.cancel();
                lazySwitch = null;
            }
        } else {
            if (lazySwitch == null) {
                Log.d("LocationTrackerService.onAttitudeUpdate",
                      "toStart=" + toStart);
                lazySwitch = new Timer("gps_lazy_switch");
                lazySwitch.schedule(new LazySwitchTask(toStart), 10000L);
            }
        }
        */
    }

    private class LazySwitchTask extends TimerTask {
        boolean mToStart;
        
        public LazySwitchTask(boolean toStart) {
            mToStart = toStart;
        }

        private void runSwitch(final boolean toStart) {
            if (toStart) {
                startLocationUpdates();
            } else {
                stopLocationUpdates();
                mStatus.sleep();
            }
            /*
            mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (toStart) {
                            startLocationUpdates();
                        } else {
                            stopLocationUpdates();
                            mStatus.sleep();
                        }
                    }
                });
            */
        }

        @Override
        public void run() {
            android.util.Log.d(TAG,
                               "LazySwitch.run" +
                               ", mStatus=" + mStatus +
                               ", mToStart=" + mToStart);
            if ((mStatus.isCalm() && !mToStart) ||
                (mStatus.isSleeping() && mToStart))
                runSwitch(mToStart);
            // if (gpsRunning != mToStart) changeTracking(mToStart);
            lazySwitch = null;
        }
    }
    
    enum Status {
        New,
        Starting,
        
        Listening,
        Calm,
            
        Sleep,
        
        /*
         * GPS is manually stopped;
         * GPS will be keep off until on is manually called.
         */
        Off
    }

    /**
     * LocationTracker status manager.
     * 
     * <p>
     * All state of location tracker is managed by this class.
     * </p>
     */
    private class StatusManager {
        
        volatile Status mStatus;
        
        public StatusManager() {
            mStatus = Status.New;
        }

        public boolean isStarted() {
            switch (mStatus) {
            case New:
            case Starting:
                return false;
            default:
                return true;
            }
        }

        public boolean isRunning() {
            switch (mStatus) {
            case Listening:
            case Calm:
                return true;
            default:
                return false;
            }
        }

        public boolean isCalm() {
            switch (mStatus) {
            case Calm:
                return true;
            default:
                return false;
            }
        }

        public boolean isSleeping() {
            switch (mStatus) {
            case Sleep:
                return true;
            default:
                return false;
            }
        }

        public boolean isOff() {
            switch (mStatus) {
            case Off:
                return true;
            default:
                return false;
            }
        }

        public void start() {
            setStatus(Status.Starting);
        }

        public void run() {
            setStatus(Status.Listening);
        }

        public void runCalmly() {
            setStatus(Status.Calm);
        }

        public void sleep() {
            setStatus(Status.Sleep);
        }

        public void off() {
            setStatus(Status.Off);
        }

        private void setStatus(Status status) {
            android.util.Log.d(TAG, "StatusManager.setStatus status=" + status);
            mStatus = status;
        }
    }
}
