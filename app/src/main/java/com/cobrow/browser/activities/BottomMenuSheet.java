package com.cobrow.browser.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import com.cobrow.browser.R;
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

        MainActivity main = ctx instanceof MainActivity ? (MainActivity) ctx : null;

        // Row 1
        dialog.findViewById(R.id.menuAddBookmark).setOnClickListener(v -> {
            if (main != null) main.addCurrentPageBookmark();
            dialog.dismiss();
        });
        dialog.findViewById(R.id.menuBookmarks).setOnClickListener(v -> {
            ctx.startActivity(new Intent(ctx, BookmarksActivity.class));
            dialog.dismiss();
        });
        dialog.findViewById(R.id.menuHistory).setOnClickListener(v -> {
            ctx.startActivity(new Intent(ctx, HistoryActivity.class));
            dialog.dismiss();
        });
        dialog.findViewById(R.id.menuShare).setOnClickListener(v -> {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT, webView.getUrl());
            ctx.startActivity(Intent.createChooser(share, "Share via"));
            dialog.dismiss();
        });

        // Row 2
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
            dialog.dismiss();
            new AdBlockMenuSheet(ctx, webView, prefs, adsBlocked).show();
        });
        dialog.findViewById(R.id.menuIncognito).setOnClickListener(v -> {
            if (main != null) main.toggleIncognito();
            updateToggleLabel(dialog, R.id.labelIncognito, "Incognito",
                    main != null && main.isIncognito());
            dialog.dismiss();
        });
        dialog.findViewById(R.id.menuNightMode).setOnClickListener(v -> {
            if (main != null) main.toggleNightMode();
            dialog.dismiss();
        });

        // Row 3
        dialog.findViewById(R.id.menuReaderMode).setOnClickListener(v -> {
            if (main != null) main.showReaderMode();
            dialog.dismiss();
        });
        dialog.findViewById(R.id.menuViewSource).setOnClickListener(v -> {
            if (main != null) main.viewSource();
            dialog.dismiss();
        });
        dialog.findViewById(R.id.menuPageInfo).setOnClickListener(v -> {
            dialog.dismiss();
            if (main != null) main.showPageInfo();
        });
        dialog.findViewById(R.id.menuTextSize).setOnClickListener(v -> {
            dialog.dismiss();
            if (main != null) main.showTextSize();
        });

        // Row 4
        dialog.findViewById(R.id.menuFindInPage).setOnClickListener(v -> {
            dialog.dismiss();
            if (main != null) main.showFindInPage();
        });
        dialog.findViewById(R.id.menuSavePage).setOnClickListener(v -> {
            dialog.dismiss();
            if (main != null) main.savePageAsMhtml();
        });
        dialog.findViewById(R.id.menuClearData).setOnClickListener(v -> {
            dialog.dismiss();
            if (main != null) main.clearBrowsingData();
        });
        dialog.findViewById(R.id.menuSettings).setOnClickListener(v -> {
            ctx.startActivity(new Intent(ctx, SettingsActivity.class));
            dialog.dismiss();
        });

        // Row 5
        dialog.findViewById(R.id.menuDownloads).setOnClickListener(v -> {
            ctx.startActivity(new Intent(ctx, DownloadsActivity.class));
            dialog.dismiss();
        });
        dialog.findViewById(R.id.menuPrint).setOnClickListener(v -> {
            if (main != null) main.printPage();
            dialog.dismiss();
        });
        dialog.findViewById(R.id.menuScreenshot).setOnClickListener(v -> {
            if (main != null) main.captureScreenshot();
            dialog.dismiss();
        });
        dialog.findViewById(R.id.menuPrivacy).setOnClickListener(v -> {
            Intent intent = new Intent(ctx, PrivacyActivity.class);
            intent.putExtra("session_blocked", adsBlocked);
            ctx.startActivity(intent);
            dialog.dismiss();
        });

        // Reflect current states
        if (main != null) {
            updateToggleLabel(dialog, R.id.labelIncognito, "Incognito", main.isIncognito());
            updateToggleLabel(dialog, R.id.labelNightMode, "Night Mode", main.isNightMode());
        }

        dialog.show();
    }

    private void updateToggleLabel(BottomSheetDialog dialog, int id, String base, boolean on) {
        TextView tv = dialog.findViewById(id);
        if (tv != null) tv.setText(on ? base + " ✓" : base);
    }
}
