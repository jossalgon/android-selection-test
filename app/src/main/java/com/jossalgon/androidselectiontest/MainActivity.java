package com.jossalgon.androidselectiontest;

import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.RadioGroup;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.jossalgon.androidselectiontest.envAnalysis.BluetoothAnalysis;
import com.jossalgon.androidselectiontest.envAnalysis.WifiAnalysis;
import com.jossalgon.androidselectiontest.sensorAnalysis.AccelerationAnalysis;
import com.jossalgon.androidselectiontest.sensorAnalysis.StepsAnalysis;

import java.lang.ref.WeakReference;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private static final double GRAVITY = 9.8;
    public static final int RC_SIGN_IN = 2;

    SensorManager mSensorManager;
    RadioGroup mFrequencySelectorRG;
    RadioGroup mThresholdSelectorRG;
    Button mRunAccelerationButton, mRunStepsCounterButton, mRunBluetoothButton, mRunWifiButton;

    BluetoothAnalysis mBluetoothAnalysis;
    AccelerationAnalysis mAccelerationAnalysis;
    StepsAnalysis mStepsAnalysis;
    WifiAnalysis mWifiAnalysis;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mFirebaseAuth = FirebaseAuth.getInstance();

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        mFrequencySelectorRG = (RadioGroup) findViewById(R.id.frequencySelector);
        mThresholdSelectorRG = (RadioGroup) findViewById(R.id.thresholdSelector);
        mRunAccelerationButton = (Button) findViewById(R.id.runAccelerationButton);
        mRunStepsCounterButton = (Button) findViewById(R.id.runStepsCounterButton);
        mRunBluetoothButton = (Button) findViewById(R.id.runBluetoothButton);
        mRunWifiButton = (Button) findViewById(R.id.runWifiButton);

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    setAnalysisSystems(user.getUid());
                } else {
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                                    .setAvailableProviders(Arrays.asList(
                                            new AuthUI.IdpConfig.GoogleBuilder().build(),
                                            new AuthUI.IdpConfig.EmailBuilder().build()))
                                    .build(),
                            RC_SIGN_IN);
                }
            }
        };
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                setAnalysisSystems(mFirebaseAuth.getUid());
            }
            else if (resultCode == RESULT_CANCELED) {
                finish();
            }
        }
    }

    private void setAnalysisSystems(String userUID) {
        if (mAccelerationAnalysis == null) {
            mAccelerationAnalysis = new AccelerationAnalysis(new WeakReference<>(getApplicationContext()),
                    mSensorManager, mRunAccelerationButton, userUID);
            mRunAccelerationButton.setOnClickListener(mAccelerationAnalysis.getmOnClickListener());
        }

        if (mStepsAnalysis == null) {
            mStepsAnalysis = new StepsAnalysis(new WeakReference<>(getApplicationContext()),
                    mSensorManager, mRunStepsCounterButton, userUID);
            mRunStepsCounterButton.setOnClickListener(mStepsAnalysis.getmOnClickListener());
        }

        if (mBluetoothAnalysis == null) {
            mBluetoothAnalysis = new BluetoothAnalysis(new WeakReference<>(getApplicationContext()),
                    this, mRunBluetoothButton, userUID);
            mRunBluetoothButton.setOnClickListener(mBluetoothAnalysis.getOnClickListener());
        }

        if (mWifiAnalysis == null) {
            mWifiAnalysis = new WifiAnalysis(new WeakReference<Context>(getApplicationContext()),
                    mRunWifiButton, userUID);
            mRunWifiButton.setOnClickListener(mWifiAnalysis.getOnClickListener());
        }

        mFrequencySelectorRG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                switch(checkedId) {
                    case R.id.radio_frequency_normal:
                        mAccelerationAnalysis.setSensorDelay(SensorManager.SENSOR_DELAY_NORMAL);
                        mStepsAnalysis.setSensorDelay(SensorManager.SENSOR_DELAY_NORMAL);
                        mWifiAnalysis.setSensorDelay(SensorManager.SENSOR_DELAY_NORMAL);
                        mBluetoothAnalysis.setSensorDelay(SensorManager.SENSOR_DELAY_NORMAL);
                        break;
                    case R.id.radio_frequency_ui:
                        mAccelerationAnalysis.setSensorDelay(SensorManager.SENSOR_DELAY_UI);
                        mStepsAnalysis.setSensorDelay(SensorManager.SENSOR_DELAY_UI);
                        mWifiAnalysis.setSensorDelay(SensorManager.SENSOR_DELAY_UI);
                        mBluetoothAnalysis.setSensorDelay(SensorManager.SENSOR_DELAY_UI);
                        break;
                    case R.id.radio_frequency_game:
                        mAccelerationAnalysis.setSensorDelay(SensorManager.SENSOR_DELAY_GAME);
                        mStepsAnalysis.setSensorDelay(SensorManager.SENSOR_DELAY_GAME);
                        mWifiAnalysis.setSensorDelay(SensorManager.SENSOR_DELAY_GAME);
                        mBluetoothAnalysis.setSensorDelay(SensorManager.SENSOR_DELAY_GAME);
                        break;
                    case R.id.radio_frequency_faster:
                        mAccelerationAnalysis.setSensorDelay(SensorManager.SENSOR_DELAY_FASTEST);
                        mStepsAnalysis.setSensorDelay(SensorManager.SENSOR_DELAY_FASTEST);
                        mWifiAnalysis.setSensorDelay(SensorManager.SENSOR_DELAY_FASTEST);
                        mBluetoothAnalysis.setSensorDelay(SensorManager.SENSOR_DELAY_FASTEST);
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
    }

    private void unregisterAnalysisSystems() {
        mBluetoothAnalysis.unregisterListener();
        mAccelerationAnalysis.unregisterListener();
        mWifiAnalysis.unregisterListener();
        mStepsAnalysis.unregisterListener();
        mFrequencySelectorRG.clearCheck();
        mThresholdSelectorRG.clearCheck();
        mBluetoothAnalysis.setSensorDelay(-1);
        mAccelerationAnalysis.setSensorDelay(-1);
        mAccelerationAnalysis.setThreshold(0d);
        mWifiAnalysis.setSensorDelay(-1);
        mStepsAnalysis.setSensorDelay(-1);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sign_out_menu:
                AuthUI.getInstance().signOut(this);
                unregisterAnalysisSystems();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }
    @Override
    protected void onPause() {
        super.onPause();
        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mBluetoothAnalysis.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}
