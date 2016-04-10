package c00112726.itcarlow.ie.finalyearproject.activities;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.Log;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

/**
 * Author: Michael Reid
 * ID: C00112726
 * Email: c00112726@itcarlow.ie
 * Date: 03/02/2016
 */
@SuppressWarnings("deprecation")
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = " CameraPreview";

    protected SurfaceHolder mSurfaceHolder;
    protected Camera mCamera;
    protected Context mContext;

    protected OrientationEventListener mOrientationListener;

    protected Rect mGuideBox;

    public CameraPreview(Context context, Camera camera) {
        super(context);

        mContext = context;
        setCamera(camera);

        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);

        // deprecated since 3.0
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceHolder.setKeepScreenOn(true);

        mOrientationListener = new OrientationEventListener(context) {
            @Override
            public void onOrientationChanged(int orientation) {
                setCameraDisplayOrientation();
            }
        };

        mGuideBox = createGuideBox();
        setWillNotDraw(false);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
            mCamera.startPreview();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        mOrientationListener.enable();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mGuideBox = createGuideBox();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mOrientationListener.disable();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Paint guideBoxPaintDetails = new Paint();
        guideBoxPaintDetails.setStyle(Paint.Style.STROKE);

        guideBoxPaintDetails.setColor(Color.BLUE);
        guideBoxPaintDetails.setStrokeWidth(15.0f);
        canvas.drawRect(mGuideBox, guideBoxPaintDetails);

        guideBoxPaintDetails.setColor(Color.YELLOW);
        guideBoxPaintDetails.setStrokeWidth(7.0f);
        canvas.drawRect(mGuideBox, guideBoxPaintDetails);
    }

    private void setCameraDisplayOrientation() {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Display display = ((Activity)mContext).getWindowManager().getDefaultDisplay();

        int rotation = display.getRotation();
        int degrees = 0;

        switch (rotation) {
            case Surface.ROTATION_90:   degrees = 0;    break;
            case Surface.ROTATION_270:  degrees = 180;  break;
        }

        // Adjust the cameras rotation
        int displayOrientation = (info.orientation - degrees + 360) % 360;
        mCamera.setDisplayOrientation(displayOrientation);

        // Adjust the CameraPreview rotation
        Camera.Parameters params = mCamera.getParameters();
        params.setRotation(degrees);
        if (params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }
        //params.setPictureSize(getWidth(), getHeight());
        try {
            mCamera.setParameters(params);
        }
        catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private Rect createGuideBox() {
        // These values are the standard dimension of an Irish license plate in millimeters.
        final float PLATE_WIDTH = 520.0f;
        final float PLATE_HEIGHT = 110.0f;
        // 4.72727272 repeating
        final float RATIO = PLATE_WIDTH / PLATE_HEIGHT;

        // The display dimensions.
        int width = getWidth();
        int height = getHeight();

        int rectW = (int) (width * 0.50f);
        int rectH = (int) (rectW / RATIO);

        return new Rect(
                (width / 2 - rectW / 2),
                (height / 2 - rectH / 2),
                (width - (width / 2 - rectW / 2)),
                (height - (height / 2 - rectH / 2))
        );
    }

    private void setCamera(Camera camera) {
        if(camera == mCamera) { return; }

        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }

        mCamera = camera;
        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
            mCamera.startPreview();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }

    public Rect getGuideBox() {
        return mGuideBox == null ? createGuideBox() : mGuideBox;
    }
}