package kvj.tegmine.android.ui.fragment;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v4.app.FragmentManager;

import org.kvj.bravo7.SuperActivity;
import org.kvj.bravo7.log.Logger;
import org.kvj.bravo7.util.Tasks;

import kvj.tegmine.android.R;
import kvj.tegmine.android.Tegmine;
import kvj.tegmine.android.data.TegmineController;
import kvj.tegmine.android.data.def.FileSystemException;
import kvj.tegmine.android.ui.ShortcutCreator;
import kvj.tegmine.android.ui.dialog.FileChooser;

/**
 * Created by kvorobyev on 2/26/15.
 */
public class MainPreferences extends PreferenceFragment {

    private static final String PREF_CONF_FILE = "p_config_file";
    private static final CharSequence PREF_REVERT = "p_config_revert";
    private TegmineController controller = null;
    private Logger logger = Logger.forInstance(this);
    private FragmentManager supportFragmentManager;
    private SharedPreferences.OnSharedPreferenceChangeListener confFileListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(PREF_CONF_FILE)) {
                Preference configFile = getPreferenceScreen().findPreference(PREF_CONF_FILE);
                configFile.setSummary(sharedPreferences.getString(PREF_CONF_FILE, ""));
            }
        }
    };

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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        SharedPreferences preferences = getPreferenceManager().getSharedPreferences();
        if (requestCode == Tegmine.REQUEST_FILE && resultCode == Activity.RESULT_OK) { // Selected
            logger.d("Selected file:", data.getAction());
            preferences.edit().putString("p_config_file", data.getAction()).commit();
        }
        if (requestCode == Tegmine.REQUEST_SHORTCUT && resultCode == Activity.RESULT_OK) { // Selected
            Intent intent = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
            String mode = data.getStringExtra(Tegmine.BUNDLE_SHORTCUT_MODE);
            logger.d("Selected shortcut:", intent, mode);
            SharedPreferences.Editor editor = getPreferenceManager().getSharedPreferences().edit();
            editor.putString(Tegmine.prefixed(mode, Tegmine.BUNDLE_VIEW_TYPE), intent.getStringExtra(Tegmine.BUNDLE_VIEW_TYPE));
            editor.putString(Tegmine.prefixed(mode, Tegmine.BUNDLE_EDIT_TYPE), intent.getStringExtra(Tegmine.BUNDLE_EDIT_TYPE));
            editor.putString(Tegmine.prefixed(mode, Tegmine.BUNDLE_EDIT_TEMPLATE), intent.getStringExtra(Tegmine.BUNDLE_EDIT_TEMPLATE));
            editor.putString(Tegmine.prefixed(mode, Tegmine.BUNDLE_SELECT), intent.getStringExtra(Tegmine.BUNDLE_SELECT));
            editor.commit();
        }
    }

    private Preference setupConfigFilePreference() {
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(confFileListener);
        final Preference configFile = getPreferenceScreen().findPreference(PREF_CONF_FILE);
        configFile.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivityForResult(new Intent(getActivity(), FileChooser.class), Tegmine.REQUEST_FILE);
                return true;
            }
        });
        confFileListener.onSharedPreferenceChanged(getPreferenceManager().getSharedPreferences(), PREF_CONF_FILE);
        Preference pref = getPreferenceScreen().findPreference(PREF_REVERT);
        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                String url = getString(R.string.p_config_file_default);
                getPreferenceManager().getSharedPreferences().edit().putString(PREF_CONF_FILE, url).commit();
                reloadConfig();
                return true;
            }
        });
        return configFile;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(confFileListener);
        addPreferencesFromResource(R.xml.main_settings);
        setupConfigFilePreference();
        setupConfigReloadPreference(getString(R.string.p_config_reload));
        setupAssistPreference();
        setupSharePreference();
    }

    private void setupSharePreference() {
        getPreferenceScreen().findPreference(getString(R.string.p_config_share_set))
            .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(getActivity(), ShortcutCreator.class);
                    intent.putExtra(Tegmine.BUNDLE_SHORTCUT_MODE, Tegmine.SHORTCUT_MODE_SHARE);
                    startActivityForResult(intent, Tegmine.REQUEST_SHORTCUT);
                    return true;
                }
            });
    }

    private void setupAssistPreference() {
        getPreferenceScreen().findPreference(getString(R.string.p_config_assist_set))
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent intent = new Intent(getActivity(), ShortcutCreator.class);
                        intent.putExtra("mode", Tegmine.SHORTCUT_MODE_ASSIST);
                        startActivityForResult(intent, Tegmine.REQUEST_SHORTCUT);
                        return true;
                    }
                });
        getPreferenceScreen().findPreference(getString(R.string.p_config_assist_reset))
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        getPreferenceManager().getSharedPreferences()
                                .edit().remove(Tegmine.BUNDLE_VIEW_TYPE).commit();
                        controller.messageShort("Shortcut cleared");
                        return true;
                    }
                });
    }

}
