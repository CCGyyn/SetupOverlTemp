package com.odm.setupwizardoverlay;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.support.v7.widget.SwitchCompat;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import com.android.setupwizardlib.util.WizardManagerHelper;
import com.google.gson.Gson;
import com.odm.setupwizardoverlay.data.VzwSetupJsonData;

import org.json.JSONObject;

import static com.odm.setupwizardoverlay.data.VzwSetupJsonData.VzwSetupPage.Element.Type.TYPE_CHECKBOX;
import static com.odm.setupwizardoverlay.data.VzwSetupJsonData.VzwSetupPage.Element.Type.TYPE_HEADER;
import static com.odm.setupwizardoverlay.data.VzwSetupJsonData.VzwSetupPage.Element.Type.TYPE_LINE_BREAK;
import static com.odm.setupwizardoverlay.data.VzwSetupJsonData.VzwSetupPage.Element.Type.TYPE_NAV_BUTTON;
import static com.odm.setupwizardoverlay.data.VzwSetupJsonData.VzwSetupPage.Element.Type.TYPE_RADIO_GROUP;
import static com.odm.setupwizardoverlay.data.VzwSetupJsonData.VzwSetupPage.Element.Type.TYPE_TEXT;
import static com.odm.setupwizardoverlay.data.VzwSetupJsonData.VzwSetupPage.Element.Type.TYPE_TOGGLE;


public class VzwCloudSetupActivity extends Activity implements NavigationBar.NavigationBarListener {
    private static final String TAG = VzwCloudSetupActivity.class.getSimpleName();
    private static final String PACKAGE_COM_VCAST_MEDIAMANAGER = "com.vcast.mediamanager"; // cloud app package
    private static final boolean DEBUG = true;

    private Context mContext;

    private NavigationBar mNavigationBar;
    private VzwSetupJsonData mCloudJsonData;
    private TextView mTitle;
    private ImageView mTitleIcon;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Constants.RESULT_CODE_NEXT) {
            onNavigateNext();
        } else if (resultCode == Constants.RESULT_CODE_BACK) {
            onBackPressed();
        }
    }

    public void setTitleText(String titleText) {
        if (mTitle != null && !TextUtils.isEmpty(titleText)) {
            mTitle.setText(titleText);
        }
    }

    public String getTitleText() {
        if (mTitle == null) return "";
        return mTitle.getText().toString();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //getWindow().setStatusBarColor(getResources().getColor(R.color.suw_color_accent_glif_dark));
        setContentView(R.layout.vzw_cloud_setup);
        mContext = this;

        mTitle = findViewById(R.id.page_header);
        mTitleIcon = findViewById(R.id.title_icon);
        setTitleText("Verizon cloud");  // pre set title
        mTitleIcon.setImageResource(R.drawable.cloud_icon);

        if (isCloudSkipped()) {
            // next page
            finishCloudSetup();
            return;
        }

        String layoutJson = getCloudLayoutFromJson();
        if (TextUtils.isEmpty(layoutJson)) {
            finishCloudSetup();
            return;
        }

        try {
            parseLayoutJson(layoutJson);
            if (mCloudJsonData == null || mCloudJsonData.getComponents() == null || mCloudJsonData.getComponents().size() <= 0) {
                finishCloudSetup();
                return;
            }
            // default page index 0
            VzwCloudSetupFragment fragment = VzwCloudSetupFragment.newInstance(mCloudJsonData.getComponents().get(0), 0, Integer.MIN_VALUE);
            getFragmentManager().beginTransaction().replace(R.id.fragments_container, fragment).commit();
        } catch (Exception e) {
            finishCloudSetup();
            e.printStackTrace();
        }
    }

    private boolean isCloudSkipped() {
        // cloud app not installed
        if (!isCloudAppInstalled()) {
            Log.d(TAG, "Cloud App not Installed, skip!");
            return true;
        }

        // no sim card
        if (!Utils.hasSimCard(getApplicationContext())) {
            Log.d(TAG, "no sim card found, skip!");
            return true;
        }

        // get activation status from secure setting
        if (Utils.isPhoneActivatedSuccess(this)) {
            Log.d(TAG, "read secure setting. phone activated, no skip!");
            return false;
        }

        // try to get current mdn
        String curMdn = Utils.getMDN(mContext);
        //if(Utils.DEBUG) Log.d(TAG, "try to get current mdn " + curMdn);
        if (Utils.isValidMdn(curMdn)) {
            Log.d(TAG, "curMdn is valid. no skip!");
            return false;
        }

        try { //  with device protected storage, sp may cause exception in new version
            // get activation status
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
            String mdn = sharedPrefs.getString(Constants.KEY_SIM_MDN, null);
            int pco = sharedPrefs.getInt(Constants.KEY_PCO_DATA, Constants.PCO_DATA_NONE);
            if (Utils.DEBUG) Log.d(TAG, "mdn=" + mdn + ", pco=" + pco);
            if (!Utils.isValidMdn(mdn) && pco != Constants.PCO_DATA_0) {
                Log.d(TAG, "phone not activated, skip!");
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    private void parseLayoutJson(String layoutJson) {
        Gson gson = new Gson();
        mCloudJsonData = gson.fromJson(layoutJson, VzwSetupJsonData.class);
    }

    private String getCloudLayoutFromJson() {
        String locale = Locale.getDefault().toString();
        String assetsJsonName = "en_hux_userflow_no_google_contact.json";
        if (locale.contains("es")) {
            assetsJsonName = "es_hux_userflow_no_google_contact.json";
        }

        Resources resources = Utils.getResourcesForApplication(this, PACKAGE_COM_VCAST_MEDIAMANAGER);
        if (resources != null) {
            InputStream inputStream = null;
            try {
                inputStream = resources.getAssets().open(assetsJsonName);
                String jsonData = Utils.parseJsonFromInputStream(inputStream);
                if (DEBUG) Log.d(TAG, "jsonData=" + jsonData);
                return jsonData;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    public static class VzwCloudSetupFragment extends Fragment implements NavigationBar.NavigationBarListener {
        protected static final String KEY_PAGE = "page";
        protected static final String KEY_PAGE_INDEX = "pageIndex";
        protected static final String KEY_LAST_PAGE_INDEX = "lastPageIndex";

        private VzwSetupJsonData.VzwSetupPage mPage;
        private int mPageIndex;
        private int mLastPageIndex;

        public static VzwCloudSetupFragment newInstance(VzwSetupJsonData.VzwSetupPage page, int pageIndex,int lastPageIndex) {
            Bundle args = new Bundle();
            args.putSerializable(KEY_PAGE, page);
            args.putInt(KEY_PAGE_INDEX, pageIndex);
            args.putInt(KEY_LAST_PAGE_INDEX, lastPageIndex);
            VzwCloudSetupFragment fragment = new VzwCloudSetupFragment();
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            Bundle arguments = getArguments();
            mPage = (VzwSetupJsonData.VzwSetupPage) arguments.getSerializable(KEY_PAGE);
            mPageIndex = arguments.getInt(KEY_PAGE_INDEX);
            mLastPageIndex = arguments.getInt(KEY_LAST_PAGE_INDEX);
            List<VzwSetupJsonData.VzwSetupPage.Element> elements = mPage.getElements();
            ViewGroup root = (ViewGroup) LayoutInflater.from(getActivity()).inflate(R.layout.layout_cloud_page_fragment, null);
            for (VzwSetupJsonData.VzwSetupPage.Element e : elements) {
                switch (e.getType()) {
                    case TYPE_HEADER:
                        /*TextView header = (TextView) LayoutInflater.from(getActivity()).inflate(R.layout.item_title_textview_page, root, false);
                        header.setText(e.getContent());
                        root.addView(header);*/
                        if ((getActivity() instanceof VzwCloudSetupActivity) && !TextUtils.isEmpty(e.getContent())) {
                            ((VzwCloudSetupActivity) getActivity()).setTitleText(e.getContent());
                        }
                        break;
                    case TYPE_LINE_BREAK:
                        break;
                    case TYPE_TEXT:
                        TextView text = (TextView) LayoutInflater.from(getActivity()).inflate(R.layout.item_textview_page, root, false);
                        CharSequence content = Html.fromHtml(e.getContent());
                        if (DEBUG) Log.d(TAG, "getContent=" + e.getContent() + " ,\ncontent=" + content);
                        if (TextUtils.isEmpty(content)) {
                            text.setText(e.getContent());
                        } else {
                            int words = Utils.wordsOfString(String.valueOf(content));
                            if (words <= 5 && words > 0) { // assume this is title
                                String textFromHtml = HtmlUtil.delHTMLTag(e.getContent());
                                if (DEBUG) Log.d(TAG, "textFromHtml=" + textFromHtml);
                                float titleSize = getResources().getDimension(R.dimen.suw_title_size);
                                text.setTextSize(TypedValue.COMPLEX_UNIT_PX,titleSize); // in px
                                text.setPadding(text.getPaddingLeft(), 5, text.getPaddingRight(), 5);
                                text.setTextColor(Color.BLACK);
                                text.setText(textFromHtml);
                            } else {
                                if (!TextUtils.isEmpty(e.getContent())&&e.getContent().startsWith("<p>")) {
                                    String textFromHtml = HtmlUtil.delHTMLTag(e.getContent());
                                    // set paragraph text padding
                                    int paragraphPadding = (int)getResources().getDimension(R.dimen.dimen_paragraph_vertical_padding);
                                    // check if previous element is title
                                    boolean isPrevEleTitle = isPreviousElementTitle(elements, e);
                                    text.setPadding(text.getPaddingLeft(), isPrevEleTitle ? 0 : paragraphPadding, text.getPaddingRight(), paragraphPadding);
                                    text.setText(textFromHtml);
                                } else {
                                    text.setPadding(text.getPaddingLeft(), 0, text.getPaddingRight(), 0);
                                    text.setText(content);
                                }
                            }
                        }

                        if (!TextUtils.isEmpty(content) && content.toString().contains("Terms")) {
                            setClickTermsAndConditions(text, content.toString());
                        }
                        root.addView(text);
                        break;
                    case TYPE_CHECKBOX:
                        CheckBox box = (CheckBox) LayoutInflater.from(getActivity()).inflate(R.layout.item_checkbox_page, root, false);
                        String boxId = e.getId();
                        box.setTag(boxId);  // set id to tag
                        box.setChecked(Boolean.parseBoolean(e.getValue()));
                        box.setText(e.getLabel());
                        box.setId(TextUtils.isEmpty(boxId) ? (int) System.currentTimeMillis() : boxId.hashCode());
                        box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
                        root.addView(box);
                        break;
                    case TYPE_RADIO_GROUP:
                        // radio header
                        TextView radioHeader = (TextView) LayoutInflater.from(getActivity()).inflate(R.layout.item_textview_page, root, false);
                        radioHeader.setText(Html.fromHtml(e.getHeader()));
                        root.addView(radioHeader);

                        // add radio group
                        List<String> items = e.getItems();
                        RadioGroup radioGroup = new RadioGroup(getActivity());
                        String rgId = e.getId();
                        radioGroup.setTag(rgId);  // set id
                        radioGroup.setId(TextUtils.isEmpty(rgId) ? (int) System.currentTimeMillis() : rgId.hashCode());
                        for (int i = 0; i < items.size(); i++) {
                            String item = items.get(i);
                            RadioButton rb = (RadioButton) LayoutInflater.from(getActivity()).inflate(R.layout.item_radiobutton_page, root, false);
                            if (i == Integer.parseInt(e.getValue())) {
                                rb.setChecked(true);
                            }
                            rb.setText(item);
                            rb.setId(i);
                            rb.setTag(String.valueOf(i));
                            radioGroup.addView(rb);
                        }
                        root.addView(radioGroup);
                        // set radioGroup margin
                        Utils.setViewLayoutMargins(radioGroup,5, 35, 5, 50);
                        break;
                    case TYPE_TOGGLE:
                        ViewGroup toggleItem = (ViewGroup) LayoutInflater.from(getActivity()).inflate(R.layout.item_toggle_page, root, false);
                        TextView toggleLabel = toggleItem.findViewById(R.id.tv_toggle_label);
                        SwitchCompat toggle = toggleItem.findViewById(R.id.switch_toggle);
                        toggleLabel.setText(e.getLabel());
                        toggleLabel.setTextSize(16);
                        String toggleId = e.getId();
                        toggle.setChecked(Boolean.parseBoolean(e.getValue()));
                        toggle.setTag(e.getId());
                        toggle.setId(TextUtils.isEmpty(toggleId) ? (int) System.currentTimeMillis() : toggleId.hashCode());
                        if (Utils.DEBUG) Log.d(TAG, "toggle getLabel=" + e.getLabel());

                        root.addView(toggleItem);
                        break;
                }
            }
            return root;
        }

        private boolean isPreviousElementTitle(List<VzwSetupJsonData.VzwSetupPage.Element> elements, VzwSetupJsonData.VzwSetupPage.Element e) {
            if (elements == null || e == null) {
                return false;
            }

            boolean isPrevEleTitle = false;
            int curIndex = elements.indexOf(e);
            int preIndex = curIndex - 1;
            if (preIndex >= 0 && preIndex < elements.size()) {
                VzwSetupJsonData.VzwSetupPage.Element preEle = elements.get(preIndex);
                if (preEle != null) {
                    int len = Utils.wordsOfString(String.valueOf(preEle.getContent()));
                    boolean isHeader = TYPE_HEADER.equals(preEle.getType()) || TYPE_LINE_BREAK.equals(preEle.getType());
                    if (len <= 5 && len > 0 && !isHeader) { // assume this is title
                        isPrevEleTitle = true;
                    }
                }
            }
            return isPrevEleTitle;
        }

        private VzwSetupJsonData.VzwSetupPage.Element getNextButtonElement() {
            List<VzwSetupJsonData.VzwSetupPage.Element> elements = mPage.getElements();
            if (elements.size() > 0) {
                VzwSetupJsonData.VzwSetupPage.Element element = elements.get(elements.size() - 1);
                if (TYPE_NAV_BUTTON.equals(element.getType())) {
                    return element;
                }
            }
            return null;
        }

        private VzwSetupJsonData.VzwSetupPage.Element getBackButtonElement() {
            List<VzwSetupJsonData.VzwSetupPage.Element> elements = mPage.getElements();
            if (elements.size() > 1) {
                VzwSetupJsonData.VzwSetupPage.Element element = elements.get(elements.size() - 2);
                if (TYPE_NAV_BUTTON.equals(element.getType())) {
                    return element;
                }
            }
            return null;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            // show nav
            Activity activity = getActivity();
            if (activity != null && activity instanceof VzwCloudSetupActivity) {
                // set title
                //((VzwCloudSetupActivity) activity).setTitleText(mPage.getPageHeader());
                NavigationBar navigationBar = ((VzwCloudSetupActivity) activity).getNavigationBar();
                // get elements
                VzwSetupJsonData.VzwSetupPage.Element nextButtonElement = getNextButtonElement();
                VzwSetupJsonData.VzwSetupPage.Element backButtonElement = getBackButtonElement();
                // set visibility
                navigationBar.getNextButton().setVisibility(nextButtonElement == null ? View.INVISIBLE : View.VISIBLE);
                navigationBar.getBackButton().setVisibility(backButtonElement == null ? View.INVISIBLE : View.VISIBLE);
                // set text
                navigationBar.getNextButton().setText(nextButtonElement != null ? nextButtonElement.getContent() : getString(R.string.setup_wizard_next_button_label));
                navigationBar.getBackButton().setText(backButtonElement != null ? backButtonElement.getContent() : getString(R.string.setup_wizard_back_button_label));
            }
        }


        private void setClickTermsAndConditions(TextView tv, String info) {
            final SpannableStringBuilder style = new SpannableStringBuilder();
            style.append(info);

            //设置部分文字点击事件
            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    getFragmentManager().beginTransaction().addToBackStack(String.valueOf(mPageIndex))
                            .replace(R.id.fragments_container, new VzwCloudTermsAndConditionsFragment()).commit();
                }
            };

            // 设置可点击的部分
            int start = info.lastIndexOf("T");
            style.setSpan(clickableSpan, start, info.length() - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            tv.setText(style);


            //配置给TextView
            tv.setMovementMethod(LinkMovementMethod.getInstance());
            tv.setText(style);
        }

        private int getNextPageIndex(boolean next) {
            List<VzwSetupJsonData.VzwSetupPage.Element> elements = mPage.getElements();
            int pageIndex = -1;
            if (elements.size() >= 2) {
                if (next) {
                    pageIndex = elements.get(elements.size() - 1).getPageIndex();
                } else {
                    pageIndex = elements.get(elements.size() - 2).getPageIndex();
                }
            }
            return pageIndex;
        }

        @Override
        public void onNavigateBack() {
            int nextPageIndex = getNextPageIndex(false);
            if (Utils.DEBUG) Log.d(TAG, "onNavigateBack mPageIndex=" + mPageIndex + ", nextPageIndex=" + nextPageIndex);
            if (nextPageIndex == mLastPageIndex) {
                getFragmentManager().popBackStack();
            } else {
                if (getActivity() instanceof VzwCloudSetupActivity) {
                    ((VzwCloudSetupActivity) getActivity()).launchNextPage(mPageIndex, nextPageIndex);
                }
            }
        }

        @Override
        public void onNavigateNext() {
            VzwCloudSetupActivity activity = (VzwCloudSetupActivity) getActivity();
            if (mPageIndex == 0) { // first page, start cloud setup
                activity.startCloudSetup();
            }

            int nextPageIndex = getNextPageIndex(true);
            if (Utils.DEBUG) Log.d(TAG, "onNavigateNext mPageIndex=" + mPageIndex + ", nextPageIndex=" + nextPageIndex);

            // send data to cloud app
            String selectionsData = getSelectionsJsonData(mPage);
            if (Utils.DEBUG) Log.d(TAG, "selectionsData=" + selectionsData);
            if (!TextUtils.isEmpty(selectionsData) && !selectionsData.equals("{}")) {
                activity.sendCloudSelectons(selectionsData);
            }

            // no next page, end setup cloud
            if (nextPageIndex < 0) {
                activity.endCloudSetup();
            }

            activity.launchNextPage(mPageIndex,nextPageIndex);
        }

        protected String getSelectionsJsonData(VzwSetupJsonData.VzwSetupPage page) {
            JSONObject selectionsJson = new JSONObject();
            for (VzwSetupJsonData.VzwSetupPage.Element element : page.getElements()) {
                String id = element.getId();
                if (TextUtils.isEmpty(id)) {
                    continue;
                }

                if (Utils.DEBUG) Log.d(TAG, "getSelectionsJsonData element.getType=" + element.getType());

                try {
                    int viewId = id.hashCode(); // use hashCode as the view's id
                    switch (element.getType()) {
                        case TYPE_CHECKBOX: {
                            CheckBox box = (CheckBox) getView().findViewById(viewId);
                            selectionsJson.put(id, String.valueOf(box.isChecked()));
                            if (Utils.DEBUG) Log.d(TAG, "getSelectionsJsonData id=" + id + " ,isChecked=" + box.isChecked());

                            break;
                        }

                        case TYPE_RADIO_GROUP:{
                            RadioGroup radioGroup = (RadioGroup)getView().findViewById(viewId);
                            for (int j = 0; j < radioGroup.getChildCount(); j++) {
                                View rb = radioGroup.getChildAt(j);
                                if (rb instanceof RadioButton) {
                                    boolean checked = ((RadioButton) rb).isChecked();
                                    if (checked) {
                                        selectionsJson.put(id, rb.getTag());
                                        if (Utils.DEBUG) Log.d(TAG, "getSelectionsJsonData id=" + id + " ,isChecked=" + checked+" ,rb=" + rb.getTag());
                                    }
                                }
                            }

                            break;
                        }

                        case TYPE_TOGGLE: {
                            SwitchCompat toggle = getView().findViewById(viewId);
                            selectionsJson.put(id, String.valueOf(toggle.isChecked()));
                            if (Utils.DEBUG) Log.d(TAG, "getSelectionsJsonData id=" + id + " ,isChecked=" + toggle.isChecked());

                            break;
                        }
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                    continue;
                }
            }

            return selectionsJson.toString();
        }

        @Override
        public void onNavigationButtonCreated(NavigationBar navigationBar) {
        }
    }

    public static class VzwCloudTermsAndConditionsFragment extends Fragment implements NavigationBar.NavigationBarListener{

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            TextView tv = (TextView) inflater.inflate(R.layout.item_textview_page, container, false);
            tv.setText(R.string.terms_conditions);
            tv.setPadding(20,30,20,20);
            return tv;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            Activity activity = getActivity();
            if (activity != null && activity instanceof VzwCloudSetupActivity) {
                // set title
                ((VzwCloudSetupActivity) activity).setTitleText(getString(R.string.terms_conditions_title));
                NavigationBar navigationBar = ((VzwCloudSetupActivity) activity).getNavigationBar();
                navigationBar.getNextButton().setVisibility(View.INVISIBLE);
                navigationBar.getBackButton().setVisibility(View.VISIBLE);
                navigationBar.getBackButton().setText(getString(R.string.setup_wizard_back_button_label));
            }
        }

        @Override
        public void onNavigateBack() {
            getFragmentManager().popBackStack();
        }

        @Override
        public void onNavigateNext() {}

        @Override
        public void onNavigationButtonCreated(NavigationBar navigationBar) {}
    }

    private void launchNextPage(int curPage, int nextPage) {
        List<VzwSetupJsonData.VzwSetupPage> components = mCloudJsonData.getComponents();
        if (nextPage >= 0 && nextPage < components.size()) {
            VzwSetupJsonData.VzwSetupPage vzwCloudSetupPage = components.get(nextPage);

            VzwCloudSetupFragment fragment = VzwCloudSetupFragment.newInstance(vzwCloudSetupPage, nextPage, curPage);
            getFragmentManager().beginTransaction()
                    .addToBackStack(String.valueOf(curPage))
                    .replace(R.id.fragments_container, fragment)
                    .commit();

        } else if (nextPage < 0) {
            finishCloudSetup();
        }
    }

    private void startCloudSetup() {
        Intent intent = new Intent("com.vcast.mediamanager.START_CLOUD");
        intent.setPackage("com.vcast.mediamanager");
        intent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND | Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND);
        sendBroadcast(intent);
        System.out.println(">>>>startCloudSetup");
    }

    private void sendCloudSelectons(String selections) {
        Intent intent = new Intent("com.vcast.mediamanager.ReceiverSelections.SEND_SELECTIONS");
        intent.setPackage("com.vcast.mediamanager");
        intent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND | Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND);
        intent.putExtra("com.vcast.mediamanager.ReceiverSelections.SELECTIONS_OBJECT", selections);
        sendBroadcast(intent);
        System.out.println(">>>>sendCloudSelectons selections=" + selections);
    }

    private void endCloudSetup() {
        Intent intent = new Intent("com.vcast.mediamanager.END_SETUP");
        intent.setPackage("com.vcast.mediamanager");
        intent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND | Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND);
        sendBroadcast(intent);
        System.out.println(">>>>endCloudSetup");
    }

    private void finishCloudSetup() {
        int resultCode = Activity.RESULT_OK;
        Intent intent = WizardManagerHelper.getNextIntent(getIntent(), resultCode);
        startActivityForResult(intent, Constants.REQUEST_CODE_NEXT);
        finish();
    }

    public NavigationBar getNavigationBar() {
        return mNavigationBar;
    }

    private boolean isCloudAppInstalled() {
        PackageInfo packageInfo = null;
        try {
            packageInfo = mContext.getPackageManager().getPackageInfo(PACKAGE_COM_VCAST_MEDIAMANAGER, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            packageInfo = null;
        }

        return packageInfo != null;
    }


    @Override
    public void onBackPressed() {
        setResult(Constants.RESULT_CODE_BACK);
        super.onBackPressed();
    }

    private Fragment getTopFragment() {
        List<Fragment> fragments = getFragmentManager().getFragments();
        for (Fragment fragment : fragments) {
            if (fragment instanceof NavigationBar) {
                continue;
            }

            if (fragment.isVisible() && fragment.isResumed()) {
                return fragment;
            }
        }

        return fragments.get(0);
    }

    @Override
    public void onNavigateBack() {
        Fragment topFragment = getTopFragment();
        Log.e(TAG, "onNavigateBack  "+(topFragment));
        if (topFragment instanceof NavigationBar.NavigationBarListener) {
            ((NavigationBar.NavigationBarListener) topFragment).onNavigateBack();
        }
    }

    @Override
    public void onNavigateNext() {

        Fragment topFragment = getTopFragment();
        Log.e(TAG, "onNavigateNext  "+(topFragment));
        if (topFragment instanceof NavigationBar.NavigationBarListener) {
            ((NavigationBar.NavigationBarListener) topFragment).onNavigateNext();
        }
    }

    @Override
    public void onNavigationButtonCreated(NavigationBar navigationBar) {
        mNavigationBar = navigationBar;
        mNavigationBar.getNextButton().setText(R.string.suw_next_button_label);
        mNavigationBar.getBackButton().setVisibility(View.VISIBLE);
        setButtonStyle(mNavigationBar.getBackButton(), false);
    }

    ColorStateList mBtnBackgroundTintList;
    public void setButtonStyle(Button button, boolean hasBackground) {
        if (button == null) return;

        if (mBtnBackgroundTintList == null) { // backup the origin tint list
            mBtnBackgroundTintList = button.getBackgroundTintList();
        }

        if (hasBackground) {
            button.setBackgroundTintList(mBtnBackgroundTintList);
            button.setBackgroundTintMode(PorterDuff.Mode.SRC_IN);
            button.setTextColor(Color.WHITE);
        } else {
            button.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            button.setBackgroundTintMode(PorterDuff.Mode.DST);
            button.setTextColor(Color.BLACK);
        }

    }


    public void onClickEmergencyCall(View view) {
        Utils.onclickEmergencyCall(getApplicationContext());
    }
}
