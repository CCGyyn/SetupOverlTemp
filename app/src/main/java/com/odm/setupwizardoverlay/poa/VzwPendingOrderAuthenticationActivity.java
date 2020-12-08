package com.odm.setupwizardoverlay.poa;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.odm.setupwizardoverlay.R;

public class VzwPendingOrderAuthenticationActivity extends PoaCommon {

    public static final String TAG = VzwPendingOrderAuthenticationActivity.class.getSimpleName();
    public static final String ATTEMPTS = "mAttempts";
    public static final double REQ_RETRY_MAX_TIMES = 5;
    public static final int MSG_RETRY_VALIDATE_ORDER_REQ = 9;
    public static final int TYPE_1_PSW = 1;
    public static final int TYPE_2_SSN = 2;

    private String mCorrelationID;
    private String mRequestID;
    private int mSecurityQID;
    private TextView mTvBillingPassword;
    private TextView mTvSsn;
    private EditText mEtPassword;
    private TextView mTvAnotherWay;
    private TextView mTvWhatIsBillingPasswd;
    private Button mBtnVerify;
    private Button mBtnSwitchInput;
    private int mInputType = TYPE_2_SSN;  /// 1,Billing Password. 2,SSN

    private ValidateTask mValidateTask;
    private int mAttempts = 0;
    private SharedPreferences mConfig;
    private ProgressDialog mProgressDialog;
    private int mReqRetry = 0;
    private String mPasswd;
    private int mOrderType;

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

    private class ValidateTask extends AsyncTask<Void, Void, Integer> {

        private final String securityQID;
        private final String correlationID;
        private final String requestID;
        private String passwd;

        ValidateCustomerRequest request;

        private ValidateTask(String securityQID, String correlationID, String requestID, String passwd) {
            this.securityQID = securityQID;
            this.correlationID = correlationID;
            this.requestID = requestID;
            this.passwd = passwd;
            request = new ValidateCustomerRequest();
        }

        protected Integer doInBackground(Void... paramVarArgs) {
            if (isCancelled() || request == null) { // return when canceled or fragment invalid state
                Log.d(TAG, "isCancelled=" + isCancelled());
                return null;
            }

            // should input passwd
            int ret = request.validateOrderReq(getApplicationContext(), passwd, correlationID, requestID, securityQID);
            Log.d("ValidateTask", "verify doInBackground ret " + ret);
            return ret;
        }

        protected void onPostExecute(Integer result) {
            if (isCancelled() || request == null || result == null) { // return when canceled or fragment invalid state
                Log.d(TAG, "isCancelled=" + isCancelled());
                return;
            }

            String statusCode = request.getStatusCode();
            if (DEBUG) {
                Log.e(TAG, "statusCode=" + statusCode);
            }
            if (!TextUtils.isEmpty(statusCode)) {
                mAttempts += 1;
                mConfig.edit().putInt(ATTEMPTS, mAttempts).apply();
                Log.e(TAG, "current attempts=" + mAttempts);
            }
            // retry for failure
            if (result != ValidateCustomerRequest.MSG_VALIDATE_CUSTOMER_TIMEOUT) {
                if (statusCode == null || VzwPoaRequest.STATUS_CODE_FAILURE.equalsIgnoreCase(statusCode)) {
                    if (DEBUG) {
                        Log.e(TAG, "request=" + request + " ,req retry times=" + mReqRetry + " , statusCode=" + statusCode + " ,errorMessage=" + request.getErrorMessage());
                    }

                    if (mReqRetry < REQ_RETRY_MAX_TIMES) {
                        mReqRetry++;
                        removeMessages(MSG_RETRY_VALIDATE_ORDER_REQ);
                        getInternalHandler().sendEmptyMessageDelayed(MSG_RETRY_VALIDATE_ORDER_REQ, 9000);
                        return;
                    }
                }
            }

            removeMessages(MSG_RETRY_VALIDATE_ORDER_REQ);

            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
            }

            if (DEBUG) Log.e(TAG, "ValidateTask onPostExecute=" + result);
            Handler handler = getInternalHandler();
            Message msg = handler.obtainMessage();
            msg.obj = request;

            switch (result) {
                case ValidateCustomerRequest.MSG_VALIDATE_CUSTOMER_SUCCESS:
                    msg.what = ValidateCustomerRequest.MSG_VALIDATE_CUSTOMER_SUCCESS;
                    break;
                case ValidateCustomerRequest.MSG_VALIDATE_CUSTOMER_FAILURE:
                    msg.what = ValidateCustomerRequest.MSG_VALIDATE_CUSTOMER_FAILURE;
                    break;
                case ValidateCustomerRequest.MSG_VALIDATE_CUSTOMER_TIMEOUT:  // timeout
                    msg.what = ValidateCustomerRequest.MSG_VALIDATE_CUSTOMER_TIMEOUT;
                    break;
                default:
                    Log.e(TAG, "error validate result type =" + result);
                    msg.what = ValidateCustomerRequest.MSG_VALIDATE_CUSTOMER_FAILURE;
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

    @Override
    public boolean handleMessage(Message msg) {
        boolean consumed = false;
        switch (msg.what) {
            case ValidateCustomerRequest.MSG_VALIDATE_CUSTOMER_SUCCESS:  /// success
                consumed = true;
                if (DEBUG) {
                    Log.e(TAG, "Validate Order success");
                    //Toast.makeText(getContext(), "Validate Order success", Toast.LENGTH_LONG).show();
                }
                handleCustomerValidateSuccess((ValidateCustomerRequest) msg.obj);
                break;
            case ValidateCustomerRequest.MSG_VALIDATE_CUSTOMER_FAILURE:  /// failure
                consumed = true;
                if (DEBUG) {
                    Log.e(TAG, "Password Validation failed");
                    //Toast.makeText(getContext(), "Validation failed", Toast.LENGTH_LONG).show();
                }
                showPoaStatus(VzwPoaStatusFragment.UpgradeOrderPasswordAuthFailed);
                break;
            case ValidateCustomerRequest.MSG_VALIDATE_CUSTOMER_TIMEOUT: /// timeout
                consumed = true;
                if (DEBUG) {
                    Log.e(TAG, "Validate Order timed out");
                    //Toast.makeText(getContext(), "Validate Order timed out", Toast.LENGTH_LONG).show();
                }
                showPoaStatus(VzwPoaStatusActivity.ValidateCustomerTimeout);
                break;
            case MSG_RETRY_VALIDATE_ORDER_REQ:
                if (DEBUG) {
                    Log.e(TAG, "retry validate order req mPasswd=" + mPasswd);
                }
                if (!TextUtils.isEmpty(mPasswd)) {
                   doVerify(mPasswd);
                } else {
                    sendEmptyMessage(ValidateCustomerRequest.MSG_VALIDATE_CUSTOMER_TIMEOUT);
                }
                break;
        }

        return consumed;
    }

    private void showPoaStatus(int status) {
        Intent intent = new Intent(this, VzwPoaStatusActivity.class);
        intent.putExtra(VzwPoaStatusActivity.POA_STATUS_KEY, status);
        intent.putExtra(VzwPoaStatusActivity.POA_ORDER_TYPE_KEY, mOrderType);
        startActivityPanel(intent);
    }

    @Override
    protected void clearIfNeeded() {
        super.clearIfNeeded();
        if (mValidateTask != null && (mValidateTask.getStatus() != AsyncTask.Status.FINISHED)) {
            mValidateTask.cancel(true);
            mValidateTask = null;
        }
    }


    private void handleCustomerValidateSuccess(ValidateCustomerRequest request) {

        Bundle args = new Bundle();
        args.putString("mCorrelationID", request.getCorrelationID());
        args.putString("mRequestID", request.getRequestID());
        args.putInt("mOrderType", mOrderType);
        args.putString("mSecurityQuestionID", mInputType == TYPE_1_PSW ? LookUpOrderRequest.SECURITY_QUESTION_ID_001 : LookUpOrderRequest.SECURITY_QUESTION_ID_002);
        boolean callerNotification = this.isCallerNotification();
        if (callerNotification) {
            Toast.makeText(getApplicationContext(), getApplicationContext().getText(R.string.activating_title)+
                    "..."+getApplicationContext().getString(R.string.activating_message), Toast.LENGTH_LONG).show();

            if (DEBUG) {
                Log.d(TAG, "callerNotification" + " , release order");
            }
            Intent intent = new Intent(getApplicationContext(), VzwActivationService.class);
            intent.putExtra("args", args);
            intent.putExtra(VzwActivationService.REQ_TYPE, VzwActivationService.REQ_RELEASE);
            intent.setPackage(getApplicationContext().getPackageName());
            getApplicationContext().startService(intent);
            finishSetup(getActivity());
            this.overridePendingTransition(R.anim.anim_right_in, R.anim.anim_left_out);
        } else {
            if (DEBUG) {
                Log.e(TAG, "startActivationPanel mOrderType=" + mOrderType);
            }
            startFragmentPanel(VzwPendingOrderActivationFragment.class.getName(), args);
        }

    }
}
