package kvj.tegmine.android.data.def;

import org.kvj.bravo7.log.Logger;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.regex.Pattern;

import kvj.tegmine.android.data.model.util.Wrappers;

/**
 * Created by kvorobyev on 2/13/15.
 */
abstract public class FileSystemProvider<T extends FileSystemItem> {

    public enum Features {CanRenameFile, CanRenameFolder, CanRemoveFolder, CanRemoveFile, CanCreateFolder, CanCreateFile};

    protected final String name;
    protected String label = null;
    protected boolean hidden = false;

    protected boolean tab = true;
    protected int tabSize = 2;
    protected boolean scrollToBottom = false;

    protected Wrappers.Tuple2<Pattern, Integer> filePattern = null;
    protected Wrappers.Tuple2<Pattern, Integer> folderPattern = null;

    protected FileSystemProvider(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    protected Logger logger = Logger.forInstance(this);

    public String label() {
        return label != null? label: name;
    }

    public void label(String label) {
        this.label = label;
    }

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

    public final OutputStream replace(FileSystemItem file) throws FileSystemException {
        return replaceT(cast(file));
    }

    protected OutputStream replaceT(T file) throws FileSystemException {
        throw new FileSystemException("Not implemented");
    }

    public final OutputStream append(FileSystemItem file) throws FileSystemException {
        return appendT(cast(file));
    }

    protected OutputStream appendT(T file) throws FileSystemException {
        throw new FileSystemException("Not implemented");
    }

    protected T cast(FileSystemItem item) {
        return (T)item;
    }

    public T fromURL(String url) throws FileSystemException {
        throw new FileSystemException("Not implemented");
    }

    public abstract T root();

    public Wrappers.Tuple2<Pattern, Integer> pattern(FileSystemItemType type) {
        return type == FileSystemItemType.File ? filePattern: folderPattern;
    }

    public void filePattern(Wrappers.Tuple2<Pattern, Integer> filePattern) {
        this.filePattern = filePattern;
    }

    public void folderPattern(Wrappers.Tuple2<Pattern, Integer> folderPattern) {
        this.folderPattern = folderPattern;
    }

    public final boolean hasFeature(FileSystemItem file, Features feature) {
        return hasFeatureT((T)file, feature);
    }

    protected boolean hasFeatureT(T file, Features feature) {
        return false;
    }

    public final void rename(FileSystemItem item, String name) throws FileSystemException {
        renameT((T) item, name);
    }

    protected void renameT(T item, String name) throws FileSystemException {
        throw new FileSystemException("Not implemented");
    }

    public final String version(FileSystemItem file) {
        return versionT((T)file);
    }

    abstract protected String versionT(T file);

    public boolean hidden() {
        return hidden;
    }
    public void tab(boolean use_tab) {
        this.tab = use_tab;
    }

    public boolean useTab() {
        return tab;
    }

    public void tabSize(int tabSize) {
        this.tabSize = tabSize;
    }

    public int tabSize() {
        return tabSize;
    }

    public boolean scrollToBottom() {
        return scrollToBottom;
    }

    public void scrollToBottom(boolean scrollToBottom) {
        this.scrollToBottom = scrollToBottom;
    }
}
