package com.odm.setupwizardoverlay;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;

import java.io.UnsupportedEncodingException;


public class VzwPlanSelection extends Activity {
    private static final String TAG = VzwPlanSelection.class.getSimpleName();

    public static final String ACTION_VZW_PLAN_SELECTION_FROM_SETUP =
            "com.odm.setupwizardoverlay.VZW_PLAN_SELECTION_FROM_SETUP";
    public static final String ACTION_VZW_PLAN_SELECTION =
            "com.odm.setupwizardoverlay.VZW_PLAN_SELECTION";

    private static final int HANDLE_ACTION_ACTIVATE_SUCCESS = 0;
    private static final int HANDLE_ACTION_REBOOT = 1;
    private static final int HANDLE_ACTION_POST_URL = 2;
    private static final int HANDLE_ACTION_POST_ERROR_CODE = 3;

    private WebView mWebView;
    private ProgressBar mProgressBar;
    private Button mExitBtn;

    private String postUrl;
    private byte[] postParamsByte;

    private boolean isFromSetupWizard = false;
    private boolean isSuccess = false;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (Utils.DEBUG) Log.d(TAG, "requestCode=" + requestCode + " ,resultCode=" + resultCode);
        switch (requestCode) {
            case Constants.REQUEST_CODE_NEXT:
                if (resultCode == Constants.RESULT_CODE_NEXT) {
                    getNext();
                }
                break;
            case Constants.REQUEST_CODE_PLAN_SELECTION:
                switch (resultCode) {
                    case Constants.RESULT_CODE_NEXT:
                        getNext();
                        break;
                    case Constants.RESULT_CODE_BACK:
                        goBack();
                        break;
                }
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //getWindow().setStatusBarColor(getResources().getColor(R.color.suw_color_accent_glif_dark));
        setContentView(R.layout.web_view);

        mExitBtn = (Button) findViewById(R.id.exit_btn);

        String action = getIntent().getAction();
        if (action != null && action.equals(ACTION_VZW_PLAN_SELECTION_FROM_SETUP)) {
            isFromSetupWizard = true;
            (findViewById(R.id.emergency_button)).setVisibility(View.VISIBLE);
        } else {
            mExitBtn.setText(android.R.string.cancel);
        }

        postUrl = getString(R.string.vzw_data_url);
        mProgressBar = (ProgressBar) findViewById(R.id.progressbar);
        mWebView = (WebView) findViewById(R.id.web_view);

        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView webView, int progress) {
                super.onProgressChanged(webView, progress);
                mProgressBar.setProgress(progress);
                if (progress == 100) {
                    mProgressBar.setVisibility(View.GONE);
                }
            }
        });
        mWebView.setWebViewClient(new WebViewClient() {
            public void onReceivedError(WebView webView, int errorCode, String description, String url) {
                Log.d(TAG, "onReceivedError " + description + " ,errorCode " + errorCode);
                if (errorCode == WebViewClient.ERROR_TIMEOUT) {
                    webView.postUrl(postUrl, postParamsByte);
                } else if (errorCode == WebViewClient.ERROR_HOST_LOOKUP) { // handle special error code
                    Message msg = mHandler.obtainMessage(HANDLE_ACTION_POST_ERROR_CODE);
                    msg.arg1 = errorCode;
                    mHandler.sendMessage(msg);
                } else {
                    mHandler.sendEmptyMessageDelayed(HANDLE_ACTION_POST_URL, 2000);
                }
            }

            public boolean shouldOverrideUrlLoading(WebView webView, String url) {
                Log.d(TAG, "shouldOverrideUrlLoading url = " + url);
                return false;
            }
        });
        mWebView.addJavascriptInterface(this, "orderStatus");
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String iccid = telephonyManager.getSimSerialNumber();
        String imei = telephonyManager.getDeviceId();
        String postParams = "iccid=" + iccid + "&imei=" + imei;
        Log.d(TAG, postParams);
        try {
            postParamsByte = postParams.getBytes("US-ASCII");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        WebSettings settings = mWebView.getSettings();
        settings.setDefaultFontSize(12);
        settings.setDefaultFixedFontSize(12);
        settings.setMinimumFontSize(7);
        settings.setMinimumLogicalFontSize(7);

        mWebView.postUrl(postUrl, postParamsByte);
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case HANDLE_ACTION_ACTIVATE_SUCCESS:
                    mExitBtn.setText(R.string.restart);
                    break;
                case HANDLE_ACTION_REBOOT:
                    ((PowerManager) getSystemService(Context.POWER_SERVICE)).reboot("");
                    break;
                case HANDLE_ACTION_POST_URL:
                    mWebView.postUrl(postUrl, postParamsByte);
                    break;
                case HANDLE_ACTION_POST_ERROR_CODE:
                    handlePostSpecialErrorCode(msg);
                    break;
                default:
                    break;
            }
        }
    };

    private void handlePostSpecialErrorCode(Message msg) {
        int errorCode = msg.arg1;
        switch (errorCode) {
            case WebViewClient.ERROR_HOST_LOOKUP:
                mHandler.removeCallbacksAndMessages(null);
                // start sim status activity
                Intent intent = new Intent(VzwPlanSelection.this, ShowSimStatusActivity.class);
                intent.putExtra(Constants.KEY_SIM_STATUS, Constants.ACTION_SHOW_NOT_ACTIVATED);
                intent.putExtra(Constants.KEY_SIM_FROM_WHERE, Constants.KEY_SIM_FROM_PLAN_SELECTION);
                startActivityForResult(intent, Constants.REQUEST_CODE_PLAN_SELECTION);
                Log.d(TAG, "start sim status activity");
                break;
        }
    }

    public void onclickExitPlan(View view) {
        Log.d(TAG, "onclickExitPlan");

        if (isFromSetupWizard && !isSuccess) {
            Intent intent = new Intent(this, VzwDataPlan.class);
            startActivityForResult(intent, Constants.REQUEST_CODE_NEXT);
        } else if (isSuccess) {
            ((PowerManager) getSystemService(Context.POWER_SERVICE)).reboot("");
        } else {
            getNext();
        }
    }

    @JavascriptInterface
    public void onSuccess() {
        Log.d(TAG, "onSuccess");

        isSuccess = true;
        mHandler.sendEmptyMessage(HANDLE_ACTION_ACTIVATE_SUCCESS);
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed");

        if (isFromSetupWizard) {
            Intent intent = new Intent(this, VzwDataPlan.class);
            startActivityForResult(intent, Constants.REQUEST_CODE_NEXT);
        } else {
            getNext();
        }
    }

    public void onclickEmergencyCall(View view) {
        Intent intent = new Intent("com.android.phone.EmergencyDialer.DIAL");
        startActivity(intent);
    }

    @JavascriptInterface
    public void closeWebview() {
        Log.d(TAG, "closeWebview");
        if (isSuccess) {
            ((PowerManager) getSystemService(Context.POWER_SERVICE)).reboot("");
        } else {
            getNext();
        }
    }

    private void getNext() {
        setResult(Constants.RESULT_CODE_NEXT);
        finish();
    }

    private void goBack() {
        setResult(Constants.RESULT_CODE_BACK);
        finish();
    }

    @Override
    protected void onDestroy() {
        if (mWebView != null) {
            mWebView.destroy();
        }
        mHandler.removeMessages(HANDLE_ACTION_POST_URL);
        super.onDestroy();
    }
}
