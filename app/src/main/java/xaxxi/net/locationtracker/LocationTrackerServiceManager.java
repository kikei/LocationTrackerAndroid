package net.xaxxi.locationtracker;

import java.util.LinkedList;
import java.util.Queue;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

/**
 * The manager class for LocationTrackerService.
 * <p>
 * LocationTrackerService is started, bound, stop and used by this object.
 * </p>
 */
public class LocationTrackerServiceManager {
    private static final String TAG =
        "LocationTracker.LocationTrackerServiceManager";

    LocationTrackerService mLocationTrackerService;
    Queue<ServiceBoundListener> mServiceBoundListeners;

    public interface ServiceBoundListener {
        public void onBoundService(LocationTrackerService service);
    }

    public LocationTrackerServiceManager() {
        mServiceBoundListeners = new LinkedList<ServiceBoundListener>();
    }

    public ComponentName startService(Context context) {
        return
            context.startService(new Intent(context,
                                            LocationTrackerService.class));
    }

    public boolean isBoundService() {
        return mLocationTrackerService != null;
    }

    public boolean bindService(Context context) {
        return
            context.bindService(new Intent(context,
                                           LocationTrackerService.class),
                                mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    public void stopService(Context context) {
        if (mLocationTrackerService != null) {
            mLocationTrackerService.stop();
            context.unbindService(mServiceConnection);

            mLocationTrackerService = null;
            mServiceConnection = null;
            context.stopService(new Intent(context,
                                           LocationTrackerService.class));
        }
    }

    public void withService(ServiceBoundListener listener) {
        mServiceBoundListeners.add(listener);
        callServiceBoundListeners();
    }
    
    private void callServiceBoundListeners() {
        if (mLocationTrackerService != null) {
            ServiceBoundListener listener = mServiceBoundListeners.poll();
            while (listener != null) {
                listener.onBoundService(mLocationTrackerService);
                listener = mServiceBoundListeners.poll();
            }
        }
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className,
                                           IBinder service) {
                mLocationTrackerService =
                    ((LocationTrackerService.LocalBinder)service).getService();
                callServiceBoundListeners();
                mLocationTrackerService.start();
                mLocationTrackerService.requestLocations();
            }
            public void onServiceDisconnected(ComponentName className) {
                // Called on unexpected unbind occuring
                mLocationTrackerService = null;
                android.util.Log.w(TAG, "onServiceDisconnected");
            }
        };
    
}
