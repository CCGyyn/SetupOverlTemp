package com.odm.setupwizardoverlay;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.android.setupwizardlib.util.WizardManagerHelper;
import com.odm.setupwizardoverlay.poa.PoaCommon;
import com.odm.setupwizardoverlay.poa.ValidateCustomerRequest;
import com.odm.setupwizardoverlay.poa.VzwActivationService;
import com.odm.setupwizardoverlay.poa.VzwPoaRequest;

import static com.odm.setupwizardoverlay.poa.LookUpOrderRequest.SECURITY_QUESTION_ID_001;
import static com.odm.setupwizardoverlay.poa.LookUpOrderRequest.SECURITY_QUESTION_ID_002;

public class VzwPendingOrderAuthenticationActivity extends PoaCommon implements DialogInterface.OnDismissListener{

    public static final String TAG = VzwPendingOrderAuthenticationActivity.class.getSimpleName();
    public static final String ATTEMPTS = "mAttempts";
    public static final double REQ_RETRY_MAX_TIMES = 5;
    public static final int MSG_RETRY_VALIDATE_ORDER_REQ = 9;
    public static final int TYPE_1_PSW = 1;
    public static final int TYPE_2_SSN = 2;

    private String mCorrelationID;
    private String mRequestID;
    private int mSecurityQID;
    private TextView mTitle;
    private TextView mTvBillingPassword;
    private TextView mTvSsn;
    private EditText mEtPassword;
    private TextView mTvAnotherWay;
    private TextView mTvWhatIsBillingPasswd;
    private Button mBtnVerify;
    private Button mBtnSwitchInput;
    private Button mEmergencyBtn;
    private Button mRightBtn;
    private int mInputType = TYPE_2_SSN;  /// 1,Billing Password. 2,SSN

    private ValidateTask mValidateTask;
    private int mAttempts = 0;
    private SharedPreferences mConfig;
    private ProgressDialog mProgressDialog;
    private int mReqRetry = 0;
    private String mPasswd;
    private int mOrderType;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mConfig = this.getSharedPreferences("config", Context.MODE_PRIVATE);
        mAttempts = mConfig.getInt(ATTEMPTS, 0);
        mReqRetry = 0;
    }

    @Override
    public void onResume() {
        super.onResume();
        //ccg
//        setRightLabel(getString(R.string.label_next));
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.po_authenticate;
    }

    @Override
    public String getTitleString() {
        return null;
    }

    @Override
    protected void initView() {
        parseArgs();
        //ccg title getString(R.string.po_authentication_title);
        mTvBillingPassword = (TextView) findViewById(R.id.tv_enterBillingPassword);
        mTvSsn = (TextView) findViewById(R.id.tv_enterSsn);
        mEtPassword = (EditText) findViewById(R.id.et_txtPassword);
        mTvAnotherWay = (TextView) findViewById(R.id.tv_billing_passwd_another_way);
        mTvWhatIsBillingPasswd = (TextView) findViewById(R.id.tv_what_is_billing_passwd);

        /*mBtnSwitchInput = (Button) findViewById(R.id.btn_switch_input);
        mBtnVerify = (Button) findViewById(R.id.btn_verify);*/
        mEmergencyBtn = (Button) findViewById(R.id.emergency_button_btn);
        mRightBtn = (Button) findViewById(R.id.right_btn);
        /*mTitle = (TextView) findViewById(R.id.page_header);
        mTitle.setText(R.string.po_authentication_title);*/
        mRightBtn.setText(R.string.label_next);
    }

    @Override
    protected void initAction() {
        mTvAnotherWay.setOnClickListener(v -> switchType());
        //mBtnSwitchInput.setOnClickListener(this);
        /*mBtnVerify.setOnClickListener(v -> {
            String passwd = mEtPassword.getText().toString().trim();
            if (TextUtils.isEmpty(passwd)) {
                Toast.makeText(getApplicationContext(), "input is null", Toast.LENGTH_SHORT).show();
                return;
            }

            mReqRetry = 0;
            doVerify(passwd);
        });*/

        if (mSecurityQID == 1) { /// Billing Password Exist
            mTvAnotherWay.setVisibility(View.VISIBLE);
            //mTvWhatIsBillingPasswd.setVisibility(View.VISIBLE);
            //mBtnSwitchInput.setVisibility(View.VISIBLE);
        }

        mTvSsn.setVisibility(View.VISIBLE);

        mRightBtn.setOnClickListener(v -> {
            mPasswd = mEtPassword.getText().toString().trim();
            if (TextUtils.isEmpty(mPasswd)) {
                Toast.makeText(this, "input is null", Toast.LENGTH_SHORT).show();
                return ;
            }

            doVerify(mPasswd);
        });
    }

    private void switchType() {
        if (DEBUG) {
            Log.e(TAG, "switchType mInputType=" + mInputType);
        }
        mInputType = mInputType == TYPE_1_PSW ? TYPE_2_SSN : TYPE_1_PSW; // 1,Billing Password.
        boolean ssn = mInputType == TYPE_2_SSN; //2,SSN
        mTvBillingPassword.setVisibility(ssn ? View.GONE : View.VISIBLE);
        mTvSsn.setVisibility(ssn ? View.VISIBLE : View.GONE);
        mEtPassword.setInputType(ssn ? InputType.TYPE_NUMBER_VARIATION_PASSWORD | InputType.TYPE_CLASS_NUMBER :
                InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_CLASS_TEXT);

        mTvAnotherWay.setText(Html.fromHtml("<u>" + getString(ssn ? R.string.billing_passwd_another_way : R.string.ssn_another_way) + "</u>"));
        if (mInputType == TYPE_1_PSW) {
            mTvWhatIsBillingPasswd.setVisibility(View.VISIBLE);
            mTvWhatIsBillingPasswd.setText(R.string.what_is_pin);
        } else {
            mTvWhatIsBillingPasswd.setVisibility(View.GONE);
        }

        mEtPassword.setText(""); // clear
        mEtPassword.requestFocus();
    }

    private void parseArgs() {
        Bundle args = getIntent().getBundleExtra(PoaCommon.ARGS);
        if (args != null) {
            mCorrelationID = args.getString("mCorrelationID");
            mRequestID = args.getString("mRequestID");
            mSecurityQID = args.getInt("mSecurityQuestionID", 0);
            mOrderType = args.getInt("mOrderType");
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        removeMessages(MSG_RETRY_VALIDATE_ORDER_REQ); // remove delay msg
        cancelValidateTask();
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
                showPoaStatus(VzwPoaStatusActivity.UpgradeOrderPasswordAuthFailed);
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
        //start to VzwPoaStatusActivity
        Intent intent = WizardManagerHelper.getNextIntent(getIntent(), 201);
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
        args.putString("mSecurityQuestionID", mInputType == TYPE_1_PSW ? SECURITY_QUESTION_ID_001 : SECURITY_QUESTION_ID_002);
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
            startService(intent);
            //finishSetup(getActivity());
            finish();
            this.overridePendingTransition(R.anim.anim_right_in, R.anim.anim_left_out);
        } else {
            if (DEBUG) {
                Log.e(TAG, "startActivationPanel mOrderType=" + mOrderType);
            }
            //start to VzwPendingOrderActivationActivity
            Intent intent = WizardManagerHelper.getNextIntent(getIntent(), 203);
            intent.putExtra(PoaCommon.ARGS, args);
            startActivityPanel(intent);
        }

    }

    private void doVerify(String passwd) {
        cancelValidateTask();

        String sqid = mInputType == TYPE_1_PSW ? SECURITY_QUESTION_ID_001 : SECURITY_QUESTION_ID_002;
        Log.e(TAG, "mInputType=" + mInputType + " sqid=" + sqid);
        if (DEBUG) {
            //Toast.makeText(getContext(), "Validate Order current attempts :" + mAttempts, Toast.LENGTH_LONG).show();
            Log.d(TAG, "Validate Order current attempts :" + mAttempts);
        }
        showVerifyDialog();
        removeMessages(MSG_RETRY_VALIDATE_ORDER_REQ);

        mValidateTask = new ValidateTask(sqid, mCorrelationID, mRequestID, passwd);
        mValidateTask.execute();
    }

    private void cancelValidateTask() {
        if (mValidateTask != null && mValidateTask.getStatus() != AsyncTask.Status.FINISHED) {
            if (DEBUG) {
                Log.e(TAG, "task status=" + mValidateTask.getStatus());
            }
            mValidateTask.cancel(true);
            mValidateTask = null;
        }
    }

    private void showVerifyDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setOnDismissListener(this);
            mProgressDialog.setMessage(getString(R.string.po_authentication_verifying));
        }

        if (!mProgressDialog.isShowing()) {
            mProgressDialog.show();
        }
    }

    public void onclickEmergencyCall(View view) {
        Utils.onclickEmergencyCall(this);
    }
}
