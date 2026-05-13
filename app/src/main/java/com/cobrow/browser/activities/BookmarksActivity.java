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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BookmarksActivity extends AppCompatActivity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private UrlListAdapter adapter;
    private List<Bookmark> currentBookmarks;
    private List<String> currentFolders;
    private String currentFolder = null; // null means root

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        updateTitle();
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        RecyclerView rv = findViewById(R.id.recyclerView);
        rv.setLayoutManager(new LinearLayoutManager(this));

        loadData(rv);
    }

    private void updateTitle() {
        setTitle(currentFolder == null ? "Bookmarks" : "📁 " + currentFolder);
    }

    private void loadData(RecyclerView rv) {
        executor.execute(() -> {
            CobrowDatabase db = CobrowDatabase.get(this);
            if (currentFolder == null) {
                currentFolders = db.bookmarkDao().getFolders();
                currentBookmarks = db.bookmarkDao().getRoot();
            } else {
                currentFolders = new ArrayList<>(); // no subfolders for now
                currentBookmarks = db.bookmarkDao().getByFolder(currentFolder);
            }

            List<String> items = new ArrayList<>();
            for (String f : currentFolders) items.add("📁 " + f + "\nFolder");
            for (Bookmark b : currentBookmarks) items.add(b.title + "\n" + b.url);

            runOnUiThread(() -> {
                adapter = new UrlListAdapter(items,
                        pos -> {
                            if (pos < currentFolders.size()) {
                                currentFolder = currentFolders.get(pos);
                                updateTitle();
                                loadData(rv);
                            } else {
                                int bookmarkPos = pos - currentFolders.size();
                                Intent i = new Intent(this, MainActivity.class);
                                i.setData(android.net.Uri.parse(currentBookmarks.get(bookmarkPos).url));
                                startActivity(i);
                                finish();
                            }
                        },
                        pos -> {
                            if (pos < currentFolders.size()) {
                                Toast.makeText(this, "Cannot delete folders here", Toast.LENGTH_SHORT).show();
                            } else {
                                int bookmarkPos = pos - currentFolders.size();
                                executor.execute(() -> {
                                    db.bookmarkDao().delete(currentBookmarks.get(bookmarkPos));
                                    currentBookmarks.remove(bookmarkPos);
                                    runOnUiThread(() -> {
                                        loadData(rv); // Reload to simplify
                                        Toast.makeText(this, "Removed", Toast.LENGTH_SHORT).show();
                                    });
                                });
                            }
                        }
                );
                rv.setAdapter(adapter);
            });
        });
    }

    @Override
    public void onBackPressed() {
        if (currentFolder != null) {
            currentFolder = null;
            updateTitle();
            loadData(findViewById(R.id.recyclerView));
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (currentFolder != null) {
            currentFolder = null;
            updateTitle();
            loadData(findViewById(R.id.recyclerView));
            return true;
        }
        finish();
        return true;
    }

    @Override
    protected void onDestroy() { executor.shutdown(); super.onDestroy(); }
}
