package kvj.tegmine.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import org.kvj.bravo7.log.Logger;
import org.kvj.bravo7.ng.App;
import org.kvj.bravo7.ng.widget.AppWidgetController;

import kvj.tegmine.android.R;
import kvj.tegmine.android.data.TegmineController;
import kvj.tegmine.android.ui.appwidget.Widget00;
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        final AppWidgetController controller = AppWidgetController.instance(this);
        int[] ids = controller.ids(Widget00.class);
        MenuItem item = menu.findItem(R.id.menu_settings_widgets);
        item.getSubMenu().clear();
        Widget00 widget = new Widget00();
        for (final int id : ids) {
            MenuItem subItem = item.getSubMenu().add(controller.title(id, widget));
            subItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    Intent intent = controller.configIntent(id);
                    startActivity(intent);
                    return true;
                }
            });
        }
        return true;
    }
}
