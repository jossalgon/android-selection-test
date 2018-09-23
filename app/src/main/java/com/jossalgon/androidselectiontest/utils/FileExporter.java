package com.jossalgon.androidselectiontest.utils;


import android.os.AsyncTask;
import android.os.Environment;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jossalgon.androidselectiontest.MainActivity;
import com.jossalgon.androidselectiontest.envAnalysis.BluetoothAnalysis.BluetoothDiscoveredDevice;
import com.jossalgon.androidselectiontest.envAnalysis.WifiAnalysis.WifiDiscoveredDevice;
import com.jossalgon.androidselectiontest.sensorAnalysis.AccelerationAnalysis.Acceleration;
import com.jossalgon.androidselectiontest.sensorAnalysis.StepsAnalysis.Step;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

public class FileExporter extends AsyncTask<FileExporter.FirebaseData, Void, String> {
    public static class FirebaseData {
        public ArrayList<Acceleration> accelerations;
        public ArrayList<Step> steps;
        public ArrayList<WifiDiscoveredDevice> wifiDevices;
        public ArrayList<BluetoothDiscoveredDevice> bluetoothDevices;

        public FirebaseData(ArrayList<Acceleration> accelerations, ArrayList<Step> steps,
                            ArrayList<WifiDiscoveredDevice> wifiDevices,
                            ArrayList<BluetoothDiscoveredDevice> bluetoothDevices) {
            this.accelerations = accelerations;
            this.steps = steps;
            this.wifiDevices = wifiDevices;
            this.bluetoothDevices = bluetoothDevices;
        }
    }

    @Override
    protected String doInBackground(FirebaseData... params) {
        try {
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            Long tsLong = System.currentTimeMillis()/1000;
            String filePath = dir.getPath()+"/"+tsLong.toString()+".json";
            Writer writer = new FileWriter(filePath);
            Gson gson = new GsonBuilder().create();
            gson.toJson(params[0], writer);
            writer.close();
            return filePath;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onPostExecute(String filePath) {
        MainActivity.onFileExporterFinished(filePath);
    }
}
