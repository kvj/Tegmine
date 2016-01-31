package kvj.tegmine.android.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import org.kvj.bravo7.log.Logger;
import org.kvj.bravo7.ng.App;

import kvj.tegmine.android.R;
import kvj.tegmine.android.data.TegmineController;
import kvj.tegmine.android.ui.fragment.MainPreferences;

/**
 * Created by kvorobyev on 2/26/15.
 */
public class Settings extends AppCompatActivity {

    private Logger logger = Logger.forInstance(this);
    private TegmineController controller = App.controller();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_settings);
        setSupportActionBar((Toolbar) findViewById(R.id.main_toolbar));
        onController();
    }

    public void onController() {
        MainPreferences preferences = new MainPreferences().create(controller, getSupportFragmentManager());
        getFragmentManager().beginTransaction().replace(R.id.settings_frame, preferences).commit();
    }
}
