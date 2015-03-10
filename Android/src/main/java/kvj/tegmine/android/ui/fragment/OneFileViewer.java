package kvj.tegmine.android.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import org.kvj.bravo7.form.FormController;
import org.kvj.bravo7.log.Logger;
import org.kvj.bravo7.util.Tasks;

import java.util.ArrayList;
import java.util.List;

import kvj.tegmine.android.R;
import kvj.tegmine.android.Tegmine;
import kvj.tegmine.android.data.TegmineController;
import kvj.tegmine.android.data.def.FileSystemException;
import kvj.tegmine.android.data.def.FileSystemItem;
import kvj.tegmine.android.ui.adapter.OneFileAdapter;
import kvj.tegmine.android.ui.form.FileSystemItemWidgetAdapter;

/**
 * Created by kvorobyev on 2/16/15.
 */
public class OneFileViewer extends ListFragment {

    public static interface FileViewerListener {
        public void openEditor(Bundle data);
    }

    private Logger logger = Logger.forInstance(this);
    private TegmineController controller = null;
    private TextView titleText = null;
    private OneFileAdapter adapter = null;
    private List<Long> offsets = null;
    private FormController form = new FormController(null);
    private FileSystemItem item = null;
    private FileViewerListener listener = null;

    public OneFileViewer() {
        super();
    }

    public OneFileViewer setListener(FileViewerListener listener) {
        this.listener = listener;
        return this;
    }

    public OneFileViewer create(TegmineController controller, Bundle bundle) {
        this.controller = controller;
        form.add(new FileSystemItemWidgetAdapter(controller, null), "select");
        form.add(new FileSystemItemWidgetAdapter(controller, null), "root");
        form.load(bundle);
        item = form.getValue("select", FileSystemItem.class);
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
        logger.d("Will create view", controller);
        View view = inflater.inflate(R.layout.fragment_one_file, container, false);
        titleText = (TextView) view.findViewById(R.id.one_file_title_text);
        titleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, controller.theme().headerTextSp());
        titleText.setTextColor(controller.theme().textColor());
        titleText.setText(item.details());
        view.findViewById(R.id.one_file_do_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startEditor(Tegmine.EDIT_TYPE_ADD);
            }
        });
        view.findViewById(R.id.one_file_do_edit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startEditor(Tegmine.EDIT_TYPE_EDIT);
            }
        });
        ListView listView = (ListView) view.findViewById(android.R.id.list);
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                onLongItemClick(position);
                return true;
            }
        });
        return view;
    }

    private void onLongItemClick(int position) {
        if (null != adapter) { // OK
            String contents = adapter.partString(position);
            if (TextUtils.isEmpty(contents)) { // No data
                return;
            }
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, contents);
            sendIntent.setType("text/plain");
            startActivity(sendIntent);
            logger.d("Sharing text:", contents);
        }
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
        form.save(outState, "root", "select");
    }

    private void startEditor(String editType) {
        Bundle bundle = new Bundle();
        bundle.putString(Tegmine.BUNDLE_EDIT_TYPE, editType);
        form.save(bundle, "root", "select");
        if (null != listener) {
            listener.openEditor(bundle);
        }
    }

    public void refresh() {
        if (null != adapter) { // Reload contents
            adapter.setBounds(0, -1);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        listener = null;
    }
}
