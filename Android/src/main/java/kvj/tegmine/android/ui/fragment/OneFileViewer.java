package kvj.tegmine.android.ui.fragment;

import android.animation.ObjectAnimator;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

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

    private ListView listView = null;
    private ImageView titleIcon = null;

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
    }

    private Logger logger = Logger.forInstance(this);
    private TegmineController controller = null;
    private TextView titleText = null;
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
        titleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, controller.theme().headerTextSp());
        titleText.setTextColor(controller.theme().textColor());
        titleIcon.setImageResource(controller.theme().fileIcon());
        listView.deferNotifyDataSetChanged();
    }

    public OneFileViewer create(final TegmineController controller, Bundle bundle) {
        this.controller = controller;
        form.add(new FileSystemItemWidgetAdapter(controller), "select");
        form.add(new FileSystemItemWidgetAdapter(controller), "root");
        form.add(new TransientAdapter<>(new BooleanBundleAdapter(), controller.showNumbers()), "showNumbers");
        form.add(new TransientAdapter<>(new BooleanBundleAdapter(), controller.wrapLines()), "wrapLines");
        form.load(bundle);
        item = form.getValue("select", FileSystemItem.class);
        logger.d("new FileViewer:", item, bundle, controller.showNumbers(), controller.wrapLines());
        if (null == item) {
            SuperActivity.notifyUser(controller.context(), "File not found");
            return null;
        }
        watcher = new FileItemWatcher(controller, item) {
            @Override
            public void itemChanged(FileSystemItem item) {
                refresh();
                item.commit(); // Change detected
            }
        };
        return this;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (null == controller) { // Invalid fragment
            return null;
        }
        setHasOptionsMenu(true);
        View view = inflater.inflate(R.layout.fragment_one_file, container, false);
        titleText = (TextView) view.findViewById(R.id.one_file_title_text);
        titleText.setText(item.details());
        titleIcon = (ImageView) view.findViewById(R.id.one_file_title_icon);
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
        listView = (ListView) view.findViewById(android.R.id.list);
        adapter = new OneFileAdapter(controller, item);
        adapter.showNumbers(form.getValue("showNumbers", Boolean.class));
        adapter.wrapLines(form.getValue("wrapLines", Boolean.class));
        listView.setAdapter(adapter);
        registerForContextMenu(listView);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onListItemClick(position);
            }
        });
        final View buttonsPane = view.findViewById(R.id.one_file_buttons);
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int state) {
                if (state == SCROLL_STATE_IDLE) {
                    // Stopped
                    changeButtonsDim(buttonsPane, listView);
                }
            }

            @Override
            public void onScroll(AbsListView absListView, int i, int i2, int i3) {
            }
        });
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
                if (controller.scrollToBottom()) {
                    listView.setSelection(adapter.getCount() - 1);
                    listView.post(new Runnable() {
                        @Override
                        public void run() {
                            changeButtonsDim(buttonsPane, listView);
                        }
                    });
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
                    copyAt(listView.getSelectedItemPosition());
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

    private boolean buttonsDimmed = false;

    private void changeButtonsDim(View buttonsPane, ListView listView) {
        boolean dimButtons = listView.getCount() == listView.getLastVisiblePosition()+1 && listView.getFirstVisiblePosition()>0;
//        logger.d("Scroll state:", listView.getCount(), listView.getFirstVisiblePosition(), listView.getLastVisiblePosition(), dimButtons);
        if (dimButtons != buttonsDimmed) { // State changed
            ObjectAnimator anim = ObjectAnimator.ofFloat(buttonsPane, "alpha", dimButtons ? 1f : 0.3f, dimButtons ? 0.3f : 1f);
            anim.setDuration(300);
            anim.start();
            buttonsDimmed = dimButtons;
        }
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
                    stream = controller.fileSystemProvider().append(item);
                    controller.writeEdited(stream, text.toString().trim(), false);
                    item.commit();
                } catch (FileSystemException e) {
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(FileSystemException e) {
                logger.d("Save result:", e);
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

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.context_share:
                shareAt(info.position);
                return true;
            case R.id.context_copy_cboard:
                copyAt(info.position);
                return true;
        }
        return false;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (null != adapter && v == listView) { // OK to show
            menu.clear();
            getActivity().getMenuInflater().inflate(R.menu.context_one_file, menu);
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
            Collection<Wrappers.Pair<String>> links = adapter.features(info.position, "link");
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
        form.save(outState, "root", "select", "wrapLines", "showNumbers");
    }

    private void startEditor(String editType, String tmpl) {
        Bundle bundle = new Bundle();
        if (null != editType) { // Have it set
            bundle.putString(Tegmine.BUNDLE_EDIT_TYPE, editType);
        }
        if (null != tmpl) {
            bundle.putString(Tegmine.BUNDLE_EDIT_TEMPLATE, tmpl);
        }
        form.save(bundle, true, "root", "select");
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
