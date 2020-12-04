/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.odm.setupwizardoverlay;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.android.setupwizardlib.util.WizardManagerHelper;


/**
 * Application that sets the provisioned bit, like SetupWizard does.
 */
public class SummaryActivity extends PreferenceActivity implements NavigationBar.NavigationBarListener {
    private static final String TAG = SummaryActivity.class.getSimpleName();

    private static final String KEY_VERIZON_CLOUD = "vzw_cloud";
    private static final String KEY_MY_VERIZON = "my_verizon";
    private static final String PACKAGE_COM_VCAST_MEDIAMANAGER = "com.vcast.mediamanager";
    private static final String PACKAGE_COM_VZW_HSS_MYVERIZON = "com.vzw.hss.myverizon";

    private NavigationBar mNavigationBar;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.summary);
        addPreferencesFromResource(R.xml.device_setup);

        updateAppInfo();
    }

    private void updateAppInfo() {
        PreferenceScreen preScreen = getPreferenceScreen();
        PackageManager packageManager = getPackageManager();
        try {
            Drawable vzwCloudIcon = packageManager.getApplicationIcon(PACKAGE_COM_VCAST_MEDIAMANAGER);
            Drawable myVerizonIcon = packageManager.getApplicationIcon(PACKAGE_COM_VZW_HSS_MYVERIZON);

            int vzwCloudEnabled = packageManager.getApplicationEnabledSetting(PACKAGE_COM_VCAST_MEDIAMANAGER);
            int myVerizonEnabled = packageManager.getApplicationEnabledSetting(PACKAGE_COM_VZW_HSS_MYVERIZON);

            Preference preference = preScreen.findPreference(KEY_VERIZON_CLOUD);
            if (preference != null) {
                if (vzwCloudIcon == null || vzwCloudEnabled == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                    preScreen.removePreference(preference);
                } else {
                    preference.setIcon(vzwCloudIcon);
                }
            }

            preference = preScreen.findPreference(KEY_MY_VERIZON);
            if (preference != null) {
                if (myVerizonIcon == null || myVerizonEnabled == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                    preScreen.removePreference(preference);
                } else {
                    preference.setIcon(myVerizonIcon);
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void done() {
        int resultCode = Activity.RESULT_OK;
        Intent intent = WizardManagerHelper.getNextIntent(getIntent(), resultCode);
        startActivityForResult(intent, Constants.REQUEST_CODE_NEXT);
        finish();
    }

    @Override
    public void onNavigateBack() {
    }

    @Override
    public void onNavigateNext() {
        done();
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();  // disable back key
        Log.d(TAG, "onBackPressed");
    }

    @Override
    public void onNavigationButtonCreated(NavigationBar navigationBar) {
       // getWindow().setStatusBarColor(getResources().getColor(R.color.suw_color_accent_glif_dark));
        mNavigationBar = navigationBar;
        mNavigationBar.getNextButton().setText(R.string.done);
    }
}

