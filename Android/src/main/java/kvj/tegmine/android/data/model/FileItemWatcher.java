package kvj.tegmine.android.data.model;

import android.os.Handler;
import android.os.Message;

import org.kvj.bravo7.log.Logger;

import java.util.ArrayList;
import java.util.List;

import kvj.tegmine.android.data.TegmineController;
import kvj.tegmine.android.data.def.FileSystemItem;

/**
 * Created by vorobyev on 3/17/15.
 */
public abstract class FileItemWatcher {

    private Logger logger = Logger.forInstance(this);

    private final TegmineController controller;
    private final Handler handler;
    private boolean active = false;
    private final FileSystemItem[] items;
    private List<String> versions = new ArrayList<>();

    public FileItemWatcher(TegmineController controller, FileSystemItem... items) {
        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message message) {
                if (!active)
                    return true;
                run();
                schedule();
                return true;
            }
        });
        this.controller = controller;
        this.items = items;
        reset();
    }

    public void active(boolean active) {
        if (active && !this.active) {
            // Becoming active
            run();
        }
        this.active = active;
        schedule();
    }

    private boolean schedule() {
        if (!active) {
            return false;
        }
        handler.sendEmptyMessageDelayed(0, controller.watchSeconds() * 1000);
//        logger.d("Posted next watch");
        return true;
    }

    private void run() {
        // Check watched
        for (int i = 0; i < items.length; i++) {
            FileSystemItem item = items[i];
            String version = controller.fileSystemProvider(item).version(item);
            if (!version.equals(versions.get(i))) {
                versions.set(i, version);
                itemChanged(item);
            }
        }
    }

    abstract public void itemChanged(FileSystemItem item);

    public void reset() {
        for (FileSystemItem item : items) {
            versions.add(controller.fileSystemProvider(item).version(item));
        }
    }
}
