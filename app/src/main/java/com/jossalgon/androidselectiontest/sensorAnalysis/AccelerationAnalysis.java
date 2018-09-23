package com.jossalgon.androidselectiontest.sensorAnalysis;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.jossalgon.androidselectiontest.R;

import java.lang.ref.WeakReference;

public class AccelerationAnalysis {
    private WeakReference<Context> mContextRef;
    private SensorManager mSensorManager;
    private Button mButton;
    private String mUserUID;

    private Sensor mSensor;
    private SensorEventListener mSensorListener;
    private boolean running = false;
    private double mThreshold = 0d;
    private int mSensorDelay = -1;
    private View.OnClickListener mOnClickListener;

    public AccelerationAnalysis(WeakReference<Context> contextRef, SensorManager sensorManager,
                                Button button, String userUID) {
        this.mContextRef = contextRef;
        this.mSensorManager = sensorManager;
        this.mButton = button;
        this.mUserUID = userUID;

        this.mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        this.mSensorListener = new SensorEventListener() {
            @Override
            public void onAccuracyChanged(Sensor arg0, int arg1) {
            }

            @Override
            public void onSensorChanged(SensorEvent event) {
                double x = event.values[0];
                double y = event.values[1];
                double z = event.values[2];
                double mod = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
                boolean thresholdExceeded = (mThreshold > 0 && mod >= mThreshold);
                Acceleration acceleration = new Acceleration(x, y, z, mod, thresholdExceeded, mUserUID);
                acceleration.saveToFirebase();
            }
        };

        this.mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSensorDelay == -1 || mThreshold == 0d) {
                    Toast.makeText(mContextRef.get(), "Please, first configure the params",
                            Toast.LENGTH_SHORT).show();
                } else if (isRunning()) {
                    unregisterListener();
                    mButton.setText(mContextRef.get().getResources().getString(R.string.startAcceleration));
                } else {
                    registerListener();
                    mButton.setText(mContextRef.get().getResources().getString(R.string.stopAcceleration));
                }
            }
        };
    }

    public void reloadListener() {
        if(isRunning()) {
            mSensorManager.unregisterListener(mSensorListener, mSensor);
            mSensorManager.registerListener(mSensorListener, mSensor, mSensorDelay);
        }
    }

    private void registerListener() {
        setRunning(true);
        mSensorManager.registerListener(mSensorListener, mSensor, mSensorDelay);
    }

    public void unregisterListener() {
        if (isRunning()) {
            setRunning(false);
            mSensorManager.unregisterListener(mSensorListener, mSensor);
            mButton.setText(mContextRef.get().getResources().getString(R.string.startAcceleration));
        }
    }

    public View.OnClickListener getmOnClickListener() {
        return mOnClickListener;
    }

    private boolean isRunning() {
        return running;
    }

    private void setRunning(boolean running) {
        this.running = running;
    }

    public void setThreshold(double threshold) {
        this.mThreshold = threshold;
    }

    public int getSensorDelay() {
        return mSensorDelay;
    }

    public void setSensorDelay(int sensorDelay) {
        this.mSensorDelay = sensorDelay;
    }

    public static class Acceleration {
        public Double acc_x1, acc_y1, acc_z1, acc_modulo1;
        public Boolean s_n1;
        public String timestamp;
        public String userUID;


        public Acceleration(double acc_x1, double acc_y1, double acc_z1, double acc_modulo1,
                            boolean s_n1, String userUID) {
            Long tsLong = System.currentTimeMillis()/1000;
            this.timestamp = tsLong.toString();
            this.acc_x1 = acc_x1;
            this.acc_y1 = acc_y1;
            this.acc_z1 = acc_z1;
            this.acc_modulo1 = acc_modulo1;
            this.s_n1 = s_n1;
            this.userUID = userUID;
        }

        public Acceleration(DataSnapshot ds) {
            this.timestamp = ds.child("timestamp").getValue(String.class);
            this.acc_x1 = ds.child("acc_x1").getValue(Double.class);
            this.acc_y1 = ds.child("acc_y1").getValue(Double.class);
            this.acc_z1 = ds.child("acc_z1").getValue(Double.class);
            this.acc_modulo1 = ds.child("acc_modulo1").getValue(Double.class);
            this.s_n1 = ds.child("s_n1").getValue(Boolean.class);
            this.userUID = ds.child("userUID").getValue(String.class);
        }

        public void saveToFirebase() {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference dbRef = database.getReference("global/accelerations");
            DatabaseReference dbRef2 = database.getReference("users/"+this.userUID+"/accelerations");
            dbRef.push().setValue(this);
            dbRef2.push().setValue(this);
        }
    }

}
