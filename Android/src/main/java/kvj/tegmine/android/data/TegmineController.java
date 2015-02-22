package kvj.tegmine.android.data;

import android.os.Environment;
import android.text.TextUtils;
import android.util.TypedValue;
import android.widget.TextView;

import org.kvj.bravo7.log.Logger;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
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
                spaces += spacesInTab();
            } else if (line.charAt(i) == ' ') { // Space
                spaces++;
            } else {
                // Everything else
                break;
            }
        }
        return spaces / spacesInTab();
    }

    public int spacesInTab() {
        return SPACES_IN_TAB;
    }

    public void linesForEditor(List<String> lines, StringBuilder buffer) {
        for (int i = 0; i < lines.size(); i++) { // Iterate over lines
            String line = lines.get(i);
            String trimmed = line.trim();
            if (i>0) { // Add new line
                buffer.append('\n');
            }
            if (trimmed.length() == 0) { // Empty
                continue;
            }
            for (int j = 0; j < indent(line) * spacesInTab(); j++) { // Add spaces
                buffer.append(' ');
            }
            buffer.append(trimmed);
        }
    }

    public void applyHeaderStyle(TextView view) {
        view.setTextColor(theme().textColor());
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, theme().headerTextSp());
    }

    public void writeEdited(OutputStream stream, String contents, boolean doEdit) throws FileSystemException {
        if (TextUtils.isEmpty(contents)) { // No data entered
            return;
        }
        BufferedOutputStream bufferedStream = null;
        try {
            bufferedStream = new BufferedOutputStream(stream);
            String[] lines = contents.split("\n");
            if (!doEdit) { // Append mode - leading new line
                bufferedStream.write('\n');
            }
            for (int i = 0; i < lines.length; i++) { // Write lines one by one
                String line = lines[i];
                if (i>0) { // Add new line
                    bufferedStream.write('\n');
                }
                for (int j = 0; j < indent(line); j++) { // Add tabs
                    bufferedStream.write('\t');
                }
                bufferedStream.write(line.trim().getBytes("utf-8"));
            }
        } catch (Throwable t) { // IO error
            logger.e(t, "Error writing contents:");
            throw new FileSystemException("IO error");
        } finally {
            if (null != bufferedStream) {
                try {
                    bufferedStream.close();
                } catch (Throwable t) {
                    logger.e(t, "Failed to close stream");
                }
            }
        }
    }
}
