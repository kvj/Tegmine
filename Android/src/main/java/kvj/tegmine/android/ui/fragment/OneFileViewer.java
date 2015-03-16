package kvj.tegmine.android.ui.fragment;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import org.kvj.bravo7.SuperActivity;
import org.kvj.bravo7.form.FormController;
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
import kvj.tegmine.android.ui.adapter.OneFileAdapter;
import kvj.tegmine.android.ui.form.FileSystemItemWidgetAdapter;

/**
 * Created by kvorobyev on 2/16/15.
 */
public class OneFileViewer extends ListFragment {

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

    public OneFileViewer() {
        super();
    }

    public OneFileViewer setListener(FileViewerListener listener) {
        this.listener = listener;
        return this;
    }

    public OneFileViewer create(final TegmineController controller, Bundle bundle) {
        this.controller = controller;
        form.add(new FileSystemItemWidgetAdapter(controller, null), "select");
        form.add(new FileSystemItemWidgetAdapter(controller, null), "root");
        form.load(bundle);
        item = form.getValue("select", FileSystemItem.class);
        logger.d("new FileViewer:", item, bundle);
        if (null == item) {
            SuperActivity.notifyUser(Tegmine.getInstance(), "File not found");
            return null;
        }
        adapter = new OneFileAdapter(controller, item);
        setListAdapter(adapter);
        return this;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (null == controller) { // Invalid fragment
            return null;
        }
        logger.d("Will create view", controller);
        View view = inflater.inflate(R.layout.fragment_one_file, container, false);
        titleText = (TextView) view.findViewById(R.id.one_file_title_text);
        titleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, controller.theme().headerTextSp());
        titleText.setTextColor(controller.theme().textColor());
        titleText.setText(item.details());
        view.findViewById(R.id.one_file_do_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startEditor(Tegmine.EDIT_TYPE_ADD);
            }
        });
        view.findViewById(R.id.one_file_do_edit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startEditor(Tegmine.EDIT_TYPE_EDIT);
            }
        });
        final ListView listView = (ListView) view.findViewById(android.R.id.list);
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                onLongItemClick(position);
                return true;
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

    private void changeButtonsDim(View buttonsPane, ListView listView) {
        boolean dimButtons = listView.getCount() == listView.getLastVisiblePosition()+1;
//        logger.d("Scroll state:", listView.getCount(), listView.getFirstVisiblePosition(), listView.getLastVisiblePosition(), dimButtons);
        ObjectAnimator anim = ObjectAnimator.ofFloat(buttonsPane, "alpha", dimButtons? 1f: 0.4f, dimButtons? 0.4f: 1f);
        anim.setDuration(300);
        anim.start();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (null != adapter) { // Toggle folding
            adapter.toggle(position);
        }
    }

    private void onLongItemClick(int position) {
        if (null != adapter) { // OK
            String contents = adapter.partString(position);
            if (TextUtils.isEmpty(contents)) { // No data
                return;
            }
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, contents);
            sendIntent.setType("text/plain");
            startActivity(sendIntent);
            logger.d("Sharing text:", contents);
        }
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
        form.save(outState, "root", "select");
    }

    private void startEditor(String editType) {
        Bundle bundle = new Bundle();
        bundle.putString(Tegmine.BUNDLE_EDIT_TYPE, editType);
        form.save(bundle, "root", "select");
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
}
