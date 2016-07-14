package kvj.tegmine.android.ui.fragment;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.kvj.bravo7.SuperActivity;
import org.kvj.bravo7.form.FormController;
import org.kvj.bravo7.form.impl.bundle.BooleanBundleAdapter;
import org.kvj.bravo7.form.impl.widget.TransientAdapter;
import org.kvj.bravo7.log.Logger;
import org.kvj.bravo7.util.Tasks;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import kvj.tegmine.android.R;
import kvj.tegmine.android.Tegmine;
import kvj.tegmine.android.data.TegmineController;
import kvj.tegmine.android.data.def.FileSystemException;
import kvj.tegmine.android.data.def.FileSystemItem;
import kvj.tegmine.android.data.def.FileSystemProvider;
import kvj.tegmine.android.data.model.FileItemWatcher;
import kvj.tegmine.android.data.model.LineMeta;
import kvj.tegmine.android.data.model.ProgressListener;
import kvj.tegmine.android.data.model.TemplateDef;
import kvj.tegmine.android.data.model.util.Wrappers;
import kvj.tegmine.android.ui.adapter.OneFileAdapter;
import kvj.tegmine.android.ui.form.FileSystemItemWidgetAdapter;

/**
 * Created by kvorobyev on 2/16/15.
 */
public class OneFileViewer extends Fragment implements ProgressListener {

    private RecyclerView listView = null;
    private FileSystemProvider provider = null;

    public void requestFocus() {
        if (null != listView) {
            listView.requestFocus();
        }
    }

    @Override
    public void activityStarted() {
    }

    @Override
    public void activityStopped() {
    }

    @Override
    public void themeChanged() {
        applyTheme();
    }

    public static interface FileViewerListener {

        public void openEditor(Bundle data);

        public void updateViewerTitle(String title);
    }

    private Logger logger = Logger.forInstance(this);
    private TegmineController controller = null;
    private OneFileAdapter adapter = null;
    private List<LineMeta> offsets = null;
    private FormController form = new FormController(null);
    private FileSystemItem item = null;
    private FileViewerListener listener = null;
    private FileItemWatcher watcher = null;

    public OneFileViewer() {
        super();
    }

    public OneFileViewer setListener(FileViewerListener listener) {
        this.listener = listener;
        return this;
    }

    private void applyTheme() {
        if (null == controller) {
            return;
        }
        adapter.notifyDataSetChanged();
    }

    public OneFileViewer create(Activity activity, final TegmineController controller, Bundle bundle) {
        this.controller = controller;
        form.add(new FileSystemItemWidgetAdapter(controller), Tegmine.BUNDLE_SELECT);
        form.add(new FileSystemItemWidgetAdapter(controller).bundleProviderKey(Tegmine.BUNDLE_PROVIDER), "root");
        form.add(new TransientAdapter<>(new BooleanBundleAdapter(), controller.showNumbers()),
                 "showNumbers");
        form.add(new TransientAdapter<>(new BooleanBundleAdapter(), controller.wrapLines()),
                "wrapLines");
        form.load(activity, bundle);
        item = form.getValue(Tegmine.BUNDLE_SELECT, FileSystemItem.class);
        logger.d("new FileViewer:", item, bundle, controller.showNumbers(), controller.wrapLines());
        if (null == item) {
            controller.messageShort("File not found");
            return null;
        }
        provider = controller.fileSystemProvider(item);
        watcher = new FileItemWatcher(controller, item) {
            @Override
            public void itemChanged(FileSystemItem item) {
                refresh();
                watcher.reset();
            }
        };
        return this;
    }

    public FileSystemItem item() {
        return form.getValue(Tegmine.BUNDLE_SELECT);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (null == controller) { // Invalid fragment
            return null;
        }
        setHasOptionsMenu(true);
        View view = inflater.inflate(R.layout.fragment_one_file, container, false);
        if (null != listener) {
            listener.updateViewerTitle(item.name);
        }
        view.findViewById(R.id.one_file_do_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startEditor(Tegmine.EDIT_TYPE_ADD, null);
            }
        });
        view.findViewById(R.id.one_file_do_edit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startEditor(Tegmine.EDIT_TYPE_EDIT, null);
            }
        });
        listView = (RecyclerView) view.findViewById(android.R.id.list);
        listView.setLayoutManager(new LinearLayoutManager(container.getContext()));
        adapter = new OneFileAdapter(controller, item) {

            @Override
            public void onClick(int position) {
                onListItemClick(position);
            }

            @Override
            protected void onContextMenu(ContextMenu menu, int position) {
                onItemContextMenu(menu, position);
            }
        };
        adapter.showNumbers(form.getValue("showNumbers", Boolean.class));
        adapter.wrapLines(form.getValue("wrapLines", Boolean.class));
        listView.setAdapter(adapter);
        listView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int key, KeyEvent keyEvent) {
                if (controller != null) return keyHandler(key, keyEvent);
                return false;
            }
        });
        adapter.setBounds(0, -1, new Runnable() {
            @Override
            public void run() {
                if (provider.scrollToBottom() && adapter.getItemCount() > 0) {
                    listView.scrollToPosition(adapter.getItemCount()-1);
                }
            }
        });
        return view;
    }

    private boolean keyHandler(int key, KeyEvent keyEvent) {
        if (keyEvent.getAction() != KeyEvent.ACTION_DOWN) {
            return false;
        }
        if (keyEvent.isCtrlPressed()) {
            switch (key) {
                case KeyEvent.KEYCODE_R:
                    refresh();
                    return true;
                case KeyEvent.KEYCODE_A:
                    startEditor(Tegmine.EDIT_TYPE_ADD, null);
                    return true;
                case KeyEvent.KEYCODE_E:
                    startEditor(Tegmine.EDIT_TYPE_EDIT, null);
                    return true;
                case KeyEvent.KEYCODE_Z:
                    startEditor(null, null); // Show editors
                    return true;
                case KeyEvent.KEYCODE_C:
//                    copyAt(listView.getSelectedItemPosition());
                    return true;
                case KeyEvent.KEYCODE_V:
                    paste();
                    return true;
                case KeyEvent.KEYCODE_W:
                    toggleWrapLines();
                    return true;
                case KeyEvent.KEYCODE_N:
                    toggleLineNumbers();
                    getActivity().invalidateOptionsMenu();
                    return true;
            }
            // Ctrl + template key
            TemplateDef tmpl = controller.templateFromKeyEvent(keyEvent);
            if (null != tmpl) {
                startEditor(Tegmine.EDIT_TYPE_ADD, tmpl.code());
                return true;
            }
        }
        return false;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_one_file, menu);
        if (null != form) {
            menu.findItem(R.id.menu_show_numbers).setChecked(form.getValue("showNumbers", Boolean.class));
            menu.findItem(R.id.menu_wrap_lines).setChecked(form.getValue("wrapLines", Boolean.class));
        }
    }

    private ClipboardManager getClipboard() {
        return (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
    }

    private void appendText(final CharSequence text, final Runnable afterFinish) {
        Tasks.SimpleTask<FileSystemException> task = new Tasks.SimpleTask<FileSystemException>() {
            @Override
            protected FileSystemException doInBackground() {
                OutputStream stream = null;
                try {
                    stream = provider.append(item);
                    controller.writeEdited(provider, stream, text.toString().trim(), false);
                    watcher.reset();
                } catch (FileSystemException e) {
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(FileSystemException e) {
//                logger.d("Save result:", e);
                if (null == e) { // Saved
                    refresh();
                    if (null != afterFinish) afterFinish.run();
                } else {
                    logger.e(e, "Fail save error");
                }
            }
        };
        task.exec();
    }

    private boolean paste() {
        if (!getClipboard().hasPrimaryClip() || !getClipboard().getPrimaryClipDescription().hasMimeType(
            ClipDescription.MIMETYPE_TEXT_PLAIN)) {
            SuperActivity.notifyUser(getActivity(), "No text to paste");
            return false;
        }
        CharSequence text = getClipboard().getPrimaryClip().getItemAt(0).getText();
        appendText(text, new Runnable() {
            @Override
            public void run() {
                SuperActivity.notifyUser(getActivity(), "Text pasted");
            }
        });
        return true;
    }

    private boolean toggleLineNumbers() {
        boolean showNumbers = !form.getValue("showNumbers", Boolean.class);
        form.setValue("showNumbers", showNumbers);
        adapter.showNumbers(showNumbers);
        adapter.notifyDataSetChanged();
        getActivity().invalidateOptionsMenu();
        return showNumbers;
    }

    private boolean toggleWrapLines() {
        boolean wrapLines = !form.getValue("wrapLines", Boolean.class);
        form.setValue("wrapLines", wrapLines);
        adapter.wrapLines(wrapLines);
        adapter.notifyDataSetChanged();
        getActivity().invalidateOptionsMenu();
        return wrapLines;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (null == controller) return true; // Ignore
        switch (item.getItemId()) {
            case R.id.menu_paste_text:
                paste();
                return true;
            case R.id.menu_show_editors:
                startEditor(null, null);
                return true;
            case R.id.menu_show_numbers:
                toggleLineNumbers();
                return true;
            case R.id.menu_wrap_lines:
                toggleWrapLines();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onListItemClick(int position) {
        if (null != adapter) { // Toggle folding
            adapter.toggle(position);
            listView.requestFocus();
        }
    }

    private boolean copyAt(int position) {
        final String contents = adapter.partString(position);
        if (TextUtils.isEmpty(contents)) { // No data
            return false;
        }
        ClipData clip = ClipData.newPlainText(adapter.line(position).data(), contents);
        getClipboard().setPrimaryClip(clip);
        SuperActivity.notifyUser(getActivity(), "Copied to clipboard");
        return true;
    }

    private boolean shareAt(int position) {
        final String contents = adapter.partString(position);
        if (TextUtils.isEmpty(contents)) { // No data
            return true;
        }
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, contents);
        sendIntent.setType("text/plain");
        startActivity(sendIntent);
        return true;
    }

    private void onItemContextMenu(ContextMenu menu, final int position) {
        if (null != adapter) { // OK to show
            getActivity().getMenuInflater().inflate(R.menu.context_one_file, menu);
            menu.findItem(R.id.context_share).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        shareAt(position);
                        return true;
                    }
                });
            menu.findItem(R.id.context_copy_cboard).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        copyAt(position);
                        return true;
                    }
                });
            Collection<Wrappers.Pair<String>> links = adapter.features(position, "link");
            for (Wrappers.Pair<String> link : links) {
                newLinkMenu(menu, link.v2());
            }
        }
    }

    private MenuItem newLinkMenu(ContextMenu menu, final String url) {
        MenuItem menuItem = menu.add(url);
        menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                controller.openLink(getActivity(), url, item);

                return true;
            }
        });
        return menuItem;
    }

    private void loadFileLayout() {
        Tasks.SimpleTask<List<LineMeta>> task = new Tasks.SimpleTask<List<LineMeta>>() {
            @Override
            protected List<LineMeta> doInBackground() {
                try {
                    return controller.makeFileLayout(item);
                } catch (FileSystemException e) {
                    logger.e(e, "Failed to read file");
                }
                return new ArrayList<>();
            }

            @Override
            protected void onPostExecute(List<LineMeta> longs) {
                offsets = longs;
                logger.d("Will have lines:", offsets.size());
            }
        };
        task.exec();

    }

    public void saveState(Bundle outState) {
        form.save(outState, "root", Tegmine.BUNDLE_SELECT, "wrapLines", "showNumbers");
    }

    private void startEditor(String editType, String tmpl) {
        Bundle bundle = new Bundle();
        if (null != editType) { // Have it set
            bundle.putString(Tegmine.BUNDLE_EDIT_TYPE, editType);
        }
        if (null != tmpl) {
            bundle.putString(Tegmine.BUNDLE_EDIT_TEMPLATE, tmpl);
        }
        form.save(bundle, true, "root", Tegmine.BUNDLE_SELECT);
        if (null != listener) {
            listener.openEditor(bundle);
        }
    }

    public void refresh() {
        if (null != adapter) { // Reload contents
            adapter.setBounds(0, -1, null);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        listener = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (null != controller) {
            controller.progressListeners().add(this);
            applyTheme();
        }
        if (null != watcher) watcher.active(true);
    }

    @Override
    public void onPause() {
        if (null != watcher) watcher.active(false);
        if (null != controller) {
            controller.progressListeners().remove(this);
        }
        super.onPause();
    }
}
