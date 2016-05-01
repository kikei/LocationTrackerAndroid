package net.xaxxi.locationtracker;

import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.AbstractMap.SimpleEntry;

import android.location.Location;

public class LocationCollector {
    TreeMap<Long, Location> mCollecting;
    double[] mStaying;

    /**
     * Constructs new LocationSummarizer instance.
     */
    public LocationCollector() {
        mCollecting = new TreeMap<Long, Location>();
        mStaying = null;
    }

    /**
     * Returns the number of locations under summarization.
     *
     * @return the number of locaions under summarization
     */
    public int size() {
        return mCollecting.size();
    }

    /**
     * Returns the latest location
     *
     * @return the latest location collected
     */
    public Entry<Long, Location> getLatest() {
        if (mCollecting.size() == 0) return null;
        
        long t = mCollecting.firstKey();
        Location l = mCollecting.get(t);

        return new SimpleEntry(t, l);
    }

    /**
     * Returns summary of collecting locations.
     * <p>
     * Summary has pair value; time and location.
     * </p>
     * <p>
     * Time is setted as same value of time when location collected at first.
     * </p>
     * <p>
     * Location consists of latitude, longitude and accuracy.
     * They are calculated from summarized region of collected locations.
     * Latitude and longitude are the balance of the region.
     * Accuracy is size of the region.
     * </p>
     *
     * @return the summary of collecting locations, or
     *         null if no locations are collected.
     */
    public Entry<Long, Location> getSummary() {
        if (mCollecting.size() == 0) {
            return null;
        }
        
        long t = mCollecting.firstKey();
        Location l = mCollecting.get(t);
        
        double[] balance = calcBalance(mStaying);
        double[] accuracy = calcAccuracy(mStaying);

        Location sum = null;
        sum = new Location(l);
        sum.setLatitude(balance[0]);
        sum.setLongitude(balance[1]);
        sum.setAccuracy((float)accuracy[0]);
        
        return new SimpleEntry(t, sum);
    }

    /**
     * Adds new location to collection, and
     * returns whether summarization is finished.
     * <p>
     * Summarization is finished when new location is out of summarizable range.
     * </p>
     * 
     * @param time the time when location measured
     * @param location the location for summarizing
     * @return whether summarization is finished
     */
    public boolean collect(long time, Location location) {
        
        double lat0 = location.getLatitude();  // deg
        double lng0 = location.getLongitude(); // deg
        float  acc0 = location.getAccuracy();  // m
        
        double lat0rad = Earth.degreeToRadian(lat0); // rad
        double lng0rad = Earth.degreeToRadian(lng0); // rad

        // rad
        double alat0 = acc0 / Earth.PrimeVerticalRadiusOfCurvature(lat0rad);
        double alng0 = acc0 / Earth.MeridianRadiusOfCurvature(lat0rad);

        double dlat = Earth.radianToDegree(alat0);
        double dlng = Earth.radianToDegree(alng0);

        double lat0Min = lat0 - dlat; // deg
        double lat0Max = lat0 + dlat; // deg

        double lng0Min = lng0 - dlng; // deg
        double lng0Max = lng0 + dlng; // deg

        android.util.Log.d("LocationTracker.LocationSummarizer.summarize",
                           "region={" + lat0Min + ", " + lat0Max +
                           ", " + lng0Min + ", " + lng0Max + "}");

        double[] received = new double[] { lat0Min, lat0Max,
                                           lng0Min, lng0Max };
        double[] collision;
        
        if (mStaying == null) {
            collision = received;
            android.util.Log.d("LocationTracker.LocationSummarizer.summarize",
                               "initial region obtained; mStaying is null, " +
                               "collision={" + collision[0] +
                               ", " + collision[1] +
                               ", " + collision[2] +
                               ", " + collision[3] + "}");
        } else {
            collision = calcCollision(mStaying, received);
            android.util.Log.d("LocationTracker.LocationSummarizer.summarize",
                               "calcCollision; mStaying is not null, " +
                               "mCollecting.size()=" + mCollecting.size() +
                               ", collision=" +
                               (collision == null ? "null" :
                                ("{" + collision[0] +
                                 ", " + collision[1] +
                                 ", " + collision[2] +
                                 ", " + collision[3] + "}")));
        }

        if (collision != null) {
            mCollecting.put(time, location);
            mStaying = collision;
            return true;
        } else {
            return false;
        }
    }

    public void flush() {
        mCollecting.clear();
        mStaying = null;
    }

    public void newCollection(long time, Location location) {
        flush();
        collect(time, location);
    }

    /**
     * Calc collision of two rectangles.
     * <pre>
     * (r0[0], r0[2]) *------+
     *                |      |
     *                |      |
     *                +------+ (r0[1], r0[3])
     * </pre>
     *
     * @param r0 points of array1
     * @param r1 points of array2
     * @return collision of two rectangles,
     *         or null if two rectangles have no collision.
     */
    private double[] calcCollision(double[] r0, double[] r1) {
        double lat00 = r0[0];
        double lat01 = r0[1];
        double lng00 = r0[2];
        double lng01 = r0[3];
        
        double lat10 = r1[0];
        double lat11 = r1[1];
        double lng10 = r1[2];
        double lng11 = r1[3];

        if (lat00 < lat11 && lat10 < lat01 &&
            lng00 < lng11 && lng10 < lng01) {
            double[] result = new double[] { Math.max(lat00, lat10),
                                             Math.min(lat01, lat11),
                                             Math.max(lng00, lng10),
                                             Math.min(lng01, lng11) };
            return result;
        } else {
            return null;
        }
    }

    // r:       [ min of lat, max of lat, min of lng, max of lng ] deg
    // balance: [ center of lat, center of lng ] deg
    private double[] calcBalance(double[] r) {
        double[] balance = new double[] { (r[0] + r[1]) * 0.5,
                                          (r[2] + r[3]) * 0.5 };
        return balance;
    }

    // r:        [ min of lat, max of lat, min of lng, max of lng ] deg
    // accuracy: [ accuracy of lat axis, accuracy of lng axis ] m
    private double[] calcAccuracy(double[] r) {
        double[] balance = calcBalance(r); // deg
        
        double lat0rad = Earth.degreeToRadian(balance[0]); // rad
        double lng0rad = Earth.degreeToRadian(balance[1]); // rad

        double dlatrad = Earth.degreeToRadian(r[1] - r[0]); // rad
        double dlngrad = Earth.degreeToRadian(r[3] - r[2]); // rad

        // m
        double alat0 = dlatrad * Earth.PrimeVerticalRadiusOfCurvature(lat0rad);
        double alng0 = dlngrad * Earth.MeridianRadiusOfCurvature(lat0rad);

        double[] accuracy = new double[] { alat0, alng0 };
        return accuracy;
    }
}
