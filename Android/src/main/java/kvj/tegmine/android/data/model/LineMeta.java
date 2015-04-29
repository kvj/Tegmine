package kvj.tegmine.android.data.model;

/**
 * Created by kvorobyev on 3/12/15.
 */
public class LineMeta {
    private int indent = 0;
    private long start;
    private boolean folded = false;
    private boolean visible = true;
    private String data = null;

    public LineMeta(int indent, long start) {
        this.indent = indent;
        this.start = start;
    }

    public int indent() {
        return indent;
    }

    public long start() {
        return start;
    }

    public boolean folded() {
        return folded;
    }

    public boolean visible() {
        return visible;
    }

    public void folded(boolean folded) {
        this.folded = folded;
    }

    public void visible(boolean visible) {
        this.visible = visible;
    }

    public void data(String data) {
        this.data = data;
    }

    public String data() {
        return data;
    }

    @Override
    public String toString() {
        return String.format("LineMeta: %d %d", indent, start);
    }
}
