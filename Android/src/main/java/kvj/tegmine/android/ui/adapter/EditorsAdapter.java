package kvj.tegmine.android.ui.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import org.kvj.bravo7.log.Logger;
import org.kvj.bravo7.ng.App;

import kvj.tegmine.android.data.TegmineController;
import kvj.tegmine.android.data.model.EditorInfo;
import kvj.tegmine.android.ui.fragment.OneEditor;

/**
 * Created by kvorobyev on 5/5/15.
 */
public class EditorsAdapter extends PagerAdapter {

    private final FragmentManager fm;
    private Logger logger = Logger.forInstance(this);

    private TegmineController controller = App.controller();

    public EditorsAdapter(FragmentManager fm) {
        super();
        this.fm = fm;
    }

    @Override
    public OneEditor instantiateItem(ViewGroup container, int position) {
        EditorInfo info = controller.editors().tab(position);
        OneEditor editor = new OneEditor().create(info, position);
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.add(container.getId(), editor);
        transaction.commit();
        return editor;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.detach((Fragment) object);
        transaction.commit();
    }

    @Override
    public int getCount() {
        return controller.editors().size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        OneEditor editor = (OneEditor) object;
        return editor.getView() == view;
    }

    @Override
    public int getItemPosition(Object object) {
        OneEditor editor = (OneEditor) object;
        if (editor.info().mode == EditorInfo.Mode.None) { // Already removed
            return POSITION_NONE;
        }
        int pos = controller.editors().position(editor.info());
        if (-1 == pos) { // Not found
            return POSITION_NONE;
        }
        return pos;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        EditorInfo info = controller.editors().tab(position);
        return String.format("%s", info.title);
    }

}
