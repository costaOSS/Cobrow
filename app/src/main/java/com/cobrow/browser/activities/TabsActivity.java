package com.cobrow.browser.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.cobrow.browser.R;
import com.cobrow.browser.data.CobrowDatabase;
import com.cobrow.browser.data.HistoryItem;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TabsActivity extends AppCompatActivity {

    private ListView listTabs;
    private List<HistoryItem> items = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tabs);

        listTabs = findViewById(R.id.listTabs);

        executor.execute(() -> {
            List<HistoryItem> list = CobrowDatabase.get(this).historyDao().getAll();
            runOnUiThread(() -> {
                if (list == null || list.isEmpty()) {
                    setResult(RESULT_CANCELED);
                    finish();
                    return;
                }
                items = list;
                ArrayList<String> display = new ArrayList<>();
                for (HistoryItem h : list) display.add(h.title != null && !h.title.isEmpty() ? h.title : h.url);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, display);
                listTabs.setAdapter(adapter);
                listTabs.setOnItemClickListener((parent, view, position, id) -> {
                    HistoryItem hi = items.get(position);
                    Intent data = new Intent();
                    data.setData(Uri.parse(hi.url));
                    setResult(RESULT_OK, data);
                    finish();
                });
            });
        });
    }
}
