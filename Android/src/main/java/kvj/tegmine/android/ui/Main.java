package kvj.tegmine.android.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import org.kvj.bravo7.ControllerConnector;
import org.kvj.bravo7.log.Logger;

import kvj.tegmine.android.R;
import kvj.tegmine.android.Tegmine;
import kvj.tegmine.android.data.TegmineController;
import kvj.tegmine.android.infra.ControllerService;
import kvj.tegmine.android.ui.fragment.FileSystemBrowser;
import kvj.tegmine.android.ui.fragment.OneFileViewer;


public class Main extends ActionBarActivity implements ControllerConnector.ControllerReceiver<TegmineController> {

    private ControllerConnector<Tegmine, TegmineController, ControllerService> conn = new ControllerConnector<>(this, this);
    private TegmineController controller = null;
    private Toolbar toolbar;
    private Logger logger = Logger.forInstance(this);
    private Bundle bundle = new Bundle();

    private FileSystemBrowser browser = null;
    private OneFileViewer viewer = null;

    protected void initBundle(Bundle savedInstanceState) {
        if (null != savedInstanceState) {
            this.bundle = savedInstanceState;
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
            browser = new FileSystemBrowser().create(controller, bundle);
            openIn(browser, R.id.main_single_view, "file_browser");
        }
        if (Tegmine.VIEW_TYPE_FILE.equals(mode)) { // Open one file viewer
            viewer = new OneFileViewer().create(controller, bundle);
            openIn(viewer, R.id.main_single_view, "one_file");
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
    }


}
