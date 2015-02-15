package kvj.tegmine.android.data.impl.provider.local;

import java.io.File;

import kvj.tegmine.android.data.def.FileSystemException;
import kvj.tegmine.android.data.def.FileSystemItem;
import kvj.tegmine.android.data.def.FileSystemItemType;

/**
 * Created by kvorobyev on 2/14/15.
 */
public class LocalFileSystemItem extends FileSystemItem<LocalFileSystemItem> {

    final File file;

    public LocalFileSystemItem(File file, LocalFileSystemItem parent) throws FileSystemException {
        this.name = file.getName();
        this.type = file.isDirectory()? FileSystemItemType.Folder: FileSystemItemType.File;
        this.parent = parent;
        this.file = file;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        return ((LocalFileSystemItem) o).file.equals(file);
    }

    @Override
    public String toString() {
        return "LocalFileSystemItem: "+file.getAbsolutePath();
    }
}
