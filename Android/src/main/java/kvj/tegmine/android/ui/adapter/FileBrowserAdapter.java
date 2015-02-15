package kvj.tegmine.android.ui.adapter;

import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.kvj.bravo7.adapter.AnotherListAdapter;
import org.kvj.bravo7.log.Logger;
import org.kvj.bravo7.util.Tasks;

import java.util.ArrayList;
import java.util.List;

import kvj.tegmine.android.R;
import kvj.tegmine.android.Tegmine;
import kvj.tegmine.android.data.TegmineController;
import kvj.tegmine.android.data.def.FileSystemException;
import kvj.tegmine.android.data.def.FileSystemItem;
import kvj.tegmine.android.data.def.FileSystemItemType;

/**
 * Created by kvorobyev on 2/15/15.
 */
public class FileBrowserAdapter extends AnotherListAdapter<FileBrowserAdapter.Item> {

    class Item {
        boolean expanded = false;
        FileSystemItem<?> item = null;
        Item parent = null;
        int level = 0;
    }

    private final TegmineController controller;
    private static Logger logger = Logger.forClass(FileBrowserAdapter.class);
    private Item root = new Item();

    public FileBrowserAdapter(TegmineController controller, FileSystemItem rootItem) {
        super(new ArrayList<Item>(), R.layout.item_file_browser);
        root.item = rootItem;
        this.controller = controller;
    }

    public void expandTo(FileSystemItem target) {
        List<FileSystemItem> parents = new ArrayList<>();
        if (null != target) { // Have item
            FileSystemItem<FileSystemItem> item = target;
            while (item != null) {
                parents.add(item);
                item = item.parent;
            }
        }
        expand(root, parents);
    }

    @Override
    public void customize(View view, int position) {
        Item item = getItem(position);
        TextView text = (TextView) view.findViewById(R.id.file_browser_item_text);
        text.setText(item.item.name);
        text.setTextColor(controller.theme().textColor());
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, controller.theme().browserTextSp());
        ImageView image = (ImageView) view.findViewById(R.id.file_browser_item_icon);
        image.setImageResource(item.item.type == FileSystemItemType.Folder?
                controller.theme().folderIcon():
                controller.theme().fileIcon());
        ViewGroup group = (ViewGroup) view.findViewById(R.id.file_browser_item);
        int leftPadding = 0;
        if (item.level > 1) { // Need padding
            leftPadding = (int) (Tegmine.getInstance().dp2px(controller.theme().padding()) * (item.level - 1));
        }
        group.setPadding(leftPadding, 0, 0, 0);
    }

    public void collapseExpand(int position) {
        Item item = getItem(position);
        if (item.expanded) { // Collapse
            collapse(item);
        } else { // Expand
            expand(item, new ArrayList<FileSystemItem>());
        }
    }

    private void collapse(final Item item) {
        if (!item.expanded) { // No need to collapse
            return;
        }
        synchronized (data) {
            int index = data.indexOf(item);
            if (index != -1) { // Found
                while (index < data.size()-1) {
                    Item child = data.get(index+1);
                    if (child.parent == item) { // This is parent
                        collapse(child);
                        data.remove(index+1);
                    } else {
                        break;
                    }
                }
                item.expanded = false;
                notifyDataSetChanged();
            }
        }
    }

    private void expand(final Item item, final List<FileSystemItem> selection) {
        logger.d("Will expand:", item.item, selection);
        Tasks.SimpleTask<List<Item>> task = new Tasks.SimpleTask<List<Item>>() {
            @Override
            protected List<Item> doInBackground() {
                try {
                    List<? extends FileSystemItem> items = controller.fileSystemProvider().children(item.item);
                    List<Item> newItems = new ArrayList<>(items.size());
                    for (FileSystemItem itm : items) { // Create new items
                        Item newItem = new Item();
                        newItem.item = itm;
                        newItem.level = item.level+1;
                        newItem.parent = item;
                        newItems.add(newItem);
                    }
                    return newItems;
                } catch (FileSystemException e) {
                    logger.e(e, "Failed to get contents");
                }
                return new ArrayList<>();
            }

            @Override
            protected void onPostExecute(List<Item> items) {
                logger.d("Contents:", items, item);
                synchronized (data) {
                    int index = data.indexOf(item);
                    if (item == root) { // Root item
                        index = 0;
                    }
                    if (index != -1) { // Found
                        if (index < data.size()-1) { // In the middle
                            data.addAll(index+1, items);
                        } else {
                            // Add to bottom
                            data.addAll(items);
                        }
                        item.expanded = true;
                        for (Item item1 : items) {
                            if (selection.contains(item1.item)) { // Have it in selection
                                expand(item1, selection);
                                return;
                            }
                        }
                        notifyDataSetChanged();
                    }
                }
            }
        };
        task.exec();
    }

    public FileSystemItem getFileSystemItem(int pos) {
        return getItem(pos).item;
    }

    public FileSystemItem root() {
        return root.item;
    }
}
