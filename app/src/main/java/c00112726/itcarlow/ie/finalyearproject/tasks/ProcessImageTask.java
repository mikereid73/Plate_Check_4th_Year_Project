package c00112726.itcarlow.ie.finalyearproject.tasks;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import org.opencv.core.Mat;

import java.io.File;
import java.util.List;
import java.util.Map;

import c00112726.itcarlow.ie.finalyearproject.exceptions.BadImageException;
import c00112726.itcarlow.ie.finalyearproject.misc.NumberPlate;
import c00112726.itcarlow.ie.finalyearproject.processing.OpenCvImageProcessor;
import c00112726.itcarlow.ie.finalyearproject.processing.template.ImageTemplates;
import c00112726.itcarlow.ie.finalyearproject.processing.template.TemplateLoader;
import c00112726.itcarlow.ie.finalyearproject.tasks.callbacks.TaskCallback;

/**
 * Author: Michael Reid.
 * ID: C00112726
 * Email: c00112726@itcarlow.ie
 * Date: 06/04/2016
 */
public class ProcessImageTask extends AsyncTask<File, String, NumberPlate> {

    private static final String TAG = "ProcessImageTask";

    private TaskCallback mTaskCallback;
    private ProgressDialog mProgressDialog;

    public ProcessImageTask(TaskCallback taskCallback) {
        mTaskCallback = taskCallback;
    }

    @Override
    protected void onPreExecute() {
        mProgressDialog = new ProgressDialog((Context) mTaskCallback);
        mProgressDialog.setMessage("Processing...");
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
    }

    @Override
    protected NumberPlate doInBackground(File... data) {
        try {
            File imageFile = data[0];
            String path = imageFile.getAbsolutePath();
            Bitmap image = BitmapFactory.decodeFile(path);

            publishProgress("Processing Image");
            // process image
            Mat imageProcessed = OpenCvImageProcessor.process(image);

            publishProgress("Segmenting Image");
            // segment image
            Map<String, List<Mat>> imageSegmented;
            // There may be nothing to segment, e.g. a black or white screen
            try {
                imageSegmented = OpenCvImageProcessor.segmentImage(imageProcessed);
            }
            catch (BadImageException e) {
                Log.d(TAG, e.getMessage());
                return null;
            }

            publishProgress("Loading Image Templates");
            ImageTemplates templates = TemplateLoader.loadTemplates((Context) mTaskCallback, "templates");

            publishProgress("Performing OCR");
            NumberPlate numberPlate = OpenCvImageProcessor.performOCR(imageSegmented, templates);
            Log.d(TAG, "Recognised text: " + numberPlate.toString());

            return numberPlate;
        }
        catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }

    @Override
    public void onPostExecute(NumberPlate numberPlate) {
        if(mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }

        mTaskCallback.onTaskComplete(numberPlate);
    }

    @Override
    protected void onProgressUpdate(String... status) {
        mProgressDialog.setMessage(status[0]);
    }
}

