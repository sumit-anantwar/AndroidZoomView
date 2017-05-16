package com.sumitanantwar.android_zoom_view.sample;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.sumitanantwar.android_zoom_view.ZoomView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements ZoomView.ZoomViewListener {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    @BindView(R.id.MA_MainContainer) FrameLayout mainContainer;

    private ZoomView zoomView;
    private RelativeLayout zoomContainer;
    private ImageView mapIV;
    private float scaleFactor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        zoomView = new ZoomView(this);
        zoomView.setListner(this);

        zoomContainer = new RelativeLayout(this);
        zoomContainer.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        zoomView.addView(zoomContainer);

        mainContainer.addView(zoomView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        updateUI();
    }

    private void updateUI()
    {
        File fileMap = new File(getCacheDir()+"/tube_map.png");
        if (!fileMap.exists()) try {

            InputStream is = getAssets().open("tube_map.png");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            FileOutputStream fos = new FileOutputStream(fileMap);
            fos.write(buffer);
            fos.close();
        } catch (Exception e) { throw new RuntimeException(e); }



        /* Read only the Map Image Specs without loading it */
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true; // Read only the specs
        BitmapFactory.decodeFile(fileMap.toString(), options);

        Log.i(LOG_TAG, "Map Size - Original -- Width : " + options.outWidth + " -- Height : " + options.outHeight);

        int mapWidth = options.outWidth;
        int mapHeight = options.outHeight;
        // Compute Scale Factor to fit the Map Image in the screen
        scaleFactor = 1.0f;

        /* Compute the Scaled Width and Height */
        int scaledWidth = (int) (mapWidth * scaleFactor);
        int scaledHeight = (int) (mapHeight * scaleFactor);

        float dpSW = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, scaledWidth, getResources().getDisplayMetrics());
        float dpSH = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, scaledHeight, getResources().getDisplayMetrics());


        /* Removing mapIV and re adding it
        * to solve the image caching effect on the ZoomView */
        if (mapIV != null) zoomContainer.removeView(mapIV);

        /* Reset the Options */
        options.inJustDecodeBounds = false;
        options.inSampleSize = ((options.outWidth / mapWidth) > 2) ? 2 : 1; // This will down-sample the image, i.e reduce the DPI
        Bitmap mapImage = BitmapFactory.decodeFile(fileMap.toString(), options);

        /* Create Map Image View */
        mapIV = new ImageView(this);
//          mapIV.setBackgroundColor(Color.BLUE);
        mapIV.setScaleType(ImageView.ScaleType.FIT_XY);
        mapIV.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mapIV.setImageBitmap(mapImage);

        RelativeLayout.LayoutParams mapIVParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mapIVParams.leftMargin = 0;
        mapIVParams.topMargin = 0;

        zoomContainer.addView(mapIV, mapIVParams);

        FrameLayout.LayoutParams zoomContainerParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        zoomContainer.setLayoutParams(zoomContainerParams);

        zoomView.setContentSize(scaledWidth, scaledHeight);
    }

    // ZoomView Listeners
    @Override
    public void onZoomStarted(float zoom, float zoomx, float zoomy) {

    }

    @Override
    public void onZooming(float zoom, float zoomx, float zoomy) {

    }

    @Override
    public void onZoomEnded(float zoom, float zoomx, float zoomy) {

    }

    @Override
    public void onScrollStarted(float scrollx, float scrolly) {

    }
}
