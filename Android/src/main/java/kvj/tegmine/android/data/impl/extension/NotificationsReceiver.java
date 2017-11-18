package kvj.tegmine.android.data.impl.extension;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import org.kvj.bravo7.log.Logger;
import org.kvj.bravo7.ng.widget.AppWidgetController;

import kvj.tegmine.android.R;
import kvj.tegmine.android.Tegmine;
import kvj.tegmine.android.ui.Main;
import kvj.tegmine.android.ui.appwidget.Widget00;

/**
 * Created by kvorobyev on 2/25/16.
 */
public class NotificationsReceiver extends BroadcastReceiver {

    private Logger logger = Logger.forInstance(this);

    @Override
    public void onReceive(Context context, Intent intent) {

        AppWidgetController controller = AppWidgetController.instance(context);
        try {
            int id = intent.getIntExtra(Tegmine.BUNDLE_WIDGET_ID, -1);
            Widget00 widget = new Widget00();
            if (!controller.valid(id, widget)) { // Invalid - ignore
                return;
            }
//            Configurator conf = controller.configurator(id);
            String title = intent.getStringExtra(Tegmine.BUNDLE_TITLE);
            logger.d("Ready to show:", id, title);
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder n = new NotificationCompat.Builder(context, "widget00");
            n.setContentTitle(controller.title(id, widget));
            n.setContentText(title);
            n.setSmallIcon(R.drawable.ic_notification);
            n.setOnlyAlertOnce(false);
            n.setDefaults(Notification.DEFAULT_ALL);
            n.setShowWhen(true);
            Intent launchIntent = controller.remoteIntent(id, Main.class);
            launchIntent.putExtra(Tegmine.BUNDLE_VIEW_TYPE, Tegmine.VIEW_TYPE_FILE);
            launchIntent.putExtra(Tegmine.BUNDLE_SELECT, intent.getStringExtra(Tegmine.BUNDLE_URL));
            n.setContentIntent(PendingIntent.getActivity(context, 0, launchIntent, PendingIntent.FLAG_CANCEL_CURRENT));
            nm.notify(null, id, n.build());
            controller.update(id, widget);
        } catch (Exception e) {
            logger.e(e, "Failed to show Notification");
        }
    }
}
