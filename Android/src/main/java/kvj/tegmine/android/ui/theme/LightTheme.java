package kvj.tegmine.android.ui.theme;

import android.graphics.Color;

import java.util.HashMap;
import java.util.Map;

import kvj.tegmine.android.R;
import kvj.tegmine.android.data.TegmineController;

/**
 * Created by kvorobyev on 2/14/15.
 */
public class LightTheme {

    private final TegmineController controller;

    public enum Size {
        headerTextSp("header_text", 18), browserTextSp("browser_text", 16), fileTextSp("file_text", 14),
        fileIndentSp("file_indent", 14), editorTextSp("editor_text", 14), paddingDp("padding", 16);

        private final String code;
        private final int def;
        Size(String code, int def) {
            this.code = code;
            this.def = def;
        }

        public String code() {
            return code;
        }

        public int def() {
            return def;
        }

        public static Size findSize(String code) {
            for (Size size : Size.values()) {
                if (size.code.equals(code)) {
                    return size;
                }
            }
            return null;
        }
    };

    public enum Colors {Base0("base0"), Base1("base1"),Base2("base2"), Base3("base3"),
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

    protected Map<Colors, Integer> mappingColors = new HashMap<>();
    protected Map<Size, Integer> mappingSizes = new HashMap<>();

    public LightTheme(TegmineController controller) {
        this.controller = controller;
    }

    public int color(Colors color, int defaultColor) {
        Integer colorInt = mappingColors.get(color);
        if (null == colorInt) {
            // No mappingColors
            return defaultColor;
        }
        return colorInt;
    }

    private int size(Size size) {
        Integer custom = mappingSizes.get(size);
        if (null != custom) {
            return custom;
        }
        return controller.size(size);
    }

    private boolean dark = false;

    public void dark(boolean dark) {
        this.dark = dark;
    }

    public boolean dark() {
        return dark;
    }

    public int textColor() {
        return color(Colors.Base0, dark? Color.WHITE: Color.BLACK);
    }

    public int markColor() {
        return color(Colors.Base2, dark? Color.DKGRAY: Color.GRAY);
    }

    public int backgroundColor() {
        return color(Colors.Base3, dark? Color.BLACK: Color.WHITE);
    }

    public int altTextColor() {
        return color(Colors.Base1, markColor());
    }

    public int headerTextSp() {
        return size(Size.headerTextSp);
    }

    public int browserTextSp() {
        return size(Size.browserTextSp);
    }

    public int fileTextSp() {
        return size(Size.fileTextSp);
    }

    public int fileIndentSp() {
        return size(Size.fileIndentSp);
    }

    public int editorTextSp() {
        return size(Size.editorTextSp);
    }

    public int paddingDp() {
        return size(Size.paddingDp);
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

    public boolean loadColor(Colors col, String color) {
        if (null != col) {
            mappingColors.put(col, Color.parseColor(color));
            return true;
        }
        return false;
    }

    public boolean loadSize(String key, int value) {
        Size size = Size.findSize(key);
        if (null != size && value >= 0) {
            mappingSizes.put(size, value);
            return true;
        }
        return false;
    }

}
