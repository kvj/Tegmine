package kvj.tegmine.android.ui.fragment;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import org.kvj.bravo7.form.FormController;
import org.kvj.bravo7.log.Logger;

import kvj.tegmine.android.R;
import kvj.tegmine.android.Tegmine;
import kvj.tegmine.android.data.TegmineController;
import kvj.tegmine.android.data.def.FileSystemException;
import kvj.tegmine.android.data.def.FileSystemItem;
import kvj.tegmine.android.data.def.FileSystemItemType;
import kvj.tegmine.android.ui.adapter.FileBrowserAdapter;
import kvj.tegmine.android.ui.form.FileSystemItemWidgetAdapter;

/**
 * Created by kvorobyev on 2/14/15.
 */
public class FileSystemBrowser extends ListFragment {

    public static interface BrowserListener {

        public void openNewWindow(Bundle data);

        public void openFile(Bundle data, FileSystemItem item);
    }

    private TegmineController controller = null;
    private TextView titleText = null;
    private FileBrowserAdapter adapter = null;
    private Logger logger = Logger.forInstance(this);
    private FormController form = new FormController(null);

    private BrowserListener listener = null;

    public FileSystemBrowser() {
        super();
    }

    public FileSystemBrowser setListener(BrowserListener listener) {
        this.listener = listener;
        return this;
    }

    @Override
    public void onDestroyView() {
        listener = null;
        super.onDestroyView();
    }

    public FileSystemBrowser create(TegmineController controller, Bundle bundle) {
        this.controller = controller;
        String providerName = bundle.getString(Tegmine.BUNDLE_PROVIDER, null);
        form.add(new FileSystemItemWidgetAdapter(controller, providerName), "select");
        form.add(new FileSystemItemWidgetAdapter(controller, providerName), "root");
        form.load(bundle);
//        logger.d("New browser", , rootItem);
        this.adapter = new FileBrowserAdapter(controller, providerName, form.getValue("root", FileSystemItem.class));
        setListAdapter(adapter);
        adapter.expandTo(form.getValue("select", FileSystemItem.class));
        return this;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (null == controller) { // Invalid fragment
            return null;
        }
        logger.d("Will create view", controller);
        View view = inflater.inflate(R.layout.fragment_file_browser, container, false);
        form.setView(view);
        titleText = (TextView) view.findViewById(R.id.file_browser_title_text);
        titleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, controller.theme().headerTextSp());
        titleText.setTextColor(controller.theme().textColor());
        if (null != adapter.root()) { // Have root defined
            titleText.setText(adapter.root().details());
        } else {
            titleText.setText("Root");
        }
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
        FileSystemItem item = adapter.getFileSystemItem(position);
        if (item.type == FileSystemItemType.Folder) { // Collapse/expand
            try {
                Bundle bundle = new Bundle();
                controller.fileSystemProvider().toBundle(bundle, "root_", item);
                if (null != listener) {
                    listener.openNewWindow(bundle);
                }
            } catch (FileSystemException e) {
                logger.e(e, "Failed to put item to bundle", item);
            }
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        FileSystemItem item = adapter.getFileSystemItem(position);
        form.setValue("select", item);
        if (item.type == FileSystemItemType.Folder) { // Collapse/expand
            adapter.collapseExpand(position);
        }
        if (item.type == FileSystemItemType.File) { // Open viewer
            try {
                Bundle bundle = new Bundle();
                if (null != adapter.root()) { // Have custom root
                    controller.fileSystemProvider().toBundle(bundle, "root_", adapter.root());
                }
                controller.fileSystemProvider().toBundle(bundle, "select_", item);
                if (null != listener) {
                    listener.openFile(bundle, item);
                }
            } catch (FileSystemException e) {
                logger.e(e, "Failed to put item to bundle", item);
            }
        }

    }

    public void saveState(Bundle outState) {
        form.save(outState, "root", "select");
        super.onSaveInstanceState(outState);
    }
}
