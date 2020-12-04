package com.odm.setupwizardoverlay;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.support.v7.widget.SwitchCompat;

import com.android.setupwizardlib.util.WizardManagerHelper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.odm.setupwizardoverlay.data.VzwSetupJsonData;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.odm.setupwizardoverlay.data.VzwSetupJsonData.VzwSetupPage.Element.Type.TYPE_CHECKBOX;
import static com.odm.setupwizardoverlay.data.VzwSetupJsonData.VzwSetupPage.Element.Type.TYPE_HEADER;
import static com.odm.setupwizardoverlay.data.VzwSetupJsonData.VzwSetupPage.Element.Type.TYPE_LINE_BREAK;
import static com.odm.setupwizardoverlay.data.VzwSetupJsonData.VzwSetupPage.Element.Type.TYPE_NAV_BUTTON;
import static com.odm.setupwizardoverlay.data.VzwSetupJsonData.VzwSetupPage.Element.Type.TYPE_RADIO_GROUP;
import static com.odm.setupwizardoverlay.data.VzwSetupJsonData.VzwSetupPage.Element.Type.TYPE_TEXT;
import static com.odm.setupwizardoverlay.data.VzwSetupJsonData.VzwSetupPage.Element.Type.TYPE_TOGGLE;

public class VzwServicesPrivacyActivity extends Activity implements NavigationBar.NavigationBarListener{
    private static final String TAG = VzwServicesPrivacyActivity.class.getSimpleName();
    private VzwSetupJsonData mPrivacyJsonData;
    private NavigationBar mNavigationBar;
    private ImageView mTitleIcon;
    private TextView mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Utils.checkSpecificPackageAvailable(this,Constants.PACKAGE_NAME_MVS)) {
            Log.d(TAG,"checkSpecificPackageAvailable not available");
            // not available
            finishServicesPrivacySetup(-1);
            return;
        }

        setContentView(R.layout.vzw_services_privacy_setup);
        mTitleIcon = findViewById(R.id.title_icon);
        mTitleIcon.setImageResource(R.drawable.ic_vzw_services);
        mTitle = findViewById(R.id.page_header);
        mTitle.setText("Verizon Services");

        String resultJson = readLayoutJsonViaProvider();
        Log.d(TAG, "readLayoutJsonViaProvider0  resultJson=" + resultJson);
        if (TextUtils.isEmpty(resultJson)) {
            resultJson = readLayoutJsonViaAssets();
            Log.d(TAG, "readLayoutJsonViaAssets0  resultJson=" + resultJson);
            if (TextUtils.isEmpty(resultJson)) {
                finishServicesPrivacySetup(-1);
                return;
            }
        }
        Log.d("resultJson=", resultJson);

        try {
            parseLayoutJson(resultJson);
            if (mPrivacyJsonData == null || mPrivacyJsonData.getComponents() == null || mPrivacyJsonData.getComponents().size() <= 0) {
                finishServicesPrivacySetup(-1);
                return;
            }
            // default page index 0
            VzwServicePrivacySetupFragment fragment = VzwServicePrivacySetupFragment.newInstance(mPrivacyJsonData.getComponents().get(0), 0, Integer.MIN_VALUE);
            getFragmentManager().beginTransaction().replace(R.id.fragments_container, fragment).commit();
        } catch (Exception e) {
            finishServicesPrivacySetup(-1);
            e.printStackTrace();
        }
    }

    private void parseLayoutJson(String layoutJson) {
        Gson gson = new Gson();
        mPrivacyJsonData = gson.fromJson(layoutJson, VzwSetupJsonData.class);
    }

    private String readLayoutJsonViaAssets() {
        AssetManager assetManager = getAssets();
        try {
            InputStream stream = assetManager.open("VerizonServices_SUWConfiguration.json");
            String resultJson = Utils.parseJsonFromInputStream(stream);
            if (Utils.DEBUG) System.out.println("readLayoutJsonViaAssets resultJson=" + resultJson);

            return resultJson;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private String readLayoutJsonViaProvider() {
        Uri uri = Uri.parse("content://com.verizon.mips.services.provider/mvsTandCFile.json");
        Log.e("uri=", uri.toString());

        BufferedReader br = null;
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            StringBuilder sb = new StringBuilder();
            br = new BufferedReader(new InputStreamReader(inputStream));
            String line = null;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            String resultJson = sb.toString();
            if (Utils.DEBUG) System.out.println("readLayoutJsonViaProvider resultJson=" + resultJson);

            return resultJson;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void setCustomTitle(String title) {
        if (mTitle != null && !TextUtils.isEmpty(title)) {
            mTitle.setText(title);
        }
    }

    public static class VzwServicePrivacySetupFragment extends Fragment implements NavigationBar.NavigationBarListener {

        protected static final String KEY_PAGE = "page";
        protected static final String KEY_PAGE_INDEX = "pageIndex";
        protected static final String KEY_LAST_PAGE_INDEX = "lastPageIndex";
        private VzwSetupJsonData.VzwSetupPage mPage;
        private int mPageIndex;
        private int mLastPageIndex;

        public static VzwServicePrivacySetupFragment newInstance(VzwSetupJsonData.VzwSetupPage page, int pageIndex,int curPageIndex) {
            Bundle args = new Bundle();
            args.putSerializable(KEY_PAGE, page);
            args.putInt(KEY_PAGE_INDEX, pageIndex);
            args.putInt(KEY_LAST_PAGE_INDEX, curPageIndex);
            VzwServicePrivacySetupFragment fragment = new VzwServicePrivacySetupFragment();
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
            ViewGroup root = (ViewGroup) LayoutInflater.from(getActivity()).inflate(R.layout.layout_service_privacy_page_fragment, null);
            boolean omitFirstBreak = true;
            for (VzwSetupJsonData.VzwSetupPage.Element e : elements) {
                switch (e.getType()) {
                    case TYPE_HEADER:
                        if ("Verizon Services".equals(e.getContent()) || "My Verizon Services".equals(e.getContent())) {
                            if (getActivity() instanceof VzwServicesPrivacyActivity) {
                                ((VzwServicesPrivacyActivity) getActivity()).setCustomTitle(e.getContent());
                            }
                        } else {
                            TextView header = (TextView) LayoutInflater.from(getActivity()).inflate(R.layout.item_title_textview_page, root, false);
                            header.setText(e.getContent());
                            root.addView(header);
                        }
                        break;
                    case TYPE_LINE_BREAK:
                        if (omitFirstBreak) {
                            omitFirstBreak = false;
                            continue;
                        }
                        View delimiter = LayoutInflater.from(getActivity()).inflate(R.layout.item_delimiter, root, false);
                        root.addView(delimiter);
                        break;
                    case TYPE_TEXT:
                        TextView text = (TextView) LayoutInflater.from(getActivity()).inflate(R.layout.item_textview_page, root, false);
                        CharSequence content = Html.fromHtml(e.getContent());
                        if (Utils.DEBUG) Log.d(TAG, "getContent=" + e.getContent() + " ,\ncontents=" + content);
                        if (TextUtils.isEmpty(content)) {
                            text.setText(e.getContent());
                        } else {
                            int words = Utils.wordsOfString(String.valueOf(content));
                            if (words <= 5 && words > 0) { // assume this is title
                                String textFromHtml = HtmlUtil.delHTMLTag(e.getContent());
                                if (Utils.DEBUG) Log.d(TAG, "textFromHtml=" + textFromHtml);
                                float titleSize = getResources().getDimension(R.dimen.suw_title_size);
                                text.setTextSize(TypedValue.COMPLEX_UNIT_PX,titleSize); // in px
                                text.setTextColor(Color.BLACK);
                                text.setText(textFromHtml);
                            } else {
                                if (!TextUtils.isEmpty(e.getContent())&&e.getContent().startsWith("<p>")) {
                                    String textFromHtml = HtmlUtil.delHTMLTag(e.getContent());
                                    if (Utils.DEBUG) Log.d(TAG, "assume this is paragraph, textFromHtml=" + textFromHtml);
                                    // set paragraph text padding
                                    int paragraphPadding = (int) getResources().getDimension(R.dimen.dimen_paragraph_vertical_padding);
                                    text.setPadding(text.getPaddingLeft(), paragraphPadding, text.getPaddingRight(), paragraphPadding);
                                    text.setText(textFromHtml);
                                } else {
                                    text.setText(content);
                                }
                            }
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
            if (activity != null && activity instanceof VzwServicesPrivacyActivity) {
                NavigationBar navigationBar = ((VzwServicesPrivacyActivity) activity).getNavigationBar();
                // get elements
                VzwSetupJsonData.VzwSetupPage.Element nextButtonElement = getNextButtonElement();
                VzwSetupJsonData.VzwSetupPage.Element backButtonElement = getBackButtonElement();
                if (Utils.DEBUG) Log.d(TAG, "nextButtonElement=" + nextButtonElement + " ,backButtonElement=" + backButtonElement);
                // set visibility
                navigationBar.getNextButton().setVisibility(nextButtonElement == null ? View.INVISIBLE : View.VISIBLE);
                navigationBar.getBackButton().setVisibility(backButtonElement == null ? View.INVISIBLE : View.VISIBLE);
                // set text
                navigationBar.getNextButton().setText(nextButtonElement != null ? nextButtonElement.getContent() : getString(R.string.setup_wizard_next_button_label));
                navigationBar.getBackButton().setText(backButtonElement != null ? backButtonElement.getContent() : getString(R.string.setup_wizard_back_button_label));
            }
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
            if (Utils.DEBUG) Log.d(TAG, "onNavigateBack nextPageIndex=" + nextPageIndex);
            if (nextPageIndex == mLastPageIndex) {
                getFragmentManager().popBackStack();
            } else {
                if (getActivity() instanceof VzwServicesPrivacyActivity) {
                    ((VzwServicesPrivacyActivity) getActivity()).launchNextPage(mPageIndex, nextPageIndex);
                }
            }
        }

        @Override
        public void onNavigateNext() {
            int nextPageIndex = getNextPageIndex(true);
            if (Utils.DEBUG) Log.d(TAG, "onNavigateNext nextPageIndex=" + nextPageIndex);
            if (getActivity() instanceof VzwServicesPrivacyActivity) {
                VzwServicesPrivacyActivity activity = (VzwServicesPrivacyActivity) getActivity();
                // send data to mvs
                HashMap<String, String> commitData = getCommitData();
                if (commitData != null && commitData.size() > 0) {
                    activity.sendUserSelectionsBroadcast(mPageIndex, commitData);
                }

                activity.launchNextPage(mPageIndex, nextPageIndex);
            }
        }

        private HashMap<String, String> getCommitData() {
            HashMap<String, String> datas = new HashMap<>();
            String selectionsData = getSelectionsJsonData(mPage);
            if (Utils.DEBUG) Log.d(TAG, "mPageIndex=" + mPageIndex + " ,selectionsData=" + selectionsData);

            if (!TextUtils.isEmpty(selectionsData) && !selectionsData.equals("{}")) {
                try {
                    JSONObject jsonObject = new JSONObject(selectionsData);
                    Iterator<String> keys = jsonObject.keys();//获得所有的key
                    while (keys.hasNext()) {//检查是否有keys
                        String key = keys.next();
                        String value = jsonObject.getString(key);
                        datas.put(key, value);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return datas;
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
                                        if (Utils.DEBUG) Log.d(TAG, "getSelectionsJsonData id=" + id + " ,isChecked=" + checked);
                                    }
                                }
                            }

                            break;
                        }

                        case TYPE_TOGGLE: {
                            SwitchCompat toggle = getView().findViewById(viewId);
                            selectionsJson.put(id, toggle.isChecked() ? "enabled" : "disabled");
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

    private void launchNextPage(int curPage, int nextPage) {
        List<VzwSetupJsonData.VzwSetupPage> components = mPrivacyJsonData.getComponents();
        if (nextPage >= 0 && nextPage < components.size()) {
            VzwSetupJsonData.VzwSetupPage setupPage = components.get(nextPage);

            VzwServicePrivacySetupFragment fragment = VzwServicePrivacySetupFragment.newInstance(setupPage, nextPage, curPage);
            getFragmentManager().beginTransaction()
                    .addToBackStack(String.valueOf(curPage))
                    .replace(R.id.fragments_container, fragment)
                    .commit();

        } else if (nextPage < 0) {
            finishServicesPrivacySetup(-1);
        }
    }


    private void sendUserSelectionsBroadcast(int pageIndex, HashMap<String, String> commitData) {
        Intent intent = new Intent("com.verizon.mips.services.PAGE_int_SETUP_COMPLETE");
        Set<Map.Entry<String, String>> entries = commitData.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (Utils.DEBUG) Log.d(TAG, "sendUserSelection key=" + key + " ,value=" + value);
            intent.putExtra(key, value);
        }
        intent.putExtra("page_index", String.valueOf(pageIndex)); //page_index depends on number of nested JSON elements(screens)

        sendBroadcast(intent);
        if (Utils.DEBUG) Log.d(TAG, "sendUserSelectionsBroadcast pageIndex=" + pageIndex);
    }

    public NavigationBar getNavigationBar() {
        return mNavigationBar;
    }

    @Override
    public void onNavigateBack() {
        Fragment topFragment = getTopFragment();
        if (Utils.DEBUG) Log.e(TAG, "onNavigateBack  "+(topFragment));
        if (topFragment instanceof NavigationBar.NavigationBarListener) {
            ((NavigationBar.NavigationBarListener) topFragment).onNavigateBack();
        }
    }

    @Override
    public void onNavigateNext() {
        Fragment topFragment = getTopFragment();
        if (Utils.DEBUG) Log.e(TAG, "onNavigateNext  "+(topFragment));
        if (topFragment instanceof NavigationBar.NavigationBarListener) {
            ((NavigationBar.NavigationBarListener) topFragment).onNavigateNext();
        }
    }

    @Override
    public void onNavigationButtonCreated(NavigationBar navigationBar) {
        mNavigationBar = navigationBar;
        mNavigationBar.getNextButton().setText(R.string.suw_accept_button_label);
        mNavigationBar.getBackButton().setVisibility(View.VISIBLE);
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

    private void finishServicesPrivacySetup(int resultCode) {
        if (Utils.DEBUG) Log.d(TAG, "finishServicesPrivacySetup resultCode=" + resultCode);
        Intent intent = WizardManagerHelper.getNextIntent(getIntent(), resultCode);
        startActivityForResult(intent, Constants.REQUEST_CODE_NEXT);

    }


}
