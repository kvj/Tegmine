package kvj.tegmine.android.ui.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

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
import kvj.tegmine.android.data.model.LineMeta;
import kvj.tegmine.android.data.model.TemplateDef;
import kvj.tegmine.android.ui.form.FileSystemItemWidgetAdapter;

/**
 * Created by kvorobyev on 2/22/15.
 */
public class Editor extends Fragment {

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
        TextView title = (TextView) view.findViewById(R.id.editor_title_text);
        controller.applyHeaderStyle(title);
        form = new FormController(view);
        form.add(new FileSystemItemWidgetAdapter(controller, null), "select");
        form.add(new FileSystemItemWidgetAdapter(controller, null), "root");
        form.add(new TransientAdapter<String>(new StringBundleAdapter(), Tegmine.EDIT_TYPE_ADD), Tegmine.BUNDLE_EDIT_TYPE);
        form.add(new TextViewStringAdapter(R.id.editor_text, null), "contents");
        form.load(bundle);
        form.setAsOriginal();
        item = form.getValue("select", FileSystemItem.class);
        if (null == item) { // Invalid
            return null;
        }
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

    private void loadContents(final EditText editor) {
        logger.d("loadContents", form.getValue("contents", String.class), form.getValue(Tegmine.BUNDLE_EDIT_TYPE, String.class));
        final boolean needValue = null == form.getValue("contents", String.class); // Value is not yet loaded
        final StringBuilder buffer = new StringBuilder();
        Tasks.SimpleTask<FileSystemException> task = new Tasks.SimpleTask<FileSystemException>() {
            @Override
            protected FileSystemException doInBackground() {
                if (Tegmine.EDIT_TYPE_ADD.equals(form.getValue(Tegmine.BUNDLE_EDIT_TYPE, String.class))) { // No contents in add mode
                    return null;
                }
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
                    editor.requestFocus();
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
                } catch (FileSystemException e) {
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(FileSystemException e) {
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

    private MenuItem.OnMenuItemClickListener templateListener(final TemplateDef tmpl) {
        return new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                TegmineController.TemplateApplyResult applyResult = controller.applyTemlate(tmpl);
                editor.setText(applyResult.value());
                editor.setSelection(applyResult.cursor());
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
}
