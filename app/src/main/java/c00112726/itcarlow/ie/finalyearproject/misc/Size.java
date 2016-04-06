package c00112726.itcarlow.ie.finalyearproject.misc;

/**
 * Author: Michael Reid.
 * ID: C00112726
 * Email: c00112726@itcarlow.ie
 * Date: 04/04/2016
 */

import c00112726.itcarlow.ie.finalyearproject.tasks.SaveImageTask;
import c00112726.itcarlow.ie.finalyearproject.tasks.TaskCallback;

/**
 * util.Size is only available from API 21.
 * This App targets API 19 and above, so a simple
 * class is all that is needed to represent size.
 */
public class Size implements TaskCallback{

    private int mWidth;
    private int mHeight;

    public Size() {
        this(0, 0);
    }

    public Size(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public void setWidth(int width) {
        mWidth = width;
    }

    public void setHeight(int height) {
        mHeight = height;
    }

    @Override public String toString() {
        return "(" + mWidth + "," + mHeight + ")";
    }

    @Override
    public void onTaskComplete() {

    }
}
