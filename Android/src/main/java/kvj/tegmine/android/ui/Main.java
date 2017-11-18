package kvj.tegmine.android.ui;

import android.Manifest;
import android.animation.LayoutTransition;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;

import org.kvj.bravo7.form.FormController;
import org.kvj.bravo7.form.impl.ViewFinder;
import org.kvj.bravo7.form.impl.bundle.StringBundleAdapter;
import org.kvj.bravo7.form.impl.widget.TransientAdapter;
import org.kvj.bravo7.log.Logger;
import org.kvj.bravo7.ng.App;
import org.kvj.bravo7.util.Compat;

import kvj.tegmine.android.R;
import kvj.tegmine.android.Tegmine;
import kvj.tegmine.android.data.TegmineController;
import kvj.tegmine.android.data.def.FileSystemException;
import kvj.tegmine.android.data.def.FileSystemItem;
import kvj.tegmine.android.data.model.ProgressListener;
import kvj.tegmine.android.ui.fragment.Editors;
import kvj.tegmine.android.ui.fragment.FileSystemBrowser;
import kvj.tegmine.android.ui.fragment.OneFileViewer;


public class Main extends AppCompatActivity implements
        FileSystemBrowser.BrowserListener,
        Editors.EditorsListener,
        OneFileViewer.FileViewerListener,
        ProgressListener,
        SensorEventListener {

    private static final int REQUEST_EDITOR = 21;
    private static final int REQUEST_BROWSER = 22;
    private static final int REQUEST_VIEWER = 23;
    private TegmineController controller = App.controller();
    private Toolbar toolbar;
    private Logger logger = Logger.forInstance(this);
    private FormController form = new FormController(new ViewFinder.ActivityViewFinder(this));
    private boolean multiView = false;
    private LinearLayout multiPane = null;

    private FileSystemBrowser browser = null;
    private OneFileViewer viewer = null;
    private Editors editors = null;
    private ContentLoadingProgressBar progressBar = null;
    private SensorManager mSensorManager = null;
    private boolean created = false;


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        for (int result : grantResults) { // Check results
            if (result != PackageManager.PERMISSION_GRANTED) { // Error
                setupPane(null);
                return;
            }
        }
        finish();
        try {
            controller.reloadConfig();
            // All OK - restart
            startActivity(new Intent(this, Main.class));
        } catch (FileSystemException e) {
            controller.messageShort(e.getMessage());
        }
    }

    private void setupPane(Bundle savedInstanceState) {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        setContentView(R.layout.activity_main);
        created = true;
        toolbar = findViewById(R.id.main_toolbar);
        progressBar = findViewById(R.id.main_progress_bar);
        setSupportActionBar(toolbar);
        form.add(new TransientAdapter<>(new StringBundleAdapter(), Tegmine.VIEW_TYPE_BROWSER), Tegmine.BUNDLE_VIEW_TYPE);
        multiPane = findViewById(R.id.main_view);
        multiView = multiPane != null;
        if (multiView) {
            Compat.levelAware(Build.VERSION_CODES.JELLY_BEAN, new Runnable() {
                @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                @Override
                public void run() {
                    LayoutTransition transition = new LayoutTransition();
                    transition.enableTransitionType(LayoutTransition.CHANGING);
                    transition.setDuration(200);
                    multiPane.setLayoutTransition(transition);
                }
            });
        }
        form.load(this, savedInstanceState);
        if (null != getIntent()) {
            String data = getIntent().getStringExtra(Intent.EXTRA_TEXT);
            if (Intent.ACTION_ASSIST.equals(getIntent().getAction())) {
                if (null == savedInstanceState) { // Create new
                    savedInstanceState = new Bundle();
                }
                startAssist(savedInstanceState);
            }
            if (Intent.ACTION_SEND.equals(getIntent().getAction()) && !TextUtils.isEmpty(data)) {
                if (null == savedInstanceState) { // Create new
                    savedInstanceState = new Bundle();
                }
                startShare(savedInstanceState, data);
            }
        }
        onController(savedInstanceState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(controller.theme().dark() ? R.style.AppThemeDark : R.style.AppThemeLight);
        super.onCreate(savedInstanceState);
        if (!controller.havePermissions()) { // Stop
            ActivityCompat.requestPermissions(this, Tegmine.STORAGE_PERMISSIONS, Tegmine.REQUEST_PERMISSIONS);
            return;
        }
        setupPane(savedInstanceState);
    }

    private void fromSettings(Bundle bundle, String prefix) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String mode = preferences.getString(Tegmine.prefixed(prefix, Tegmine.BUNDLE_VIEW_TYPE), null);
        logger.d("Assist mode:", mode);
        if (!TextUtils.isEmpty(mode)) { // Have
            form.setValue(Tegmine.BUNDLE_VIEW_TYPE, mode);
            bundle.putString(Tegmine.BUNDLE_EDIT_TYPE,
                             preferences.getString(Tegmine.prefixed(prefix, Tegmine.BUNDLE_EDIT_TYPE), Tegmine.EDIT_TYPE_ADD));
            bundle.putString(Tegmine.BUNDLE_EDIT_TEMPLATE,
                             preferences.getString(Tegmine.prefixed(prefix, Tegmine.BUNDLE_EDIT_TEMPLATE), null));
            bundle.putString(Tegmine.BUNDLE_SELECT,
                             preferences.getString(Tegmine.prefixed(prefix, Tegmine.BUNDLE_SELECT), null));
        }
    }

    private void startShare(Bundle bundle, String data) {
        fromSettings(bundle, Tegmine.SHORTCUT_MODE_SHARE);
        bundle.putString(Tegmine.BUNDLE_EDIT_SHARED, data);
    }

    private void startAssist(Bundle bundle) {
        fromSettings(bundle, Tegmine.SHORTCUT_MODE_ASSIST);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!created) return false;
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.menu_settings:
                startActivity(new Intent(this, Settings.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showBrowser(Bundle bundle) {
        browser = new FileSystemBrowser().setListener(this).create(this, controller, bundle);
        browser.setupToolbar(this);
        if (multiView) { // Load to first cell
            openIn(browser, R.id.main_left_view, "file_browser");
        } else {
            openIn(browser, R.id.main_single_view, "file_browser");
        }
    }

    private void showViewer(Bundle data) {
        viewer = new OneFileViewer().setListener(this).create(this, controller, data);
        if (multiView) {
            openIn(viewer, R.id.main_center_view, "one_file");
        } else {
            openIn(viewer, R.id.main_single_view, "one_file");
        }
    }

    private void showEditor(Bundle data) {
        if (null != editors) {
            // Add existing
            editors.add(data);
            return;
        }
        editors = new Editors().addListener(this).create(this, controller, data);
        if (multiView) {
            openIn(editors, R.id.main_right_view, "editor");
        } else {
            openIn(editors, R.id.main_single_view, "editor");
        }

    }

    private void applyTheme() {
        findViewById(R.id.main_root).setBackgroundColor(controller.theme().backgroundColor());
    }

    public void onController(Bundle savedInstanceState) {
        applyTheme();
        String mode = form.getValue(Tegmine.BUNDLE_VIEW_TYPE, String.class);
        if (Tegmine.VIEW_TYPE_BROWSER.equals(mode)) { // Open single browser
            showBrowser(savedInstanceState);
        }
        if (Tegmine.VIEW_TYPE_FILE.equals(mode)) { // Open one file viewer
            if (multiView) {
                showBrowser(savedInstanceState);
            }
            showViewer(savedInstanceState);
        }
        if (Tegmine.VIEW_TYPE_EDITOR.equals(mode)) { // Open editor
            if (multiView) {
                showBrowser(savedInstanceState);
                showViewer(savedInstanceState);
            }
            showEditor(savedInstanceState);
        }
        resizeMultiPane();
        controller.progressListeners().add(this);
    }

    private boolean openIn(Fragment fragment, int id, String tag) {
        if (null == fragment) {
            // Nothing to add
            return false;
        }
//        fragment.setRetainInstance(true);
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(id, fragment, tag);
//        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (!created) return;
        form.save(outState);
        if (null != browser) {
            browser.saveState(outState);
        }
        if (null != viewer) {
            viewer.saveState(outState);
        }
        if (null != editors) {
            editors.saveState(outState);
        }
    }

    private void setType(String type) {
        form.setValue(Tegmine.BUNDLE_VIEW_TYPE, type);
        resizeMultiPane();
    }

    private void hideEditor(boolean confirm, final Runnable afterHide) {
        final Runnable doHide = new Runnable() {
            @Override
            public void run() {
                if (multiView && null != editors) { // Just remove from right view
                    getSupportFragmentManager().beginTransaction().remove(editors).commit();
                    editors = null;
                }
                afterHide.run();
            }
        };
        doHide.run();
    }

    private int[] resizePaneIDs = {R.id.main_left_view, R.id.main_center_view, R.id.main_right_view};

    private void resizeMultiPane() {
        if (!multiView) {
            return;
        }
        String type = form.getValue(Tegmine.BUNDLE_VIEW_TYPE, String.class);
        float[] sizes = {0.7f, 0.3f, 0f};
        if (Tegmine.VIEW_TYPE_FILE.equals(type)) {
            sizes = new float[]{0.4f, 0.6f, 0f};
        }
        if (Tegmine.VIEW_TYPE_EDITOR.equals(type)) {
            sizes = new float[]{0.2f, 0.3f, 0.5f};
        }
        for (int i = 0; i < sizes.length; i++) {
            findViewById(resizePaneIDs[i]).setLayoutParams(
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, sizes[i]));
        }
    }

    private boolean isType(String type) {
        return type.equals(form.getValue(Tegmine.BUNDLE_VIEW_TYPE));
    }

    private void closeEditor(boolean confirm) {
        if (multiView && isType(Tegmine.VIEW_TYPE_FILE)) {
            // Close
            getSupportFragmentManager().beginTransaction().remove(viewer).commit();
            viewer = null;
            setType(Tegmine.VIEW_TYPE_BROWSER);
            browser.requestFocus();
            return;
        }
        final boolean haveEditor = editors != null;
        if (haveEditor && editors.closeFindDialog()) { // Closed find dialog
            return;
        }
        hideEditor(confirm, new Runnable() {
            @Override
            public void run() {
                // Hidden
                if (haveEditor && multiView) {
                    // Just closed editor
                    setType(Tegmine.VIEW_TYPE_FILE);
                    viewer.refresh();
                    viewer.requestFocus();
                    return;
                }
                finish(); // In all other cases - close
            }
        });
    }

    @Override
    public void onBackPressed() {
        closeEditor(true);
    }

    @Override
    public void openNewWindow(Bundle data) {
        Intent intent = new Intent(this, Main.class);
        intent.putExtra(Tegmine.BUNDLE_VIEW_TYPE, Tegmine.VIEW_TYPE_BROWSER);
        intent.putExtras(data);
        startActivityForResult(intent, REQUEST_BROWSER);
    }

    @Override
    public void openFile(Bundle data, FileSystemItem item) {
        if (multiView) { // Show viever here
            showViewer(data);
            if (null == editors) { // No editor now
                setType(Tegmine.VIEW_TYPE_FILE);
            }
            return;
        }
        Intent intent = new Intent(this, Main.class);
        intent.putExtra(Tegmine.BUNDLE_VIEW_TYPE, Tegmine.VIEW_TYPE_FILE);
        intent.putExtras(data);
        startActivityForResult(intent, REQUEST_VIEWER);
    }

    @Override
    public void openEditor(final Bundle data) {
        if (multiView) { // Show in center
            showEditor(data);
            setType(Tegmine.VIEW_TYPE_EDITOR);
        } else {
            Intent intent = new Intent(this, Main.class);
            intent.putExtra(Tegmine.BUNDLE_VIEW_TYPE, Tegmine.VIEW_TYPE_EDITOR);
            intent.putExtras(data);
            startActivityForResult(intent, REQUEST_EDITOR);
        }
    }

    @Override
    public void updateViewerTitle(String title) {
        toolbar.setTitle(controller.fileSystemProvider(viewer.item()).label());
        toolbar.setSubtitle(title);
    }

    @Override
    public void updateBrowserTitle(String title) {
        toolbar.setSubtitle(title);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        logger.d("Activity result:", resultCode, requestCode, viewer);
        super.onActivityResult(requestCode, resultCode, data);
        if (null != viewer) {
            viewer.refresh();
        }
        if (null != browser) {
            browser.refresh();
        }
    }

    @Override
    public void activityStarted() {
        if (null != progressBar) {
            progressBar.show();
        }
    }

    @Override
    public void activityStopped() {
        if (null != progressBar) {
            progressBar.hide();
        }
    }

    @Override
    public void themeChanged() {
        applyTheme();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!created) return;
        controller.progressListeners().add(this);
        themeChanged();
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT), SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!created) return;
        if (null != controller) {
            controller.progressListeners().remove(this);
        }
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float light = event.values[0];
        if (null != controller) {
            controller.ambientLightChanged(light);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onHide() {
        setResult(RESULT_OK);
        closeEditor(false);
    }
}
