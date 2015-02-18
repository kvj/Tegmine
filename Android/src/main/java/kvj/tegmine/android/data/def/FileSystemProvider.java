package kvj.tegmine.android.data.def;

import android.os.Bundle;

import org.kvj.bravo7.log.Logger;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Created by kvorobyev on 2/13/15.
 */
abstract public class FileSystemProvider<T extends FileSystemItem> {

    protected Logger logger = Logger.forInstance(this);

    public final List<? extends FileSystemItem> children(FileSystemItem parent) throws FileSystemException {
        return childrenT(cast(parent));
    }

    protected List<T> childrenT(T parent) throws FileSystemException {
        throw new FileSystemException("Not implemented");
    }

    public final InputStream read(FileSystemItem file) throws FileSystemException {
        return readT(cast(file));
    }

    protected InputStream readT(T file) throws FileSystemException{
        throw new FileSystemException("Not implemented");
    }

    public final void remove(FileSystemItem item) throws FileSystemException {
        removeT(cast(item));
    }

    protected void removeT(T item) throws FileSystemException {
        throw new FileSystemException("Not implemented");
    }

    public final void create(FileSystemItemType type, FileSystemItem folder, String name) throws FileSystemException {
        createT(type, cast(folder), name);
    }

    protected void createT(FileSystemItemType type, T folder, String name) throws FileSystemException {
        throw new FileSystemException("Not implemented");
    }

    protected void replaceT(T file, OutputStream writer) throws FileSystemException {
        throw new FileSystemException("Not implemented");
    }

    protected void appendT(T file, OutputStream writer) throws FileSystemException {
        throw new FileSystemException("Not implemented");
    }

    protected T cast(FileSystemItem item) {
        return (T)item;
    }

    public void toBundle(Bundle bundle, String prefix, FileSystemItem item) throws FileSystemException {
        toBundleT(bundle, prefix, cast(item));
    }

    protected void toBundleT(Bundle bundle, String prefix, T item) throws FileSystemException{
        throw new FileSystemException("Not implemented");
    }

    public T fromBundle(String prefix, Bundle bundle) throws FileSystemException {
        throw new FileSystemException("Not implemented");
    }
}
