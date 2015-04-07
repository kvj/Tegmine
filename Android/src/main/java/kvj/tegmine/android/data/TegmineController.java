package kvj.tegmine.android.data;

import android.os.Environment;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.widget.TextView;

import org.kvj.bravo7.log.Logger;
import org.kvj.bravo7.util.Listeners;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import kvj.tegmine.android.Tegmine;
import kvj.tegmine.android.data.def.FileSystemException;
import kvj.tegmine.android.data.def.FileSystemItem;
import kvj.tegmine.android.data.def.FileSystemProvider;
import kvj.tegmine.android.data.impl.provider.local.LocalFileSystemProvider;
import kvj.tegmine.android.data.model.AutoThemeChanger;
import kvj.tegmine.android.data.model.LineMeta;
import kvj.tegmine.android.data.model.ProgressListener;
import kvj.tegmine.android.data.model.SyntaxDef;
import kvj.tegmine.android.data.model.TemplateDef;
import kvj.tegmine.android.ui.theme.LightTheme;

/**
 * Created by kvorobyev on 2/13/15.
 */
public class TegmineController {

    private static final int SPACES_IN_TAB = 2;
    private final AutoThemeChanger autoThemeChanger;
    private Map<String, FileSystemProvider> fileSystemProviders = new LinkedHashMap<>();
    private Map<String, TemplateDef> templates = new LinkedHashMap<>();
    private FileSystemProvider defaultProvider = null;
    private LightTheme theme = new LightTheme();
    private Logger logger = Logger.forInstance(this);
    private Map<String, Object> config = new HashMap<>();
    private Map<String, LightTheme> colorSchemes = new HashMap<>();
    private Map<String, SyntaxDef> syntaxes = new HashMap<>();
    private int watchSeconds = 60;

    private boolean newLineBefore = true;
    private boolean newLineAfter = false;
    private String selectedTheme = "default";
    private boolean scrollToBottom = true;
    private String clientName = "Undefined";
    private boolean showNumbers = false;
    private boolean wrapLines = true;

    private Listeners<ProgressListener> progressListeners = new Listeners<>();

    public TegmineController() {
        autoThemeChanger = new AutoThemeChanger() {

            @Override
            public void onNewTheme(final String name) {
                if (colorSchemes.containsKey(name) && !selectedTheme.equals(name)) { // Theme changed
                    selectedTheme = name;
                    progressListeners().emit(new Listeners.ListenerEmitter<ProgressListener>() {
                        @Override
                        public boolean emit(ProgressListener listener) {
                            listener.themeChanged();
                            return true;
                        }
                    });
                }
            }
        };
        try {
            reloadConfig();
        } catch (FileSystemException e) {
            logger.e(e, "Failed to load config");
        }
    }

    public FileSystemProvider fileSystemProvider() {
        return defaultProvider;
    }

    public FileSystemProvider fileSystemProvider(String name) {
        if (null == name) { // Default is requested:
            return fileSystemProvider();
        }
        FileSystemProvider provider = fileSystemProviders.get(name);
        if (null != provider) { // OK
            return provider;
        }
        return fileSystemProvider(); // Failsafe
    }

    public LightTheme theme() {
        return colorSchemes.get(selectedTheme);
    }

    public List<LineMeta> makeFileLayout(FileSystemItem item) throws FileSystemException {
        // Read file and make list with line offsets
        List<LineMeta> offsets = new ArrayList<>();
        InputStream stream = fileSystemProvider().read(item);
        long offset = 0;
        long from = 0;
        int data;
        try {
            ByteArrayOutputStream oneLine = new ByteArrayOutputStream();
            while ((data = stream.read()) >= 0) { // Read until end
                offset++;
                if (data == '\n') { // New line
                    LineMeta meta = new LineMeta(indent(oneLine.toString("utf-8")), from);
                    oneLine.reset();
                    offsets.add(meta);
                    from = offset;
                } else {
                    oneLine.write(data);
                }
            }
            if (from<offset) { // Add last
                LineMeta meta = new LineMeta(indent(oneLine.toString("utf-8")), from);
                offsets.add(meta);
            }
            stream.close();
            return offsets;
        } catch (IOException e) {
            logger.e(e, "Failed to make layout");
            throw new FileSystemException("Failed to read file");
        }
    }

    public void loadFilePart(List<LineMeta> buffer, FileSystemItem item, long from, int lines) throws FileSystemException {
        try {
            InputStream stream = fileSystemProvider().read(item);
            long skipped = stream.skip(from);
            if (from != skipped) { // Invalid offset
                stream.close();
                throw new FileSystemException("Invalid position");
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "utf-8"));
            String line;
            int linesRead = 0;
            while ((line = reader.readLine()) != null) { // Read lines one by one
                LineMeta meta = new LineMeta(indent(line), -1);
                meta.data(line.trim());
                buffer.add(meta);
                linesRead++;
                if ((lines > 0) && (linesRead >= lines)) { // Enough
                    break;
                }
            }
            stream.close();
            return;
        } catch (IOException e) {
            logger.e(e, "Failed to read file");
            throw new FileSystemException("Failed to read file");
        }
    }

    public int indent(String line) {
        if (TextUtils.isEmpty(line)) { // Empty line - indent undefined
            return -1;
        }
        if (line.trim().length() == 0) {
            return -1;
        }
        int spaces = 0;
        for (int i = 0; i < line.length(); i++) { // Search for a first non space/tab
            if (line.charAt(i) == '\t') { // Tab
                spaces += spacesInTab();
            } else if (line.charAt(i) == ' ') { // Space
                spaces++;
            } else {
                // Everything else
                break;
            }
        }
        return spaces / spacesInTab();
    }

    public int spacesInTab() {
        return SPACES_IN_TAB;
    }

    public void linesForEditor(List<LineMeta> lines, StringBuilder buffer) {
        for (int i = 0; i < lines.size(); i++) { // Iterate over lines
            LineMeta line = lines.get(i);
            String trimmed = line.data();
            if (i>0) { // Add new line
                buffer.append('\n');
            }
            if (trimmed.length() == 0) { // Empty
                continue;
            }
            for (int j = 0; j < line.indent() * spacesInTab(); j++) { // Add spaces
                buffer.append(' ');
            }
            buffer.append(trimmed);
        }
    }

    public void applyHeaderStyle(TextView view) {
        view.setTextColor(theme().textColor());
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, theme().headerTextSp());
    }

    public void writeEdited(OutputStream stream, String contents, boolean doEdit) throws FileSystemException {
        if (TextUtils.isEmpty(contents)) { // No data entered
            return;
        }
        BufferedOutputStream bufferedStream = null;
        try {
            bufferedStream = new BufferedOutputStream(stream);
            String[] lines = contents.split("\n");
            if (!doEdit && newLineBefore) { // Append mode - leading new line, if set
                bufferedStream.write('\n');
            }
            for (int i = 0; i < lines.length; i++) { // Write lines one by one
                String line = lines[i];
                if (i>0) { // Add new line
                    bufferedStream.write('\n');
                }
                for (int j = 0; j < indent(line); j++) { // Add tabs
                    bufferedStream.write('\t');
                }
                bufferedStream.write(line.trim().getBytes("utf-8"));
            }
            if (newLineAfter) { // Trailing new line
                bufferedStream.write('\n');
            }
        } catch (Throwable t) { // IO error
            logger.e(t, "Error writing contents:");
            throw new FileSystemException("IO error");
        } finally {
            if (null != bufferedStream) {
                try {
                    bufferedStream.close();
                } catch (Throwable t) {
                    logger.e(t, "Failed to commit stream");
                }
            }
        }
    }

    private static Pattern urlPattern = Pattern.compile("^tegmine\\+([a-z0-9_]+)://(.*)$");

    public FileSystemItem fromURL(String url) {
        if (TextUtils.isEmpty(url)) {
            return null;
        }
        Matcher m = urlPattern.matcher(url);
        if (!m.find()) { // Not found
            logger.w("Failed to load item from URL:", url);
            return null;
        }
        try {
            logger.d("fromURL", m.group(1), m.group(2), fileSystemProviders.containsKey(m.group(1)));
            return fileSystemProvider(m.group(1)).fromURL(m.group(2));
        } catch (FileSystemException e) {
            e.printStackTrace();
            return null;
        }
    }

    private int readObject(List<LineMeta> lines, int from, Map<String, Object> to, int indent) {
        int i = from;
        for (; i<lines.size(); i++) {
            LineMeta line = lines.get(i);
            int ind = line.indent();
            if (ind == -1) { // Empty line
                continue;
            }
            if (ind < indent) { // Out of requested indent
                return i-1;
            }
            if (ind > indent) { // Something broken - skip
                logger.d("Invalid config indent:", line, ind, indent);
                continue;
            }
            // Requested indent
            boolean objStart = false;
            String lineStr = line.data();
            if (lineStr.startsWith("#")) { // Comment
                continue;
            }
            if ((lineStr.startsWith("[") && lineStr.endsWith("]"))) { // Remove brackets
                lineStr = lineStr.substring(1, lineStr.length()-1);
                objStart = true;
            }
            int spacePos = lineStr.indexOf(' ');
            if (spacePos == -1) { // No space inside
                objStart = true;
            }
            boolean arrayMode = false;
            if (lineStr.endsWith("[]")) {
                arrayMode = true;
                lineStr = lineStr.substring(0, lineStr.length()-2);
            }
            if (objStart) { // Object will start from here
                Map<String, Object> values = new LinkedHashMap<>();
                i = readObject(lines, i+1, values, indent+1);
                if (arrayMode) {
                    List<String> valuesList = new ArrayList<>();
                    valuesList.addAll(values.keySet());
                    to.put(lineStr, valuesList);
                } else {
                    to.put(lineStr, values);
                }
                continue;
            }
            // Value mode
            to.put(lineStr.substring(0, spacePos).trim(), lineStr.substring(spacePos).trim());
        }
        return i;
    }

    private void setupFailsave() throws FileSystemException {
        templates.clear();
        fileSystemProviders.clear();
        colorSchemes.clear();
        syntaxes.clear();
        autoThemeChanger.clear();
        if (!fileSystemProviders.containsKey("sdcard")) { // Failsafe - should always be there
            FileSystemProvider provider = new LocalFileSystemProvider(Environment.getExternalStorageDirectory(), "sdcard");
            provider.label("SD Card");
            fileSystemProviders.put("sdcard", provider);
        }
        if (null == defaultProvider) { // No default provider
            defaultProvider = fileSystemProviders.get("sdcard");
        }
        colorSchemes.put("default", new LightTheme());
        selectedTheme = "default";
    }

    private Map<String, Object> file2Object(FileSystemItem item) throws FileSystemException {
        List<LineMeta> lines = new ArrayList<>();
        loadFilePart(lines, item, 0, -1);
        Map<String, Object> object = new HashMap<>();
        readObject(lines, 0, object, 0);
        return object;
    }

    private void reloadSyntaxes() {
        Map<String, Object> items = objectObject(config, "syntax");
        if (null == items) {
            return;
        }
        for (Map.Entry<String, Object> oneBlock : items.entrySet()) {
            String key = oneBlock.getKey();
            Map<String, Object> keyConfig = objectObject(items, key);
            try {
                Pattern pattern = Pattern.compile(String.format("^%s$", objectString(keyConfig, "pattern", "")));
                SyntaxDef sy = new SyntaxDef(pattern, key);
                FileSystemItem syntaxFile = fromURL(objectString(keyConfig, "file", null));
                if (null == syntaxFile) {
                    logger.w("Failed to load color scheme:", key, keyConfig);
                    continue;
                }
                Map<String, Object> oneSyntaxData = file2Object(syntaxFile);
                sy.read(oneSyntaxData);
                syntaxes.put(key, sy);
            } catch (Exception e) {
                logger.w(e, "Failed to read syntax file");
                continue;
            }
        }
    }

    public SyntaxDef findSyntax(FileSystemItem item) {
        for (SyntaxDef sy : syntaxes.values()) {
            if (sy.filePattern().matcher(item.name).find()) {
                logger.d("Find syntax for:", item.name, sy.filePattern());
                return sy;
            }
        }
        logger.w("No syntax found:", item.name);
        return null;
    }

    private void reloadColorSchemes() {
        Map<String, Object> colorsConfig = objectObject(config, "colorschemes");
        if (null != colorsConfig) {
            for (Map.Entry<String, Object> oneLine : colorsConfig.entrySet()) {
                FileSystemItem fileItem = fromURL(oneLine.getValue().toString());
                if (null == fileItem) {
                    logger.w("Failed to load color scheme:", oneLine.getKey(), oneLine.getValue());
                    continue;
                }
                try {
                    Map<String, Object> themeConfig = file2Object(fileItem);
                    LightTheme newTheme = new LightTheme();
                    newTheme.dark(objectBoolean(themeConfig, "dark", false));
                    for (Map.Entry<String, Object> oneThemeLine : themeConfig.entrySet()) {
                        boolean loaded = newTheme.loadColor(oneThemeLine.getKey(), oneThemeLine.getValue().toString());
                        if (!loaded) {
                            logger.w("Theme line ignored:", oneThemeLine.getKey(), oneThemeLine.getValue());
                        }
                    }
                    colorSchemes.put(oneLine.getKey(), newTheme);
                } catch (FileSystemException e) {
                    logger.w(e, "Failed to read theme file", fileItem.toURL());
                }
            }
        }
        String themeStr = objectString(config, "colorscheme", "default");
        if (colorSchemes.containsKey(themeStr)) {
            selectedTheme = themeStr;
        }
    }

    public void reloadConfig() throws FileSystemException {
        FileSystemException ex = null;
        setupFailsave();
        try {
            String path = Tegmine.getInstance().getStringPreference("p_config_file", "");
            if (TextUtils.isEmpty(path)) { // Not set
                throw new FileSystemException("Config file not set");
            }
            FileSystemItem configItem = fromURL(path);
            if (null == configItem) { // Invalid
                throw new FileSystemException("Failed to read config file");
            }
            List<LineMeta> lines = new ArrayList<>();
            loadFilePart(lines, configItem, 0, -1);
            config = new HashMap<>();
            readObject(lines, 0, config, 0);
            defaultProvider = null;
            newLineBefore = objectBoolean(config, "newLineBefore", newLineBefore);
            newLineAfter = objectBoolean(config, "newLineAfter", newLineAfter);
            scrollToBottom = objectBoolean(config, "scrollToBottom", true);
            watchSeconds = objectInteger(config, "watchSeconds", watchSeconds);
            clientName = objectString(config, "client", clientName);
            showNumbers = objectBoolean(config, "showNumbers", showNumbers);
            wrapLines = objectBoolean(config, "wrapLines", wrapLines);
            reloadStorage(config);
            reloadTemplates(config);
            autoThemeChanger.setup(config);
            reloadColorSchemes();
            reloadSyntaxes();
        } catch (FileSystemException e) {
            ex = e;
        }
        if (null != ex) { // Report status
            throw ex;
        }
    }

    private void reloadTemplates(Map<String, Object> config) {
        Map<String, Object> templatesConfig = objectObject(config, "templates");
        if (null != templatesConfig) {
            for (String key : templatesConfig.keySet()) { // Create new instances
                Map<String, Object> conf = objectObject(templatesConfig, key);
                if (null == conf) { // Invalid
                    continue;
                }
                TemplateDef tmpl = new TemplateDef(key, objectString(conf, "template", ""));
                tmpl.label(objectString(conf, "label", null));
                tmpl.key(objectString(conf, "key", null));
                templates.put(key, tmpl);
            }
        }
    }

    private void reloadStorage(Map<String, Object> config) throws FileSystemException {
        Map<String, Object> storageConfig = objectObject(config, "storage");
        if (null != storageConfig) { // Have config
            for (String key : storageConfig.keySet()) { // Create new instances
                Map<String, Object> conf = objectObject(storageConfig, key);
                if (null == conf) { // Invalid
                    continue;
                }
                FileSystemProvider provider = null;
                String type = objectString(conf, "type", "local");
                if ("local".equals(type)) { // Local storage
                    String storagePath = objectString(conf, "path", null);
                    provider = new LocalFileSystemProvider(new File(storagePath), key);
                }
                if (null != provider) {
                    provider.label(objectString(conf, "label", null));
                    fileSystemProviders.put(key, provider);
                    if (objectBoolean(conf, "default", false)) { // Default
                        defaultProvider = provider;
                    }
                }
            }
        }
    }

    public Map<String, Object> config() {
        return config;
    }

    public static String objectString(Map<String, Object> obj, String name, String def) {
        Object value = obj.get(name);
        if (null == value || !(value instanceof String)) {
            return def;
        }
        return (String) value;
    }

    public static Map<String, Object> objectObject(Map<String, Object> obj, String name) {
        Object value = obj.get(name);
        if (null == value || !(value instanceof Map)) {
            return null;
        }
        return (Map) value;
    }

    public static <T> List<T> objectList(Map<String, Object> obj, Class<T> cls, String name) {
        Object value = obj.get(name);
        if (null == value || !(value instanceof List)) {
            return null;
        }
        return (List) value;
    }

    public static boolean objectBoolean(Map<String, Object> obj, String name, boolean def) {
        if (!obj.containsKey(name)) { // Def
            return def;
        }
        String value = objectString(obj, name, "n");
        return (value.equalsIgnoreCase("y") || value.equalsIgnoreCase("yes") || value.equals("1") || value.equalsIgnoreCase("true"));
    }

    public static int objectInteger(Map<String, Object> obj, String name, int def) {
        String value = objectString(obj, name, "");
        if (TextUtils.isEmpty(value)) return def;
        try {
            return Integer.parseInt(value, 10);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public Map<String, TemplateDef> templates() {
        return templates;
    }

    public boolean scrollToBottom() {
        return scrollToBottom;
    }

    public boolean isRoot(FileSystemItem item) {
        return fileSystemProvider(item.providerName()).root().equals(item);
    }

    public static class TemplateApplyResult {
        private final String value;
        private final int cursor;

        public TemplateApplyResult(String value, int cursor) {
            this.value = value;
            this.cursor = cursor;
        }

        public int cursor() {
            return cursor;
        }

        public String value() {
            return value;
        }
    }

    private static Pattern tmplPattern = Pattern.compile("\\$\\{([^\\}]+?)\\}");
    public TemplateApplyResult applyTemplate(String text, TemplateDef tmpl) {
        Matcher m = tmplPattern.matcher(tmpl.template());
        StringBuffer buffer = new StringBuffer();
        buffer.append(text);
        int cursor = -1;
        while (m.find()) {
            String value = m.group(1);
            StringBuilder repl = new StringBuilder("???");
            if ("t".equals(value)) { // Tab
                repl.setLength(0);
                for (int i = 0; i < spacesInTab(); i++) { // Add spaces
                    repl.append(' ');
                }
            }
            if ("n".equals(value)) { // New line
                repl.setLength(0);
                repl.append('\n');
            }
            if ("client".equals(value)) { // Client name
                repl.setLength(0);
                repl.append(clientName);
            }
            if ("c".equals(value)) { // Cursor - remove
                repl.setLength(0);
            }
            if (value.startsWith("d:")) { // Date format
                repl.setLength(0);
                repl.append(new SimpleDateFormat(value.substring(2)).format(new Date()));
            }
            // logger.d("Template:", tmpl.template(), value, repl);
            m.appendReplacement(buffer, repl.toString());
            if ("c".equals(value)) { // Cursor - remember position
                cursor = buffer.length();
            }
        }
        m.appendTail(buffer);
        if (-1 == cursor) { // Not set yet
            cursor = buffer.length();
        }
        return new TemplateApplyResult(buffer.toString(), cursor);
    }

    public void addIndent(StringBuilder sb, int indent) {
        for (int i = 0; i < indent * spacesInTab(); i++) { // Add spaces
            sb.append(' ');
        }
    }

    public String part(List<LineMeta> lines, int from) {
        StringBuilder sb = new StringBuilder();
        int topIndent = lines.get(from).indent();
        sb.append(lines.get(from).data()); // Top line
        for (int i = from+1; i < lines.size(); i++) { // Add sub-lines
            LineMeta line = lines.get(i);
            int indent = line.indent();
            if (indent <= topIndent) { // Out of block
                break;
            }
            sb.append('\n');
            if (indent>0) { // Indent relative
                addIndent(sb, indent - topIndent);
            }
            sb.append(line.data());
        }
        return sb.toString();
    }

    public Listeners<ProgressListener> progressListeners() {
        return progressListeners;
    }

    public SyntaxDef.SyntaxedStringBuilder syntaxize(SyntaxDef syntax, LineMeta line) {
        SyntaxDef.SyntaxedStringBuilder syntaxedStringBuilder = new SyntaxDef.SyntaxedStringBuilder(line.data());
        syntax.apply(syntaxedStringBuilder);
        return syntaxedStringBuilder;
    }

    public SyntaxDef.SyntaxedStringBuilder applyTheme(SyntaxDef syntax, LineMeta line, SpannableStringBuilder builder) {
        if (null == syntax) {
            builder.append(line.data());
            return null;
        }
        SyntaxDef.SyntaxedStringBuilder syntaxedStringBuilder = new SyntaxDef.SyntaxedStringBuilder(line.data());
        syntax.apply(syntaxedStringBuilder);
        syntaxedStringBuilder.span(theme(), builder);
        return syntaxedStringBuilder;
    }

    public int watchSeconds() {
        return watchSeconds;
    }

    public TemplateDef templateFromKeyEvent(KeyEvent keyEvent) {
        String label = new String(""+keyEvent.getDisplayLabel()).toLowerCase();
        logger.d("template key:", label);
        for (TemplateDef tmpl : templates().values()) {
            if (tmpl.key() != null && tmpl.key().equals(label)) {
                return tmpl;
            }
        }
        return null;
    }

    public boolean showNumbers() {
        return showNumbers;
    }

    public boolean wrapLines() {
        return wrapLines;
    }

    private Pattern signPattern = Pattern.compile("^\\s*(\\S)\\s\\S.*$");

    public String signInLine(String line) {
        Matcher m = signPattern.matcher(line);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    public Collection<String> fileSystemProviders() {
        return fileSystemProviders.keySet();
    }

    public void ambientLightChanged(float value) {
        autoThemeChanger.ambientLightChanged(value);
    }
}
