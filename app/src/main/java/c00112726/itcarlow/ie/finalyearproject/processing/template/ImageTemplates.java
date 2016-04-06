package c00112726.itcarlow.ie.finalyearproject.processing.template;

import java.util.List;

/**
 * Author: Michael Reid.
 * ID: C00112726
 * Email: c00112726@itcarlow.ie
 * Date: 02/04/2016
 */
public class ImageTemplates {

    private List<ImageTemplate> letterTemplates;
    private List<ImageTemplate> numberTemplates;

    public ImageTemplates(List<ImageTemplate> letterTemplates, List<ImageTemplate> numberTemplates) {
        this.letterTemplates = letterTemplates;
        this.numberTemplates = numberTemplates;
    }

    public List<ImageTemplate> getLetterTemplates() {
        return letterTemplates;
    }

    public List<ImageTemplate> getNumberTemplates() {
        return numberTemplates;
    }
}