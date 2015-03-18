package kvj.tegmine.android.data.impl.provider.local;

import android.os.Bundle;
import android.text.TextUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
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

    public LocalFileSystemProvider(File parent, String name) {
        super(name);
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
        File[] files = from.listFiles();
        if (null != files) {
            for (File file : from.listFiles()) {
                result.add(new LocalFileSystemItem(this, file, parent));
            }
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

    private LocalFileSystemItem fromPath(String path) throws FileSystemException {
        if (TextUtils.isEmpty(path)) { // Not defined
            return null;
        }
        File file = path.startsWith("/") ? new File(path): new File(parent, path);
        if (!file.exists()) { // Invalid file
            logger.w("Non existing file:", path, file.getAbsolutePath(), parent.getAbsolutePath());
            return null;
        }
        LocalFileSystemItem item = new LocalFileSystemItem(this, file, null);
        List<File> parents = new ArrayList<>();
        file = file.getParentFile();
        while (!this.parent.equals(file)) {
            if (file.getParentFile() == null) { // Different tree
                logger.w("File from different tree:", file.getAbsolutePath(), parent.getAbsolutePath());
                return null;
            }
            parents.add(file);
            file = file.getParentFile();
        }
        LocalFileSystemItem current = item;
        for (File parentPath : parents) { // Create parents
            LocalFileSystemItem parent = new LocalFileSystemItem(this, parentPath, null);
            current.parent = parent;
            current = parent;
        }
        return item;

    }

    @Override
    public LocalFileSystemItem fromBundle(String prefix, Bundle bundle) throws FileSystemException {
        String path = bundle.getString(prefix+Tegmine.BUNDLE_FILE_LOCATION, null);
        return fromPath(path);
    }

    @Override
    protected InputStream readT(LocalFileSystemItem file) throws FileSystemException {
        if (!file.file.isFile() || !file.file.canRead()) { // Invalid access
            throw new FileSystemException("Invalid file: "+file.file);
        }
        try {
            return new FileInputStream(file.file);
        } catch (FileNotFoundException e) {
            logger.e(e, "Failed to read:", file.file);
            throw new FileSystemException("Invalid file: "+file.file);
        }
    }

    @Override
    protected OutputStream appendT(LocalFileSystemItem file) throws FileSystemException {
        if (!file.file.isFile() || !file.file.canWrite()) { // Invalid access
            throw new FileSystemException("Invalid file: "+file.file);
        }
        try {
            return new FileOutputStream(file.file, true);
        } catch (FileNotFoundException e) {
            logger.e(e, "Failed to append:");
            throw new FileSystemException("Invalid file: "+file.file);
        }
    }

    @Override
    protected OutputStream replaceT(LocalFileSystemItem file) throws FileSystemException {
        if (!file.file.isFile() || !file.file.canWrite()) { // Invalid access
            throw new FileSystemException("Invalid file: "+file.file);
        }
        try {
            return new FileOutputStream(file.file, false);
        } catch (FileNotFoundException e) {
            logger.e(e, "Failed to append:");
            throw new FileSystemException("Invalid file: "+file.file);
        }
    }

    @Override
    public LocalFileSystemItem fromURL(String url) throws FileSystemException {
        LocalFileSystemItem item = fromPath(url);
        if (null == item) { // Failed to read
            throw new FileSystemException("Invalid URL");
        }
        return item;
    }
}
