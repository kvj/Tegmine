package kvj.tegmine.android.ui.form;

import android.os.Bundle;

import org.kvj.bravo7.form.impl.widget.TransientAdapter;
import org.kvj.bravo7.ng.Controller;

import kvj.tegmine.android.Tegmine;
import kvj.tegmine.android.data.TegmineController;
import kvj.tegmine.android.data.def.FileSystemItem;

/**
 * Created by kvorobyev on 2/19/15.
 */
public class FileSystemItemWidgetAdapter extends TransientAdapter<FileSystemItem>{

    private String bundleProviderKey = null;
    private TegmineController controller = Tegmine.controller();

    public FileSystemItemWidgetAdapter(TegmineController ctrl) {
        super(new FileSystemItemAdapter(ctrl), null);
    }

    public FileSystemItemWidgetAdapter(TegmineController ctrl, FileSystemItem def) {
        super(new FileSystemItemAdapter(ctrl), def);
    }

    public FileSystemItemWidgetAdapter bundleProviderKey(String bundleProviderKey) {
        this.bundleProviderKey = bundleProviderKey;
        return this;
    }

    @Override
    public void setWidgetValue(FileSystemItem v, Bundle bundle) {
        super.setWidgetValue(v, bundle);
        if (value == null && bundleProviderKey != key && bundle != null) {
            this.value = controller.fileSystemProvider(bundle.getString(bundleProviderKey)).root();
        }
    }
}
