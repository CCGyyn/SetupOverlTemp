package com.odm.setupwizardoverlay;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManager;

import com.android.setupwizardlib.util.WizardManagerHelper;

import com.odm.setupwizardoverlay.poa.LookUpOrderRequest;
import com.odm.setupwizardoverlay.poa.PoaCommon;
import com.odm.setupwizardoverlay.poa.PoaConfig;
import com.odm.setupwizardoverlay.poa.VzwActivationService;
import com.odm.setupwizardoverlay.poa.VzwPoaRequest;
import com.qualcomm.qti.remoteSimlock.manager.RemoteSimlockManager;
import com.qualcomm.qti.remoteSimlock.manager.RemoteSimlockManagerCallback;
import com.android.internal.telephony.TelephonyIntents;

public class VzwSimCheckActivity extends Activity {
    private static final String TAG = VzwSimCheckActivity.class.getSimpleName();
    public static final int MIN_PAGE_WAIT_TIME = 5000;

    public static final String SIM_DESCRIPTION_ABSENT = "absent";
    public static final String SIM_DESCRIPTION_NOT_READY = "not_ready";
    public static final String SIM_DESCRIPTION_READY = "ready";
    public static final String SIM_DESCRIPTION_ERROR = "error";
    public static final int REQ_RETRY_MAX_TIMES = 5;
    public static final int MSG_RETRY_LOOKUP_ORDER_REQ = 100;

    private Context mContext;
    private TelephonyManager mTpManager;

    private String mMdn = null;
    private int mPco = Constants.PCO_DATA_INIT;
    public static final long TIMEOUT_LONG = 60 * 1000 * 5; // 5min
    public static final long TIMEOUT_LONG_SKIP = 60 * 1000 * 6; // 6min
    public static final int SIM_CHECK_DELAY_MILLIS = 30 * 1000; // 30s

    private boolean mIsReceiveSimState = false;
    private boolean mIsNewActivityStarted = false; // whether we started a new activity
    private volatile boolean mIsStoppedThread = false; // whether the work thread state is stopped
    private volatile boolean mIsNonVzwSim = false; // whether the inserted sim card is non vzw sim

    private PcoChangeReceiver mPcoChangeReceiver;
    private SimStateChangeReceiver mSimStateChangeReceiver;
    private PowerManager.WakeLock mWakeLock;
    private Thread mWorkerThread;
    private Thread mReadFDRThread;
    private String mFdrDate;
    private long mStartTimeMillis;

    public static final int MSG_ON_SIM_LOCK_SERVICE_CONNECTION_CHANGED = 1;
    public static final int MSG_RETRY_CONNECT_SIM_LOCK_SERVICE = 2;
    public static final int MSG_TRY_GET_SIM_LOCK_STATUS = 3;
    private RemoteSimlockManager mRemoteSimlockManager;
    private RemoteSimlockManagerCallback mSimlockManagerCallback;
    private Handler mSimLockCallbackHandler;
    private boolean mGetSimlockStatusResponsed = false;
    private Intent mPendingIntent;

    private boolean DEBUG = true;

    private String mSimDescription;
    private String mImsi;
    private String mImei;
    private LookUpOrderTask mLookupOrderTask;
    private int mReqRetry;

    private String mCorrelationID;
    private String mRequestID;
    private int mSecurityQID;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(TAG, "onActivityResult requestCode:" + requestCode + " resultCode:" + resultCode);

        if (requestCode == Constants.REQUEST_CODE_NEXT) {
            if (resultCode == Constants.RESULT_CODE_NEXT) {
                getNext();
            } else if (resultCode == Constants.RESULT_CODE_BACK) {
                onBackPressed();
            }
        } else if (requestCode == Constants.PROVISION_REQUEST_CODE) {
            if (data != null && data.getExtras() != null) {
                Log.d(TAG, "onActivityResult data: " + data.getExtras().toString());
            }
            getNext();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mStartTimeMillis = System.currentTimeMillis();
        //getWindow().setStatusBarColor(getResources().getColor(R.color.suw_color_accent_glif_dark));
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.sim_check);
        mContext = this;
        mTpManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        acquireWakeLock();

        mReqRetry = 0;

        /*Intent intentService = new Intent(mContext, VzwActivationService.class);
        startService(intentService);*/
        // register sim lock callback to get unlockStatus
        registerSimLockCallback(getApplicationContext());

        readFDRDate();
        mMdn = Utils.getMDN(mContext);
        registerReceivers();
	    // a long timeout msg in case no sim state change receives
        mHandler.sendEmptyMessageDelayed(Constants.ACTION_SKIP_DISPLAY, TIMEOUT_LONG_SKIP);
        // a delay action to check sim state in case no sim state change receives
        mHandler.postDelayed(() -> {
            if(mIsReceiveSimState) return; // if received sim state , no need to continue.
            if (Utils.hasSimCard(getApplicationContext())) {
                checkSimActivationState();
            } else {
                mHandler.sendEmptyMessage(Constants.ACTION_SHOW_NO_SIM);
            }
        }, SIM_CHECK_DELAY_MILLIS);
    }

    @SuppressLint("HandlerLeak")
    private void registerSimLockCallback(Context context) {
        if (mRemoteSimlockManager != null && mRemoteSimlockManager.isServiceConnected()) {
            Log.d(TAG, "registerSimLockCallback : already connected.");
            return;
        }

        if (mRemoteSimlockManager == null) {
            mRemoteSimlockManager = new RemoteSimlockManager(context);
        }

        if (mSimLockCallbackHandler == null) {
            mSimLockCallbackHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    switch (msg.what) {
                        case MSG_ON_SIM_LOCK_SERVICE_CONNECTION_CHANGED:
                            if (mRemoteSimlockManager == null) return;

                            boolean isServiceConnected = mRemoteSimlockManager.isServiceConnected();
                            Log.d(TAG, "RemoteSimLockService connection change : isServiceConnected=" + isServiceConnected);

                            if (isServiceConnected) {
                                if (mSimlockManagerCallback == null) {
                                    mSimlockManagerCallback = new RemoteSimlockManagerCallback() {
                                        public void uimRemoteSimlockGetSimlockStatusResponse(int token, int responseCode,
                                                                                             int unlockStatus, long unlockTime) {
                                            mGetSimlockStatusResponsed = true;
                                            Log.d(TAG, "getSimlockStatusResponse unlockStatus=" + unlockStatus + " ,responseCode=" + responseCode + " ,token=" + token);
                                            Settings.Global.putInt(context.getContentResolver(), Constants.VERIZON_SIM_UNLOCK_STATE, unlockStatus);
                                            if (mPendingIntent != null) {
                                                Log.d(TAG, "pending intent exists, starting..");
                                                int status = mPendingIntent.getIntExtra(Constants.KEY_SIM_STATUS,
                                                        Utils.hasSimCard(context) ? Constants.ACTION_SIM_READY : Constants.ACTION_SHOW_NO_SIM);
                                                startActivityMayWait(mPendingIntent,Constants.REQUEST_CODE_NEXT,status);
                                            }
                                        }
                                    };
                                }
                                mRemoteSimlockManager.registerCallback(mSimlockManagerCallback);

                                // try get lock status
                                sendEmptyMessage(MSG_TRY_GET_SIM_LOCK_STATUS);
                            }
                            break;
                        case MSG_RETRY_CONNECT_SIM_LOCK_SERVICE:
                            Log.d(TAG, "retry registerSimLockCallback");
                            registerSimLockCallback(getApplicationContext());
                            break;
                        case MSG_TRY_GET_SIM_LOCK_STATUS:
                            if (mRemoteSimlockManager == null || !mRemoteSimlockManager.isServiceConnected()) {
                                Log.e(TAG, "not connect RemoteSimLockService yet..");
                                if (hasMessages(MSG_RETRY_CONNECT_SIM_LOCK_SERVICE)) {
                                    removeMessages(MSG_RETRY_CONNECT_SIM_LOCK_SERVICE);
                                }
                                sendEmptyMessage(MSG_RETRY_CONNECT_SIM_LOCK_SERVICE);
                            } else {
                                int ret = mRemoteSimlockManager.uimRemoteSimlockGetSimlockStatus(10);
                                Log.d(TAG, "call uimRemoteSimlockGetSimlockStatus ret=" + ret);
                                if (ret == 1) {
                                    sendEmptyMessageDelayed(MSG_TRY_GET_SIM_LOCK_STATUS, 500);
                                }
                            }
                            break;
                        case MSG_RETRY_LOOKUP_ORDER_REQ:
                            Log.e(TAG, "retry lookup order req what=" + msg.what);

                            if (mPco == 0 || mPco == 5) {
                                lookUpOrder();
                            } else {
                                Log.e(TAG, "error : wrong pco value");
                            }
                            break;
                        case LookUpOrderRequest.MSG_PO_NEW_ORDER:
                        case LookUpOrderRequest.MSG_PO_UPGRADE_ORDER:
                            if (DEBUG) {
                                Log.e(TAG, "handlePendingOrderFound what=" + msg.what);
                            }
                            handlePendingOrderFound((LookUpOrderRequest) msg.obj);
                            break;
                        case LookUpOrderRequest.MSG_PO_NOT_FOUND:
                            if (DEBUG) {
                                Log.e(TAG, "handlePendingOrderNotFound what=" + msg.what);
                            }
                            handlePendingOrderNotFound((LookUpOrderRequest) msg.obj);
                            break;
                        case LookUpOrderRequest.MSG_PO_TIME_OUT:
                            if (DEBUG) {
                                Log.e(TAG, "handlePendingOrderLookupTimeout what=" + msg.what);
                            }
                            handlePendingOrderLookupTimeout((LookUpOrderRequest) msg.obj);
                            break;
                    }
                }
            };
        }

        if (!mRemoteSimlockManager.connectService(mSimLockCallbackHandler, MSG_ON_SIM_LOCK_SERVICE_CONNECTION_CHANGED)) {
            Log.e(TAG, "cannot bind RemoteSimLockService.., retry later");
            mSimLockCallbackHandler.sendEmptyMessageDelayed(MSG_RETRY_CONNECT_SIM_LOCK_SERVICE, 300);
        } else {
            Log.d(TAG, "success bind RemoteSimLockService..");
        }
    }


    private void deregisterSimLockCallback() {
        if (mRemoteSimlockManager != null) {
            Log.e(TAG, "deregisterSimLockCallback..");

            if (mSimlockManagerCallback != null) {
                mRemoteSimlockManager.deregisterCallback(mSimlockManagerCallback);
                mSimlockManagerCallback = null;
            }
            mRemoteSimlockManager.disconnectService();
        }
    }

    private void readFDRDate() {
        if (mReadFDRThread != null && mReadFDRThread.isAlive()) {
            Log.d(TAG, "readFDRDate : mReadFDRThread isAlive");
            return;
        }

        mReadFDRThread = new Thread() {
            @Override
            public void run() {
                super.run();
                mFdrDate = Utils.getLastFactoryResetDate();
                Log.d(TAG, "mFdrDate=" + mFdrDate);
                runOnUiThread(() -> {
                    sendBroadcastToMVS(mFdrDate);
                });
            }
        };
        mReadFDRThread.start();
    }

    private void sendBroadcastToMVS(String fdrDate) {
        Intent intent = new Intent("com.verizon.provider.Settings.SUW_ACTIVATION_SCREEN_STATUS");
        intent.setPackage("com.verizon.mips.services");
        intent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND | Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND);
        intent.putExtra("scenario", TextUtils.isEmpty(fdrDate) ? Constants.SCENARIO_SUW : Constants.SCENARIO_FDR_SUW);
        sendBroadcast(intent);
    }


    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "handleMessage what=" + msg.what);

            Intent showSimStatusIntent = new Intent(VzwSimCheckActivity.this, ShowSimStatusActivity.class);

            switch (msg.what) {
                case Constants.ACTION_SHOW_NO_SIM:
                    startShowSimStatusActivity(showSimStatusIntent, msg.what);
                    Log.d(TAG, "handleMessage ACTION_SHOW_NO_SIM");
                    break;
                case Constants.ACTION_SIM_NOT_READY:
                    Log.d(TAG, "handleMessage ACTION_SIM_NOT_READY");
                    break;
                case Constants.ACTION_SHOW_SIM_ERROR:
                    startShowSimStatusActivity(showSimStatusIntent, msg.what);
                    Log.d(TAG, "handleMessage ACTION_SHOW_SIM_ERROR");
                    break;
                case Constants.ACTION_SIM_READY:
                    Log.d(TAG, "handleMessage ACTION_SIM_READY");
                    break;
                case Constants.ACTION_SHOW_ACTIVATED:
                    showSimStatusIntent.putExtra(Constants.KEY_SIM_MDN, mMdn);
                    startShowSimStatusActivity(showSimStatusIntent, msg.what);
                    Log.d(TAG, "handleMessage ACTION_SHOW_ACTIVATED");
                    break;
                case Constants.ACTION_SKIP_DISPLAY:
                case Constants.ACTION_SHOW_NOT_ACTIVATED:
                    startShowSimStatusActivity(showSimStatusIntent, msg.what);
                    Log.d(TAG, "handleMessage ACTION_SHOW_NOT_ACTIVATED");
                    break;
                case Constants.ACTION_SHOW_PLAN_SELECTION:
                    startShowPlanSelectionActivity();
                    Log.d(TAG, "handleMessage ACTION_SHOW_PLAN_SELECTION");
                    break;
                case Constants.MSG_ACTION_NON_VZW_SIM_CHECK:
                    Log.d(TAG, "handleMessage MSG_ACTION_NON_VZW_SIM_CHECK");
                    checkIfNonVzwSimInterval();
                    break;
                default:
                    Log.d(TAG, "default handleMessage " + msg.what);
                    break;
            }
        }
    };

    private void startShowPlanSelectionActivity() {
        Intent planSelectionIntent = new Intent(VzwPlanSelection.ACTION_VZW_PLAN_SELECTION_FROM_SETUP);
        int simStatus = Utils.hasSimCard(getApplicationContext()) ? Constants.ACTION_SIM_READY : Constants.ACTION_SHOW_NO_SIM;
        planSelectionIntent.putExtra(Constants.KEY_SIM_STATUS, simStatus);

        startActivityMayWait(planSelectionIntent, Constants.REQUEST_CODE_NEXT, simStatus);
    }

    private void startShowSimStatusActivity(Intent intent, int simStatus) {
        intent.putExtra(Constants.KEY_SIM_STATUS, simStatus);
        intent.putExtra(Constants.KEY_PCO_DATA, mPco);

        startActivityMayWait(intent, Constants.REQUEST_CODE_NEXT, simStatus);
    }

    // start Activity may wait if this screen lasts less than 5s
    private void startActivityMayWait(Intent intent, int requestCode, int simStatus) {
        // get sim lock status
        mSimLockCallbackHandler.sendEmptyMessage(MSG_TRY_GET_SIM_LOCK_STATUS);

        // need wait if we have not gotten sim lock status util we get status or timeout
        if (!mGetSimlockStatusResponsed && simStatus != Constants.ACTION_SKIP_DISPLAY) {
            mPendingIntent = intent;
            return;
        }
        // reset pending intent
        mPendingIntent = null;

        long wastedTimeMillis = System.currentTimeMillis() - mStartTimeMillis;
        if (Utils.DEBUG) Log.d(TAG, "wastedTimeMillis=" + wastedTimeMillis);
        if (wastedTimeMillis > MIN_PAGE_WAIT_TIME) {
            startActivityForResultSafely(intent, requestCode,simStatus);
        } else {
            long needWaitTimeMillis = MIN_PAGE_WAIT_TIME - wastedTimeMillis;
            if (Utils.DEBUG) Log.d(TAG, "startActivityMayWait needWaitTimeMillis=" + needWaitTimeMillis);
            mHandler.postDelayed(() -> {
                startActivityForResultSafely(intent, requestCode,simStatus);
            }, needWaitTimeMillis);
        }
    }

    private void startActivityForResultSafely(Intent intent, int requestCode, int simStatus) {
        if (Utils.hasSimCard(getApplicationContext()) && Constants.ACTION_SHOW_NO_SIM == simStatus) {
            Log.d(TAG, "currently sim inserted,we should not skip for simStatus " + simStatus);
            return;
        }

        if (mIsNewActivityStarted) {
            Log.d(TAG, "startActivityForResultSafely : currently new activity is started!");
            return;
        }

        // get sim lock status
        mSimLockCallbackHandler.sendEmptyMessage(MSG_TRY_GET_SIM_LOCK_STATUS);

        Log.d(TAG, "startActivityForResultSafely " + intent);

        writeActivationStatus();
        cleanUp();

        // now, starting
        startActivityForResult(intent, requestCode);
    }

    private void cleanUp() {
        // set flags
        mIsStoppedThread = true;
        mIsNewActivityStarted = true;
        // remove pending msg and callbacks , such as long timeout msg
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }

        if (mSimLockCallbackHandler != null) {
            mSimLockCallbackHandler.removeCallbacksAndMessages(null);
        }

        stopFDRReadThread();
        unregisterSimStateChangeReceiver();
        unregisterPcoStateChangeReceiver();
    }

    private void writeActivationStatus() {
        if (mIsNonVzwSim) { // if non vzw sim, set specific pco to identify it
            mPco = Constants.PCO_DATA_NON_VZW;
        }

        // write activation status to secure settings
        Settings.Secure.putString(getContentResolver(), Constants.KEY_ACTIVATION_STATUS,
                String.valueOf((TextUtils.isEmpty(mMdn) ? "00000" : mMdn) + ":" + mPco));

        // record mdn and pco value
        try { //  with device protected storage, sp may cause exception in new version
            PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                    .edit()
                    .putString(Constants.KEY_SIM_MDN, mMdn)
                    .putInt(Constants.KEY_PCO_DATA, mPco)
                    .commit();
            if (Utils.DEBUG) Log.d(TAG, "save status to storage");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkSimActivationState() {
        if (mWorkerThread != null && mWorkerThread.isAlive()) {
            Log.d(TAG, "checkSimActivationState() thread is already running");
            return;
        }

        mIsStoppedThread = mIsNewActivityStarted; // assume work thread state same with NewActivity state
        if (Utils.DEBUG) Log.d(TAG, "checkSimActivationState : before starting mIsStoppedThread=" + mIsStoppedThread);
        if (!mIsStoppedThread) {
            Log.d(TAG, "checkSimActivationState()");
            mWorkerThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    int wasted = 0;
                    for (; wasted <= TIMEOUT_LONG; wasted += 5000) {
                        if (mIsStoppedThread) return;
                        mMdn = Utils.getMDN(mContext);

                        if (!Utils.isValidMdn(mMdn)) {
                            SystemClock.sleep(5000);
                            Log.d(TAG, "checkSimActivationState() wait " + wasted);
                        } else {
                            if (mPco != Constants.PCO_DATA_INIT) {
                                Log.d(TAG, "re-send broadcast PCO_CHANGE");
                                sendPcoChangeBroadcast(mPco);
                                return;
                            }
                            break;
                        }
                    }

                    // GO here , mdn is valid , wait valid pco
                    if (TIMEOUT_LONG > wasted) {
                        Log.d(TAG, "mdn is valid , wait valid pco " + (TIMEOUT_LONG - wasted));
                        SystemClock.sleep(TIMEOUT_LONG - wasted);
                    }

                    // timeout ,send broadcast
                    if (!mIsStoppedThread) {
                        Log.d(TAG, "send 'receive pco time out' broadcast");
                        sendPcoChangeBroadcast(Constants.PCO_DATA_TIME_OUT);
                    }
                }
            });
            mWorkerThread.start();
        }
    }

    private void sendPcoChangeBroadcast(int pco) {
        Intent intent = new Intent(Constants.ACTION_PCO_CHANGE);
        intent.putExtra(Constants.KEY_PCO_DATA, pco);
        sendBroadcast(intent);
    }

    class SimStateChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String state = intent.getStringExtra("ss");

            int curState = mTpManager.getSimState();
            Log.d(TAG, "receive SIM state " + state + " ,curState: " + curState);

            checkIfNonVzwSimInterval();

            switch (curState) {
                case TelephonyManager.SIM_STATE_ABSENT:
                    mIsReceiveSimState = true;
                    mHandler.sendEmptyMessage(Constants.ACTION_SHOW_NO_SIM);
                    break;
                case TelephonyManager.SIM_STATE_CARD_IO_ERROR:
                case TelephonyManager.SIM_STATE_UNKNOWN:
                    mIsReceiveSimState = true;
                    mHandler.sendEmptyMessage(Constants.ACTION_SHOW_SIM_ERROR);
                    break;
                case TelephonyManager.SIM_STATE_READY:
                    mIsReceiveSimState = true;
                    checkSimActivationState();
                    break;
            }
            int mSimState = mTpManager.getSimState();
            switch (state) {
                case "ABSENT":
                    mSimDescription = SIM_DESCRIPTION_ABSENT;
                    break;
                case "NOT_READY":
                    mSimDescription = SIM_DESCRIPTION_NOT_READY;
                    break;
                case "LOADED":
                    Log.d(TAG, "getSimState " + mSimState);
                    if (mSimState == TelephonyManager.SIM_STATE_READY) {
                        mSimDescription = SIM_DESCRIPTION_READY;
                    }
                    break;
                case "READY":
                case "IMSI":
                    mSimDescription = SIM_DESCRIPTION_READY;
                    break;
                case "CARD_IO_ERROR":
                case "UNKNOWN":
                    mSimDescription = SIM_DESCRIPTION_ERROR;
                    break;
            }

            /*if (SIM_DESCRIPTION_READY.equalsIgnoreCase(mSimDescription) &&
                    !mInternalHandler.hasMessages(MSG_ACTIVATION_TIMEOUT)) { // when ready , schedule timeout
                if (DEBUG) Log.e(TAG, "scheduleNextTimeoutIfNeeded when sim state is ready");
                mInternalHandler.removeMessages(MSG_RETRY_SCHEDULE_TIMEOUT);
                scheduleNextTimeoutIfNeeded();
            }*/
        }
    }

    private final String vzwSimMccMnc = "311480";
    private void checkIfNonVzwSimInterval() {
        String operator = mTpManager.getSimOperator();
        Log.d(TAG, "checkIfNonVzwSimInterval operator=" + operator);

        if (TextUtils.isEmpty(operator)) {
            Log.d(TAG, "checkIfNonVzwSimInterval : operator not available, retry later");
            mHandler.sendMessageDelayed(mHandler.obtainMessage(Constants.MSG_ACTION_NON_VZW_SIM_CHECK), 15 * 1000);
        } else{
            if (vzwSimMccMnc.contains(operator)) {
                mIsNonVzwSim = false;
                Log.d(TAG, "checkIfNonVzwSimInterval : current is vzw sim");
            } else {
                mIsNonVzwSim = true;
                Log.d(TAG, "checkIfNonVzwSimInterval : current is non vzw sim");
                Intent showSimStatusIntent = new Intent(VzwSimCheckActivity.this, ShowSimStatusActivity.class);
                startShowSimStatusActivity(showSimStatusIntent,Constants.ACTION_NON_VZW_SIM);
            }

            Log.d(TAG, "checkIfNonVzwSimInterval : mIsNonVzwSim=" + mIsNonVzwSim);
        }
    }

    class PcoChangeReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            mPco = intent.getIntExtra(Constants.KEY_PCO_DATA, Constants.PCO_DATA_NONE);
            Log.d(TAG, "receive pco " + mPco);

            //Settings.Secure.putInt(getContentResolver(), "vzw_pco", mPco);
            if (!mIsReceiveSimState) {
                return;
            }

            boolean validMdn = Utils.isValidMdn(mMdn);

            switch (mPco) {
                case Constants.PCO_DATA_0:
                    if (validMdn) {
                        mHandler.sendEmptyMessage(Constants.ACTION_SHOW_ACTIVATED);
                    } else {
                        Log.d(TAG, "mMdn is empty, wait...");
                    }
                    break;
                case Constants.PCO_DATA_3:
                    Log.d(TAG, "start plan selection");
                    mHandler.sendEmptyMessage(Constants.ACTION_SHOW_PLAN_SELECTION);
                    break;
                case Constants.PCO_DATA_5:
                    onActivateWithMBB(mPco);
                    break;
                case Constants.PCO_DATA_TIME_OUT:
                case Constants.PCO_DATA_NONE:
                    if (mPco == Constants.PCO_DATA_NONE) {
                        Log.d(TAG, "receive pco is -1");
                    } else {
                        Log.d(TAG, "receive pco or read mdn time out");
                    }

                    int what = validMdn ? Constants.ACTION_SHOW_ACTIVATED : Constants.ACTION_SHOW_NOT_ACTIVATED;
                    mHandler.sendEmptyMessage(what);
                    break;
                default:
                    Log.d(TAG, "default case");
                    if (validMdn) {
                        mHandler.sendEmptyMessage(Constants.ACTION_SHOW_ACTIVATED);
                    } else {
                        Log.d(TAG, "mMdn is empty, wait...");
                    }
                    break;
            }
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        mIsNewActivityStarted = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void acquireWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
            mWakeLock.acquire(TIMEOUT_LONG + 10000);
        }
    }

    private void releaseWakeLock() {
        if (mWakeLock != null) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }


    private void registerReceivers() {
        IntentFilter intentFilter = new IntentFilter(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        mSimStateChangeReceiver = new SimStateChangeReceiver();
        registerReceiver(mSimStateChangeReceiver, intentFilter);

        intentFilter = new IntentFilter(Constants.ACTION_PCO_CHANGE);
        mPcoChangeReceiver = new PcoChangeReceiver();
        registerReceiver(mPcoChangeReceiver, intentFilter);
        Log.d(TAG, "registerReceivers..");
    }

    private void unregisterSimStateChangeReceiver() {
        if (mSimStateChangeReceiver != null) {
            unregisterReceiver(mSimStateChangeReceiver);
            mSimStateChangeReceiver = null;
            Log.d(TAG, "unregisterSimReceivers..");
        }
    }

    private void unregisterPcoStateChangeReceiver() {
        if (mPcoChangeReceiver != null) {
            unregisterReceiver(mPcoChangeReceiver);
            mPcoChangeReceiver = null;
            Log.d(TAG, "unregisterPcoReceivers..");
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        cleanUp();
        releaseWakeLock();

        deregisterSimLockCallback();
        clearIfNeeded();
    }

    private void stopFDRReadThread() {
        if (mReadFDRThread != null && mReadFDRThread.isAlive()) {
            sendBroadcastToMVS(mFdrDate);
            mReadFDRThread.interrupt();
            mReadFDRThread = null;
        }
    }

    public void getNext() {
        int resultCode = Activity.RESULT_OK;
        Intent intent = WizardManagerHelper.getNextIntent(getIntent(), resultCode);
        startActivityForResult(intent, Constants.REQUEST_CODE_NEXT);
        finish();
    }

    private void onActivateWithMBB(int pco) {
        if (DEBUG) Log.e(TAG, "onActivateWithMBB pco=" + pco);
        if (pco == 5) {
            mSimLockCallbackHandler.postDelayed(this::startPcoCheck, 100);
        }
    }

    public void removeMessages(int what) {
        mSimLockCallbackHandler.removeMessages(what);
    }

    private void startPcoCheck() {
        if (SIM_DESCRIPTION_ABSENT.equals(mSimDescription) ||
                SIM_DESCRIPTION_ERROR.equals(mSimDescription)) {
            Log.e(TAG, "error sim state");
            return; // absent or error
        }

        mImsi = Utils.getImsi(getApplicationContext());
        mImei = Utils.getImei(getApplicationContext());

        if (DEBUG) {
            Log.e(TAG, "startPcoCheck pco=" + mPco);
        }

        /// if task is running , just return
        if (mLookupOrderTask != null && mLookupOrderTask.getStatus() == AsyncTask.Status.RUNNING) {
            return;
        }

        processPcoValues(getApplicationContext(), mPco);
    }

    void processPcoValues(Context context, int pco) {
        if (pco <= Constants.PCO_DATA_NONE) {
            return; // invalid pco
        }

        if (PoaConfig.isDebuggable()) {
            if (pco == Constants.PCO_DATA_0) { // pco 0 for test
                lookUpOrder();
            }
        } else if (pco == Constants.PCO_DATA_5) {  // pco 5 for product
            lookUpOrder();
        }
    }

    void lookUpOrder() {
        if ((mLookupOrderTask != null) && (mLookupOrderTask.getStatus() != AsyncTask.Status.FINISHED)) {
            if (DEBUG) {
                Log.e(TAG, "status=" + mLookupOrderTask.getStatus());
            }

            mLookupOrderTask.cancel(true);
            mLookupOrderTask = null;
        }

        if (DEBUG) {
            //Toast.makeText(getActivity(), "lookup Order ...", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "lookup Order ...");
        }

        removeMessages(MSG_RETRY_LOOKUP_ORDER_REQ);

        mLookupOrderTask = new LookUpOrderTask();
        mLookupOrderTask.execute();
    }

    private void startActivityPanel(Intent intent) {
        clearIfNeeded();
        startActivityForResult(intent, 1);
        finish();
    }

    private void handlePendingOrderFound(LookUpOrderRequest request) {
        int rc = request.getAccountRestricted();
        Log.d(TAG, "mLookupReq.getAccountRestricted rc  =" + rc);
        if (rc == 0) { // Restricted Ac
            Log.e(TAG, "handlePendingOrderFound Restricted Ac");
            String errorCode = request.getErrorCode();
            Log.d(TAG, "handlePendingOrderFound: mLookupReq.getErrorCode=" + errorCode + " ,getOrderType=" + request.getOrderType());
            //start to VzwPoaStatusActivity
            Intent intent = WizardManagerHelper.getNextIntent(getIntent(), 201);
            intent.putExtra(VzwPoaStatusActivity.POA_STATUS_KEY, VzwPoaStatusActivity.NewActOrderRestricted);
            intent.putExtra(VzwPoaStatusActivity.POA_ORDER_TYPE_KEY, request.getOrderType());
            startActivityPanel(intent);
            return;
        }

        String errorCode = request.getErrorCode();
        if (DEBUG) {
            Log.e(TAG, "handlePendingOrderFound errorCode=" + errorCode);
        }

        if (VzwPoaRequest.ERR_CODE_00000.equals(errorCode)) {
            mCorrelationID = request.getCorrelationID();
            mRequestID = request.getRequestID();
            mSecurityQID = request.getSecurityQuestionID();
            Bundle args = new Bundle();
            args.putString("mCorrelationID", mCorrelationID);
            args.putString("mRequestID", mRequestID);
            args.putInt("mSecurityQuestionID", mSecurityQID);
            args.putInt("mOrderType",request.getOrderType());
            Log.e(TAG, "mOrderType=" + request.getOrderType());
            //start to VzwPendingOrderAuthenticationActivity
            Intent intent = WizardManagerHelper.getNextIntent(getIntent(), 202);
            intent.putExtra(PoaCommon.ARGS, args);
            startActivityPanel(intent);
            Log.d(TAG, "handlePendingOrderFound mSecurityQID =" + mSecurityQID
                    + "\n mRequestID=" + mRequestID + "\n mCorrelationID=" + mCorrelationID);

            Log.d(TAG, "Billing Password Exist =" + (mSecurityQID == 1));
        } else {
            Log.d(TAG, "order found but error code is " + rc);
        }
    }

    private void handlePendingOrderLookupTimeout(LookUpOrderRequest request) {
        String errorCode = request.getErrorCode();
        if (DEBUG) {
            //Toast.makeText(getActivity(), "lookup order timeout errorCode=" + errorCode, Toast.LENGTH_SHORT).show();
            Log.d(TAG, "lookup order timeout errorCode=" + errorCode);
            Log.d(TAG, "handlePendingOrderLookupTimeout: mLookupReq.getErrorCode=" + errorCode + " ,getOrderType=" + request.getOrderType());
        }
        //start to VzwPoaStatusActivity
        Intent intent = WizardManagerHelper.getNextIntent(getIntent(), 201);
        intent.putExtra(VzwPoaStatusActivity.POA_STATUS_KEY, VzwPoaStatusActivity.LookupOrderTimeout);
        intent.putExtra(VzwPoaStatusActivity.POA_ORDER_TYPE_KEY, request.getOrderType());
        startActivityPanel(intent);
    }

    void handlePendingOrderNotFound(LookUpOrderRequest request) {
        String errorCode = request.getErrorCode();
        String errorMessage = request.getErrorMessage();
        if (DEBUG) {
            //Toast.makeText(getActivity(), "no order found errorCode=" + errorCode, Toast.LENGTH_SHORT).show();
            Log.d(TAG, "handlePendingOrderNotFound: mLookupReq.getErrorCode=" + errorCode + " ,getOrderType=" + request.getOrderType());
        }
        //start to VzwPoaStatusActivity
        Intent intent = WizardManagerHelper.getNextIntent(getIntent(), 201);
        int whatCase;
        if (VzwPoaRequest.matchSecurityFailure(errorCode, errorMessage)) {
            Log.e(TAG, "security error occurred");
            // show security error
            whatCase = VzwPoaStatusActivity.Lost_and_Stolen_Device_or_SIM;
        } else {
            whatCase = VzwPoaStatusActivity.NewActOrderNotFound;
        }
        intent.putExtra(VzwPoaStatusActivity.POA_STATUS_KEY, whatCase);
        intent.putExtra(VzwPoaStatusActivity.POA_ORDER_TYPE_KEY, request.getOrderType());
        startActivityPanel(intent);
    }

    private void clearIfNeeded() {
        //releaseWakeLock();

        if (mLookupOrderTask != null && (mLookupOrderTask.getStatus() != AsyncTask.Status.FINISHED)) {
            mLookupOrderTask.cancel(true);
            mLookupOrderTask = null;
        }
    }

    class LookUpOrderTask extends AsyncTask<Void, Void, Integer> {
        LookUpOrderRequest request;
        LookUpOrderTask() {
            request = new LookUpOrderRequest();
        }

        @Override
        protected Integer doInBackground(Void... args) {
            if (isCancelled() || request == null) {
                Log.e(TAG, "doInBackground no need to do work");
                return null;
            }

            Log.d(TAG, "LookUpOrderTask doInBackground..");
            Log.e(TAG, "imsi=" + mImsi + " imei=" + mImei);
            return request.lookupOrderReq(getApplicationContext(), mImsi, mImei);
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (isCancelled() || request == null || result == null) { // return when canceled or fragment invalid state
                Log.d(TAG, "isCancelled=" + isCancelled());
                return;
            }

            // retry for failure
            if (result != LookUpOrderRequest.MSG_PO_TIME_OUT) {
                if (request.getStatusCode() == null || VzwPoaRequest.STATUS_CODE_FAILURE.equalsIgnoreCase(request.getStatusCode())) {
                    Log.e(TAG, "request=" + request + " , req retry times=" + mReqRetry + " , statusCode=" + request.getStatusCode());
                    if (mReqRetry < REQ_RETRY_MAX_TIMES) {
                        mReqRetry++;
                        removeMessages(MSG_RETRY_LOOKUP_ORDER_REQ);
                        mSimLockCallbackHandler.sendEmptyMessageDelayed(MSG_RETRY_LOOKUP_ORDER_REQ, 9000);
                        return;
                    }
                }
            }

            removeMessages(MSG_RETRY_LOOKUP_ORDER_REQ);

            Log.d(TAG, "LookUpOrderTask onPostExecute result=" + result);


            Message msg = mSimLockCallbackHandler.obtainMessage();
            msg.obj = request;

            switch (result) {
                case LookUpOrderRequest.MSG_PO_NEW_ORDER:
                    msg.what = LookUpOrderRequest.MSG_PO_NEW_ORDER;
                    break;
                case LookUpOrderRequest.MSG_PO_UPGRADE_ORDER:
                    msg.what = LookUpOrderRequest.MSG_PO_UPGRADE_ORDER;
                    break;
                case LookUpOrderRequest.MSG_PO_NOT_FOUND:
                    msg.what = LookUpOrderRequest.MSG_PO_NOT_FOUND;
                    break;
                case LookUpOrderRequest.MSG_PO_TIME_OUT:
                    msg.what = LookUpOrderRequest.MSG_PO_TIME_OUT;
                    break;
            }

            msg.sendToTarget();
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            if (DEBUG) {
                Log.e(TAG, this + " onCancelled");
            }
            request.onCancelled();
            request = null;
        }
    }
}
