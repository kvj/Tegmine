package kvj.tegmine.android.ui.fragment;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
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
public class OneFileViewer extends Fragment {

    private ListView listView = null;

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
        listView = (ListView) view.findViewById(android.R.id.list);
        registerForContextMenu(listView);
        adapter = new OneFileAdapter(controller, item);
        listView.setAdapter(adapter);
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                onLongItemClick(position);
                return true;
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onListItemClick(position);
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

    private boolean buttonsDimmed = false;

    private void changeButtonsDim(View buttonsPane, ListView listView) {
        boolean dimButtons = listView.getCount() == listView.getLastVisiblePosition()+1;
//        logger.d("Scroll state:", listView.getCount(), listView.getFirstVisiblePosition(), listView.getLastVisiblePosition(), dimButtons);
        if (dimButtons != buttonsDimmed) { // State changed
            ObjectAnimator anim = ObjectAnimator.ofFloat(buttonsPane, "alpha", dimButtons ? 1f : 0.4f, dimButtons ? 0.4f : 1f);
            anim.setDuration(300);
            anim.start();
            buttonsDimmed = dimButtons;
        }
    }

    public void onListItemClick(int position) {
        if (null != adapter) { // Toggle folding
            adapter.toggle(position);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (null != adapter && v == listView) { // OK to show
            logger.d("Listview menu:", menuInfo);
            menu.clear();
            getActivity().getMenuInflater().inflate(R.menu.context_one_file, menu);
            final String contents = adapter.partString(listView.getSelectedItemPosition());
            logger.d("Show menu for:", listView.getSelectedItemPosition(), contents);
            if (TextUtils.isEmpty(contents)) { // No data
                return;
            }
            menu.findItem(R.id.context_copy_cboard).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    // Copy here
                    return true;
                }
            });
            menu.findItem(R.id.context_share).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, contents);
                    sendIntent.setType("text/plain");
                    startActivity(sendIntent);
                    return true;
                }
            });
        }
    }

    private void onLongItemClick(int position) {
//        listView.showContextMenu();
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
