package net.xaxxi.locationtracker;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import android.hardware.SensorManager;
import android.location.LocationManager;

public class LocationTrackerService extends Service {
    private static final String TAG = "LocationTracker.LocationTrackerService";

    LocationTracker mLocationTracker;
    LocationManager mLocationManager;
    SensorOperator mSensorOperator;

    Handler mHandler;

    public class LocalBinder extends Binder {
        LocationTrackerService getService() {
            return LocationTrackerService.this;
        }
    }

    final IBinder mBinder = new LocalBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("LocationTrackerService", "onCreate");

        mLocationManager =
            (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        
        SensorManager sensorManager =
            (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        mSensorOperator = new SensorOperator(sensorManager);

        Model model = new Model(this);
        mLocationTracker =
            new LocationTracker(model, mLocationManager, mSensorOperator);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("LocationTrackerService", "onStartCommand");
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        Log.d("LocationTrackerService", "onDestroy");
        super.onDestroy();
        mLocationTracker.stop();
        mSensorOperator.stop();
        // mDbHelper.close();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("LocationTrackerService", "onBind");
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.d("LocationTrackerService", "onRebind");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d("LocationTrackerService", "onUnbind");
        return true;
    }

    public void setHandler(Handler handler) {
        mHandler = handler;
        mLocationTracker.setHandler(handler);
    }

    /*
    public void setListener(LocationTracker.Listener listener) {
        mLocationTrackerListener = listener;
    }
    
    public void setListener(SensorOperator.Listener listener) {
        // TODO: replace by sendMessage
        // mSensorOperatorListener = listener;
    }
    */

    public void start() {
        startTracking();
        // mSensorOperator.start();
    }

    public void stop() {
        mLocationTracker.stop();
        // stopTracking();
        // mSensorOperator.stop();
    }

    public void forceStopTracking(boolean toStop) {
        if (toStop)
            mLocationTracker.forceStop();
        else
            mLocationTracker.unforceStop();
    }

    public void switchToCallListener(boolean todo) {
        mLocationTracker.switchToCallListener(todo);
    }

    public void requestLocations() {
        mLocationTracker.requestLocations();
    }

    private void startTracking() {
        mLocationTracker.start();
    }
}
