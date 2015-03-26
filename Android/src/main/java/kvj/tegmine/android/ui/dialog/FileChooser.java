package kvj.tegmine.android.ui.dialog;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.kvj.bravo7.log.Logger;

import kvj.tegmine.android.R;
import kvj.tegmine.android.Tegmine;
import kvj.tegmine.android.data.TegmineController;
import kvj.tegmine.android.data.def.FileSystemItem;
import kvj.tegmine.android.ui.fragment.FileSystemBrowser;

/**
 * Created by kvorobyev on 2/26/15.
 */
public class FileChooser extends DialogFragment implements FileSystemBrowser.BrowserListener {

    private FileChooserListener listener = null;

    public static interface FileChooserListener {
        public void onFile(FileSystemItem item);
    }

    private TegmineController controller = null;
    private Logger logger = Logger.forInstance(this);

    public static FileChooser newDialog(TegmineController controller, FileChooserListener listener) {
        FileChooser instance = new FileChooser();
        instance.setShowsDialog(false);
        instance.setStyle(DialogFragment.STYLE_NO_TITLE, instance.getTheme());
        instance.controller = controller;
        instance.listener = listener;
        return instance;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (null == controller) { // Not initialised
            return null;
        }
        Bundle data = new Bundle();
        data.putString(Tegmine.BUNDLE_PROVIDER, "sdcard");
        FileSystemBrowser browser = new FileSystemBrowser().create(controller, data).setListener(this);
        View v = inflater.inflate(R.layout.dialog_settings, container, false);
        v.findViewById(R.id.settings_frame).setBackgroundColor(controller.theme().backgroundColor());
        getChildFragmentManager().beginTransaction().addToBackStack("browser").replace(R.id.settings_frame, browser, "browser").commit();
        return v;
    }

    @Override
    public void openNewWindow(Bundle data) {

    }

    @Override
    public void openFile(Bundle data, FileSystemItem item) {
        dismiss();
        if (null != listener) { // OK
            listener.onFile(item);
        }
    }
}
