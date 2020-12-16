package com.odm.setupwizardoverlay;

public class Constants {
    public static final int REQUEST_CODE_NEXT = 1;
    public static final int RESULT_CODE_BACK = 0;
    public static final int RESULT_CODE_NEXT = 1;
    public static final int RESULT_CODE_USE_WIFI = 2;
    public static final int PROVISION_REQUEST_CODE = 2;
    public static final int REQUEST_CODE_PLAN_SELECTION = 3;

    public static final String ACTION_PCO_CHANGE = "com.odm.setupwizardoverlay.PCO_CHANGE";

    public static final String ACTION_POA_DEBUG_MODE_CHANGE = "com.android.provision.ACTION_DEBUG_MODE_CHANGE";

    public static final String KEY_PCO_DATA = "pco_data";
    public static final String KEY_ACTIVATION_STATUS = "vzw_activation_status";

    // sim keys
    public static final String KEY_SIM_STATUS = "sim_status";
    public static final String KEY_SIM_MDN = "sim_mdn";
    public static final String KEY_SIM_FROM_NOTIFICATION = "from_notification";
    public static final String KEY_SIM_FROM_WHERE = "from_where";
    public static final String KEY_SIM_FROM_PLAN_SELECTION = "from_plan_selection";

    // pco values
    public static final int PCO_DATA_0 = 0;
    public static final int PCO_DATA_2 = 2;
    public static final int PCO_DATA_3 = 3;
    public static final int PCO_DATA_4 = 4;
    public static final int PCO_DATA_5 = 5;
    public static final int PCO_DATA_NONE = -1;
    public static final int PCO_DATA_TIME_OUT = -22;
    public static final int PCO_DATA_INIT = -33;
    public static final int PCO_DATA_NON_VZW = -99;

    // activation status
    public static final int ACTION_SKIP_DISPLAY = -1;
    public static final int ACTION_SHOW_NO_SIM = 0;
    public static final int ACTION_SIM_NOT_READY = 1;
    public static final int ACTION_SHOW_SIM_ERROR = 2;
    public static final int ACTION_SIM_READY = 3;
    public static final int ACTION_SHOW_ACTIVATED = 4;
    public static final int ACTION_SHOW_NOT_ACTIVATED = 5;
    public static final int ACTION_SHOW_PLAN_SELECTION = 6;
    public static final int ACTION_NON_VZW_SIM = 7;
    public static final int ACTION_PCO_2_UI = 8;

    public static final int MSG_ACTION_NON_VZW_SIM_CHECK = 66;

    // packages
    public static final String PACKAGE_NAME_MVS = "com.verizon.mips.services";

    // path to FDR date file
    public static final String PATH_FDR_DATE_FILE = "/mnt/umx/barcode/reset_date";

    // String value that defines SUW for out of box or SUW for FDR
    public static final String SCENARIO_SUW = "SUW";
    public static final String SCENARIO_FDR_SUW = "FACTORY_DATA_RESET_SUW";

    protected static final String VERIZON_SIM_UNLOCK_STATE = "verizon_sim_unlock_state";
}
