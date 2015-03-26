package kvj.tegmine.android.data.impl.provider.local;

import java.io.File;
import java.util.Date;

import kvj.tegmine.android.data.def.FileSystemException;
import kvj.tegmine.android.data.def.FileSystemItem;
import kvj.tegmine.android.data.def.FileSystemItemType;

/**
 * Created by kvorobyev on 2/14/15.
 */
public class LocalFileSystemItem extends FileSystemItem<LocalFileSystemItem> {

    final File file;
    private final LocalFileSystemProvider provider;

    private long lastUpdateTime = 0;

    public LocalFileSystemItem(LocalFileSystemProvider provider, File file, LocalFileSystemItem parent) throws FileSystemException {
        super(provider.name());
        this.name = file.getName();
        this.type = file.isDirectory()? FileSystemItemType.Folder: FileSystemItemType.File;
        this.parent = parent;
        this.file = file;
        this.provider = provider;
        markNotModified();
    }

    private void markNotModified() {
        lastUpdateTime = file.lastModified();
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

    @Override
    public String toURL() {
        return String.format("tegmine+%s://%s", provider.name(), file.getAbsolutePath());
    }

    @Override
    public String details() {
        return file.getAbsolutePath();
    }

    @Override
    public boolean hasBeenChanged() {
        return file.lastModified() != lastUpdateTime;
    }

    @Override
    public void commit() {
        markNotModified();
    }
}
