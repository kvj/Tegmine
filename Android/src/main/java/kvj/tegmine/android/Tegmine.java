package kvj.tegmine.android;

import org.kvj.bravo7.ApplicationContext;
import org.kvj.bravo7.log.AndroidLogger;
import org.kvj.bravo7.log.Logger;

import kvj.tegmine.android.data.TegmineController;

/**
 * Created by kvorobyev on 2/13/15.
 */
public class Tegmine extends ApplicationContext {

    public static final String BUNDLE_VIEW_TYPE = "view_type";
    public static final String BUNDLE_FILE_LOCATION = "file_location";
    public static final String VIEW_TYPE_BROWSER = "browser";
    public static final String VIEW_TYPE_FILE = "file";
    public static final String VIEW_TYPE_EDITOR = "editor";
    public static final String BUNDLE_EDIT_TYPE = "edit_type";
    public static final String EDIT_TYPE_ADD = "edit_add";
    public static final String EDIT_TYPE_EDIT = "edit_edit";
    public static final String BUNDLE_PROVIDER = "provider";

    @Override
    public void onCreate() {
        super.onCreate();
        Logger.setOutput(new AndroidLogger("Tegmine:"));
        publishBean(new TegmineController());
    }

    @Override
    protected void init() {
    }
}
