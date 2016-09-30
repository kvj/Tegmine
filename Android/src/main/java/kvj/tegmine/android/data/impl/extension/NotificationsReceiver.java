package kvj.tegmine.android.data.impl.extension;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.kvj.bravo7.log.Logger;
import org.kvj.bravo7.ng.widget.AppWidgetController;

import kvj.tegmine.android.Tegmine;
import kvj.tegmine.android.ui.appwidget.Widget00;

/**
 * Created by kvorobyev on 2/25/16.
 */
public class NotificationsReceiver extends BroadcastReceiver {

    private Logger logger = Logger.forInstance(this);

    @Override
    public void onReceive(Context context, Intent intent) {

        AppWidgetController controller = AppWidgetController.instance(context);
        logger.d("Update all widgets:");
        try {
            int id = intent.getIntExtra(Tegmine.BUNDLE_WIDGET_ID, -1);
            String title = intent.getStringExtra(Tegmine.BUNDLE_TITLE);
            logger.d("Ready to show:", id, title);
            controller.update(id, new Widget00());
        } catch (Exception e) {
            logger.e(e, "Failed to show Notification");
        }
    }
}
