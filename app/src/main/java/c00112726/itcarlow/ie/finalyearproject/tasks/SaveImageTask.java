package c00112726.itcarlow.ie.finalyearproject.tasks;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import c00112726.itcarlow.ie.finalyearproject.activities.CameraPreview;
import c00112726.itcarlow.ie.finalyearproject.tasks.callbacks.TaskCallback;

/**
 * Author: Michael Reid.
 * ID: C00112726
 * Email: C00112726@itcarlow.ie
 * Date: 05/02/2016
 */
public class SaveImageTask extends AsyncTask<byte[], String, File> {

    private static final String IMAGE_DATA_PATH =
            Environment.getExternalStorageDirectory().toString() + "/MyAppFolder/AppImages/";
    private static final String TAG = "SaveImageTask";

    private TaskCallback mTaskCallback;
    private CameraPreview mCameraPreview;

    private ProgressDialog mProgressDialog;

    public SaveImageTask(TaskCallback taskCallback, CameraPreview cameraPreview) {
        mTaskCallback = taskCallback;
        mCameraPreview = cameraPreview;
    }

    @Override
    protected void onPreExecute() {
        mProgressDialog = new ProgressDialog((Context) mTaskCallback);
        mProgressDialog.setMessage("Saving Image...");
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
    }

    @Override
    protected File doInBackground(byte[]... data) {

        File imageFile = createOutputPictureFile();
        if(imageFile == null) {
            Log.e(TAG, "No file created");
            return null;
        }
        try {
            Bitmap image = BitmapFactory.decodeByteArray(data[0], 0, data[0].length);
            Bitmap croppedImage = cropImage(image);
            FileOutputStream out = new FileOutputStream(imageFile);
            croppedImage.compress(Bitmap.CompressFormat.JPEG, 100, out);
            croppedImage = correctRotation(croppedImage, imageFile.getAbsolutePath());
            croppedImage.compress(Bitmap.CompressFormat.JPEG, 100, out);

            out.flush();
            out.close();
            return imageFile;
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }

    @Override
    public void onPostExecute(File file) {
        if(mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }

        if(mTaskCallback != null) {
            mTaskCallback.onTaskComplete(file);
        }
    }

    private File createOutputPictureFile() {
        File imageStorageDirectory = new File(IMAGE_DATA_PATH);

        // If the default save directory doesn't exist, try and create it
        if (!imageStorageDirectory.exists()){
            if (!imageStorageDirectory.mkdirs()){
                Log.e(TAG, "Required media storage does not exist");
                return null;
            }
        }

        // Create a timestamp and use it as part of the file name
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.UK);
        String timeStamp = dateFormat.format(new Date());
        String fileName = "img_"+ timeStamp + ".jpg";

        return new File (imageStorageDirectory, fileName);
    }

    private Bitmap cropImage(Bitmap image) {

        Rect guideBox           = mCameraPreview.getGuideBox();

        int cameraPreviewWidth  = mCameraPreview.getWidth();
        int cameraPreviewHeight = mCameraPreview.getHeight();

        int imageWidth          = image.getWidth();
        int imageHeight         = image.getHeight();

        float widthRatio        = (float)imageWidth / (float)cameraPreviewWidth;
        float heightRatio       = (float)imageHeight / (float)cameraPreviewHeight;

        int cropX               = (int)(guideBox.left * widthRatio);
        int cropY               = (int)(guideBox.top * heightRatio);
        int cropWidth           = (int)(guideBox.width() * widthRatio);
        int cropHeight          = (int)(guideBox.height() * heightRatio);

        return Bitmap.createBitmap(image, cropX, cropY, cropWidth, cropHeight);
    }

    private Bitmap correctRotation(Bitmap image, String path) {
        try {
            ExifInterface exif = new ExifInterface(path);
            int exifOrientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);

            Log.d(TAG, "Orientation: " + exifOrientation);

            int rotate = 0;

            switch (exifOrientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 0;
                    break;

                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 180;
                    break;
            }

            Log.v(TAG, "Rotation: " + rotate);

            if (rotate != 0) {

                // Getting width & height of the given image.
                int w = image.getWidth();
                int h = image.getHeight();

                // Setting pre rotate
                Matrix mtx = new Matrix();
                mtx.preRotate(rotate);

                // Rotating Bitmap
                image = Bitmap.createBitmap(image, 0, 0, w, h, mtx, false);
            }

            // Convert to ARGB_8888
            return image.copy(Bitmap.Config.ARGB_8888, true);

        } catch (IOException e) {
            Log.e(TAG, "Couldn't correct orientation: " + e.toString());
        }
        return image;
    }
}
