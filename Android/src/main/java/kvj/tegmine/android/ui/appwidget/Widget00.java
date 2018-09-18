package kvj.tegmine.android.ui.appwidget;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.TypedValue;
import android.widget.RemoteViews;

import org.kvj.bravo7.log.Logger;
import org.kvj.bravo7.ng.conf.Configurator;
import org.kvj.bravo7.ng.widget.AppWidget;
import org.kvj.bravo7.ng.widget.AppWidgetController;
import org.kvj.bravo7.ng.widget.AppWidgetRemote;
import org.kvj.bravo7.util.Compat;

import java.util.ArrayList;
import java.util.List;

import kvj.tegmine.android.R;
import kvj.tegmine.android.Tegmine;
import kvj.tegmine.android.data.TegmineController;
import kvj.tegmine.android.data.def.FileSystemException;
import kvj.tegmine.android.data.def.FileSystemItem;
import kvj.tegmine.android.data.def.FileSystemProvider;
import kvj.tegmine.android.data.def.WidgetExtension;
import kvj.tegmine.android.data.model.LineMeta;
import kvj.tegmine.android.data.model.SyntaxDef;
import kvj.tegmine.android.data.model.util.Wrappers;
import kvj.tegmine.android.ui.Main;

/**
 * Created by vorobyev on 7/27/15.
 */
public class Widget00 extends AppWidget implements AppWidget.AppWidgetUpdate {

    Logger logger = Logger.forInstance(this);
    TegmineController tegmine = Tegmine.controller();

    private static void applyTheme(final RemoteViews rv, final int id, final TegmineController tegmine) {
        rv.setTextColor(id, tegmine.theme().textColor());
        Compat.levelAware(Build.VERSION_CODES.JELLY_BEAN, new Runnable() {
            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void run() {
                rv.setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_SP, tegmine.theme().fileTextSp());
            }
        });
    }

    private Intent createLaunchIntent(AppWidgetController controller, int id, String url, String viewType, String editType) {
        Intent launchIntent = controller.remoteIntent(id, Main.class);
        launchIntent.putExtra(Tegmine.BUNDLE_VIEW_TYPE, viewType);
        if (null != editType) {
            launchIntent.putExtra(Tegmine.BUNDLE_EDIT_TYPE, editType);
        }
        launchIntent.putExtra(Tegmine.BUNDLE_SELECT, url);
        launchIntent.setType(String.format("%d.%s.%s", id, viewType, editType));
        return launchIntent;
    }

    @Override
    public RemoteViews update(AppWidgetController controller, int id) {
//        logger.d("Update widget", id);
        RemoteViews rv = controller.create(R.layout.widget_00);
        Configurator conf = controller.configurator(id);
        String url = conf.settingsString(R.string.conf_widget_url, "");
        String title = conf.settingsString(R.string.conf_widget_title, "");
        if (TextUtils.isEmpty(title)) {
            title = conf.settingsString(R.string.conf_widget_label, "<Untitled>");
        }
        rv.setTextViewText(R.id.widget_00_title, title);
        applyTheme(rv, R.id.widget_00_title, tegmine);
        applyTheme(rv, R.id.widget_00_empty, tegmine);
        rv.setImageViewResource(R.id.widget_00_refresh_icon, tegmine.theme().refreshIcon());
        rv.setOnClickPendingIntent(R.id.widget_00_refresh_icon,
                controller.refreshPendingIntent(id, Widget00.Service.class));
        rv.setImageViewResource(R.id.widget_00_config_icon, tegmine.theme().configIcon());
        rv.setOnClickPendingIntent(R.id.widget_00_config_icon, controller.configPendingIntent(id));
        rv.setImageViewResource(R.id.widget_00_add_icon, tegmine.theme().addIcon());
        rv.setImageViewResource(R.id.widget_00_edit_icon, tegmine.theme().editIcon());
        rv.setRemoteAdapter(R.id.widget_00_list,
                controller.remoteIntent(id, Widget00.Service.class));
        Intent launchIntent = controller.remoteIntent(id, Main.class);
        launchIntent.putExtra(Tegmine.BUNDLE_VIEW_TYPE, Tegmine.VIEW_TYPE_FILE);
        launchIntent.putExtra(Tegmine.BUNDLE_SELECT, url);
        rv.setPendingIntentTemplate(R.id.widget_00_list,
                controller.activityPending(createLaunchIntent(controller, id, url, Tegmine.VIEW_TYPE_FILE, null)));
        rv.setOnClickPendingIntent(R.id.widget_00_add_icon,
                controller.activityPending(createLaunchIntent(controller, id, url, Tegmine.VIEW_TYPE_EDITOR, Tegmine.EDIT_TYPE_ADD)));
        rv.setOnClickPendingIntent(R.id.widget_00_edit_icon,
                controller.activityPending(createLaunchIntent(controller, id, url, Tegmine.VIEW_TYPE_EDITOR, Tegmine.EDIT_TYPE_EDIT)));
        rv.setEmptyView(R.id.widget_00_list, R.id.widget_00_empty);
        controller.notify(id, R.id.widget_00_list);
        return rv;
    }

    @Override
    public AppWidgetUpdate updater() {
        return this;
    }

    @Override
    public String title(AppWidgetController controller, int id) {
        Configurator conf = controller.configurator(id);
        String title = conf.settingsString(R.string.conf_widget_label, "");
        if (!TextUtils.isEmpty(title)) {
            return title;
        }
        return super.title(controller, id);
    }

    private static FileSystemItem loadItem(Configurator conf) {
        TegmineController tegmine = Tegmine.controller();
        String url = conf.settingsString(R.string.conf_widget_url, "");
        return tegmine.fromURL(url);
    }

    private static List<LineMeta> loadContents(Configurator conf, FileSystemItem item) {
        TegmineController tegmine = Tegmine.controller();
        Logger logger = Logger.forClass(Widget00.class);
        SyntaxDef syntax = tegmine.findSyntax(item);
        List<LineMeta> buffer = new ArrayList<>();
        try {
            tegmine.loadFilePart(buffer, item, syntax, 0, -1);
        } catch (FileSystemException e) {
            logger.e(e, "Failed to read:", item);
            return null;
        }
        String condition = conf.settingsString(R.string.conf_widget_data, "");
        Wrappers.Pair<Integer> frame = tegmine.findIn(buffer, condition);
        return buffer.subList(frame.v1(), frame.v1()+frame.v2());
    }

    static class Adapter extends AppWidgetRemote.AppWidgetRemoteAdapter {

        TegmineController tegmine = Tegmine.controller();
        List<CharSequence> lines = new ArrayList<>();
        private boolean wordWrap = false;
        private Intent clickIntent = new Intent(); // Line not important

        @Override
        public void onDataSetChanged() {
            super.onDataSetChanged();
            lines.clear();
            Configurator conf = controller.configurator(id);
            wordWrap = conf.settingsBoolean(R.string.conf_widget_line_wrap, false);
            FileSystemItem item = loadItem(conf);
            if (item == null) { // Invalid
                return;
            }
            List<LineMeta> buffer = loadContents(conf, item);
            if (buffer == null) { // Not loaded
                return;
            }
            FileSystemProvider provider = tegmine.fileSystemProvider(item);
            SyntaxDef syntax = tegmine.findSyntax(item);
            int indent = 0;
            if (!buffer.isEmpty()) {
                indent = buffer.get(0).indent();
            }
            for (LineMeta line : buffer) {
                if (!line.visible()) { // Hidden line
                    continue;
                }
                StringBuilder sb = new StringBuilder();
                tegmine.addIndent(provider, sb, line.indent() - indent);
                sb.append(line.data());
                SpannableStringBuilder b = new SpannableStringBuilder();
                tegmine.applyTheme(provider, syntax, sb.toString(), b, SyntaxDef.Feature.Shrink);
                lines.add(b);
            }
            for (WidgetExtension ext : tegmine.extensions(conf.settingsString(R.string.conf_widget_ext, ""))) {
                ext.process(item, buffer, id);
            }
        }

        @Override
        public int getCount() {
            return lines.size();
        }

        @Override
        public RemoteViews getViewAt(int i) {
            RemoteViews rv = controller.create(R.layout.widget_line);
            rv.setTextViewText(R.id.widget_00_line_text, lines.get(i));
            rv.setBoolean(R.id.widget_00_line_text, "setSingleLine", !wordWrap);
            Widget00.applyTheme(rv, R.id.widget_00_line_text, tegmine);
            rv.setOnClickFillInIntent(R.id.widget_00_line_text, clickIntent);
            return rv;
        }
    }

    public static class Service extends AppWidgetRemote.AppWidgetRemoteService {

        @Override
        protected AppWidgetRemote.AppWidgetRemoteAdapter adapter() {
            return new Adapter();
        }

        @Override
        protected AppWidgetUpdate widgetUpdate() {
            return new Widget00();
        }

    }
}
