package net.xaxxi.locationtracker;

import android.support.test.runner.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.closeTo;

import net.xaxxi.locationtracker.Earth;

@RunWith(AndroidJUnit4.class)
public class EarthTest {
    @Test
    public void degreeToRadianTest(){
        assertThat(Earth.degreeToRadian(0.0), closeTo(0.0, 0.0001));
        assertThat(Earth.degreeToRadian(360.0), closeTo(Math.PI*2, 0.0001));
    }
}
