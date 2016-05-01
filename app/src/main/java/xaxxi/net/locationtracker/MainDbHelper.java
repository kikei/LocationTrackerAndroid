package net.xaxxi.locationtracker;

import java.text.DecimalFormat;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.provider.BaseColumns;

import android.util.Log;

public class MainDbHelper extends SQLiteOpenHelper {

    private static MainDbHelper instance;
    public static synchronized MainDbHelper getInstance(Context context) {
        if (instance == null)
            instance = new MainDbHelper(context);
        return instance;
    }
    
    // Datatypes in SQLite Version
    // https://www.sqlite.org/datatype3.html
    public static final int DATABASE_VERSION = 5;
    public static final String DATABASE_NAME = "locationtracker.db";
    
    public static abstract class LocationEntry implements BaseColumns {
        public static final String TABLE_NAME = "locations";
        public static final String COLUMN_TIME = "time";
        public static final String COLUMN_LATITUDE = "latitude";
        public static final String COLUMN_LONGITUDE = "longitude";
    }

    public static abstract class ScoreEntry implements BaseColumns {
        public static final String TABLE_NAME = "scores";
        public static final String COLUMN_SEGMENT = "segment";
        public static final String COLUMN_SCORE = "score";
    }

    private static final String SQL_CREATE_LOCATIONS_TABLE =
        "CREATE TABLE " + LocationEntry.TABLE_NAME +
        "(" + LocationEntry._ID              + " INTEGER PRIMARY KEY" +
        "," + LocationEntry.COLUMN_TIME      + " INTEGER" +
        "," + LocationEntry.COLUMN_LATITUDE  + " TEXT" +
        "," + LocationEntry.COLUMN_LONGITUDE + " TEXT" +
        ")";

    private static final String SQL_DELETE_LOCATIONS_TABLE =
        "DROP TABLE IF EXISTS " + LocationEntry.TABLE_NAME;

    private static final String SQL_CREATE_SCORES_TABLE =
        "CREATE TABLE " + ScoreEntry.TABLE_NAME +
        "(" + ScoreEntry._ID            + " INTEGER PRIMARY KEY" +
        "," + ScoreEntry.COLUMN_SEGMENT + " INTEGER" +
        "," + ScoreEntry.COLUMN_SCORE   + " TEXT" +
        ")";

    private static final String SQL_DELETE_SCORES_TABLE =
        "DROP TABLE IF EXISTS " + ScoreEntry.TABLE_NAME;

    private MainDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_LOCATIONS_TABLE);
        db.execSQL(SQL_CREATE_SCORES_TABLE);
    }
    
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Recreation
        db.execSQL(SQL_DELETE_LOCATIONS_TABLE);
        db.execSQL(SQL_DELETE_SCORES_TABLE);
        onCreate(db);
    }
    
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    /**
     * Database for learning scores.
     * Scores are contained with segment number, the number is 0-origin.
     */
    public static class ScoresDatabase {
        
        private static String scoreToText(double d) {
            DecimalFormat f = new DecimalFormat("##.####");
            return f.format(d);
        }

        /**
         * Initialize scores on database.
         * 
         * @param db the database instance
         * @param scores learning scores for all segments
         * @return the row id of the finally and newly inserted row, 
         *         or -1 if an error occurred
         */
        public static long initialize(SQLiteDatabase db, double[] scores) {
            long row = 0L;
            for (int i = 0; i < scores.length; i++) {
                ContentValues values = new ContentValues();
                values.put(ScoreEntry.COLUMN_SEGMENT, i);
                values.put(ScoreEntry.COLUMN_SCORE, scores[i]);
                row = db.insert(ScoreEntry.TABLE_NAME, null, values);
            }
            return row;
        }

        /**
         * Save scores on database.
         * 
         * @param db the database instance
         * @param segment a learning segment number
         * @param score a learning score
         * @return the row id of the newly inserted row, 
         *         or -1 if an error occurred
         */
        public static long save(SQLiteDatabase db,
                                int segment, double score) {
            ContentValues values = new ContentValues();
            values.put(ScoreEntry.COLUMN_SEGMENT, segment);
            values.put(ScoreEntry.COLUMN_SCORE, score);
            return db.update(ScoreEntry.TABLE_NAME, values,
                             ScoreEntry.COLUMN_SEGMENT + "=?",
                             new String[] { String.valueOf(segment) });
        }

        /**
         * Returns whether learning scores database is initilized
         *
         * @param db the database instance
         * @return whether learning scores database is initilized
         */
        public static boolean isInitialized(SQLiteDatabase db) {
            if (db == null) {
                android.util.Log.e("LocationTracker.MainDbHelper.." +
                                   "getAllScores",
                                   "BUG: db is null");
                return false;
            }
            String[] projection = {
                ScoreEntry.COLUMN_SCORE
            };

            boolean initialized = false;
            Cursor c = db.query(ScoreEntry.TABLE_NAME,
                                projection,
                                ScoreEntry.COLUMN_SEGMENT + "=?",
                                new String[] { "0" },
                                null, null, null, "1");
            if (c != null && c.moveToFirst()) {
                if (!c.isNull(0)) {
                    String s = c.getString(0);
                    try {
                        Double.parseDouble(s);
                        initialized = true;
                    }
                    catch (NumberFormatException e) { }
                }
            }
            c.close();

            return initialized;
        }

        /**
         * Returns all tracking scores as double array for
         * learning segment.
         * <p>
         * The score is 0.0 if a score of a segment has never registered.
         * It is recommended that we register all scores.
         * </p>
         * 
         * @param db the database instance
         * @return the tracking scores read from database
         */
        public static double[] getAllScores(SQLiteDatabase db) {
            if (db == null) {
                android.util.Log.e("LocationTracker.MainDbHelper.." +
                                   "getAllScores",
                                   "BUG: db is null");
                return null;
            }

            String[] projection = {
                ScoreEntry.COLUMN_SCORE
            };

            ArrayList<Double> list = new ArrayList<Double>();
            Cursor c = db.query(ScoreEntry.TABLE_NAME,
                                projection,
                                null, null, null, null,
                                ScoreEntry.COLUMN_SEGMENT + " ASC");

            while (c != null && c.moveToNext()) {
                double score = 0.0;;
                
                if (!c.isNull(0)) {
                    String s = c.getString(0);
                    try {
                        score = Double.valueOf(s);
                    }
                    catch (NumberFormatException e) { }
                }
                list.add(score);
            }
            c.close();

            double[] scores = new double[list.size()];
            for (int i = 0; i < list.size(); i++) scores[i] = list.get(i);
            
            return scores;
        }

        /**
         * Returns the score of given segment on database.
         *
         * @param db the database instance
         * @param segment the segment number
         * @return the score of given segment
         */
        public static double getScoreOfSegment(SQLiteDatabase db, int segment) {
            if (db == null) {
                android.util.Log.e("LocationTracker.MainDbHelper." +
                                   "getScoreOfSegment",
                                   "BUG: db is null");
                return 0.0;
            }

            String[] projection = {
                ScoreEntry.COLUMN_SCORE
            };

            double score = 0.0;
            Cursor c = db.query(ScoreEntry.TABLE_NAME,
                                projection,
                                ScoreEntry.COLUMN_SEGMENT + " = ?",
                                new String[] { Integer.toString(segment) },
                                null, null, null, "1");
            if (c != null && c.moveToFirst()) {
                String s = c.getString(0);
                try {
                    score = Double.parseDouble(s);
                }
                catch (NumberFormatException e) { }
            }
            c.close();

            return score;
        }
    }

    public static class LocationsDatabase {

        private static String doubleToText(double d) {
            DecimalFormat f = new DecimalFormat("###.######");
            return f.format(d);
        }

        public static long save(SQLiteDatabase db,
                                Long time, Location location) {
            ContentValues values = new ContentValues();
            values.put(LocationEntry.COLUMN_TIME, time);
            /*
            Log.d("LocationDatabase",
                  "save(String) " + doubleToText(location.getLatitude()) +
                  ", " + doubleToText(location.getLongitude()));
            */
            values.put(LocationEntry.COLUMN_LATITUDE,
                       doubleToText(location.getLatitude()));
            values.put(LocationEntry.COLUMN_LONGITUDE,
                       doubleToText(location.getLongitude()));
            return db.insert(LocationEntry.TABLE_NAME, null, values);
        }

        public static long save(SQLiteDatabase db,
                                TreeMap<Long, Location> locations) {
            Entry<Long, Location> entry0 = getLatestEntry(db);
            SortedMap<Long, Location> locs =
                entry0 == null ?
                locations :
                locations.tailMap(entry0.getKey(), false);

            long rowId = -1;
            
            for (Entry<Long, Location> entry : locs.entrySet())
                rowId = save(db, entry.getKey(), entry.getValue());
            
            return rowId;
        }
        
        public static Entry<Long, Location> getLatestEntry(SQLiteDatabase db) {
            if (db == null) {
                Log.d("MainDbHelper.getLatestEntry", "db is null");
                return null;
            }
            
            SimpleEntry entry = null;
            
            String[] projection = {
                // LocationEntry._ID,
                LocationEntry.COLUMN_TIME,
                LocationEntry.COLUMN_LATITUDE,
                LocationEntry.COLUMN_LONGITUDE
            };
            
            Cursor c = db.query(LocationEntry.TABLE_NAME,
                                projection,
                                null, null, null, null,
                                LocationEntry.COLUMN_TIME + " DESC",
                                "1");
            if (c != null && c.moveToFirst()) {
                long time = c.getLong(0);
                Location loc = new Location("no_provider");
                String lat = c.getString(1);
                String lng = c.getString(2);
                try {
                    loc.setLatitude(Double.parseDouble(lat));
                    loc.setLongitude(Double.parseDouble(lng));
                    entry = new SimpleEntry(time, loc);
                }
                catch (NumberFormatException e) { }
            }
            c.close();
            
            return entry;
        }

        public static TreeMap<Long, Location> getLaterEntries(SQLiteDatabase db,
                                                              long time) {
            if (db == null) {
                Log.d("MainDbHelper.getLatestEntry", "db is null");
                return null;
            }
            
            TreeMap<Long, Location> map = new TreeMap<Long, Location>();
            
            String[] projection = {
                // LocationEntry._ID,
                LocationEntry.COLUMN_TIME,
                LocationEntry.COLUMN_LATITUDE,
                LocationEntry.COLUMN_LONGITUDE
            };

            Cursor c = db.query(LocationEntry.TABLE_NAME,
                                projection,
                                LocationEntry.COLUMN_TIME + " > ?",
                                new String[] { Long.toString(time) },
                                null, null,
                                LocationEntry.COLUMN_TIME + " ASC");
            while (c != null && c.moveToNext()) {
                long t = c.getLong(0);
                Location loc = new Location("no_provider");
                String lat = c.getString(1);
                String lng = c.getString(2);
                /*
                Log.d("LocationDatabase",
                      "getLaterEntries(String) " + lat + "," + lng);
                */
                try {
                    loc.setLatitude(Double.parseDouble(lat));
                    loc.setLongitude(Double.parseDouble(lng));
                    /*
                    Log.d("LocationDatabase",
                          "getLaterEntries " + Double.parseDouble(lat) +
                          ", " + Double.parseDouble(lng));
                    */
                    map.put(t, loc);
                }
                catch (NumberFormatException e) { }
            }
            c.close();
            
            return map;
        }

        public static TreeMap<Long, Location> getDayEntries(SQLiteDatabase db,
                                                            long time) {
            if (db == null) {
                Log.d("MainDbHelper.getLatestEntry", "db is null");
                return null;
            }

            TreeMap<Long, Location> map = new TreeMap<Long, Location>();
            
            String[] projection = {
                // LocationEntry._ID,
                LocationEntry.COLUMN_TIME,
                LocationEntry.COLUMN_LATITUDE,
                LocationEntry.COLUMN_LONGITUDE
            };

            long end = time + 24 * 60 * 60 * 1000L;
            
            Cursor c = db.query(LocationEntry.TABLE_NAME,
                                projection,
                                LocationEntry.COLUMN_TIME + " > ? AND " +
                                LocationEntry.COLUMN_TIME + " < ?",
                                new String[] { Long.toString(time),
                                               Long.toString(end) },
                                null, null,
                                LocationEntry.COLUMN_TIME + " ASC");
            while (c != null && c.moveToNext()) {
                long t = c.getLong(0);
                Location loc = new Location("no_provider");
                String lat = c.getString(1);
                String lng = c.getString(2);
                /*
                android.util
                    .Log.d("LocationDatabase",
                           "getLaterEntries(String) " + lat + "," + lng);
                */
                try {
                    loc.setLatitude(Double.parseDouble(lat));
                    loc.setLongitude(Double.parseDouble(lng));
                    /*
                    android.util.
                        Log.d("LocationDatabase",
                              "getLaterEntries " + Double.parseDouble(lat) +
                              ", " + Double.parseDouble(lng));
                    */
                    map.put(t, loc);
                }
                catch (NumberFormatException e) { }
            }
            c.close();
            
            return map;
        }

    }
}
