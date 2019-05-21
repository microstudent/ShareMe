package com.leaves.app.shareme;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

import com.alibaba.android.arouter.launcher.ARouter;
import com.tencent.bugly.crashreport.CrashReport;

import io.realm.Realm;

/**
 * Created by leaves on 17-1-23.
 */
public class SApplication extends Application {
    @SuppressLint("StaticFieldLeak")
    private static Context sContext;

    public SApplication() {
    }


    @Override
    public void onCreate() {
        super.onCreate();
        sContext = this;
        Realm.init(this);
        if (BuildConfig.DEBUG) {           // These two lines must be written before init, otherwise these configurations will be invalid in the init process
            ARouter.openLog();     // Print log
            ARouter.openDebug();   // Turn on debugging mode (If you are running in InstantRun mode, you must turn on debug mode! Online version needs to be closed, otherwise there is a security risk)
        }
        ARouter.init(this); // As early as possible, it is recommended to initialize in the Application
        CrashReport.initCrashReport(getApplicationContext(), "883f8226b9", !BuildConfig.DEBUG);
    }

    public static Context getContext() {
        return sContext;
    }

}
