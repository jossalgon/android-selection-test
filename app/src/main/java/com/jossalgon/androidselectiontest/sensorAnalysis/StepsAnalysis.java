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

public class StepsAnalysis {
    private WeakReference<Context> mContextRef;
    private SensorManager mSensorManager;
    private Button mButton;
    private String mUserUID;

    private int mStepsOffset = 0;
    private Sensor mSensor;
    private SensorEventListener mSensorListener;
    private boolean running = false;
    private int mSensorDelay = -1;
    private View.OnClickListener mOnClickListener;

    public StepsAnalysis(WeakReference<Context> contextRef, SensorManager sensorManager,
                         Button button, final String userUID) {
        this.mContextRef = contextRef;
        this.mSensorManager = sensorManager;
        this.mButton = button;
        this.mUserUID = userUID;

        this.mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        this.mSensorListener = new SensorEventListener() {
            @Override
            public void onAccuracyChanged(Sensor arg0, int arg1) {
            }

            @Override
            public void onSensorChanged(SensorEvent event) {
                int counter = (int) event.values[0];
                if (mStepsOffset == 0) {
                    mStepsOffset = counter;
                }
                Step step = new Step(counter - mStepsOffset, mUserUID);
                step.saveToFirebase();
            }
        };

        this.mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSensorDelay == -1) {
                    Toast.makeText(mContextRef.get(), "Please, first configure the frequency",
                            Toast.LENGTH_SHORT).show();
                } else if (isRunning()) {
                    unregisterListener();
                    mButton.setText(mContextRef.get().getResources().getString(R.string.startStepsCounter));
                } else {
                    registerListener();
                    mButton.setText(mContextRef.get().getResources().getString(R.string.stopStepsCounter));
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
            mButton.setText(mContextRef.get().getResources().getString(R.string.startStepsCounter));
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

    public int getSensorDelay() {
        return mSensorDelay;
    }

    public void setSensorDelay(int sensorDelay) {
        this.mSensorDelay = sensorDelay;
    }

    public static class Step {
        public Integer counter;
        public String timestamp;
        public String userUID;

        public Step(int counter, String userUID) {
            Long tsLong = System.currentTimeMillis()/1000;
            this.timestamp = tsLong.toString();
            this.counter = counter;
            this.userUID = userUID;
        }

        public Step(DataSnapshot ds) {
            this.timestamp = ds.child("timestamp").getValue(String.class);
            this.counter = ds.child("counter").getValue(Integer.class);
            this.userUID = ds.child("userUID").getValue(String.class);
        }

        public void saveToFirebase() {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference dbRef = database.getReference("global/steps");
            DatabaseReference dbRef2 = database.getReference("users/"+this.userUID+"/steps");
            dbRef.push().setValue(this);
            dbRef2.push().setValue(this);
        }
    }

}
