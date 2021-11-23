package com.example.cam_stream;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.WorkManager;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import com.example.camera2.R;

public class MainActivity extends AppCompatActivity {
    private static final String[] CAMERA_PERMISSION = new String[]{Manifest.permission.CAMERA};
    private static final int CAMERA_REQUEST_CODE = 10;
    private WorkManager mWorkManager;
    protected static final String TAG = "cam_stream";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow(). addFlags (WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Log.i(TAG,"in main");
        //startService();

        Server server=new Server();
        server.startServer();

        checkPermissionsOrRequest();

        if (hasCameraPermission()) {
            Intent cameraServiceIntent = new Intent(MainActivity.this, Camera2Service.class);
            Log.i(TAG,"starting cam..");

            // camera apis expect the cameraId to be a string
            // from testing, regular lens = 0, wide angle = 1
            String idString = Integer.toString(0);
            cameraServiceIntent.putExtra("cameraId", idString);
            Log.i(TAG,"starting service...");
            //startService(cameraServiceIntent);
            ContextCompat.startForegroundService(this, cameraServiceIntent);
        } else {
            requestPermission();
        }



    }

    private void checkPermissionsOrRequest() {
        // The request code used in ActivityCompat.requestPermissions()
        // and returned in the Activity's onRequestPermissionsResult()
        int PERMISSION_ALL = 1;
        String[] permissions = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,
                Manifest.permission.WAKE_LOCK,
                Manifest.permission.ACCESS_NETWORK_STATE
        };

        if (!hasPermissions(this, permissions)) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_ALL);
        }
    }

    public boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "hasPermissions: no permission for " + permission);
                    return false;
                } else {
                    Log.d(TAG, "hasPermissions: YES permission for " + permission);
                }
            }
        }
        return true;
    }


    public void startService() {
        Log.i(TAG,"in start service..");
        Intent serviceIntent = new Intent(this, ServerService.class);
        serviceIntent.putExtra("inputExtra", "passing any text");
        ContextCompat.startForegroundService(this, serviceIntent);
    }
    public void stopService(View v) {
        Intent serviceIntent = new Intent(this, ServerService.class);
        stopService(serviceIntent);
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED;
    }
    private void requestPermission() {
        ActivityCompat.requestPermissions(
                this,
                CAMERA_PERMISSION,
                CAMERA_REQUEST_CODE
        );
    }
}