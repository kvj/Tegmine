package kvj.tegmine.android.data.model;

import org.kvj.bravo7.log.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

import kvj.tegmine.android.data.TegmineController;

/**
 * Created by kvorobyev on 4/6/15.
 */
abstract public class AutoThemeChanger {

    private class ChangeRule {
        float lightLess = -1;
        float lightMore = -1;
    }

    Map<String, ChangeRule> rules = new LinkedHashMap<>();
    private Logger logger = Logger.forInstance(this);

    private float light = -1;

    public void setup(Map<String, Object> data) {
        Map<String, Object> config = TegmineController.objectObject(data, "auto_theme");
        if (config == null) {
            return;
        }
        for (String theme : config.keySet()) {
            Map<String, Object> ruleConf = TegmineController.objectObject(config, theme);
            ChangeRule rule = new ChangeRule();
            rule.lightLess = TegmineController.objectInteger(ruleConf, "light_less", -1);
            rule.lightMore = TegmineController.objectInteger(ruleConf, "light_more", -1);
            rules.put(theme, rule);
        }
    }

    public void clear() {
        rules.clear();
    }

    abstract public void onNewTheme(String name);

    public void ambientLightChanged(float newLight) {
        String newTheme = null;
        for (String theme : rules.keySet()) { // $COMMENT
            ChangeRule rule = rules.get(theme);
            if (rule.lightLess != -1) {
                if (light >= rule.lightLess && newLight < rule.lightLess) {
                    newTheme = theme;
                }
                if (light == -1 && newLight < rule.lightLess) {
                    newTheme = theme;
                }
            }
            if (rule.lightMore != -1) {
                if (light <= rule.lightMore && newLight > rule.lightMore) {
                    newTheme = theme;
                }
                if (light == -1 && newLight >= rule.lightMore) {
                    newTheme = theme;
                }
            }
        }
        light = newLight;
        if (null != newTheme) { // Found
            onNewTheme(newTheme);
        }
    }
}
