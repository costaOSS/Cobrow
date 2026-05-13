package com.cobrow.browser.activities;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.cobrow.browser.R;
import com.cobrow.browser.data.Bookmark;
import com.cobrow.browser.data.CobrowDatabase;
import com.cobrow.browser.data.HistoryItem;
import com.cobrow.browser.engine.AdBlocker;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsActivity extends AppCompatActivity {

    private static final String[] SEARCH_NAMES = {"Google", "Bing", "DuckDuckGo", "Brave Search", "Ecosia", "Startpage"};
    private static final String[] SEARCH_URLS = {
            "https://www.google.com/search?q=",
            "https://www.bing.com/search?q=",
            "https://duckduckgo.com/?q=",
            "https://search.brave.com/search?q=",
            "https://www.ecosia.org/search?q=",
            "https://www.startpage.com/do/search?q="
    };

    private static final String[] FONT_NAMES = {"Serif", "Sans-serif", "Monospace"};
    private static final String[] FONT_VALUES = {"Georgia, serif", "sans-serif", "monospace"};

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<String> exportLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/json"),
            uri -> {
                if (uri != null) performExport(uri);
            });

    private final ActivityResultLauncher<String[]> importLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) performImport(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setTitle("Settings");
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        SharedPreferences prefs = getSharedPreferences("cobrow_prefs", MODE_PRIVATE);

        Switch switchAdblock = findViewById(R.id.switchAdblock);
        Switch switchJs = findViewById(R.id.switchJs);
        Switch switchDarkMode = findViewById(R.id.switchDarkMode);
        Spinner spinnerSearch = findViewById(R.id.spinnerSearchEngine);
        Spinner spinnerFont = findViewById(R.id.spinnerReaderFont);
        EditText etHomeUrl = findViewById(R.id.etHomeUrl);
        EditText etUserAgent = findViewById(R.id.etUserAgent);

        switchAdblock.setChecked(prefs.getBoolean("adblock", true));
        switchJs.setChecked(prefs.getBoolean("javascript", true));
        switchDarkMode.setChecked(prefs.getBoolean("night_mode", false));
        etHomeUrl.setText(prefs.getString("home_url", "cobrow://newtab"));
        etUserAgent.setText(prefs.getString("user_agent", ""));

        // Setup search engine spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, SEARCH_NAMES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSearch.setAdapter(adapter);

        String currentEngine = prefs.getString("search_engine", SEARCH_URLS[0]);
        for (int i = 0; i < SEARCH_URLS.length; i++) {
            if (SEARCH_URLS[i].equals(currentEngine)) {
                spinnerSearch.setSelection(i);
                break;
            }
        }

        // Setup font spinner
        ArrayAdapter<String> fontAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, FONT_NAMES);
        fontAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFont.setAdapter(fontAdapter);

        String currentFont = prefs.getString("reader_font", FONT_VALUES[0]);
        for (int i = 0; i < FONT_VALUES.length; i++) {
            if (FONT_VALUES[i].equals(currentFont)) {
                spinnerFont.setSelection(i);
                break;
            }
        }

        switchAdblock.setOnCheckedChangeListener((btn, checked) -> {
            prefs.edit().putBoolean("adblock", checked).apply();
            AdBlocker.getInstance().setEnabled(checked);
        });
        switchJs.setOnCheckedChangeListener((btn, checked) ->
                prefs.edit().putBoolean("javascript", checked).apply());
        switchDarkMode.setOnCheckedChangeListener((btn, checked) -> {
            prefs.edit().putBoolean("night_mode", checked).apply();
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                    checked ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES :
                            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        });

        findViewById(R.id.btnSaveSettings).setOnClickListener(v -> {
            prefs.edit()
                    .putString("home_url", etHomeUrl.getText().toString().trim())
                    .putString("user_agent", etUserAgent.getText().toString().trim())
                    .putString("search_engine", SEARCH_URLS[spinnerSearch.getSelectedItemPosition()])
                    .putString("reader_font", FONT_VALUES[spinnerFont.getSelectedItemPosition()])
                    .apply();
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
            finish();
        });

        findViewById(R.id.btnExport).setOnClickListener(v -> exportLauncher.launch("cobrow_backup.json"));
        findViewById(R.id.btnImport).setOnClickListener(v -> importLauncher.launch(new String[]{"application/json", "application/octet-stream"}));
    }

    private void performExport(Uri uri) {
        executor.execute(() -> {
            try {
                CobrowDatabase db = CobrowDatabase.get(this);
                List<Bookmark> bookmarks = db.bookmarkDao().getAll();
                List<HistoryItem> history = db.historyDao().getAll();

                JSONObject root = new JSONObject();
                JSONArray bArray = new JSONArray();
                for (Bookmark b : bookmarks) {
                    JSONObject o = new JSONObject();
                    o.put("title", b.title); o.put("url", b.url); o.put("folder", b.folder);
                    bArray.put(o);
                }
                root.put("bookmarks", bArray);

                JSONArray hArray = new JSONArray();
                for (HistoryItem h : history) {
                    JSONObject o = new JSONObject();
                    o.put("title", h.title); o.put("url", h.url);
                    hArray.put(o);
                }
                root.put("history", hArray);

                OutputStream os = getContentResolver().openOutputStream(uri);
                if (os != null) {
                    os.write(root.toString(2).getBytes());
                    os.close();
                    runOnUiThread(() -> Toast.makeText(this, "Backup exported successfully", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void performImport(Uri uri) {
        executor.execute(() -> {
            try {
                InputStream is = getContentResolver().openInputStream(uri);
                if (is == null) return;
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                is.close();

                JSONObject root = new JSONObject(sb.toString());
                CobrowDatabase db = CobrowDatabase.get(this);

                JSONArray bArray = root.optJSONArray("bookmarks");
                if (bArray != null) {
                    for (int i = 0; i < bArray.length(); i++) {
                        JSONObject o = bArray.getJSONObject(i);
                        Bookmark b = new Bookmark(o.getString("title"), o.getString("url"));
                        b.folder = o.optString("folder", null);
                        db.bookmarkDao().insert(b);
                    }
                }

                JSONArray hArray = root.optJSONArray("history");
                if (hArray != null) {
                    for (int i = 0; i < hArray.length(); i++) {
                        JSONObject o = hArray.getJSONObject(i);
                        db.historyDao().insert(new HistoryItem(o.getString("title"), o.getString("url")));
                    }
                }

                runOnUiThread(() -> Toast.makeText(this, "Backup imported successfully", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Import failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }

    @Override
    protected void onDestroy() { executor.shutdown(); super.onDestroy(); }
}
