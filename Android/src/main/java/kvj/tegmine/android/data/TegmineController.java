package kvj.tegmine.android.data;

import android.os.Environment;
import android.text.TextUtils;

import org.kvj.bravo7.log.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import kvj.tegmine.android.data.def.FileSystemException;
import kvj.tegmine.android.data.def.FileSystemItem;
import kvj.tegmine.android.data.impl.provider.local.LocalFileSystemProvider;
import kvj.tegmine.android.ui.theme.LightTheme;

/**
 * Created by kvorobyev on 2/13/15.
 */
public class TegmineController {

    private static final int SPACES_IN_TAB = 2;
    private final LocalFileSystemProvider fileSystemProvider;
    private LightTheme theme = new LightTheme();
    private Logger logger = Logger.forInstance(this);

    public TegmineController() {
        this.fileSystemProvider = new LocalFileSystemProvider(Environment.getExternalStorageDirectory());
    }

    public LocalFileSystemProvider fileSystemProvider() {
        return fileSystemProvider;
    }

    public LightTheme theme() {
        return theme;
    }

    public List<Long> makeFileLayout(FileSystemItem item) throws FileSystemException {
        // Read file and make list with line offsets
        List<Long> offsets = new ArrayList<>();
        InputStream stream = fileSystemProvider.read(item);
        long offset = 0;
        long from = 0;
        int data;
        try {
            while ((data = stream.read()) >= 0) { // Read until end
                offset++;
                if (data == '\n') { // New line
                    offsets.add(from);
                    from = offset;
                }
            }
            if (from<offset) { // Add last
                offsets.add(from);
            }
            stream.close();
            return offsets;
        } catch (IOException e) {
            logger.e(e, "Failed to make layout");
            throw new FileSystemException("Failed to read file");
        }
    }

    public void loadFilePart(List<String> buffer, FileSystemItem item, long from, int lines) throws FileSystemException {
        try {
            InputStream stream = fileSystemProvider.read(item);
            long skipped = stream.skip(from);
            if (from != skipped) { // Invalid offset
                stream.close();
                throw new FileSystemException("Invalid position");
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "utf-8"));
            String line;
            int linesRead = 0;
            while ((line = reader.readLine()) != null) { // Read lines one by one
                buffer.add(line);
                linesRead++;
                if ((lines > 0) && (linesRead >= lines)) { // Enough
                    break;
                }
            }
            stream.close();
            return;
        } catch (IOException e) {
            logger.e(e, "Failed to read file");
            throw new FileSystemException("Failed to read file");
        }
    }

    public int indent(String line) {
        if (TextUtils.isEmpty(line)) { // Empty line - indent undefined
            return -1;
        }
        int spaces = 0;
        for (int i = 0; i < line.length(); i++) { // Search for a first non space/tab
            if (line.charAt(i) == '\t') { // Tab
                spaces += SPACES_IN_TAB;
            } else if (line.charAt(i) == ' ') { // Space
                spaces++;
            } else {
                // Everything else
                break;
            }
        }
        return spaces / SPACES_IN_TAB;
    }
}
