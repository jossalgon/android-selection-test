package com.jossalgon.androidselectiontest;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jossalgon.androidselectiontest.envAnalysis.BluetoothAnalysis;
import com.jossalgon.androidselectiontest.envAnalysis.BluetoothAnalysis.BluetoothDiscoveredDevice;
import com.jossalgon.androidselectiontest.envAnalysis.WifiAnalysis;
import com.jossalgon.androidselectiontest.envAnalysis.WifiAnalysis.WifiDiscoveredDevice;
import com.jossalgon.androidselectiontest.sensorAnalysis.AccelerationAnalysis;
import com.jossalgon.androidselectiontest.sensorAnalysis.AccelerationAnalysis.Acceleration;
import com.jossalgon.androidselectiontest.sensorAnalysis.StepsAnalysis;
import com.jossalgon.androidselectiontest.sensorAnalysis.StepsAnalysis.Step;
import com.jossalgon.androidselectiontest.utils.FileExporter;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private static final double GRAVITY = 9.8;
    public static final int RC_SIGN_IN = 2;
    private static final int ACTION_REQUEST_STORAGE_PERMISSION = 3;
    static WeakReference<Context> mContextRef;

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
        mContextRef = new WeakReference<>(getApplicationContext());
        
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
            mAccelerationAnalysis = new AccelerationAnalysis(mContextRef,
                    mSensorManager, mRunAccelerationButton, userUID);
            mRunAccelerationButton.setOnClickListener(mAccelerationAnalysis.getmOnClickListener());
        }

        if (mStepsAnalysis == null) {
            mStepsAnalysis = new StepsAnalysis(mContextRef,
                    mSensorManager, mRunStepsCounterButton, userUID);
            mRunStepsCounterButton.setOnClickListener(mStepsAnalysis.getmOnClickListener());
        }

        if (mBluetoothAnalysis == null) {
            mBluetoothAnalysis = new BluetoothAnalysis(mContextRef,
                    this, mRunBluetoothButton, userUID);
            mRunBluetoothButton.setOnClickListener(mBluetoothAnalysis.getOnClickListener());
        }

        if (mWifiAnalysis == null) {
            mWifiAnalysis = new WifiAnalysis(mContextRef,
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
            case R.id.export_data_menu:
                isStoragePermissionGranted();
                return true;
            case R.id.remove_data_menu:
                removeUserDataFromFirebase();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void removeUserDataFromFirebase() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference dbRef = database.getReference("users/"+mFirebaseAuth.getUid());
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                dataSnapshot.getRef().removeValue();
                Toast.makeText(MainActivity.this, "Data removed", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    public boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                exportDatabase();
                return true;
            } else {
                ActivityCompat.requestPermissions(this, new String[]{
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        ACTION_REQUEST_STORAGE_PERMISSION);
                return false;
            }
        }
        else {
            exportDatabase();
            return true;
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
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                requestCode == ACTION_REQUEST_STORAGE_PERMISSION){
                exportDatabase();
        }
        mBluetoothAnalysis.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void exportDatabase() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference dbRef = database.getReference("users/"+mFirebaseAuth.getUid());
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ArrayList<Acceleration> accelerations = new ArrayList<>();
                ArrayList<Step> steps = new ArrayList<>();
                ArrayList<WifiDiscoveredDevice> wifiDevices = new ArrayList<>();
                ArrayList<BluetoothDiscoveredDevice> bluetoothDevices = new ArrayList<>();
                for (DataSnapshot ds : dataSnapshot.child("accelerations").getChildren()) {
                    Acceleration a = new Acceleration(ds);
                    accelerations.add(a);
                }
                for (DataSnapshot ds : dataSnapshot.child("steps").getChildren()) {
                    Step s = new Step(ds);
                    steps.add(s);
                }
                for (DataSnapshot ds : dataSnapshot.child("wifiDevices").getChildren()) {
                    WifiDiscoveredDevice w = new WifiDiscoveredDevice(ds);
                    wifiDevices.add(w);
                }
                for (DataSnapshot ds : dataSnapshot.child("bluetoothDevices").getChildren()) {
                    BluetoothDiscoveredDevice b = new BluetoothDiscoveredDevice(ds);
                    bluetoothDevices.add(b);
                }
                new FileExporter().execute(new FileExporter.FirebaseData(accelerations, steps, 
                        wifiDevices, bluetoothDevices));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

    }

    public static void onFileExporterFinished(String filePath) {
        Toast.makeText(mContextRef.get(), "File saved at " + filePath, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}
