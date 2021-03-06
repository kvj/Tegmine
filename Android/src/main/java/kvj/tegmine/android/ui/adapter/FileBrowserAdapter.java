package kvj.tegmine.android.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.kvj.bravo7.log.Logger;
import org.kvj.bravo7.util.Tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import kvj.tegmine.android.R;
import kvj.tegmine.android.data.TegmineController;
import kvj.tegmine.android.data.def.FileSystemException;
import kvj.tegmine.android.data.def.FileSystemItem;
import kvj.tegmine.android.data.def.FileSystemItemType;
import kvj.tegmine.android.data.def.FileSystemProvider;
import kvj.tegmine.android.data.model.util.Wrappers;

/**
 * Created by kvorobyev on 2/15/15.
 */
public abstract class FileBrowserAdapter extends RecyclerView.Adapter<FileBrowserAdapter.Holder> {

    @Override
    public Holder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.item_file_browser, viewGroup, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(Holder holder, final int position) {
        Item item = data.get(position);
        holder.text.setText(item.name);
        holder.text.setTextColor(controller.theme().textColor());
        holder.text.setTextSize(TypedValue.COMPLEX_UNIT_SP, controller.theme().browserTextSp());
        holder.image.setImageResource(item.item.type == FileSystemItemType.Folder?
                controller.theme().folderIcon():
                controller.theme().fileIcon());
        int leftPadding = 0;
        if (item.level > 1) { // Need padding
            leftPadding = (int) (controller.dp2px(controller.theme().paddingDp()) * (item.level - 1));
        }
        holder.group.setPadding(leftPadding, 0, 0, 0);

    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    class Holder extends RecyclerView.ViewHolder
            implements MenuItem.OnMenuItemClickListener,
            View.OnClickListener,
            View.OnLongClickListener,
            View.OnCreateContextMenuListener {
        private final TextView text;
        private final ImageView image;
        private final ViewGroup group;
        public final View root;

        public Holder(View view) {
            super(view);
            root = view;
            text = (TextView) view.findViewById(R.id.file_browser_item_text);
            image = (ImageView) view.findViewById(R.id.file_browser_item_icon);
            group = (ViewGroup) view.findViewById(R.id.file_browser_item);
            root.setOnClickListener(this);
            root.setOnLongClickListener(this);
            root.setOnCreateContextMenuListener(this);
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            return FileBrowserAdapter.this.onMenuItemClick(item, getAdapterPosition());
        }

        @Override
        public void onClick(View v) {
            FileBrowserAdapter.this.onClick(getAdapterPosition());
        }

        @Override
        public boolean onLongClick(View v) {
            root.showContextMenu();
            return true;
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            FileBrowserAdapter.this.onCreateContextMenu(menu, getAdapterPosition());
            for (int i = 0; i < menu.size(); i++) {
                menu.getItem(i).setOnMenuItemClickListener(this);
            }
        }
    }

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
    private final List<Item> data = new ArrayList<>();

    public FileBrowserAdapter(TegmineController controller) {
        super();
        this.controller = controller;
    }

    public void load(FileSystemItem rootItem, FileSystemItem expandTo) {
        root = new Item(rootItem.name);
        root.item = rootItem;
        data.clear();
        notifyDataSetChanged();
        expandTo(expandTo);
    }

    public void select(FileSystemItem expandTo) {
        data.clear();
        notifyDataSetChanged();
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

    public void collapseExpand(int position) {
        Item item = data.get(position);
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
                        data.remove(index + 1);
                        notifyItemRemoved(index + 1);
                    } else {
                        break;
                    }
                }
                item.expanded = false;
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
                        notifyItemRangeInserted(index+1, items.size());
                        item.expanded = true;
                        for (Item item1 : items) {
                            if (selection.contains(item1.item)) { // Have it in selection
                                expand(item1, selection);
                                return;
                            }
                        }
                    }
                }
            }
        };
        task.exec();
    }

    abstract public void onClick(int position);
    public abstract void onCreateContextMenu(ContextMenu menu, int position);
    protected abstract boolean onMenuItemClick(MenuItem item, int position);

    public FileSystemItem getFileSystemItem(int pos) {
        return data.get(pos).item;
    }

    public FileSystemItem root() {
        return root.item;
    }
}
