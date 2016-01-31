package kvj.tegmine.android.ui.dialog;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import org.kvj.bravo7.log.Logger;

import kvj.tegmine.android.R;
import kvj.tegmine.android.Tegmine;
import kvj.tegmine.android.data.TegmineController;
import kvj.tegmine.android.data.def.FileSystemItem;
import kvj.tegmine.android.ui.fragment.FileSystemBrowser;

/**
 * Created by kvorobyev on 2/26/15.
 */
public class FileChooser extends AppCompatActivity implements FileSystemBrowser.BrowserListener {

    public static interface FileChooserListener {
        public void onFile(FileSystemItem item);
    }

    private TegmineController controller = Tegmine.controller();
    private Logger logger = Logger.forInstance(this);


    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(controller.theme().dark() ? R.style.AppDialogDark : R.style.AppDialogLight);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_settings);
        Bundle data = new Bundle();
        data.putString(Tegmine.BUNDLE_PROVIDER, "sdcard");
        final FileSystemBrowser
                browser = new FileSystemBrowser().create(this, controller, data).setListener(this);
        browser.setupToolbar(this).setSubtitle("Choose a file");
        getSupportFragmentManager().beginTransaction().replace(R.id.settings_frame, browser).commit();
    }

    @Override
    public void openNewWindow(Bundle data) {

    }

    @Override
    public void openFile(Bundle data, FileSystemItem item) {
        setResult(RESULT_OK, new Intent(item.toURL()));
        finish();
    }

    @Override
    public void updateBrowserTitle(String title) {
        // TODO: Update title
    }
}
