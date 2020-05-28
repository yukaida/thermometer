package com.ebanswers.thermometer;

import android.app.Application;

public class MyApplication extends Application {
    public static MyApplication INSTANCE;
    @Override
    public void onCreate() {
        super.onCreate();
        INSTANCE = this;
    }

    public static MyApplication getInstance() {
        return INSTANCE;
    }
}
