package com.odm.setupwizardoverlay.poa;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.Nullable;

import com.odm.setupwizardoverlay.Utils;

public abstract class PoaCommon extends Activity {
    private static final String TAG = PoaCommon.class.getSimpleName();
    protected static final boolean DEBUG = Utils.DEBUG;
    public static final String PHONE_ACTIVATED = "phone_activated";
    public static final String AGREE_TERMS_CONTIDIONS = "agree_terms_contidions";
    public static final String ACTIVATED_PHONE_NUMBER = "activated_phone_number";

    private boolean mHidden;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getLayoutResId() != 0) {
            setContentView(getLayoutResId());
        }
        initView();
        initAction();
    }

    protected abstract int getLayoutResId();

    public abstract String getTitleString();

    protected abstract void initView();

    protected abstract void initAction();

    public void registerBroadcastReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        getApplicationContext().registerReceiver(receiver, filter);
    }

    public void unregisterBroadcastReceiver(BroadcastReceiver receiver) {
        getApplicationContext().unregisterReceiver(receiver);
    }

    public static void provision(Activity activity) {
        // Add a persistent setting to allow other apps to know the device has been provisioned.
        ContentResolver resolver = activity.getContentResolver();
        Settings.Global.putInt(resolver, Settings.Global.DEVICE_PROVISIONED, 1);
        Settings.Secure.putInt(resolver, Settings.Secure.USER_SETUP_COMPLETE, 1);
        Settings.Secure.putInt(resolver, Settings.Secure.TV_USER_SETUP_COMPLETE, 1);
    }

}
