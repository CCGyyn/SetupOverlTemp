package com.odm.setupwizardoverlay;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class VzwDataPlan extends Activity {
    private static final String TAG = VzwDataPlan.class.getSimpleName();

    protected void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);
        //etWindow().setStatusBarColor(getResources().getColor(R.color.suw_color_accent_glif_dark));
        setContentView(R.layout.data_plan);
    }

    public void onclickEmergencyCall(View paramView) {
        Intent intent = new Intent("com.android.phone.EmergencyDialer.DIAL");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    public void onclickReturn(View view) {
        setResult(Constants.RESULT_CODE_BACK);
        finish();
    }

    @Override
    public void onBackPressed() {
        setResult(Constants.RESULT_CODE_BACK);
        super.onBackPressed();
    }

    public void onclickSkip(View view) {
        setResult(Constants.RESULT_CODE_NEXT);
        finish();
    }
}

