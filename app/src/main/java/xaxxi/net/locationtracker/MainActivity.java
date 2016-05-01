package net.xaxxi.locationtracker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.database.sqlite.SQLiteDatabase;

import android.location.GpsStatus;
import android.location.GpsSatellite;
import android.location.Location;
import android.location.LocationManager;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

import android.support.v7.app.AlertDialog;;
import android.support.v7.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;

import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;

import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.ToggleButton;

import net.xaxxi.locationtracker.MainMessage.MessageCode;

public class MainActivity
    extends AppCompatActivity
    implements OnGpsOperatedListener {
    
    private static final String TAG = "LocationTracker.MainActivity";
    
    public static final int PAGER_INDEX_HELLO = 0;
    public static final int PAGER_INDEX_MAP = 1;
    public static final int PAGER_INDEX_STATUS = 2;

    LocationTrackerServiceManager mLocationTrackerServiceManager;

    MainFragmentPagerAdapter mAdapter;
    ViewPager mViewPager;

    Button mButton1;
    Button mButton2;
    ToggleButton mButtonGpsToggle;
    PopupWindow mMenuWindow;

    Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        PRNGFixes.apply();
        
        setContentView(R.layout.activity_main);

        mHandler = new MainHandler();

        FragmentManager manager = getSupportFragmentManager();
        mAdapter = new MainFragmentPagerAdapter(manager);
        mViewPager = (ViewPager)findViewById(R.id.pager);
        mViewPager.setAdapter(mAdapter);

        // View switching button
        mButton1 = (Button)findViewById(R.id.btn_1);

        mViewPager.setCurrentItem(PAGER_INDEX_HELLO);
        mButton1.setText("Map");
        
        mButton1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Button button = (Button)v;
                    int item = mViewPager.getCurrentItem();
                    if (item == PAGER_INDEX_MAP) {
                        switchPageToStatus();
                    } else {
                        switchPageToMap();
                    }
                }
            });

        // Menu button
        mButton2 = (Button)findViewById(R.id.btn_2);
        mButton2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mMenuWindow.showAtLocation(v,
                                               Gravity.BOTTOM | Gravity.RIGHT, 
                                               0, 0);
                }
            });

        mButtonGpsToggle = (ToggleButton)findViewById(R.id.btn_gps_toggle);
        mButtonGpsToggle.setChecked(true);
        CompoundButton.OnCheckedChangeListener gpsToggleListener =
            new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView,
                                             boolean checked) {
                    if (checked) {
                        onMakeGpsStart();
                    } else {
                        onMakeGpsStop();
                    }
                }
            };
        mButtonGpsToggle.setOnCheckedChangeListener(gpsToggleListener);

        CalendarView calendarView =
            (CalendarView)findViewById(R.id.view_calendar);

        mMenuWindow = new PopupWindow(MainActivity.this);
        View menuView =
            getLayoutInflater().inflate(R.layout.menu_main, null);
        mMenuWindow.setContentView(menuView);
        mMenuWindow.setOutsideTouchable(true);
        mMenuWindow.setFocusable(true);
        View settingTextView = menuView.findViewById(R.id.action_settings);
        settingTextView.setOnClickListener(new TextView.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this,
                                               MainPreferencesActivity.class);
                    startActivityForResult(intent, 0);
                }
            });

        mLocationTrackerServiceManager = new LocationTrackerServiceManager();
        mLocationTrackerServiceManager.startService(MainActivity.this);
    }

    @Override
    public void onStart() {

        super.onStart();
        android.util.Log.d("LocationTracker.MainActivity", "onStart");

        LocationManager manager =
            (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // Application launched but GPS is off
            showDialogAttemptGpsOn();
        } else if (!mLocationTrackerServiceManager.isBoundService()) {
            // Application launched
            switchPageToMap();
            mLocationTrackerServiceManager
                .withService(new LocationTrackerServiceManager
                             .ServiceBoundListener() {
                        @Override
                        public void
                            onBoundService(LocationTrackerService service) {
                            service.setHandler(mHandler);
                        }
                    });
            mLocationTrackerServiceManager.bindService(MainActivity.this);
        } else {
            // Application has been launched
            mLocationTrackerServiceManager
                .withService(new LocationTrackerServiceManager
                             .ServiceBoundListener() {
                        @Override
                        public void
                            onBoundService(LocationTrackerService service) {
                            service.switchToCallListener(true);
                            service.requestLocations();
                        }
                    });
        }
    }

    @Override
    public void openOptionsMenu() {

        android.content.res.Configuration config =
            getResources().getConfiguration();

        if((config.screenLayout &
            android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK) >
           android.content.res.Configuration.SCREENLAYOUT_SIZE_LARGE) {

            int originalScreenLayout = config.screenLayout;
            config.screenLayout =
                android.content.res.Configuration.SCREENLAYOUT_SIZE_LARGE;
            super.openOptionsMenu();
            config.screenLayout = originalScreenLayout;

        } else {
            super.openOptionsMenu();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        android.util.Log.d("LocationTracker.MainActivity", "onResume");
    }
    
    @Override
    public void onPause() {
        android.util.Log.d("LocationTracker.MainActivity", "onPause");
        super.onStop();
    }
    
    @Override
    public void onStop() {
        android.util.Log.d("LocationTracker.MainActivity", "onStop");
        super.onStop();
        mLocationTrackerServiceManager
            .withService(new LocationTrackerServiceManager
                         .ServiceBoundListener() {
                    public void onBoundService(LocationTrackerService service) {
                        service.switchToCallListener(false);
                    }
                });
    }

    @Override
    public void onDestroy() {
        android.util.Log.d("LocationTracker.MainActivity", "onDestroy");
        super.onDestroy();
        if (mLocationTrackerServiceManager != null)
            mLocationTrackerServiceManager.stopService(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        android.util.Log.d("LocationTracker.MainActivity",
                           "onActivityResult requestCode=" + requestCode +
                           ", resultCode=" + resultCode);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // Confirm whether to terminate gps or not.
            DialogInterface.OnClickListener yesListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivity.this.finish();
                    }
                };
            DialogInterface.OnClickListener noListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        moveTaskToBack(true);
                    }
                };
            new AlertDialog.Builder(MainActivity.this)
                .setTitle("Exit")
                .setMessage("Are you sure you want to terminate tracking?")
                .setPositiveButton("Yes", yesListener)
                .setNeutralButton("No", noListener)
                .setNegativeButton("Cancel", null)
                .show();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        // getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    
    /*
     * Handle GPS On/Off operation
     */
    @Override
    public void onMakeGpsStart() {
        android.util.Log.d("LocationTracker.MainActivity.onMakeGpsStart",
                           "=================");
        mLocationTrackerServiceManager
            .withService(new LocationTrackerServiceManager
                         .ServiceBoundListener() {
                    @Override
                    public void onBoundService(LocationTrackerService service) {
                        service.forceStopTracking(false);
                    }
                });
    }
    
    @Override
    public void onMakeGpsStop() {
        android.util.Log.d("LocationTracker.MainActivity.onMakeGpsStop",
              "=================");
        mLocationTrackerServiceManager
            .withService(new LocationTrackerServiceManager
                         .ServiceBoundListener() {
                    @Override
                    public void onBoundService(LocationTrackerService service) {
                        service.forceStopTracking(true);
                    }
                });

        // I have no idea for finding gps is stopped,
        // so implicitly notify deactivation to fragment.
        ControlFragment f = getControlFragment();
        f.notifyGpsActive(false);
    }

    public SQLiteDatabase getReadOnlyDatabase() {
        return MainDbHelper.getInstance(this).getReadableDatabase();
    }

    /*
    public LocationTrackerServiceManager getLocationTrackerServiceManager() {
        return mLocationTrackerServiceManager;
    }
    */

    private void showDialogAttemptGpsOn() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        DialogInterface.OnClickListener yesListener =
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Intent intent =
                        new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                    // Closing activity causes onStart
                }
            };
        DialogInterface.OnClickListener noListener =
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            };
        
        builder
            .setMessage("GPS is disabled, do you want to enable it?")
            .setCancelable(false)
            .setPositiveButton("Yes", yesListener)
            .setNegativeButton("No", noListener);
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void switchPageToMap() {
        mButton1.setText("Detail");
        mViewPager.setCurrentItem(PAGER_INDEX_MAP);
    }

    private void switchPageToStatus() {
        mButton1.setText("Map");
        mViewPager.setCurrentItem(PAGER_INDEX_STATUS);
    }
    
    private ControlFragment getControlFragment() {
        if (mAdapter != null)
            return (ControlFragment)
                mAdapter.instantiateItem(mViewPager, PAGER_INDEX_STATUS);
        return null;
    }
    
    private MapFragment getMapFragment() {
        if (mAdapter != null)
            return (MapFragment)
                mAdapter.instantiateItem(mViewPager, PAGER_INDEX_MAP);
        return null;
    }

    private void onLocationUpdated(TreeMap<Long, Location> locations) {
        android.util.Log.d("LocationTracker.MainActivity",
                           "onLocationUpdated");
        
        ControlFragment f = getControlFragment();
        if (f != null) {
            // f.addMessage("onLocationUpdated");
            f.updateLocations(locations);
        }
        
        MapFragment mf = getMapFragment();
        if (mf != null) {
            mf.updateLocations(locations);
        }
    }

    private void onTemporaryLocationUpdated(long time, Location location) {
        ControlFragment f = getControlFragment();
        if (f != null) {
            // f.addMessage("onTemporaryLocationUpdated");
            f.updateTemporaryLocation(time, location);
        }
        
        MapFragment mf = getMapFragment();
        if (mf != null) {
            mf.updateTemporaryLocation(time, location);
        }
    }

    private void onLocationUpdateIntervalChanged(int interval) {
        ControlFragment f = getControlFragment();
        if (f != null) {
            // f.addMessage("onLocationUpdateIntervalChanged" +
            //              ", interval=" + interval);
            f.updateUpdateInterval(interval);
        }
    }

    private void onLocationUpdateLevelChanged(String level) {
        ControlFragment f = getControlFragment();
        if (f != null) {
            // f.addMessage("onLocationUpdateLevelChanged" +
            //              ", level=" + level);
            f.updateUpdateLevel(level);
        }
    }
        
    private void onProviderEnabled(String provider) {
        ControlFragment cf = getControlFragment();
        if (cf != null) {
            cf.addMessage("Provider(" + provider + ") enabled");
        }
    }

    private void onProviderDisabled(String provider) {
        ControlFragment f = getControlFragment();
        if (f == null) return;
        
        f.addMessage("Provider(" + provider + ") disabled");
    }

    private void onLocationOutOfService(String provider) {
        ControlFragment f = getControlFragment();
        if (f == null) return;
        
        f.addMessage("Provider(" + provider + ") location out of service");
    }

    private void onLocationTemporarilyUnavailable(String provider) {
        ControlFragment f = getControlFragment();
        if (f == null) return;
        
        f.addMessage("Provider(" + provider + ")" +
                     " location temporarily unavailable");
    }

    private void onLocationUnavailable(String provider) {
        ControlFragment f = getControlFragment();
        if (f == null) return;
        
        f.addMessage("Provider(" + provider + ") location unavailable");
    }

    private void onGpsFirstFix(GpsStatus status) {
        ControlFragment f = getControlFragment();
        if (f == null) return;
        
        f.notifyGpsActive(true);
        f.addMessage("GPS first fix");
    }
    
    private void onGpsStarted(GpsStatus status) {
        ControlFragment f = getControlFragment();
        if (f != null) {
            f.notifyGpsActive(true);
            f.addMessage("GPS started");
        }
    }
    
    private void onGpsStopped(GpsStatus status) {
        ControlFragment f = getControlFragment();
        if (f != null) {
            f.notifyGpsActive(false);
            f.addMessage("GPS stopped");
        }
    }
    
    private void onGpsSatelliteChanged(GpsStatus status) {
        ControlFragment f = getControlFragment();
        if (f != null) {
            f.notifyGpsActive(true);
            // f.addMessage("GPS satellite changed");
            
            Iterable<GpsSatellite> sats = status.getSatellites();
            f.setSatellites(sats);
        }
    }
    
    private void onAttitudeUpdated(float azumith, float pitch, float roll) {
        ControlFragment f = getControlFragment();
        if (f != null) {
            f.setAttitude(azumith, pitch, roll);
        }
    }

    // Message dispatcher
    class MainHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            // android.util.Log.d(TAG, "MainHandler.handleMessage" +
            //                    ", msg.what=" + msg.what);
            switch (msg.what) {
            case MessageCode.LOCATION_UPDATED:
                onLocationUpdated((TreeMap<Long, Location>)msg.obj);
                break;
            case MessageCode.LOCATION_TEMPORARILY_UPDATED:
                Entry<Long, Location> entry =
                    (Entry<Long, Location>)msg.obj;
                onTemporaryLocationUpdated(entry.getKey(), entry.getValue());
                break;
            case MessageCode.LOCATION_UPDATE_INTERVAL_CHANGED:
                onLocationUpdateIntervalChanged((Integer)msg.obj);
                break;
            case MessageCode.LOCATION_UPDATE_LEVEL_CHANGED:
                onLocationUpdateLevelChanged((String)msg.obj);
                break;
            case MessageCode.PROVIDER_ENABLED:
                onProviderEnabled((String)msg.obj);
                break;
            case MessageCode.PROVIDER_DISABLED:
                onProviderDisabled((String)msg.obj);
                break;
            case MessageCode.LOCATION_OUT_OF_SERVICE:
                onLocationOutOfService((String)msg.obj);
                break;
            case MessageCode.LOCATION_TEMPORARILY_UNAVAILABLE:
                onLocationTemporarilyUnavailable((String)msg.obj);
                break;
            case MessageCode.LOCATION_AVAILABLE:
                onLocationUnavailable((String)msg.obj);
                break;
            case MessageCode.GPS_FIRST_FIX:
                onGpsFirstFix((GpsStatus)msg.obj);
                break;
            case MessageCode.GPS_STARTED:
                onGpsStarted((GpsStatus)msg.obj);
                break;
            case MessageCode.GPS_STOPPED:
                onGpsStopped((GpsStatus)msg.obj);
                break;
            case MessageCode.GPS_SATELLITE_STATUS:
                onGpsSatelliteChanged((GpsStatus)msg.obj);
                break;
            case MessageCode.ATTITUDE_UPDATE:
                float[] att = (float[])msg.obj;
                onAttitudeUpdated(att[0], att[1], att[2]);
            default:
                return;
            }
        }
    }
    
    class MainFragmentPagerAdapter extends FragmentPagerAdapter {
        
        public MainFragmentPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public Fragment getItem(int position) {
            Bundle bundle = new Bundle();

            // Log.d("MainActivity", "getItem" + Integer.toString(position));

            switch (position) {
            case PAGER_INDEX_HELLO: return new HelloFragment();
            case PAGER_INDEX_STATUS: return new ControlFragment();
            case PAGER_INDEX_MAP: return new MapFragment();
            default: return null;
            }
        }

    }
    
}
