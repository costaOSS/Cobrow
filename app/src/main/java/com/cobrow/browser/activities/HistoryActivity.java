package com.cobrow.browser.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cobrow.browser.R;
import com.cobrow.browser.adapters.UrlListAdapter;
import com.cobrow.browser.data.CobrowDatabase;
import com.cobrow.browser.data.HistoryItem;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class HistoryActivity extends AppCompatActivity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private List<HistoryItem> items;
    private UrlListAdapter adapter;
    private String currentQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        setTitle("History");
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        RecyclerView rv = findViewById(R.id.recyclerView);
        rv.setLayoutManager(new LinearLayoutManager(this));

        SearchView searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }
            @Override public boolean onQueryTextChange(String newText) {
                currentQuery = newText.trim();
                loadData(rv);
                return true;
            }
        });

        loadData(rv);
    }

    private void loadData(RecyclerView rv) {
        executor.execute(() -> {
            if (currentQuery.isEmpty()) {
                items = CobrowDatabase.get(this).historyDao().getAll();
            } else {
                items = CobrowDatabase.get(this).historyDao().search(currentQuery);
            }
            runOnUiThread(() -> {
                adapter = new UrlListAdapter(
                        items.stream().map(h -> h.title + "\n" + h.url).collect(Collectors.toList()),
                        pos -> {
                            Intent i = new Intent(this, MainActivity.class);
                            i.setData(android.net.Uri.parse(items.get(pos).url));
                            startActivity(i);
                            finish();
                        },
                        null
                );
                rv.setAdapter(adapter);
            });
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "Clear History").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            executor.execute(() -> {
                CobrowDatabase.get(this).historyDao().clearAll();
                runOnUiThread(() -> {
                    items.clear();
                    adapter.notifyDataSetChanged();
                });
            });
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
    @Override
    protected void onDestroy() { executor.shutdown(); super.onDestroy(); }
}
