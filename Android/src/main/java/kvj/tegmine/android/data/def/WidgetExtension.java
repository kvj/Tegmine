package kvj.tegmine.android.data.def;

import java.util.List;

import kvj.tegmine.android.data.model.LineMeta;

/**
 * Created by vorobyev on 9/30/16.
 */

public interface WidgetExtension {

    public String title();

    public void process(FileSystemItem item, List<LineMeta> lines, int id);
}
