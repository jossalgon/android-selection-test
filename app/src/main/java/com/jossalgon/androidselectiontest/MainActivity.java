package com.jossalgon.androidselectiontest;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v7.app.AppCompatActivity;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor mSensor;

    private TextView mAccelerationTV;
    RadioGroup mFrequencySelectorRG;
    RadioGroup mThresholdSelectorRG;

    private static final double GRAVITY = 9.8;
    private double mThreshold = 0d;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAccelerationTV = (TextView) findViewById(R.id.acceleration);
        mFrequencySelectorRG = (RadioGroup) findViewById(R.id.frequencySelector);
        mThresholdSelectorRG = (RadioGroup) findViewById(R.id.thresholdSelector);

        mFrequencySelectorRG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                int sensorDelay = SensorManager.SENSOR_DELAY_NORMAL;

                switch(checkedId) {
                    case R.id.radio_frequency_normal:
                        sensorDelay = SensorManager.SENSOR_DELAY_NORMAL;
                        break;
                    case R.id.radio_frequency_ui:
                        sensorDelay = SensorManager.SENSOR_DELAY_UI;
                        break;
                    case R.id.radio_frequency_game:
                        sensorDelay = SensorManager.SENSOR_DELAY_GAME;
                        break;
                    case R.id.radio_frequency_faster:
                        sensorDelay = SensorManager.SENSOR_DELAY_FASTEST;
                        break;
                }
                mSensorManager.unregisterListener(MainActivity.this);
                mSensorManager.registerListener(MainActivity.this, mSensor, sensorDelay);
            }
        });

        mThresholdSelectorRG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                switch(checkedId) {
                    case R.id.radio_2g:
                        mThreshold = 2 * GRAVITY;
                        break;
                    case R.id.radio_25g:
                        mThreshold = 2.5 * GRAVITY;
                        break;
                    case R.id.radio_3g:
                        mThreshold = 3 * GRAVITY;
                        break;
                }
            }
        });

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        double x = event.values[0];
        double y = event.values[1];
        double z = event.values[2];
        double mod = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
        mAccelerationTV.setText("X: " + x +
                "\nY: " + y +
                "\nZ: " + z +
                "\nMod: " + mod);
        if (mThreshold > 0 && mod >= mThreshold) {
            Toast.makeText(this, "THRESHOLD EXCEEDED", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
