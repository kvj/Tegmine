package kvj.tegmine.android.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import org.kvj.bravo7.log.Logger;

import kvj.tegmine.android.R;
import kvj.tegmine.android.Tegmine;
import kvj.tegmine.android.data.TegmineController;
import kvj.tegmine.android.data.def.FileSystemException;
import kvj.tegmine.android.data.def.FileSystemItem;
import kvj.tegmine.android.data.def.FileSystemItemType;
import kvj.tegmine.android.ui.Main;
import kvj.tegmine.android.ui.adapter.FileBrowserAdapter;

/**
 * Created by kvorobyev on 2/14/15.
 */
public class FileSystemBrowser extends ListFragment {

    private TegmineController controller = null;
    private TextView titleText = null;
    private FileBrowserAdapter adapter = null;
    private Logger logger = Logger.forInstance(this);
    private FileSystemItem lastSelected = null;

    public FileSystemBrowser create(TegmineController controller, Bundle bundle) {
        this.controller = controller;
        FileSystemItem rootItem = null;
        try {
            lastSelected = controller.fileSystemProvider().fromBundle("select_", bundle);
            rootItem = controller.fileSystemProvider().fromBundle("root_", bundle);
        } catch (FileSystemException e) {
            logger.e(e, "Failed to load", bundle);
        }
        logger.d("New browser", lastSelected, rootItem);
        this.adapter = new FileBrowserAdapter(controller, rootItem);
        setListAdapter(adapter);
        adapter.expandTo(lastSelected);
        return this;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (null == controller) { // Invalid fragment
            return null;
        }
        logger.d("Will create view", controller);
        View view = inflater.inflate(R.layout.fragment_file_browser, container, false);
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
            Intent intent = new Intent(this.getActivity(), Main.class);
            intent.putExtra(Tegmine.BUNDLE_VIEW_TYPE, Tegmine.VIEW_TYPE_BROWSER);
            try {
                Bundle bundle = new Bundle();
                controller.fileSystemProvider().toBundle(bundle, "root_", item);
                intent.putExtras(bundle);
            } catch (FileSystemException e) {
                logger.e(e, "Failed to put item to bundle", item);
            }
            logger.d("Start activity", intent.getExtras());
            startActivity(intent);
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        FileSystemItem item = adapter.getFileSystemItem(position);
        lastSelected = item;
        if (item.type == FileSystemItemType.Folder) { // Collapse/expand
            adapter.collapseExpand(position);
        }
        if (item.type == FileSystemItemType.File) { // Open viewer
            Intent intent = new Intent(this.getActivity(), Main.class);
            intent.putExtra(Tegmine.BUNDLE_VIEW_TYPE, Tegmine.VIEW_TYPE_FILE);
            try {
                Bundle bundle = new Bundle();
                if (null != adapter.root()) { // Have custom root
                    controller.fileSystemProvider().toBundle(bundle, "root_", adapter.root());
                }
                controller.fileSystemProvider().toBundle(bundle, "select_", item);
                intent.putExtras(bundle);
            } catch (FileSystemException e) {
                logger.e(e, "Failed to put item to bundle", item);
            }
            startActivity(intent);
        }

    }

    public void saveState(Bundle outState) {
        if (null != lastSelected) { // Have some item selected
            try {
                controller.fileSystemProvider().toBundle(outState, "select_", lastSelected);
            } catch (FileSystemException e) {
                logger.e(e, "Failed to save to Bundle", lastSelected);
            }
        }
        if (null != adapter.root()) { // Have special root
            try {
                controller.fileSystemProvider().toBundle(outState, "root_", adapter.root());
            } catch (FileSystemException e) {
                logger.e(e, "Failed to save to Bundle", lastSelected);
            }
        }
        super.onSaveInstanceState(outState);
    }
}
