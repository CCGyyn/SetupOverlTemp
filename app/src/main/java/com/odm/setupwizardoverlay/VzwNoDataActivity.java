package com.odm.setupwizardoverlay;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class VzwNoDataActivity extends Activity implements NavigationBar.NavigationBarListener {
    private static final String TAG = VzwNoDataActivity.class.getSimpleName();

    private NavigationBar mNavigationBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //getWindow().setStatusBarColor(getResources().getColor(R.color.suw_color_accent_glif_dark));
        setContentView(R.layout.wifi_loc_info);
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
        setResult(Constants.RESULT_CODE_NEXT);
        finish();
    }

    @Override
    public void onNavigationButtonCreated(NavigationBar navigationBar) {
        mNavigationBar = navigationBar;
        mNavigationBar.getNextButton().setText(R.string.suw_next_button_label);
    }

    public void onclickEmergencyCall(View view) {
        Intent intent = new Intent("com.android.phone.EmergencyDialer.DIAL");
        startActivity(intent);
    }
}
