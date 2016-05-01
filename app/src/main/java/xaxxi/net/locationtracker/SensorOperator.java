package net.xaxxi.locationtracker;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.support.v4.app.Fragment;

public class SensorOperator {

    public interface Listener {
        public void onAttitudeUpdate(float azumith, float pitch, float roll);
    }

    SensorManager mSensorManager;
    Listener mListener = null;
    boolean sensorRunning = false;
    
    public SensorOperator(SensorManager manager) {
        mSensorManager = manager;
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void start() {
        if (!sensorRunning) {
            sensorRunning = true;
            
            Sensor sen;
            sen = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mSensorManager.registerListener(mSensorEventListener, sen,
                                            SensorManager.SENSOR_DELAY_NORMAL);
        
            sen = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            mSensorManager.registerListener(mSensorEventListener, sen,
                                            SensorManager.SENSOR_DELAY_NORMAL);
        }
    }


    public void stop() {
        if (sensorRunning) {
            sensorRunning = false;
            
            Sensor sen;
            sen = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mSensorManager.unregisterListener(mSensorEventListener, sen);
        
            sen = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            mSensorManager.unregisterListener(mSensorEventListener, sen);
        }
    }

    float[] mAccl = null;
    float[] mMagn = null;

    SensorEventListener mSensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                switch (event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    mAccl = event.values.clone();
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    mMagn = event.values.clone();
                    break;
                }
                if (mAccl != null && mMagn != null) {
                    // Rotation array(world)
                    float[] r0 = new float[9];
                    // Rotation array(device)
                    float[] r1 = new float[9];
                    // Attitude array
                    float[] att = new float[3];
                    
                    SensorManager.getRotationMatrix(r0, null, mAccl, mMagn);
                    SensorManager.remapCoordinateSystem(r0,
                                                        SensorManager.AXIS_X,
                                                        SensorManager.AXIS_Y,
                                                        r1);
                    SensorManager.getOrientation(r1, att);
                    if (mListener != null)
                        mListener.onAttitudeUpdate(att[0], att[1], att[2]);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };

}
