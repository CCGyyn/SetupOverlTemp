package com.odm.setupwizardoverlay;

import android.app.Activity;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;

public class WebViewScreenActivity extends Activity {

    private static final String TAG = WebViewScreenActivity.class.getSimpleName();

    private Button mBackButton;

    private WebView mCloudView;

    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle extras) {

        super.onCreate(extras);

        setContentView(R.layout.web_view_layout);

        mBackButton = (Button)findViewById(R.id.back_button);

        mCloudView = (WebView)findViewById(R.id.cloud_view);

        mProgressBar = (ProgressBar)findViewById(R.id.progressBar);

        mBackButton.setOnClickListener(v -> {
            if (mCloudView.canGoBack()) {
                mCloudView.goBack();
                return;
            }
            super.onBackPressed();
        });

        final WebSettings settings = mCloudView.getSettings();

        settings.setSaveFormData(false);

        settings.setJavaScriptEnabled(true);

        settings.setSupportZoom(true);

        settings.setBuiltInZoomControls(true);

        mCloudView.setWebViewClient((WebViewClient)new CloudWebViewClient());

        extras = getIntent().getExtras();

        if (extras != null && extras.getString("URL") != null) {

            mCloudView.loadUrl(extras.getString("URL"));

            return;

        }
        finish();
    }



    private class CloudWebViewClient extends WebViewClient

    {

        @Override
        public void onPageFinished(final WebView webView, final String s) {

            super.onPageFinished(webView, s);

            mProgressBar.setVisibility(View.GONE);

            mCloudView.setVisibility(View.VISIBLE);

        }

        @Override
        public void onPageStarted(final WebView webView, final String s, final Bitmap bitmap) {

            super.onPageStarted(webView, s, bitmap);

            mProgressBar.setVisibility(View.VISIBLE);

            mCloudView.setVisibility(View.GONE);

        }

        @Override
        public void onReceivedError(final WebView webView, final int n, final String s, final String s2) {

            if (n != 0) {

                final StringBuilder sb = new StringBuilder();

                sb.append("WebView view = ");

                sb.append(webView);

                Log.d(TAG, sb.toString());

                final StringBuilder sb2 = new StringBuilder();

                sb2.append("WebView errorCode = ");

                sb2.append(n);

                Log.d(TAG, sb2.toString());

                final StringBuilder sb3 = new StringBuilder();

                sb3.append("WebView errorDescr = ");

                sb3.append(s);

                Log.d(TAG, sb3.toString());

                final StringBuilder sb4 = new StringBuilder();

                sb4.append("WebView url = ");

                sb4.append(s2);

                Log.d(TAG, sb4.toString());

            }

            super.onReceivedError(webView, n, s, s2);

        }

        @Override
        public void onReceivedSslError(final WebView webView, final SslErrorHandler sslErrorHandler, final SslError sslError) {

            sslErrorHandler.proceed();

        }

    }
}
