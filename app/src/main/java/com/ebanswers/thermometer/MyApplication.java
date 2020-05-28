package com.ebanswers.thermometer;

import android.app.Application;

import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechUtility;
import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.Logger;
import com.tencent.bugly.crashreport.CrashReport;

public class MyApplication extends Application {
    public static MyApplication INSTANCE;
    @Override
    public void onCreate() {
        super.onCreate();
        INSTANCE = this;
        Logger.addLogAdapter(new AndroidLogAdapter());
        //bugly配置
        CrashReport.initCrashReport(getApplicationContext(), "70c3427e4d", false);
        SpeechUtility.createUtility(this, SpeechConstant.APPID + "=5ec5e6f6");
    }

    public static MyApplication getInstance() {
        return INSTANCE;
    }
}
