package net.xaxxi.locationtracker;

import java.util.AbstractMap.SimpleEntry;
import java.util.Calendar;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import android.database.sqlite.SQLiteDatabase;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;

/**
 * The learning system class for location tracking.
 * <p>
 * Score means how many locations will be measured in 1 minutes;
 * </p>
 */
public class LocationTrackerBrain {
    private static final String TAG = "LocationTracker.LocationTrackerBrain";

    public interface BrainListener {
        public void onLocationFixed(long time, Location loction);
        public void onLocationPresumed(long time, Location location);
        public void onRequestedChangeLocationUpdates(UpdateLevel updateLevel,
                                                     int timeInterval);
    }

    public enum UpdateLevel { High, Midium, Low }

    // Duration in minutes of learning segment.
    static final long LEARNING_SEGMENT_DURATION_MINUTE = 10; // 10min
    static final long LEARNING_SEGMENT_DURATION =
        LEARNING_SEGMENT_DURATION_MINUTE * 60 * 1000;

    static final int MAX_UPDATE_INTERVAL = 10 * 60 * 1000; // 10 min
    static final int MIN_UPDATE_INTERVAL = 1  * 60 * 1000; // 1  min

    static final int THRESHOLD_AVAILABLE_SATELLITES = 3;

    BrainListener mBrainListener;

    LocationCollector mCollector;
    TreeMap<Long, Location> mFixedLocations;
    
    long mCurrentTime;
    Location mCurrentLocation;

    double mNewScore = 0.0;
    int mUpdateInterval = MIN_UPDATE_INTERVAL;
    UpdateLevel mUpdateLevel = UpdateLevel.High;
    
    // The number of segments.
    // Learning cycle is assumed 1 week.
    static final int SEGMENTS_NUMBER =
        (int)(60 * 24 * 7 / LEARNING_SEGMENT_DURATION_MINUTE);

    // The score used at database initialization
    static final double PRESET_SCORE = 2.0;

    // LPC coefficient.
    static final int LPC_NUMBER = 32;

    Model mModel;
    LinearPredictiveCoding mLPC;

    long mGpsLastStarted = 0L;
    long mGpsLastStopped = 0L;

    int mUsedSatellites = 0;
    
    /**
     * Constructs a new instance of {@link #LocationTrackerBrain}.
     *
     * <p>
     * Presetting database occur if learning score database has not already
     * initialized,
     * </p>
     *
     * @param model Model object
     * @param listener listener object implements BrainListener
     */
    public LocationTrackerBrain(Model model, BrainListener listener) {
        mBrainListener = listener;
        mModel = model;
        
        mCollector = new LocationCollector();
        mFixedLocations = new TreeMap<Long, Location>();

        mCurrentTime = -1L;
        mCurrentLocation = null;

        if (!mModel.isScoresInitilized()) {
            initializeDb();
        }
        double[] scores = mModel.getAllScores();
        mLPC = new LinearPredictiveCoding(LPC_NUMBER, scores);

        updateLocationUpdatesInterval();

        startScoreKeeper();
    }

    /**
     * Returns all collected fixed locations.
     *
     * @return collected fixed locations
     */
    public TreeMap<Long, Location> getFixedLocations() {
        return mFixedLocations;
    }

    /**
     * Returns the latest reasonable location.
     * 
     * @return latest reasonable location
     */
    public Entry<Long, Location> getPresumedLocation() {
        if (mCurrentLocation == null)
            return null;
        else
            return new SimpleEntry(mCurrentTime, mCurrentLocation);
    }

    /**
     * Commit location to the brain.
     *
     * @param time the time in milliseconds
     * @param location the latest location
     */
    public void commitLocation(long time, Location location) {
        locationPresumed(time, location);

        // Ignore new location if not gps condition is reasonable
        if (!isGpsAvailable()) {
            android.util.Log.d(TAG, "commitLocation, but ignored" +
                               ", satellites=" + mUsedSatellites);
            return;
        }

        boolean collected = mCollector.collect(time, location);
        if (!collected) {
            // Out of collection 
            android.util.Log.d(TAG, "collection finished");
            Entry<Long, Location> summary = mCollector.getSummary();
            mCollector.newCollection(time, location);
            locationFixed(summary.getKey(), summary.getValue());
        } else {
            // New location is collected
            android.util.Log.d(TAG, "new location collected");
        }
    }

    protected boolean isGpsAvailable() {
        // NG if not sufficient number of satellites are used
        return mUsedSatellites > THRESHOLD_AVAILABLE_SATELLITES;
    }

    public void commitGpsProviderStatusChanged(String provider, int status,
                                               Bundle extras) {
    }

    /**
     * Receive gps status data.
     *
     * @param event a gps status event type
     *        (1: started, 2: stopped, 3: first fix, 4: satellites status)
     * @param status GpsStatus object
     * @see <a href="http://developer.android.com/intl/ja/reference/android/location/GpsStatus.html">android.location.GpsStatus</a>
     */
    public void commitGpsStatusChanged(int event, GpsStatus status) {
        // android.util.Log.d(TAG, "commitGpsStatusChanged: event=" + event);

        switch (event) {
        case GpsStatus.GPS_EVENT_STARTED:
            mGpsLastStarted = Utilities.getTimeInMillis();
            break;
        case GpsStatus.GPS_EVENT_STOPPED:
            mGpsLastStopped = Utilities.getTimeInMillis();
            break;
        case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
            Iterable<GpsSatellite> sats = status.getSatellites();
            boolean gpsAvailable = true;

            int satN = 0;
            int satUsed = 0;

            for (GpsSatellite sat : sats) {
                ++satN;
                if (sat.usedInFix()) ++satUsed;
            }
            mUsedSatellites = satUsed;
            
            if (satUsed == 0) gpsAvailable = false;

            if (!gpsAvailable && mCollector.size() > 0) {
                android.util.Log.d(TAG,
                                   "current location fixed; satellites lost");
                Entry<Long, Location> summary = mCollector.getSummary();
                mCollector.flush();
                locationFixed(summary.getKey(), summary.getValue());
            }
        }
    }

    /**
     * Returns recommended time interval of gps
     *
     * @return time interval of gps
     */
    public int getUpdateInterval() {
        return mUpdateInterval;
    }

    /**
     * Returns recommeded update level
     *
     * @return update level
     */
    public UpdateLevel getUpdateLevel() {
        return mUpdateLevel;
    }

    /**
     * Calculate and return recommended time interval of location updates
     * for current segment.
     *
     * <p>
     * This updates mUpdateInterval and mUpdateLevel.
     * </p>
     */
    private void updateLocationUpdatesInterval() {
        double next = predictNextScore();
        int interval;
        UpdateLevel level;

        next += mNewScore;

        if (next < 0.5) {
            interval = MAX_UPDATE_INTERVAL;
            level = UpdateLevel.Low;
        } else if (0.5 <= next && next < 5.0) {
            interval = (int)Math.round(MAX_UPDATE_INTERVAL / (2.0 * next));
            level = UpdateLevel.Midium;
        } else {
            interval = MIN_UPDATE_INTERVAL;
            level = UpdateLevel.High;
        }
        android.util.Log.d(TAG,
                           "getLocationUpdatesInterval" +
                           " mNewScore=" + mNewScore +
                           ", predictedNextScore=" + next + 
                           ", updateInterval=" + interval +
                           ", updateLevel=" + level);
        mUpdateInterval = interval;
        mUpdateLevel = level;
    }

    /**
     *
     * @param time the time in milliseconds 
     * @param locations the collected time and location
     */
    /*
    protected void commit(long time, TreeMap<Long, Location> locations) {
    }
    */
    protected double predictNextScore() {
        Calendar calendar = Utilities.getCalendar();
        int seg = getLearningSegmentNumber(calendar);

        double[] scores = mModel.getAllScores();

        double[] data = new double[LPC_NUMBER];
        if (seg >= LPC_NUMBER) {
            for (int i = 0; i < LPC_NUMBER; i++)
                data[i] = scores[seg - LPC_NUMBER + i];
        } else {
            for (int i = 0; i < LPC_NUMBER - seg; i++)
                data[i] = scores[SEGMENTS_NUMBER - (LPC_NUMBER - seg) + i];
            for (int i = 0; i < seg; i++)
                data[LPC_NUMBER - seg + i] = scores[i];
        }
        double next = mLPC.predictNext(data);

        android.util.Log.d(TAG,
                           "predictNextScore seg=" + seg +
                           ", scores[seg]=" + scores[seg] +
                           ", next=" + next);

        return Math.max(next, scores[seg]);
    }

    /**
     * Calculate new score of segment of given time and save it on database.
     *
     * @param segment the learning segment to update
     * @param score the latest score
     */
    protected void updateScore(int segment, double score) {

        double[] scores = mModel.getAllScores();

        double score0 = scores[segment];
        double score1 = calcScore(score0, score);

        android.util.Log.d(TAG,
                           "new score, score=" + score +
                           ", score0=" + score0 + ", score1=" + score1);

        scores[segment] = score1;
        mModel.putScore(segment, score1);

        mLPC = new LinearPredictiveCoding(LPC_NUMBER, scores);
    }

    /**
     * Initialize score database by presetting scores
     */
    private void initializeDb() {
        android.util.Log.i(TAG, "initializing scores database...");
        double[] scores = new double[SEGMENTS_NUMBER];
        for (int i = 0; i < scores.length; i++) {
            scores[i] = PRESET_SCORE;
        }
        mModel.initializeScores(scores);
        android.util.Log.i(TAG, "initializing done!");
    }

    protected void locationPresumed(long time, Location location) {
        mCurrentTime = time;
        mCurrentLocation = location;
        if (mBrainListener != null)
            mBrainListener.onLocationPresumed(time, location);
    }

    protected void locationFixed(long time, Location location) {
        mFixedLocations.put(time, location);
        if (mBrainListener != null)
            mBrainListener.onLocationFixed(time, location);

        Calendar calendar = Utilities.getCalendarByTimeInMillis(time);
        int seg = getLearningSegmentNumber(calendar);

        long from = Utilities.getTimeInMillis(getWeekStartOf(calendar));

        SortedMap<Long, Location> latests = mFixedLocations.tailMap(from, true);
        double score = getScore(latests.size());
        mNewScore = score;

        android.util.Log.d(TAG,
                           "updateScore" +
                           " time=" + time +
                           ", calendar=" + calendar +
                           ", from=" + from +
                           ", score=" + score +
                           ", seg=" + seg);

        int lastInterval = mUpdateInterval;
        UpdateLevel lastLevel = mUpdateLevel;
        updateLocationUpdatesInterval();
        if (mUpdateInterval != lastInterval ||
            mUpdateLevel    != lastLevel) 
            // Request LocationUpdates control
            mBrainListener
                .onRequestedChangeLocationUpdates(mUpdateLevel, mUpdateInterval);
    }

    private void startScoreKeeper() {
        Handler handler = new Handler();
        Calendar calendar = Utilities.getCalendar();
        long now = Utilities.getTimeInMillis(calendar);
        int seg = getLearningSegmentNumber(calendar);
        long to = getStartTimeOfSegment(calendar, seg + 1);
        handler.postDelayed(new ScoreKeeperTask(calendar, seg), to - now);
    }

    private class ScoreKeeperTask implements Runnable {
        final Calendar mCalendar;
        final int mSegment;
        /**
         * Create ScoreKeeperTask instance.
         *
         * @param calendar current time
         * @param segment target segment to update
         */
        public ScoreKeeperTask(Calendar calendar, int segment) {
            mCalendar = calendar;
            mSegment = segment;
        }

        /**
         * Check if gps has runned during the segment.
         *
         * <p>
         * This function checks if gps is running now
         * (the time gps started is later than that gps stopped), or
         * gps was stopped in current segment (the time gps stopped is later
         * than that current segment begins).
         * </p>
         *
         * @return true if gps has runned.
         */
        private boolean toUpdateScore() {
            long segmentStart = getStartTimeOfSegment(mCalendar, mSegment);
            return
                mGpsLastStopped < mGpsLastStarted ||
                segmentStart < mGpsLastStopped;
        }
        
        @Override
        public void run() {
            boolean update = toUpdateScore();
            
            long segmentStart = getStartTimeOfSegment(mCalendar, mSegment);
            android.util.Log.d(TAG, "ScoreKeeperTask run" +
                               ", update=" + update +
                               ", segment=" + mSegment +
                               ", last gps start=" + mGpsLastStarted +
                               ", last gps stop=" + mGpsLastStopped +
                               ", segment start=" + segmentStart);
            if (update) {
                SortedMap<Long, Location> latests =
                    mFixedLocations.tailMap(segmentStart, true);
                double score = getScore(latests.size());
                mNewScore = score;
                updateScore(mSegment, score);

                int lastInterval = mUpdateInterval;
                UpdateLevel lastLevel = mUpdateLevel;
                updateLocationUpdatesInterval();
                if (mUpdateInterval != lastInterval ||
                    mUpdateLevel    != lastLevel) 
                    // Request LocationUpdates control
                    mBrainListener
                        .onRequestedChangeLocationUpdates(mUpdateLevel,
                                                          mUpdateInterval);
            }
            try {
                Thread.sleep(LEARNING_SEGMENT_DURATION / 10L);
            } catch (InterruptedException e) {
                android.util.Log.w(TAG, "scorekeeper interrupted");
            }
            startScoreKeeper();
        }
    }

    /**
     * Return score from the number of locations.
     *
     * @param size the number of fixed locations
     * @return score calculated score
     */
    protected static double getScore(int size) {
        return (double)size / LEARNING_SEGMENT_DURATION_MINUTE;
    }

    /**
     * Return new score calculated by old and new scores.
     * <p>
     * Score is calculated by
     * \( s_{n+1}=\frac{s_{n}+s}{1+\frac{1}{e}} \)
     * </p>
     *
     * @param score0 the old score
     * @param score the new score
     * @return calculated score
     */
    protected static double calcScore(double score0, double score) {
        return (score + score0 / Math.E) / (1.0 + 1.0 / Math.E);
    }

    /**
     * Return datetime of the start of week given.
     * <p>
     * This assumes that a week begins from Sunday and so
     * the start of a week as 00:00:00 in Sunday.
     * </p>
     * 
     * @param calendar The datetime of week
     * @return the start datetime of week given as parameter
     */
    protected static Calendar getWeekStartOf(Calendar calendar) {
        int weekday = calendar.get(Calendar.DAY_OF_WEEK);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        int msec = calendar.get(Calendar.MILLISECOND);
        
        Calendar result = (Calendar)calendar.clone();
        result.add(Calendar.DATE, Calendar.SUNDAY - weekday);
        result.add(Calendar.HOUR_OF_DAY, -hour);
        result.add(Calendar.MINUTE, -minute);
        result.add(Calendar.SECOND, -second);
        result.add(Calendar.MILLISECOND, -msec);

        return result;
    }
    
    protected static int getLearningSegmentNumber(Calendar calendar) {
        Calendar base = getWeekStartOf(calendar);
        long tbase = Utilities.getTimeInMillis(base);
        long tcale = Utilities.getTimeInMillis(calendar);
        if (tcale > tbase) {
            return (int)((tcale - tbase) / LEARNING_SEGMENT_DURATION);
        } else {
            android.util.Log.e(TAG,
                               "BUG: tbase > tcale");
            return -1;
        }
    }

    protected static long getStartTimeOfSegment(Calendar calendar,
                                                int segment) {
        long weekStart = Utilities.getTimeInMillis(getWeekStartOf(calendar));
        long segStart = weekStart + segment * LEARNING_SEGMENT_DURATION;
        return segStart;
    }
}
