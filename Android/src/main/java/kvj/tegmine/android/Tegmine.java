package kvj.tegmine.android;

import org.kvj.bravo7.ApplicationContext;
import org.kvj.bravo7.log.AndroidLogger;
import org.kvj.bravo7.log.Logger;

import kvj.tegmine.android.data.TegmineController;

/**
 * Created by kvorobyev on 2/13/15.
 */
public class Tegmine extends ApplicationContext {

    @Override
    protected void init() {
        Logger.setOutput(new AndroidLogger());
        publishBean(new TegmineController());
    }
}
