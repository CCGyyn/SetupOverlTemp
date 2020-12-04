package com.odm.setupwizardoverlay;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class NavigationBar extends Fragment implements View.OnClickListener {
    private Button mBackButton;
    private Button mNextButton;
    private View mRootView;
    private NavigationBarListener mCallback;

    public Button getBackButton() {
        return this.mBackButton;
    }

    public Button getNextButton() {
        return this.mNextButton;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCallback = (NavigationBarListener) activity;
    }

    public void onClick(View view) {
        if (view.getId() == R.id.back_button) {
            mCallback.onNavigateBack();
        } else if (view.getId() == R.id.next_button) {
            mCallback.onNavigateNext();
        }
    }

    public View onCreateView(LayoutInflater paramLayoutInflater, ViewGroup paramViewGroup, Bundle paramBundle) {
        mRootView = paramLayoutInflater.inflate(R.layout.navigation_buttons, paramViewGroup, false);
        mBackButton = ((Button) mRootView.findViewById(R.id.back_button));
        mNextButton = ((Button) mRootView.findViewById(R.id.next_button));
        mBackButton.setOnClickListener(this);
        mNextButton.setOnClickListener(this);
        return this.mRootView;
    }

    public void onViewCreated(View paramView, Bundle paramBundle) {
        super.onViewCreated(paramView, paramBundle);
        mCallback.onNavigationButtonCreated(this);
    }

    public static abstract interface NavigationBarListener {
        public abstract void onNavigateBack();
        public abstract void onNavigateNext();
        public abstract void onNavigationButtonCreated(NavigationBar navigationBar);
    }
}
