package c00112726.itcarlow.ie.finalyearproject.processing;

import android.graphics.Bitmap;
import android.util.Log;

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

import c00112726.itcarlow.ie.finalyearproject.exceptions.BadImageException;
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

    /* The lowest value of RGB */
    private static final int MIN_RGB_BOUND = 0;
    /* The highest value of RGB */
    private static final int MAX_RGB_BOUND = 255;
    /* The average width of a plate character */
    private static final float TARGET_CHAR_WIDTH = 36.0f;
    /* The average height of a plate character */
    private static final float TARGET_CHAR_HEIGHT = 70.0f;
    /* The average aspect ratio of a character */
    private static final float TARGET_ASPECT_RATIO = TARGET_CHAR_WIDTH / TARGET_CHAR_HEIGHT;
    /* A generous 55% error tolerance */
    private static final float ERROR_ALLOWED = 0.55f;
    /* Minimum height of a character */
    private static final float MIN_CHAR_HEIGHT = 50.0f;
    /* Maximum height of a character*/
    private static final float MAX_CHAR_HEIGHT = 100f;
    /* Minimum character aspect ratio of a character. Allows things like the number 1 */
    private static final float MIN_CHAR_ASPECT = 0.05f;
    /* Maximum character aspect ratio, taking error allowance into account */
    private static final float MAX_CHAR_ASPECT = TARGET_ASPECT_RATIO + TARGET_ASPECT_RATIO * ERROR_ALLOWED;
    /* Width image is resized to. Conforms to legal Irish plate width */
    private static final float RESIZE_WIDTH = 520;
    /* Height image is resized to. Conforms to legal Irish plate height */
    private static final float RESIZE_HEIGHT = 110;
    /* The width of the kernel used when performing image opening */
    private static final float MORPH_OPEN_WIDTH = 7;
    /* The height of the kernel used when performing image opening */
    private static final float MORPH_OPEN_HEIGHT = 5;

    /* No instance allowed */
    private OpenCvImageProcessor() {
    }

    /**
     * Utility method to convert an Android Bitmap to an OpenCV Mat
     * @param input image to convert
     * @param type type Mat type
     * @return new Mat
     */
    public static Mat bitmapToMat(Bitmap input, int type) {
        int width = input.getWidth();
        int height = input.getHeight();
        Mat output = new Mat(height, width, type);

        Utils.bitmapToMat(input, output);

        return output;
    }

    /**
     * Pre-process the image for character segmentation.
     * Calls a number of OpenCV methods.
     * @param image image to process
     * @return Mat of processed image
     */
    public static Mat process(Bitmap image) {
        // Convert to Mat, OpenCv only works with Mat
        Mat input = bitmapToMat(image, CvType.CV_8UC3);
        Mat output = new Mat();

        // Resize image. Allows code to work regardless of image quality and size
        Imgproc.resize(input, output, new Size(RESIZE_WIDTH, RESIZE_HEIGHT));
        // Convert to greyscale. Working with colour images is not an option.
        Imgproc.cvtColor(output, output, Imgproc.COLOR_BGR2GRAY);

        // Calculate the Otsu value to use as a lower threshold value.
        double ostuValue = Imgproc.threshold(
                output,
                new Mat(), // we don't care about changing any Mats, we want the return value
                MIN_RGB_BOUND,
                MAX_RGB_BOUND,
                Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU
        );

        // Threshold the image, using the Otsu value calculated.
        // Invert black and white for later steps.
        Imgproc.threshold(
                output,
                output,
                ostuValue,
                MAX_RGB_BOUND,
                Imgproc.THRESH_BINARY_INV
        );

        // Create a structuring element to be used when opening the image.
        Mat element = Imgproc.getStructuringElement(
                Imgproc.MORPH_RECT,
                new Size(MORPH_OPEN_WIDTH, MORPH_OPEN_HEIGHT)
        );

        // Perform image opening.
        // Erode followed by a Dilate
        Imgproc.morphologyEx(
                output,
                output,
                Imgproc.MORPH_OPEN,
                element

        );
        return output;
    }

    /**
     * Takes in a list of found contours in an image and creates a bounding box for them.
     * The boxes are filtered to remove any which don't have the required dimension.
     * @param contours List of found contours
     * @return List of rectangles representing bounding boxes
     */
    private static List<Rect> getBoundingBoxes(List<MatOfPoint> contours) {
        final List<Rect> boundingBoxes = new ArrayList<>();

        //For each contour found
        for (int i = 0; i < contours.size(); i++) {
            MatOfPoint currentContour = contours.get(i);
            Rect box = Imgproc.boundingRect(currentContour);
            float charAspect = (float)box.width / (float)box.height;

            if (    charAspect >= MIN_CHAR_ASPECT &&
                    charAspect <= MAX_CHAR_ASPECT &&
                    box.height >= MIN_CHAR_HEIGHT &&
                    box.height <= MAX_CHAR_HEIGHT) {
                boundingBoxes.add(box);
            } else {
                contours.remove(i--);
            }
        }
        sort(boundingBoxes);
        return boundingBoxes;
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

    /**
     * Segments the individual characters from the processed image.
     * @param processedImage The pre processed image
     * @return A map containing the characters of the image, separated as year, county, reg
     * @throws BadImageException Thrown if no characters are found
     */
    public static Map<String, List<Mat>> segmentImage(Mat processedImage) throws BadImageException {
        // Find all contours in the image and store them in a list.
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(
                processedImage.clone(),
                contours,
                new Mat(),                    // don't care about hierarchy
                Imgproc.RETR_EXTERNAL,        // ignore contours within contours
                Imgproc.CHAIN_APPROX_NONE
        );
        // Throw exception if no contours are found
        if(contours.size() <= 0) {
            throw new BadImageException("No contours were detected");
        }

        // Get bounding boxes for the found contours
        List<Rect> boundingBoxes = getBoundingBoxes(contours);

        // Throw exception if no bounding boxes are returned
        if(boundingBoxes.size() <= 0) {
            throw new BadImageException("No bounding boxes were detected");
        }

        // Using the bounding boxes, segment those areas from main image
        List<Mat> segmentedImages = new ArrayList<>();
        for (int i = 0; i < boundingBoxes.size(); i++) {
            Rect currentBoundingBox = boundingBoxes.get(i);
            Mat currentMat = processedImage.submat(currentBoundingBox);
            segmentedImages.add(currentMat);
        }

        // Split the reg and return characters
        return splitIrishReg(segmentedImages, boundingBoxes);
    }

    /**
     * Split the bounding boxes into 3 parts; year, county, and reg
     * This is useful when performing OCR, to avoid mismatching numbers and letters.
     * Find the two biggest gaps between bounding boxes, which represent the hyphen.
     * @param mats List of segmented characters
     * @param boundingBoxes List of bounding boxes of segmented characters
     * @return Map of the segmented characters, split up into year, county, reg
     */
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
        // Swap values to ensure subList doesn't try to map backwards.
        // i.e.  0-4, 5-2, etc
        if(secondBiggestGapIndex < biggestGapIndex) {
            int temp = biggestGapIndex;
            biggestGapIndex = secondBiggestGapIndex;
            secondBiggestGapIndex = temp;
        }

        try {
            List<Mat> year = mats.subList(0, biggestGapIndex);
            List<Mat> county = mats.subList(biggestGapIndex, secondBiggestGapIndex);
            List<Mat> reg = mats.subList(secondBiggestGapIndex, mats.size());

            // Stick results in a map and return it
            Map<String, List<Mat>> fullRegSplit = new HashMap<>();
            fullRegSplit.put("year", year);
            fullRegSplit.put("county", county);
            fullRegSplit.put("reg", reg);
            return fullRegSplit;
        }
        catch (Exception e) {
            Log.e("Reg Split Failed", e.getMessage());
            return null;
        }
    }

    /**
     * Perform character recognition on the 3 groups of images
     * @param images year, county, reg
     * @param imageTemplates templates for available characters
     * @return Image represented as a NumberPlate object
     */
    public static NumberPlate performOCR(Map<String, List<Mat>> images,
                                         ImageTemplates imageTemplates) {

        List<Mat> yearImages = images.get("year");
        List<Mat> countyImages = images.get("county");
        List<Mat> regImages = images.get("reg");

        String year = matchNumbers(yearImages, imageTemplates);
        String county = matchLetters(countyImages, imageTemplates);
        String reg = matchNumbers(regImages, imageTemplates);

        // Store result in a simple number plate class
        return new NumberPlate(year, county, reg);
    }

    /**
     * Perform template matching on images considered to be letters
     * @param segmentedImages list of character images
     * @param imageTemplates character templates
     * @return segmentedImages represented as a String
     */
    public static String matchLetters(List<Mat> segmentedImages, ImageTemplates imageTemplates) {
        final List<ImageTemplate> letterTemplates = imageTemplates.getLetterTemplates();
        final StringBuilder sb = new StringBuilder();

        for(int x = 0; x < segmentedImages.size(); x++) {
            Mat current = segmentedImages.get(x).clone(); // clone to avoid changing original size
            Imgproc.resize(current, current, new Size(TARGET_CHAR_WIDTH, TARGET_CHAR_HEIGHT));

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

                    // / Localising the best match with minMaxLoc
                    Core.MinMaxLocResult minMaxResult = Core.minMaxLoc(result);
                    if(minMaxResult.maxVal > bestMatchAccuracy) {
                        bestMatchAccuracy = minMaxResult.maxVal;
                        bestMatchName = currentLetter.toString();
                    }
                }
            }
            sb.append(bestMatchName);
        }
        if(segmentedImages.size() == 1 && sb.toString().equals("O")) {
            sb.deleteCharAt(0);
            sb.append("D");
        }
        return sb.toString();
    }

    /**
     * Perform template matching on images considered to be numbers
     * @param segmentedImages list of number images
     * @param imageTemplates character templates
     * @return segmentedImages represented as a String
     */
    public static String matchNumbers(List<Mat> segmentedImages, ImageTemplates imageTemplates) {
        final List<ImageTemplate> numberTemplates = imageTemplates.getNumberTemplates();
        final StringBuilder sb = new StringBuilder();

        for(int x = 0; x < segmentedImages.size(); x++) {
            Mat current = segmentedImages.get(x).clone(); // clone to avoid changing original size
            Imgproc.resize(current, current, new Size(TARGET_CHAR_WIDTH, TARGET_CHAR_HEIGHT));

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

                    // / Localising the best match with minMaxLoc
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