// CameraActivity.java, an activity for heart rate detection, displaying the camera output to
// the screen, and playing an animation when a heart beat is detected.
// - Wesley Chavez, 11/28/16
// Adapted from code at http://stackoverflow.com/questions/39260034/android-studio-camera2-eliminate-noise
//
// This activity displays camera output on the screen and detects a heartbeat from the blood
// oscillations present in the finger (placed on the camera lens) when the torch is enabled.  A
// bitmap is created from the texture (screen) in order to gather luminance information and
// detect a heart rate from the oscillations in the luminance values.


package com.example.android.heartrate;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.widget.ImageView;
import android.widget.Toast;
import java.util.Arrays;

public class CameraActivity extends AppCompatActivity {
    private static final String TAG = "CameraActivity";
    private TextureView textureView; //TextureView to deploy camera data
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private ImageView heartView; //To show heart animation in sync with heartbeat

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
    // Camera member variables
    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private static final int REQUEST_CAMERA_PERMISSION = 1;

    // Thread handler member variables
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    //Heart rate detector member variables
    private int mHeartRateInBPM;
    private int mCurrentRollingAverage;
    private int mLastRollingAverage;
    private int mLastLastRollingAverage;
    private long [] mTimeArray;
    private int numCaptures = 0;
    private int mNumBeats = 0;
    private static final String EXTRA_RESULT_HEART_RATE =
            "package com.example.android.heartrate;";

    // To Return result to Main Activity
    public static int getHeartRate(Intent result) {
        return result.getIntExtra(EXTRA_RESULT_HEART_RATE, 0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Inflate and display texture (camera data)
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        textureView =  findViewById(R.id.texture);
        assert textureView != null;

        // Set a listener when the screen is updated.
        textureView.setSurfaceTextureListener(textureListener);

        // Inflate and display heart image
        heartView =  findViewById(R.id.heartImg);
        heartView.bringToFront();

        // Create an array for storing the current time in milliseconds whenever a heartbeat is
        // detected.
        mTimeArray = new long [30];

        // Display a toast for user with directions on how to proceed.
        Toast.makeText(getApplicationContext(), ("Place your finger lightly on the camera lens." +
                "  Try not to move it.  Elevating your arm increases accuracy."), Toast.LENGTH_LONG).show();
    }
    // Intent for Camera Activity
    public static Intent newIntent(Context packageContext) {
        Intent i = new Intent(packageContext, CameraActivity.class);
        return i;
    }
    // TextureListener to listen for updates in screen pixels
    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //open your camera here
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        // Gets called whenever the surface texture is updated (a variable 20-100 ms)
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            Log.d(TAG, "onSurfaceTextureUpdated");
            // Get bitmap of pixels from texture
            Bitmap bmp = textureView.getBitmap();
            int width = bmp.getWidth();
            int height = bmp.getHeight();
            // Integer array to store formatted pixel values.
            int[] pixels = new int[height * width];
            // Get pixels from the bitmap, starting at (x,y) = (width/2,height/2)
            // and totaling width/20 rows and height/20 columns
            bmp.getPixels(pixels, 0, width, width / 2, height / 2, width / 20, height / 20);

            // Gets red channel information and calculates sum
            int sum = 0;
            for (int i = 0; i < height * width; i++) {
                int red = (pixels[i] >> 16) & 0xFF;
                //int green = (pixels[i] >> 8) & 0xFF;
                //int blue = (pixels[i]) & 0xFF;
                sum = sum + red;// + green + blue;
            }
            // Waits 20 captures, to remove startup artifacts.  First average is the sum.
            if (numCaptures == 20) {
                mCurrentRollingAverage = sum;
            }
            // Next 18 averages needs to incorporate the sum with the correct N multiplier
            // in rolling average.
            else if (numCaptures > 20 && numCaptures < 49) {
                mCurrentRollingAverage = (mCurrentRollingAverage*(numCaptures-20) + sum)/(numCaptures-19);
            }
            // From 49 on, the rolling average incorporates the last 30 rolling averages.
            else if (numCaptures >= 49) {
                mCurrentRollingAverage = (mCurrentRollingAverage*29 + sum)/30;
                // Peak detector/heartbeat detector.  Last and LastLast refer to the previous two
                // values of the rolling average.
                if (mLastRollingAverage > mCurrentRollingAverage && mLastRollingAverage > mLastLastRollingAverage && mNumBeats < 15) {
                    // Save the current time
                    mTimeArray[mNumBeats] = System.currentTimeMillis();
                    // Increment the number of beats detected
                    mNumBeats++;

                    // Play sound and animate heart image
                    //playSound();
                    heartView.setImageResource(R.drawable.heart2);
                    // If 15 beats have been found, this is sufficient and calculate heart rate.
                    if (mNumBeats == 15) {
                        calcBPM();
                    }
                }
            }
            // Another capture
            numCaptures++;
            // Save previous two values
            mLastLastRollingAverage = mLastRollingAverage;
            mLastRollingAverage = mCurrentRollingAverage;
        }
    };
    // A callback objects for receiving updates about the state of a camera device.  The callback
    // is passed to openCamera()
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //This is called when the camera is open
            Log.e(TAG, "onOpened");
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            if (cameraDevice != null)
                cameraDevice.close();
            cameraDevice = null;
        }
    };

    // onResume
    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }
    // onPause
    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Calculate heart rate, put in in an intent's extra for the Main Activity
    private void calcBPM() {
        // Median distance between two peaks of rolling average
        int med;
        // Populate distance array with subtraction of consecutive peaks (15 peaks detected, 14
        // time differences between peaks).
        long [] timedist = new long [14];
        for (int i = 0; i < 14; i++) {
            timedist[i] = mTimeArray[i+1] - mTimeArray[i];
        }
        // Sort to find median
        Arrays.sort(timedist);
        // Midway through the sorted array.
        med = (int) timedist[timedist.length/2];
        // med is in milliseconds
        mHeartRateInBPM = 60000/med;
        // Show user their heart rate
        Toast.makeText(getApplicationContext(), ("Heart Rate = " + mHeartRateInBPM + "BPM"), Toast.LENGTH_LONG).show();
        // Put heart rate in an extra for Main Activity to save in SQL database
        Intent data = new Intent();
        data.putExtra(EXTRA_RESULT_HEART_RATE, mHeartRateInBPM);
        setResult(RESULT_OK, data);
    }
    // After the camera device is opened, it calls this method
    protected void createCameraPreview() {
        try {
            // Create a surface from our texture (screen view of camera data) with size of camera
            // characteristics
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            // Create a capture request and add the newly-created surface as a target.
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(CameraActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    // Opening the rear-facing camera for use
    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Log.e(TAG, "is camera open");
        try {
            // 0 is the rear-facing camera
            cameraId = manager.getCameraIdList()[0];
            //Get output sizes from the camera and save to member variable
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(CameraActivity.this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "openCamera X");
    }
    // Called in createCameraPreview(), sets automatic control mode and enables flash.
    protected void updatePreview() {
        if (null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        // Auto control mode
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        // Enable flash
        captureRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
        try {
            // Request to keep capturing data from camera
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    // Closes camera
    private void closeCamera() {
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    // If permission not granted, display toast and close.
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(CameraActivity.this, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
    // Start thread, open camera or set the surface texture listener
    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }
    // Don't forget to close the camera and kill the thread.
    @Override
    protected void onPause() {
        Log.e(TAG, "onPause");
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }
}








