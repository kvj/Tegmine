package kvj.tegmine.android.ui.adapter;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.kvj.bravo7.log.Logger;
import org.kvj.bravo7.util.Tasks;

import java.util.ArrayList;
import java.util.List;

import kvj.tegmine.android.R;
import kvj.tegmine.android.Tegmine;
import kvj.tegmine.android.data.TegmineController;
import kvj.tegmine.android.data.def.FileSystemException;
import kvj.tegmine.android.data.def.FileSystemItem;
import kvj.tegmine.android.data.model.LineMeta;
import kvj.tegmine.android.data.model.SyntaxDef;

/**
 * Created by kvorobyev on 2/17/15.
 */
public class OneFileAdapter extends BaseAdapter {

    private static final int MAX_LINES = 30;
    private final SyntaxDef syntax;
    private boolean showNumbers = false;
    private boolean wrapLines = false;
    private String showNumbersFormat = "%d";

    public void showNumbers(boolean showNumbers) {
        this.showNumbers = showNumbers;
    }

    public void wrapLines(boolean wrapLines) {
        this.wrapLines = wrapLines;
    }

    private enum DataState {FrameLoaded, FrameRequested};

    private final Object lock = new Object();
    private DataState state = DataState.FrameRequested;
    private static Logger logger = Logger.forClass(OneFileAdapter.class);

    private final TegmineController controller;
    private final FileSystemItem item;

    private final List<LineMeta> lines = new ArrayList<>(MAX_LINES);
    private final List<Integer> visibleLines = new ArrayList<>();
    private int fromLine = 0;

    public OneFileAdapter(TegmineController controller, FileSystemItem item) {
        this.controller = controller;
        this.item = item;
        this.syntax = controller.findSyntax(item);
    }

    public void setBounds(final int offset, final int linesCount, final Runnable afterDone) {
        synchronized (lock) {
            state = DataState.FrameRequested;
            Tasks.SimpleTask<Boolean> task = new Tasks.SimpleTask<Boolean>() {
                @Override
                protected Boolean doInBackground() {
                    lines.clear(); // From start
                    try {
                        logger.d("Will load from:", offset, linesCount);
                        controller.loadFilePart(lines, item, offset, linesCount);
                        int linesTotal = lines.size();
                        int digits = 1;
                        while (linesTotal >= 10) {
                            linesTotal /= 10;
                            digits++;
                        }
                        showNumbersFormat = String.format("%% %dd", digits);
                        return true;
                    } catch (FileSystemException e) {
                        logger.d(e, "Failed to read file");
                        return false;
                    }
                }

                @Override
                protected void onPostExecute(Boolean result) {
                    if (!result) {
                        return;
                    }
                    synchronized (lock) {
                        logger.d("Loaded:", offset, lines.size());
                        state = DataState.FrameLoaded;
                        updateVisible();
                    }
                    if (null != afterDone) {
                        afterDone.run();
                    }
                }
            };
            task.exec();
        }
    }


    @Override
    public int getCount() {
        return visibleLines.size();
    }

    @Override
    public String getItem(int position) {
        return line(position).data();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public LineMeta line(final int position) {
        synchronized (lock) {
            if (state == DataState.FrameLoaded) { // Only in this case we can get it
                return lines.get(visibleLines.get(position));
            } else {
                return null; // No line yet
            }
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
//        logger.d("Render line:", position);
        TextView text = null;
        TextView lineno = null;
        View border = null;
        if (null == convertView) { // inflate
            LayoutInflater inflater = (LayoutInflater) parent.getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.item_one_file, parent, false);
            border = convertView.findViewById(R.id.one_file_item_border);
            border.setBackgroundColor(controller.theme().markColor());
            text = (TextView) convertView.findViewById(R.id.one_file_item_text);
            lineno = (TextView) convertView.findViewById(R.id.one_file_item_lineno);
            text.setTextSize(TypedValue.COMPLEX_UNIT_SP, controller.theme().fileTextSp());
            text.setTextColor(controller.theme().textColor());
            lineno.setTextSize(TypedValue.COMPLEX_UNIT_SP, controller.theme().fileTextSp());
            lineno.setTextColor(controller.theme().markColor());
            lineno.setTextSize(TypedValue.COMPLEX_UNIT_SP, controller.theme().fileTextSp());
        } else {
            border = convertView.findViewById(R.id.one_file_item_border);
            text = (TextView) convertView.findViewById(R.id.one_file_item_text);
            lineno = (TextView) convertView.findViewById(R.id.one_file_item_lineno);
        }
        LineMeta line = line(position);
        if (null == line) { // Invalid line
            text.setText("");
        } else {
            border.setVisibility(line.folded()? View.VISIBLE: View.GONE);
            int indent = line.indent();
            SyntaxDef.SyntaxedStringBuilder syntaxedBuilder = new SyntaxDef.SyntaxedStringBuilder(line.data());
            SpannableStringBuilder builder = new SpannableStringBuilder();
            controller.applyTheme(syntax, line, builder);
            text.setText(builder);
            int leftIndent = (int) Tegmine.getInstance().sp2px(indent * controller.theme().fileIndentSp());
            text.setPadding(leftIndent, 0, 0, 0);
            if (wrapLines) {
                text.setSingleLine(false);
                text.setEllipsize(TextUtils.TruncateAt.MIDDLE);
            } else {
                text.setSingleLine(true);
            }
            if (showNumbers) {
                lineno.setText(String.format(showNumbersFormat, visibleLines.get(position)+1));
                lineno.setVisibility(View.VISIBLE);
            } else {
                lineno.setVisibility(View.GONE);
            }
        }
        return convertView;
    }

    public String partString(int position) {
        if (position >= visibleLines.size() || position<0) { // Invalid position
            return null;
        }
        return controller.part(lines, visibleLines.get(position)).toString();
    }

    public void toggle(int position) {
        synchronized (lock) {
            LineMeta startLine = line(position);
            if (-1 == startLine.indent()) { // No toggle possible
                return;
            }
            startLine.folded(!startLine.folded());
            int index = visibleLines.get(position)+1;
            int folded = 0;
            while (index < lines.size()) {
                LineMeta line = lines.get(index);
                if (line.indent()>startLine.indent() || line.indent() == -1) { // Fold
                    folded++;
                    line.visible(!startLine.folded());
                    if (line.visible() && line.folded()) { // Has been folded before
                        line.folded(false); // Unfold - too hard to skip at this moment
                    }
                } else {
                    break;
                }
                index++;
            }
            if (folded == 0) {
                // Nothing to fold
                startLine.folded(false);
            }
            updateVisible();
        }
    }

    private void updateVisible() {
        synchronized (lock) {
            visibleLines.clear();
            for (int i = 0; i < lines.size(); i++) { // Store indexes of visible lines
                if (lines.get(i).visible()) { // Visible line
                    visibleLines.add(i);
                }
            }
            notifyDataSetChanged();
        }
    }

}
