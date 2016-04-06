package c00112726.itcarlow.ie.finalyearproject.processing.template;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import c00112726.itcarlow.ie.finalyearproject.processing.OpenCvImageProcessor;

/**
 * Author: Michael Reid.
 * ID: C00112726
 * Email: c00112726@itcarlow.ie
 * Date: 02/04/2016
 */
public class TemplateLoader {

    private final static String[] dirs = {
            "/image_set_1/",
            "/image_set_2/",
            "/image_set_3/",
    };

    private final static String[] letters = {
            "C", "D", "E", "G", "H",
            "K", "L", "M", "N", "O", "R",
            "S", "T", "W", "X", "Y"
    };

    private final static String[] numbers = {
            "0", "1", "2", "3", "4",
            "5", "6", "7", "8", "9"
    };

    private TemplateLoader() {}

    public static ImageTemplates loadTemplates(Context context, String directory) throws IOException {
        List<ImageTemplate> letterTemplates = new ArrayList<>();
        List<ImageTemplate> numberTemplates = new ArrayList<>();

        for (String letter : letters) {
            letterTemplates.add(loadImageTemplates(context, directory, letter));
        }

        for (String number : numbers) {
            numberTemplates.add(loadImageTemplates(context, directory, number));
        }

        return new ImageTemplates(letterTemplates, numberTemplates);
    }

    private static ImageTemplate loadImageTemplates(Context context, String directory, String templateName) throws IOException {
        final List<Mat> images = new ArrayList<>();

        for (String dir : dirs) {
            try {
                String filename = directory + dir + templateName + ".jpg";
                InputStream is = context.getAssets().open(filename);
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                Mat mat = OpenCvImageProcessor.bitmapToMat(bitmap, CvType.CV_8UC3);
                Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);
                Imgproc.resize(mat, mat, new Size(36.0f, 70.0f));
                images.add(mat);
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }

        return new ImageTemplate(templateName, images);
    }
}