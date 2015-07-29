package kvj.tegmine.android.data.def;

import android.net.Uri;

/**
 * Created by kvorobyev on 2/13/15.
 */
abstract public class FileSystemItem<T> {

    public String name = "";
    public FileSystemItemType type = FileSystemItemType.Folder;
    public T parent = null;
    private String provider = null;

    protected FileSystemItem(String provider) {
        this.provider = provider;
    }

    public String details() {
        return name; // By default just name
    }

    @Override
    public String toString() {
        return "FileSystemItem: "+name;
    }

    abstract public String toURL();

    public boolean hasBeenChanged() {
        return false; // Override it in order to detect
    }

    public String providerName() {
        return provider;
    }

    abstract public String relativeURL(String location);

    public abstract Uri toUri();
}
