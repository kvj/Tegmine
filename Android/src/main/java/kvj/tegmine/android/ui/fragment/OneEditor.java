package kvj.tegmine.android.ui.fragment;

import android.content.Context;
import android.os.Bundle;
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
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import org.kvj.bravo7.SuperActivity;
import org.kvj.bravo7.log.Logger;
import org.kvj.bravo7.ng.App;
import org.kvj.bravo7.util.Listeners;
import org.kvj.bravo7.util.Tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import kvj.tegmine.android.R;
import kvj.tegmine.android.data.TegmineController;
import kvj.tegmine.android.data.def.FileSystemException;
import kvj.tegmine.android.data.def.FileSystemItem;
import kvj.tegmine.android.data.model.EditorInfo;
import kvj.tegmine.android.data.model.FileItemWatcher;
import kvj.tegmine.android.data.model.LineMeta;
import kvj.tegmine.android.data.model.ProgressListener;
import kvj.tegmine.android.data.model.SyntaxDef;
import kvj.tegmine.android.data.model.TemplateDef;

/**
 * Created by kvorobyev on 5/5/15.
 */
public class OneEditor extends Fragment implements ProgressListener {

    private EditorInfo info = null;

    private TegmineController controller = App.controller();
    private EditText editor = null;
    private TextView title = null;
    private FileSystemItem item = null;
    private SyntaxDef syntax = null;
    private FileItemWatcher watcher = null;
    private ImageView titleIcon = null;

    private Logger logger = Logger.forInstance(this);
    private int index = -1;
    private InputMethodManager imm = null;

    public OneEditor() {
        super();
        setRetainInstance(false);
    }

    public OneEditor create(EditorInfo info, int index) {
        this.info = info;
        this.index = index;
        return this;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (null == info) { // Invalid
            return null;
        }
        if (info.mode == EditorInfo.Mode.None) { // Not loaded yet
            return null;
        }
        item = controller.fromURL(info.itemURL);
        if (null == item) { // Invalid
            return null;
        }
        imm = (InputMethodManager) controller.context().getSystemService(
            Context.INPUT_METHOD_SERVICE);
        info.view = this;
        View view = inflater.inflate(R.layout.fragment_one_editor, container, false);
        editor = (EditText) view.findViewById(R.id.editor_text);
        title = (TextView) view.findViewById(R.id.editor_title_text);
        editor.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                return keyHandler(i, keyEvent);
            }
        });
        this.syntax = controller.findSyntax(item);
        title.setText(item.details());
        titleIcon = (ImageView) view.findViewById(R.id.editor_title_icon);
        if (null == info.text) { // First time - load text
            loadContents();
        } else {
            info2Editor();
            enableEditorListeners();
            appendTemplate();
        }
        watcher = new FileItemWatcher(controller, item) {
            @Override
            public void itemChanged(FileSystemItem item) {
                // File has been changed - ask for reload
                item.commit(); // Change detected
                final boolean doEdit = info.mode == EditorInfo.Mode.Edit;
                if (doEdit) {
                    // Makes sense - can ask for refresh
                    SuperActivity.showQuestionDialog(getActivity(), null, "File has been changed. Reload?",
                         new Runnable() {
                             @Override
                             public void run() {
                                 loadContents();
                             }
                         }, new Runnable() {
                            @Override
                            public void run() {
                            }
                        });
                }
            }
        };
        return view;
    }

    void info2Editor() {
        // Load text from info and set cursor
        final SpannableStringBuilder builder = new SpannableStringBuilder();
        Tasks.SimpleTask<Void> task = new Tasks.SimpleTask<Void>() {
            @Override
            protected Void doInBackground() {
                List<LineMeta> lines = new ArrayList<>();
                controller.split(lines, info.text);
                controller.linesForEditor(lines, builder, syntax);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                text2Editor(builder, info.selectionStart, info.selectionEnd);
            }
        };
        task.exec();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        toInfo();
        super.onSaveInstanceState(outState);
    }

    private boolean keyHandler(final int key, final KeyEvent keyEvent) {
        if (keyEvent.getAction() != KeyEvent.ACTION_DOWN) {
            return false;
        }
        if (key == KeyEvent.KEYCODE_TAB) {
            if (!keyEvent.isCtrlPressed() && !keyEvent.isAltPressed()) { // only tab and shift-tab
                shiftIndent(keyEvent.isShiftPressed());
                return true;
            }
        }
        if (keyEvent.isCtrlPressed()) {
            // Ctrl + template key
            TemplateDef tmpl = controller.templateFromKeyEvent(keyEvent);
            if (null != tmpl) {
                applyTemplate(tmpl);
                return true;
            }
        }
        return !controller.editors().keyListeners.emit(new Listeners.ListenerEmitter<View.OnKeyListener>() {
            @Override
            public boolean emit(View.OnKeyListener listener) {
                return listener.onKey(editor, key, keyEvent);
            }
        });
    }

    public void toInfo() {
        if (null != info) {
            info.fromEditor(editor);
        }
    }

    public FileSystemItem item() {
        return item;
    }

    public EditorInfo info() {
        return info;
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

    private void shiftIndent(boolean reverse) {
        String input = editor.getText().toString();
        int selStart = editor.getSelectionStart();
        int selFinish = editor.getSelectionEnd();
        PositionInText start = findString(input, selStart);
        PositionInText finish = findString(input, editor.getSelectionEnd());
        List<String> lines = new ArrayList<>();
        Collections.addAll(lines, input.split("\n"));
        StringBuilder indented = new StringBuilder();
        for (int i = 0; i < controller.spacesInTab(); i++) {
            indented.append(' ');
        }
        for (int i = start.lineNo; i <= finish.lineNo && i<lines.size(); i++) {
            if (!reverse) { // Add indent
                lines.set(i, indented.toString() + lines.get(i));
                if (i == start.lineNo) { // First time
                    selStart += controller.spacesInTab();
                }
                selFinish += controller.spacesInTab();
            } else {
                int indent = controller.indent(lines.get(i));
                if (indent > 0) { // Remove indent
                    lines.set(i, lines.get(i).substring(controller.spacesInTab()));
                    if (i == start.lineNo) { // First time
                        selStart -= controller.spacesInTab();
                    }
                    selFinish -= controller.spacesInTab();
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

    private void enableEditorListeners() {
        editor.setFilters(new InputFilter[]{indentFilter});
        editor.addTextChangedListener(coloringWatcher);
    }

    private void loadContents() {
        final SpannableStringBuilder buffer = new SpannableStringBuilder();
        Tasks.SimpleTask<FileSystemException> task = new Tasks.SimpleTask<FileSystemException>() {
            @Override
            protected FileSystemException doInBackground() {
                try {
                    if (info.mode == EditorInfo.Mode.Append) { // No load
                        return null;
                    }
                    List<LineMeta> lines = new ArrayList<>();
                    controller.loadFilePart(lines, item, 0, -1);
                    controller.linesForEditor(lines, buffer, syntax);
                    return null;
                } catch (FileSystemException e) {
                    return e;
                }
            }

            @Override
            protected void onPostExecute(FileSystemException e) {
                logger.d("Loaded file contents", e, buffer.length());
                if (null == e) { // Success
                    info.crc = EditorInfo.hash(buffer.toString()); // Save
                    text2Editor(buffer, buffer.length(), -1);
                    enableEditorListeners();
                    appendTemplate();
                } else {
                    logger.e(e, "Failed to load file contents");
                }
            }
        };
        task.exec();

    }

    public void appendTemplate() {
        applyTemplate(controller.templates().get(info.template));
        info.template = null; // Only once
    }

    private void text2Editor(SpannableStringBuilder text, int selectionStart, int selectionEnd) {
        editor.setText(text);
        if (selectionStart <= editor.getText().length() && selectionStart >= 0) {
            int selEnd = selectionStart;
            if (selectionEnd > selectionStart && selectionEnd <= editor.getText().length()) {
                selEnd = selectionEnd;
            }
            editor.setSelection(selectionStart, selEnd);
        } else {
            editor.setSelection(editor.getText().length());
        }
    }

    void applyTemplate(TemplateDef tmpl) {
        TegmineController.TemplateApplyResult applyResult = controller.applyTemplate(editor.getText().toString(), tmpl);
        toInfo();
        if (!TextUtils.isEmpty(info.text) && !info.text.endsWith("\n")) { // No new line at bottom
            info.text += "\n";
        }
        int len = info.text.length();
        info.text += applyResult.value();
        info.selectionStart = len + applyResult.cursor();
        info.selectionEnd = -1;
        info2Editor();
    }

    private void applyTheme() {
        if (null == info) {
            return;
        }
        boolean doEdit = info.mode == EditorInfo.Mode.Edit;
        editor.setTextColor(controller.theme().textColor());
        editor.setTextSize(TypedValue.COMPLEX_UNIT_SP, controller.theme().editorTextSp());
        controller.applyHeaderStyle(title);
        titleIcon.setImageResource(doEdit ?
                        controller.theme().fileEditIcon() :
                        controller.theme().fileAddIcon()
        );
    }

    @Override
    public void onDestroy() {
        if (null != info) { // Break the link
            info.view = null;
        }
        super.onDestroy();
    }

    private static class ChangeMark {
        private final int start;
        private final int count;

        private ChangeMark(int start, int count) {
            this.start = start;
            this.count = count;
        }
    }

    private InputFilter indentFilter = new InputFilter() {
        @Override
        public CharSequence filter(CharSequence source, int start, int end,
                                   Spanned spanned, int dstart, int dend) {
//        logger.d("Filter:", source, start, end, dstart, dend);
            if (controller == null) return null;
            if (source.length() == 1 && source.charAt(0) == '\n') {
                // New line
                String line = findString(spanned.toString(), dstart).line;
                int indent = controller.indent(line);
                String sign = controller.signInLine(line);
//            logger.d("Indent:", line, indent, sign);
                StringBuilder builder = new StringBuilder(source);
                controller.addIndent(builder, indent);
                if (null != sign) {
                    builder.append(sign);
                    builder.append(' ');
                }
                return builder;
            }
            return null;
        }
    };

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
                controller.applyTheme(syntax, line, lineBuilder);
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
    public void onResume() {
        super.onResume();
        controller.progressListeners().add(this);
        applyTheme();
        if (null != watcher) watcher.active(true);
        if (null != editor) {
            editor.requestFocus();
            if (null != imm) imm.showSoftInput(editor, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    @Override
    public void onPause() {
        if (null != watcher) watcher.active(false);
        controller.progressListeners().remove(this);
        toInfo();
        super.onPause();
    }
}
