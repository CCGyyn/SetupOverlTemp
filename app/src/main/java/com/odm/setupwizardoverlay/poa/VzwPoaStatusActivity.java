package com.odm.setupwizardoverlay.poa;

import android.app.Activity;
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

import com.odm.setupwizardoverlay.R;
import com.odm.setupwizardoverlay.Utils;

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
        Intent intent = getIntent();
        mPoaStatus = intent.getIntExtra(POA_STATUS_KEY, -1);
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
        mTitle = (TextView) findViewById(R.id.poa_title);
        mTvNotice = (TextView) findViewById(R.id.tv_notice);
        mTvNotice.setMovementMethod(ScrollingMovementMethod.getInstance());
        mEmergencyBtn = (Button) findViewById(R.id.emergency_button_btn);
        mRightBtn = (Button) findViewById(R.id.right_btn);

        switch (mPoaStatus) {
            case UpgradeOrderNotFound:
            case NewActOrderNotFound:
                showWifiButton();
                mTvNotice.setText(R.string.poa_upgrade_order_not_found);
                break;
            case UpgradeOrderPasswordAuthFailed:
            case UpgradeOrderSSNAuthFailed:
            case NewActCustValidatePasswdIncorrect:
            case NewActCustValidateSSNIncorrect:
                setEmergencyBtnVisibility(View.VISIBLE);
                mTvNotice.setText(R.string.poa_entry_not_match_records);
                break;
            case UpgradeReleaseOrderAuthFailed:
            case UpgradeReleaseOrderFailedOrderNotExist:
            case NewActReleaseOrderFailedAuthError:
            case NewActReleaseOrderPendingNotExist:
                showWifiButton();
                mTvNotice.setText(R.string.sorry_not_activate);
                break;
            case UpgradeReleaseOrderCorrelationIdIncorrect:
            case NewActReleaseOrderCoorrelIDIncorrect:
                setEmergencyBtnVisibility(View.VISIBLE);
                showWifiButton();
                mTvNotice.setText(R.string.sorry_not_activate);
                break;
            case NewActOrderRestricted:
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
                mRightBtn.setText(R.string.turn_off_phone);
                break;
            case LookupOrderTimeout:
                mTvNotice.setText(R.string.lookup_order_timeout);
                break;
            case ValidateCustomerTimeout:
                mTvNotice.setText(R.string.validate_customer_timeout);
                break;
            case ReleaseOrderTimeout:
                showWifiButton();
                mTvNotice.setText(R.string.release_order_timeout);
                break;
            case Lost_and_Stolen_Device_or_SIM:
                mTvNotice.setText(R.string.lost_and_stolen_device_or_sim);
                break;
            case PendingProvisionErrorCode00013:
                mTvNotice.setText(R.string.pending_provision_error_code_00013);
                break;
            case NewActReleaseOrderSuccess:
            case UpgradeReleaseSuccess5CharAccountPIN:
                isActivated = true;
                setRightLabel(getString(R.string.label_next));

                Bundle args = getArguments();
                if (args != null) {
                    String mdn = args.getString("mdn", null);
                    int type = args.getInt(POA_ORDER_TYPE_KEY);
                    boolean showEm = (type == LookUpOrderRequest.MSG_PO_NEW_ORDER) || mPoaStatus == UpgradeReleaseSuccess5CharAccountPIN;
                    setEmergencyBtnVisibility(showEm ? View.VISIBLE : View.GONE);
                    String phoneNumber = getString(R.string.phone_number_unknown);
                    if (mdn != null) {
                        phoneNumber = PhoneNumberUtils.formatNumber(mdn, Locale.getDefault().getCountry());
                    }

                    String info = String.format(getString(R.string.phone_is_now_active), phoneNumber);
                    mTvNotice.setText(info);

                    sendActivationSuccessBroadcast(mdn);
                }
                break;
        }
    }

    @Override
    protected void initAction() {
        switch (mPoaStatus) {
            case NewActOrderRestricted:
                mRightBtn.setOnClickListener(v -> {
                    //ccg
                    //turn off phone
                    powerOff(getApplicationContext());
                });
                break;
        }
    }

    public void onclickEmergencyCall(View view) {
        Utils.onclickEmergencyCall(getApplicationContext());
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
        getApplicationContext().sendBroadcast(intent);
    }

}
