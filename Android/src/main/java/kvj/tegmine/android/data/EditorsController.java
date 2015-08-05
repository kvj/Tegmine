package kvj.tegmine.android.data;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import org.kvj.bravo7.util.Listeners;

import java.util.ArrayList;
import java.util.List;

import kvj.tegmine.android.R;
import kvj.tegmine.android.Tegmine;
import kvj.tegmine.android.data.def.FileSystemItem;
import kvj.tegmine.android.data.model.EditorInfo;

/**
 * Created by kvorobyev on 5/5/15.
 */
public class EditorsController {

    private final TegmineController controller;
    List<EditorInfo> tabs = new ArrayList<>();
    private int selected = 0;

    public final Listeners<View.OnKeyListener> keyListeners = new Listeners<>();

    public EditorsController(TegmineController controller) {
        this.controller = controller;
        loadState();
    }

    private void loadTitle(EditorInfo tab) {
        FileSystemItem item = controller.fromURL(tab.itemURL);
        if (null == item) { // Invalid
            tab.title = "???";
            return;
        }
        tab.title = String.format("%s%s", (tab.mode == EditorInfo.Mode.Append? "+": ""), item.name);
    }

    private void loadState() {
        tabs.clear();
        int size = controller.preferences(null).getInt(controller.context().getString(R.string.p_tabs_size), 0);
        for (int i = 0; i < size; i++) { // $COMMENT
            EditorInfo tab = new EditorInfo();
            tab.readFromPreferences(controller.preferences(null), String.format("p_tab_%d_", i));
            if (tab.mode != EditorInfo.Mode.None) { // Not first not empty
                loadTitle(tab);
                tabs.add(tab);
            }
        }
    }

    public void saveState() {
        SharedPreferences.Editor pref = controller.preferences(null).edit();
        pref.putInt(controller.context().getString(R.string.p_tabs_size), tabs.size());
        for (int i = 0; i < tabs.size(); i++) { // Iterate
            EditorInfo tab = tab(i);
            if (tab.view != null) {
                tab.view.toInfo();
            }
            tab.writeToPreferences(pref, String.format("p_tab_%d_", i));
        }
        pref.apply();
    }

    public EditorInfo fromBundle(Bundle data) {
        EditorInfo.Mode mode = EditorInfo.Mode.fromString(data.getString(Tegmine.BUNDLE_EDIT_TYPE));
        if (mode == EditorInfo.Mode.None) { // Not set
            return null;
        }
        String url = data.getString("select", "");
        for (int i = 0; i < tabs.size(); i++) { // Iterate
            EditorInfo info = tab(i);
            if (url.equals(info.itemURL)) { // Same file
                if (info.mode == EditorInfo.Mode.Edit) {
                    // Want to add but editing now - edit
                    selected = i;
                    info.template = data.getString(Tegmine.BUNDLE_EDIT_TEMPLATE, null);
                    if (null != info.view) { // Also have view
                        info.view.appendTemplate();
                    }
                    return info; // No load needed
                }
            }
        }
        final EditorInfo info = new EditorInfo();
        selected = tabs.size();
        tabs.add(info);
        info.text = null; // Ask for load
        info.mode = mode;
        info.itemURL = url;
        info.template = data.getString(Tegmine.BUNDLE_EDIT_TEMPLATE, null);
        loadTitle(info);
        saveState();
        return info;
    }

    public EditorInfo tab(int index) {
        if (index < 0 || index >= tabs.size()) { // Invalid index
            return null;
        }
        return tabs.get(index);
    }

    public int size() {
        return tabs.size();
    }

    public int selected() {
        return selected;
    }

    public void selected(int position) {
        selected = position;
    }

    public void remove(int sel) {
        tab(sel).mode = EditorInfo.Mode.None;
        tabs.remove(tab(sel));
        saveState();
    }

    public int nextSelected() {
        if (selected > 0) { // Left tab
            return selected - 1;
        } else {
            return selected + 1; // Right tab
        }
    }

    public int position(EditorInfo info) {
        return tabs.indexOf(info);
    }
}
