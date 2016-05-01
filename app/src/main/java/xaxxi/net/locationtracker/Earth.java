package net.xaxxi.locationtracker;

public class Earth {
    public static final double SemiMajorAxis = 6738137;
    public static final double Flattening = 1.0/298.257222101;
    public static final double SquaredEccentricity = 0.006694380;

    static final double a = SemiMajorAxis;
    static final double c = a * (1.0 - SquaredEccentricity);

    public static double degreeToRadian(double deg) {
        return deg / 180.0 * Math.PI;
    }

    public static double radianToDegree(double rad) {
        return rad * 180.0 / Math.PI;
    }


    // φ is latitude [radian]
    // radian -> m/radian
    public static double MeridianRadiusOfCurvature(double phi) {
        final double w = 
            Math.sqrt(1.0 - SquaredEccentricity * Math.sin(phi));
        return a * (1.0 - SquaredEccentricity) / Math.pow(w, 3.0);
    }

    // φ is latitude [radian]
    // radian -> m/radian
    public static double PrimeVerticalRadiusOfCurvature(double phi) {
        final double w = 
            Math.sqrt(1.0 - SquaredEccentricity * Math.sin(phi));
        return a / w;
    }

    // φ is latitude [radian]
    /*
    public static double distancePerLongitude(double phi) {
        return 
            Math.PI * MeridianRadiusOfCurvature(phi);
    }

    // φ is latitude [radian]
    public static double distancePerLatitude(double phi) {
        return
            Math.PI * PrimeVerticalRadiusOfCurvature(phi);
    }
    */
    
    /*
    public double distancePerLongitudeSecond(double phi) {
        return
            Math.PI / (180 * 3600) * MeridianRadiusOfCurvature(phi);
    }

    // φ is latitude [radian]
    public double distancePerLatitudeSecond(double phi) {
        return
            Math.PI / (180 * 3600) * PrimeVerticalRadiusOfCurvature(phi);
    }
    */

    /**
     * Returns DMS of degree.
     *
     * @param deg degree value
     * @return DMS(AMI VOR/DME) of degree value
     */
    public static String degreeToDMS(double deg) {
        deg = Math.abs(deg);
        int dd = (int)Math.floor(deg);
        int mm = (int)Math.floor((deg - dd) * 60.0);
        double ss = ((deg - dd) - (mm / 60.0)) * 3600.0;
        return String.format("%02d°%02d' %02.2f''", dd, mm, ss);
    }

    /**
     * Returns DMS of a latitude and longitude.
     *
     * @param lat a latitude in degree
     * @param lng a longitude in degree
     * @return DMS(AMI VOR/DME) of a latitude and longitude
     */
    public static String degreeToDMS(double lat, double lng) {
        return
            degreeToDMS(lat) + (lat > 0.0 ? "N" : "S") + " " +
            degreeToDMS(lng) + (lng > 0.0 ? "E" : "W");
    }
}
