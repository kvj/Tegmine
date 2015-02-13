package kvj.tegmine.android.ui;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import org.kvj.bravo7.ControllerConnector;

import kvj.tegmine.android.R;
import kvj.tegmine.android.Tegmine;
import kvj.tegmine.android.data.TegmineController;
import kvj.tegmine.android.infra.ControllerService;


public class Main extends ActionBarActivity implements ControllerConnector.ControllerReceiver<TegmineController> {

    private ControllerConnector<Tegmine, TegmineController, ControllerService> conn = new ControllerConnector<>(this, this);
    private TegmineController controller = null;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        this.controller = controller;
    }
}
