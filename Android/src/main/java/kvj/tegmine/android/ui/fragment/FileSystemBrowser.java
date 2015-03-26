package kvj.tegmine.android.ui.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.DrawerLayout;
import android.util.TypedValue;
import android.view.Gravity;
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
import kvj.tegmine.android.ui.adapter.StorageNavigationAdapter;
import kvj.tegmine.android.ui.form.FileSystemItemWidgetAdapter;

/**
 * Created by kvorobyev on 2/14/15.
 */
public class FileSystemBrowser extends Fragment {

    private ListView listView = null;
    private DrawerLayout drawer = null;
    private View drawerPane = null;
    private ListView storageList = null;
    private StorageNavigationAdapter storageListAdapter = null;

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
        form.add(new FileSystemItemWidgetAdapter(controller, controller.fileSystemProvider(bundle.getString(Tegmine.BUNDLE_PROVIDER)).root()), "root");
        form.add(new FileSystemItemWidgetAdapter(controller), "select");
        form.load(bundle);
        logger.d("Will create view", controller, bundle);
        return this;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (null == controller) { // Invalid fragment
            return null;
        }
        View view = inflater.inflate(R.layout.fragment_file_browser, container, false);
        form.setView(view);
        titleText = (TextView) view.findViewById(R.id.file_browser_title_text);
        titleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, controller.theme().headerTextSp());
        titleText.setTextColor(controller.theme().textColor());
        listView = (ListView) view.findViewById(android.R.id.list);
        drawer = (DrawerLayout) view.findViewById(R.id.file_browser_drawer);
        drawerPane = view.findViewById(R.id.file_browser_navigation);
        storageList = (ListView) view.findViewById(R.id.file_browser_storage_list);
        storageList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                selectStorage(storageListAdapter.getItem(i));
            }
        });
        storageListAdapter = new StorageNavigationAdapter(controller) {

            @Override
            public boolean selected(String name) {
                return adapter.root().providerName().equals(name);
            }
        };
        storageList.setAdapter(storageListAdapter);
        storageListAdapter.refresh();
        drawerPane.setBackgroundColor(controller.theme().backgroundColor());
        adapter = new FileBrowserAdapter(controller);
        adapter.load(form.getValue("root", FileSystemItem.class), form.getValue("select", FileSystemItem.class));
        listView.setAdapter(adapter);
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                onLongItemClick(position);
                return true;
            }
        });
        listView.setOnItemClickListener( new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long l) {
                onListItemClick(pos);
            }
        });
        drawer.setDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                storageListAdapter.refresh();
            }

            @Override
            public void onDrawerClosed(View drawerView) {
            }

            @Override
            public void onDrawerStateChanged(int newState) {
            }
        });
        updateTitle();
        return view;
    }

    private void updateTitle() {
        if (controller.isRoot(adapter.root())) { // Have root defined
            titleText.setText(controller.fileSystemProvider(adapter.root().providerName()).label());
        } else {
            titleText.setText(adapter.root().details());
        }
    }

    private void selectStorage(String name) {
        form.setValue("root", controller.fileSystemProvider(name).root());
        form.setValue("select", null);
        adapter.load(form.getValue("root", FileSystemItem.class), null);
        drawer.closeDrawer(Gravity.LEFT);
        updateTitle();
    }

    private void onLongItemClick(int position) {
        FileSystemItem item = adapter.getFileSystemItem(position);
        if (item.type == FileSystemItemType.Folder) { // Collapse/expand
            Bundle bundle = new Bundle();
            bundle.putString("root", item.toURL());
            if (null != listener) {
                listener.openNewWindow(bundle);
            }
        }
    }

    public void onListItemClick(int position) {
        FileSystemItem item = adapter.getFileSystemItem(position);
        form.setValue("select", item);
        if (item.type == FileSystemItemType.Folder) { // Collapse/expand
            adapter.collapseExpand(position);
        }
        if (item.type == FileSystemItemType.File) { // Open viewer
            Bundle bundle = new Bundle();
            if (null != adapter.root()) { // Have custom root
                bundle.putString("root", adapter.root().toURL());
            }
            bundle.putString("select", item.toURL());
            if (null != listener) {
                listener.openFile(bundle, item);
            }
        }

    }

    public void saveState(Bundle outState) {
        form.save(outState, "root", "select");
    }

    public void toggleNavigation() {
        if (null != drawer) {
            if (drawer.isDrawerOpen(Gravity.LEFT)) {
                drawer.closeDrawer(Gravity.LEFT);
            } else {
                drawer.openDrawer(Gravity.LEFT);
            }

        }
    }

    public void refresh() {
        if (null != storageListAdapter) {
            storageListAdapter.refresh();
        }
    }

}
