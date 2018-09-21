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
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.lang.ref.WeakReference;


public class BluetoothAnalysis {
    private static final int ACTION_REQUEST_BLUETOOTH_PERMISSIONS = 1;

    private WeakReference<Context> mContextRef;
    private Activity mActivity;
    private BluetoothAdapter mBluetoothAdapter;

    public BluetoothAnalysis(WeakReference<Context> mContextRef, Activity mActivity) {
        this.mContextRef = mContextRef;
        this.mActivity = mActivity;
        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public void startSearching() {
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

    public void unregister() {
        mContextRef.get().unregisterReceiver(mReceiver);
    }


}
