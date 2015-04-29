package kvj.tegmine.android.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import org.kvj.bravo7.ControllerConnector;
import org.kvj.bravo7.log.Logger;

import kvj.tegmine.android.R;
import kvj.tegmine.android.Tegmine;
import kvj.tegmine.android.data.TegmineController;
import kvj.tegmine.android.infra.ControllerService;
import kvj.tegmine.android.ui.fragment.MainPreferences;

/**
 * Created by kvorobyev on 2/26/15.
 */
public class Settings extends AppCompatActivity implements ControllerConnector.ControllerReceiver<TegmineController> {
    private ControllerConnector<Tegmine, TegmineController, ControllerService> conn = new ControllerConnector<>(this, this);

    private Logger logger = Logger.forInstance(this);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_settings);
    }

    @Override
    public void onController(TegmineController controller) {
        if (null == controller) {
            return;
        }
        MainPreferences preferences = new MainPreferences().create(controller, getSupportFragmentManager());
        getFragmentManager().beginTransaction().replace(R.id.settings_frame, preferences).commit();
    }

    @Override
    protected void onStart() {
        super.onStart();
        conn.connectController(ControllerService.class);
    }

    @Override
    protected void onStop() {
        super.onStop();
        conn.disconnectController();
    }
}
