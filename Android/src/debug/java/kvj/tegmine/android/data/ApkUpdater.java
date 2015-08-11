package kvj.tegmine.android.data;

import android.content.Context;

import com.lazydroid.autoupdateapk.AutoUpdateApk;

/**
 * Created by vorobyev on 8/11/15.
 */
public class ApkUpdater {

    private final AutoUpdateApk apk;

    public ApkUpdater(Context context) {
        apk = new AutoUpdateApk(context);
        apk.setUpdateInterval(AutoUpdateApk.DAYS);

    }

    public void checkForUpdates() {
        apk.checkUpdatesManually();
    }
}
