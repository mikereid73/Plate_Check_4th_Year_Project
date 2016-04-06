package c00112726.itcarlow.ie.finalyearproject.processing;

import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import c00112726.itcarlow.ie.finalyearproject.misc.NumberPlate;
import c00112726.itcarlow.ie.finalyearproject.processing.template.ImageTemplate;
import c00112726.itcarlow.ie.finalyearproject.processing.template.ImageTemplates;

/**
 * Author: Michael Reid
 * ID: C00112726
 * Email: c00112726@itcarlow.ie
 * Date: 03/02/2016
 */
public class OpenCvImageProcessor {

    private OpenCvImageProcessor() {
    }

    public static Mat bitmapToMat(Bitmap input, int type) {
        int width = input.getWidth();
        int height = input.getHeight();
        Mat output = new Mat(height, width, type);

        Utils.bitmapToMat(input, output);

        return output;
    }

    public static Bitmap matToBitmap(Mat input, Bitmap.Config config) {
        int width = input.cols();
        int height = input.rows();
        Bitmap output = Bitmap.createBitmap(width, height, config);

        Utils.matToBitmap(input, output);

        return output;
    }

    public static Mat process(Bitmap image) {
        Mat input = bitmapToMat(image, CvType.CV_8UC3);
        Mat output = new Mat();

        //Imgproc.GaussianBlur(input, output, new Size(3, 3), 0);
        Imgproc.resize(input, output, new Size(520, 110));
        Imgproc.cvtColor(output, output, Imgproc.COLOR_BGR2GRAY);

        double ostuValue = Imgproc.threshold(
                output,
                new Mat(), // we don't care about changing any Mats, we want the return value
                0,
                255,
                Imgproc.THRESH_OTSU // Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU
        );

        Imgproc.threshold(
                output,
                output,
                ostuValue,
                255,
                Imgproc.THRESH_BINARY_INV
        );

        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));
        Imgproc.morphologyEx(
                output,
                output,
                Imgproc.MORPH_OPEN,
                element

        );
        Core.bitwise_not(output, output);

        Imgproc.morphologyEx(
                output,
                output,
                Imgproc.MORPH_OPEN,
                element
        );
        Core.bitwise_not(output, output);

        return output;
    }

    private static List<Rect> getBoundingBoxes(List<MatOfPoint> contours) {
        final List<Rect> boundingBoxes = new ArrayList<>();

        //For each contour found
        for (int i = 0; i < contours.size(); i++) {
            MatOfPoint currentContour = contours.get(i);
            Rect box = Imgproc.boundingRect(currentContour);
            float charAspect = (float)box.width / (float)box.height;

            final float aspect = 36.0f / 70.0f;
            final float error = 0.55f;
            final float minHeight = 50;
            final float minAspect = 0.05f;
            final float maxAspect = aspect + aspect * error;

            if (!(charAspect >= minAspect && charAspect <= maxAspect && box.height >= minHeight)) {
                contours.remove(i--);
            } else {
                boundingBoxes.add(box);
            }
        }
        sort(boundingBoxes);
        return boundingBoxes;
    }

    public static Map<String, List<Mat>> segmentImage(Mat processedImage) {
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(
                processedImage.clone(),
                contours,
                new Mat(),                    // don't care about hierarchy
                Imgproc.RETR_EXTERNAL,        // ignore contours within contours
                Imgproc.CHAIN_APPROX_NONE
        );


        List<Rect> boundingBoxes = getBoundingBoxes(contours);
        List<Mat> segmentedImages = new ArrayList<>();

        for (int i = 0; i < boundingBoxes.size(); i++) {
            Rect currentBoundingBox = boundingBoxes.get(i);
            Mat currentMat = processedImage.submat(currentBoundingBox);
            segmentedImages.add(currentMat);
        }

        return splitIrishReg(segmentedImages, boundingBoxes);
    }

    /**
     * Sort bounding boxes based on their x coordinate.
     * Ensures the segmented images are in order, left to right
     * @param boxes bounding boxes to order
     */
    public static void sort(List<Rect> boxes) {
        Collections.sort(boxes, new Comparator<Rect>() {
            @Override
            public int compare(Rect r1, Rect r2) {
                return r1.x > (r2.x) ? 1 : -1;
            }
        });
    }

    public static NumberPlate performOCR(Map<String, List<Mat>> images, ImageTemplates imageTemplates) {

        List<Mat> yearImages = images.get("year");
        List<Mat> countyImages = images.get("county");
        List<Mat> regImages = images.get("reg");

        String year = matchNumbers(yearImages, imageTemplates);
        String county = matchLetters(countyImages, imageTemplates);
        String reg = matchNumbers(regImages, imageTemplates);

        return new NumberPlate(year, county, reg);
    }

    public static Map<String, List<Mat>> splitIrishReg(List<Mat> mats, List<Rect> boundingBoxes) {
        int biggestGap = 0;
        int secondBiggestGap = 0;
        int biggestGapIndex = -1;
        int secondBiggestGapIndex = -1;

        for(int i = 1; i < boundingBoxes.size(); i++) {
            Rect current = boundingBoxes.get(i);
            Rect previous = boundingBoxes.get(i - 1);
            int distance = current.x - (previous.x + previous.width);

            if (distance > biggestGap) {
                // Biggest is now second biggest
                secondBiggestGap = biggestGap;
                secondBiggestGapIndex = biggestGapIndex;

                // New Biggest
                biggestGap = distance;
                biggestGapIndex = i;
            }
            else if (distance > secondBiggestGap && distance <= biggestGap) {
                // distance <= biggestGap : gaps might be same size
                secondBiggestGap = distance;
                secondBiggestGapIndex = i;
            }
        }
        if(secondBiggestGapIndex < biggestGapIndex) {
            int temp = biggestGapIndex;
            biggestGapIndex = secondBiggestGapIndex;
            secondBiggestGapIndex = temp;
        }

        List<Mat> year = mats.subList(0, biggestGapIndex);
        List<Mat> county = mats.subList(biggestGapIndex, secondBiggestGapIndex);
        List<Mat> reg = mats.subList(secondBiggestGapIndex, mats.size());

        Map<String, List<Mat>> fullRegSplit = new HashMap<>();
        fullRegSplit.put("year", year);
        fullRegSplit.put("county", county);
        fullRegSplit.put("reg", reg);
        return fullRegSplit;
    }

    public static String matchLetters(List<Mat> mats, ImageTemplates imageTemplates) {
        final List<ImageTemplate> letterTemplates = imageTemplates.getLetterTemplates();
        final StringBuilder sb = new StringBuilder();

        for(int x = 0; x < mats.size(); x++) {
            Mat current = mats.get(x).clone();
            Imgproc.resize(current, current, new Size(36.0f, 70.0f));

            double bestMatchAccuracy = 0;
            String bestMatchName = "";

            // match letters
            for (ImageTemplate currentLetter: letterTemplates) {
                List<Mat> templates = currentLetter.getImages();
                for(int i = 0; i < templates.size(); i++) {
                    Mat template = templates.get(i);

                    // / Do the matching and normalise
                    Mat result = new Mat();
                    Imgproc.matchTemplate(current, template, result, Imgproc.TM_CCOEFF);

                    // / Localizing the best match with minMaxLoc
                    Core.MinMaxLocResult minMaxResult = Core.minMaxLoc(result);
                    if(minMaxResult.maxVal > bestMatchAccuracy) {
                        bestMatchAccuracy = minMaxResult.maxVal;
                        bestMatchName = currentLetter.toString();
                    }
                }
            }
            sb.append(bestMatchName);
        }
        return sb.toString();
    }

    public static String matchNumbers(List<Mat> segementedImages, ImageTemplates imageTemplates) {
        final List<ImageTemplate> numberTemplates = imageTemplates.getNumberTemplates();
        final StringBuilder sb = new StringBuilder();

        for(int x = 0; x < segementedImages.size(); x++) {
            Mat current = segementedImages.get(x).clone(); // clone to avoid changing original size
            Imgproc.resize(current, current, new Size(36.0f, 70.0f));

            double bestMatchAccuracy = 0;
            String bestMatchName = "";

            // match numbers
            for (ImageTemplate currentNumber: numberTemplates) {
                List<Mat> templates = currentNumber.getImages();
                for(int i = 0; i < templates.size(); i++) {
                    Mat template = templates.get(i);

                    // / Do the matching and normalise
                    Mat result = new Mat();
                    Imgproc.matchTemplate(current, template, result, Imgproc.TM_CCOEFF);

                    // / Localizing the best match with minMaxLoc
                    Core.MinMaxLocResult mmr = Core.minMaxLoc(result);
                    if(mmr.maxVal > bestMatchAccuracy) {
                        bestMatchAccuracy = mmr.maxVal;
                        bestMatchName = currentNumber.toString();
                    }
                }
            }
            sb.append(bestMatchName);
        }
        return sb.toString();
    }
}
