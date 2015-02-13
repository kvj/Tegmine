package kvj.tegmine.android.infra;

import org.kvj.bravo7.SuperService;

import kvj.tegmine.android.Tegmine;
import kvj.tegmine.android.data.TegmineController;

/**
 * Created by kvorobyev on 2/13/15.
 */
public class ControllerService extends SuperService<TegmineController, Tegmine> {

    public ControllerService() {
        super(TegmineController.class, "Tegmine");
    }
}
