package kvj.tegmine.android.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.kvj.bravo7.log.Logger;
import org.kvj.bravo7.util.Tasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import kvj.tegmine.android.R;
import kvj.tegmine.android.data.TegmineController;
import kvj.tegmine.android.data.def.FileSystemException;
import kvj.tegmine.android.data.def.FileSystemItem;
import kvj.tegmine.android.data.def.FileSystemProvider;
import kvj.tegmine.android.data.model.LineMeta;
import kvj.tegmine.android.data.model.SyntaxDef;
import kvj.tegmine.android.data.model.util.Wrappers;

/**
 * Created by kvorobyev on 2/17/15.
 */
public abstract class OneFileAdapter extends RecyclerView.Adapter<OneFileAdapter.Holder> {

    private final FileSystemProvider provider;
    private final int lastLineMargin;

    class Holder extends RecyclerView.ViewHolder implements View.OnClickListener,
                                                            View.OnLongClickListener,
                                                            View.OnCreateContextMenuListener {

        private final View border;
        private final TextView text;
        private final TextView lineno;

        public Holder(View view) {
            super(view);
            border = view.findViewById(R.id.one_file_item_border);
            text = (TextView) view.findViewById(R.id.one_file_item_text);
            lineno = (TextView) view.findViewById(R.id.one_file_item_lineno);
            view.setOnClickListener(this);
            view.setOnLongClickListener(this);
            view.setOnCreateContextMenuListener(this);
            border.setBackgroundColor(controller.theme().markColor());
            text.setTextSize(TypedValue.COMPLEX_UNIT_SP, controller.theme().fileTextSp());
            text.setTextColor(controller.theme().textColor());
            lineno.setTextSize(TypedValue.COMPLEX_UNIT_SP, controller.theme().fileTextSp());
            lineno.setTextColor(controller.theme().altTextColor());
            lineno.setTextSize(TypedValue.COMPLEX_UNIT_SP, controller.theme().fileTextSp());
        }

        @Override
        public void onClick(View view) {
            OneFileAdapter.this.onClick(getAdapterPosition());
        }

        @Override
        public boolean onLongClick(View view) {
            return false;
        }

        @Override
        public void onCreateContextMenu(ContextMenu contextMenu, View view,
                                        ContextMenu.ContextMenuInfo contextMenuInfo) {
            OneFileAdapter.this.onContextMenu(contextMenu, getAdapterPosition());
        }
    }

    protected abstract void onClick(int position);
    protected abstract void onContextMenu(ContextMenu menu, int position);

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
    private final LinkedList<Integer> visibleLines = new LinkedList<>();
    private int fromLine = 0;

    public OneFileAdapter(TegmineController controller, FileSystemItem item) {
        this.controller = controller;
        this.item = item;
        this.syntax = controller.findSyntax(item);
        this.provider = controller.fileSystemProvider(item);
        this.lastLineMargin = (int) controller.context().getResources().getDimension(R.dimen.last_line_margin);
    }

    public void setBounds(final int offset, final int linesCount, final Runnable afterDone) {
        synchronized (lock) {
            state = DataState.FrameRequested;
            Tasks.SimpleTask<Boolean> task = new Tasks.SimpleTask<Boolean>() {
                @Override
                protected Boolean doInBackground() {
                    lines.clear(); // From start
                    try {
//                        logger.d("Will load from:", offset, linesCount, syntax);
                        controller.loadFilePart(lines, item, syntax, offset, linesCount);
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
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_one_file, parent,
                                                                  false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
        LineMeta line = line(position);
        boolean lastLine = position == visibleLines.size()-1;
        holder.itemView.setPadding(0, 0, 0, lastLine? lastLineMargin: 0);
        if (null == line) { // Invalid line
            holder.text.setText("");
        } else {
            holder.border.setVisibility(line.folded()? View.VISIBLE: View.GONE);
            int indent = line.indent();
            SpannableStringBuilder builder = new SpannableStringBuilder();
            controller.applyTheme(provider, syntax, line.data(), builder, SyntaxDef.Feature.Shrink);
            holder.text.setText(builder);
            int leftIndent = (int) controller.sp2px(indent * controller.theme().fileIndentSp());
            holder.text.setPadding(leftIndent, 0, 0, 0);
            if (wrapLines) {
                holder.text.setSingleLine(false);
                holder.text.setEllipsize(TextUtils.TruncateAt.MIDDLE);
            } else {
                holder.text.setSingleLine(true);
            }
            if (showNumbers) {
                holder.lineno.setText(String.format(showNumbersFormat, visibleLines.get(position)+1));
                holder.lineno.setVisibility(View.VISIBLE);
            } else {
                holder.lineno.setVisibility(View.GONE);
            }
        }

    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return visibleLines.size();
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

    public Collection<Wrappers.Pair<String>> features(int position, String... features) {
        List<Wrappers.Pair<String>> result = new ArrayList<>();
        Set<String> featuresSet = new HashSet<>();
        Collections.addAll(featuresSet, features);
        if (null == syntax || featuresSet.isEmpty()) {
            return result;
        }
        SyntaxDef.SyntaxedStringBuilder syntaxed = controller.syntaxize(syntax, line(position));
        Collection<Wrappers.Pair<String>> pairs = syntaxed.allFeatures(-1);// All from line
        for (Wrappers.Pair<String> pair : pairs) {
            if (featuresSet.contains(pair.v1())) {
                result.add(pair);
            }
        }
        return result;
    }

    public String partString(int position) {
        if (position >= visibleLines.size() || position<0) { // Invalid position
            return null;
        }
        return controller.part(provider, lines, visibleLines.get(position)).toString();
    }

    public void toggle(int position) {
        synchronized (lock) {
            LineMeta startLine = line(position);
            if (-1 == startLine.indent()) { // No toggle possible
                return;
            }
            startLine.folded(!startLine.folded());
            int index = visibleLines.get(position)+1;
            boolean insert = false;
            int lastIndex = lines.size(); // Last line
            int folded = 0;
            List<Integer> lineNos = new ArrayList<>();
            while (index < lines.size()) {
                LineMeta line = lines.get(index);
                if (line.indent()>startLine.indent() || line.indent() == -1) { // Fold
                    folded++;
                    line.visible(!startLine.folded());
                    line.folded(false);
                    lineNos.add(index);
                } else {
                    insert = true;
                    lastIndex = index;
                    break;
                }
                index++;
            }
            notifyItemChanged(position);
            if (folded == 0) {
                // Nothing to fold
                startLine.folded(false);
                return;
            }
            if (startLine.folded()) {
                // Remove
                while (position + 1 < visibleLines.size() && visibleLines.get(position + 1) < lastIndex) {
                    visibleLines.remove(position + 1);
                    notifyItemRemoved(position + 1);
                }
            } else {
                // Add
                if (insert) {
                    visibleLines.addAll(position+1, lineNos);
                } else {
                    visibleLines.addAll(lineNos);
                }
                notifyItemRangeInserted(position+1, lineNos.size());
            }
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
