package kvj.tegmine.android.data;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.kvj.bravo7.log.Logger;
import org.kvj.bravo7.ng.widget.AppWidgetController;

import kvj.tegmine.android.ui.appwidget.Widget00;

/**
 * Created by kvorobyev on 2/25/16.
 */
public class UserReceiver extends BroadcastReceiver {

    private Logger logger = Logger.forInstance(this);

    @Override
    public void onReceive(Context context, Intent intent) {
        AppWidgetController controller = AppWidgetController.instance(context);
        logger.d("Update all widgets:");
        controller.updateAll(new Widget00());
    }
}
