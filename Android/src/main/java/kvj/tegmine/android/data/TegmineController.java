package kvj.tegmine.android.data;

import android.os.Environment;

import kvj.tegmine.android.data.impl.provider.local.LocalFileSystemProvider;
import kvj.tegmine.android.ui.theme.LightTheme;

/**
 * Created by kvorobyev on 2/13/15.
 */
public class TegmineController {

    private final LocalFileSystemProvider fileSystemProvider;
    private LightTheme theme = new LightTheme();

    public TegmineController() {
        this.fileSystemProvider = new LocalFileSystemProvider(Environment.getExternalStorageDirectory());
    }

    public LocalFileSystemProvider fileSystemProvider() {
        return fileSystemProvider;
    }

    public LightTheme theme() {
        return theme;
    }
}
