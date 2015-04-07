package kvj.tegmine.android.ui.adapter;

import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.kvj.bravo7.adapter.AnotherListAdapter;

import java.util.ArrayList;

import kvj.tegmine.android.R;
import kvj.tegmine.android.data.TegmineController;
import kvj.tegmine.android.data.def.FileSystemProvider;

/**
 * Created by vorobyev on 3/26/15.
 */
public abstract class StorageNavigationAdapter extends AnotherListAdapter<String> {

    private TegmineController controller = null;

    public StorageNavigationAdapter(TegmineController controller) {
        super(new ArrayList<String>(), R.layout.item_navigation);
        this.controller = controller;
    }

    @Override
    public void customize(View view, int position) {
        FileSystemProvider provider = controller.fileSystemProvider(getItem(position));
        TextView caption = (TextView) view.findViewById(R.id.item_navigation_text);
        ImageView icon = (ImageView) view.findViewById(R.id.item_navigation_icon);
        caption.setTextColor(controller.theme().textColor());
        caption.setText(provider.label());
        icon.setImageResource(controller.theme().folderIcon());
        view.setBackgroundColor(selected(getItem(position))? controller.theme().selectedColor(): Color.TRANSPARENT);
    }

    public void refresh() {
        data.clear();
        for (String name : controller.fileSystemProviders()) {
            if (controller.fileSystemProvider() == controller.fileSystemProvider(name)) {
                // Default one
                data.add(0, name);
            } else {
                // Just any other
                data.add(name);
            }
        }
        notifyDataSetChanged();
    }

    abstract public boolean selected(String name);
}
