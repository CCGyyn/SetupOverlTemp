package com.odm.setupwizardoverlay.poa;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;

import com.odm.setupwizardoverlay.Utils;

public abstract class PoaCommon extends Activity {
    private static final String TAG = PoaCommon.class.getSimpleName();
    protected static final boolean DEBUG = Utils.DEBUG;
    public static final String ARGS = "args";
    public static final String START_FROM_NOTIFICATION = "start_from_notification";
    public static final String PHONE_ACTIVATED = "phone_activated";
    public static final String AGREE_TERMS_CONTIDIONS = "agree_terms_contidions";
    public static final String ACTIVATED_PHONE_NUMBER = "activated_phone_number";

    private boolean mHidden;

    private final Handler mInternalHandler = new Handler(new HC());


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getLayoutResId() != 0) {
            setContentView(getLayoutResId());
        }
        handleIntent(getIntent());
        initView();
        initAction();
    }

    protected abstract int getLayoutResId();

    public abstract String getTitleString();

    protected abstract void initView();

    protected abstract void initAction();

    public void registerBroadcastReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        registerReceiver(receiver, filter);
    }

    public void unregisterBroadcastReceiver(BroadcastReceiver receiver) {
        unregisterReceiver(receiver);
    }

    public static void provision(Activity activity) {
        // Add a persistent setting to allow other apps to know the device has been provisioned.
        ContentResolver resolver = activity.getContentResolver();
        Settings.Global.putInt(resolver, Settings.Global.DEVICE_PROVISIONED, 1);
        Settings.Secure.putInt(resolver, Settings.Secure.USER_SETUP_COMPLETE, 1);
        Settings.Secure.putInt(resolver, Settings.Secure.TV_USER_SETUP_COMPLETE, 1);
    }

    class HC implements Handler.Callback {

        @Override
        public boolean handleMessage(Message msg) {
            return PoaCommon.this.handleMessage(msg);
        }

    }
    public boolean handleMessage(Message msg){
        return false;
    }

    public final boolean sendMessage(Message msg){
        return getInternalHandler().sendMessage(msg);
    }

    public final boolean sendMessageDelayed(Message msg, long delayMillis){
        return getInternalHandler().sendMessageDelayed(msg, delayMillis);
    }

    public final boolean sendEmptyMessage(int what){
        return getInternalHandler().sendEmptyMessage(what);
    }

    public void post(Runnable r){
        getInternalHandler().post(r);
    }

    public void postDelayed(Runnable r,long delayMillis){
        getInternalHandler().postDelayed(r,delayMillis);
    }

    public void removeCallbacksAndMessages(Object token) {
        getInternalHandler().removeCallbacksAndMessages(token);
    }

    public void removeCallbacks(Runnable r) {
        getInternalHandler().removeCallbacks(r);
    }

    public void removeMessages(int what) {
        getInternalHandler().removeMessages(what);
    }


    protected Handler getInternalHandler() {
        return mInternalHandler;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        clearIfNeeded();
        if (mInternalHandler != null) {
            if (DEBUG) Log.d(TAG, "onDestroy removeCallbacksAndMessages");
            mInternalHandler.removeCallbacksAndMessages(null);
        }
    }

    protected void clearIfNeeded() {
        if (DEBUG) {
            Log.d(TAG, "clear before page changes");
        }
        removeCallbacksAndMessages(null);
    }

    protected void startActivityPanel(Intent intent) {
        clearIfNeeded();
        startActivity(intent);
    }

    private boolean isCallerNotification = false;

    public boolean isCallerNotification() {
        return isCallerNotification;
    }

    private void handleIntent(Intent intent) {
        if (intent == null) {
//            finishIfNeeded(true);
            return;
        }

        // handle notification req
        isCallerNotification = intent.getBooleanExtra(START_FROM_NOTIFICATION, false);
        if (DEBUG) Log.d(TAG, "isNotiReq=" + isCallerNotification);

    }
}
