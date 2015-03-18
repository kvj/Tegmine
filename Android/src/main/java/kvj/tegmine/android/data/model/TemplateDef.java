package kvj.tegmine.android.data.model;

/**
 * Created by kvorobyev on 3/8/15.
 */
public class TemplateDef {

    private String code;
    private String label = null;
    private String template;
    private String key = null;

    public TemplateDef(String code, String template) {
        this.code = code;
        this.template = template;
    }

    public String code() {
        return code;
    }

    public String template() {
        return template;
    }

    public void label(String label) {
        this.label = label;
    }

    public String label() {
        return label != null ? label: code;
    }

    public void key(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
