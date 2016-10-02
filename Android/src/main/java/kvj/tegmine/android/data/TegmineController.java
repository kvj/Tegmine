package kvj.tegmine.android.data;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Environment;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.webkit.MimeTypeMap;
import android.widget.TextView;
import android.widget.Toast;

import org.kvj.bravo7.log.Logger;
import org.kvj.bravo7.ng.Controller;
import org.kvj.bravo7.ng.widget.AppWidgetController;
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

import kvj.tegmine.android.R;
import kvj.tegmine.android.data.def.FileSystemException;
import kvj.tegmine.android.data.def.FileSystemItem;
import kvj.tegmine.android.data.def.FileSystemProvider;
import kvj.tegmine.android.data.def.WidgetExtension;
import kvj.tegmine.android.data.impl.extension.NotificationsExtension;
import kvj.tegmine.android.data.impl.provider.asset.AssetFileSystemProvider;
import kvj.tegmine.android.data.impl.provider.local.LocalFileSystemProvider;
import kvj.tegmine.android.data.model.AutoThemeChanger;
import kvj.tegmine.android.data.model.LineMeta;
import kvj.tegmine.android.data.model.ProgressListener;
import kvj.tegmine.android.data.model.SyntaxDef;
import kvj.tegmine.android.data.model.TemplateDef;
import kvj.tegmine.android.data.model.util.Wrappers;
import kvj.tegmine.android.ui.appwidget.Widget00;
import kvj.tegmine.android.ui.theme.LightTheme;

/**
 * Created by kvorobyev on 2/13/15.
 */
public class TegmineController extends Controller {


    private Map<String, WidgetExtension> widgetExtensions = new HashMap<>();

    private static final int SPACES_IN_TAB = 2;
    private final AutoThemeChanger autoThemeChanger;
    private final EditorsController editors;
    private Map<String, FileSystemProvider> fileSystemProviders = new LinkedHashMap<>();
    private Map<String, TemplateDef> templates = new LinkedHashMap<>();
    private FileSystemProvider defaultProvider = null;
    private Logger logger = Logger.forInstance(this);
    private Map<String, Object> config = new HashMap<>();
    private Map<String, Integer> sizes = new HashMap<>();
    private Map<String, LightTheme> colorSchemes = new HashMap<>();
    private Map<String, SyntaxDef> syntaxes = new LinkedHashMap<>();
    private int watchSeconds = 60;

    private boolean newLineBefore = true;
    private boolean newLineAfter = false;
    private String selectedTheme = "default";
    private boolean scrollToBottom = true;
    private String clientName = "Undefined";
    private boolean showNumbers = false;
    private boolean wrapLines = true;

    private Listeners<ProgressListener> progressListeners = new Listeners<>();

    public TegmineController(Context context) {
        super(context, "Tegmine:");
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
        widgetExtensions.put("notifications", new NotificationsExtension(this));
        try {
            reloadConfig();
        } catch (FileSystemException e) {
            logger.e(e, "Failed to load config");
        }
        editors = new EditorsController(this);
    }

    public FileSystemProvider fileSystemProvider(FileSystemItem item) {
        if (null == item) { // Failsafe
            return defaultProvider;
        }
        return fileSystemProvider(item.providerName());
    }

    public FileSystemProvider fileSystemProvider(String name) {
        if (null == name) { // Default is requested:
            return defaultProvider;
        }
        FileSystemProvider provider = fileSystemProviders.get(name);
        if (null != provider) { // OK
            return provider;
        }
        return defaultProvider; // Failsafe
    }

    public LightTheme theme() {
        return colorSchemes.get(selectedTheme);
    }

    public List<LineMeta> makeFileLayout(FileSystemItem item) throws FileSystemException {
        // Read file and make list with line offsets
        List<LineMeta> offsets = new ArrayList<>();
        FileSystemProvider provider = fileSystemProvider(item);
        InputStream stream = provider.read(item);
        long offset = 0;
        long from = 0;
        int data;
        try {
            ByteArrayOutputStream oneLine = new ByteArrayOutputStream();
            while ((data = stream.read()) >= 0) { // Read until end
                offset++;
                if (data == '\n') { // New line
                    LineMeta meta = new LineMeta(indent(provider, oneLine.toString("utf-8")), from);
                    oneLine.reset();
                    offsets.add(meta);
                    from = offset;
                } else {
                    oneLine.write(data);
                }
            }
            if (from<offset) { // Add last
                LineMeta meta = new LineMeta(indent(provider, oneLine.toString("utf-8")), from);
                offsets.add(meta);
            }
            stream.close();
            return offsets;
        } catch (IOException e) {
            logger.e(e, "Failed to make layout");
            throw new FileSystemException("Failed to read file");
        }
    }

    private static String ltrim(String s) {
        int start = 0, last = s.length() - 1;
        while ((start <= last) && (s.charAt(start) <= ' ')) {
            start++;
        }
        if (start == 0) {
            return s;
        }
        return s.substring(start);
    }

    public void split(FileSystemProvider provider, List<LineMeta> lines, String text) {
        if (TextUtils.isEmpty(text)) { // No lines
            return;
        }
        String[] lineStrs = text.split("\n");
        for (String line : lineStrs) {
            LineMeta meta = new LineMeta(indent(provider, line, false), -1);
            meta.data(ltrim(line));
            lines.add(meta);
        }
    }

    public int foldEnd(List<LineMeta> buffer, int line) {
        int indent = buffer.get(line).indent();
        if (indent == -1) {
            return -1;
        }
        for (int i = line+1; i < buffer.size(); i++) {
            if (buffer.get(i).indent() <= indent) {
                return i;
            }
        }
        return buffer.size();
    }

    public Wrappers.Pair<Integer> findIn(List<LineMeta> buffer, String def) {
        int start = 0;
        int len = buffer.size();
        if (!TextUtils.isEmpty(def)) {
            if (def.startsWith("?") || def.startsWith("/")) {
                // Search backwards
                int sstart = buffer.size() - 1;
                int ssend = -1;
                int sdir = -1;
                if (def.startsWith("/")) { // Search forward
                    ssend = buffer.size();
                    sstart = 0;
                    sdir = 1;
                }
                String lookFor = def.substring(1);
                for (int i = sstart; i != ssend; i += sdir) {
                    if (buffer.get(i).data().indexOf(lookFor) != -1) {
                        // Found
                        int indented = foldEnd(buffer, i);
                        if (-1 != indented) {
                            return new Wrappers.Pair<>(i, indented - i);
                        }
                        break; // Found
                    }
                }
            }
            return new Wrappers.Pair<>(start, 0);
        }
        return new Wrappers.Pair<>(start, len);
    }

    public void loadFilePart(List<LineMeta> buffer, FileSystemItem item, SyntaxDef syntax, long from, int lines) throws FileSystemException {
        try {
            InputStream stream = fileSystemProvider(item).read(item);
            long skipped = stream.skip(from);
            if (from != skipped) { // Invalid offset
                stream.close();
                throw new FileSystemException("Invalid position");
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "utf-8"));
            String line;
            int linesRead = 0;
            while ((line = reader.readLine()) != null) { // Read lines one by one
                LineMeta meta = new LineMeta(indent(fileSystemProvider(item), line), -1);
                meta.data(line.trim());
                if ((lines > 0) && (linesRead >= lines) && meta.indent() == 0) { // Enough
                    break;
                }
                buffer.add(meta);
                linesRead++;
            }
            stream.close();
            if (syntax != null && syntax.hasFolding()) { // Apply folding patterns
                for (int i = 0; i < buffer.size(); i++) { // Check every line except last
                    LineMeta ll = buffer.get(i);
                    if (syntax.folded(ll)) { // Folded
                        ll.folded(true); // Hide any children lines
                        while (i+1 < buffer.size()) { // Have children
                            LineMeta sl = buffer.get(i+1);
                            if (sl.indent() > ll.indent() || sl.indent() == -1) { // Child
                                sl.visible(false);
                                i++;
                            } else {
                                break;
                            }
                        }
                    }
                }
            }
            return;
        } catch (IOException e) {
            logger.e(e, "Failed to read file");
            throw new FileSystemException("Failed to read file");
        }
    }

    public int indent(FileSystemProvider provider, String line) {
        return indent(provider, line, true);
    }

    public int indent(FileSystemProvider provider, String line, boolean trimEmpty) {
        if (TextUtils.isEmpty(line)) { // Empty line - indent undefined
            return -1;
        }
        if (line.trim().length() == 0 && trimEmpty) {
            return -1;
        }
        int spaces = 0;
        for (int i = 0; i < line.length(); i++) { // Search for a first non space/tab
            if (line.charAt(i) == '\t') { // Tab
                spaces += spacesInTab(provider);
            } else if (line.charAt(i) == ' ') { // Space
                spaces++;
            } else {
                // Everything else
                break;
            }
        }
        return spaces / spacesInTab(provider);
    }

    public int spacesInTab(FileSystemProvider provider) {
        return provider.tabSize();
    }

    public void linesForEditor(FileSystemProvider provider, List<LineMeta> lines, SpannableStringBuilder buffer, SyntaxDef syntax) {
        for (int i = 0; i < lines.size(); i++) { // Iterate over lines
            LineMeta line = lines.get(i);
            String trimmed = line.data();
            if (i>0) { // Add new line
                buffer.append('\n');
            }
            for (int j = 0; j < line.indent() * spacesInTab(provider); j++) { // Add spaces
                buffer.append(' ');
            }
            if (null == syntax) { //
                buffer.append(trimmed);
            } else {
                SyntaxDef.SyntaxedStringBuilder syntaxedStringBuilder = new SyntaxDef.SyntaxedStringBuilder(trimmed);
                syntax.apply(syntaxedStringBuilder);
                syntaxedStringBuilder.span(theme(), buffer);
            }
        }
    }

    public void applyHeaderStyle(TextView view) {
        view.setTextColor(theme().textColor());
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, theme().headerTextSp());
    }

    public void writeEdited(FileSystemProvider provider, OutputStream stream, String contents, boolean doEdit) throws FileSystemException {
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
                for (int j = 0; j < indent(provider, line); j++) { // Add tabs
                    if (provider.useTab()) { // Tabs in output
                        bufferedStream.write('\t');
                    } else { // Spaces mode
                        for (int k = 0; k < provider.tabSize(); k++) { // Add spaces
                            bufferedStream.write(' ');
                        }
                    }
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
                    updateWidgets();
                } catch (Throwable t) {
                    logger.e(t, "Failed to commit stream");
                }
            }
        }
    }

    private void updateWidgets() {
        AppWidgetController controller = AppWidgetController.instance(context);
        controller.updateAll(new Widget00());

    }

    private static final String TEGMINE_SCHEME = "tegmine+";

    public FileSystemItem fromURL(String url) {
        if (TextUtils.isEmpty(url)) {
            return null;
        }
        try {
            Uri uri = Uri.parse(url);
            String path = uri.getAuthority()+uri.getPath();
            if (uri.getScheme().equals("conf")) { // Relative to config file
                FileSystemItem item = configFile();
                if (null == item) { // No configuration
                    logger.w("No configuration", url);
                    return null;
                }
                return fromURL(item.relativeURL(path));
            }
            if (!uri.getScheme().startsWith(TEGMINE_SCHEME)) { // Not supported
                logger.w("Not supported type:", url, uri.getScheme());
                return null;
            }
            String providerName = uri.getScheme().substring(TEGMINE_SCHEME.length());
            logger.d("fromURL",
                    providerName, uri.getPath(),
                    uri.getAuthority(),
                    path,
                    url);
            return fileSystemProvider(providerName).fromURL(path);
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
                Map<String, Object> values = objectObject(to, lineStr); // Extend existing?
                if (null == values) { // Create new
                    values = new LinkedHashMap<>();
                }
                i = readObject(lines, i+1, values, indent+1);
                if (arrayMode) {
                    List<String> valuesList = objectList(to, String.class, lineStr);
                    if (null == valuesList) {
                        valuesList = new ArrayList<>();
                    }
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
        sizes.clear();
        defaultProvider = null;
        if (!fileSystemProviders.containsKey("sdcard")) { // Failsafe - should always be there
            FileSystemProvider provider = new LocalFileSystemProvider(Environment.getExternalStorageDirectory(), "sdcard");
            provider.label("SD Card");
            fileSystemProviders.put("sdcard", provider);
        }
        if (!fileSystemProviders.containsKey("assets")) { // Failsafe - should always be there
            FileSystemProvider provider = new AssetFileSystemProvider("assets", context);
            fileSystemProviders.put("assets", provider);
        }
        defaultProvider = fileSystemProviders.get("sdcard");
        colorSchemes.put("default", new LightTheme(this));
        selectedTheme = "default";
    }

    private Map<String, Object> file2Object(FileSystemItem item) throws FileSystemException {
        Map<String, Object> object = new LinkedHashMap<>();
        return file2Object(item, object);
    }
    private Map<String, Object> file2Object(FileSystemItem item, Map<String, Object> object) throws FileSystemException {
        List<LineMeta> lines = new ArrayList<>();
        loadFilePart(lines, item, null, 0, -1);
        readObject(lines, 0, object, 0);
        return object;
    }

    private void reloadSizes() {
        Map<String, Object> items = objectObject(config, "size");
        if (null == items) {
            return;
        }
        for (LightTheme.Size size: LightTheme.Size.values()) {
            if (items.containsKey(size.code())) { // Found
                sizes.put(size.code(), objectInteger(items, size.code(), size.def()));
            }
        }
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
                List<String> patterns = objectList(keyConfig, String.class, "pattern");
                SyntaxDef sy = new SyntaxDef(patterns, key);
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
            if (sy.matches(item.name)) {
                return sy;
            }
        }
        logger.w("No syntax found:", item.name);
        return null;
    }

    private void loadIncludes(Map<String, Object> config) {
        List<String> list = objectList(config, String.class, "include");
        if (null == list) { // Not found
            return;
        }
        for (String url : list) { // Parse and extend config
            FileSystemItem fileItem = fromURL(url);
            if (null == fileItem) {
                logger.w("Failed to load color scheme:", url);
                continue;
            }
            try {
                file2Object(fileItem, config);
            } catch (FileSystemException e) {
                logger.e(e, "Failed to read include:", url, fileItem);
            }
        }
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
                    LightTheme newTheme = new LightTheme(this);
                    newTheme.dark(objectBoolean(themeConfig, "dark", false));
                    for (Map.Entry<String, Object> oneThemeLine : themeConfig.entrySet()) {
                        String key = oneThemeLine.getKey();
                        LightTheme.Colors color = LightTheme.Colors.findColor(key);
                        if (null != color) {
                            newTheme.loadColor(color, oneThemeLine.getValue().toString());
                        } else {
                            // Size variable
                            newTheme.loadSize(oneThemeLine.getKey(), objectInteger(themeConfig, key, -1));
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

    private FileSystemItem configFile() {
        String path = settings().settingsString(R.string.p_config_file, context.getString(R.string.p_config_file_default));
        if (TextUtils.isEmpty(path)) { // Not set
            return null;
        }
        return fromURL(path);
    }

    public void reloadConfig() throws FileSystemException {
        FileSystemException ex = null;
        setupFailsave();
        try {
            FileSystemItem configItem = configFile();
            if (null == configItem) { // Invalid
                throw new FileSystemException("Failed to read config file");
            }
            List<LineMeta> lines = new ArrayList<>();
            loadFilePart(lines, configItem, null, 0, -1);
            config = new HashMap<>();
            readObject(lines, 0, config, 0);
            logger.d("Config:", lines, config);
            newLineBefore = objectBoolean(config, "newLineBefore", false);
            newLineAfter = objectBoolean(config, "newLineAfter", true);
            scrollToBottom = objectBoolean(config, "scrollToBottom", true);
            reloadStorage(config);
            loadIncludes(config);

            watchSeconds = objectInteger(config, "watchSeconds", watchSeconds);
            clientName = objectString(config, "client", clientName);
            showNumbers = objectBoolean(config, "showNumbers", showNumbers);
            wrapLines = objectBoolean(config, "wrapLines", wrapLines);
            reloadTemplates(config);
            autoThemeChanger.setup(config);
            reloadColorSchemes();
            reloadSyntaxes();
            reloadSizes();
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

    private Wrappers.Tuple2<Pattern, Integer> loadPattern(Map<String, Object> config, String name) {
        String pattern = objectString(config, name, null);
        if (TextUtils.isEmpty(pattern)) {
            return null;
        }
        try {
            Pattern p = Pattern.compile(pattern);
            return new Wrappers.Tuple2<Pattern, Integer>(p, objectInteger(config, name+"_group", 0));
        } catch (Exception e) {
            logger.e(e, "Failed to parse pattern", pattern);
        }
        return null;
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
                    provider.filePattern(loadPattern(conf, "file_pattern"));
                    provider.folderPattern(loadPattern(conf, "folder_pattern"));
                    provider.tab(objectBoolean(conf, "useTab", true));
                    provider.tabSize(objectInteger(conf, "tabSize", SPACES_IN_TAB));
                    provider.scrollToBottom(objectBoolean(conf, "scrollToBottom", scrollToBottom));
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
        if (null == value) { // Not found
            return null;
        }
        if (cls.isAssignableFrom(value.getClass())) { // Single value
            List<T> list = new ArrayList<>();
            list.add((T) value);
            return list;
        }
        if (!(value instanceof List)) {
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
    public TemplateApplyResult applyTemplate(FileSystemProvider provider, String text, TemplateDef tmpl) {
        int cursor = -1;
        StringBuffer buffer = new StringBuffer();
        if (null != tmpl) { // No template
            Matcher m = tmplPattern.matcher(tmpl.template());
            while (m.find()) {
                String value = m.group(1);
                StringBuilder repl = new StringBuilder("???");
                if ("t".equals(value)) { // Tab
                    repl.setLength(0);
                    for (int i = 0; i < spacesInTab(provider); i++) { // Add spaces
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
                    if (!TextUtils.isEmpty(text)) {
                        repl.append(text); // Add shared text
                    }
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
        }
        if (-1 == cursor) { // Not set yet
            if (!TextUtils.isEmpty(text)) {
                buffer.append(text); // Add shared text
            }
            cursor = buffer.length();
        }
        logger.d("Template:", tmpl, String.format("[%s]", buffer.toString()), buffer.length(), cursor);
        return new TemplateApplyResult(buffer.toString(), cursor);
    }

    public void addIndent(FileSystemProvider provider, StringBuilder sb, int indent) {
        for (int i = 0; i < indent * spacesInTab(provider); i++) { // Add spaces
            sb.append(' ');
        }
    }

    public String part(FileSystemProvider provider, List<LineMeta> lines, int from) {
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
                addIndent(provider, sb, indent - topIndent);
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

    public SyntaxDef.SyntaxedStringBuilder applyTheme(FileSystemProvider provider, SyntaxDef syntax, String line, SpannableStringBuilder builder, SyntaxDef.Feature... features) {
        if (null == syntax) {
            builder.append(line);
            return null;
        }
        for (int i = 0; i < line.length(); i++) { // Search for a first non space/tab
            if (line.charAt(i) == '\t') { // Tab
                for (int j = 0; j < spacesInTab(provider); j++) { // Add spaces
                    builder.append(' ');
                }
            } else if (line.charAt(i) == ' ') { // Space
                builder.append(' ');
            } else {
                // Everything else
                break;
            }
        }
        SyntaxDef.SyntaxedStringBuilder syntaxedStringBuilder = new SyntaxDef.SyntaxedStringBuilder(line.trim());
        syntax.apply(syntaxedStringBuilder);
        syntaxedStringBuilder.span(theme(), builder, features);
        return syntaxedStringBuilder;
    }

    public int watchSeconds() {
        return watchSeconds;
    }

    public TemplateDef templateFromKeyEvent(KeyEvent keyEvent) {
        String label = new String(""+keyEvent.getDisplayLabel()).toLowerCase();
//        logger.d("template key:", label);
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

    public float dp2px(float dp) {
        Resources r = context.getResources();
        return r.getDisplayMetrics().density * dp;
    }

    public float sp2px(float dp) {
        Resources r = context.getResources();
        return r.getDisplayMetrics().scaledDensity * dp;
    }

    public EditorsController editors() {
        return editors;
    }

    public void openLink(Activity activity, String url, FileSystemItem item) {
        Uri uri = Uri.parse(url);
        if (url.startsWith("att://")) { // Attachment - read config
            String attachmentFolder = objectString(config(), "attachment_folder", "");
            String urlStr = item.relativeURL(String.format("%s%s", attachmentFolder, url.substring(6)));
            uri = fromURL(urlStr).toUri();
        }
        Intent browserIntent = new Intent(Intent.ACTION_VIEW);
        browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (uri.getScheme().equals("file")) { // Detect extension
            String extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
            String mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            browserIntent.setDataAndType(uri, mimetype);
        } else {
            browserIntent.setData(uri);
        }
        try {
            activity.startActivity(browserIntent);
        } catch (ActivityNotFoundException e) {
            logger.e(e, "Failed to open Activity");
            Toast toast = Toast.makeText(context, "Failed to open attachment", Toast.LENGTH_LONG);
            toast.show();
        }
    }

    public int size(LightTheme.Size size) {
        Integer value = sizes.get(size.code());
        if (null != value) { // Found
            return value;
        }
        return size.def();
    }

    public boolean itemHasFeature(FileSystemItem item, FileSystemProvider.Features feature) {
        return fileSystemProvider(item).hasFeature(item, feature);
    }

    public List<WidgetExtension> extensions(String def) {
        List<WidgetExtension> result = new ArrayList<>();
        if (TextUtils.isEmpty(def)) return result;
        for (String s : def.split(",")) {
            String id = s.trim();
            if (widgetExtensions.containsKey(id)) result.add(widgetExtensions.get(id));
        }
        return result;
    }

}
