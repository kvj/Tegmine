package kvj.tegmine.android.data.def;

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

    public void commit() {
        // Called when IO operation is done
    }

    public String providerName() {
        return provider;
    }
}
