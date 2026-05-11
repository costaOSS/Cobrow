package com.cobrow.browser.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.webkit.WebView;
import android.widget.Toast;

import com.cobrow.browser.R;
import com.cobrow.browser.engine.AdBlocker;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class BottomMenuSheet {
    private final Context ctx;
    private final WebView webView;
    private final SharedPreferences prefs;
    private final int adsBlocked;

    public BottomMenuSheet(Context ctx, WebView webView, SharedPreferences prefs, int adsBlocked) {
        this.ctx = ctx;
        this.webView = webView;
        this.prefs = prefs;
        this.adsBlocked = adsBlocked;
    }

    public void show() {
        BottomSheetDialog dialog = new BottomSheetDialog(ctx, R.style.BottomSheetStyle);
        dialog.setContentView(R.layout.sheet_menu);

        dialog.findViewById(R.id.menuBookmarks).setOnClickListener(v -> {
            ctx.startActivity(new Intent(ctx, BookmarksActivity.class));
            dialog.dismiss();
        });
        dialog.findViewById(R.id.menuHistory).setOnClickListener(v -> {
            ctx.startActivity(new Intent(ctx, HistoryActivity.class));
            dialog.dismiss();
        });
        dialog.findViewById(R.id.menuSettings).setOnClickListener(v -> {
            ctx.startActivity(new Intent(ctx, SettingsActivity.class));
            dialog.dismiss();
        });
        dialog.findViewById(R.id.menuShare).setOnClickListener(v -> {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT, webView.getUrl());
            ctx.startActivity(Intent.createChooser(share, "Share via"));
            dialog.dismiss();
        });
        dialog.findViewById(R.id.menuDesktop).setOnClickListener(v -> {
            String ua = webView.getSettings().getUserAgentString();
            if (ua.contains("Mobile")) {
                webView.getSettings().setUserAgentString(
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36");
                Toast.makeText(ctx, "Desktop mode ON", Toast.LENGTH_SHORT).show();
            } else {
                webView.getSettings().setUserAgentString(null);
                Toast.makeText(ctx, "Mobile mode ON", Toast.LENGTH_SHORT).show();
            }
            webView.reload();
            dialog.dismiss();
        });
        dialog.findViewById(R.id.menuAdblock).setOnClickListener(v -> {
            boolean current = AdBlocker.getInstance().isEnabled();
            AdBlocker.getInstance().setEnabled(!current);
            prefs.edit().putBoolean("adblock", !current).apply();
            Toast.makeText(ctx, "Ad Block: " + (!current ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
            webView.reload();
            dialog.dismiss();
        });
        dialog.findViewById(R.id.menuAddBookmark).setOnClickListener(v -> {
            if (ctx instanceof MainActivity) {
                ((MainActivity) ctx).addCurrentPageBookmark();
            }
            dialog.dismiss();
        });
        dialog.show();
    }
}
