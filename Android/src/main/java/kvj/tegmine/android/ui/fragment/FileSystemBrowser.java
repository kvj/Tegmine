package kvj.tegmine.android.ui.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.kvj.bravo7.form.FormController;
import org.kvj.bravo7.log.Logger;
import org.kvj.bravo7.util.Tasks;
import org.kvj.bravo7.widget.Dialogs;

import kvj.tegmine.android.R;
import kvj.tegmine.android.Tegmine;
import kvj.tegmine.android.data.TegmineController;
import kvj.tegmine.android.data.def.FileSystemException;
import kvj.tegmine.android.data.def.FileSystemItem;
import kvj.tegmine.android.data.def.FileSystemItemType;
import kvj.tegmine.android.data.def.FileSystemProvider;
import kvj.tegmine.android.data.model.ProgressListener;
import kvj.tegmine.android.ui.adapter.FileBrowserAdapter;
import kvj.tegmine.android.ui.adapter.StorageNavigationAdapter;
import kvj.tegmine.android.ui.form.FileSystemItemWidgetAdapter;

/**
 * Created by kvorobyev on 2/14/15.
 */
public class FileSystemBrowser extends Fragment implements ProgressListener {

    private ListView listView = null;
    private DrawerLayout drawer = null;
    private View drawerPane = null;
    private ListView storageList = null;
    private StorageNavigationAdapter storageListAdapter = null;
    private ImageView titleIcon = null;

    @Override
    public void activityStarted() {
    }

    @Override
    public void activityStopped() {
    }

    @Override
    public void themeChanged() {
        logger.d("Theme changed:", controller.theme().textColor());
        applyTheme();
    }

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

    private void applyTheme() {
        if (null == controller) {
            return;
        }
        titleText.setTextColor(controller.theme().textColor());
        titleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, controller.theme().headerTextSp());
        titleIcon.setImageResource(controller.theme().folderIcon());
        drawerPane.setBackgroundColor(controller.theme().backgroundColor());
        adapter.notifyDataSetChanged();
        storageListAdapter.notifyDataSetChanged();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (null == controller) { // Invalid fragment
            return null;
        }
        View view = inflater.inflate(R.layout.fragment_file_browser, container, false);
        form.setView(view);
        titleText = (TextView) view.findViewById(R.id.file_browser_title_text);
        titleIcon = (ImageView) view.findViewById(R.id.file_browser_title_icon);
        listView = (ListView) view.findViewById(android.R.id.list);
        registerForContextMenu(listView);
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
        adapter = new FileBrowserAdapter(controller);
        adapter.load(form.getValue("root", FileSystemItem.class), form.getValue("select", FileSystemItem.class));
        listView.setAdapter(adapter);
//        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
//            @Override
//            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
//                onLongItemClick(position);
//                return true;
//            }
//        });

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

    private void openNewWindow(FileSystemItem item) {
        if (item.type == FileSystemItemType.Folder) { // Call parent
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

    @Override
    public void onPause() {
        if (null != controller) {
            controller.progressListeners().remove(this);
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (null != controller) {
            controller.progressListeners().add(this);
            applyTheme();
        }
    }

    private void input(String title, String text, final Dialogs.Callback<String> callback) {
        EditText input = Dialogs.inputTextDialog(getActivity(), title, new Dialogs.Callback<String>() {
            @Override
            public void run(String data) {
                if (!TextUtils.isEmpty(data)) { // Have input
                    callback.run(data);
                }
            }
        });
        input.setSelectAllOnFocus(true);
        if (!TextUtils.isEmpty(text)) { // Have input
            input.setText(text);
        }
    }

    private void question(String title, Dialogs.Callback<Void> callback) {
        Dialogs.questionDialog(getActivity(), null, title, callback);
    }

    private interface FileSystemOperation {

        public void exec(FileSystemItem item, FileSystemProvider provider) throws FileSystemException;
    }

    private void executeOperation(final FileSystemItem item, final FileSystemOperation op) {
        Tasks.SimpleTask<FileSystemException> task = new Tasks.SimpleTask<FileSystemException>() {
            @Override
            protected FileSystemException doInBackground() {
                FileSystemProvider provider = controller.fileSystemProvider(item);
                try {
                    op.exec(item, provider);
                } catch (FileSystemException e) {
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(FileSystemException e) {
                if (null != e) { // Have error
                    logger.e(e, "Operation error");
                    Dialogs.toast(getActivity(), e.getMessage());
                } else {
                    adapter.select(item);
                }
            }
        };
        task.exec();
    }

    @Override
    public boolean onContextItemSelected(MenuItem menuItem) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuItem.getMenuInfo();
        final FileSystemItem item = adapter.getFileSystemItem(info.position);
        logger.d("Context item", item.name, info.position, menuItem.getItemId());
        switch (menuItem.getItemId()) {
            case R.id.context_open_folder:
                openNewWindow(item);
                return true;
            case R.id.context_rename:
                input("Enter new name:", item.name, new Dialogs.Callback<String>() {
                    @Override
                    public void run(final String data) {
                        executeOperation(item, new FileSystemOperation() {
                            @Override
                            public void exec(FileSystemItem item, FileSystemProvider provider) throws FileSystemException {
                                provider.rename(item, data);
                            }
                        });
                    }
                });
                return true;
            case R.id.context_new_file:
                input("New file name:", "File", new Dialogs.Callback<String>() {
                    @Override
                    public void run(final String data) {
                        executeOperation(item, new FileSystemOperation() {
                            @Override
                            public void exec(FileSystemItem item, FileSystemProvider provider) throws FileSystemException {
                                provider.create(FileSystemItemType.File, item, data);
                            }
                        });
                    }
                });
                return true;
            case R.id.context_new_folder:
                input("New folder name:", "Folder", new Dialogs.Callback<String>() {
                    @Override
                    public void run(final String data) {
                        executeOperation(item, new FileSystemOperation() {
                            @Override
                            public void exec(FileSystemItem item, FileSystemProvider provider) throws FileSystemException {
                                provider.create(FileSystemItemType.Folder, item, data);
                            }
                        });
                    }
                });
                return true;
            case R.id.context_remove:
                question(String.format("Sure want to remove '%s'?", item.name), new Dialogs.Callback<Void>() {
                    @Override
                    public void run(Void data) {
                        executeOperation(item, new FileSystemOperation() {
                            @Override
                            public void exec(FileSystemItem item, FileSystemProvider provider) throws FileSystemException {
                                provider.remove(item);
                            }
                        });
                    }
                });
                return true;
        }
        return false;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (null == adapter || v != listView) { // Not OK to show
            return;
        }
        menu.clear();
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
        FileSystemItem item = adapter.getFileSystemItem(info.position);
        if (item.type == FileSystemItemType.Folder) { // Folder mode
            getActivity().getMenuInflater().inflate(R.menu.context_folder, menu);
            menu.findItem(R.id.context_new_file).setVisible(
                    controller.itemHasFeature(item, FileSystemProvider.Features.CanCreateFile));
            menu.findItem(R.id.context_new_folder).setVisible(
                    controller.itemHasFeature(item, FileSystemProvider.Features.CanCreateFolder));
            menu.findItem(R.id.context_rename).setVisible(
                    controller.itemHasFeature(item, FileSystemProvider.Features.CanRenameFolder));
            menu.findItem(R.id.context_remove).setVisible(
                    controller.itemHasFeature(item, FileSystemProvider.Features.CanRemoveFolder));
        }
        if (item.type == FileSystemItemType.File) { // Folder mode
            getActivity().getMenuInflater().inflate(R.menu.context_file, menu);
            menu.findItem(R.id.context_rename).setVisible(
                    controller.itemHasFeature(item, FileSystemProvider.Features.CanRenameFile));
            menu.findItem(R.id.context_remove).setVisible(
                    controller.itemHasFeature(item, FileSystemProvider.Features.CanRemoveFile));
        }
    }
}
