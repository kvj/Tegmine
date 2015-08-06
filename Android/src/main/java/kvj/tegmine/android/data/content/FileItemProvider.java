package kvj.tegmine.android.data.content;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Parcel;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;

import org.kvj.bravo7.log.Logger;
import org.kvj.bravo7.ng.App;

import java.util.ArrayList;
import java.util.List;

import kvj.tegmine.android.data.TegmineController;
import kvj.tegmine.android.data.def.FileSystemException;
import kvj.tegmine.android.data.def.FileSystemItem;
import kvj.tegmine.android.data.model.LineMeta;
import kvj.tegmine.android.data.model.SyntaxDef;
import kvj.tegmine.android.data.model.util.Wrappers;

/**
 * Created by vorobyev on 8/3/15.
 */
public class FileItemProvider extends ContentProvider {

    private Logger logger = Logger.forInstance(this);

    private static final String AUTHORITY = "kvj.tegmine.contents";
    private UriMatcher uriMatcher = null;

    @Override
    public boolean onCreate() {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, "*", 1);
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] fields, String where, String[] whereArg, String order) {
        TegmineController controller = App.controller();
        if (null == controller) {
            // Cold start
            controller = new TegmineController(getContext());
        }
        if (uriMatcher.match(uri) != 1) {
            throw new IllegalArgumentException("Unknown URI:" + uri);
        }
        String condition = null;
        int colonPos = where.indexOf(":");
        if (colonPos != -1) {
            condition = where.substring(colonPos+1);
            where = where.substring(0, colonPos);
        }
        String url = String.format("%s://%s", uri.getPath().substring(1), where);
        logger.d("Query:", uri, uri.getPath(), uri.getQuery(), where, whereArg, condition);
        FileSystemItem item = controller.fromURL(url);
        if (null == item) {
            // Not accessible
            logger.e("Item not found:", url);
            throw new IllegalArgumentException("Item not found:" + url+", "+uri);
        }
        SyntaxDef syntax = controller.findSyntax(item);
        List<LineMeta> buffer = new ArrayList<>();
        try {
            controller.loadFilePart(buffer, item, 0, -1);
        } catch (FileSystemException e) {
            logger.e(e, "Failed to read:", item);
            throw new IllegalArgumentException("IO error:" + item);
        }
        logger.d("Lines:", buffer.size(), condition);
        MatrixCursor cursor = new MatrixCursor(fields);
        int index = 0;
        Wrappers.Pair<Integer> frame = controller.findIn(buffer, condition);
        logger.d("Result:", frame.v1(), frame.v2());
        int indent = 0;
        if (frame.v2() > 0) {
            indent = buffer.get(frame.v1()).indent();
        }
        for (int idx = frame.v1(); idx < frame.v1()+frame.v2(); idx++) {
            LineMeta line = buffer.get(idx);
            Object[] row = new Object[fields.length];
            StringBuilder sb = new StringBuilder();
            controller.addIndent(sb, line.indent() - indent);
            sb.append(line.data());
            for (int i = 0; i < fields.length; i++) {
                Object value = null;
                if ("id".equals(fields[i])) {
                    value = index++;
                }
                if ("text".equals(fields[i])) {
                    value = sb.toString();
                }
                if ("colored".equals(fields[i])) {
                    SpannableStringBuilder b = new SpannableStringBuilder();
                    controller.applyTheme(syntax, sb.toString(), b, SyntaxDef.Feature.Shrink);
                    Parcel p = Parcel.obtain();
                    TextUtils.writeToParcel(b, p, 0);
                    p.setDataPosition(0);
                    value = p.marshall();
                }
                row[i] = value;
            }
            cursor.addRow(row);
        }
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        return null;
    }

    @Override
    public int delete(Uri uri, String s, String[] strings) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String s, String[] strings) {
        return 0;
    }
}
