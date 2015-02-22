package kvj.tegmine.android.ui.form;

import org.kvj.bravo7.form.impl.widget.TransientAdapter;

import kvj.tegmine.android.data.TegmineController;
import kvj.tegmine.android.data.def.FileSystemItem;

/**
 * Created by kvorobyev on 2/19/15.
 */
public class FileSystemItemWidgetAdapter extends TransientAdapter<FileSystemItem>{

    public FileSystemItemWidgetAdapter(TegmineController ctrl) {
        super(new FileSystemItemAdapter(ctrl), null);
    }
}
