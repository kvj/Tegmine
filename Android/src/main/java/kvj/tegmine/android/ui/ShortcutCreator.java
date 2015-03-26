package kvj.tegmine.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import org.kvj.bravo7.ControllerConnector;
import org.kvj.bravo7.SuperActivity;
import org.kvj.bravo7.form.FormController;
import org.kvj.bravo7.form.impl.widget.SpinnerIntegerAdapter;
import org.kvj.bravo7.form.impl.widget.TextViewStringAdapter;
import org.kvj.bravo7.log.Logger;

import java.util.Map;

import kvj.tegmine.android.R;
import kvj.tegmine.android.Tegmine;
import kvj.tegmine.android.data.TegmineController;
import kvj.tegmine.android.data.def.FileSystemException;
import kvj.tegmine.android.data.def.FileSystemItem;
import kvj.tegmine.android.data.model.TemplateDef;
import kvj.tegmine.android.infra.ControllerService;
import kvj.tegmine.android.ui.dialog.FileChooser;
import kvj.tegmine.android.ui.form.FileSystemItemWidgetAdapter;
import kvj.tegmine.android.ui.fragment.FileSystemBrowser;
import kvj.tegmine.android.ui.fragment.MainPreferences;

/**
 * Created by kvorobyev on 2/26/15.
 */
public class ShortcutCreator extends FragmentActivity implements ControllerConnector.ControllerReceiver<TegmineController>,FileSystemBrowser.BrowserListener {
    private ControllerConnector<Tegmine, TegmineController, ControllerService> conn = new ControllerConnector<>(this, this);

    private Logger logger = Logger.forInstance(this);
    private TegmineController controller = null;
    private Spinner typeSpinner = null;
    private Spinner templateSpinner = null;
    private FormController form = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shortcut);
        form = new FormController(findViewById(R.id.shortcut_root));
        form.add(new SpinnerIntegerAdapter(R.id.shortcut_type_spinner, 0), "type");
        form.add(new SpinnerIntegerAdapter(R.id.shortcut_template_spinner, 0), "template");
        form.add(new TextViewStringAdapter(R.id.shortcut_title, ""), "title");
        typeSpinner = (Spinner) findViewById(R.id.shortcut_type_spinner);
        ArrayAdapter<CharSequence> typeAdapter =
            ArrayAdapter.createFromResource(this, R.array.shortcut_types, android.R.layout.simple_spinner_item);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(typeAdapter);
        templateSpinner = (Spinner) findViewById(R.id.shortcut_template_spinner);
        Button finishButton = (Button) findViewById(R.id.shortcut_do_create);
        finishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doFinish();
            }
        });
    }

    private void doFinish() {
        // Check input and create shortcut
        FileSystemItem selected = form.getValue("selected", FileSystemItem.class);
        if (null == selected) {
            SuperActivity.notifyUser(this, "File not selected");
            return;
        }
        String title = form.getValue("title", String.class);
        if (TextUtils.isEmpty(title)) {
            SuperActivity.notifyUser(this, "Title is required");
            return;
        }
        int mode = form.getValue("type", Integer.class);
        int template = form.getValue("template", Integer.class);
        logger.d("Finishing:", selected.toURL(), title, mode, template);
        Intent.ShortcutIconResource icon =
            Intent.ShortcutIconResource.fromContext(this, R.mipmap.ic_launcher);
        Intent intent = new Intent();
        Intent launchIntent = new Intent(this, Main.class);
        if (mode == 0) {
            launchIntent.putExtra(Tegmine.BUNDLE_VIEW_TYPE, Tegmine.VIEW_TYPE_FILE);
        } else {
            launchIntent.putExtra(Tegmine.BUNDLE_VIEW_TYPE, Tegmine.VIEW_TYPE_EDITOR);
        }
        if (mode == 1) {
            // Append
            launchIntent.putExtra(Tegmine.BUNDLE_EDIT_TYPE, Tegmine.EDIT_TYPE_ADD);
            if (template > 0) {
                // Template selected
                launchIntent.putExtra(Tegmine.BUNDLE_EDIT_TEMPLATE, controller.templates().keySet().toArray(new String[0])[template-1]);
            }
        }
        if (mode == 2) {
            // Edit file
            launchIntent.putExtra(Tegmine.BUNDLE_EDIT_TYPE, Tegmine.EDIT_TYPE_EDIT);
        }
        Bundle itemsBundle = new Bundle();
        itemsBundle.putString("select", selected.toURL());
        launchIntent.putExtras(itemsBundle);
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, launchIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, title);
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onController(TegmineController controller) {
        if (null == controller) {
            return;
        }
        if (this.controller == null) {
            form.add(new FileSystemItemWidgetAdapter(controller), "selected");
            findViewById(R.id.shortcut_file_selector).setBackgroundColor(controller.theme().backgroundColor());
            Bundle data = new Bundle();
            FileSystemBrowser
                browser = new FileSystemBrowser().create(controller, data).setListener(this);
            getSupportFragmentManager().beginTransaction().replace(R.id.shortcut_file_selector, browser).commit();
            ArrayAdapter<CharSequence> templateAdapter =
                new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item);
            templateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            templateAdapter.add("No template");
            for (Map.Entry<String, TemplateDef> tmpl : controller.templates().entrySet()) {
                templateAdapter.add(tmpl.getValue().label());
            }
            templateSpinner.setAdapter(templateAdapter);
            form.load(this, data);
        }
        this.controller = controller;
    }

    @Override
    protected void onStart() {
        super.onStart();
        conn.connectController(ControllerService.class);
    }

    @Override
    protected void onStop() {
        super.onStop();
        conn.disconnectController();
    }

    @Override
    public void openNewWindow(Bundle data) {
        // Ignore
    }

    @Override
    public void openFile(Bundle data, FileSystemItem item) {
        form.setValue("selected", item);
        if (TextUtils.isEmpty(form.getValue("title", String.class))) {
            // Put file name
            form.setValue("title", item.name);
        }
    }
}
