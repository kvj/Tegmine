package kvj.tegmine.android.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.CharacterStyle;
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

import org.kvj.bravo7.form.FormControllerNG;
import org.kvj.bravo7.form.impl.ViewFinder;
import org.kvj.bravo7.form.impl.bundle.StringBundleAdapter;
import org.kvj.bravo7.form.impl.widget.TextViewCharSequenceAdapter;
import org.kvj.bravo7.form.impl.widget.TransientAdapter;
import org.kvj.bravo7.log.Logger;
import org.kvj.bravo7.util.DataUtil;
import org.kvj.bravo7.util.Tasks;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
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
import kvj.tegmine.android.data.model.SyntaxDef;
import kvj.tegmine.android.data.model.TemplateDef;
import kvj.tegmine.android.ui.form.FileSystemItemWidgetAdapter;

/**
 * Created by kvorobyev on 2/22/15.
 */
public class Editor extends Fragment implements ProgressListener {

    private InputFilter indentFilter = new InputFilter() {
        @Override
        public CharSequence filter(CharSequence source, int start, int end,
                                   Spanned spanned, int dstart, int dend) {
//        logger.d("Filter:", source, start, end, dstart, dend);
            if (source.length() == 1 && source.charAt(0) == '\n') {
                // New line
                String line = findString(spanned.toString(), dstart).line;
                int indent = controller.indent(provider, line);
                String sign = controller.signInLine(line);
//            logger.d("Indent:", line, indent, sign);
                StringBuilder builder = new StringBuilder(source);
                controller.addIndent(provider, builder, indent);
                if (null != sign) {
                    builder.append(sign);
                    builder.append(' ');
                }
                return builder;
            }
            return null;
        }
    };

    private TextView title = null;
    private ImageView titleIcon = null;
    private SyntaxDef syntax = null;

    private static class ChangeMark {
        private final int start;
        private final int count;

        private ChangeMark(int start, int count) {
            this.start = start;
            this.count = count;
        }
    }

    private TextWatcher coloringWatcher = new TextWatcher() {

        private volatile boolean inChange = false;

        private ChangeMark findMark(Spannable s) {
            ChangeMark[] marks = s.getSpans(0, 1, ChangeMark.class);
            if (marks.length > 0) { // Found
                return marks[0];
            }
            return null;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (inChange) { // Ignore
                return;
            }
            Spannable builder = (Spannable) s;
            ChangeMark mark = findMark(builder);
            if (null == mark && s.length() > 0) { // Add mark
                builder.setSpan(new ChangeMark(start, count), 0, 1, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (null == syntax) { // Ignore
                return;
            }
            if (inChange) { // Ignore
                return;
            }
            SpannableStringBuilder builder = (SpannableStringBuilder) s;
            ChangeMark mark = findMark(builder);
            if (null == mark) { // In change
                return;
            }
            inChange = true;
            String txt = s.toString();
            PositionInText startLine = findString(txt, mark.start);
            PositionInText finishLine = findString(txt, mark.start + mark.count);
            int startIndex = startLine.lineStarts;
            int endIndex = finishLine.lineStarts + finishLine.line.length();
            String part = txt.substring(startIndex, endIndex);
            CharacterStyle[] spans = builder.getSpans(startIndex, endIndex, CharacterStyle.class);
            for (CharacterStyle span : spans) { // Clear span
                builder.removeSpan(span);
            }
            String[] parts = part.split("\n");
            int index = startIndex;
            for (int i = 0; i < parts.length; i++) { // Replace text
                String line = parts[i];
                SpannableStringBuilder lineBuilder = new SpannableStringBuilder();
                controller.applyTheme(provider, syntax, line, lineBuilder);
                spans = lineBuilder.getSpans(0, lineBuilder.length(), CharacterStyle.class);
                for (CharacterStyle span : spans) { // $COMMENT
                    builder.setSpan(span,
                            lineBuilder.getSpanStart(span)+index,
                            lineBuilder.getSpanEnd(span)+index,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                index += line.length()+1;
            }
            builder.removeSpan(mark);
            inChange = false;
        }
    };

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

    private void applyTheme() {
        boolean doEdit = Tegmine.EDIT_TYPE_EDIT.equals(form.get(Tegmine.BUNDLE_EDIT_TYPE));
        editor.setTextColor(controller.theme().textColor());
        editor.setTextSize(TypedValue.COMPLEX_UNIT_SP, controller.theme().editorTextSp());
        if (title != null)
            controller.applyHeaderStyle(title);
        if (titleIcon != null)
            titleIcon.setImageResource(doEdit ?
                    controller.theme().fileEditIcon():
                    controller.theme().fileAddIcon());
    }

    private class PositionInText {

        int lineStarts;
        int lineNo;
        int positionInLine;
        String line;
    }

    private PositionInText findString(String where, int point) {
        int lineStarts = where.substring(0, point).lastIndexOf('\n')+1;
        int lineEnds = where.substring(point).indexOf('\n');
        if (-1 == lineEnds) {
            lineEnds = where.length();
        } else {
            lineEnds += point;
        }
        PositionInText pos = new PositionInText();
        pos.lineStarts = lineStarts;
        pos.line = where.substring(lineStarts, lineEnds);
        pos.positionInLine = point - lineStarts;
        pos.lineNo = 0;
        int newLineStart = 0;
        while (true) {
            int newLine = where.indexOf('\n', newLineStart);
            if (newLine == -1 || newLine >= lineStarts) {
                break;
            }
            pos.lineNo++;
            newLineStart = newLine+1;
        }
        return pos;
    }
    public interface EditorListener {

        void onAfterSave();
    }

    public Editor() {
        super();
    }

    private Logger logger = Logger.forInstance(this);
    private TegmineController controller = null;
    private FormControllerNG form = new FormControllerNG(new ViewFinder.FragmentViewFinder(this));
    private FileSystemItem item = null;
    private FileSystemProvider provider = null;
    private EditorListener listener = null;
    private EditText editor = null;
    private FileItemWatcher watcher = null;

    public Editor create(Intent intent, final TegmineController controller, Bundle bundle) {
        this.controller = controller;
        form.init(intent, bundle);
        setHasOptionsMenu(true);
        return this;
    }

    private boolean addMode() {
        return Tegmine.EDIT_TYPE_ADD.equals(form.get(Tegmine.BUNDLE_EDIT_TYPE));
    }

    private void loadValue(final DataUtil.Callback<CharSequence> cb) {
        final SpannableStringBuilder buffer = new SpannableStringBuilder();
        if (addMode()) { // No contents in add mode
            String template = form.get(Tegmine.BUNDLE_EDIT_TEMPLATE);
            requestFocusFor(editor);
            enableEditorListeners();
            TegmineController.TemplateApplyResult applyResult = applyTemplate(controller.templates().get(template), buffer);
            cb.call(buffer);
            if (applyResult != null) {
                editor.setSelection(applyResult.cursor());
            }
            return;
        }
        Tasks.SimpleTask<FileSystemException> task = new Tasks.SimpleTask<FileSystemException>() {
            @Override
            protected FileSystemException doInBackground() {
                try {
                    List<LineMeta> lines = new ArrayList<>();
                    controller.loadFilePart(lines, item, syntax, 0, -1);
                    controller.linesForEditor(provider, lines, buffer, syntax);
                    return null;
                } catch (FileSystemException e) {
                    return e;
                }
            }

            @Override
            protected void onPostExecute(FileSystemException e) {
                logger.d("Loaded file contents", e, buffer.length());
                if (null == e) { // Success
                    cb.call(buffer);
                    requestFocusFor(editor);
                    enableEditorListeners();
                } else {
                    logger.e(e, "Failed to load file contents");
                    controller.messageLong("Failed to load contents");
                }
            }
        };
        task.exec();
    }

    public Editor setListener(EditorListener listener) {
        this.listener = listener;
        return this;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        item = form.add("select", new FileSystemItemWidgetAdapter(controller));
        if (null == item) { // Invalid
            return;
        }
        form.add("root", new FileSystemItemWidgetAdapter(controller));
        provider = controller.fileSystemProvider(item);
        syntax = controller.findSyntax(item);
        form.add(Tegmine.BUNDLE_EDIT_TYPE, new TransientAdapter<>(new StringBundleAdapter(), Tegmine.EDIT_TYPE_ADD));
        form.add(Tegmine.BUNDLE_EDIT_TEMPLATE, new TransientAdapter<>(new StringBundleAdapter(), null));
        form.add("contents", new TextViewCharSequenceAdapter(R.id.editor_text, null) {
            @Override
            public void load(String key, Bundle values, DataUtil.Callback<CharSequence> cb) {
                loadValue(cb);
            }
        });
        watcher = new FileItemWatcher(controller, item) {
            @Override
            public void itemChanged(FileSystemItem item) {
                // File has been changed - ask for reload
                final boolean doEdit = Tegmine.EDIT_TYPE_EDIT.equals(form.get(Tegmine.BUNDLE_EDIT_TYPE));
                if (doEdit) {
                    controller.messageLong("Warning: contents were modified outside of editor");
                }
            }
        };
        title.setText(item.details());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (null == controller) { // Ignore auto fragment
            return null;
        }
        final View view = inflater.inflate(R.layout.fragment_editor, container, false);
        editor = view.findViewById(R.id.editor_text);
        editor.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                return keyHandler(i, keyEvent);
            }
        });
        title = view.findViewById(R.id.editor_title_text);
        titleIcon = view.findViewById(R.id.editor_title_icon);
        ImageView saveBtn = view.findViewById(R.id.editor_save);
        saveBtn.setImageResource(controller.theme().saveIcon());
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
            }
        });
        ImageView voiceBtn = view.findViewById(R.id.editor_voice);
        voiceBtn.setImageResource(controller.theme().voiceIcon());
        voiceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                insertVoiceSelected();
            }
        });
        return view;
    }

    private boolean keyHandler(int key, KeyEvent keyEvent) {
        if (null == controller) {
            return false;
        }
        if (keyEvent.getAction() != KeyEvent.ACTION_DOWN) {
            return false;
        }
        if (key == KeyEvent.KEYCODE_TAB) {
            if (!keyEvent.isCtrlPressed() && !keyEvent.isAltPressed()) { // only tab and shift-tab
                shiftIndent(keyEvent.isShiftPressed());
                return true;
            }
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

    public void insert(String text) {
        String input = editor.getText().toString();
        int selStart = editor.getSelectionStart();
        int selFinish = editor.getSelectionEnd();
        String newInput = input.substring(0, selStart)+text+input.substring(selFinish);
        form.set("contents", newInput);
    }

    private void shiftIndent(boolean reverse) {
        String input = editor.getText().toString();
        int selStart = editor.getSelectionStart();
        int selFinish = editor.getSelectionEnd();
        PositionInText start = findString(input, selStart);
        PositionInText finish = findString(input, editor.getSelectionEnd());
        List<String> lines = new ArrayList<>();
        Collections.addAll(lines, input.split("\n"));
        StringBuilder indented = new StringBuilder();
        for (int i = 0; i < controller.spacesInTab(provider); i++) {
            indented.append(' ');
        }
        for (int i = start.lineNo; i <= finish.lineNo && i<lines.size(); i++) {
            if (!reverse) { // Add indent
                lines.set(i, indented.toString() + lines.get(i));
                if (i == start.lineNo) { // First time
                    selStart += controller.spacesInTab(provider);
                }
                selFinish += controller.spacesInTab(provider);
            } else {
                int indent = controller.indent(provider, lines.get(i));
                if (indent > 0) { // Remove indent
                    lines.set(i, lines.get(i).substring(controller.spacesInTab(provider)));
                    if (i == start.lineNo) { // First time
                        selStart -= controller.spacesInTab(provider);
                    }
                    selFinish -= controller.spacesInTab(provider);
                }
            }
        }
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) { // $COMMENT
            if (i>0) {
                output.append('\n');
            }
            output.append(lines.get(i));
        }
        editor.setText(output.toString());
        if (selStart>output.length()) {
            selStart = output.length();
        }
        if (selFinish>output.length()) {
            selFinish = output.length();
        }
        editor.setSelection(selStart, selFinish);
    }

    private void requestFocusFor(View view) {
        view.requestFocus();
        InputMethodManager imm = (InputMethodManager) controller.context().getSystemService(
                Context.INPUT_METHOD_SERVICE);
        if (null != imm) {
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void enableEditorListeners() {
        editor.setFilters(new InputFilter[]{indentFilter});
        editor.addTextChangedListener(coloringWatcher);
    }

    public void saveState(Bundle data) {
        form.save(data);
        logger.d("Saved state:", data);
    }

    public boolean changed() {
        return form.changed();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Tegmine.REQUEST_VOICE && resultCode == Activity.RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results.isEmpty()) {
                return;
            }
            String spokenText = results.get(0);
            if (TextUtils.isEmpty(spokenText)) { // Failure
                return;
            }
            insert(spokenText);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void insertVoiceSelected() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        startActivityForResult(intent, Tegmine.REQUEST_VOICE);
    }

    private void save() {
        if (null == controller) { // ???
            return;
        }
        final CharSequence contents = form.get("contents");
        final boolean doEdit = Tegmine.EDIT_TYPE_EDIT.equals(form.get(Tegmine.BUNDLE_EDIT_TYPE));
        if (!doEdit && TextUtils.isEmpty(contents)) { // No data entered in add mode - nothing to do
            onAfterSave();
            return;
        }
        Tasks.SimpleTask<FileSystemException> task = new Tasks.SimpleTask<FileSystemException>() {
            @Override
            protected FileSystemException doInBackground() {
                OutputStream stream;
                try {
                    if (doEdit) { // Replace
                        stream = provider.replace(item);
                    } else {
                        stream = provider.append(item);
                    }
                    controller.writeEdited(provider, stream, contents.toString().trim(), doEdit);
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

    private void text2Editor(SpannableStringBuilder text, int cursor) {
        form.set("contents", text);
        if (cursor <= editor.getText().length()) {
            editor.setSelection(cursor);
        } else {
            editor.setSelection(editor.getText().length());
        }
    }

    private TegmineController.TemplateApplyResult applyTemplate(TemplateDef tmpl, SpannableStringBuilder buffer) {
        if (null == tmpl)
            return null;
        TegmineController.TemplateApplyResult applyResult = controller.applyTemplate(provider, (String) form.get("contents"), tmpl);
        List<LineMeta> lines = new ArrayList<>();
        controller.split(provider, lines, applyResult.value());
        logger.d("applyTemplate:", lines);
        controller.linesForEditor(provider, lines, buffer, syntax);
        return applyResult;
    }

    private void applyTemplate(TemplateDef tmpl) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        TegmineController.TemplateApplyResult applyResult = applyTemplate(tmpl, builder);
        if (null != applyResult) {
            text2Editor(builder, applyResult.cursor());
        }
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
