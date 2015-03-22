package kvj.tegmine.android.data.model;

import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import org.kvj.bravo7.log.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
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

    private void append(LightTheme theme, String data, int from, int to, SpannableStringBuilder builder,
                        LightTheme.Colors bg, LightTheme.Colors fg, Boolean bold) {
        if (from < to) { // Have some data
            String text = data.substring(from, to);
            int start = builder.length();
            int end = start + text.length();
            builder.append(text);
            if (Boolean.TRUE.equals(bold)) { // Bold text
                CharacterStyle span = new StyleSpan(Typeface.BOLD);
                builder.setSpan(span, start, end, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (null != bg) { // Have color
                CharacterStyle span = new BackgroundColorSpan(theme.color(bg, theme.backgroundColor()));
                builder.setSpan(span, start, end, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (null != fg) { // Have color
                CharacterStyle span = new ForegroundColorSpan(theme.color(fg, theme.textColor()));
                builder.setSpan(span, start, end, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    private void apply(LightTheme theme, String data, SpannableStringBuilder builder, List<PatternDef> patterns,
                       LightTheme.Colors bg, LightTheme.Colors fg, Boolean bold) {
        Matcher[] matches = new Matcher[patterns.size()];
        int start = 0;
        while (true) {
            String target = data.substring(start);
            int closest = -1;
            int closestStart = -1;
            int closestLength = 0;
            for (int i = 0; i < patterns.size(); i++) { // Check every pattern and find best match
                PatternDef pattern = patterns.get(i);
                Matcher m = pattern.pattern.matcher(target);
                if (m.find()) { // OK
                    matches[i] = m;
                    int st = m.start(pattern.group);
                    int len = m.group(pattern.group).length();
                    if ((st < closestStart) || ((len > closestLength) && (closestStart == st)) || (closest == -1)) {
                        // First match or group starts earlier or match is longer
                        closest = i;
                        closestStart = st;
                        closestLength = len;
                    }
                } else {
                    matches[i] = null;
                }
            }
//            logger.d("apply", start, target, patterns.size(), closest, closestStart);
            if (closest == -1) { // Not found
                append(theme, target, 0, target.length(), builder, bg, fg, bold);
                break;
            } else {
                Matcher m = matches[closest];
                PatternDef p = patterns.get(closest);
                append(theme, target, 0, closestStart, builder, bg, fg, bold);
                // Call recursive
                apply(theme, m.group(p.group), builder, p.includes,
                        plus(bg, p.bg), plus(fg, p.fg), plus(bold, p.bold));
                start = start+closestStart + closestLength;
//                logger.d("After apply", start, target, closestStart, p.pattern, m.group(p.group));
            }
        }
    }

    <T> T plus(T curr, T add) {
        if (add != null) {
            return add;
        }
        return curr;
    }

    public void apply(LightTheme theme, String data, SpannableStringBuilder builder) {
        // Here is the story
        apply(theme, data, builder, patterns, null, null, null);
    }

}
