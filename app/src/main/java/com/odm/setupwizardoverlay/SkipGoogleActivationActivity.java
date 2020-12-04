package com.odm.setupwizardoverlay;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class SkipGoogleActivationActivity  extends Activity {
    public static final String TAG = SkipGoogleActivationActivity.class.getSimpleName();

    protected void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);
        Log.d(TAG, "onCreate for SkipGoogleActivationActivity");
        finish();
    }
}