package com.cobrow.browser.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.cobrow.browser.R;
import com.cobrow.browser.engine.AdBlocker;

public class SettingsActivity extends AppCompatActivity {

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
        EditText etHomeUrl = findViewById(R.id.etHomeUrl);
        EditText etUserAgent = findViewById(R.id.etUserAgent);

        switchAdblock.setChecked(prefs.getBoolean("adblock", true));
        switchJs.setChecked(prefs.getBoolean("javascript", true));
        switchDarkMode.setChecked(prefs.getBoolean("dark_mode", false));
        etHomeUrl.setText(prefs.getString("home_url", "https://www.google.com"));
        etUserAgent.setText(prefs.getString("user_agent", ""));

        switchAdblock.setOnCheckedChangeListener((btn, checked) -> {
            prefs.edit().putBoolean("adblock", checked).apply();
            AdBlocker.getInstance().setEnabled(checked);
        });
        switchJs.setOnCheckedChangeListener((btn, checked) ->
                prefs.edit().putBoolean("javascript", checked).apply());
        switchDarkMode.setOnCheckedChangeListener((btn, checked) ->
                prefs.edit().putBoolean("dark_mode", checked).apply());

        findViewById(R.id.btnSaveSettings).setOnClickListener(v -> {
            prefs.edit()
                    .putString("home_url", etHomeUrl.getText().toString().trim())
                    .putString("user_agent", etUserAgent.getText().toString().trim())
                    .apply();
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}
