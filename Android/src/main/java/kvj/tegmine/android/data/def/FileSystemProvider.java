package kvj.tegmine.android.data.def;

import java.io.Reader;
import java.io.Writer;
import java.util.List;

/**
 * Created by kvorobyev on 2/13/15.
 */
public interface FileSystemProvider {

    public List<FileSystemItem> children(FileSystemItem parent) throws FileSystemException;

    public Reader read(FileSystemItem file) throws FileSystemException;

    public void remove(FileSystemItem item) throws FileSystemException;

    public void create(FileSystemItemType type, FileSystemItem folder, String name) throws FileSystemException;

    public void replace(FileSystemItem file, Writer writer) throws FileSystemException;

    public void append(FileSystemItem file, Writer writer) throws FileSystemException;
}
