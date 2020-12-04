package com.odm.setupwizardoverlay;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Process;
import android.util.Log;

public class SetupWizardApplication extends Application {
    private static final String TAG = SetupWizardApplication.class.getSimpleName();
	public static final String RESTART_PCO_DATA_LISTEN_SERVICE = "com.android.settings.pcodata.RESTART_PCO_DATA_LISTEN_SERVICE";

    @Override
    public void onCreate() {
        super.onCreate();

        String packageName = getPackageName();
        Log.d(TAG, "onCreate " + packageName);

        if (getCurProcessName().equals(packageName)) {
            Log.d(TAG, "start pco data observer");

            //START_PCO_DATA_LISTEN_SERVICE
			/*Intent intent = new Intent(RESTART_PCO_DATA_LISTEN_SERVICE);
			intent.setFlags(Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND); //qinyu add for background app
			getApplicationContext().sendBroadcast(intent); */

			//start pco data observer
			PcoDataObserver.startListen(this);
            preInitWifi();

        }
    }

    private void preInitWifi() {
        // pre init wifi
        Intent intent = new Intent("com.odm.setupwizardoverlay.ACTION_WIFI_PRE_INIT");
        intent.setPackage("com.android.settings");
        intent.addFlags(Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND);
        sendBroadcast(intent);
        if (Utils.DEBUG) Log.d(TAG, "send broadcast ACTION_WIFI_PRE_INIT");
    }

    private String getCurProcessName() {
        int pid = Process.myPid();
        ActivityManager activityManager = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo appProcess : activityManager.getRunningAppProcesses()) {
            if (appProcess.pid == pid) {
                return appProcess.processName;
            }
        }
        return "";
    }
}
