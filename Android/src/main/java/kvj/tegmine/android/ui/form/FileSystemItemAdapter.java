package kvj.tegmine.android.ui.form;

import android.os.Bundle;

import org.kvj.bravo7.form.BundleAdapter;
import org.kvj.bravo7.log.Logger;

import kvj.tegmine.android.data.TegmineController;
import kvj.tegmine.android.data.def.FileSystemException;
import kvj.tegmine.android.data.def.FileSystemItem;

/**
 * Created by kvorobyev on 2/19/15.
 */
public class FileSystemItemAdapter implements BundleAdapter<FileSystemItem> {

    Logger logger = Logger.forInstance(this);
    private final TegmineController controller;

    public FileSystemItemAdapter(TegmineController controller) {
        this.controller = controller;
    }

    @Override
    public FileSystemItem get(Bundle bundle, String name, FileSystemItem def) {
        try {
            FileSystemItem item = controller.fileSystemProvider().fromBundle(name+"_", bundle);
            if (null != item) { // Found item
                return item;
            }
        } catch (FileSystemException e) {
            logger.e(e, "Failed to read");
        }
        return def;
    }

    @Override
    public void set(Bundle bundle, String name, FileSystemItem value) {
        if (null == value) {
            return;
        }
        try {
            controller.fileSystemProvider().toBundle(bundle, name+"_", value);
        } catch (FileSystemException e) {
            logger.e(e, "");
        }
    }
}
