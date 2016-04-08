package c00112726.itcarlow.ie.finalyearproject.tasks.callbacks;

import java.io.File;

import c00112726.itcarlow.ie.finalyearproject.misc.NumberPlate;

/**
 * Author: Michael Reid.
 * ID: C00112726
 * Email: c00112726@itcarlow.ie
 * Date: 01/04/2016
 */
public interface TaskCallback {
    void onTaskComplete(NumberPlate numberPlate);
    void onTaskComplete(File file);
}
