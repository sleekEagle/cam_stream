package com.example.cam_stream;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.sql.Timestamp;

public class ServerService extends Service {
    protected static final String TAG = "cam_stream";

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG,"onStartCOmmand");
        String input = intent.getStringExtra("inputExtra");
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, MyApp.CHANNEL_ID)
                .setContentTitle("Auto Start Service")
                .setContentText(input)
                .setContentIntent(pendingIntent)
                .build();
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel( MyApp.CHANNEL_ID, MyApp.CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            mNotificationManager.createNotificationChannel(channel);
            new NotificationCompat.Builder(this, MyApp.CHANNEL_ID);
        }

        startForeground(1, notification);
        Log.i(TAG,"after starting foreground");
        Server server=new Server();
        server.startServer();
        return START_NOT_STICKY;
    }

    public void display(){
        while(true){
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            Log.i(TAG,"displaying " + timestamp);
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}