package com.cobrow.browser.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.cobrow.browser.R;
import com.cobrow.browser.data.Tab;
import com.cobrow.browser.data.TabsManager;

import java.util.ArrayList;
import java.util.List;

public class TabsActivity extends AppCompatActivity {

    private ListView listTabs;
    private Button btnAddTab;
    private TabsManager tabsManager;
    private List<Tab> items = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tabs);

        tabsManager = new TabsManager(this);
        listTabs = findViewById(R.id.listTabs);
        btnAddTab = findViewById(R.id.btnAddTab);

        refreshList();

        btnAddTab.setOnClickListener(v -> {
            tabsManager.addTab(new Tab("", tabsManager.getTabs().isEmpty() ? "https://www.google.com" : tabsManager.getTabs().get(tabsManager.getTabs().size() - 1).url));
            refreshList();
        });

        listTabs.setOnItemClickListener((parent, view, position, id) -> {
            Intent data = new Intent();
            data.putExtra("selected_index", position);
            setResult(RESULT_OK, data);
            finish();
        });

        listTabs.setOnItemLongClickListener((parent, view, position, id) -> {
            new AlertDialog.Builder(this)
                    .setTitle("Close tab")
                    .setMessage("Close this tab?")
                    .setPositiveButton("Close", (dialog, which) -> {
                        tabsManager.removeTab(position);
                        refreshList();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });
    }

    private void refreshList() {
        items = tabsManager.getTabs();
        ArrayList<String> display = new ArrayList<>();
        for (Tab t : items) display.add((t.title != null && !t.title.isEmpty()) ? t.title : t.url);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, display);
        listTabs.setAdapter(adapter);
    }
}
