package kvj.tegmine.android.ui.appwidget;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;

import org.kvj.bravo7.log.Logger;
import org.kvj.bravo7.ng.widget.AppWidget;
import org.kvj.bravo7.ng.widget.AppWidgetConfigActivity;

import kvj.tegmine.android.R;
import kvj.tegmine.android.Tegmine;
import kvj.tegmine.android.data.TegmineController;
import kvj.tegmine.android.ui.dialog.FileChooser;

/**
 * Created by vorobyev on 7/27/15.
 */
public class Widget00Config extends AppWidgetConfigActivity {

    private Logger logger = Logger.forInstance(this);
    private Preference urlPref = null;
    private TegmineController controller = Tegmine.controller();

    @Override
    protected int preferenceXML() {
        return R.xml.widget_00_config;
    }

    @Override
    protected AppWidget.AppWidgetUpdate appWidget() {
        return new Widget00();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(controller.theme().dark() ? R.style.AppThemeDark : R.style.AppThemeLight);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected String toolbarSubTitle() {
        return "Widget Configuration";
    }

    @Override
    protected void onPreferencesLoaded() {
        super.onPreferencesLoaded();
        urlPref = fragment.getPreferenceManager().findPreference(
                getString(R.string.conf_widget_url));
        urlPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                // Start selector
                startActivityForResult(new Intent(Widget00Config.this, FileChooser.class), Tegmine.REQUEST_FILE);
                return true;
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Tegmine.REQUEST_FILE && resultCode == Activity.RESULT_OK) { // Selected
            logger.d("Selected file:", data.getAction());
            urlPref.getEditor().putString(urlPref.getKey(), data.getAction()).commit();
        }
    }
}
