package com.example.wearsensor;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class MainActivity extends WearableActivity implements SensorEventListener {

    private ImageButton btnStart, btnStop, btnWifi, btnUpload;
    private FileWriter fileWriterAcc, fileWriterGyro;

    private String currentDate;
    private String hostIp, hostSSID, hostPassword;

    public static final int MULTIPLE_PERMISSIONS = 10;
    private final String[] permissions = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.INTERNET};

    private SensorManager senSensorManager;
    private Sensor senAccelerometer, senGyroscope;
    private boolean connectedToMobile;

    public static WifiManager wifiManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        while (!hasPermissions(this,permissions)){
            ActivityCompat.requestPermissions(this,permissions,MULTIPLE_PERMISSIONS);
        }

        // Enables Always-on
        setAmbientEnabled();

        addDirectory();

        if(!(Thread.getDefaultUncaughtExceptionHandler() instanceof CustomExceptionHandler)) {
            Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler(
                    String.valueOf(Environment.getExternalStoragePublicDirectory("FreeForm-Writing"))));
        }

        initialize();
        onClickListner();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (wifiManager.isWifiEnabled()){
            WifiInfo info = wifiManager.getConnectionInfo();
            String checkSSID = info.getSSID();
            if (checkSSID.equals(hostSSID)){
                btnWifi.setImageDrawable(getResources().getDrawable(R.drawable.ic_baseline_wifi_off_24));
            } else {
                btnWifi.setImageDrawable(getResources().getDrawable(R.drawable.ic_wifi_black_24dp));
            }
        }
    }

    private void initialize() {
        btnStart = findViewById(R.id.btn_start);
        btnStop = findViewById(R.id.btn_stop);
        btnUpload = findViewById(R.id.btn_upload);
        btnWifi = findViewById(R.id.btn_wifi);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        connectedToMobile = false;
        hostSSID = "dummy";

        currentDate = new SimpleDateFormat("MMddyyyy", Locale.getDefault()).format(new Date());
        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senGyroscope = senSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReciever,
                new IntentFilter("custom-event-name"));
    }

    private BroadcastReceiver mMessageReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            getInfoFromString(intent.getStringExtra("info"));
        }
    };

    private void onClickListner() {
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String fileNameAcc = "ACC_" + currentDate + ".csv";
                String fileNameGyro = "GYRO_" + currentDate + ".csv";
                Toast.makeText(MainActivity.this,"Data Recording Started",Toast.LENGTH_LONG).show();
                try {
                    fileWriterAcc = new FileWriter(Environment.getExternalStoragePublicDirectory("FreeForm-Writing/Recorded Data/" + fileNameAcc),true);
                    fileWriterGyro = new FileWriter(Environment.getExternalStoragePublicDirectory("FreeForm-Writing/Recorded Data/" + fileNameGyro),true);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                senSensorManager.registerListener(MainActivity.this,senAccelerometer,SensorManager.SENSOR_DELAY_GAME);
                senSensorManager.registerListener(MainActivity.this,senGyroscope,SensorManager.SENSOR_DELAY_GAME);
                btnUpload.setVisibility(View.GONE);
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this,"Data Recording Stopped",Toast.LENGTH_LONG).show();
                senSensorManager.unregisterListener(MainActivity.this,senAccelerometer);
                senSensorManager.unregisterListener(MainActivity.this,senGyroscope);
                /*new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            for (int i = 1 ; i <= 200 ; i++){
                                fileWriterAcc.write("dummy,dummy,dummy,dummy\n");
                                fileWriterAcc.flush();
                                fileWriterGyro.write("dummy,dummy,dummy,dummy\n");
                                fileWriterGyro.flush();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();*/
                btnUpload.setVisibility(View.VISIBLE);
            }
        });

        btnWifi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (wifiManager.isWifiEnabled()){
                    WifiInfo info = wifiManager.getConnectionInfo();
                    String checkSSID = info.getSSID();
                    if (!checkSSID.equals(hostSSID)){
                        if (wifiManager.pingSupplicant()){
                            wifiManager.disconnect();
                            wifiManager.disableNetwork(info.getNetworkId());
                            connectToHotspot();
                        }
                    } else {
                        Toast.makeText(MainActivity.this,"Already Connected to Host",Toast.LENGTH_SHORT).show();
                    }
                } else{
                    wifiManager.setWifiEnabled(true);
                    connectToHotspot();
                }
            }
        });

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new ClientTask(hostIp,8988,MainActivity.this)).start();
            }
        });
    }

    private void connectToHotspot(){
        if (!hostSSID.equals("dummy")){
            WifiConfiguration wc = new WifiConfiguration();
            wc.SSID = "\"" + hostSSID + "\"";
            wc.preSharedKey = "\"" + hostPassword + "\"";
            wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);

            int res = wifiManager.addNetwork(wc);
            boolean b = wifiManager.enableNetwork(res,true);
            if (b){
                Toast.makeText(this,"Connected to Host",Toast.LENGTH_SHORT).show();
                Log.e("MainActivity","Connected");
                btnWifi.setImageDrawable(getResources().getDrawable(R.drawable.ic_baseline_wifi_off_24));
            }
            else{
                Toast.makeText(this,"Not Connected",Toast.LENGTH_SHORT).show();
                Log.e("MainActivity","Not Connected");
                btnWifi.setImageDrawable(getResources().getDrawable(R.drawable.ic_wifi_black_24dp));
            }
        } else {
            Toast.makeText(this,"Create Host first",Toast.LENGTH_SHORT).show();
            btnWifi.setImageDrawable(getResources().getDrawable(R.drawable.ic_wifi_black_24dp));
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        long timeStamp = System.currentTimeMillis();
        double x = event.values[0];
        double y = event.values[1];
        double z = event.values[2];
        String msg = timeStamp + "," + x + "," + y + "," + z ;
        try {
            if (sensor.getType() == Sensor.TYPE_ACCELEROMETER ){
                fileWriterAcc.write(msg + "\n");
                fileWriterAcc.flush();
            } else if (sensor.getType() == Sensor.TYPE_GYROSCOPE){
                fileWriterGyro.write(msg + "\n");
                fileWriterGyro.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void getInfoFromString(String info) {
        Log.e("MainActivity", "Info Received " + info);
        String[] values = info.split("::");
        if (values.length == 2)
            Toast.makeText(MainActivity.this,values[0],Toast.LENGTH_LONG).show();
        else {
            Toast.makeText(MainActivity.this,"Credentials received",Toast.LENGTH_SHORT).show();
            hostSSID = values[0];
            hostPassword = values[1];
            hostIp = values[2];
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        senSensorManager.unregisterListener(MainActivity.this,senAccelerometer);
        senSensorManager.unregisterListener(MainActivity.this,senGyroscope);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReciever);
    }

    public boolean hasPermissions(Context context, String... permissions){
        if(context!=null && permissions!=null ){
            for(String permission:permissions){
                if(ActivityCompat.checkSelfPermission(context,permission)!= PackageManager.PERMISSION_GRANTED){
                    return false;
                }
            }
        }
        return true;
    }

    private void addDirectory(){
        File main = Environment.getExternalStoragePublicDirectory("FreeForm-Writing");
        if (!main.exists())
            main.mkdir();
        File record = Environment.getExternalStoragePublicDirectory("FreeForm-Writing/Recorded Data");
        if (!record.exists())
            record.mkdir();
    }
}
