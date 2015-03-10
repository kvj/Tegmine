package kvj.tegmine.android.ui.adapter;

import android.content.Context;
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

/**
 * Created by kvorobyev on 2/17/15.
 */
public class OneFileAdapter extends BaseAdapter {

    private static final int MAX_LINES = 30;

    private enum DataState {FrameLoaded, FrameRequested};

    private final Object lock = new Object();
    private DataState state = DataState.FrameRequested;
    private static Logger logger = Logger.forClass(OneFileAdapter.class);

    private final TegmineController controller;
    private final FileSystemItem item;

    private final List<String> lines = new ArrayList<>(MAX_LINES);
    private int fromLine = 0;

    public OneFileAdapter(TegmineController controller, FileSystemItem item) {
        this.controller = controller;
        this.item = item;
    }

    public void setBounds(final int offset, final int linesCount) {
        synchronized (lock) {
            state = DataState.FrameRequested;
            Tasks.SimpleTask<Boolean> task = new Tasks.SimpleTask<Boolean>() {
                @Override
                protected Boolean doInBackground() {
                    lines.clear(); // From start
                    try {
                        logger.d("Will load from:", offset, linesCount);
                        controller.loadFilePart(lines, item, offset, linesCount);
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
                        notifyDataSetChanged();
                    }
                }
            };
            task.exec();
        }
    }


    @Override
    public int getCount() {
        synchronized (lock) {
            return lines.size();
        }
    }

    @Override
    public String getItem(int position) {
        return lines.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    String line(final int position) {
        synchronized (lock) {
            if (state == DataState.FrameLoaded) { // Only in this case we can get it
                return lines.get(position);
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
        if (null == convertView) { // inflate
            LayoutInflater inflater = (LayoutInflater) parent.getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.item_one_file, parent, false);
            text = (TextView) convertView.findViewById(R.id.one_file_item_text);
            lineno = (TextView) convertView.findViewById(R.id.one_file_item_lineno);
            text.setTextSize(TypedValue.COMPLEX_UNIT_SP, controller.theme().fileTextSp());
            text.setTextColor(controller.theme().textColor());
            lineno.setTextSize(TypedValue.COMPLEX_UNIT_SP, controller.theme().fileTextSp());
            lineno.setVisibility(View.GONE);
            text.setSingleLine(false);
        } else {
            text = (TextView) convertView.findViewById(R.id.one_file_item_text);
            lineno = (TextView) convertView.findViewById(R.id.one_file_item_lineno);
        }
        String line = line(position);
        if (null == line) { // Invalid line
            text.setText("");
        } else {
            int indent = controller.indent(line);
            text.setText(line.trim());
            int leftIndent = (int) Tegmine.getInstance().sp2px(indent * controller.theme().fileIndentSp());
            text.setPadding(leftIndent, 0, 0, 0);
        }
        lineno.setText(String.format("% 4d", position+1));
        return convertView;
    }

    public String partString(int position) {
        if (position >= lines.size()) { // Invalid position
            return null;
        }
        return controller.part(lines, position).toString();
    }
}
