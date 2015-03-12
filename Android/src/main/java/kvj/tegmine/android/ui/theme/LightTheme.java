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

    public int textColor() {
        return color(Colors.Base0, Color.BLACK);
    }

    public int markColor() {
        return color(Colors.Base2, Color.BLACK);
    }

    public int backgroundColor() {
        return color(Colors.Base3, Color.WHITE);
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
        return R.drawable.icn_folder_light;
    }

    public int fileIcon() {
        return R.drawable.icn_file_light;
    }

    public boolean loadColor(String code, String color) {
        for (Colors col : Colors.values()) {
            if (col.code().equals(code)) {
                mapping.put(col, Color.parseColor(color));
                return true;
            }
        }
        return false;
    }
}
