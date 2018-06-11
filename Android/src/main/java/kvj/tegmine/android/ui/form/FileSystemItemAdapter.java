package kvj.tegmine.android.ui.form;

import android.os.Bundle;

import org.kvj.bravo7.form.BundleAdapter;
import org.kvj.bravo7.log.Logger;
import org.kvj.bravo7.util.Compat;

import kvj.tegmine.android.data.TegmineController;
import kvj.tegmine.android.data.def.FileSystemItem;

/**
 * Created by kvorobyev on 2/19/15.
 */
public class FileSystemItemAdapter extends BundleAdapter<FileSystemItem> {

    Logger logger = Logger.forInstance(this);
    private final TegmineController controller;

    public FileSystemItemAdapter(TegmineController controller) {
        this.controller = controller;
    }

    @Override
    public FileSystemItem get(Bundle bundle, String name, Compat.Producer<FileSystemItem> def) {
        String url = bundle.getString(name);
        FileSystemItem item = controller.fromURL(url);
        if (null != item) { // Found item
            return item;
        }
        return def.produce();
    }

    @Override
    public void set(Bundle bundle, String name, FileSystemItem value) {
        if (null == value) {
            return;
        }
        bundle.putString(name, value.toURL());
    }
}
