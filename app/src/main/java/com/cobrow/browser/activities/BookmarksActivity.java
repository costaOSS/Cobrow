package com.cobrow.browser.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cobrow.browser.R;
import com.cobrow.browser.adapters.UrlListAdapter;
import com.cobrow.browser.data.Bookmark;
import com.cobrow.browser.data.CobrowDatabase;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BookmarksActivity extends AppCompatActivity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        setTitle("Bookmarks");
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        RecyclerView rv = findViewById(R.id.recyclerView);
        rv.setLayoutManager(new LinearLayoutManager(this));

        executor.execute(() -> {
            List<Bookmark> items = CobrowDatabase.get(this).bookmarkDao().getAll();
            runOnUiThread(() -> {
                UrlListAdapter adapter = new UrlListAdapter(
                        items.stream().map(b -> b.title + "\n" + b.url).collect(java.util.stream.Collectors.toList()),
                        pos -> {
                            Intent i = new Intent(this, MainActivity.class);
                            i.setData(android.net.Uri.parse(items.get(pos).url));
                            startActivity(i);
                            finish();
                        },
                        pos -> {
                            executor.execute(() -> {
                                CobrowDatabase.get(this).bookmarkDao().delete(items.get(pos));
                                items.remove(pos);
                                runOnUiThread(() -> {
                                    adapter2[0].notifyItemRemoved(pos);
                                    Toast.makeText(this, "Removed", Toast.LENGTH_SHORT).show();
                                });
                            });
                        }
                );
                adapter2[0] = adapter;
                rv.setAdapter(adapter);
            });
        });
    }

    // Workaround for lambda capture
    private final UrlListAdapter[] adapter2 = new UrlListAdapter[1];

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
    @Override
    protected void onDestroy() { executor.shutdown(); super.onDestroy(); }
}
