package c00112726.itcarlow.ie.finalyearproject.tasks;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import c00112726.itcarlow.ie.finalyearproject.R;
import c00112726.itcarlow.ie.finalyearproject.misc.NumberPlate;
import c00112726.itcarlow.ie.finalyearproject.activities.CameraPreview;
import c00112726.itcarlow.ie.finalyearproject.activities.EditRegActivity;
import c00112726.itcarlow.ie.finalyearproject.misc.Util;
import c00112726.itcarlow.ie.finalyearproject.processing.OpenCvImageProcessor;
import c00112726.itcarlow.ie.finalyearproject.processing.template.ImageTemplates;
import c00112726.itcarlow.ie.finalyearproject.processing.template.TemplateLoader;

/**
 * Author: Michael Reid.
 * ID: C00112726
 * Email: C00112726@itcarlow.ie
 * Date: 05/02/2016
 */
public class SaveImageTask extends AsyncTask<byte[], String, NumberPlate> {

    private static final String IMAGE_DATA_PATH =
            Environment.getExternalStorageDirectory().toString() + "/MyAppFolder/AppImages/";
    private static final String TAG = "SaveImageTask";

    private TaskCallback mTaskCallback;
    private CameraPreview mCameraPreview;

    private ProgressDialog mProgressDialog;
    private File imageFile = null;

    public SaveImageTask(TaskCallback taskCallback, CameraPreview cameraPreview) {
        mTaskCallback = taskCallback;
        mCameraPreview = cameraPreview;
    }

    @Override
    protected void onPreExecute() {
        mProgressDialog = new ProgressDialog((Context) mTaskCallback);
        mProgressDialog.setMessage("Preparing...");
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();

        imageFile = createOutputPictureFile();
    }

    @Override
    protected NumberPlate doInBackground(byte[]... data) {
        if(imageFile == null) {
            imageFile = createOutputPictureFile();
        }

        try {
            publishProgress("Saving Image");
            Bitmap image = BitmapFactory.decodeByteArray(data[0], 0, data[0].length);
            Bitmap croppedImage = cropImage(image);
            FileOutputStream out = new FileOutputStream(imageFile);


            publishProgress("Processing Image");
            // process image
            Mat imageProcessed = OpenCvImageProcessor.process(croppedImage);
            OpenCvImageProcessor.matToBitmap(imageProcessed, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
            notifyDeviceOfNewFile(imageFile);

            publishProgress("Segmenting Image");
            // segment image
            Map<String, List<Mat>> imageSegmented = OpenCvImageProcessor.segmentImage(imageProcessed);

            publishProgress("Loading Image Templates");
            ImageTemplates templates = TemplateLoader.loadTemplates((Context)mTaskCallback, "templates");

            publishProgress("Performing OCR");
            NumberPlate numberPlate = OpenCvImageProcessor.performOCR(imageSegmented, templates);
            Log.d(TAG, "Recognised text: " + numberPlate.toString());

            return numberPlate;
        }
        catch (Exception e) {
            Log.e(TAG, "Error: " + e.getMessage());
        }

        return new NumberPlate("", "", "");
    }

    @Override
    public void onPostExecute(NumberPlate result) {
        if(mTaskCallback == null) { return; }
        if(mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
        showDialog(result);
    }

    private void showDialog(final NumberPlate numberPlate) {

        AlertDialog.Builder builder = new AlertDialog.Builder((Context) mTaskCallback);
        builder.setTitle("Result Validation");
        builder.setMessage("Number Plate: " + numberPlate.toString() + "\nEdit or Continue?");
        builder.setCancelable(false);
        builder.setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                mTaskCallback.onTaskComplete();
            }
        });
        builder.setNegativeButton("Edit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent((Context) mTaskCallback, EditRegActivity.class);
                intent.putExtra("number plate", numberPlate);
                intent.putExtra("image file", imageFile);
                ((Activity) mTaskCallback).startActivity(intent);
            }
        });
        builder.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(Util.isNetworkAvailable((Context)mTaskCallback)) {
                    DatabaseConnectionTask dbt = new DatabaseConnectionTask(mTaskCallback);
                    dbt.execute(numberPlate);
                }
                else {
                    String message = ((Context) mTaskCallback).getString(R.string.no_network_1);
                    Util.showToast((Context)mTaskCallback, message, Toast.LENGTH_SHORT);
                }
            }
        });

        builder.show();
    }

    private File createOutputPictureFile() {
        File imageStorageDirectory = new File(IMAGE_DATA_PATH);

        // If the default save directory doesn't exist, try and create it
        if (!imageStorageDirectory.exists()){
            if (!imageStorageDirectory.mkdirs()){
                Log.e(TAG, "ERROR: Required media storage does not exist");
                return null;
            }
        }

        // Create a timestamp and use it as part of the file name
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.UK);
        String timeStamp = dateFormat.format(new Date());
        String fileName = "img_"+ timeStamp + ".jpg";

        return new File (imageStorageDirectory, fileName);
    }

    @Override
    protected void onProgressUpdate(String... status) {
        mProgressDialog.setMessage(status[0]);
    }

    private void notifyDeviceOfNewFile(File file) {
        final Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        final Uri contentUri = Uri.fromFile(file);
        mediaScanIntent.setData(contentUri);
        ((Context) mTaskCallback).sendBroadcast(mediaScanIntent);
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
}
