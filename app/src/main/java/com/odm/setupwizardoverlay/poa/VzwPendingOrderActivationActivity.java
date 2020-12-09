package com.odm.setupwizardoverlay.poa;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.odm.setupwizardoverlay.view.ScrollViewExt;

public class VzwPendingOrderActivationActivity extends PoaCommon {

    private static final String TAG = VzwPendingOrderActivationActivity.class.getSimpleName();
    public static final double REQ_RETRY_MAX_TIMES = 5;
    public static final int MSG_HANDLE_AIRPLANE_MODE_CHANGED = 9;
    public static final int MSG_GET_MDN = 10;
    private static final int MSG_RETRY_RELEASE_ORDER_REQ = 11;
    public static final int MSG_ACTIVATION_SUCCESS = 12;
    public static final int MSG_AUTO_ACTIVATION = 13;

    public static final int GET_MDN_DELAY_MILLIS = 3500;
    public static final int ACTIVATION_SUCCESS_DISPLAY_DELAY_MILLIS = 5000;
    public static final int AIRPLANE_MODE_CHANGED_DELAY_MILLIS = 3000;

    private ProgressBar mProgressIndicator;
    private Button mBtnActivateNow;
    private TextView mTvNotice;
    private String mCorrelationID;
    private String mRequestID;
    private ReleaseTask mReleaseTask;
    private int mPco;
    private int mReqRetry = 0;
    private ProgressDialog mProgressDialog;
    private int mOrderType;
    private PcoChangeReceiver mPcoReceiver;
    private ScrollViewExt mScrollView;
    private String mSecurityQuestionID;

    @Override
    protected int getLayoutResId() {
        return 0;
    }

    @Override
    public String getTitleString() {
        return null;
    }

    @Override
    protected void initView() {

    }

    @Override
    protected void initAction() {

    }

    private void parseArgs() {
        Bundle args = getIntent().getBundleExtra(PoaCommon.ARGS);
        if (args != null) {
            mCorrelationID = args.getString("mCorrelationID");
            mRequestID = args.getString("mRequestID");
            mSecurityQuestionID = args.getString("mSecurityQuestionID");
            mOrderType = args.getInt("mOrderType");
        }
    }
}
