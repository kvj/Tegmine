package kvj.tegmine.android.data.impl.provider.local;

import android.text.TextUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import kvj.tegmine.android.data.def.FileSystemException;
import kvj.tegmine.android.data.def.FileSystemItemType;
import kvj.tegmine.android.data.def.FileSystemProvider;

/**
 * Created by kvorobyev on 2/14/15.
 */
public class LocalFileSystemProvider extends FileSystemProvider<LocalFileSystemItem> {

    private final LocalFileSystemItem root;

    public LocalFileSystemProvider(File parent, String name) throws FileSystemException {
        super(name);
        this.root = new LocalFileSystemItem(this, parent, null);
    }

    @Override
    protected List<LocalFileSystemItem> childrenT(LocalFileSystemItem parent) throws FileSystemException {
        File from = parent != null ? parent.file: this.root.file;
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

    private LocalFileSystemItem fromPath(String path) throws FileSystemException {
        if (TextUtils.isEmpty(path)) { // Not defined
            return null;
        }
        File file = path.startsWith("/") ? new File(path): new File(root.file, path);
        if (file.equals(root.file)) {
            return root;
        }
        LocalFileSystemItem item = new LocalFileSystemItem(this, file, null);
        List<File> parents = new ArrayList<>();
        file = file.getParentFile();
        while (!this.root.file.equals(file)) {
            if (file.getParentFile() == null) { // Different tree
                logger.w("File from different tree:", file.getAbsolutePath(), root.file.getAbsolutePath(), path);
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

    @Override
    public LocalFileSystemItem root() {
        return root;
    }

    @Override
    protected boolean hasFeatureT(LocalFileSystemItem file, Features feature) {
        return true; // All features
    }

    @Override
    protected void renameT(LocalFileSystemItem item, String name) throws FileSystemException {
        if (!item.file.exists()) { // Invalid
            throw new FileSystemException("File does not exist");
        }
        boolean result = item.file.renameTo(new File(item.file.getParentFile(), name));
        if (!result) { // Failed
            throw new FileSystemException("Failed to rename");
        }
    }

    @Override
    protected void createT(FileSystemItemType type, LocalFileSystemItem folder, String name) throws FileSystemException {
        if (!folder.file.exists() || !folder.file.isDirectory()) { // Invalid
            throw new FileSystemException("Invalid folder");
        }
        File file = new File(folder.file, name);
        if (type == FileSystemItemType.Folder) { // Mkdirs
            if (!file.mkdirs()) { // Failed
                throw new FileSystemException("Failed to create folder");
            }
        }
        if (type == FileSystemItemType.File) { // Create new file
            try {
                if (!file.createNewFile()) { // Failed
                    throw new FileSystemException("Failed to create folder");
                }
            } catch (IOException e) {
                throw new FileSystemException(e.getMessage());
            }
        }
    }

    @Override
    protected void removeT(LocalFileSystemItem item) throws FileSystemException {
        if (!item.file.exists()) { // Invalid
            return; // Already removed
        }
        boolean result = item.file.delete();
        if (!result) { // Failed
            throw new FileSystemException("Failed to remove");
        }
    }
}
