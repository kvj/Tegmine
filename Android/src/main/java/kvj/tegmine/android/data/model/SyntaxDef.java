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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import kvj.tegmine.android.data.TegmineController;
import kvj.tegmine.android.data.model.util.Wrappers;
import kvj.tegmine.android.ui.theme.LightTheme;

/**
 * Created by vorobyev on 3/13/15.
 */
public class SyntaxDef {


    public enum Feature {Shrink};

    public static class SyntaxBlock {
        private final Wrappers.Pair<Integer> pair;
        private final PatternDef pattern;

        public SyntaxBlock(Wrappers.Pair<Integer> pair, PatternDef pattern) {
            this.pair = pair;
            this.pattern = pattern;
        }
    }

    public static class SyntaxedStringBuilder {
        private Logger logger = Logger.forInstance(this);

        private List<SyntaxBlock> blocks = new ArrayList<>();

        private final String data;

        public SyntaxedStringBuilder(String data) {
            this.data = data;
        }

        public void add(int start, int finish, PatternDef pattern) {
            blocks.add(new SyntaxBlock(new Wrappers.Pair<Integer>(start, finish), pattern));
        }

        public Iterable<SyntaxBlock> allInside(final int position) {
            return new Iterable<SyntaxBlock>() {
                @Override
                public Iterator<SyntaxBlock> iterator() {
                    return new Iterator<SyntaxBlock>() {

                        private int index = 0;

                        @Override
                        public boolean hasNext() {
                            if (index >= blocks.size()) {
                                return false;
                            }
                            if (blocks.get(index).pair.v1() > position || blocks.get(index).pair.v2() <= position) {
                                index++;
                                while (index < blocks.size()) {
                                    if (blocks.get(index).pair.v1() <= position && blocks.get(index).pair.v2() > position) {
                                        return true; // New index
                                    }
                                    index++;
                                }
                                return false;
                            }
                            return true;
                        }

                        @Override
                        public SyntaxBlock next() {
                            SyntaxBlock
                                syntaxBlock = blocks.get(index);
                            index++;
                            return syntaxBlock;
                        }

                        @Override
                        public void remove() {
                        }
                    };
                }
            };
        }

        public Iterable<Wrappers.Pair<Integer>> layout() {
            Set<Integer> indexes = new HashSet<>();
            indexes.add(0);
            indexes.add(data.length());
            for (int i = 0; i < blocks.size(); i++) {
                indexes.add(blocks.get(i).pair.v1());
                indexes.add(blocks.get(i).pair.v2());
            }
            final List<Integer> sorted = new ArrayList<>(indexes);
            Collections.sort(sorted);
//            logger.d("layout", starts, ends, sorted, data);
            final Iterator<Wrappers.Pair<Integer>> iterator = new Iterator<Wrappers.Pair<Integer>>() {

                private int index = 1;

                @Override
                public boolean hasNext() {
                    return index < sorted.size();
                }

                @Override
                public Wrappers.Pair<Integer> next() {

                    Wrappers.Pair<Integer> result = new Wrappers.Pair<>(sorted.get(index-1), sorted.get(index));
                    index++;
                    return result;
                }

                @Override
                public void remove() {
                }
            };
            return new Iterable<Wrappers.Pair<Integer>>() {
                @Override
                public Iterator<Wrappers.Pair<Integer>> iterator() {
                    return iterator;
                }
            };
        }

        public Collection<Wrappers.Pair<String>> allFeatures(int position) {
            List<Wrappers.Pair<String>> result = new ArrayList<>();
            Iterable<SyntaxBlock> patternDefs = blocks;
            if (position != -1) {
                patternDefs = allInside(position);
            }
            for (SyntaxBlock syntaxBlock : patternDefs) {
                for (String feature : syntaxBlock.pattern.features) {
                    result.add(new Wrappers.Pair<String>(feature, data.substring(syntaxBlock.pair.v1(), syntaxBlock.pair.v2())));
                }
            }
            return result;
        }

        private static boolean hasFeature(Feature feature, Feature... features) {
            if (null == features) {
                return false;
            }
            for (Feature f : features) { // $COMMENT
                if (f == feature) {
                    return true;
                }
            }
            return false;
        }

        public void span(LightTheme theme, SpannableStringBuilder builder, Feature... features) {
            // Make spans
            int spanShift = 0;
            for (Wrappers.Pair<Integer> startFinish : layout()) {
                if (startFinish.v1() == startFinish.v2()) {
                    continue;
                }
                LightTheme.Colors bg = null;
                LightTheme.Colors fg = null;
                Boolean bold = null;
                int start = builder.length();
                int finish = start + startFinish.v2() - startFinish.v1();
                String text = data.substring(startFinish.v1(), startFinish.v2());
                for (SyntaxBlock block : allInside(startFinish.v1())) {
//                    logger.d("Pattern:", patternDef.name, patternDef.shrink);
                    bg = SyntaxDef.plus(bg, block.pattern.bg);
                    fg = SyntaxDef.plus(fg, block.pattern.fg);
                    bold = SyntaxDef.plus(bold, block.pattern.bold);
                    if (hasFeature(Feature.Shrink, features)
                            && block.pattern.shrink > 0
                            && (finish - start > block.pattern.shrink)) {
                        text = text.substring(0, block.pattern.shrink) + "…";
                        finish = start + text.length();
                    }
                }
//                logger.d("pair", start, finish, bg, fg, data, text);
                builder.append(text);
                if (Boolean.TRUE.equals(bold)) { // Bold text
                    CharacterStyle span = new StyleSpan(Typeface.BOLD);
                    builder.setSpan(span, spanShift+start, spanShift+finish,
                                    SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                if (null != bg) { // Have color
                    CharacterStyle
                        span =
                        new BackgroundColorSpan(theme.color(bg, theme.backgroundColor()));
                    builder.setSpan(span, spanShift+start, spanShift+finish,
                                    SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                if (null != fg) { // Have color
                    CharacterStyle
                        span =
                        new ForegroundColorSpan(theme.color(fg, theme.textColor()));
                    builder.setSpan(span, spanShift+start, spanShift+finish,
                                    SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }

    }

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
        int shrink = 0;
        Set<String> features = new HashSet<>();
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
            String features = TegmineController.objectString(ruleConfig, "feature", null);
            if (!TextUtils.isEmpty(features)) {
                for (String f : features.split(" ")) {
                    p.features.add(f);
                }
            }
            p.shrink = TegmineController.objectInteger(ruleConfig, "shrink", 0);
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
                logger.d("Configured pattern:", p.name, p.pattern, p.includesStr, p.includes, p.shrink);
                patterns.add(p);
            }
        }
    }

    LightTheme.Colors inObject(Map<String, Object> data, String name) {
        String colorName = TegmineController.objectString(data, name, null);
        if (TextUtils.isEmpty(colorName)) return null;
        return LightTheme.Colors.findColor(colorName);
    }

    private void append(SyntaxedStringBuilder builder, int from, int to, PatternDef current) {
        if (from < to && null != current) { // Have some data
            builder.add(from, to, current);
        }
    }

    private void apply(SyntaxedStringBuilder builder, int from, int to, List<PatternDef> patterns, PatternDef current) {
        Matcher[] matches = new Matcher[patterns.size()];
        int start = from;
        append(builder, from, to, current);
        while (true) {
            String target = builder.data.substring(start, to);
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
                break;
            } else {
                Matcher m = matches[closest];
                PatternDef p = patterns.get(closest);
                // Call recursive
                apply(builder, start+closestStart, start+closestStart+closestLength,
                        p.includes, p);
                start = start+closestStart + closestLength;
//                logger.d("After apply", start, target, closestStart, p.pattern, m.group(p.group));
            }
        }
    }

    public static <T> T plus(T curr, T add) {
        if (add != null) {
            return add;
        }
        return curr;
    }

    public void apply(SyntaxedStringBuilder builder) {
        // Here is the story
        apply(builder, 0, builder.data.length(), patterns, null);
    }

}
