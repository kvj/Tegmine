package kvj.tegmine.android.data.impl.provider.local;

import android.os.Bundle;
import android.text.TextUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import kvj.tegmine.android.Tegmine;
import kvj.tegmine.android.data.def.FileSystemException;
import kvj.tegmine.android.data.def.FileSystemItemType;
import kvj.tegmine.android.data.def.FileSystemProvider;

/**
 * Created by kvorobyev on 2/14/15.
 */
public class LocalFileSystemProvider extends FileSystemProvider<LocalFileSystemItem> {

    private final File parent;

    public LocalFileSystemProvider(File parent) {
        this.parent = parent;
    }

    @Override
    protected List<LocalFileSystemItem> childrenT(LocalFileSystemItem parent) throws FileSystemException {
        File from = parent != null ? parent.file: this.parent;
        List<LocalFileSystemItem> result = new ArrayList<>();
        logger.d("Getting contents of", from.getAbsolutePath(), from.isDirectory());
        if (!from.exists() || !from.isDirectory()) { // Invalid file
            throw new FileSystemException("Invalid parent file: "+from.getAbsolutePath());
        }
        for (File file : from.listFiles()) {
            logger.d("File", file.getAbsolutePath());
            result.add(new LocalFileSystemItem(file, parent));
        }
        Collections.sort(result, new Comparator<LocalFileSystemItem>() {
            @Override
            public int compare(LocalFileSystemItem lhs, LocalFileSystemItem rhs) {
                if (lhs.type == FileSystemItemType.Folder && rhs.type == FileSystemItemType.File) { // Folders first
                    return -1;
                }
                if (lhs.type == FileSystemItemType.File && rhs.type == FileSystemItemType.Folder) { // Folders first
                    return 1;
                }
                return lhs.name.compareToIgnoreCase(rhs.name);
            }
        });
        return result;
    }

    @Override
    protected void toBundleT(Bundle bundle, String prefix, LocalFileSystemItem item) throws FileSystemException {
        bundle.putString(prefix+Tegmine.BUNDLE_FILE_LOCATION, item.file.getAbsolutePath());
    }

    @Override
    public LocalFileSystemItem fromBundle(String prefix, Bundle bundle) throws FileSystemException {
        String path = bundle.getString(prefix+Tegmine.BUNDLE_FILE_LOCATION, null);
        if (TextUtils.isEmpty(path)) { // Not defined
            return null;
        }
        File file = new File(path);
        if (!file.exists()) { // Invalid file
            return null;
        }
        LocalFileSystemItem item = new LocalFileSystemItem(file, null);
        List<File> parents = new ArrayList<>();
        file = file.getParentFile();
        while (!this.parent.equals(file)) {
            if (file.getParentFile() == null) { // Different tree
                return null;
            }
            parents.add(file);
            file = file.getParentFile();
        }
        LocalFileSystemItem current = item;
        for (File parentPath : parents) { // Create parents
            LocalFileSystemItem parent = new LocalFileSystemItem(parentPath, null);
            current.parent = parent;
            current = parent;
        }
        return item;
    }
}
