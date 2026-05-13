package com.cobrow.browser.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.cobrow.browser.R;
import com.cobrow.browser.engine.AdBlocker;

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
        EditText etHomeUrl = findViewById(R.id.etHomeUrl);
        EditText etUserAgent = findViewById(R.id.etUserAgent);

        switchAdblock.setChecked(prefs.getBoolean("adblock", true));
        switchJs.setChecked(prefs.getBoolean("javascript", true));
        switchDarkMode.setChecked(prefs.getBoolean("night_mode", false));
        etHomeUrl.setText(prefs.getString("home_url", "https://www.google.com"));
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
                    .apply();
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}
