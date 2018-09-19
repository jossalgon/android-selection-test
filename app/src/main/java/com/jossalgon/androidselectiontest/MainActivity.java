package com.jossalgon.androidselectiontest;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor mSensor;

    private TextView mAccelerationTV;
    RadioGroup mFrequencySelectorRG;
    RadioGroup mThresholdSelectorRG;
    Button mRunButton;

    private static final double GRAVITY = 9.8;
    private Double mThreshold;
    private Integer mSensorDelay;
    private boolean isRunning = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAccelerationTV = (TextView) findViewById(R.id.acceleration);
        mFrequencySelectorRG = (RadioGroup) findViewById(R.id.frequencySelector);
        mThresholdSelectorRG = (RadioGroup) findViewById(R.id.thresholdSelector);
        mRunButton = (Button) findViewById(R.id.runButton);

        mFrequencySelectorRG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                switch(checkedId) {
                    case R.id.radio_frequency_normal:
                        mSensorDelay = SensorManager.SENSOR_DELAY_NORMAL;
                        break;
                    case R.id.radio_frequency_ui:
                        mSensorDelay = SensorManager.SENSOR_DELAY_UI;
                        break;
                    case R.id.radio_frequency_game:
                        mSensorDelay = SensorManager.SENSOR_DELAY_GAME;
                        break;
                    case R.id.radio_frequency_faster:
                        mSensorDelay = SensorManager.SENSOR_DELAY_FASTEST;
                        break;
                }

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

        mRunButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSensorDelay == null || mThreshold == null) {
                    Toast.makeText(MainActivity.this, "First configure the params",
                            Toast.LENGTH_SHORT).show();
                }
                else if (isRunning) {
                    mSensorManager.unregisterListener(MainActivity.this);
                    isRunning = false;
                    mRunButton.setText(getResources().getString(R.string.start));
                } else {
                    mSensorManager.registerListener(MainActivity.this, mSensor, mSensorDelay);
                    isRunning = true;
                    mRunButton.setText(getResources().getString(R.string.stop));
                }
            }
        });

    }

    private static class Acceleration {
        public double acc_x1, acc_y1, acc_z1, acc_modulo1;
        public boolean s_n1;
        public String timestamp;

        public Acceleration(double acc_x1, double acc_y1, double acc_z1, double acc_modulo1,
                            boolean s_n1) {
            Long tsLong = System.currentTimeMillis()/1000;
            this.timestamp = tsLong.toString();
            this.acc_x1 = acc_x1;
            this.acc_y1 = acc_y1;
            this.acc_z1 = acc_z1;
            this.acc_modulo1 = acc_modulo1;
            this.s_n1 = s_n1;
        }

        @Override
        public String toString() {
            return "X: " + acc_x1 +
                    "\nY: " + acc_y1 +
                    "\nZ: " + acc_z1 +
                    "\nMod: " + acc_modulo1 +
                    "\nTimestamp: " + timestamp +
                    "\nExceeded: " + s_n1;
        }
    }

    private void saveToFirebase(Acceleration acceleration) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference accelerationsRef = database.getReference("accelerations");
        accelerationsRef.push().setValue(acceleration);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        double x = event.values[0];
        double y = event.values[1];
        double z = event.values[2];
        double mod = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
        boolean thresholdExceeded = (mThreshold > 0 && mod >= mThreshold);
        Acceleration acceleration = new Acceleration(x, y, z, mod, thresholdExceeded);
        mAccelerationTV.setText(acceleration.toString());
        saveToFirebase(acceleration);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
