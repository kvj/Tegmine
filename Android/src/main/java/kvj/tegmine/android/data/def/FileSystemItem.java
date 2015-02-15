package kvj.tegmine.android.data.def;

/**
 * Created by kvorobyev on 2/13/15.
 */
public class FileSystemItem<T> {

    public String name = "";
    public FileSystemItemType type = FileSystemItemType.Folder;
    public T parent = null;

    public String details() {
        return name; // By default just name
    }

    @Override
    public String toString() {
        return "FileSystemItem: "+name;
    }
}
