package com.jossalgon.androidselectiontest;

import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.RadioGroup;

import com.jossalgon.androidselectiontest.envAnalysis.BluetoothAnalysis;
import com.jossalgon.androidselectiontest.sensorAnalysis.AccelerationAnalysis;
import com.jossalgon.androidselectiontest.sensorAnalysis.StepsAnalysis;

import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity {
    private static final double GRAVITY = 9.8;

    SensorManager mSensorManager;
    RadioGroup mFrequencySelectorRG;
    RadioGroup mThresholdSelectorRG;
    Button mRunAccelerationButton, mRunStepsCounterButton, mRunBluetoothButton, mRunWifiButton;
    BluetoothAnalysis mBluetoothAnalysis;
    AccelerationAnalysis mAccelerationAnalysis;
    StepsAnalysis mStepsAnalysis;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        mFrequencySelectorRG = (RadioGroup) findViewById(R.id.frequencySelector);
        mThresholdSelectorRG = (RadioGroup) findViewById(R.id.thresholdSelector);
        mRunAccelerationButton = (Button) findViewById(R.id.runAccelerationButton);
        mRunStepsCounterButton = (Button) findViewById(R.id.runStepsCounterButton);
        mRunBluetoothButton = (Button) findViewById(R.id.runBluetoothButton);
        mRunWifiButton = (Button) findViewById(R.id.runWifiButton);

        mAccelerationAnalysis = new AccelerationAnalysis(new WeakReference<>(getApplicationContext()),
                mSensorManager,
                mRunAccelerationButton);
        mRunAccelerationButton.setOnClickListener(mAccelerationAnalysis.getmOnClickListener());

        mStepsAnalysis = new StepsAnalysis(new WeakReference<>(getApplicationContext()),
                mSensorManager,
                mRunStepsCounterButton);
        mRunStepsCounterButton.setOnClickListener(mStepsAnalysis.getmOnClickListener());

        mFrequencySelectorRG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                switch(checkedId) {
                    case R.id.radio_frequency_normal:
                        mAccelerationAnalysis.setSensorDelay(SensorManager.SENSOR_DELAY_NORMAL);
                        mStepsAnalysis.setSensorDelay(SensorManager.SENSOR_DELAY_NORMAL);
                        break;
                    case R.id.radio_frequency_ui:
                        mAccelerationAnalysis.setSensorDelay(SensorManager.SENSOR_DELAY_UI);
                        mStepsAnalysis.setSensorDelay(SensorManager.SENSOR_DELAY_UI);
                        break;
                    case R.id.radio_frequency_game:
                        mAccelerationAnalysis.setSensorDelay(SensorManager.SENSOR_DELAY_GAME);
                        mStepsAnalysis.setSensorDelay(SensorManager.SENSOR_DELAY_GAME);
                        break;
                    case R.id.radio_frequency_faster:
                        mAccelerationAnalysis.setSensorDelay(SensorManager.SENSOR_DELAY_FASTEST);
                        mStepsAnalysis.setSensorDelay(SensorManager.SENSOR_DELAY_FASTEST);
                        break;
                }
                mAccelerationAnalysis.reloadListener();
                mStepsAnalysis.reloadListener();
            }
        });

        mThresholdSelectorRG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                switch(checkedId) {
                    case R.id.radio_2g:
                        mAccelerationAnalysis.setThreshold(2 * GRAVITY);
                        break;
                    case R.id.radio_25g:
                        mAccelerationAnalysis.setThreshold(2.5 * GRAVITY);
                        break;
                    case R.id.radio_3g:
                        mAccelerationAnalysis.setThreshold(3 * GRAVITY);
                        break;
                }
            }
        });

        mBluetoothAnalysis = new BluetoothAnalysis(new WeakReference<>(getApplicationContext()),
                this, mRunBluetoothButton);
        mRunBluetoothButton.setOnClickListener(mBluetoothAnalysis.getOnClickListener());

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mBluetoothAnalysis.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onDestroy() {
        mBluetoothAnalysis.unregisterListener();
        mAccelerationAnalysis.unregisterListener();
        super.onDestroy();
    }

}
