package com.cobrow.browser.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.WebView;
import android.widget.Switch;
import android.widget.TextView;

import com.cobrow.browser.R;
import com.cobrow.browser.engine.AdBlocker;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class AdBlockMenuSheet {
    private final Context ctx;
    private final WebView webView;
    private final SharedPreferences prefs;
    private final int adsBlocked;

    public AdBlockMenuSheet(Context ctx, WebView webView, SharedPreferences prefs, int adsBlocked) {
        this.ctx = ctx;
        this.webView = webView;
        this.prefs = prefs;
        this.adsBlocked = adsBlocked;
    }

    public void show() {
        BottomSheetDialog dialog = new BottomSheetDialog(ctx, R.style.BottomSheetStyle);
        dialog.setContentView(R.layout.sheet_adblock);

        AdBlocker adBlocker = AdBlocker.getInstance();
        String currentUrl = webView.getUrl();
        String domain = extractDomain(currentUrl);

        Switch switchAdblock = dialog.findViewById(R.id.switchAdblock);
        Switch switchWhitelist = dialog.findViewById(R.id.switchWhitelist);
        TextView tvBlockedCount = dialog.findViewById(R.id.tvBlockedCount);
        TextView tvCurrentDomain = dialog.findViewById(R.id.tvCurrentDomain);

        if (switchAdblock != null) switchAdblock.setChecked(adBlocker.isEnabled());
        if (tvBlockedCount != null) tvBlockedCount.setText(String.valueOf(adsBlocked));
        if (tvCurrentDomain != null) tvCurrentDomain.setText(domain != null ? domain : "");
        if (switchWhitelist != null && domain != null)
            switchWhitelist.setChecked(adBlocker.isWhitelisted(domain));

        // Toggle adblock enable/disable
        if (dialog.findViewById(R.id.rowAdblockToggle) != null) {
            dialog.findViewById(R.id.rowAdblockToggle).setOnClickListener(v -> {
                boolean newState = !adBlocker.isEnabled();
                adBlocker.setEnabled(newState);
                prefs.edit().putBoolean("adblock", newState).apply();
                if (switchAdblock != null) switchAdblock.setChecked(newState);
                webView.reload();
            });
        }

        // Whitelist toggle
        if (dialog.findViewById(R.id.rowWhitelist) != null && domain != null) {
            final String finalDomain = domain;
            dialog.findViewById(R.id.rowWhitelist).setOnClickListener(v -> {
                boolean nowWhitelisted = adBlocker.isWhitelisted(finalDomain);
                if (nowWhitelisted) {
                    adBlocker.removeFromWhitelist(finalDomain);
                } else {
                    adBlocker.addToWhitelist(finalDomain);
                }
                if (switchWhitelist != null) switchWhitelist.setChecked(!nowWhitelisted);
                webView.reload();
            });
        }

        dialog.show();
    }

    private String extractDomain(String url) {
        if (url == null) return null;
        try {
            int start = url.indexOf("://");
            if (start == -1) return null;
            start += 3;
            int end = url.indexOf('/', start);
            return end == -1 ? url.substring(start) : url.substring(start, end);
        } catch (Exception e) {
            return null;
        }
    }
}
