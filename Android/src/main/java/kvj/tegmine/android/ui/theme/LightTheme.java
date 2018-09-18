package kvj.tegmine.android.ui.theme;

import android.graphics.Color;

import org.kvj.bravo7.log.Logger;

import java.util.HashMap;
import java.util.Map;

import kvj.tegmine.android.R;
import kvj.tegmine.android.data.TegmineController;

/**
 * Created by kvorobyev on 2/14/15.
 */
public class LightTheme {

    private final TegmineController controller;
    private Logger logger = Logger.forInstance(this);

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
    }

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

    public int refreshIcon() {
        return dark? R.drawable.ic_widget_refresh_dk: R.drawable.ic_widget_refresh_lt;
    }

    public int configIcon() {
        return dark? R.drawable.ic_widget_conf_dk: R.drawable.ic_widget_conf_lt;
    }

    public int addIcon() {
        return dark? R.drawable.ic_widget_add_dk: R.drawable.ic_widget_add_lt;
    }

    public int editIcon() {
        return dark? R.drawable.ic_widget_edit_dk: R.drawable.ic_widget_edit_lt;
    }

    public int saveIcon() {
        return dark? R.drawable.ic_action_save_dk: R.drawable.ic_action_save_lt;
    }

    public int voiceIcon() {
        return dark? R.drawable.ic_action_voice_dk: R.drawable.ic_action_voice_lt;
    }

    public boolean loadColor(Colors col, String color) {
        if (null != col) {
            int attr = key2attr(color);
            if (-1 != attr) { // Parsed
                try {
                    int colorVal = controller.context().getResources().getColor(attr);
                    mappingColors.put(col, colorVal);
                } catch (Exception e) {
                    logger.w("Not resolved:", color, attr);
                }

            }
            if (!color.startsWith("#")) { // Ignore
                return false;
            }
            mappingColors.put(col, Color.parseColor(color));
            return true;
        }
        return false;
    }

    private int key2attr(String key) {
        if (!key.startsWith("attr_")) { // Invalid
            return -1;
        }
        String name = key.substring(5);
        if ("textDark".equals(name)) {
            return android.support.v7.appcompat.R.color.primary_text_default_material_dark;
        }
        if ("textLight".equals(name)) {
            return android.support.v7.appcompat.R.color.primary_text_default_material_light;
        }
        if ("bgDark".equals(name)) {
            return android.support.v7.appcompat.R.color.background_material_dark;
        }
        if ("bgLight".equals(name)) {
            return android.support.v7.appcompat.R.color.background_material_light;
        }
        if ("bgDarkAlt".equals(name)) {
            return android.support.v7.appcompat.R.color.background_floating_material_dark;
        }
        if ("bgLightAlt".equals(name)) {
            return android.support.v7.appcompat.R.color.background_floating_material_light;
        }
        if ("fgDark".equals(name)) {
            return android.support.v7.appcompat.R.color.foreground_material_dark;
        }
        if ("fgLight".equals(name)) {
            return android.support.v7.appcompat.R.color.foreground_material_light;
        }
        if ("textDarkAlt".equals(name)) {
            return android.support.v7.appcompat.R.color.secondary_text_default_material_dark;
        }
        if ("textLightAlt".equals(name)) {
            return android.support.v7.appcompat.R.color.secondary_text_default_material_light;
        }
        return -1;
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
