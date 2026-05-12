package com.cobrow.browser.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.cobrow.browser.R;

public class WelcomeActivity extends AppCompatActivity {

    private static final String PREF_ACCEPTED = "terms_accepted";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("cobrow_prefs", MODE_PRIVATE);
        if (prefs.getBoolean(PREF_ACCEPTED, false)) {
            launch();
            return;
        }

        setContentView(R.layout.activity_welcome);

        findViewById(R.id.btnAgree).setOnClickListener(v -> {
            prefs.edit().putBoolean(PREF_ACCEPTED, true).apply();
            launch();
        });

        findViewById(R.id.btnDisagree).setOnClickListener(v ->
            new AlertDialog.Builder(this)
                .setTitle("Exit")
                .setMessage("You must agree to the Terms of Use and Privacy Policy to use Cobrow.")
                .setPositiveButton("Exit", (d, w) -> finishAffinity())
                .setNegativeButton("Back", null)
                .show()
        );
    }

    private void launch() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
