package com.odm.setupwizardoverlay;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.os.SystemProperties;

import androidx.annotation.Nullable;

import com.android.setupwizardlib.util.WizardManagerHelper;
import com.mcafee.verizonoobe.aidl.IVZWMMSOOBERegistration;

import java.util.List;

public class VzwDigitalSecureActivity extends Activity implements CompoundButton.OnCheckedChangeListener{

    private static boolean mVZRegisterState = false;

    private final int SET_VZREGISTRATION_TIME = 1500;

    private final int SET_VZSKIP_TIME = 600;

    private final String TAG = VzwDigitalSecureActivity.class.getSimpleName();

    private final String VZPRIVACY_LINK_URL = "https://www.verizon.com/about/privacy/";

    private final String VZSECURITY_LINK_URL = "https://www.verizonwireless.com/support/digital-secure-legal/";

    private Button mVZAcceptButton;

    private Button mVZAcceptButtonGray;

    private Context mContext;

    private Handler mVZHandler = new Handler();

    private String[] mVZLinkUrl = new String[]{"https://www.verizonwireless.com/support/digital-secure-legal/", "https://www.verizon.com/about/privacy/"};

    private String mVZMDN;

    //private boolean mVZPrepaidPCO;

    private TextView mVZPrvacyText;

    private IVZWMMSOOBERegistration mVZRegistrationService;

    private TextView mVZSecurityText;

    private VZSecurityServiceConnection mVZServiceConnection;

    private Button mVZSkipButton;

    private Switch mVZSwitch;

    private TextView termsAndConditions;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mContext = getApplicationContext();
        this.mVZMDN = getSimVZMDN();
        bindService();
        if (!checkVZSecurityActivityShow()) {
            this.mVZHandler.postDelayed(() -> skipRegistration(),  600L);
            launchNextPage(RESULT_OK);
            Log.d(TAG, "The phone has no sim card & no pre-granted phone permission, so skip registration!");
            finish();
            overridePendingTransition(0, 0);
        }
        setContentView(R.layout.activity_digital_secure);
        initView();
        initAction();
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.mVZAcceptButton.setClickable(true);
        this.mVZSkipButton.setClickable(true);
    }

    protected void onDestroy() {
        super.onDestroy();
        unBindService();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        skipRegistration();
    }

    private void initView() {
        mVZPrvacyText = (TextView)findViewById(R.id.vzsecurity_privacy_context);
        mVZSwitch = (Switch)findViewById(R.id.vzsecurity_switch_compat);
        mVZSwitch.setChecked(true);
        mVZSecurityText = (TextView)findViewById(R.id.vzsecurity_terms);
        termsAndConditions = (TextView) findViewById(R.id.terms_and_conditions);
        setVZLinkTextContent();
        mVZSkipButton = (Button)findViewById(R.id.vzsecurity_skip);
        mVZAcceptButton = (Button)findViewById(R.id.vzsecurity_accept);
        mVZAcceptButtonGray = (Button)findViewById(R.id.vzsecurity_accept_gray);
    }

    private void initAction() {
        mVZSkipButton.setOnClickListener(v -> {
            skipRegistration();
            mVZHandler.postDelayed(() -> launchNextPage(RESULT_OK),
            1500L);
            mVZSkipButton.setClickable(false);
        });
        mVZAcceptButton.setOnClickListener(v -> {
            if (!mVZRegisterState) {
                Log.d(this.TAG, "onClick: click next button, start initiate registration!");
                initiateRegistration();
                mVZRegisterState = true;
                mVZHandler.postDelayed(() -> launchNextPage(RESULT_OK),
                1500L);
            } else {
                launchNextPage(RESULT_OK);
            }
            this.mVZAcceptButton.setClickable(false);
        });
        mVZSwitch.setOnCheckedChangeListener(this);
        termsAndConditions.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.odm.setupwizardoverlay", "com.odm.setupwizardoverlay.WebViewScreenActivity"));
            intent.putExtra("URL", "https://www.verizonwireless.com/support/digital-secure-legal/");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK );
            this.mContext.startActivity(intent);
        });
    }

    private void launchNextPage(int resultCode) {
        Intent intent = WizardManagerHelper.getNextIntent(getIntent(), resultCode);
        startActivityForResult(intent,1);
        Log.d(TAG, "launchNextPage startActivityForResult  resultCode=" + resultCode + " ,intent=" + intent);
    }

    @Override
    public void onCheckedChanged(CompoundButton paramCompoundButton, boolean paramBoolean) {
        if (paramBoolean) {
            mVZPrvacyText.setTextColor(android.graphics.Color.BLACK);
            mVZAcceptButton.setVisibility(View.VISIBLE);
            mVZAcceptButtonGray.setVisibility(View.GONE);
            Log.d(TAG, "onCheckedChanged: Accept button is ok, can register the VZSecurity & Privacy!");
            return;
        }
        mVZPrvacyText.setTextColor(android.graphics.Color.GRAY);
        mVZAcceptButton.setVisibility(View.GONE);
        mVZAcceptButtonGray.setVisibility(View.VISIBLE);
        mVZAcceptButtonGray.setEnabled(false);
        Log.d(TAG, "onCheckedChanged: Accept button is not ok, can't register the VZSecurity & Privacy!");
    }

    private boolean checkVZSecurityActivityShow() {
        return (this.mVZMDN != null || hasVZPhonePermission());
    }

    private String getSimVZMDN() {
        TelephonyManager telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        if (hasVZSim(telephonyManager)) {
            String str = telephonyManager.getLine1Number();
            Log.d(TAG, "Get the sim card's phone number: " + str);
            return str;
        }
        Log.d(TAG, "the sim MDN is null");
        return null;
    }

    private boolean hasVZPhonePermission() {
        boolean bool;
        if (getPackageManager().checkPermission("android.permission.READ_PHONE_STATE",
                "com.securityandprivacy.android.verizon.vms") == PackageManager.PERMISSION_GRANTED) {
            bool = true;
        } else {
            bool = false;
        }
        Log.d(TAG, "Check the Verizon Security&Privacy's phone permission: phonePermission = " + bool);
        return bool;
    }

    private boolean hasVZSim(TelephonyManager paramTelephonyManager) {
        if (paramTelephonyManager != null) {
            int i = paramTelephonyManager.getSimState();
            if (i == 0 || i == 1) {
                Log.d(TAG, "no sim card");
                return false;
            }
        }
        return true;
    }

    private void setVZLinkTextContent() {
        String str1 = String.format(getResources().getString(R.string.verizon_vzsecurity_terms),
                new Object[] { "https://www.verizonwireless.com/support/digital-secure-legal/",
                        "https://www.verizon.com/about/privacy/" });
        Log.d(TAG, "setVZLinkTextContent: htmlString = " + str1);
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(Html.fromHtml(str1, 0));
        URLSpan[] arrayOfURLSpan = spannableStringBuilder.getSpans(0, spannableStringBuilder.length(), URLSpan.class);
        int k = arrayOfURLSpan.length;
        int i = 0;
        int j = i;
        while (i < k) {
            URLSpan uRLSpan = arrayOfURLSpan[i];
            int m = spannableStringBuilder.getSpanStart(uRLSpan);
            int n = spannableStringBuilder.getSpanEnd(uRLSpan);
            spannableStringBuilder.removeSpan(uRLSpan);
            spannableStringBuilder.setSpan(new VZNoUnderLineURLSpan(this, this.mVZLinkUrl[j]), m, n, 33);
            spannableStringBuilder.setSpan(new StyleSpan(1), m, n, 33);
            j++;
            i++;
        }
        Log.d(TAG, "setVZLinkTextContent: text show content = " + spannableStringBuilder);
        this.mVZSecurityText.setText(spannableStringBuilder);
        this.mVZSecurityText.setMovementMethod(LinkMovementMethod.getInstance());
        this.mVZSecurityText.setHighlightColor(0);

        termsAndConditions.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
    }

    private void bindService() {
        mVZServiceConnection = new VZSecurityServiceConnection();

        try {
            /*Intent oobeServiceIntent = new Intent("com.mcafee.oobe.aidl.VerizonOOBEService");
            oobeServiceIntent.setPackage("com.securityandprivacy.android.verizon.vms");
            oobeServiceIntent = convertImplicitIntentToExplicitIntent(oobeServiceIntent,
                    this);
            mContext.bindService(oobeServiceIntent, this.mVZServiceConnection,
                    Context.BIND_AUTO_CREATE);*/
            Intent oobeServiceIntent = new Intent();
            ComponentName component = new ComponentName("com.securityandprivacy.android.verizon.vms",
                    "com.mcafee.verizonoobe.VerizonOOBEService");
            oobeServiceIntent.setComponent(component);
            mContext.bindService(oobeServiceIntent, mVZServiceConnection, Context.BIND_AUTO_CREATE);

            Log.d(TAG, "bindService: bind the verizon security service.");
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }

    }

    private void unBindService() {
        mContext.unbindService(mVZServiceConnection);
    }


    private Intent convertImplicitIntentToExplicitIntent(Intent implicitIntent, Context
            context) {
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolveInfoList = pm.queryIntentServices(implicitIntent, 0);
        if (resolveInfoList == null || resolveInfoList.size() != 1) {
            return null;
        }
        ResolveInfo serviceInfo = resolveInfoList.get(0);
        ComponentName component = new
                ComponentName(serviceInfo.serviceInfo.packageName,
                serviceInfo.serviceInfo.name);
        Intent explicitIntent = new Intent(implicitIntent);
        explicitIntent.setComponent(component);
        return explicitIntent;
    }

    private void initiateRegistration() {
        Thread lThread = new Thread(() -> {
            try {
                Bundle regBundle = new Bundle();
                regBundle.putString("MSISDN", mVZMDN);
                mVZRegistrationService.initiateRegistration(regBundle);
                //unBindService();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        lThread.start();
    }

    private void skipRegistration () {
        try {
            Bundle params = new Bundle();
            mVZRegistrationService.skipRegistration(params);
            //unBindService();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class VZNoUnderLineURLSpan extends URLSpan {
        private Context mContext;

        private String mUrl;

        public VZNoUnderLineURLSpan(Context param1Context, String param1String) {
            super(param1String);
            this.mContext = param1Context;
            this.mUrl = param1String;
        }

        @Override
        public void onClick(View param1View) {
            try {
                Log.d(TAG, "onClick: Open the verizon cloud setupwizard's webview activity!");
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("com.odm.setupwizardoverlay", "com.odm.setupwizardoverlay.WebViewScreenActivity"));
                intent.putExtra("URL", this.mUrl);
                this.mContext.startActivity(intent);
                return;
            } catch (Exception exception) {
                exception.printStackTrace();
                return;
            }
        }

        public void updateDrawState(TextPaint param1TextPaint) {
            if (param1TextPaint != null) {
                param1TextPaint.setColor(VzwDigitalSecureActivity.this.getResources().getColor(R.color.material_blue_700));
                param1TextPaint.setUnderlineText(false);
                return;
            }
            Log.e(TAG, "Error: The link-text is null!");
        }
    }

    class VZSecurityServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(VzwDigitalSecureActivity.this.TAG, "VerizonSecurityService is connected ...");
            mVZRegistrationService = IVZWMMSOOBERegistration.Stub.asInterface((IBinder)service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(VzwDigitalSecureActivity.this.TAG, "VerizonSecurityService is disconnected ...");
            mVZRegistrationService = null;
        }
    }
}
