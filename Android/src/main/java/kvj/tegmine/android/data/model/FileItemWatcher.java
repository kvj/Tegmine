package kvj.tegmine.android.data.model;

import android.os.Handler;
import android.os.Message;

import org.kvj.bravo7.log.Logger;

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
        for (FileSystemItem item : items) {
//            logger.d("File changed:", item.hasBeenChanged(), item.name);
            if (item.hasBeenChanged()) {
                itemChanged(item);
            }
        }
    }

    abstract public void itemChanged(FileSystemItem item);
}
