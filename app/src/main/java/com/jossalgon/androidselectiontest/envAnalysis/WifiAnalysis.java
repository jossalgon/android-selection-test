package com.jossalgon.androidselectiontest.envAnalysis;

import android.content.Context;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.jossalgon.androidselectiontest.R;
import com.thanosfisherman.wifiutils.WifiUtils;
import com.thanosfisherman.wifiutils.wifiScan.ScanResultsListener;
import com.thanosfisherman.wifiutils.wifiState.WifiStateListener;

import java.lang.ref.WeakReference;
import java.util.List;


public class WifiAnalysis {
    private WeakReference<Context> mContextRef;
    private Button mButton;

    private View.OnClickListener mOnClickListener;
    private int mSensorDelay = -1;
    private boolean mRunning = false;


    public WifiAnalysis(WeakReference<Context> contextRef, Button button) {
        this.mContextRef = contextRef;
        this.mButton = button;

        this.mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSensorDelay == -1) {
                    Toast.makeText(mContextRef.get(), "Please, first configure the frequency",
                            Toast.LENGTH_SHORT).show();
                } else if (isRunning()) {
                    unregisterListener();
                    mButton.setText(mContextRef.get().getResources().getString(R.string.startWifi));
                } else {
                    enableWifiAndScan();
                    mButton.setText(mContextRef.get().getResources().getString(R.string.stopWifi));
                }
            }
        };
    }

    public View.OnClickListener getOnClickListener() {
        return mOnClickListener;
    }

    private boolean isRunning() {
        return mRunning;
    }

    private void setRunning(boolean running) {
        this.mRunning = running;
    }

    private void enableWifiAndScan() {
        WifiUtils.withContext(mContextRef.get()).enableWifi(new WifiStateListener() {
            @Override
            public void isSuccess(boolean isSuccess) {
                if (isSuccess) {
                    setRunning(true);
                    scanWifi();
                } else {
                    Toast.makeText(mContextRef.get(), "COULDN'T ENABLE WIFI", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    private void scanWifiWithDelay() {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                scanWifi();
            }
        }, getDelay());
    }

    private void scanWifi() {
        WifiUtils.withContext(mContextRef.get()).scanWifi(new ScanResultsListener() {
            @Override
            public void onScanResults(@NonNull List<ScanResult> scanResults) {
                boolean shouldContinue = isRunning();
                if (shouldContinue && !scanResults.isEmpty()) {
                    for (ScanResult scanResult : scanResults) {
                        WifiDiscoveredDevice wifiDiscoveredDevice = new WifiDiscoveredDevice(
                                scanResult.BSSID, scanResult.level);
                        wifiDiscoveredDevice.saveToFirebase();
                    }
                    scanWifiWithDelay();
                }
            }
        }).start();
    }

    private int getDelay() {
        int delay = -1;
        switch (mSensorDelay) {
            case SensorManager.SENSOR_DELAY_FASTEST:
                delay = 0;
                break;
            case SensorManager.SENSOR_DELAY_GAME:
                delay = 20;
                break;
            case SensorManager.SENSOR_DELAY_UI:
                delay = 66;
                break;
            case SensorManager.SENSOR_DELAY_NORMAL:
                delay = 200;
                break;
            default:
                delay = mSensorDelay;
                break;
        }
        return delay;
    }

    public void setSensorDelay(int sensorDelay) {
        this.mSensorDelay = sensorDelay;
    }

    private static class WifiDiscoveredDevice {
        public String bssid;
        public int level;
        public String timestamp;

        public WifiDiscoveredDevice(String bssid, int level) {
            Long tsLong = System.currentTimeMillis()/1000;
            this.timestamp = tsLong.toString();
            this.bssid = bssid;
            this.level = level;
        }

        public void saveToFirebase() {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference dbRef = database.getReference("wifiDevices");
            dbRef.push().setValue(this);
        }
    }


    public void unregisterListener() {
        if (isRunning()) {
            setRunning(false);
        }
    }


}
