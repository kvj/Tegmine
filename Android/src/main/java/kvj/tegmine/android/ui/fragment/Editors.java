package kvj.tegmine.android.ui.fragment;

import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerTitleStrip;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;

import org.kvj.bravo7.SuperActivity;
import org.kvj.bravo7.form.FormController;
import org.kvj.bravo7.log.Logger;
import org.kvj.bravo7.util.Listeners;
import org.kvj.bravo7.util.Tasks;

import java.io.OutputStream;

import kvj.tegmine.android.R;
import kvj.tegmine.android.data.TegmineController;
import kvj.tegmine.android.data.def.FileSystemException;
import kvj.tegmine.android.data.model.EditorInfo;
import kvj.tegmine.android.data.model.TemplateDef;
import kvj.tegmine.android.ui.adapter.EditorsAdapter;
import kvj.tegmine.android.ui.form.FileSystemItemWidgetAdapter;

/**
 * Created by kvorobyev on 5/5/15.
 */
public class Editors extends Fragment {

    public EditorInfo add(Bundle data) {
        EditorInfo info = controller.editors().fromBundle(data);// Loaded?
        if (null != pager) {
            // Switch to page
            adapter.notifyDataSetChanged();
            pager.setCurrentItem(controller.editors().selected());
        }
        return info;
    }

    public boolean closeFindDialog() {
        if (selected() == null) { // No editor
            return false;
        }
        return selected().view.closeFindDialog();
    }

    public interface EditorsListener {
        public void onHide();
    }

    private Listeners<EditorsListener> listeners = new Listeners<>();

    private Logger logger = Logger.forInstance(this);

    private TegmineController controller = null;
    private ViewPager pager = null;
    private PagerTitleStrip tabStrip = null;
    private EditorsAdapter adapter = null;
    private FormController form = null;

    public Editors create(TegmineController controller, Bundle bundle) {
        this.controller = controller;
        setHasOptionsMenu(true);
        form = new FormController(null);
        form.add(new FileSystemItemWidgetAdapter(controller), "select");
        form.add(new FileSystemItemWidgetAdapter(controller), "root");
        form.load(bundle);
        add(bundle);
        return this;
    }

    public Listeners<EditorsListener> listeners() {
        return listeners;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (null == controller) {
            return null;
        }
        logger.d("New editors");
        View view = inflater.inflate(R.layout.fragment_editors, container, false);
        pager = (ViewPager) view.findViewById(R.id.editors_pager);
        tabStrip = (PagerTitleStrip) view.findViewById(R.id.editors_strip);
        adapter = new EditorsAdapter(getActivity().getSupportFragmentManager());
        adapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                pager.setVisibility(adapter.getCount() > 0 ? View.VISIBLE : View.GONE);
            }
        });
        pager.setAdapter(adapter);
        pager.setSaveEnabled(false);
        pager.setCurrentItem(controller.editors().selected());
        pager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset,
                                       int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                controller.editors().selected(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
        view.findViewById(R.id.editors_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeSelected(true);
            }
        });
        view.findViewById(R.id.editors_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSelected(true);
            }
        });
        return view;
    }

    private View.OnKeyListener keyListener = new View.OnKeyListener() {
        @Override
        public boolean onKey(View view, int i, KeyEvent keyEvent) {
            return keyHandler(i, keyEvent);
        }
    };

    private boolean keyHandler(int key, KeyEvent keyEvent) {
        if (keyEvent.isCtrlPressed() && key == KeyEvent.KEYCODE_S) {
            saveSelected(true);
            return false;
        }
        if (keyEvent.isCtrlPressed() && key == KeyEvent.KEYCODE_Q) {
            closeSelected(true);
            return false;
        }
        if (keyEvent.isCtrlPressed() && key == KeyEvent.KEYCODE_TAB) {
            // Ctrl+Tab
            if (!pager.arrowScroll(View.FOCUS_RIGHT)) {
                pager.setCurrentItem(0);
            }
            return false;
        }
        return true;
    }

    private MenuItem.OnMenuItemClickListener templateListener(final TemplateDef tmpl) {
        return new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                EditorInfo info = selected();
                if (null != info && null != info.view) {
                    info.view.applyTemplate(tmpl);
                }
                return true;
            }
        };
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_editor_find:
                if (null != selected()) { // Have selected
                    selected().view.showFindDialog("", true);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_editor, menu);
        if (null != controller) { // OK
            SubMenu subMenu = menu.findItem(R.id.menu_editor_templates).getSubMenu();
            int idx = 0;
            for (TemplateDef template : controller.templates().values()) {
                MenuItem menuItem = subMenu.add(1, idx++, idx, template.label());
                menuItem.setOnMenuItemClickListener(templateListener(template));
            }
        }
    }

    private EditorInfo selected() {
        final int sel = controller.editors().selected();
        final EditorInfo selected = controller.editors().tab(sel);
        if (null == selected || null == selected.view) {
            return null;
        }
        if (selected.mode == EditorInfo.Mode.None) { // Nothing to close
            return null;
        }
        return selected;
    }

    private void saveSelected(final boolean close) {
        final EditorInfo selected = selected();
        if (null == selected) {
            return;
        }
        selected.view.toInfo();
        final boolean doEdit = selected.mode == EditorInfo.Mode.Edit;
        Tasks.SimpleTask<FileSystemException> task = new Tasks.SimpleTask<FileSystemException>() {
            @Override
            protected FileSystemException doInBackground() {
                if (!doEdit && TextUtils.isEmpty(selected.text)) { // No data entered in add mode - nothing to do
                    return null;
                }
                OutputStream stream = null;
                try {
                    if (doEdit) { // Replace
                        stream = controller.fileSystemProvider().replace(selected.view.item());
                    } else {
                        stream = controller.fileSystemProvider().append(selected.view.item());
                    }
                    controller.writeEdited(stream, selected.text, doEdit);
                    selected.view.item().commit();
                } catch (FileSystemException e) {
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(FileSystemException e) {
                logger.d("Save result:", e);
                if (null == e) { // Saved
                    if (close) { // Close after save
                        closeSelected(false);
                    }
                } else {
                    logger.e(e, "Fail save error");
                }
            }
        };
        task.exec();
    }

    private void closeSelected(boolean confirm) {
        // Close selected tab and show dialog if needed
        final int sel = controller.editors().selected();
        final EditorInfo selected = selected();
        if (null == selected) {
            return;
        }
        selected.view.toInfo();
        final Runnable closeTask = new Runnable() {
            @Override
            public void run() {
//                pager.setCurrentItem(controller.editors().nextSelected(), false);
                controller.editors().remove(sel);
                adapter.notifyDataSetChanged();
                if (controller.editors().size() == 0) {
                    // Report hide
                    listeners.emit(new Listeners.ListenerEmitter<EditorsListener>() {
                        @Override
                        public boolean emit(EditorsListener listener) {
                            listener.onHide();
                            return true;
                        }
                    });
                }
            }
        };
        if (confirm && selected.crc != EditorInfo.hash(selected.text)) { // Changed
            // Changed - show confirm
            logger.d("Changed?", selected.crc, EditorInfo.hash(selected.text));
            SuperActivity.showQuestionDialog(getActivity(), null,
                    "Contents have been changed. Really close?",
                    new Runnable() {
                        @Override
                        public void run() {
                            closeTask.run();
                        }
                    }, new Runnable() {
                        @Override
                        public void run() {
                        }
                    });
        } else {
            closeTask.run();
        }
    }

    public void saveState(Bundle outState) {
        controller.editors().saveState();
        form.save(outState);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        if (null != controller) {
            controller.editors().keyListeners.add(keyListener);
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        if (null != controller) {
            controller.editors().keyListeners.remove(keyListener);
            controller.editors().saveState();
        }
        super.onDestroy();
    }
}
