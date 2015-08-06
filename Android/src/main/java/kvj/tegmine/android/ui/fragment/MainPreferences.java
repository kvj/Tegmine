package kvj.tegmine.android.ui.fragment;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v4.app.FragmentManager;

import org.kvj.bravo7.SuperActivity;
import org.kvj.bravo7.log.Logger;
import org.kvj.bravo7.util.Tasks;

import kvj.tegmine.android.R;
import kvj.tegmine.android.data.TegmineController;
import kvj.tegmine.android.data.def.FileSystemException;
import kvj.tegmine.android.data.def.FileSystemItem;
import kvj.tegmine.android.ui.dialog.FileChooser;

/**
 * Created by kvorobyev on 2/26/15.
 */
public class MainPreferences extends PreferenceFragment {

    private TegmineController controller = null;
    private Logger logger = Logger.forInstance(this);
    private FragmentManager supportFragmentManager;

    public MainPreferences create(TegmineController controller, FragmentManager supportFragmentManager) {
        this.controller = controller;
        this.supportFragmentManager = supportFragmentManager;
        return this;
    }

    public MainPreferences() {
    }

    private void reloadConfig() {
        Tasks.SimpleTask<FileSystemException> task = new Tasks.SimpleTask<FileSystemException>() {
            @Override
            protected FileSystemException doInBackground() {
                try {
                    controller.reloadConfig();
                } catch (FileSystemException e) {
                    logger.e(e, "Failed to load config");
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(FileSystemException e) {
                if (null != e) { // Error loading config
                    SuperActivity.notifyUser(getActivity(), e.getMessage());
                }
            }
        };
        task.exec();
    }

    private void setupConfigReloadPreference(String name) {
        Preference pref = getPreferenceScreen().findPreference(name);
        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                reloadConfig();
                return true;
            }
        });
    }

    private Preference setupConfigFilePreference(final String name, final String revert_name) {
        final Preference configFile = getPreferenceScreen().findPreference(name);
        final Preference.OnPreferenceChangeListener listener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                configFile.setSummary(newValue.toString());
                return true;
            }
        };
        configFile.setOnPreferenceChangeListener(listener);
        String value = getPreferenceManager().getSharedPreferences().getString(name, "");
        listener.onPreferenceChange(configFile, value);
        configFile.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                FileChooser.newDialog(controller, new FileChooser.FileChooserListener() {
                    @Override
                    public void onFile(FileSystemItem item) {
                        getPreferenceManager().getSharedPreferences().edit()
                            .putString(name, item.toURL()).commit();
                        listener.onPreferenceChange(configFile, item.toURL());
                    }
                }).show(supportFragmentManager, "dialog");
                return true;
            }
        });
        Preference pref = getPreferenceScreen().findPreference(revert_name);
        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                String url = getString(R.string.p_config_file_default);
                getPreferenceManager().getSharedPreferences().edit().putString(name, url).commit();
                listener.onPreferenceChange(configFile, url);
                reloadConfig();
                return true;
            }
        });
        return configFile;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.main_settings);
        setupConfigFilePreference("p_config_file", getString(R.string.p_config_revert));
        setupConfigReloadPreference(getString(R.string.p_config_reload));
    }

}
