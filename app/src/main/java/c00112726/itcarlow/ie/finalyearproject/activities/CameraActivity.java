package c00112726.itcarlow.ie.finalyearproject.activities;


import android.content.Context;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.OpenCVLoader;

import c00112726.itcarlow.ie.finalyearproject.R;
import c00112726.itcarlow.ie.finalyearproject.tasks.SaveImageTask;
import c00112726.itcarlow.ie.finalyearproject.tasks.TaskCallback;

/**
 * Author: Michael Reid
 * ID: C00112726
 * Email: c00112726@itcarlow.ie
 * Date: 03/02/2016
 */
public class CameraActivity extends AppCompatActivity implements TaskCallback {

    private static final String TAG = "CameraActivity";

    protected CameraPreview mCameraPreview;

    protected Camera mCamera;

    protected FrameLayout mPreview;

    protected boolean mCanTakePicture;
    protected boolean mBackgroundTaskRunning;

    protected Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            mCamera.stopPreview();
            mBackgroundTaskRunning = true;
            SaveImageTask task = new SaveImageTask(CameraActivity.this, mCameraPreview);
            task.execute(data);
        }
    };

    protected View.OnClickListener mPreviewOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mCanTakePicture) {
                mCanTakePicture = false;
                mCamera.takePicture(null, null, mPictureCallback);
            }
        }
    };

    protected BaseLoaderCallback mOpenCVCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case BaseLoaderCallback.SUCCESS:    Log.i(TAG, "OpenCV Loaded");        break;
                default:                            super.onManagerConnected(status);   break;
            }
        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        mCamera = getCameraInstance();
        mCameraPreview = new CameraPreview(this, mCamera);

        mPreview = (FrameLayout) findViewById(R.id.camera_preview);
        mPreview.addView(mCameraPreview);
        mPreview.setOnClickListener(mPreviewOnClickListener);

        mCanTakePicture = true;
        mBackgroundTaskRunning = false;

        // Attempt to load OpenCV from OpenCV Manager. This will eventually be removed.
        if(!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mOpenCVCallback)) {
            Log.e(TAG, "Cannot connect to OpenCV Manager");
        }
        else {
            Log.i(TAG, "OpenCV was initialised");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // When the activity is paused, we have to immediately release the camera.
        // If we don't, if another activity or app tries to access it, it will be locked.
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }

        // We no longer have a device camera, so we remove the preview display as well.
        if (mCameraPreview != null) {
            mPreview.removeView(mCameraPreview);
            mCameraPreview = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // When the activity resumes, we get a camera instance.
        if (mCamera == null) {
            mCamera = getCameraInstance();
        }

        // We also need somewhere to display the camera preview.
        if (mCameraPreview == null) {
            mCameraPreview = new CameraPreview(this, mCamera);
            mPreview.addView(mCameraPreview);
        }

        mCanTakePicture = true;
        //if(!mBackgroundTaskRunning){
            mCamera.startPreview();
        //}
    }

    private Camera getCameraInstance() {
        Camera camera = null;
        try {
            camera = Camera.open();
        }
        catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return camera;
    }

    @Override
    public void onTaskComplete() {
        Toast.makeText(CameraActivity.this, "Complete", Toast.LENGTH_SHORT).show();
        mCanTakePicture = true;
        mBackgroundTaskRunning = false;
        mCamera.startPreview();
    }
}