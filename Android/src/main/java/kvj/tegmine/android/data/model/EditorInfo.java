package kvj.tegmine.android.data.model;

import android.content.SharedPreferences;
import android.widget.EditText;

import java.io.UnsupportedEncodingException;
import java.util.zip.CRC32;

import kvj.tegmine.android.ui.fragment.OneEditor;

/**
 * Created by kvorobyev on 5/4/15.
 */
public class EditorInfo {

    public OneEditor view = null;
    public String title = "";

    public enum Mode {None("mode_none"), Append("edit_add"), Edit("edit_edit");

        private final String code;

        Mode(String code) {
            this.code = code;
        }

        public String code() {
            return code;
        }

        public static Mode fromString(String code) {
            for (Mode mode : values()) { // Search by mode
                if (mode.code.equals(code)) { // Found
                    return mode;
                }
            }
            return None; // Default
        }
    };

    public String itemURL = "";
    public long crc = -1L;
    public String text = null;
    public String template = null;
    public Mode mode = Mode.None;
    public int selectionStart = -1;
    public int selectionEnd = -1;
    public final long id = System.currentTimeMillis();

    public void readFromPreferences(SharedPreferences pref, String prefix) {
        mode = Mode.fromString(pref.getString(prefix+"mode", ""));
        if (mode == Mode.None) { // Failed
            return;
        }
        itemURL = pref.getString(prefix+"url", null);
        crc = pref.getLong(prefix + "hash", -1L);
        text = pref.getString(prefix + "text", "");
        selectionStart = pref.getInt(prefix + "sel_start", -1);
        selectionEnd = pref.getInt(prefix+"sel_end", -1);
    }

    public void writeToPreferences(SharedPreferences.Editor pref, String prefix) {
        pref.putString(prefix+"mode", mode.code());
        pref.putString(prefix+"url", itemURL);
        pref.putLong(prefix + "hash", crc);
        pref.putString(prefix + "text", text);
        pref.putInt(prefix + "sel_start", selectionStart);
        pref.putInt(prefix+"sel_start", selectionEnd);
    }

    public void fromEditor(EditText editor) {
        if (null != editor) { //
            text = editor.getText().toString().trim();
            selectionStart = editor.getSelectionStart();
            selectionEnd = editor.getSelectionEnd();
        }
    }

    public void asOriginal(String text) {
        crc = hash(text);
    }

    public static long hash(String text) {
        CRC32 crc32 = new CRC32();
        try {
            crc32.update(text.getBytes("utf-8"));
            return crc32.getValue();
        } catch (UnsupportedEncodingException e) {
        }
        return -1L;
    }

    @Override
    public String toString() {
        return String.format("EditorInfo [%s, %s, %d]", mode, itemURL, crc);
    }
}