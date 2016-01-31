package kvj.tegmine.android.ui.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import org.kvj.bravo7.form.FormController;
import org.kvj.bravo7.form.impl.ViewFinder;
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
import kvj.tegmine.android.ui.form.FileSystemItemWidgetAdapter;

/**
 * Created by kvorobyev on 2/14/15.
 */
public class FileSystemBrowser extends Fragment implements ProgressListener {

    private RecyclerView listView = null;
    private DrawerLayout drawer = null;
    private NavigationView drawerPane = null;

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

    public void requestFocus() {
        if (null != listView) {
            listView.requestFocus();
        }
    }

    public static interface BrowserListener {

        public void openNewWindow(Bundle data);

        public void openFile(Bundle data, FileSystemItem item);

        public void updateBrowserTitle(String title);
    }

    private TegmineController controller = null;
    private FileBrowserAdapter adapter = null;
    private Logger logger = Logger.forInstance(this);
    private FormController form = null;

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

    public FileSystemBrowser create(Activity activity, TegmineController controller, Bundle bundle) {
        this.controller = controller;
        form = new FormController(new ViewFinder.ActivityViewFinder(activity));
        form.add(new FileSystemItemWidgetAdapter(controller, null).bundleProviderKey(Tegmine.BUNDLE_PROVIDER), "root");
        form.add(new FileSystemItemWidgetAdapter(controller), "select");
        form.load(activity, bundle);
        logger.d("Will create view", controller, bundle);
        return this;
    }

    private void applyTheme() {
        if (null == controller) {
            return;
        }
        drawerPane.setBackgroundColor(controller.theme().backgroundColor());
        adapter.notifyDataSetChanged();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (null == controller) { // Invalid fragment
            return null;
        }
        View view = inflater.inflate(R.layout.fragment_file_browser, container, false);
        listView = (RecyclerView) view.findViewById(android.R.id.list);
        listView.setLayoutManager(new LinearLayoutManager(container.getContext()));
        registerForContextMenu(listView);
        drawer = (DrawerLayout) view.findViewById(R.id.file_browser_drawer);
        drawerPane = (NavigationView) view.findViewById(R.id.file_browser_navigation);
        drawerPane.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                return navigationHandler(menuItem);
            }
        });
        adapter = new FileBrowserAdapter(controller) {

            @Override
            public void onClick(int position) {
                onListItemClick(position);
            }

            @Override
            public void onCreateContextMenu(ContextMenu menu, int position) {
                FileSystemBrowser.this.onCreateContextMenu(menu, position);
            }

            @Override
            protected boolean onMenuItemClick(MenuItem item, int position) {
                return FileSystemBrowser.this.onContextItemSelected(item, position);
            }
        };
        adapter.load(form.getValue("root", FileSystemItem.class), form.getValue("select", FileSystemItem.class));
        listView.setAdapter(adapter);
        updateTitle();
        refresh();
        return view;
    }

    private boolean navigationHandler(MenuItem menuItem) {
        return true;
    }

    private void updateTitle() {
        if (controller.isRoot(adapter.root())) { // Have root defined
            listener.updateBrowserTitle(controller.fileSystemProvider(adapter.root()).label());
        } else {
            listener.updateBrowserTitle(adapter.root().details());
        }
    }

    private void selectStorage(String name) {
        form.setValue("root", controller.fileSystemProvider(name).root());
        form.setValue("select", null);
        adapter.load(form.getValue("root", FileSystemItem.class), null);
        drawer.closeDrawer(Gravity.LEFT);
        updateTitle();
        refresh(); // Update menu
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
        if (null == controller) {
            return;
        }
        MenuBuilder mb = (MenuBuilder) drawerPane.getMenu();
        SubMenu menu = mb.findItem(R.id.menu_storages).getSubMenu();
        int index = 1;
        menu.clear();
        for (final String name : controller.fileSystemProviders()) {
            if (controller.fileSystemProvider(name).hidden()) {
                continue;
            }
            String label = controller.fileSystemProvider(name).label();
            MenuItem item = menu.add(
                    R.id.menu_storages_group,
                    R.id.menu_storages+index,
                    index,
                    label);
            item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    selectStorage(name);
                    return true;
                }
            });
            item.setCheckable(true);
            item.setChecked(name.equals(adapter.root().providerName()));
            index++;
        }
        menu.setGroupCheckable(R.id.menu_storages_group, true, true);
        mb.onItemsChanged(true);
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

    public boolean onContextItemSelected(MenuItem menuItem, int position) {
        final FileSystemItem item = adapter.getFileSystemItem(position);
//        logger.d("Context item", item.name, menuItem.getItemId());
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

    public void onCreateContextMenu(ContextMenu menu, int position) {
        if (null == adapter) { // Not OK to show
            return;
        }
        FileSystemItem item = adapter.getFileSystemItem(position);
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

    public Toolbar setupToolbar(AppCompatActivity activity) {
        Toolbar toolbar = (Toolbar) activity.findViewById(R.id.main_toolbar);
        activity.setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(activity.getDrawerToggleDelegate().getThemeUpIndicator());
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleNavigation();
            }
        });
        return toolbar;
    }
}
