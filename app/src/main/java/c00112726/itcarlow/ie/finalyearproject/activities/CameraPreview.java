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
import java.util.List;

/**
 * Author: Michael Reid
 * ID: C00112726
 * Email: c00112726@itcarlow.ie
 * Date: 03/02/2016
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = " CameraPrevie Activity";

    protected SurfaceHolder mSurfaceHolder;
    protected Camera mCamera;
    protected Context mContext;
    protected Camera.Size mPreviewSize;
    protected List<Camera.Size> mSupportedPreviewSizes;

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
            Log.e(TAG, "Failed in surfaceCreated. " + e.getMessage());
        }
        mOrientationListener.enable();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if(mSurfaceHolder.getSurface() == null) { return; }

        try {
            mCamera.stopPreview();
            mCamera.setPreviewDisplay(mSurfaceHolder);
            mCamera.startPreview();
        } catch (Exception e) {
            Log.e(TAG, "Failed in surfaceCreated. " + e.getMessage());
        }

        mGuideBox = createGuideBox();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mOrientationListener.disable();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(width, height);

        if (mSupportedPreviewSizes != null) {
            mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
        }
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

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int width, int height)
    {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) width / height;
        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = height;

        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }
        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
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
        mCamera.setParameters(params);
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

        Rect guideBox = new Rect(
                (width / 2 - rectW / 2),
                (height / 2 - rectH / 2),
                (width - (width / 2 - rectW / 2)),
                (height - (height / 2 - rectH / 2))
        );

        return guideBox;
    }

    private void setCamera(Camera camera) {
        if(camera == mCamera) { return; }

        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }

        mCamera = camera;

        if (mCamera != null) {
            Camera.Parameters params = mCamera.getParameters();

            mSupportedPreviewSizes = params.getSupportedPreviewSizes();

            if (params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                mCamera.setParameters(params);
            }

            requestLayout();

            try {
                mCamera.setPreviewDisplay(mSurfaceHolder);
                mCamera.startPreview();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Rect getGuideBox() {
        return mGuideBox == null ? createGuideBox() : mGuideBox;
    }

    public Rect getCameraNativeResolution() {
        Camera.Parameters parameters = mCamera.getParameters();
        Camera.Size size = parameters.getPictureSize();
        final int width = size.width;
        final int height = size.height;
        return new Rect(0, 0, width, height);
    }

    public Rect getDisplayResolution() {
        if(mPreviewSize == null) {
            return null;
        }
        final int width = mPreviewSize.width;
        final int height = mPreviewSize.height;
        return new Rect(0, 0, width, height);
    }

    public Rect getGuideBoxResolution() {
        final int width = mGuideBox.width();
        final int height = mGuideBox.height();
        return new Rect(0, 0, width, height);
    }
}