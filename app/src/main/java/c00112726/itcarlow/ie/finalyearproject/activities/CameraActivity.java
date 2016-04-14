package c00112726.itcarlow.ie.finalyearproject.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.OpenCVLoader;

import java.io.File;

import c00112726.itcarlow.ie.finalyearproject.R;
import c00112726.itcarlow.ie.finalyearproject.misc.NumberPlate;
import c00112726.itcarlow.ie.finalyearproject.misc.Util;
import c00112726.itcarlow.ie.finalyearproject.tasks.DatabaseConnectionTask;
import c00112726.itcarlow.ie.finalyearproject.tasks.ProcessImageTask;
import c00112726.itcarlow.ie.finalyearproject.tasks.SaveImageTask;
import c00112726.itcarlow.ie.finalyearproject.tasks.callbacks.TaskCallback;
import c00112726.itcarlow.ie.finalyearproject.tasks.callbacks.TaskCallbackJSON;

/**
 * Author: Michael Reid
 * ID: C00112726
 * Email: c00112726@itcarlow.ie
 * Date: 03/02/2016
 */
@SuppressWarnings("deprecation")
public class CameraActivity extends AppCompatActivity implements TaskCallback, TaskCallbackJSON {

    private static final String TAG = "CameraActivity";

    protected CameraPreview mCameraPreview;
    protected Camera mCamera;
    protected FrameLayout mPreview;
    protected boolean mCanTakePicture;
    private File imageFile;

    /**
     * Callback which is called when the camera is told to take a picture.
     * From here, we spawn an AsyncTask to perform the save.
     */
    protected PictureCallback mPictureCallback = new PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            mCamera.stopPreview();
            SaveImageTask task = new SaveImageTask(CameraActivity.this, mCameraPreview);
            task.execute(data);
        }
    };

    /**
     * Define what happens when the screen is tapped.
     * In this case, we tell the camera to take a picture
     */
    protected View.OnClickListener mPreviewOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mCanTakePicture) {
                mCanTakePicture = false;
                mCamera.takePicture(null, null, mPictureCallback);
            }
        }
    };

    /**
     * A callback used to link the app with OpenCV manager.
     * In the case it is not installed, the option to install it
     * will be presented.
     */
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

        // Attempt to load OpenCV from OpenCV Manager.
        if(!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mOpenCVCallback)) {
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
        mCamera.startPreview();
    }

    /**
     * Safe method to open the camera
     * @return the device camera
     */
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

    private void startCamera() {
        mCanTakePicture = true;
        mCamera.startPreview();
    }

    /**
     * Callback called by an AsyncTask to inform the Activity that it
     * has finished it's task. Allows the Activity to decide what to do next.
     * @param json The JSON result from the Web API
     */
    @Override
    public void onTaskComplete(JSONObject json) {
        if(json == null) {
            String message = getString(R.string.connect_failed);
            Util.showToast(this, message, Toast.LENGTH_SHORT);
            startCamera();
            return;
        }
        String KEY_REGISTRATION = "registration";
        String KEY_INFRACTION = "infraction";
        try {
            String infraction = json.getString(KEY_INFRACTION);
            String registration = json.getString(KEY_REGISTRATION);
            if(Boolean.parseBoolean(infraction)) {
                String message = getString(R.string.infraction_occured);
                message +=  "\n" + getString(R.string.registration) + registration;
                Util.showToast(this, message, Toast.LENGTH_LONG);
            }
            else {
                String message = getString(R.string.no_infraction);
                message += "\n" + getString(R.string.registration) + registration;
                Util.showToast(this, message, Toast.LENGTH_LONG);
            }
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
            String message = getString(R.string.bad_json);
            Util.showToast(this, message, Toast.LENGTH_SHORT);
        }
        startCamera();
    }

    /**
     * Callback called by an AsyncTask to inform the Activity that it
     * has finished it's task. Allows the Activity to decide what to do next.
     * @param numberPlate The OCR result of the ImageProcessing
     */
    @Override
    public void onTaskComplete(NumberPlate numberPlate) {
        if(numberPlate == null) {
            String message = getString(R.string.segment_failed);
            Util.showToast(this, message, Toast.LENGTH_SHORT);
            numberPlate = new NumberPlate();
        }
        showConfirmationDialog(numberPlate);
    }

    /**
     * Callback called by an AsyncTask to inform the Activity that it
     * has finished it's task. Allows the Activity to decide what to do next.
     * @param file The file where the image was saved
     */
    @Override
    public void onTaskComplete(File file) {
        if(file == null) {
            String message = getString(R.string.save_fail);
            Util.showToast(this, message, Toast.LENGTH_SHORT);
            return;
        }

        imageFile = file;
        notifyDeviceOfNewFile(file);

        ProcessImageTask pit = new ProcessImageTask(this);
        pit.execute(file);
}

    /**
     * Inform device a new file has been saved to it
     * @param file the new file
     */
    private void notifyDeviceOfNewFile(File file) {
        final Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        final Uri contentUri = Uri.fromFile(file);
        mediaScanIntent.setData(contentUri);
        sendBroadcast(mediaScanIntent);
    }

    /**
     * Display a confirmation dialog to validate the OCR result
     * @param numberPlate The OCR result of the ImageProcessing
     */
    private void showConfirmationDialog(final NumberPlate numberPlate) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.result_validate);
        builder.setMessage(getString(R.string.number_plate) +
                numberPlate.toString() + "\n" +
                getString(R.string.edit_continue));
        builder.setCancelable(false);
        builder.setNeutralButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                startCamera();
            }
        });
        builder.setNegativeButton(getString(R.string.edit), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                editClick(numberPlate);
            }
        });
        builder.setPositiveButton(getString(R.string.continue_str),
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                okClick(numberPlate);
            }
        });

        builder.show();
    }

    /**
     * Called when the user validates the reg. Sends OCR result to database.
     * @param numberPlate The OCR result of the ImageProcessing
     */
    private void okClick(NumberPlate numberPlate) {
        if(Util.isNetworkAvailable(this)) {
            DatabaseConnectionTask dbt = new DatabaseConnectionTask(this);
            dbt.execute(numberPlate);
        }
        else {
            String message = getString(R.string.no_network_1);
            Util.showToast(this, message, Toast.LENGTH_SHORT);
            startCamera();
        }
    }

    /**
     * Called when the user want to edit the reg. Starts new activity
     * @param numberPlate The OCR result of the ImageProcessing
     */
    private void editClick(NumberPlate numberPlate) {
        Intent intent = new Intent(this, EditRegActivity.class);
        intent.putExtra("number plate", numberPlate);
        intent.putExtra("image file", imageFile);
        startActivity(intent);
    }
}