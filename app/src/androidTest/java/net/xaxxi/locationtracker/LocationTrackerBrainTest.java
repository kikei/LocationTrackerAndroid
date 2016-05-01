package net.xaxxi.locationtracker;

import java.util.Calendar;
import java.util.TreeMap;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.RenamingDelegatingContext;

import org.junit.Test;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.experimental.runners.Enclosed;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import org.hamcrest.Matcher;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

@RunWith(Enclosed.class)
public final class LocationTrackerBrainTest {

    static final String PREFIX_FOR_TEST = "test_";

    @RunWith(Theories.class)
    public static class GetWeekStartOfTest {
                
        @DataPoints
        public static Fixture[] getFixture() {
            LocationTrackerBrain brain = newLocationTrackerBrain();
            return new Fixture[] {
                new Fixture(that(brain, 2016, 2, 1, 22, 33, 44),
                            equalTo(1454133600000L)),
                new Fixture(that(brain, 2016, 2, 2, 23, 59, 59),
                            equalTo(1454133600000L)),
                new Fixture(that(brain, 2016, 3, 6, 0, 0, 0),
                            equalTo(1457157600000L))
            };
        };

        @Theory
        public void getWeekStartOfTest(Fixture fixture) {
            assertThat(fixture.getThat(), fixture.getExpected());
        }

        private static long that(LocationTrackerBrain brain,
                                 int year, int month, int date,
                                 int hour, int minute, int second) {
            Calendar calendar =
                getCalendarOf(year, month, date, hour, minute, second);
            return Utilities.getTimeInMillis(brain.getWeekStartOf(calendar));
        }
    }

    @RunWith(Theories.class)
    public static class getLearningSegmentNumberTest {
        
        @DataPoints
        public static Fixture[] getFixture() {
            LocationTrackerBrain brain = newLocationTrackerBrain();
            return new Fixture[] {
                new Fixture(that(brain, 2016, 3, 6, 0, 0, 0), is(0)),
                new Fixture(that(brain, 2016, 3, 6, 0, 9, 59), is(0)),
                new Fixture(that(brain, 2016, 3, 6, 0, 10, 0), is(1)),
                new Fixture(that(brain, 2016, 3, 7, 0, 0, 0),
                            is(6 * 24 * 1)),
                new Fixture(that(brain, 2016, 3, 12, 0, 0, 0),
                            is(6 * 24 * 6)),
                new Fixture(that(brain, 2016, 3, 12, 23, 59, 59),
                            is(6 * 24 * 7 - 1))
            };
        }

        @Theory
        public void getWeekStartOfTest(Fixture fixture) {
            assertThat(fixture.getThat(), fixture.getExpected());
        }

        private static int that(LocationTrackerBrain brain,
                                int year, int month, int date,
                                int hour, int minute, int second) {
            Calendar calendar =
                getCalendarOf(year, month, date, hour, minute, second);
            return brain.getLearningSegmentNumber(calendar);
        }
    }

    public static class UpdateStateTest {

        SQLiteDatabase mDatabase;
        LocationTrackerBrain mBrain;
        
        @Before
        public void before() {
            Context context = getTestContext();
            MainDbHelper dbHelper = new MainDbHelper(context);
            mDatabase = getWritableDatabase();
            dbHelper.onUpgrade(mDatabase, 0, 1); // Force database refresh
            
            mBrain = new LocationTrackerBrain(mDatabase);
        }

        @Test
        public void isDatabaseInitialized() {
            double[] scores =
                MainDbHelper.ScoresDatabase.getAllScores(mDatabase);
            for (int i = 0; i < LocationTrackerBrain.SEGMENTS_NUMBER; i++)
                assertThat(scores[i],
                           equalTo(LocationTrackerBrain.PRESET_SCORE));
        }
        
        @Test
        public void updateByScoreZero1() {
            testScoreUpdate(0, LocationTrackerBrain.SEGMENTS_NUMBER, 0);
            testScoreUpdate(LocationTrackerBrain.SEGMENTS_NUMBER,
                            LocationTrackerBrain.SEGMENTS_NUMBER + 1, 0);
            testScoreUpdate(LocationTrackerBrain.SEGMENTS_NUMBER + 1,
                            0, 0);
        }

        private void testScoreUpdate(int segment0, int segment1, int score) {
            Calendar calendar = Calendar.getInstance();
            calendar = mBrain.getWeekStartOf(calendar);
            long t0 = Utilities.getTimeInMillis(calendar);
            long time =
                t0 + segment1 * LocationTrackerBrain.LEARNING_SEGMENT_DURATION;
            
            TreeMap<Long, Location> locations = new TreeMap<Long, Location>();
            for (int i = 0;
                 i < score * LocationTrackerBrain.LEARNING_SEGMENT_DURATION_MINUTE; i++)
                locations.put(time + 1, null);

            double score0 =
                MainDbHelper.ScoresDatabase.getScoreOfSegment(mDatabase,
                                                              segment0);
            mBrain.updateState(mDatabase, time, locations);
            double score1 =
                MainDbHelper.ScoresDatabase.getScoreOfSegment(mDatabase,
                                                              segment0);
            assertThat(score1, closeTo(mBrain.calcScore(score0, score), 0.01));
        }
    }
    
    private static Calendar getCalendarOf(int year, int month, int date,
                                          int hour, int minute, int second) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, date, hour, minute, second);
        return calendar;
    }

    private static Context getTestContext() {
        Context context = InstrumentationRegistry.getTargetContext();
        context = new RenamingDelegatingContext(context, PREFIX_FOR_TEST);
        return context;
    }

    private static SQLiteDatabase getWritableDatabase() {
        Context context = getTestContext();
        MainDbHelper dbHelper = new MainDbHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db;
    }

    private static LocationTrackerBrain newLocationTrackerBrain() {
        SQLiteDatabase db= getWritableDatabase();
        return new LocationTrackerBrain(db);
    }

    /**
     * The Utility class for TheoryTest
     * @typeparam T The type of values tested
     */
    private static class Fixture<T> {
        T mValue;
        Matcher<T> mExpected;

        public Fixture(T value, Matcher<T> expected) {
            mValue = value;
            mExpected = expected;
        }
        public T getThat() {
            return mValue;
        }
        public Matcher<T> getExpected() {
            return mExpected;
        }
    }

}
