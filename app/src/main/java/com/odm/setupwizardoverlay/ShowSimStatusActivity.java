package com.odm.setupwizardoverlay;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import java.util.Arrays;
import java.util.Locale;


public class ShowSimStatusActivity extends Activity implements NavigationBar.NavigationBarListener, View.OnClickListener {
    private static final String TAG = ShowSimStatusActivity.class.getSimpleName();

    private static final int UI_STEP_NOE = 0;
    private static final int UI_STEP_NO_SIM_NEXT = 1;
    private static final int UI_STEP_2 = 2;
    private static final int UI_STEP_3 = 3;
    private static final int UI_STEP_4 = 4;
    private static final int UI_STEP_5 = 5;

    private Context mContext;

    private NavigationBar mNavigationBar;

    private TextView mWelcomeSubTitleText;
    private TextView mSimStatusText;
    private TextView mEmergencyText;
    private TextView mLocationText;
    private ProgressBar mActProgress;
    private Button mRebootBt;
    private ImageView mSimImage;
    private Button mEmergencyBtn;

    private LinearLayout mSimStatusBodyLayout;

    private int mSimStatus = -1;

    private String mMdn = null;

    private boolean mIsFromNotification;

    private int mUiStep = UI_STEP_NOE;

    private int mDemoModeClickCount;
    private int mDemoModeAttemptCount;
    private int mPco;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Constants.RESULT_CODE_NEXT) {
            onNavigateNext();
        } else if (resultCode == Constants.RESULT_CODE_BACK) {
            onBackPressed();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       // getWindow().setStatusBarColor(getResources().getColor(R.color.suw_color_accent_glif_dark));
       // getWindow().setNavigationBarColor(Color.WHITE);

        setContentView(R.layout.show_sim_status);

        mContext = this;

        Intent intent = getIntent();
        if (intent.hasExtra(Constants.KEY_SIM_STATUS)) { // SIM key
            mSimStatus = intent.getIntExtra(Constants.KEY_SIM_STATUS, Constants.ACTION_SKIP_DISPLAY);
        }
        if (intent.hasExtra(Constants.KEY_SIM_MDN)) { // MDN key
            mMdn = intent.getStringExtra(Constants.KEY_SIM_MDN);
        }

        if (intent.hasExtra(Constants.KEY_PCO_DATA)) { // pco
            mPco = intent.getIntExtra(Constants.KEY_PCO_DATA, -1);
        }

        if (intent.hasExtra(Constants.KEY_SIM_FROM_NOTIFICATION)) { // notification key
            mIsFromNotification = intent.getBooleanExtra(Constants.KEY_SIM_FROM_NOTIFICATION, false);
        }

        initUI();
    }

    @Override
    protected void onResume() {
        super.onResume();

        //disableNextIfSimLocked();
    }

    private void disableNextIfSimLocked() {
        boolean simUnlocked = Utils.isSimUnlocked(this);
        // no sim
        if (!Utils.hasSimCard(this)) {
            if (!simUnlocked) {  // locked
                setNextButtonEnabled(false);
            }
        } else {
            // has sim card
            boolean activated = Utils.isValidMdn(mMdn) || mPco == Constants.PCO_DATA_0;
            if (!activated && !simUnlocked) { // activation fails & Device is locked
                setNextButtonEnabled(false);
            } else {
                setNextButtonEnabled(true);
            }
        }

    }

    private void setNextButtonEnabled(boolean enabled) {
        Button nextButton = mNavigationBar.getNextButton();
        if (nextButton == null) {
            return;
        }

        nextButton.setBackgroundTintList(ColorStateList.valueOf(enabled ? getColor(R.color.suw_color_accent_glif_dark) : Color.GRAY));
        nextButton.setEnabled(enabled);
    }

    private void initUI() {
        Log.d(TAG, "SimStatus: " + mSimStatus);

        TextView pageHeader = (TextView) findViewById(R.id.page_header);
        pageHeader.setOnClickListener(this);

        mSimStatusBodyLayout = (LinearLayout) findViewById(R.id.sim_status_body);
        mSimStatusBodyLayout.setOnClickListener(onClickSimStatusBodyListener);

        mWelcomeSubTitleText = (TextView) findViewById(R.id.welcome_sub_title);
        mSimStatusText = (TextView) findViewById(R.id.sim_status_text);
        mEmergencyText = (TextView) findViewById(R.id.emergency_text);
        mLocationText = (TextView) findViewById(R.id.location_text);
        mLocationText.setOnClickListener(onClickSimStatusBodyListener);
        mActProgress = (ProgressBar) findViewById(R.id.act_progress_indicator);
        mRebootBt = (Button) findViewById(R.id.power_button);
        mSimImage = (ImageView) findViewById(R.id.sim_image);
        mEmergencyBtn = (Button) findViewById(R.id.emergency_button_btn);


        boolean activatedSuccess = Utils.isPhoneActivatedSuccess(this);
        boolean simUnlocked = Utils.isSimUnlocked(this);
        Log.d(TAG, "activatedSuccess=" + activatedSuccess + " ,simUnlocked=" + simUnlocked);
        if (!activatedSuccess && !simUnlocked) {
            setNextButtonEnabled(false);
        }

        switch(mSimStatus) {
            case Constants.ACTION_SHOW_NO_SIM: {
                if (!simUnlocked) {
                    setNextButtonEnabled(false);
                }

                mLocationText.setText(TextUtils.expandTemplate(mContext.getText(R.string.no_sim), "SIM card"));
                mLocationText.setVisibility(View.VISIBLE);
                mSimImage.setBackgroundResource(R.drawable.ic_siminsert_left_hor);
                ((LinearLayout) mSimImage.getParent()).setVisibility(View.VISIBLE);

                mUiStep = UI_STEP_NO_SIM_NEXT;
                break;
            }
            case Constants.ACTION_SIM_NOT_READY:
                Log.d(TAG, "sim is not ready");
                break;
            case Constants.ACTION_SHOW_SIM_ERROR: {
                mLocationText.setText(R.string.corrupt_sim);
                mLocationText.setVisibility(View.VISIBLE);
                ((LinearLayout) mSimImage.getParent()).setVisibility(View.GONE);

                mUiStep = UI_STEP_NO_SIM_NEXT;
                break;
            }
            case Constants.ACTION_SIM_READY:
                Log.d(TAG, "sim is ready");
                break;
            case Constants.ACTION_SKIP_DISPLAY:
            case Constants.ACTION_SHOW_NOT_ACTIVATED:
                Log.d(TAG, "action show not activated " + mSimStatus);
                Intent intent = getIntent();
                if (intent != null && intent.hasExtra(Constants.KEY_SIM_FROM_WHERE)) {
                    String fromWhere = intent.getStringExtra(Constants.KEY_SIM_FROM_WHERE);
                    if (Utils.DEBUG) Log.d(TAG, "fromWhere=" + fromWhere);
                    switch (fromWhere) {
                        case Constants.KEY_SIM_FROM_PLAN_SELECTION:
                            mLocationText.setText(String.format(getString(R.string.cold_sim_msg), getString(R.string.vzw_customer_service)));
                            mLocationText.setVisibility(View.VISIBLE);
                            break;
                    }
                }
                break;
            case Constants.ACTION_SHOW_ACTIVATED: {
                setNextButtonEnabled(true);
                Log.d(TAG, "action show activated");
                mWelcomeSubTitleText.setText(R.string.sim_card_already_activatd_txt);
                mWelcomeSubTitleText.setVisibility(View.VISIBLE);
                String phoneNumber = "unknow";
                if (mMdn != null) {
                    phoneNumber = PhoneNumberUtils.formatNumber(mMdn, Locale.getDefault().getCountry());
                }
                mSimStatusText.setText(phoneNumber);
                mSimStatusText.setVisibility(View.VISIBLE);
                mEmergencyText.setText(R.string.emergency_text);
                mEmergencyText.setVisibility(View.VISIBLE);
                //mLocationText.setText(R.string.loc_text_vzw);
                mLocationText.setVisibility(View.GONE);
                ((LinearLayout) mSimImage.getParent()).setVisibility(View.GONE);
                break;
            }
            case Constants.ACTION_NON_VZW_SIM:
                Log.d(TAG, "non vzw sim");
                mLocationText.setText(R.string.wrong_operator_vzw);
                mLocationText.setVisibility(View.VISIBLE);

                if (mNavigationBar != null) {
                    mNavigationBar.getNextButton().setText(R.string.wi_fi);
                }
                break;
            default: {
                setResult(Constants.RESULT_CODE_NEXT);
                finish();
                break;
            }
        }

        if (mIsFromNotification) {
            mEmergencyBtn.setVisibility(View.GONE);
            mNavigationBar.getNextButton().setText(R.string.ok);
            mUiStep = UI_STEP_NOE;
        }
    }

    private View.OnClickListener onClickSimStatusBodyListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mSimStatus == Constants.ACTION_SHOW_ACTIVATED) {
                mDemoModeClickCount++;
                if (mDemoModeClickCount == 10) {
                    mDemoModeClickCount = 0;
                    showVzwDemoModeDialog();
                }
            }
        }
    };

    private void showVzwDemoModeDialog() {
        Log.d(TAG, "show Vzw Demo Mode Dialog");

        LinearLayout passwordLayout = (LinearLayout)getLayoutInflater().inflate(R.layout.vzw_demo_mode_dlg_layout, null);
        TextView passwordText = (TextView) passwordLayout.findViewById(R.id.password_edit);

        new AlertDialog.Builder(mContext, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle(R.string.vzw_demo_mode_dialog_title)
                .setMessage(R.string.vzw_demo_mode_dialog_msg)
                .setView(passwordLayout)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        doPositiveClick(passwordText.getEditableText().toString());
                    }
                })
                .create().show();
    }

    public void doPositiveClick(String password) {
        Log.d(TAG, "Vzw Demo Mode Dialog do Positive click!");
        String VzwPassword = "#VerizonDemoUnit#";

        if (VzwPassword.equals(password)) {
            mDemoModeAttemptCount = 0;

            PackageManager pm = getPackageManager();
            pm.setApplicationEnabledSetting("com.verizon.llkagent",
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED, 0);

            InputMethodManager inputMgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMgr.toggleSoftInput(InputMethodManager.HIDE_NOT_ALWAYS, 0);

            startActivity(new Intent("com.verizon.llkagent.DEMO_MODE_DIALOG"));
            return;
        }
        mDemoModeAttemptCount++;
        if (mDemoModeAttemptCount < 3) {
            showVzwDemoModeDialog();
            return;
        }
        mDemoModeAttemptCount = 0;
    }

    private void changeUi() {
        switch (mUiStep) {
            case UI_STEP_NO_SIM_NEXT:
                mWelcomeSubTitleText.setText(R.string.wifi_loc_text_1);
                mWelcomeSubTitleText.setVisibility(View.VISIBLE);
                mEmergencyText.setText(R.string.wifi_loc_text_2);
                mEmergencyText.setVisibility(View.VISIBLE);
                mLocationText.setText(R.string.loc_text_vzw);
                mLocationText.setVisibility(View.VISIBLE);
                ((LinearLayout) mSimImage.getParent()).setVisibility(View.GONE);

                mUiStep = UI_STEP_NOE;
                break;
            default:
                break;
        }
    }

    @Override
    public void onBackPressed() {
        setResult(Constants.RESULT_CODE_BACK);
        super.onBackPressed();
    }

    @Override
    public void onNavigateBack() {
    }

    @Override
    public void onNavigateNext() {
        if (mUiStep == UI_STEP_NOE) {
            setResult(Constants.RESULT_CODE_NEXT);
            finish();
        } else {
            Intent intent = new Intent(mContext, VzwNoDataActivity.class);
            startActivityForResult(intent, Constants.REQUEST_CODE_NEXT);
            mUiStep = UI_STEP_NOE;
        }
    }

    @Override
    public void onNavigationButtonCreated(NavigationBar navigationBar) {
        mNavigationBar = navigationBar;
        mNavigationBar.getNextButton().setText(R.string.suw_next_button_label);
    }

    public void onclickEmergencyCall(View view) {
        Utils.onclickEmergencyCall(getApplicationContext());
    }

    private static final int DELAY_TIMER_MILLIS = 6000;
    private static final int TEST_TRIGGER_COUNT = 6;
    private static final String PASSWORD_TEST = "vzwtest996";

    private final long[] mHits = new long[TEST_TRIGGER_COUNT];

    private void onClickPageHeader() { // multiply click page_header to show test dialog
        // arrayCopy
        System.arraycopy(mHits, 1, mHits, 0, mHits.length - 1);
        // record current time
        mHits[mHits.length - 1] = SystemClock.uptimeMillis();
        // check
        if (mHits[0] >= (SystemClock.uptimeMillis() - DELAY_TIMER_MILLIS)) {
            Log.d(TAG, "perform multiply times " + TEST_TRIGGER_COUNT);

            // reset after every match result
            Arrays.fill(mHits, 0);

            showTestPsdConfirmDialog();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.page_header:
                onClickPageHeader();
                break;
        }
    }

    // dialog used to confirm test password.
    // when the password matches the required, we will force to skip this page
    private void showTestPsdConfirmDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm test password");
        EditText et = new EditText(this);
        et.setPadding(30, 90, 30, 30);
        builder.setView(et);
        //builder.setCancelable(false);

        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            String password = et.getText().toString().trim();
            if (PASSWORD_TEST.equals(password)) {
                setResult(Constants.RESULT_CODE_NEXT);
                finish();
            } else {
                et.setText("");
                Toast.makeText(getApplicationContext(), "wrong password.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }
}
