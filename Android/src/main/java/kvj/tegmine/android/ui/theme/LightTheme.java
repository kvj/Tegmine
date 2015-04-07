package kvj.tegmine.android.ui.theme;

import android.graphics.Color;

import java.util.HashMap;
import java.util.Map;

import kvj.tegmine.android.R;

/**
 * Created by kvorobyev on 2/14/15.
 */
public class LightTheme {

    public static enum Colors {Base0("base0"), Base1("base1"), Base2("base2"), Base3("base3"),
        Yellow("yellow"), Orange("orange"), Red("red"), Magenta("magenta"),
        Violet("violet"), Blue("blue"), Cyan("cyan"), Green("green");
        private final String code;

        Colors(String code) {
            this.code = code;
        }

        public String code() {
            return code;
        }

        public static Colors findColor(String code) {
            for (Colors col : Colors.values()) {
                if (col.code().equals(code)) {
                    return col;
                }
            }
            return null;
        }
    };

    protected Map<Colors, Integer> mapping = new HashMap<>();

    public int color(Colors color, int defaultColor) {
        Integer colorInt = mapping.get(color);
        if (null == colorInt) {
            // No mapping
            return defaultColor;
        }
        return colorInt;
    }

    private boolean dark = false;

    public void dark(boolean dark) {
        this.dark = dark;
    }

    public boolean dark() {
        return dark;
    }

    public int textColor() {
        return color(Colors.Base0, dark? Color.BLACK: Color.WHITE);
    }

    public int markColor() {
        return color(Colors.Base2, dark? Color.BLACK: Color.WHITE);
    }

    public int backgroundColor() {
        return color(Colors.Base3, dark? Color.WHITE: Color.BLACK);
    }

    public int selectedColor() {
        return color(Colors.Base2, Color.GRAY);
    }

    public int headerTextSp() {
        return 18;
    }

    public int browserTextSp() {
        return 16;
    }

    public int fileTextSp() {
        return 14;
    }

    public int fileIndentSp() {
        return 14;
    }

    public int editorTextSp() {
        return 14;
    }

    public int padding() {
        return 16;
    }

    public int folderIcon() {
        return dark? R.drawable.icn_folder_dark: R.drawable.icn_folder_light;
    }

    public int fileIcon() {
        return dark? R.drawable.icn_file_dark: R.drawable.icn_file_light;
    }

    public int fileEditIcon() {
        return dark? R.drawable.icn_file_edit_dark: R.drawable.icn_file_edit_light;
    }

    public int fileAddIcon() {
        return dark? R.drawable.icn_file_add_dark: R.drawable.icn_file_add_light;
    }

    public int fileSaveIcon() {
        return dark? R.drawable.icn_file_save_dark: R.drawable.icn_file_save_light;
    }

    public boolean loadColor(String code, String color) {
        Colors col = Colors.findColor(code);
        if (null != col) {
            mapping.put(col, Color.parseColor(color));
            return true;
        }
        return false;
    }
}
