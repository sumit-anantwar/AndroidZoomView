package com.sumitanantwar.android_zoom_view.sample;

import android.app.Application;
import android.content.Context;

/**
 * Created by Sumit Anantwar on 4/16/17.
 */

public class App extends Application
{
    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
    }

    public static Context getContext()
    {
        return mContext;
    }
}
