package com.cobrow.browser.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.cobrow.browser.R;

public class PrivacyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy);
        setTitle("Privacy Dashboard");
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        SharedPreferences prefs = getSharedPreferences("cobrow_prefs", MODE_PRIVATE);
        int lifetime = prefs.getInt("blocked_lifetime", 0);
        int session = getIntent().getIntExtra("session_blocked", 0);

        TextView tvLifetime = findViewById(R.id.tvBlockedLifetime);
        TextView tvSession = findViewById(R.id.tvBlockedSession);
        TextView tvDataSaved = findViewById(R.id.tvDataSaved);

        tvLifetime.setText(String.valueOf(lifetime));
        tvSession.setText("Session: " + session);

        // Estimate data saved: ~50KB per ad/tracker
        double mbSaved = (lifetime * 50.0) / 1024.0;
        if (mbSaved > 1024) {
            tvDataSaved.setText(String.format("%.2f GB", mbSaved / 1024.0));
        } else {
            tvDataSaved.setText(String.format("%.2f MB", mbSaved));
        }
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}
