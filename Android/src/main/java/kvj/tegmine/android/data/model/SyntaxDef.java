package kvj.tegmine.android.data.model;

import android.text.SpannableStringBuilder;
import android.text.TextUtils;

import org.kvj.bravo7.log.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import kvj.tegmine.android.data.TegmineController;
import kvj.tegmine.android.ui.theme.LightTheme;

/**
 * Created by vorobyev on 3/13/15.
 */
public class SyntaxDef {

    private Logger logger = Logger.forInstance(this);

    private class PatternDef {
        String name = null;
        Pattern pattern = null;
        int group = 0;
        List<String> includesStr = null;
        List<PatternDef> includes = new ArrayList<>();
        LightTheme.Colors bg = null;
        LightTheme.Colors fg = null;
        Boolean bold = null;
    }

    private final String code;
    private final Pattern filePattern;
    private List<PatternDef> patterns = new ArrayList<>();

    public SyntaxDef(Pattern filePattern, String code) {
        this.filePattern = filePattern;
        this.code = code;
    }

    public Pattern filePattern() {
        return filePattern;
    }

    public void read(Map<String, Object> data) {
        Map<String, Object> rules = TegmineController.objectObject(data, "rules");
        Map<String, PatternDef> rulesMap = new LinkedHashMap<>();
        if (null == rules) {
            return;
        }
        for (Map.Entry<String, Object> oneRule : rules.entrySet()) {
            PatternDef p = new PatternDef();
            p.name = oneRule.getKey();
            Map<String, Object> ruleConfig = TegmineController.objectObject(rules, p.name);
            String pattern = TegmineController.objectString(ruleConfig, "pattern", null);
            if (null == pattern) {
                logger.w("No pattern", p.name);
                continue;
            }
            p.pattern = Pattern.compile(pattern);
            p.group = TegmineController.objectInteger(ruleConfig, "group", 0);
            p.includesStr = TegmineController.objectList(ruleConfig, String.class, "includes");
            rulesMap.put(p.name, p);
        }
        Map<String, List<String>> groupsData = new LinkedHashMap<>();
        Map<String, Object> groups = TegmineController.objectObject(data, "groups");
        if (null != groups) {
            for (Map.Entry<String, Object> oneGroup : groups.entrySet()) {
                List<String> contents = TegmineController.objectList(groups, String.class, oneGroup.getKey());
                if (null != contents) {
                    groupsData.put(oneGroup.getKey(), contents);
                }
            }
        }
        Map<String, Object> mapping = TegmineController.objectObject(data, "mapping");
        if (null != mapping) {
            for (Map.Entry<String, Object> oneMapping : mapping.entrySet()) {
                String key = oneMapping.getKey();
                PatternDef p = rulesMap.get(key);
                if (null == p) {
                    logger.w("Invalid pattern:", key);
                    continue;
                }
                Map<String, Object> mappingConfig = TegmineController.objectObject(mapping, key);
                p.bg = inObject(mappingConfig, "bg");
                p.fg = inObject(mappingConfig, "fg");
                if (mappingConfig.containsKey("bold")) {
                    p.bold = TegmineController.objectBoolean(mappingConfig, "bold", false);
                }
                if (null != p.includesStr) {
                    for (String name : p.includesStr) {
                        List<String> groupContents = groupsData.get(name);
                        if (null == groupContents) {
                            groupContents = new ArrayList<>();
                            groupContents.add(name);
                        }
                        for (String groupContent : groupContents) {
                            if (rulesMap.containsKey(groupContent)) {
                                p.includes.add(rulesMap.get(groupContent));
                                continue;
                            }
                            logger.w("Invalid includes stmt:", groupContent, p.name);
                        }
                    }
                }
                logger.d("Configured pattern:", p.name, p.pattern, p.includesStr, p.includes);
                patterns.add(p);
            }
        }
    }

    LightTheme.Colors inObject(Map<String, Object> data, String name) {
        String colorName = TegmineController.objectString(data, name, null);
        if (TextUtils.isEmpty(colorName)) return null;
        return LightTheme.Colors.findColor(colorName);
    }

    private void apply(String data, SpannableStringBuilder builder, List<PatternDef> patterns) {
        int[] starts = new int[patterns.size()];

    }

    public void apply(String data, SpannableStringBuilder builder) {
        // Here is the story
    }

}
