package com.sumitanantwar.android_zoom_view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

/**
 * Zooming view.
 */
public class ZoomView extends FrameLayout
{
    private static final String LOG_TAG = ZoomView.class.getSimpleName();

    /**
     * Zooming view listener interface.
     * 
     * @author karooolek
     * 
     */
    public interface ZoomViewListener {

        void onZoomStarted(float zoom, float zoomx, float zoomy);

        void onZooming(float zoom, float zoomx, float zoomy);

        void onZoomEnded(float zoom, float zoomx, float zoomy);

        void onScrollStarted(float scrollx, float scrolly);
    }

    // zooming
    float zoom = 1.0f;
    float maxZoom = 2.0f;
    float minZoom = 0.5f;
    float smoothZoom = 1.0f;
    float zoomX, zoomY;
    float smoothZoomX, smoothZoomY;
    private boolean scrolling;

    // touching variables
    private long lastTapTime;
    private float touchStartX, touchStartY;
    private float touchLastX, touchLastY;
    private float initial_displacement;
    private boolean pinching;
    private float last_displacement;
    private float last_f1_dx, last_f1_dy;
    private float last_f2_dx, last_f2_dy;
    private int contentWidth, contentHeight;
    private boolean zooming;


    // drawing
    private final Matrix zMatrix = new Matrix();
    private final Paint zPaint = new Paint();

    // listener
    ZoomViewListener listener;

    private Bitmap zCache;

    public ZoomView(final Context context) {
        super(context);
        init();
    }

    public ZoomView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init();
    }

    public ZoomView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom)
    {
        super.onLayout(changed, left, top, right, bottom);

//        View v = getChildAt(0);
//        contentWidth = v.getWidth();
//        contentHeight = v.getHeight();

//        Log.i(LOG_TAG, "ContentView - Width : " + contentWidth + " -- Height : " + contentHeight);
    }

    private void init()
    {

    }

    public float getZoom() {
        return zoom;
    }

    public float getMaxZoom() {
        return maxZoom;
    }

    public int getContentWidth()
    {
        return contentWidth;
    }

    public int getContentHeight()
    {
        return contentHeight;
    }

    public void setContentSize(int width, int height)
    {
        contentWidth = width;
        contentHeight = height;
    }


    public void setMaxZoom(final float maxZoom) {
        if (maxZoom < 1.0f) {
            return;
        }

        this.maxZoom = maxZoom;
    }


    public void zoomTo(final float zoom, final float x, final float y) {
        this.zoom = Math.min(zoom, maxZoom);
        zoomX = x;
        zoomY = y;
        smoothZoomTo(this.zoom, x, y);
    }

    public ZoomViewListener getListener() {
        return listener;
    }

    public void setListner(final ZoomViewListener listener) {
        this.listener = listener;
    }

    public float getZoomFocusX() {
        return zoomX * zoom;
    }

    public float getZoomFocusY() {
        return zoomY * zoom;
    }

    @Override
    public boolean dispatchTouchEvent(final MotionEvent ev) {
        // single touch
        if (ev.getPointerCount() == 1) {
            processOneFingerTouchEvent(ev);
        }

        // // double touch
        if (ev.getPointerCount() == 2) {
            processTwoFingerTouchEvent(ev);
        }

        // redraw
        getRootView().invalidate();
        invalidate();

        return true;
    }

    private void processOneFingerTouchEvent(final MotionEvent mEvent) {
        final float x = mEvent.getX();
        final float y = mEvent.getY();
        float x_lenght = x - touchStartX;
        float y_lenght = y - touchStartY;
        final float effective_length = (float) Math.hypot(x_lenght, y_lenght);
        float x_displacement = x - touchLastX;
        float y_displacement = y - touchLastY;
        touchLastX = x;
        touchLastY = y;

        switch (mEvent.getAction()) {
        case MotionEvent.ACTION_DOWN:
            //Store the touch position on Start
            touchStartX = x;
            touchStartY = y;
            touchLastX = x;
            touchLastY = y;
            x_displacement = 0;
            y_displacement = 0;
            x_lenght = 0;
            y_lenght = 0;
            scrolling = false;
            break;

        case MotionEvent.ACTION_MOVE:
//            if (scrolling || (smoothZoom > 1.0f && l > 30.0f)) {
            if (scrolling || (effective_length > 30.0f)) {
                if (!scrolling) {
                    scrolling = true;
                    mEvent.setAction(MotionEvent.ACTION_CANCEL);
                    super.dispatchTouchEvent(mEvent);
                }
                smoothZoomX -= x_displacement / zoom;
                smoothZoomY -= y_displacement / zoom;

//                Log.i(LOG_TAG, "Zoom : " + smoothZoom + "-- X : " + smoothZoomX + " -- Y : " + smoothZoomY);
                return;
            }
            break;

        case MotionEvent.ACTION_OUTSIDE:
        case MotionEvent.ACTION_UP:

            // tap
            if (effective_length < 30.0f) {
                // check double tap
                if (System.currentTimeMillis() - lastTapTime < 500) {
                    if (smoothZoom == 1.0f) {
                        smoothZoomTo(maxZoom, x, y);
                    } else {
                        smoothZoomTo(1.0f, contentWidth / 2.0f, contentHeight / 2.0f);
                    }
                    lastTapTime = 0;
                    mEvent.setAction(MotionEvent.ACTION_CANCEL);
                    super.dispatchTouchEvent(mEvent);
                    return;
                }

                lastTapTime = System.currentTimeMillis();

                performClick();
            }
            break;

        default:
            break;
        }

        mEvent.setLocation(zoomX + ((x - 0.5f * contentWidth) / zoom), zoomY + ((y - 0.5f * contentHeight) / zoom));

//        mEvent.getX();
//        mEvent.getY();

        super.dispatchTouchEvent(mEvent);
    }

    /**
     * Process Two Finger Pinching
     * @param ev MotionEvent
     */
    private void processTwoFingerTouchEvent(final MotionEvent ev) {

        final float f1_x = ev.getX(0);
        final float f1_dx = f1_x - last_f1_dx;
        last_f1_dx = f1_x;
        final float f1_y = ev.getY(0);
        final float f1_dy = f1_y - last_f1_dy;
        last_f1_dy = f1_y;
        final float f2_x = ev.getX(1);
        final float f2_dx = f2_x - last_f2_dx;
        last_f2_dx = f2_x;
        final float f2_y = ev.getY(1);
        final float f2_dy = f2_y - last_f2_dy;
        last_f2_dy = f2_y;

        // pointers distance
        final float f_spacing = (float) Math.hypot((f2_x -f1_x), (f2_y -f1_y)); //Finger Spacing is the Hypotenuse
        final float f_displacement = f_spacing - last_displacement;
        last_displacement = f_spacing;
        final float abs_displacement = Math.abs(f_spacing - initial_displacement);

        switch (ev.getAction()) {
        case MotionEvent.ACTION_DOWN:
            initial_displacement = f_spacing; //Store the initial spacing on Touch Down
            pinching = false;
            break;

        case MotionEvent.ACTION_MOVE:
            if (pinching || abs_displacement > 30.0f) {
                pinching = true;
                final float dxk = 0.5f * (f1_dx + f2_dx); //Pinch Center X
                final float dyk = 0.5f * (f1_dy + f2_dy); //Pinch Center Y
                smoothZoomTo(
                        Math.max(1.0f, ((zoom *f_spacing) /(f_spacing -f_displacement))), //Minimum 1x Zoom
                        (zoomX -(dxk /zoom)),
                        (zoomY -(dyk /zoom))
                );
            }

            break;

        case MotionEvent.ACTION_UP:
        default:
            pinching = false;
            break;
        }

        ev.setAction(MotionEvent.ACTION_CANCEL);
        super.dispatchTouchEvent(ev);
    }

    /**
     * Set Smooth Zoom parameters
     * @param zoom Zoom scale
     * @param x X Position
     * @param y Y Position
     */
    public void smoothZoomTo(final float zoom, final float x, final float y) {
        smoothZoom = clamp(1.0f, zoom, maxZoom);
        smoothZoomX = x;
        smoothZoomY = y;
        if (listener != null) {
            listener.onZoomStarted(smoothZoom, x, y);
        }
    }

    /**
     * Crops the value between min and max
     * @param min
     * @param value
     * @param max
     * @return
     */
    private float clamp(final float min, final float value, final float max) {
        return Math.max(min, Math.min(value, max));
    }

    private float lerp(final float a, final float b, final float k) {
        return a + ((b - a) * k);
    }

    /**
     *
     * @param a
     * @param b
     * @param k
     * @return
     */
    private float bias(final float a, final float b, final float k)
    {
        float bias = (Math.abs(b - a) >= k) ? (a + k * Math.signum(b - a)) : b;
//        Log.i(LOG_TAG, "Bias : " + bias);
        return bias;
    }

    @Override
    protected void dispatchDraw(final Canvas canvas) {
        // do zoom
        zoom = lerp(bias(zoom, smoothZoom, 0.05f), smoothZoom, 0.2f);
        //FIXME : Implement proper Clamping
        float minSZ_x = ((0.5f *contentWidth) /smoothZoom);
        float maxSZ_x = (contentWidth -((0.5f *getWidth()) /smoothZoom));
        float minSZ_y = ((0.5f *contentHeight) /smoothZoom);
        float maxSZ_y = (contentHeight -((0.5f *getHeight()) /smoothZoom));
//        Log.i(LOG_TAG, "SZ - miW : " + minSZ_x + " - maW : " + maxSZ_x + " - miH : " + minSZ_y + " - maH : " + maxSZ_y);

        smoothZoomX = clamp(minSZ_x, smoothZoomX, maxSZ_x);
        smoothZoomY = clamp(minSZ_y, smoothZoomY, maxSZ_y);

        zoomX = lerp(bias(zoomX, smoothZoomX, 0.1f), smoothZoomX, 0.35f);
        zoomY = lerp(bias(zoomY, smoothZoomY, 0.1f), smoothZoomY, 0.35f);
        if (zoom != smoothZoom && listener != null) {
            zooming = true;
            listener.onZooming(zoom, zoomX, zoomY);
        } else if (zooming && listener != null) {
            zooming = false;
            listener.onZoomEnded(zoom, zoomX, zoomY);
        }

        final boolean animating = Math.abs(zoom - smoothZoom) > 0.0000001f
                || Math.abs(zoomX - smoothZoomX) > 0.0000001f || Math.abs(zoomY - smoothZoomY) > 0.0000001f;
        // nothing to draw
        if (getChildCount() == 0) {
            return;
        }

        // prepare matrix
        zMatrix.setTranslate(0.5f * contentWidth, 0.5f * contentHeight);
        zMatrix.preScale(zoom, zoom);

        //FIXME : Implement proper Clamping
        float minZWidth = ((0.5f *contentWidth) /zoom);
        float maxZWidth = (contentWidth -((0.5f *getWidth()) /zoom));
        float minZHeight = ((0.5f *contentHeight) /zoom);
        float maxZHeight = (contentHeight -((0.5f *getHeight()) /zoom));
//        Log.i(LOG_TAG, "Z - miW : " + minZWidth + " - maW : " + maxZWidth + " - miH : " + minZHeight + " - maH : " + maxZHeight);

        zMatrix.preTranslate(-clamp(minZWidth, zoomX, maxZWidth),
                -clamp(minZHeight, zoomY, maxZHeight));
//        zMatrix.preTranslate(-zoomX, -zoomY);

        // get view
        final View v = getChildAt(0);
        zMatrix.preTranslate(v.getLeft(), v.getTop());

        // get drawing cache if available
        if (animating && zCache == null && isAnimationCacheEnabled()) {
            v.setDrawingCacheEnabled(true);
            zCache = v.getDrawingCache();
        }

        // draw using cache while animating
        if (animating && isAnimationCacheEnabled() && zCache != null) {
            zPaint.setColor(0xffffffff);
            canvas.drawBitmap(zCache, zMatrix, zPaint);
        } else { // zoomed or cache unavailable
            zCache = null;
            canvas.save();
            canvas.concat(zMatrix);
            v.draw(canvas);
            canvas.restore();
        }

        // redraw
        // if (animating) {
        getRootView().invalidate();
        invalidate();
        // }
    }
}
