package net.xaxxi.locationtracker;

import java.util.Calendar;
import java.lang.StringBuilder;

public class Utilities {
    public static long getTimeInMillis() {
        return getTimeInMillis(Calendar.getInstance());
    }
    public static long getTimeInMillis(Calendar calendar) {
        return
            calendar.getTimeInMillis() -
            calendar.getTimeZone().getOffset(calendar.getTimeInMillis());
    }
    public static Calendar getCalendar() {
        return getCalendarByTimeInMillis(getTimeInMillis());
    }
    public static Calendar getCalendarByTimeInMillis(long t) {
        Calendar calendar = Calendar.getInstance();
        t += calendar.getTimeZone().getOffset(calendar.getTimeInMillis());
        calendar.setTimeInMillis(t);
        return calendar;
    }

    public static class Debug {
        public static String joinArray(String sep, double[] arr) {
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < arr.length; i++) {
                b.append(String.valueOf(arr[i]));
                if (i != arr.length - 1) b.append(sep);
            }
            return b.toString();
        }
    }
}
