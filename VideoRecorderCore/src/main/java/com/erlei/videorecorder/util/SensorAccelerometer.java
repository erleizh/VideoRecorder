package com.erlei.videorecorder.util;

import android.content.Context;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Created by lll on 2018/1/20.
 * 使用重力传感器监听屏幕方向
 */
public class SensorAccelerometer implements SensorEventListener {

    private final OrientationChangeListener mListener;
    private SensorManager mSensorManager;

    public SensorAccelerometer(Context context, OrientationChangeListener listener) {
        mListener = listener;
        try {
            mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            if (mSensorManager != null) {
                android.hardware.Sensor sensor = mSensorManager.getDefaultSensor(SensorManager.SENSOR_ACCELEROMETER);
                mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (mListener == null || event.sensor == null) return;
        if (event.sensor.getType() == android.hardware.Sensor.TYPE_ACCELEROMETER) {
            int x = (int) event.values[0];
            int y = (int) event.values[1];
            if (Math.abs(x) > 6) {// 倾斜度超过60度 10*1.732/2
                if (x <= -3)
                    mListener.onChange(0);
                else
                    mListener.onChange(1);
            } else {
                if (y <= -3)
                    mListener.onChange(2);
                else
                    mListener.onChange(3);
            }

        }
    }

    @Override
    public void onAccuracyChanged(android.hardware.Sensor sensor, int accuracy) {

    }

    public void release() {
        if (mSensorManager != null) mSensorManager.unregisterListener(this);

    }

    public interface OrientationChangeListener {

        void onChange(int orientation);
    }
}
