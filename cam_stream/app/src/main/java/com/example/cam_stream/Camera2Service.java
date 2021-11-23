package com.example.cam_stream;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.util.Range;

import java.nio.ByteBuffer;
import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

public class Camera2Service extends Service {
    protected static final int CAMERA_CALIBRATION_DELAY = 5000;
    protected static final String TAG = "camera2Service";
    protected static final int CAMERACHOICE = CameraCharacteristics.LENS_FACING_FRONT;
    protected static long cameraCaptureStartTime;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession session;
    protected ImageReader imageReader;
    protected CameraCharacteristics camCharacteristics;
    protected int seq=0;
    int ONGOING_NOTIFICATION_ID=1;
    private static final int ID_SERVICE = 101;

    protected CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.d(TAG, "CameraDevice.StateCallback onOpened");
            cameraDevice = camera;
            actOnReadyCameraDevice();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.w(TAG, "CameraDevice.StateCallback onDisconnected");
        }

        @Override
        public void onError(@NonNull CameraDevice camera,
                            int error) {
            Log.e(TAG, "CameraDevice.StateCallback onError " + error);
        }
    };

    protected CameraCaptureSession.StateCallback sessionStateCallback = new CameraCaptureSession.StateCallback() {

        @Override
        public void onReady(CameraCaptureSession session) {
            Camera2Service.this.session = session;
            try {
                session.setRepeatingRequest(createCaptureRequest(), null, null);
//                session.capture(createCaptureRequest(), null, null);
                cameraCaptureStartTime = System.currentTimeMillis();
            } catch (CameraAccessException e) {
                Log.e(TAG, e.getMessage());
            }
        }


        @Override
        public void onConfigured(CameraCaptureSession session) {

        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
        }
    };
    /*
    Frames per seconds when
    no load = 29
    ByteBuffer buffer = img.getPlanes()[0].getBuffer(); = 31
    byte[] bytes = new byte[buffer.remaining()];
    buffer.get(bytes); = 40 !
    Bitmap bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null); = 30
    height=bitmapImage.getHeight();
    width=bitmapImage.getWidth();
    int[] data = new int[width * height];
    bitmapImage.getPixels(data, 0, width, 0, 0, width, height); = 30
    *****bottleneck***********
     String str_img=Arrays.toString(data);  = 4 :O

     */

    protected ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            //Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            //Log.i(TAG,"displaying " + timestamp);
            Image img = reader.acquireLatestImage();
            ByteBuffer buf = img.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buf.remaining()];
            buf.get(bytes);
            /*
            ByteBuffer buf = img.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buf.remaining()];
            buf.get(bytes);
            //Log.i(TAG,String.valueOf(bytes.length));
            int i=bytes[0];
            //Log.i(TAG,String.valueOf(i));
            //i=bytes[1];
            //Log.i(TAG,String.valueOf(i));

            Log.d(TAG, String.valueOf(reader.getHeight())+","+String.valueOf(reader.getWidth()));
            //Log.d(TAG, String.valueOf(img.getHeight())+","+String.valueOf(img.getWidth()));
            */

            if(Server.client!=null && Server.client.isBound() && Server.ready){
                Log.d(TAG, "on image available ");
                //Log.d(TAG, "not null");
                if (img != null) {
                    Server.seq+=1;
                    //Log.i(TAG,String.valueOf(bytes.length));
                    //keep the buffer to a fixed max length
                    ImageData data=new ImageData(seq,img.getHeight(),img.getWidth(),bytes.length,bytes);
                    if(Server.img_list.size() < 5){
                        Log.d(TAG, "added image to list");
                        Server.img_list.add(data);
                    }
                }
            }
            if (img != null) {
                img.close();
            }
        seq+=1;
        }
    };

    public void readyCamera(String cameraId) {
        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
//            String pickedCamera = getCamera(manager);
            String pickedCamera = cameraId; // using the hardcoded cameraId instead of picking with code
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            manager.openCamera(pickedCamera, cameraStateCallback, null);
            imageReader = ImageReader.newInstance(820, 1052, ImageFormat.JPEG, 5 /* images buffered */);
            imageReader.setOnImageAvailableListener(onImageAvailableListener, null);
            Log.d(TAG, "imageReader created");
        } catch (CameraAccessException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public String getCamera(CameraManager manager) {
        try {
            for (String cameraId : manager.getCameraIdList()) {
                camCharacteristics = manager.getCameraCharacteristics(cameraId);
                int cOrientation = camCharacteristics.get(CameraCharacteristics.LENS_FACING);
                if (cOrientation == CAMERACHOICE) {
                    return cameraId;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent,
                              int flags,
                              int startId) {
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
        Log.d(TAG, "onStartCommand flags " + flags + " startId " + startId);
        //String cameraId = intent.getStringExtra("cameraId");
        readyCamera("0");

        return START_NOT_STICKY;
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel(NotificationManager notificationManager){
        String channelId = "my_service_channelid";
        String channelName = "My Foreground Service";
        NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
        // omitted the LED color
        channel.setImportance(NotificationManager.IMPORTANCE_NONE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        notificationManager.createNotificationChannel(channel);
        return channelId;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate service");
        super.onCreate();
    }

    public void actOnReadyCameraDevice() {
        try {
            cameraDevice.createCaptureSession(Arrays.asList(imageReader.getSurface()), sessionStateCallback, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        cameraDevice.close();
    }



    protected CaptureRequest createCaptureRequest() {
        try {
            CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            builder.addTarget(imageReader.getSurface());
//            WindowManager windowManager = (WindowManager)getSystemService(WINDOW_SERVICE);
//            int rotation = windowManager.getDefaultDisplay().getRotation();
//            int jpegRotation = getJpegOrientation(camCharacteristics, rotation);
            Range<Integer> fpsRange = new Range<>(30,40);
            builder.set(CaptureRequest.JPEG_ORIENTATION, 180); // hardcoding orientation for the tomy camera
            builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,fpsRange);
            return builder.build();
        } catch (CameraAccessException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}