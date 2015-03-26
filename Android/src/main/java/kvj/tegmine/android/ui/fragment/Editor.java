package kvj.tegmine.android.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import org.kvj.bravo7.SuperActivity;
import org.kvj.bravo7.form.FormController;
import org.kvj.bravo7.form.impl.bundle.StringBundleAdapter;
import org.kvj.bravo7.form.impl.widget.TextViewStringAdapter;
import org.kvj.bravo7.form.impl.widget.TransientAdapter;
import org.kvj.bravo7.log.Logger;
import org.kvj.bravo7.util.Tasks;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import kvj.tegmine.android.R;
import kvj.tegmine.android.Tegmine;
import kvj.tegmine.android.data.TegmineController;
import kvj.tegmine.android.data.def.FileSystemException;
import kvj.tegmine.android.data.def.FileSystemItem;
import kvj.tegmine.android.data.model.FileItemWatcher;
import kvj.tegmine.android.data.model.LineMeta;
import kvj.tegmine.android.data.model.TemplateDef;
import kvj.tegmine.android.ui.form.FileSystemItemWidgetAdapter;

/**
 * Created by kvorobyev on 2/22/15.
 */
public class Editor extends Fragment implements InputFilter {

    private String findString(String where, int point) {
        int lineStarts = where.substring(0, point).lastIndexOf('\n')+1;
        int lineEnds = where.substring(point).indexOf('\n');
        if (-1 == lineEnds) {
            lineEnds = where.length();
        } else {
            lineEnds += point;
        }
        return where.substring(lineStarts, lineEnds);
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end,
                               Spanned spanned, int dstart, int dend) {
//        logger.d("Filter:", source, start, end, dstart, dend);
        if (controller == null) return null;
        if (source.length() == 1 && source.charAt(0) == '\n') {
            // New line
            String line = findString(spanned.toString(), dstart);
            int indent = controller.indent(line);
            String sign = controller.signInLine(line);
//            logger.d("Indent:", line, indent, sign);
            StringBuilder builder = new StringBuilder(source);
            controller.addIndent(builder, indent);
            if (null != sign) {
                builder.append(sign);
                builder.append(' ');
            }
            return builder.toString();
        }
        return null;
    }

    public static interface EditorListener {

        public void onAfterSave();
    }

    public Editor() {
        super();
    }

    private Logger logger = Logger.forInstance(this);
    private TegmineController controller = null;
    private FormController form = null;
    private Bundle bundle = null;
    private FileSystemItem item = null;
    private EditorListener listener = null;
    private EditText editor = null;
    private FileItemWatcher watcher = null;

    public Editor create(TegmineController controller, Bundle bundle) {
        this.controller = controller;
        this.bundle = bundle;
        setHasOptionsMenu(true);
        return this;
    }

    public Editor setListener(EditorListener listener) {
        this.listener = listener;
        return this;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (null == controller) { // Ignore auto fragment
            return null;
        }
        logger.d("Load editor:", bundle);
        View view = inflater.inflate(R.layout.fragment_editor, container, false);
        editor = (EditText) view.findViewById(R.id.editor_text);
        editor.setTextColor(controller.theme().textColor());
        editor.setTextSize(TypedValue.COMPLEX_UNIT_SP, controller.theme().editorTextSp());
        editor.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                return keyHandler(i, keyEvent);
            }
        });
        editor.setFilters(new InputFilter[] {this});
        TextView title = (TextView) view.findViewById(R.id.editor_title_text);
        controller.applyHeaderStyle(title);
        form = new FormController(view);
        form.add(new FileSystemItemWidgetAdapter(controller), "select");
        form.add(new FileSystemItemWidgetAdapter(controller), "root");
        form.add(new TransientAdapter<String>(new StringBundleAdapter(), Tegmine.EDIT_TYPE_ADD), Tegmine.BUNDLE_EDIT_TYPE);
        form.add(new TransientAdapter<String>(new StringBundleAdapter(), null), Tegmine.BUNDLE_EDIT_TEMPLATE);
        form.add(new TextViewStringAdapter(R.id.editor_text, null), "contents");
        form.load(bundle);
        form.setAsOriginal();
        item = form.getValue("select", FileSystemItem.class);
        if (null == item) { // Invalid
            return null;
        }
        watcher = new FileItemWatcher(controller, item) {
            @Override
            public void itemChanged(FileSystemItem item) {
                // File has been changed - ask for reload
                item.commit(); // Change detected
                final boolean doEdit = Tegmine.EDIT_TYPE_EDIT.equals(form.getValue(Tegmine.BUNDLE_EDIT_TYPE, String.class));
                if (doEdit) {
                    // Makes sense - can ask for refresh
                    SuperActivity
                        .showQuestionDialog(getActivity(), "Reload editor?",
                                            "File has been changed. Reload?",
                        new Runnable() {
                            @Override
                            public void run() {
                                loadContents(editor);
                            }
                        }, new Runnable() {
                            @Override
                            public void run() {
                            }
                        });
                }
            }
        };
        title.setText(item.details());
        ImageView icon = (ImageView) view.findViewById(R.id.editor_title_icon);
        boolean doEdit = Tegmine.EDIT_TYPE_EDIT.equals(form.getValue(Tegmine.BUNDLE_EDIT_TYPE, String.class));
        icon.setImageResource(doEdit ? R.drawable.icn_file_edit_light : R.drawable.icn_file_add_light);
        loadContents(editor);
        view.findViewById(R.id.editor_do_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
            }
        });
        return view;
    }

    private boolean keyHandler(int key, KeyEvent keyEvent) {
        if (null == controller) {
            return false;
        }
        if (keyEvent.getAction() != KeyEvent.ACTION_UP) {
            return false;
        }
        if (key == KeyEvent.KEYCODE_TAB) {
            logger.d("Tab event:", keyEvent.getFlags(), keyEvent.getModifiers());
        }
        if (keyEvent.isCtrlPressed() && key == KeyEvent.KEYCODE_S) {
            save();
            return true;
        }
        if (keyEvent.isCtrlPressed()) {
            // Ctrl + template key
            TemplateDef tmpl = controller.templateFromKeyEvent(keyEvent);
            if (null != tmpl) {
                applyTemplate(tmpl);
                return true;
            }
        }
        return false;
    }

    private void requestFocusFor(View view) {
        view.requestFocus();
        InputMethodManager imm = (InputMethodManager) Tegmine.getInstance().getSystemService(
            Context.INPUT_METHOD_SERVICE);
        if (null != imm) {
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void loadContents(final EditText editor) {
        final boolean needValue = (null == form.getValue("contents", String.class)); // Value is not yet loaded
        logger.d("loadContents", needValue);
        final StringBuilder buffer = new StringBuilder();
        if (Tegmine.EDIT_TYPE_ADD.equals(form.getValue(Tegmine.BUNDLE_EDIT_TYPE, String.class))) { // No contents in add mode
            if (needValue) {
                String template = form.getValue(Tegmine.BUNDLE_EDIT_TEMPLATE, String.class);
                // Try to get template
                TemplateDef tmpl = controller.templates().get(template);
                if (null != tmpl) {
                    applyTemplate(tmpl);
                } else {
                    form.setValue("contents", "");
                }
                form.setOriginalValue("contents", form.getValue("contents", String.class));
            }
            requestFocusFor(editor);
            return;
        }
        Tasks.SimpleTask<FileSystemException> task = new Tasks.SimpleTask<FileSystemException>() {
            @Override
            protected FileSystemException doInBackground() {
                try {
                    List<LineMeta> lines = new ArrayList<>();
                    controller.loadFilePart(lines, item, 0, -1);
                    controller.linesForEditor(lines, buffer);
                    return null;
                } catch (FileSystemException e) {
                    return e;
                }
            }

            @Override
            protected void onPostExecute(FileSystemException e) {
                logger.d("Loaded file contents", e, buffer.length());
                if (null == e) { // Success
                    if (needValue) { // Have to set
                        form.setValue("contents", buffer.toString());
                    }
                    form.setOriginalValue("contents", buffer.toString()); // Always
                    requestFocusFor(editor);
                    editor.setSelection(buffer.length());
                } else {
                    logger.e(e, "Failed to load file contents");
                }
            }
        };
        task.exec();
    }

    public void saveState(Bundle data) {
        form.save(data);
        logger.d("Saved state:", data);
    }

    public boolean changed() {
        return form.changed();
    }

    private void save() {
        if (null == controller) { // ???
            return;
        }
        final String contents = form.getValue("contents", String.class);
        final boolean doEdit = Tegmine.EDIT_TYPE_EDIT.equals(form.getValue(Tegmine.BUNDLE_EDIT_TYPE, String.class));
        if (!doEdit && TextUtils.isEmpty(contents)) { // No data entered in add mode - nothing to do
            onAfterSave();
            return;
        }
        Tasks.SimpleTask<FileSystemException> task = new Tasks.SimpleTask<FileSystemException>() {
            @Override
            protected FileSystemException doInBackground() {
                OutputStream stream = null;
                try {
                    if (doEdit) { // Replace
                        stream = controller.fileSystemProvider().replace(item);
                    } else {
                        stream = controller.fileSystemProvider().append(item);
                    }
                    controller.writeEdited(stream, contents, doEdit);
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
                    onAfterSave();
                } else {
                    logger.e(e, "Fail save error");
                }
            }
        };
        task.exec();
    }

    private void onAfterSave() {
        if (null != listener) {
            listener.onAfterSave();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        listener = null;
    }

    private void applyTemplate(TemplateDef tmpl) {
        TegmineController.TemplateApplyResult applyResult = controller.applyTemlate(tmpl);
        form.setValue("contents", applyResult.value());
        editor.setSelection(applyResult.cursor());
    }

    private MenuItem.OnMenuItemClickListener templateListener(final TemplateDef tmpl) {
        return new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                applyTemplate(tmpl);
                return true;
            }
        };
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

    @Override
    public void onResume() {
        super.onResume();
        if (null != watcher) watcher.active(true);
    }

    @Override
    public void onPause() {
        if (null != watcher) watcher.active(false);
        super.onPause();
    }
}
