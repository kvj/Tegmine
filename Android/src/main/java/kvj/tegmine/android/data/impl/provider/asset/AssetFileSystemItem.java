package kvj.tegmine.android.data.impl.provider.asset;

import android.net.Uri;

import kvj.tegmine.android.data.def.FileSystemItem;
import kvj.tegmine.android.data.def.FileSystemItemType;

/**
 * Created by vorobyev on 8/6/15.
 */
public class AssetFileSystemItem extends FileSystemItem<AssetFileSystemItem> {

    final boolean root;

    protected AssetFileSystemItem(String provider, boolean root) {
        super(provider);
        this.root = root;
        if (root) {
            type = FileSystemItemType.Folder;
        }
    }

    @Override
    public String toURL() {
        return String.format("tegmine+%s://%s", provider, root? "": name);
    }

    @Override
    public String relativeURL(String location) {
        return String.format("tegmine+%s://%s", provider, location);
    }

    @Override
    public Uri toUri() {
        return null; // Not supported
    }
}
