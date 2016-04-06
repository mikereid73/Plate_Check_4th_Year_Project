package c00112726.itcarlow.ie.finalyearproject.processing.template;

import org.opencv.core.Mat;

import java.util.List;

/**
 * Author: Michael Reid.
 * ID: C00112726
 * Email: c00112726@itcarlow.ie
 * Date: 02/04/2016
 */
public class ImageTemplate {

    private String templateName;
    private List<Mat> images;

    public ImageTemplate(String templateName, List<Mat> images) {
        this.templateName = templateName;
        this.images = images;
    }

    public List<Mat> getImages() {
        return images;
    }

    @Override
    public String toString() {
        return templateName;
    }
}