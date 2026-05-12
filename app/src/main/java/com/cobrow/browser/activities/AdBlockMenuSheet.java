package com.cobrow.browser.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.cobrow.browser.R;
import com.cobrow.browser.engine.AdBlocker;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class AdBlockMenuSheet {
    private final Context ctx;
    private final WebView webView;
    private final SharedPreferences prefs;
    private final int adsBlocked;

    public AdBlockMenuSheet(Context ctx, WebView webView, SharedPreferences prefs, int adsBlocked) {
        this.ctx = ctx; this.webView = webView; this.prefs = prefs; this.adsBlocked = adsBlocked;
    }

    public void show() {
        BottomSheetDialog dialog = new BottomSheetDialog(ctx, R.style.BottomSheetStyle);
        dialog.setContentView(R.layout.sheet_adblock);

        AdBlocker ab = AdBlocker.getInstance();
        String domain = extractDomain(webView.getUrl());

        Switch swEnable = dialog.findViewById(R.id.switchAdblock);
        Switch swWhitelist = dialog.findViewById(R.id.switchWhitelist);
        Switch swCookies = dialog.findViewById(R.id.switchCookieNotices);
        Switch swThirdParty = dialog.findViewById(R.id.switchThirdParty);
        TextView tvCount = dialog.findViewById(R.id.tvBlockedCount);
        TextView tvDomain = dialog.findViewById(R.id.tvCurrentDomain);
        TextView tvRules = dialog.findViewById(R.id.tvFilterInfo);

        if (swEnable != null) swEnable.setChecked(ab.isEnabled());
        if (tvCount != null) tvCount.setText(String.valueOf(adsBlocked));
        if (tvDomain != null) tvDomain.setText(domain != null ? domain : "");
        if (swWhitelist != null && domain != null) swWhitelist.setChecked(ab.isWhitelisted(domain));
        if (swCookies != null) swCookies.setChecked(ab.isBlockCookieNotices());
        if (swThirdParty != null) swThirdParty.setChecked(ab.isBlockThirdPartyTrackers());
        if (tvRules != null) tvRules.setText(ab.getDomainRulesCount() + " domains · " + ab.getRegexRulesCount() + " rules");

        if (dialog.findViewById(R.id.rowAdblockToggle) != null)
            dialog.findViewById(R.id.rowAdblockToggle).setOnClickListener(v -> {
                boolean on = !ab.isEnabled();
                ab.setEnabled(on); prefs.edit().putBoolean("adblock", on).apply();
                if (swEnable != null) swEnable.setChecked(on);
                webView.reload();
            });

        if (dialog.findViewById(R.id.rowWhitelist) != null && domain != null) {
            final String d = domain;
            dialog.findViewById(R.id.rowWhitelist).setOnClickListener(v -> {
                boolean was = ab.isWhitelisted(d);
                if (was) ab.removeFromWhitelist(d); else ab.addToWhitelist(d);
                if (swWhitelist != null) swWhitelist.setChecked(!was);
                webView.reload();
            });
        }

        if (dialog.findViewById(R.id.rowCookieNotices) != null)
            dialog.findViewById(R.id.rowCookieNotices).setOnClickListener(v -> {
                boolean on = !ab.isBlockCookieNotices();
                ab.setBlockCookieNotices(on);
                if (swCookies != null) swCookies.setChecked(on);
                Toast.makeText(ctx, "Cookie notice blocking: " + (on ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
            });

        if (dialog.findViewById(R.id.rowThirdParty) != null)
            dialog.findViewById(R.id.rowThirdParty).setOnClickListener(v -> {
                boolean on = !ab.isBlockThirdPartyTrackers();
                ab.setBlockThirdPartyTrackers(on);
                if (swThirdParty != null) swThirdParty.setChecked(on);
                Toast.makeText(ctx, "3rd-party trackers: " + (on ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
            });

        if (dialog.findViewById(R.id.rowCustomRules) != null)
            dialog.findViewById(R.id.rowCustomRules).setOnClickListener(v -> {
                dialog.dismiss();
                showCustomRulesDialog(ab);
            });

        dialog.show();
    }

    private void showCustomRulesDialog(AdBlocker ab) {
        EditText et = new EditText(ctx);
        et.setHint("||example.com^\n||ads.site.com^");
        et.setMinLines(4);
        et.setPadding(48, 24, 48, 24);
        new AlertDialog.Builder(ctx)
            .setTitle("Custom Block Rules")
            .setMessage("One rule per line (AdBlock Plus format)")
            .setView(et)
            .setPositiveButton("Add", (d, w) -> {
                String[] lines = et.getText().toString().split("\n");
                int added = 0;
                for (String line : lines) {
                    line = line.trim();
                    if (!line.isEmpty()) { ab.addCustomRule(line); added++; }
                }
                Toast.makeText(ctx, added + " rules added", Toast.LENGTH_SHORT).show();
                webView.reload();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private String extractDomain(String url) {
        if (url == null) return null;
        try {
            int s = url.indexOf("://"); if (s < 0) return null; s += 3;
            int e = url.indexOf('/', s);
            return e < 0 ? url.substring(s) : url.substring(s, e);
        } catch (Exception e) { return null; }
    }
}
