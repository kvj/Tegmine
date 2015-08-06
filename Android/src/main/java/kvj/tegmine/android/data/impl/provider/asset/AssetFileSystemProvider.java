package kvj.tegmine.android.data.impl.provider.asset;

import android.content.Context;

import org.kvj.bravo7.ng.App;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import kvj.tegmine.android.Tegmine;
import kvj.tegmine.android.data.TegmineController;
import kvj.tegmine.android.data.def.FileSystemException;
import kvj.tegmine.android.data.def.FileSystemProvider;

/**
 * Created by vorobyev on 8/6/15.
 */
public class AssetFileSystemProvider extends FileSystemProvider<AssetFileSystemItem> {

    private final Context context;
    private AssetFileSystemItem root = null;

    public AssetFileSystemProvider(String name, Context context) {
        super(name);
        this.context = context;
        root = new AssetFileSystemItem(name, true);
        root.name = "Assets";
        hidden = true;
    }

    @Override
    public AssetFileSystemItem fromURL(String url) throws FileSystemException {
        AssetFileSystemItem item = new AssetFileSystemItem(name, false);
        item.name = url;
        return item;
    }

    @Override
    public AssetFileSystemItem root() {
        return root;
    }

    @Override
    protected String versionT(AssetFileSystemItem file) {
        return file.name;
    }

    @Override
    protected List<AssetFileSystemItem> childrenT(AssetFileSystemItem parent) throws FileSystemException {
        try {
            String[] files = context.getResources().getAssets().list("");
            List<AssetFileSystemItem> result = new ArrayList<>();
            for (String name : files) {
                AssetFileSystemItem item = new AssetFileSystemItem(this.name, false);
                item.name = name;
                result.add(item);
            }
            return result;
        } catch (IOException e) {
            logger.e(e, "Error listing assets");
            throw new FileSystemException("IO error");
        }
    }

    @Override
    protected InputStream readT(AssetFileSystemItem file) throws FileSystemException {
        try {
            return context.getResources().getAssets().open(file.name);
        } catch (IOException e) {
            logger.e(e, "Error reading file:", file.name);
            throw new FileSystemException("IO error");
        }
    }
}
