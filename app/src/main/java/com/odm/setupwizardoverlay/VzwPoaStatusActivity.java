package com.odm.setupwizardoverlay;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.android.setupwizardlib.util.WizardManagerHelper;
import com.odm.setupwizardoverlay.poa.LookUpOrderRequest;
import com.odm.setupwizardoverlay.poa.PoaCommon;

import java.util.Locale;


public class VzwPoaStatusActivity extends PoaCommon {
    private static final String TAG = VzwPoaStatusActivity.class.getSimpleName();
    public static final String POA_STATUS_KEY = "poa_status";
    public static final String POA_ORDER_TYPE_KEY = "mOrderType";
    private static final String ACTION_SHOW_ACTIVATION_SUCCESS = "com.android.phone.ACTION_ACTIVATION_SUCCESS";

    public static final int UpgradeOrderNotFound = 1;
    public static final int UpgradeOrderPasswordAuthFailed = 2;
    public static final int UpgradeOrderSSNAuthFailed = 3;
    public static final int UpgradeReleaseOrderCorrelationIdIncorrect = 4;
    public static final int UpgradeReleaseOrderFailedOrderNotExist = 5;
    public static final int UpgradeReleaseOrderAuthFailed = 6;
    public static final int UpgradeReleaseOrderMutipleReq = 7;
    public static final int UpgradeReleaseOrderSuccess = 8;
    public static final int NewActOrderNotFound = 9;
    public static final int NewActOrderRestricted = 10;
    public static final int NewActCustValidatePasswdIncorrect = 11;
    public static final int NewActCustValidateSSNIncorrect = 12;
    public static final int NewActReleaseOrderCoorrelIDIncorrect = 13;
    public static final int NewActReleaseOrderPendingNotExist = 14;
    public static final int NewActReleaseOrderFailedAuthError = 15;
    public static final int NewActReleaseOrderMultipleReq = 16;
    public static final int NewActReleaseOrderSuccess = 17;
    public static final int LookupOrderTimeout = 18;
    public static final int ValidateCustomerTimeout = 19;
    public static final int ReleaseOrderTimeout = 20;
    public static final int Lost_and_Stolen_Device_or_SIM = 21;
    public static final int PendingProvisionErrorCode00013 = 22;
    public static final int UpgradeReleaseOrderSuccessThruNotification = 23;
    public static final int UpgradeReleaseOrderSuccessThruWebPortal = 24;
    public static final int UpgradeReleaseSuccess5CharAccountPIN = 25;

    private TextView mTitle;
    private TextView mTvNotice;
    private Button mEmergencyBtn;
    private Button mRightBtn;
    private int mPoaStatus;
    private boolean isActivated = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.poa_status;
    }

    @Override
    public String getTitleString() {
        return null;
    }

    @Override
    protected void initView() {
        mPoaStatus = getIntent().getIntExtra(POA_STATUS_KEY, -1);
        Log.d(TAG, "mPoaStatus=" + mPoaStatus);
        mTitle = (TextView) findViewById(R.id.page_header);
        mTvNotice = (TextView) findViewById(R.id.tv_notice);
        mTvNotice.setMovementMethod(ScrollingMovementMethod.getInstance());
        mEmergencyBtn = (Button) findViewById(R.id.emergency_button_btn);
        mRightBtn = (Button) findViewById(R.id.right_btn);

        mTitle.setText(R.string.phone_act_title);

        switch (mPoaStatus) {
            case UpgradeOrderNotFound:
            case NewActOrderNotFound:
                //mTitle.setText(R.string.phone_act_title);
                mTvNotice.setText(R.string.poa_upgrade_order_not_found);
                //ccg Emergency Call (911)] [Turn off phone] 不确定
                initWifiButton(mRightBtn);
                break;
            case UpgradeOrderPasswordAuthFailed:
            case UpgradeOrderSSNAuthFailed:
            case NewActCustValidatePasswdIncorrect:
            case NewActCustValidateSSNIncorrect:
                mTvNotice.setText(R.string.poa_entry_not_match_records);
                //VZ_REQ_ACTIVATIONUI_39837
                initBackButton(mRightBtn);
                break;
            case UpgradeReleaseOrderAuthFailed:
            case UpgradeReleaseOrderFailedOrderNotExist:
            case NewActReleaseOrderFailedAuthError:
            case NewActReleaseOrderPendingNotExist:
                mTvNotice.setText(R.string.sorry_not_activate);
                //ccg may be Smartphone VZ_REQ_ACTIVATIONUI_39819
                initWifiButton(mRightBtn);
                break;
            case UpgradeReleaseOrderCorrelationIdIncorrect:
            case NewActReleaseOrderCoorrelIDIncorrect:
                mTvNotice.setText(R.string.sorry_not_activate);
                //ccg may be Smartphone VZ_REQ_ACTIVATIONUI_39819
                initWifiButton(mRightBtn);
                break;
            case NewActOrderRestricted:
                //AU VZ_REQ_ACTIVATIONUI_39821
                // get Device ID and SIM ID
                mTitle.setText(R.string.acc_restricted_title);
                String imsi = Utils.getImsi(getApplicationContext());
                String imei = Utils.getImei(getApplicationContext());

                String format = getString(R.string.account_restricted);
                String restrictedInfo = String.format(format, imei, imsi);
                if (DEBUG) {
                    Log.d(TAG, "restricted info=" + restrictedInfo);
                }
                mTvNotice.setText(Html.fromHtml(restrictedInfo));
                //mRightBtn.setText(R.string.turn_off_phone);
                onlyEmergencyBtn();
                break;
            case LookupOrderTimeout:
                mTvNotice.setText(R.string.lookup_order_timeout);
                //ccg
                /*
                *Activation Timeout VZ_REQ_ACTIVATIONUI_39796 Buttons: [Emergency Call (911)] [Restart phone] [Use Wi-F
                *General timeout Smartphone VZ_REQ_ACTIVATIONUI_39843 Smartphone VZ_REQ_ACTIVATIONUI_39843 Buttons: [Emergency Call (911)]
                * Activation NOT successful VZ_REQ_ACTIVATIONUI_39774 Buttons: [Emergency Call (911)] [Use Wi-Fi]
                */
                initWifiButton(mRightBtn);
                break;
            case ValidateCustomerTimeout:
                mTvNotice.setText(R.string.validate_customer_timeout);
                //initWifiButton(mRightBtn);
                initBackButton(mRightBtn);
                break;
            case ReleaseOrderTimeout:
                mTvNotice.setText(R.string.release_order_timeout);
                initWifiButton(mRightBtn);
                break;
            case Lost_and_Stolen_Device_or_SIM:
                //VZ_REQ_ACTIVATIONUI_39831
                //VZ_REQ_ACTIVATIONUI_39843
                mTvNotice.setText(R.string.lost_and_stolen_device_or_sim);
                onlyEmergencyBtn();
                break;
            case PendingProvisionErrorCode00013:
                //ccg VZ_REQ_ACTIVATIONUI_39843
                //only Emergency Cal
                mTvNotice.setText(R.string.pending_provision_error_code_00013);
                initWifiButton(mRightBtn);
                break;
            case NewActReleaseOrderSuccess:
            case UpgradeReleaseSuccess5CharAccountPIN:
                isActivated = true;
                mRightBtn.setText(R.string.label_next);
                mRightBtn.setOnClickListener(v -> {
                    /*Intent intent = new Intent(this, ShowSimStatusActivity.class);
                    intent.putExtra(Constants.KEY_SIM_STATUS, Constants.ACTION_SKIP_DISPLAY);
                    intent.putExtra(Constants.KEY_PCO_DATA, Constants.PCO_DATA_0);
                    startActivityPanel(intent);*/
                    //start to VzwCloudSetupActivity
                    Intent intent = WizardManagerHelper.getNextIntent(getIntent(), 102);
                    startActivityPanel(intent);
                });
                Bundle args = getIntent().getBundleExtra(PoaCommon.ARGS);
                if (args != null) {
                    String mdn = args.getString("mdn", null);
                    int type = args.getInt(POA_ORDER_TYPE_KEY);
                    boolean showEm = (type == LookUpOrderRequest.MSG_PO_NEW_ORDER) || mPoaStatus == UpgradeReleaseSuccess5CharAccountPIN;
                    mEmergencyBtn.setVisibility(showEm ? View.VISIBLE : View.INVISIBLE);
                    String phoneNumber = getString(R.string.phone_number_unknown);
                    if (mdn != null) {
                        phoneNumber = PhoneNumberUtils.formatNumber(mdn, Locale.getDefault().getCountry());
                    }

                    //ccg VZ_REQ_ACTIVATIONUI_39740
                    //[Emergency Call (911)] [Next]
                    String info = String.format(getString(R.string.phone_is_now_active), phoneNumber);
                    mTvNotice.setText(info);

                    sendActivationSuccessBroadcast(mdn);
                }
                break;
        }
    }

    @Override
    protected void initAction() {
    }

    private void onlyEmergencyBtn() {
        mRightBtn.setVisibility(View.GONE);
    }

    private void initWifiButton(Button button) {
        button.setText(R.string.wi_fi);
        //ccg
        button.setOnClickListener(v -> {
            //start to carrier_setup, use wifi
            Intent intent = WizardManagerHelper.getNextIntent(getIntent(), RESULT_OK);
            startActivityPanel(intent);
        });
    }

    private void initBackButton(Button button) {
        button.setText(R.string.label_back);
        button.setOnClickListener(v -> {
            onBackPressed();
        });
    }

    private void initTurnOffPhone(Button button) {
        button.setText(R.string.turn_off_phone);
        button.setOnClickListener(v -> {
            //ccg
            //turn off phone
            powerOff(this);
        });
    }

    public void onclickEmergencyCall(View view) {
        Utils.onclickEmergencyCall(this);
    }

    private void powerOff(Context context) {
        Intent requestShutdown = new Intent(
                Intent.ACTION_REQUEST_SHUTDOWN);
        requestShutdown.putExtra(Intent.EXTRA_KEY_CONFIRM, false);
        requestShutdown.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(requestShutdown);
    }

    private void sendActivationSuccessBroadcast(String mdn) {
        Intent intent = new Intent(ACTION_SHOW_ACTIVATION_SUCCESS);
        intent.setPackage("com.android.phone");
        sendBroadcast(intent);
    }

}
