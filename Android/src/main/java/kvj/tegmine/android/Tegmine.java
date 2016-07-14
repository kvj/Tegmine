package kvj.tegmine.android;

import android.text.TextUtils;

import org.kvj.bravo7.ng.App;

import kvj.tegmine.android.data.TegmineController;

/**
 * Created by kvorobyev on 2/13/15.
 */
public class Tegmine extends App<TegmineController> {

    public static final String BUNDLE_VIEW_TYPE = "view_type";
    public static final String BUNDLE_SELECT = "select";
    public static final String BUNDLE_FILE_LOCATION = "file_location";
    public static final String VIEW_TYPE_BROWSER = "browser";
    public static final String VIEW_TYPE_FILE = "file";
    public static final String VIEW_TYPE_EDITOR = "editor";
    public static final String BUNDLE_EDIT_TYPE = "edit_type";
    public static final String BUNDLE_EDIT_TEMPLATE = "edit_template";
    public static final String EDIT_TYPE_ADD = "edit_add";
    public static final String EDIT_TYPE_EDIT = "edit_edit";
    public static final String BUNDLE_EDIT_SHARED = "edit_shared";
    public static final String BUNDLE_PROVIDER = "provider";
    public static final int REQUEST_FILE = 3;
    public static final int REQUEST_SHORTCUT = 4;
    public static final int REQUEST_VOICE = 5;
    public static final String BUNDLE_SHORTCUT_MODE = "shortcut_mode";
    public static final String SHORTCUT_MODE_SHARE = "share";
    public static final String SHORTCUT_MODE_ASSIST = "assist";

    @Override
    protected TegmineController create() {
        return new TegmineController(this);
    }

    @Override
    protected void init() {
    }

    public static String prefixed(String pr, String sf) {
        if (TextUtils.isEmpty(pr)) pr = "";
        return String.format("%s_%s", pr, sf);
    }
}
