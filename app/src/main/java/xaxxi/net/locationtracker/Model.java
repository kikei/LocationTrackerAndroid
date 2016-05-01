package net.xaxxi.locationtracker;

import java.util.TreeMap;

import android.content.Context;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.preference.PreferenceManager;

/**
 * Integrated data manager
 * 
 * <p>
 * Real data storages are abstracted by this class.
 * </p>
 */
public class Model {
    private static final String TAG = "LocationTracker.Model";

    public final String PREF_USER_NAME;
    public final String PREF_SESSION_ID;
    public final String PREF_SYNC_INTERVAL;
    
    SharedPreferences mPreferences;
    SQLiteOpenHelper mDatabaseHelper;

    public Model(Context context) {
        mPreferences =
            PreferenceManager.getDefaultSharedPreferences(context);

        mDatabaseHelper = MainDbHelper.getInstance(context);
        
        Resources res = context.getResources();
        PREF_USER_NAME = res.getString(R.string.pref_user_name);
        PREF_SESSION_ID = res.getString(R.string.pref_session_id);
        PREF_SYNC_INTERVAL = res.getString(R.string.pref_sync_interval);
    }

    /**
     * Write a location data.
     * 
     * @param time a time of a location measured
     * @param location a location
     */
    public void putLocationData(long time, Location location) {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        long rowId = MainDbHelper.LocationsDatabase.save(db, time, location);
        android.util.Log.d(TAG, "saved location, rowId=" + rowId);
    }

    /**
     * Read milliseconds value for syncronizing interval from preference, 
     * and return it.
     *
     * @return milliseconds value for syncronizing interval
     */
    public Long getSyncInterval() {
        String str = getPreferenceString(mPreferences, PREF_SYNC_INTERVAL);
        Integer level = Integer.valueOf(str);
        long def = 1000L * 60 * 60 * 24; // 1 day
        
        if (level == null) return def;
        switch (level.intValue()) {
        case 0: return null; // Never sync
        case 1: return 1000L * 60 * 60 * 24; // 1 day
        case 2: return 1000L * 60 * 60; // 1 hour
        case 3: return 1000L * 60 * 10; // 10 minutes
        default:
            android.util.Log.w(TAG, "unknown sync interval");
            return def;
        }
    }

    /**
     * Write preference of syncronizing interval level.
     *
     * @param level syncronizing interval level
     */
    public void putSyncIntervalLevel(int level) {
        putPreferenceString(mPreferences, PREF_SYNC_INTERVAL,
                            Integer.toString(level));
    }

    /**
     * Read user name from preference.
     *
     * @return user name
     */ 
    public String getUserName() {
        return getPreferenceString(mPreferences, PREF_USER_NAME);
    }

    /**
     * Write user name to preference.
     *
     * @param userName user name
     */
    public void setUserName(String userName) {
        putPreferenceString(mPreferences, PREF_USER_NAME, userName);
    }

    /**
     * Read sessoin id from preference.
     *
     * @return session id
     */
    public String getSessionId() {
        return getPreferenceString(mPreferences, PREF_SESSION_ID);
    }

    /**
     * Write session id to preference.
     *
     * @param sessionId session id
     */
    public void putSessionId(String sessionId) {
        putPreferenceString(mPreferences, PREF_SESSION_ID, sessionId);
    }

    /**
     * Read locations.
     *
     * @param time read locations are only measured after specified time.
     * @return locations measured after specified time
     */
    public TreeMap<Long, Location> getLocationsLater(long time) {
        SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
        return MainDbHelper.LocationsDatabase.getLaterEntries(db, time);
    }

    /**
     * Return scores database has been initilized
     *
     * @return true when database has been initilized, false otherwise.
     */
    public boolean isScoresInitilized() {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        return MainDbHelper.ScoresDatabase.isInitialized(db);
    }

    /*
     * Write scores to database
     * 
     * <p>
     * Fill learning scores timeline by specified score.
     * </p>
     *
     * @param scores all scores to write database
     */
    public void initializeScores(double[] scores) {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        MainDbHelper.ScoresDatabase.initialize(db, scores);
    }

    /**
     * Read all learning scores.
     *
     * @return all learning scores in array
     */
    public double[] getAllScores() {
        SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
        return MainDbHelper.ScoresDatabase.getAllScores(db);
    }

    /*
     * Write learning score.
     *
     * @param segment a segment number of the segment to save new score
     * @param score new learning score
     */
    public void putScore(int segment, double score) {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        long rowId =
            MainDbHelper.ScoresDatabase.save(db, segment, score);
        android.util.Log.d("LocationTracker.LocationTrackerBrain.saveLocation",
                           "saved score; rowId=" + rowId + ", score=" + score);
    }
    
    private static Integer getPreferenceInt(SharedPreferences preferences,
                                            String name) {
        return preferences.contains(name) ?
            preferences.getInt(name, 0) : null;
    }

    private static String getPreferenceString(SharedPreferences preferences,
                                              String name) {
        return preferences.contains(name) ?
            preferences.getString(name, "") : null;
    }

    private static void putPreferenceInt(SharedPreferences preferences,
                                         String name, int value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(name, value);
        editor.commit();        
    }
    
    private static void putPreferenceString(SharedPreferences preferences,
                                            String name, String value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(name, value);
        editor.commit();        
    }
        
}
