package com.jossalgon.androidselectiontest.envAnalysis;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.jossalgon.androidselectiontest.R;

import java.lang.ref.WeakReference;


public class BluetoothAnalysis {
    private WeakReference<Context> mContextRef;
    private Activity mActivity;
    private Button mButton;
    private String mUserUID;

    private static final int ACTION_REQUEST_BLUETOOTH_PERMISSIONS = 1;
    private BluetoothAdapter mBluetoothAdapter;
    private View.OnClickListener mOnClickListener;
    private int mSensorDelay = -1;
    private boolean mRunning = false;


    public BluetoothAnalysis(WeakReference<Context> contextRef, Activity activity, Button button,
                             String userUID) {
        this.mContextRef = contextRef;
        this.mActivity = activity;
        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.mButton = button;
        this.mUserUID = userUID;

        this.mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBluetoothAdapter == null) {
                    Toast.makeText(mContextRef.get(), "NO BLUETOOTH", Toast.LENGTH_SHORT).show();
                } else if (mSensorDelay == -1) {
                    Toast.makeText(mContextRef.get(), "Please, first configure the frequency",
                            Toast.LENGTH_SHORT).show();
                } else if (isRunning()) {
                    unregisterListener();
                    mButton.setText(mContextRef.get().getResources().getString(R.string.startBluetooth));
                } else {
                    startSearching();
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

    private void startSearching() {
        int pCheck = ContextCompat.checkSelfPermission(mContextRef.get(), android.Manifest.permission.ACCESS_FINE_LOCATION);
        pCheck += ContextCompat.checkSelfPermission(mContextRef.get(), android.Manifest.permission.ACCESS_COARSE_LOCATION);
        pCheck += ContextCompat.checkSelfPermission(mContextRef.get(), android.Manifest.permission.BLUETOOTH_ADMIN);
        pCheck += ContextCompat.checkSelfPermission(mContextRef.get(), android.Manifest.permission.BLUETOOTH);
        if (pCheck != 0) {
            ActivityCompat.requestPermissions(mActivity, new String[]{
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.BLUETOOTH_ADMIN,
                    android.Manifest.permission.BLUETOOTH}, ACTION_REQUEST_BLUETOOTH_PERMISSIONS);
        } else {
            registerReceiver();
            searchBluetoothDevices();
            setRunning(true);
            mButton.setText(mContextRef.get().getResources().getString(R.string.stopBluetooth));
        }
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();

        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

        mContextRef.get().registerReceiver(mReceiver, filter);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            boolean shouldContinue = isRunning();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI,Short.MIN_VALUE);
                BluetoothDiscoveredDevice discoveredDevice = new BluetoothDiscoveredDevice(
                        device.getAddress(), rssi, mUserUID);
                discoveredDevice.saveToFirebase();
            }
            else if ((BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) && shouldContinue) {
                startDiscoveryWithDelay();
            }
            else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action) &&
                    shouldContinue) {
                checkBluetooth();
                startSearching();
            }
        }
    };

    private void searchBluetoothDevices() {
        checkBluetooth();
        mBluetoothAdapter.startDiscovery();
    }

    private void checkBluetooth() {
        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
            if (mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();
            }
        }
    }

    private void startDiscoveryWithDelay() {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                searchBluetoothDevices();
            }
        }, getDelay());
    }

    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case ACTION_REQUEST_BLUETOOTH_PERMISSIONS:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    searchBluetoothDevices();
                } else {
                    Toast.makeText(mContextRef.get(), "Please, accept permissions to search devices",
                            Toast.LENGTH_SHORT).show();
                    startSearching();
                }
        }
    }

    private static class BluetoothDiscoveredDevice {
        public String mac_address_BT;
        public int rssi;
        public String timestamp;
        public String userUID;

        public BluetoothDiscoveredDevice(String mac_address_BT, int rssi, String userUID) {
            Long tsLong = System.currentTimeMillis()/1000;
            this.timestamp = tsLong.toString();
            this.mac_address_BT = mac_address_BT;
            this.rssi = rssi;
            this.userUID = userUID;
        }

        public void saveToFirebase() {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference dbRef = database.getReference("global/bluetoothDevices");
            DatabaseReference dbRef2 = database.getReference("users/"+this.userUID+"/bluetoothDevices");
            dbRef.push().setValue(this);
            dbRef2.push().setValue(this);
        }
    }

    public void unregisterListener() {
        if (isRunning()) {
            setRunning(false);
            mContextRef.get().unregisterReceiver(mReceiver);
            mButton.setText(mContextRef.get().getResources().getString(R.string.startBluetooth));
        }
    }

    public void setSensorDelay(int sensorDelay) {
        this.mSensorDelay = sensorDelay;
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


}
