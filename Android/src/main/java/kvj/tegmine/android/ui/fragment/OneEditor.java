package kvj.tegmine.android.ui.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.SpannableStringBuilder;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import org.kvj.bravo7.log.Logger;
import org.kvj.bravo7.ng.App;
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
import kvj.tegmine.android.data.model.SyntaxDef;
import kvj.tegmine.android.data.model.TemplateDef;

/**
 * Created by kvorobyev on 5/5/15.
 */
public class OneEditor extends Fragment {

    private static final String BUNDLE_INDEX = "tab_index";
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
            loadContents(editor);
        } else {
            info2Editor();
        }
        applyTheme();
        /*
        watcher = new FileItemWatcher(controller, item) {
            @Override
            public void itemChanged(FileSystemItem item) {
                // File has been changed - ask for reload
                item.commit(); // Change detected
                final boolean doEdit = info.mode == EditorInfo.Mode.Edit;
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
        */
        return view;
    }

    private void info2Editor() {
        // Load text from info and set cursor
        List<LineMeta> lines = new ArrayList<>();
        controller.split(lines, info.text);
        SpannableStringBuilder builder = new SpannableStringBuilder();
        controller.linesForEditor(lines, builder, syntax);
        text2Editor(builder, info.cursor);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (null != info) { // Save to info
            info.fromEditor(editor);
            outState.putInt(BUNDLE_INDEX, index);
        }
        super.onSaveInstanceState(outState);
    }

    private boolean keyHandler(int key, KeyEvent keyEvent) {
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
        return false;
    }

    public void toInfo() {
        info.fromEditor(editor);
    }

    public FileSystemItem item() {
        return item;
    }

    public EditorInfo info() {
        return info;
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
    }

    private void loadContents(final EditText editor) {
        final SpannableStringBuilder buffer = new SpannableStringBuilder();
        if (info.mode == EditorInfo.Mode.Append) { //
            TemplateDef tmpl = controller.templates().get(info.template);
            if (null != tmpl) {
                applyTemplate(tmpl);
            } else {
                info.text = ""; // Just empty
            }
            info.crc = EditorInfo.hash(info.text); // Save
            info2Editor(); // Parse
            enableEditorListeners();
            return;
        }
        Tasks.SimpleTask<FileSystemException> task = new Tasks.SimpleTask<FileSystemException>() {
            @Override
            protected FileSystemException doInBackground() {
                try {
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
                    text2Editor(buffer, buffer.length());
                    enableEditorListeners();
                } else {
                    logger.e(e, "Failed to load file contents");
                }
            }
        };
        task.exec();

    }

    private void text2Editor(SpannableStringBuilder text, int cursor) {
        editor.setText(text);
        if (cursor <= editor.getText().length() && cursor >= 0) {
            editor.setSelection(cursor);
        } else {
            editor.setSelection(editor.getText().length());
        }
    }

    private void applyTemplate(TemplateDef tmpl) {
        TegmineController.TemplateApplyResult applyResult = controller.applyTemplate(editor.getText().toString(), tmpl);
        info.text = applyResult.value();
        info.cursor = applyResult.cursor();
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
    public void onDetach() {
        if (null != info) { // Save to info
            toInfo();
            info.view = null;
        }
        super.onDetach();
    }

    @Override
    public void onDestroy() {
        if (null != info) { // Save to info
            toInfo();
            info.view = null;
        }
        super.onDestroy();
    }
}
