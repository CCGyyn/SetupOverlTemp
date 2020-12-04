package com.odm.setupwizardoverlay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.Arrays;


public class PcoDataObserver {
    private static final String TAG = PcoDataObserver.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static Context mContext;

    private static final int OEM_NAME_LENGTH = 8;
    private static final int OEM_REQUEST_ID_LEN = 4;
    private static final int OEM_REQUEST_DATA_LEN = 4;

    private static final String HOOK_OEM_NAME = "QOEMHOOK";
    private static final int EVT_HOOK_UNSOL_OPERATOR_RESERVED_PCO = 0x80425;  //QCRIL_EVT_HOOK_UNSOL_OPERATOR_RESERVED_PCO
    private static final int APP_SPECIFIC_INFO = 255;

    private static final String ACTION_PCO_CHANGE = "com.odm.setupwizardoverlay.PCO_CHANGE";
    private static final String ACTION_SEND_PCO_DATA = "com.android.settings.pcodata.SEND_PCO_DATA";
    private static final String ACTION_TELEPHONY_PCO_CHANGE = "com.android.internal.telephony.ACTION_PCO_CHANGE";
    public static final String KEY_PCO_DATA = "pco_data";

    private static MyPhoneStateListener mPhoneStateListener;
    private static volatile boolean isListening = false;
    private static PcoDataReceiver sPcoDataReceiver;

    public static void startListen(Context context) {
        if (isListening){
            if (DEBUG) Log.e(TAG, "PcoDataObserver is listening..");
            return;
        }
        if (context == null) {
            throw new NullPointerException(TAG+" context is null");
        }

        isListening = true;

        mContext = context.getApplicationContext();
        if (mPhoneStateListener == null) mPhoneStateListener = new MyPhoneStateListener(mContext);
        TelephonyManager.from(mContext).listen(mPhoneStateListener, PhoneStateListener.LISTEN_OEM_HOOK_RAW_EVENT);

        if (sPcoDataReceiver == null)  sPcoDataReceiver = new PcoDataReceiver();
        IntentFilter filter = new IntentFilter(TelephonyManager.ACTION_CARRIER_SIGNAL_PCO_VALUE);
        filter.addAction(Intent.ACTION_BOOT_COMPLETED);
        filter.addAction(ACTION_TELEPHONY_PCO_CHANGE);
        mContext.registerReceiver(sPcoDataReceiver, filter);
        if (DEBUG) Log.e(TAG, "registerReceiver PcoDataReceiver");
        if (DEBUG) Log.e(TAG, "startListen");
    }

    public static void stopListen() {
        if (!isListening) {
            if (DEBUG) Log.e(TAG, "shoule call startListen first");
            return;
        }

        if (DEBUG) Log.e(TAG, "stopListen");
        isListening = false;
        if (mPhoneStateListener != null && mContext != null) {
            TelephonyManager.from(mContext).listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        }

        if (sPcoDataReceiver != null && mContext != null) {
            mContext.unregisterReceiver(sPcoDataReceiver);
        }
        mContext = null;
        mPhoneStateListener = null;
        sPcoDataReceiver = null;
    }

    private static class PcoDataReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DEBUG) {
                Log.e(TAG, "onReceive " + intent);
                Log.e(TAG, "onReceive " + action);
            }
            
            if (action != null) {
                switch (action) {
                    case TelephonyManager.ACTION_CARRIER_SIGNAL_PCO_VALUE: {
                        String apn = intent.getStringExtra(TelephonyManager.EXTRA_APN_TYPE);
                        String bearerProto = intent.getStringExtra(TelephonyManager.EXTRA_APN_PROTOCOL);
                        int pcoId = intent.getIntExtra(TelephonyManager.EXTRA_PCO_ID, -1);
                        byte[] contents = intent.getByteArrayExtra(TelephonyManager.EXTRA_PCO_VALUE);

                        if (DEBUG)
                            Log.e(TAG, "onReceive apn=" + apn + " ,bearerProto=" + bearerProto +
                                    " ,pcoId=" + pcoId + " ,contents=" + Arrays.toString(contents));

                        if (DEBUG) Log.e(TAG, "onReceive contents=" + contents);

                        if (contents != null && contents.length > 0) {
                            int pco = contents[0];
                            Log.d(TAG, "onReceive pco=" + pco);
                            if (pco != -1) {
                                Intent pcoIntent = new Intent(ACTION_PCO_CHANGE);
                                pcoIntent.putExtra(KEY_PCO_DATA, pco);
                                pcoIntent.setFlags(Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND); //qinyu add for background app
                                context.sendStickyBroadcast(pcoIntent);
                                Log.e(TAG, "sendStickyBroadcast : " + pco);

                            }
                        }
                    }
                    break;

                    case ACTION_TELEPHONY_PCO_CHANGE:{
                        int pco = intent.getIntExtra(KEY_PCO_DATA, -1);
                        Log.e(TAG, "action=" + action + ",pco=" + pco);

                    }
                    break;

                }
            }


        }
    };

    private static class MyPhoneStateListener extends PhoneStateListener {
        private Context context;

        MyPhoneStateListener(Context context) {
            this.context = context;
        }

        @Override
        public void onOemHookRawEvent(byte[] rawData) {
            if (DEBUG) Log.e(TAG, "Receive Oem Hook Raw Event");

            int pcoData = parseOemHookRawEventData_PcoData(rawData);
            if (DEBUG) Log.e(TAG, "pcoData=" + pcoData);
            if(pcoData != -1) {
                Intent intent = new Intent(ACTION_PCO_CHANGE);
                intent.putExtra(KEY_PCO_DATA, pcoData);
                intent.setFlags(Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND); //qinyu add for background app
                context.sendStickyBroadcast(intent);
                Log.e(TAG, "sendStickyBroadcast : " + pcoData);
            }
        }
    };


    private static int parseOemHookRawEventData_PcoData(byte[] rawData) {
        String oem_name = "";
        int unsol_event_id = 0;
        int index = 0;
        int pos = 0;
        int data_len = 0;

        //get oem name
        for (index = 0; index < OEM_NAME_LENGTH; index++) {
            oem_name += (char) (rawData[index]);
        }
        pos = index;
        Log.e(TAG, "oem_name is " + oem_name);

        //get event it
        for (index = 0; index < OEM_REQUEST_ID_LEN; index++) {
            unsol_event_id |= (int) ((rawData[index + pos] & 0xff) << (8 * index));
        }
        Log.e(TAG, "unsol_event_id is " + unsol_event_id);
        if (unsol_event_id != EVT_HOOK_UNSOL_OPERATOR_RESERVED_PCO || !oem_name.equals(HOOK_OEM_NAME)) {
            return -1;
        }
        pos += index;

        //get length of operator_reserved_pco
        for (index = 0; index < OEM_REQUEST_DATA_LEN; index++) {
            data_len |= (int) ((rawData[index + pos] & 0xff) << (8 * index));
        }
        pos += index;
        Log.e(TAG, "data_len is " + data_len);

        //get operator_reserved_pco data
        //get mcc and mnc
        int mcc = (rawData[pos] & 0xff) | ((rawData[pos + 1] & 0xff) << 8);
        pos += 2;
        int mnc = (rawData[pos] & 0xff) | ((rawData[pos + 1] & 0xff) << 8);
        pos += 2;
        //get mnc_includes_pcs_digit
        int mnc_includes_pcs_digit = (rawData[pos] & 0xff) | ((rawData[pos + 1] & 0xff) << 8) |
                ((rawData[pos + 1] & 0xff) << 16) | ((rawData[pos + 1] & 0xff) << 24);
        pos += 4;
        //get app_specific_info_len
        int app_specific_info_len = (rawData[pos] & 0xff) | ((rawData[pos + 1] & 0xff) << 8) |
                ((rawData[pos + 1] & 0xff) << 16) | ((rawData[pos + 1] & 0xff) << 24);
        pos += 4;
        Log.e(TAG, "mcc is " + mcc + ", mnc is " + mnc + ", mnc_includes_pcs_digit is " + mnc_includes_pcs_digit
                + ", app_specific_info_len is " + app_specific_info_len);

        //get app_specific_info
        int app_specific_info = rawData[pos] & 0xff;
        pos += ((int) (APP_SPECIFIC_INFO / 4) + 1) * 4;
        int container_id = (rawData[pos] & 0xff) | ((rawData[pos + 1] & 0xff) << 8) |
                ((rawData[pos + 2] & 0xff) << 16) | ((rawData[pos + 3] & 0xff) << 24);

        Log.e(TAG, "app_specific_info is " + app_specific_info + ", container_id is " + container_id);

        pos = 0;

        Log.e(TAG, "app_specific_info = " + app_specific_info);

        return app_specific_info;
    }

}
