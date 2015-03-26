package kvj.tegmine.android.ui;

import android.animation.LayoutTransition;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import org.kvj.bravo7.ControllerConnector;
import org.kvj.bravo7.SuperActivity;
import org.kvj.bravo7.form.FormController;
import org.kvj.bravo7.form.impl.bundle.StringBundleAdapter;
import org.kvj.bravo7.form.impl.widget.TransientAdapter;
import org.kvj.bravo7.log.Logger;

import kvj.tegmine.android.R;
import kvj.tegmine.android.Tegmine;
import kvj.tegmine.android.data.TegmineController;
import kvj.tegmine.android.data.def.FileSystemItem;
import kvj.tegmine.android.data.model.ProgressListener;
import kvj.tegmine.android.infra.ControllerService;
import kvj.tegmine.android.ui.fragment.Editor;
import kvj.tegmine.android.ui.fragment.FileSystemBrowser;
import kvj.tegmine.android.ui.fragment.OneFileViewer;


public class Main extends ActionBarActivity implements ControllerConnector.ControllerReceiver<TegmineController>,
        FileSystemBrowser.BrowserListener,
        Editor.EditorListener,
        OneFileViewer.FileViewerListener,
        ProgressListener {

    private static final int REQUEST_EDITOR = 21;
    private static final int REQUEST_BROWSER = 22;
    private static final int REQUEST_VIEWER = 23;
    private ControllerConnector<Tegmine, TegmineController, ControllerService> conn = new ControllerConnector<>(this, this);
    private TegmineController controller = null;
    private Toolbar toolbar;
    private Logger logger = Logger.forInstance(this);
    private Bundle bundle = new Bundle();
    private FormController form = null;
    private boolean multiView = false;
    private LinearLayout multiPane = null;

    private FileSystemBrowser browser = null;
    private OneFileViewer viewer = null;
    private Editor editor = null;
    private ContentLoadingProgressBar progressBar = null;

    protected void initBundle(Bundle savedInstanceState) {
        if (null != savedInstanceState) {
            this.bundle = savedInstanceState;
            return;
        }
        if (null != getIntent() && null != getIntent().getExtras()) { // Have intent extras
            this.bundle = getIntent().getExtras();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initBundle(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        progressBar = (ContentLoadingProgressBar)findViewById(R.id.main_progress_bar);
        setSupportActionBar(toolbar);
        setupToolbar(toolbar);
        form = new FormController(null);
        form.add(new TransientAdapter<String>(new StringBundleAdapter(), Tegmine.VIEW_TYPE_BROWSER), Tegmine.BUNDLE_VIEW_TYPE);
        multiPane = (LinearLayout) findViewById(R.id.main_view);
        multiView = multiPane != null;
        if (multiView) {
            LayoutTransition transition = new LayoutTransition();
            transition.enableTransitionType(LayoutTransition.CHANGING);
            transition.setDuration(200);
            multiPane.setLayoutTransition(transition);
        }
    }

    private void setupToolbar(Toolbar toolbar) {
        toolbar.setTitle(R.string.app_name);
        toolbar.setNavigationIcon(R.drawable.toolbar_drawer);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (null != browser) {
                    browser.toggleNavigation();
                }
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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
            case R.id.menu_check_updates:
                Tegmine.app().getAutoUpdate().checkUpdatesManually();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        conn.connectController(ControllerService.class);
    }

    @Override
    protected void onStop() {
        conn.disconnectController();
        super.onStop();
    }

    private void showBrowser() {
        browser = new FileSystemBrowser().setListener(this).create(controller, bundle);
        if (multiView) { // Load to first cell
            openIn(browser, R.id.main_left_view, "file_browser");
        } else {
            openIn(browser, R.id.main_single_view, "file_browser");
        }
    }

    private void showViewer(Bundle data) {
        viewer = new OneFileViewer().setListener(this).create(controller, data);
        if (multiView) {
            openIn(viewer, R.id.main_center_view, "one_file");
        } else {
            openIn(viewer, R.id.main_single_view, "one_file");
        }
    }

    private void showEditor(Bundle data) {
        editor = new Editor().setListener(this).create(controller, data);
        if (multiView) {
            openIn(editor, R.id.main_right_view, "editor");
        } else {
            openIn(editor, R.id.main_single_view, "editor");
        }

    }

    @Override
    public void onController(TegmineController controller) {
        if (this.controller != null) { // Already set
            return;
        }
        form.setView(findViewById(R.id.main_root));
        form.load(bundle);
        findViewById(R.id.main_root).setBackgroundColor(controller.theme().backgroundColor());
        this.controller = controller;
        String mode = form.getValue(Tegmine.BUNDLE_VIEW_TYPE, String.class);

        if (Tegmine.VIEW_TYPE_BROWSER.equals(mode)) { // Open single browser
            showBrowser();
        }
        if (Tegmine.VIEW_TYPE_FILE.equals(mode)) { // Open one file viewer
            if (multiView) {
                showBrowser();
            }
            showViewer(bundle);
        }
        if (Tegmine.VIEW_TYPE_EDITOR.equals(mode)) { // Open editor
            if (multiView) {
                showBrowser();
                showViewer(bundle);
            }
            showEditor(bundle);
        }
        resizeMultiPane();
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
        form.save(outState);
        if (null != browser) {
            browser.saveState(outState);
        }
        if (null != viewer) {
            viewer.saveState(outState);
        }
        if (null != editor) {
            editor.saveState(outState);
        }
    }

    private void setType(String type) {
        form.setValue(Tegmine.BUNDLE_VIEW_TYPE, type);
        resizeMultiPane();
    }

    private void hideEditor(boolean confirm, final Runnable afterHide) {
        if (null == editor) {
            afterHide.run();
            return;
        }
        final Runnable doHide = new Runnable() {
            @Override
            public void run() {
                if (multiView) { // Just remove from right view
                    getSupportFragmentManager().beginTransaction().remove(editor).commit();
                    editor = null;
                }
                afterHide.run();
            }
        };
        if (!confirm || !editor.changed()) {
            doHide.run();
            return;
        }
        SuperActivity.showQuestionDialog(this, "Really exit?", "There are unsaved changes. Exit?", new Runnable() {
            @Override
            public void run() {
                doHide.run();
            }
        }, new Runnable() {
            @Override
            public void run() {
            }
        });
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

    private void closeEditor(boolean confirm) {
        final boolean haveEditor = editor != null;
        hideEditor(confirm, new Runnable() {
            @Override
            public void run() {
                // Hidden
                if (haveEditor && multiView) {
                    // Just closed editor
                    setType(Tegmine.VIEW_TYPE_FILE);
                    viewer.refresh();
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
            if (null == editor) { // No editor now
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
    public void onAfterSave() {
        setResult(RESULT_OK);
        closeEditor(false);
    }

    @Override
    public void openEditor(final Bundle data) {
        if (multiView) { // Show in center
            if (null != editor) { // Have to check OK to replace or not and close
                hideEditor(true, new Runnable() {
                    @Override
                    public void run() {
                        showEditor(data);
                    }
                });
            } else {
                showEditor(data);
                setType(Tegmine.VIEW_TYPE_EDITOR);
            }
        } else {
            Intent intent = new Intent(this, Main.class);
            intent.putExtra(Tegmine.BUNDLE_VIEW_TYPE, Tegmine.VIEW_TYPE_EDITOR);
            intent.putExtras(data);
            startActivityForResult(intent, REQUEST_EDITOR);
        }
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
}
