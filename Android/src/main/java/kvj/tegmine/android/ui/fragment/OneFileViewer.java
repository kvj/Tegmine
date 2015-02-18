package kvj.tegmine.android.ui.fragment;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.kvj.bravo7.log.Logger;
import org.kvj.bravo7.util.Tasks;

import java.util.ArrayList;
import java.util.List;

import kvj.tegmine.android.R;
import kvj.tegmine.android.data.TegmineController;
import kvj.tegmine.android.data.def.FileSystemException;
import kvj.tegmine.android.data.impl.provider.local.LocalFileSystemItem;
import kvj.tegmine.android.ui.adapter.OneFileAdapter;

/**
 * Created by kvorobyev on 2/16/15.
 */
public class OneFileViewer extends ListFragment {

    private Logger logger = Logger.forInstance(this);
    private TegmineController controller = null;
    private TextView titleText = null;
    private LocalFileSystemItem item = null;
    private LocalFileSystemItem rootItem = null;
    private OneFileAdapter adapter = null;
    private List<Long> offsets = null;

    public OneFileViewer create(TegmineController controller, Bundle bundle) {
        this.controller = controller;
        try {
            item = controller.fileSystemProvider().fromBundle("select_", bundle);
            rootItem = controller.fileSystemProvider().fromBundle("root_", bundle);
        } catch (FileSystemException e) {
            logger.e(e, "Failed to load", bundle);
        }
        adapter = new OneFileAdapter(controller, item);
        setListAdapter(adapter);
        adapter.setBounds(0, -1);
        return this;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (null == controller) { // Invalid fragment
            return null;
        }
        // logger.d("Will create view", controller);
        View view = inflater.inflate(R.layout.fragment_one_file, container, false);
        titleText = (TextView) view.findViewById(R.id.one_file_title_text);
        titleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, controller.theme().headerTextSp());
        titleText.setTextColor(controller.theme().textColor());
        titleText.setText(item.details());
        return view;
    }

    private void loadFileLayout() {
        Tasks.SimpleTask<List<Long>> task = new Tasks.SimpleTask<List<Long>>() {
            @Override
            protected List<Long> doInBackground() {
                try {
                    return controller.makeFileLayout(item);
                } catch (FileSystemException e) {
                    logger.e(e, "Failed to read file");
                }
                return new ArrayList<>();
            }

            @Override
            protected void onPostExecute(List<Long> longs) {
                offsets = longs;
                logger.d("Will have lines:", offsets.size());
            }
        };
        task.exec();

    }

    public void saveState(Bundle outState) {
        try {
            if (null != item) {
                    controller.fileSystemProvider().toBundle(outState, "select_", item);
            }
            if (null != rootItem) {
                controller.fileSystemProvider().toBundle(outState, "root_", rootItem);
            }
        } catch (FileSystemException e) {
            logger.e(e, "Failed to save");
        }
    }
}
