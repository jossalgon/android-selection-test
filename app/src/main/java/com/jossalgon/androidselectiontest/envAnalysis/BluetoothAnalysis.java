package com.jossalgon.androidselectiontest.envAnalysis;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
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

    private static final int ACTION_REQUEST_BLUETOOTH_PERMISSIONS = 1;
    private BluetoothAdapter mBluetoothAdapter;
    private View.OnClickListener mOnClickListener;
    private boolean mRunning = false;


    public BluetoothAnalysis(WeakReference<Context> contextRef, Activity activity, Button button) {
        this.mContextRef = contextRef;
        this.mActivity = activity;
        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.mButton = button;

        this.mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRunning()) {
                    unregisterListener();
                    mButton.setText(mContextRef.get().getResources().getString(R.string.startBluetooth));
                } else {
                    startSearching();
                    mButton.setText(mContextRef.get().getResources().getString(R.string.stopBluetooth));
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

    public void startSearching() {
        setRunning(true);
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
            searchBluetoothDevices();
        }
    }

    private void searchBluetoothDevices() {
        if (mBluetoothAdapter == null) {
            Toast.makeText(mContextRef.get(), "NO BLUETOOTH", Toast.LENGTH_SHORT).show();
        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                mBluetoothAdapter.enable();
                if (mBluetoothAdapter.isDiscovering()) {
                    mBluetoothAdapter.cancelDiscovery();
                }
            }

            IntentFilter filter = new IntentFilter();

            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

            mContextRef.get().registerReceiver(mReceiver, filter);
            mBluetoothAdapter.startDiscovery();
        }
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
        public String timestamp;

        public BluetoothDiscoveredDevice(String mac_address_BT) {
            Long tsLong = System.currentTimeMillis()/1000;
            this.timestamp = tsLong.toString();
            this.mac_address_BT = mac_address_BT;
        }

        public void saveToFirebase() {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference dbRef = database.getReference("bluetoothDevices");
            dbRef.push().setValue(this);
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                BluetoothDiscoveredDevice discoveredDevice = new BluetoothDiscoveredDevice(device.getAddress());
                discoveredDevice.saveToFirebase();
            }
        }
    };

    public void unregisterListener() {
        if (isRunning()) {
            setRunning(false);
            mContextRef.get().unregisterReceiver(mReceiver);
        }
    }


}
