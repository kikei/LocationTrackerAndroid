package net.xaxxi.locationtracker;

public class MainMessage {

    public static class MessageCode {
        public static final int LOCATION_UPDATED = 100;
        public static final int LOCATION_TEMPORARILY_UPDATED = 101;
        public static final int LOCATION_UPDATE_INTERVAL_CHANGED = 102;
        public static final int LOCATION_UPDATE_LEVEL_CHANGED = 103;
        public static final int LOCATION_TRACKER_STATUS_CHANGED = 104;
        public static final int PROVIDER_ENABLED = 200;
        public static final int PROVIDER_DISABLED = 201;
        public static final int LOCATION_OUT_OF_SERVICE = 300;
        public static final int LOCATION_TEMPORARILY_UNAVAILABLE = 301;
        public static final int LOCATION_AVAILABLE = 302;
        public static final int GPS_FIRST_FIX = 400;
        public static final int GPS_STARTED = 401;
        public static final int GPS_STOPPED = 402;
        public static final int GPS_SATELLITE_STATUS = 403;
        public static final int ATTITUDE_UPDATE = 500;
    }
    
}
