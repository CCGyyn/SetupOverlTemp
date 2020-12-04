package com.odm.setupwizardoverlay;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Created by pecuyu on 20-3-3.
 */

public class Utils {
    public static final boolean DEBUG = true;
    public static final String TAG = Utils.class.getSimpleName();

    public static void closeStreamQuieltly(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String parseJsonFromInputStream(InputStream is) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(is));
            String line = null;
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            Utils.closeStreamQuieltly(br);
        }

        return null;
    }

    public static Resources getResourcesForApplication(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        Resources resources = null;
        try {
            resources = pm.getResourcesForApplication(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return resources;
    }

    /**
     * 判断是否包含SIM卡
     *
     * @return 状态
     */
    public static boolean hasSimCard(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);
        boolean result = true;
        int simState = -1;
        if (telephonyManager != null) {
            simState = telephonyManager.getSimState();
            switch (simState) {
                case TelephonyManager.SIM_STATE_ABSENT: // 没有SIM卡
                case TelephonyManager.SIM_STATE_UNKNOWN: // 未知，可能处于状态转换
                    result = false;
                    break;
            }
        } else {
            result = false;
        }
        if (DEBUG) Log.d(TAG, result ? "sim card is inserted" : "no sim card found , simState="+simState);
        return result;
    }

    /**
     * enable airplane mode or not
     * @param context
     * @param enable
     */
    public static void enableAirplaneMode(Context context, boolean enable) {
        // Change the system setting
        Settings.Global.putInt(context.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON,
                enable ? 1 : 0);

        // Post the intent
        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.setFlags(Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND); //qinyu add for background app
        intent.putExtra("state", enable);
        context.sendBroadcast(intent);
    }

    // set the Margins of the view
    // this view should be a child of the parent,as this must be called after addView
    public static void setViewLayoutMargins(View view, int left, int top, int right, int bottom) {
        if (view == null) {
            return;
        }

        ViewGroup.LayoutParams params = view.getLayoutParams();
        ViewGroup.MarginLayoutParams marginParams = null;
        //获取view的margin设置参数
        if (params instanceof ViewGroup.MarginLayoutParams) {
            marginParams = (ViewGroup.MarginLayoutParams) params;
        } else {//不存在时创建一个新的参数
            marginParams = new ViewGroup.MarginLayoutParams(params);
        }
        //设置margin
        marginParams.setMargins(left, top, right, bottom);
        view.setLayoutParams(marginParams);
    }

    // words Of the specific String
    public static int wordsOfString(String content) {
        if (TextUtils.isEmpty(content)) {
            return 0;
        }

        String[] parts = content.split(" ");
        return parts == null ? 0 : parts.length;
    }

    public static boolean checkSpecificPackageAvailable(Context context,String packageName) {
        PackageInfo packageInfo = null;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            packageInfo = null;
        }

        return packageInfo != null;
    }

    public static String getLastFactoryResetDate() {
        String date = "";
        File file = new File(Constants.PATH_FDR_DATE_FILE);
        if (file.exists()) {
            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader(file));
                // read one line is enough, otherwise read all content.
                date = br.readLine();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                Utils.closeStreamQuieltly(br);
            }
        }

        if (Utils.DEBUG){
            Log.d(TAG, "read FDR date : " + date);
        }

        return date;
    }

    public static boolean isSimUnlocked(Context context) {
        int status = Settings.Global.getInt(context.getContentResolver(), Constants.VERIZON_SIM_UNLOCK_STATE, 1);
        boolean unlocked = status == 1 || status == 2;
        if (Utils.DEBUG){
            Log.d(TAG, "isSimUnlocked : " + unlocked + " ,status=" + status);
        }
        return unlocked;
    }

    public static boolean isValidMdn(String mdn) {
        return !TextUtils.isEmpty(mdn) && !mdn.startsWith("00000");
    }

    // get phone activation status from secure setting
    public static boolean isPhoneActivatedSuccess(Context context) {
        // get phone activation status
        String activationStatus = Settings.Secure.getString(context.getContentResolver(), Constants.KEY_ACTIVATION_STATUS);
        if (Utils.DEBUG) Log.d(TAG, "activationStatus=" + activationStatus);
        if (!TextUtils.isEmpty(activationStatus)) {
            String[] datas = activationStatus.split(":");
            if (datas != null && datas.length == 2) { // two parts, [mdn,pco]
                String mdn = datas[0];
                int pco = Integer.parseInt(datas[1]);
                if (Utils.DEBUG) Log.d(TAG, "isPhoneActivatedSuccess mdn=" + mdn + " ,pco=" + pco);
                return isValidMdn(mdn) && pco == 0;
            }
        }

        return false;
    }

    public static String getMDN(Context context) {
        SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
        String mdn = null;
        if (subscriptionManager != null) {
            List<SubscriptionInfo> subscriptionInfos = subscriptionManager.getActiveSubscriptionInfoList();

            if (subscriptionInfos != null && subscriptionInfos.size() > 0) {
                SubscriptionInfo selectableSubInfo = subscriptionInfos.get(0);
                if (selectableSubInfo != null) {
                    mdn = selectableSubInfo.getNumber();
                    //if (DEBUG) Log.d(TAG, "mdn: " + mdn);
                }
            }
        }

        return mdn;
    }


}
