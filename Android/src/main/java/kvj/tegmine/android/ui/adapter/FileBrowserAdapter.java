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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import kvj.tegmine.android.R;
import kvj.tegmine.android.Tegmine;
import kvj.tegmine.android.data.TegmineController;
import kvj.tegmine.android.data.def.FileSystemException;
import kvj.tegmine.android.data.def.FileSystemItem;
import kvj.tegmine.android.data.def.FileSystemItemType;
import kvj.tegmine.android.data.def.FileSystemProvider;
import kvj.tegmine.android.data.model.util.Wrappers;

/**
 * Created by kvorobyev on 2/15/15.
 */
public class FileBrowserAdapter extends AnotherListAdapter<FileBrowserAdapter.Item> {

    class Item {
        final String name;
        boolean expanded = false;
        FileSystemItem<?> item = null;
        Item parent = null;
        int level = 0;

        public Item(String name) {
            this.name = name;
        }
    }

    private final TegmineController controller;
    private static Logger logger = Logger.forClass(FileBrowserAdapter.class);
    private Item root = new Item("");

    public FileBrowserAdapter(TegmineController controller) {
        super(new ArrayList<Item>(), R.layout.item_file_browser);
        this.controller = controller;
    }

    public void load(FileSystemItem rootItem, FileSystemItem expandTo) {
        root = new Item(rootItem.name);
        root.item = rootItem;
        data.clear();
        expandTo(expandTo);
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
        text.setText(item.name);
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
//        logger.d("Will expand:", item.item, selection);
        Tasks.SimpleTask<List<Item>> task = new Tasks.SimpleTask<List<Item>>() {
            @Override
            protected List<Item> doInBackground() {
                try {
                    FileSystemProvider provider = controller.fileSystemProvider(item.item.providerName());
                    List<? extends FileSystemItem> items = provider.children(item.item);
                    List<Item> newItems = new ArrayList<>(items.size());
                    for (FileSystemItem itm : items) { // Create new items
                        String name = itm.name;
                        Wrappers.Tuple2<Pattern, Integer> pattern = provider.pattern(itm.type);
                        if (null != pattern) {
                            Matcher m = pattern.v1().matcher(name);
                            if (!m.find()) { // Skip
                                continue;
                            }
                            name = m.group(pattern.v2());
                        }
                        Item newItem = new Item(name);
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
//                logger.d("Contents:", items, item);
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
