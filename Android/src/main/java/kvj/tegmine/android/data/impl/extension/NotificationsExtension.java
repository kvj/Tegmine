package kvj.tegmine.android.data.impl.extension;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import org.kvj.bravo7.log.Logger;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import kvj.tegmine.android.Tegmine;
import kvj.tegmine.android.data.TegmineController;
import kvj.tegmine.android.data.def.FileSystemItem;
import kvj.tegmine.android.data.def.WidgetExtension;
import kvj.tegmine.android.data.model.LineMeta;

/**
 * Created by vorobyev on 9/30/16.
 */

public class NotificationsExtension implements WidgetExtension {

    Logger logger = Logger.forInstance(this);
    private final TegmineController controller;
    Pattern pattern = Pattern.compile("(\\s|^)<(((\\d{2}/)?\\d{1,2}/\\d{1,2})?\\s?(\\d{1,2}:\\d{2})?)>");
    SimpleDateFormat sdf = new SimpleDateFormat("yy/M/d H:mm");
    SimpleDateFormat sdfY = new SimpleDateFormat("yy/");
    SimpleDateFormat sdfD = new SimpleDateFormat("yy/M/d");

    public NotificationsExtension(TegmineController controller) {
        this.controller = controller;
    }

    @Override
    public String title() {
        return null;
    }

    private class EntryInfo {
        final Date date;
        final String title;

        private EntryInfo(Date date, String title) {
            this.date = date;
            this.title = title;
        }
    }

    @Override
    public void process(FileSystemItem item, List<LineMeta> lines, int id) {
        logger.d("Looking for dates:", item.toURL(), lines.size());
        String defaultTime = TegmineController.objectString(controller.config(), "notifications_time", "8:00");
        List<EntryInfo> entries = new ArrayList<>();
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MINUTE, 1);
        Date now = c.getTime();
        for (LineMeta line : lines) {
            Matcher m = pattern.matcher(line.data());
            if (m.find()) {
                String date = m.group(3);
                String time = m.group(5);
//                logger.d("Found:", date, time);
                if (TextUtils.isEmpty(date) && TextUtils.isEmpty(time)) continue;
                if (TextUtils.isEmpty(date))
                    date = sdfD.format(now);
                else {
                    if (TextUtils.isEmpty(m.group(4))) // No year
                        date = sdfY.format(now) + date;
                }
                if (TextUtils.isEmpty(time)) {
                    time = defaultTime;
                }
                StringBuffer sb = new StringBuffer();
                m.appendReplacement(sb, "");
                m.appendTail(sb);
                Date parsed;
                try {
                    parsed = sdf.parse(String.format("%s %s", date, time));
                } catch (Exception e) {
                    logger.w(e, "Invalid date/time", date, time);
                    continue;
                }
//                logger.d("Found date:", parsed, sb, line.data());
                if (now.after(parsed)) continue; // Skip past dates
                entries.add(new EntryInfo(parsed, sb.toString()));
            }
        }
        if (entries.isEmpty()) return; // No dates
        Collections.sort(entries, new Comparator<EntryInfo>() {
            @Override
            public int compare(EntryInfo t0, EntryInfo t1) {
                return t0.date.compareTo(t1.date);
            }
        });
        EntryInfo entry = entries.get(0);
        logger.d("Schedule:", entry.date, entry.title);
        Intent intent = new Intent(controller.context(), NotificationsReceiver.class);
        intent.setData(Uri.fromParts("tegmine", "widget00", Integer.toString(id)));
        intent.putExtra(Tegmine.BUNDLE_URL, item.toURL());
        intent.putExtra(Tegmine.BUNDLE_WIDGET_ID, id);
        intent.putExtra(Tegmine.BUNDLE_TITLE, entry.title);
        PendingIntent pintent = PendingIntent.getBroadcast(controller.context(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        controller.scheduleAlarm(entry.date, pintent);
    }
}
