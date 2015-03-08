package kvj.tegmine.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import org.kvj.bravo7.ControllerConnector;
import org.kvj.bravo7.SuperActivity;
import org.kvj.bravo7.log.Logger;

import kvj.tegmine.android.R;
import kvj.tegmine.android.Tegmine;
import kvj.tegmine.android.data.TegmineController;
import kvj.tegmine.android.data.def.FileSystemItem;
import kvj.tegmine.android.infra.ControllerService;
import kvj.tegmine.android.ui.fragment.Editor;
import kvj.tegmine.android.ui.fragment.FileSystemBrowser;
import kvj.tegmine.android.ui.fragment.OneFileViewer;


public class Main extends ActionBarActivity implements ControllerConnector.ControllerReceiver<TegmineController>,FileSystemBrowser.BrowserListener, Editor.EditorListener, OneFileViewer.FileViewerListener {

    private static final int REQUEST_EDITOR = 21;
    private static final int REQUEST_BROWSER = 22;
    private static final int REQUEST_VIEWER = 23;
    private ControllerConnector<Tegmine, TegmineController, ControllerService> conn = new ControllerConnector<>(this, this);
    private TegmineController controller = null;
    private Toolbar toolbar;
    private Logger logger = Logger.forInstance(this);
    private Bundle bundle = new Bundle();

    private FileSystemBrowser browser = null;
    private OneFileViewer viewer = null;
    private Editor editor = null;

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
        setupToolbar(toolbar);
        setSupportActionBar(toolbar);
    }

    private void setupToolbar(Toolbar toolbar) {
        toolbar.setLogo(R.mipmap.ic_launcher);
        toolbar.setLogoDescription(R.string.app_name);
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

    @Override
    public void onController(TegmineController controller) {
        if (this.controller != null) { // Already set
            return;
        }
        findViewById(R.id.main_root).setBackgroundColor(controller.theme().backgroundColor());
        this.controller = controller;
        String mode = getIntent().getStringExtra(Tegmine.BUNDLE_VIEW_TYPE);
        if (null == mode) { // Not defined
            mode = Tegmine.VIEW_TYPE_BROWSER;
        }
//        logger.d("View type", mode);
        if (Tegmine.VIEW_TYPE_BROWSER.equals(mode)) { // Open single browser
            browser = new FileSystemBrowser().setListener(this).create(controller, bundle);
            openIn(browser, R.id.main_single_view, "file_browser");
        }
        if (Tegmine.VIEW_TYPE_FILE.equals(mode)) { // Open one file viewer
            viewer = new OneFileViewer().setListener(this).create(controller, bundle);
            openIn(viewer, R.id.main_single_view, "one_file");
        }
        if (Tegmine.VIEW_TYPE_EDITOR.equals(mode)) { // Open editor
            editor = new Editor().setListener(this).create(controller, bundle);
            openIn(editor, R.id.main_single_view, "editor");
        }
    }

    private void openIn(Fragment fragment, int id, String tag) {
//        fragment.setRetainInstance(true);
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(id, fragment, tag);
//        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (null != browser) {
            browser.saveState(outState);
        }
        if (null != viewer) {
            viewer.saveState(outState);
        }
        if (null != editor) {
            editor.saveState(outState);
        }
//        logger.d("saveState", outState, browser, viewer, editor);
    }

    @Override
    public void onBackPressed() {
        if (null != editor) { // Have editor - ask for change
            if (editor.changed()) { // Have to ask
                SuperActivity.showQuestionDialog(this, "Really exit?", "There are unsaved changes. Exit?", new Runnable() {
                    @Override
                    public void run() {
                        Main.super.onBackPressed();
                    }
                }, new Runnable() {
                    @Override
                    public void run() {
                    }
                });
                return;
            }
        }
        super.onBackPressed();
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
        Intent intent = new Intent(this, Main.class);
        intent.putExtra(Tegmine.BUNDLE_VIEW_TYPE, Tegmine.VIEW_TYPE_FILE);
        intent.putExtras(data);
        startActivityForResult(intent, REQUEST_VIEWER);
    }

    @Override
    public void onAfterSave() {
        logger.d("After save");
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void openEditor(Bundle data) {
        Intent intent = new Intent(this, Main.class);
        intent.putExtra(Tegmine.BUNDLE_VIEW_TYPE, Tegmine.VIEW_TYPE_EDITOR);
        intent.putExtras(data);
        startActivityForResult(intent, REQUEST_EDITOR);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        logger.d("Activity result:", resultCode, requestCode, viewer);
        super.onActivityResult(requestCode, resultCode, data);
        if (null != viewer) {
            viewer.refresh();
        }
    }
}
