package kvj.tegmine.android;

import org.kvj.bravo7.ng.App;

import kvj.tegmine.android.data.TegmineController;

/**
 * Created by kvorobyev on 2/13/15.
 */
public class Tegmine extends App<TegmineController> {

    public static final String BUNDLE_VIEW_TYPE = "view_type";
    public static final String BUNDLE_FILE_LOCATION = "file_location";
    public static final String VIEW_TYPE_BROWSER = "browser";
    public static final String VIEW_TYPE_FILE = "file";
    public static final String VIEW_TYPE_EDITOR = "editor";
    public static final String BUNDLE_EDIT_TYPE = "edit_type";
    public static final String BUNDLE_EDIT_TEMPLATE = "edit_template";
    public static final String EDIT_TYPE_ADD = "edit_add";
    public static final String EDIT_TYPE_EDIT = "edit_edit";
    public static final String BUNDLE_PROVIDER = "provider";

    @Override
    protected TegmineController create() {
        return new TegmineController(this);
    }

    @Override
    protected void init() {
    }
}
