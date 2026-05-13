package com.cobrow.browser.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cobrow.browser.R;
import com.cobrow.browser.adapters.UrlListAdapter;
import com.cobrow.browser.data.CobrowDatabase;
import com.cobrow.browser.data.DownloadItem;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class DownloadsActivity extends AppCompatActivity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private UrlListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        setTitle("Downloads");
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        RecyclerView rv = findViewById(R.id.recyclerView);
        rv.setLayoutManager(new LinearLayoutManager(this));

        loadData(rv);
    }

    private void loadData(RecyclerView rv) {
        executor.execute(() -> {
            List<DownloadItem> items = CobrowDatabase.get(this).downloadDao().getAll();
            runOnUiThread(() -> {
                adapter = new UrlListAdapter(
                        items.stream().map(i -> i.fileName + "\n" + i.url).collect(Collectors.toList()),
                        pos -> {
                            openFile(items.get(pos));
                        },
                        pos -> {
                            executor.execute(() -> {
                                CobrowDatabase.get(this).downloadDao().delete(items.get(pos));
                                runOnUiThread(() -> {
                                    loadData(rv);
                                    Toast.makeText(this, "Removed from list", Toast.LENGTH_SHORT).show();
                                });
                            });
                        }
                );
                rv.setAdapter(adapter);
            });
        });
    }

    private void openFile(DownloadItem item) {
        File file = new File(item.path);
        if (!file.exists()) {
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, item.mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Cannot open file", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
    @Override
    protected void onDestroy() { executor.shutdown(); super.onDestroy(); }
}
