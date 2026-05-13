package com.cobrow.browser.activities;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import com.cobrow.browser.R;
import com.cobrow.browser.adapters.UrlSuggestionsAdapter;
import com.cobrow.browser.data.CobrowDatabase;
import com.cobrow.browser.data.HistoryItem;
import com.cobrow.browser.engine.AdBlocker;
import com.cobrow.browser.engine.CredentialManager;
import com.cobrow.browser.utils.UrlUtils;
import android.view.GestureDetector;
import android.view.MotionEvent;
import com.cobrow.browser.data.TabsManager;
import com.cobrow.browser.data.Tab;

import java.io.ByteArrayInputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private AutoCompleteTextView urlBar;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefresh;
    private ImageButton btnBack, btnForward, btnRefresh, btnHome, btnTabs, btnMenu;
    private TextView blockedCount;

    // Tabs
    private TabsManager tabsManager;
    private int currentTabIndex = 0;
    private GestureDetector gestureDetector;

    // Find in page
    private LinearLayout findBar;
    private EditText etFind;
    private TextView tvFindCount;
    private ImageButton btnFindPrev, btnFindNext, btnFindClose;

    private SharedPreferences prefs;
    private int adsBlocked = 0;
    private boolean isIncognito = false;
    private CredentialManager credentialManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Track current page host safely (updated on main thread)
    private volatile String currentPageHost = null;
    private static final String HOME_URL = "cobrow://newtab";
    private static final String PREF_HOME = "home_url";
    private static final String PREF_UA = "user_agent";
    private static final String PREF_JS = "javascript";
    private static final String PREF_ADBLOCK = "adblock";
    private static final String PREF_NIGHT = "night_mode";
    private static final String PREF_TEXT_SIZE = "text_size";

    private static final String PREF_SEARCH_ENGINE = "search_engine";
    private static final String DEFAULT_SEARCH_URL = "https://www.google.com/search?q=";

    // Night mode CSS injection
    private static final String NIGHT_MODE_JS =
        "javascript:(function(){" +
        "var s=document.createElement('style');" +
        "s.id='cobrow-night';" +
        "s.innerHTML='html{filter:invert(1) hue-rotate(180deg) !important;}" +
        "img,video,canvas,iframe{filter:invert(1) hue-rotate(180deg) !important;}';" +
        "document.head.appendChild(s);})()";
    private static final String NIGHT_MODE_REMOVE_JS =
        "javascript:(function(){var s=document.getElementById('cobrow-night');if(s)s.remove();})()";

    private LinearLayout topBar, bottomBar;
    private boolean isToolbarVisible = true;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("cobrow_prefs", MODE_PRIVATE);

        // Apply night mode
        if (prefs.getBoolean(PREF_NIGHT, false)) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
        }

        webView = findViewById(R.id.webView);
        urlBar = findViewById(R.id.urlBar);
        progressBar = findViewById(R.id.progressBar);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        btnBack = findViewById(R.id.btnBack);
        btnForward = findViewById(R.id.btnForward);
        btnRefresh = findViewById(R.id.btnRefresh);
        btnHome = findViewById(R.id.btnHome);
        btnTabs = findViewById(R.id.btnTabs);
        btnMenu = findViewById(R.id.btnMenu);
        blockedCount = findViewById(R.id.blockedCount);
        findBar = findViewById(R.id.findBar);
        etFind = findViewById(R.id.etFind);
        tvFindCount = findViewById(R.id.tvFindCount);
        btnFindPrev = findViewById(R.id.btnFindPrev);
        btnFindNext = findViewById(R.id.btnFindNext);
        btnFindClose = findViewById(R.id.btnFindClose);

        topBar = (LinearLayout) urlBar.getParent();
        bottomBar = (LinearLayout) btnBack.getParent();

        setupWebView();
        setupUrlBar();
        setupButtons();
        setupSwipeRefresh();
        setupFindInPage();
        setupFullscreenScroll();
        setupLongClickMenu();

        // Initialize tabs
        tabsManager = new TabsManager(this);
        if (tabsManager.getTabs().isEmpty()) {
            tabsManager.addTab(new Tab(prefs.getString(PREF_HOME, HOME_URL), prefs.getString(PREF_HOME, HOME_URL), null, false));
            tabsManager.setCurrentIndex(0);
        }
        currentTabIndex = Math.max(0, Math.min(tabsManager.getCurrentIndex(), tabsManager.getTabs().size() - 1));

        // Gesture detector for swipe left/right to change tabs
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_MIN_DISTANCE = 120;
            private static final int SWIPE_THRESHOLD_VELOCITY = 200;
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                float dx = e2.getX() - e1.getX();
                float dy = e2.getY() - e1.getY();
                if (Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    if (dx < 0) switchToNextTab();
                    else switchToPrevTab();
                    return true;
                }
                return false;
            }
        });
        webView.setOnTouchListener((v, event) -> { gestureDetector.onTouchEvent(event); return false; });

        // Load URL: intent has priority, otherwise restore current tab
        String intentUrl = getIntent().getDataString();
        if (intentUrl != null) {
            loadUrl(intentUrl);
        } else {
            Tab cur = tabsManager.getTabs().get(currentTabIndex);
            loadUrl(cur != null && cur.url != null && !cur.url.isEmpty() ? cur.url : prefs.getString(PREF_HOME, HOME_URL));
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(prefs.getBoolean(PREF_JS, true));
        s.setDomStorageEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setSupportMultipleWindows(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setTextZoom(prefs.getInt(PREF_TEXT_SIZE, 100));

        String ua = prefs.getString(PREF_UA, null);
        if (ua != null && !ua.isEmpty()) s.setUserAgentString(ua);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false);

        credentialManager = new CredentialManager(this);
        webView.addJavascriptInterface(credentialManager, "CobrowPasswordManager");

        webView.setWebViewClient(new CobrowWebViewClient());
        webView.setWebChromeClient(new CobrowChromeClient());

        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
            DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url));
            req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            req.addRequestHeader("User-Agent", userAgent);
            req.setMimeType(mimeType);
            ((DownloadManager) getSystemService(DOWNLOAD_SERVICE)).enqueue(req);
            
            // Record download in DB
            executor.execute(() -> {
                com.cobrow.browser.data.DownloadItem item = new com.cobrow.browser.data.DownloadItem(
                        fileName, url, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/" + fileName,
                        contentLength, mimeType);
                CobrowDatabase.get(this).downloadDao().insert(item);
            });

            Toast.makeText(this, "Downloading...", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupUrlBar() {
        UrlSuggestionsAdapter adapter = new UrlSuggestionsAdapter(this);
        urlBar.setAdapter(adapter);
        urlBar.setOnItemClickListener((parent, view, position, id) -> {
            UrlSuggestionsAdapter.Suggestion s = adapter.getItem(position);
            loadUrl(s.url);
            hideKeyboard();
        });
        urlBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                loadUrl(urlBar.getText().toString().trim());
                hideKeyboard();
                return true;
            }
            return false;
        });
        urlBar.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) urlBar.selectAll(); });
    }

    private void setupButtons() {
        btnBack.setOnClickListener(v -> { if (webView.canGoBack()) webView.goBack(); });
        btnForward.setOnClickListener(v -> { if (webView.canGoForward()) webView.goForward(); });
        btnRefresh.setOnClickListener(v -> {
            if (webView.getProgress() < 100) webView.stopLoading();
            else webView.reload();
        });
        btnHome.setOnClickListener(v -> loadUrl(prefs.getString(PREF_HOME, HOME_URL)));
        btnTabs.setOnClickListener(v -> startActivityForResult(new Intent(this, TabsActivity.class), 1001));
        btnMenu.setOnClickListener(v -> showMenu());
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener(() -> { webView.reload(); swipeRefresh.setRefreshing(false); });
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
        
        // Fix conflict with WebView scroll
        webView.getViewTreeObserver().addOnScrollChangedListener(() -> {
            swipeRefresh.setEnabled(webView.getScrollY() == 0);
        });
    }

    private void setupFullscreenScroll() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            webView.setOnScrollChangeListener((v, scrollX, scrollY, oldX, oldY) -> {
                if (scrollY == 0) {
                    setToolbarVisible(true);
                    return;
                }
                int dy = scrollY - oldY;
                if (dy > 10 && isToolbarVisible) {
                    setToolbarVisible(false);
                } else if (dy < -10 && !isToolbarVisible) {
                    setToolbarVisible(true);
                }
            });
        }
    }

    private void setupLongClickMenu() {
        webView.setOnLongClickListener(v -> {
            WebView.HitTestResult result = webView.getHitTestResult();
            final String url = result.getExtra();
            if (url == null) return false;

            int type = result.getType();
            if (type == WebView.HitTestResult.SRC_ANCHOR_TYPE || type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                new AlertDialog.Builder(this)
                        .setTitle(url)
                        .setItems(new String[]{"Open in new tab", "Copy link", "Download link", "Share"}, (dialog, which) -> {
                            switch (which) {
                                case 0: addNewTab(url); break;
                                case 1:
                                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("URL", url));
                                    Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
                                    break;
                                case 2: webView.loadUrl(url); // This will trigger DownloadListener if it's a file
                                    break;
                                case 3:
                                    Intent share = new Intent(Intent.ACTION_SEND);
                                    share.setType("text/plain");
                                    share.putExtra(Intent.EXTRA_TEXT, url);
                                    startActivity(Intent.createChooser(share, "Share link"));
                                    break;
                            }
                        }).show();
                return true;
            }
            return false;
        });
    }

    private void setToolbarVisible(boolean visible) {
        if (isToolbarVisible == visible) return;
        isToolbarVisible = visible;
        
        float topY = visible ? 0 : -topBar.getHeight();
        float bottomY = visible ? 0 : bottomBar.getHeight();

        topBar.animate().translationY(topY).setDuration(200).start();
        bottomBar.animate().translationY(bottomY).setDuration(200).start();
    }

    private void setupFindInPage() {
        etFind.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                String q = s.toString().trim();
                if (!q.isEmpty()) webView.findAllAsync(q);
                else { webView.clearMatches(); tvFindCount.setText(""); }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        etFind.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) { webView.findNext(true); return true; }
            return false;
        });
        webView.setFindListener((activeMatchOrdinal, numberOfMatches, isDoneCounting) -> {
            if (isDoneCounting)
                tvFindCount.setText(numberOfMatches > 0 ? (activeMatchOrdinal + 1) + "/" + numberOfMatches : "No results");
        });
        btnFindPrev.setOnClickListener(v -> webView.findNext(false));
        btnFindNext.setOnClickListener(v -> webView.findNext(true));
        btnFindClose.setOnClickListener(v -> hideFindInPage());
    }

    // ── Public feature methods called from BottomMenuSheet ─────────────────────

    public void showFindInPage() {
        findBar.setVisibility(View.VISIBLE);
        etFind.requestFocus();
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                .showSoftInput(etFind, InputMethodManager.SHOW_IMPLICIT);
    }

    public void savePageAsMhtml() {
        String fileName = URLUtil.guessFileName(webView.getUrl(), null, null);
        if (!fileName.endsWith(".mhtml")) fileName = fileName.replaceAll("\\.[^.]+$", "") + ".mhtml";
        java.io.File file = new java.io.File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
        final String fn = fileName;
        webView.saveWebArchive(file.getAbsolutePath(), false,
                path -> Toast.makeText(this, path != null ? "Saved: " + fn : "Save failed", Toast.LENGTH_SHORT).show());
    }

    public void clearBrowsingData() {
        new AlertDialog.Builder(this)
                .setTitle("Clear Browsing Data")
                .setMessage("Clear cookies, cache, and history?")
                .setPositiveButton("Clear", (d, w) -> {
                    webView.clearCache(true);
                    webView.clearHistory();
                    CookieManager.getInstance().removeAllCookies(null);
                    CookieManager.getInstance().flush();
                    WebStorage.getInstance().deleteAllData();
                    executor.execute(() -> CobrowDatabase.get(this).historyDao().clearAll());
                    adsBlocked = 0;
                    blockedCount.setText("0");
                    Toast.makeText(this, "Browsing data cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null).show();
    }

    public void toggleIncognito() {
        isIncognito = !isIncognito;
        applyIncognitoState(isIncognito);
        
        // Update current tab
        executor.execute(() -> {
            try {
                Tab cur = tabsManager.getTabs().get(currentTabIndex);
                cur.incognito = isIncognito;
                tabsManager.updateTab(currentTabIndex, cur);
            } catch (Exception ignored) {}
        });

        Toast.makeText(this, isIncognito ? "Incognito ON" : "Incognito OFF", Toast.LENGTH_SHORT).show();
        webView.reload();
    }

    private void applyIncognitoState(boolean incognito) {
        this.isIncognito = incognito;
        CookieManager.getInstance().setAcceptCookie(!incognito);
        webView.getSettings().setCacheMode(incognito ? WebSettings.LOAD_NO_CACHE : WebSettings.LOAD_DEFAULT);
        if (incognito) {
            webView.clearCache(true);
            CookieManager.getInstance().removeAllCookies(null);
        }
    }

    public boolean isIncognito() { return isIncognito; }

    public void toggleNightMode() {
        boolean night = !prefs.getBoolean(PREF_NIGHT, false);
        prefs.edit().putBoolean(PREF_NIGHT, night).apply();
        AppCompatDelegate.setDefaultNightMode(night ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        applyNightModeToWebView(night);
        Toast.makeText(this, night ? "Night Mode ON" : "Night Mode OFF", Toast.LENGTH_SHORT).show();
    }

    private void applyNightModeToWebView(boolean night) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            WebSettingsCompat.setForceDark(webView.getSettings(),
                    night ? WebSettingsCompat.FORCE_DARK_ON : WebSettingsCompat.FORCE_DARK_OFF);
        } else {
            // Fallback to JS injection if FORCE_DARK is not supported
            webView.loadUrl(night ? NIGHT_MODE_JS : NIGHT_MODE_REMOVE_JS);
        }
    }

    public boolean isNightMode() { return prefs.getBoolean(PREF_NIGHT, false); }

    public void showReaderMode() {
        // Inject Readability-style JS to strip page to main content
        String js = "javascript:(function(){" +
            "var article=document.querySelector('article')||document.querySelector('main')||document.body;" +
            "var title=document.title;" +
            "var content=article.innerText;" +
            "document.open();" +
            "document.write('<html><head><meta name=viewport content=width=device-width><style>" +
            "body{max-width:680px;margin:24px auto;padding:0 16px;font-family:Georgia,serif;font-size:18px;line-height:1.7;color:#222;background:#fafafa;}" +
            "h1{font-size:24px;margin-bottom:8px;}" +
            "</style></head><body>" +
            "<h1>'+title+'</h1><p>'+content.replace(/\\n/g,'</p><p>')+'</p>" +
            "</body></html>');" +
            "document.close();})()";
        webView.loadUrl(js);
    }

    public void viewSource() {
        String url = webView.getUrl();
        if (url == null) return;
        webView.loadUrl("view-source:" + url);
    }

    public void printPage() {
        PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
        String jobName = getString(R.string.app_name) + " Document";
        PrintDocumentAdapter printAdapter = webView.createPrintDocumentAdapter(jobName);
        if (printManager != null) {
            printManager.print(jobName, printAdapter, new PrintAttributes.Builder().build());
        }
    }

    public void captureScreenshot() {
        try {
            Bitmap bitmap = Bitmap.createBitmap(webView.getWidth(), webView.getHeight(), Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
            webView.draw(canvas);

            String fileName = "Cobrow_Screenshot_" + System.currentTimeMillis() + ".png";
            java.io.File file = new java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), fileName);
            java.io.FileOutputStream out = new java.io.FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();

            // Notify gallery
            android.media.MediaScannerConnection.scanFile(this, new String[]{file.getAbsolutePath()}, null, null);
            Toast.makeText(this, "Screenshot saved to Pictures", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to capture screenshot", Toast.LENGTH_SHORT).show();
        }
    }

    public void showPageInfo() {
        String url = webView.getUrl();
        String title = webView.getTitle();
        boolean isSecure = url != null && url.startsWith("https://");
        new AlertDialog.Builder(this)
                .setTitle("Page Info")
                .setMessage(
                    "Title: " + (title != null ? title : "—") + "\n\n" +
                    "URL: " + (url != null ? url : "—") + "\n\n" +
                    "Connection: " + (isSecure ? "🔒 Secure (HTTPS)" : "⚠ Not Secure (HTTP)"))
                .setPositiveButton("OK", null)
                .show();
    }

    public void showTextSize() {
        int current = prefs.getInt(PREF_TEXT_SIZE, 100);
        View view = getLayoutInflater().inflate(android.R.layout.activity_list_item, null);
        // Use a simple SeekBar dialog
        final int[] size = {current};
        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(150); // 50% to 200% → offset by 50
        seekBar.setProgress(current - 50);
        seekBar.setPadding(48, 32, 48, 16);

        TextView label = new TextView(this);
        label.setText(current + "%");
        label.setGravity(android.view.Gravity.CENTER);
        label.setPadding(0, 8, 0, 0);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                size[0] = progress + 50;
                label.setText(size[0] + "%");
                webView.getSettings().setTextZoom(size[0]);
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(seekBar);
        layout.addView(label);

        new AlertDialog.Builder(this)
                .setTitle("Text Size")
                .setView(layout)
                .setPositiveButton("Apply", (d, w) -> prefs.edit().putInt(PREF_TEXT_SIZE, size[0]).apply())
                .setNegativeButton("Cancel", (d, w) -> webView.getSettings().setTextZoom(current))
                .show();
    }

    // ── Internal helpers ───────────────────────────────────────────────────────

    public void loadUrl(String input) {
        String engine = prefs.getString(PREF_SEARCH_ENGINE, DEFAULT_SEARCH_URL);
        webView.loadUrl(UrlUtils.toUrl(input, engine));
    }

    private void showMenu() {
        new BottomMenuSheet(this, webView, prefs, adsBlocked).show();
    }

    private void hideFindInPage() {
        findBar.setVisibility(View.GONE);
        etFind.setText("");
        tvFindCount.setText("");
        webView.clearMatches();
        hideKeyboard();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(urlBar.getWindowToken(), 0);
        urlBar.clearFocus();
    }

    public void addCurrentPageBookmark() {
        final EditText etFolder = new EditText(this);
        etFolder.setHint("Folder (optional)");
        etFolder.setPadding(48, 16, 48, 16);

        new AlertDialog.Builder(this)
                .setTitle("Add Bookmark")
                .setMessage("Enter folder name (leave empty for root):")
                .setView(etFolder)
                .setPositiveButton("Save", (dialog, which) -> {
                    String folder = etFolder.getText().toString().trim();
                    executor.execute(() -> {
                        com.cobrow.browser.data.Bookmark bm = new com.cobrow.browser.data.Bookmark(
                                webView.getTitle(), webView.getUrl());
                        if (!folder.isEmpty()) bm.folder = folder;
                        CobrowDatabase.get(this).bookmarkDao().insert(bm);
                        runOnUiThread(() -> Toast.makeText(this, "Bookmarked in " + (folder.isEmpty() ? "root" : folder), Toast.LENGTH_SHORT).show());
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveToHistory(String title, String url) {
        if (!isIncognito)
            executor.execute(() -> CobrowDatabase.get(this).historyDao().insert(new HistoryItem(title, url)));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            int idx = data.getIntExtra("selected_index", -1);
            if (idx >= 0) switchToTab(idx);
        }
    }

    // Tab switching helpers
    private void switchToTab(int idx) {
        List<Tab> tabs = tabsManager.getTabs();
        if (tabs == null || tabs.isEmpty()) return;
        if (idx < 0 || idx >= tabs.size()) return;
        // save current
        try { tabsManager.updateTab(currentTabIndex, new com.cobrow.browser.data.Tab(webView.getTitle(), webView.getUrl(), null, isIncognito())); } catch (Exception ignored) {}
        currentTabIndex = idx;
        tabsManager.setCurrentIndex(idx);
        Tab t = tabs.get(idx);
        if (t != null) {
            applyIncognitoState(t.incognito);
            if (t.url != null) loadUrl(t.url);
        }
    }

    private void switchToNextTab() {
        List<Tab> tabs = tabsManager.getTabs();
        if (tabs.size() <= 1) return;
        int next = (currentTabIndex + 1) % tabs.size();
        switchToTab(next);
    }

    private void switchToPrevTab() {
        List<Tab> tabs = tabsManager.getTabs();
        if (tabs.size() <= 1) return;
        int prev = (currentTabIndex - 1 + tabs.size()) % tabs.size();
        switchToTab(prev);
    }

    private void addNewTab(String url) {
        tabsManager.addTab(new com.cobrow.browser.data.Tab("", url != null ? url : prefs.getString(PREF_HOME, HOME_URL), null, false));
        switchToTab(tabsManager.getCurrentIndex());
    }

    private void closeTab(int idx) {
        List<Tab> tabs = tabsManager.getTabs();
        if (tabs.size() <= 1) {
            // keep at least one tab: reset to home
            tabsManager.saveTabs(new java.util.ArrayList<com.cobrow.browser.data.Tab>());
            tabsManager.addTab(new com.cobrow.browser.data.Tab(prefs.getString(PREF_HOME, HOME_URL), prefs.getString(PREF_HOME, HOME_URL), null, false));
            switchToTab(0);
            return;
        }
        boolean removingCurrent = (idx == currentTabIndex);
        tabsManager.removeTab(idx);
        if (removingCurrent) {
            int newIdx = Math.max(0, Math.min(idx, tabsManager.getTabs().size() - 1));
            switchToTab(newIdx);
        } else {
            // adjust current index if necessary
            if (idx < currentTabIndex) currentTabIndex--;
            tabsManager.setCurrentIndex(currentTabIndex);
        }
    }

    @Override
    public void onBackPressed() {
        if (findBar.getVisibility() == View.VISIBLE) hideFindInPage();
        else if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        executor.shutdown();
        webView.destroy();
        super.onDestroy();
    }

    // ── WebViewClient ──────────────────────────────────────────────────────────

    private class CobrowWebViewClient extends WebViewClient {
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            if (prefs.getBoolean(PREF_ADBLOCK, true) && AdBlocker.getInstance().shouldBlock(url, currentPageHost)) {
                adsBlocked++;
                runOnUiThread(() -> blockedCount.setText(String.valueOf(adsBlocked)));
                return new WebResourceResponse("text/plain", "utf-8",
                        new ByteArrayInputStream("".getBytes()));
            }
            return super.shouldInterceptRequest(view, request);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            currentPageHost = extractHost(url);
            progressBar.setVisibility(View.VISIBLE);
            urlBar.setText(url);
            btnRefresh.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            updateNavButtons();
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            progressBar.setVisibility(View.GONE);
            urlBar.setText(url);
            btnRefresh.setImageResource(android.R.drawable.ic_menu_rotate);
            updateNavButtons();
            saveToHistory(view.getTitle(), url);
            applyNightModeToWebView(prefs.getBoolean(PREF_NIGHT, false));
            if (!isIncognito) {
                credentialManager.autofill(view, currentPageHost);
                credentialManager.injectFormDetection(view);
            }
            if (prefs.getBoolean(PREF_ADBLOCK, true))
                AdBlocker.getInstance().injectElementHiding(view, url);

            // Capture thumbnail for the current tab
            try {
                int w = webView.getWidth();
                int h = webView.getHeight();
                if (w > 0 && h > 0) {
                    int thumbW = 360; int thumbH = 240;
                    Bitmap bmp = Bitmap.createBitmap(thumbW, thumbH, Bitmap.Config.ARGB_8888);
                    android.graphics.Canvas canvas = new android.graphics.Canvas(bmp);
                    float scale = Math.min((float) thumbW / Math.max(1, w), (float) thumbH / Math.max(1, h));
                    canvas.scale(scale, scale);
                    webView.draw(canvas);
                    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                    bmp.compress(Bitmap.CompressFormat.PNG, 80, baos);
                    String b64 = android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.NO_WRAP);
                    // update tab
                    try { tabsManager.updateTab(currentTabIndex, new com.cobrow.browser.data.Tab(view.getTitle(), url, b64, isIncognito())); } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            if (url.startsWith("http") || url.startsWith("https") || url.startsWith("view-source:")) return false;
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception ignored) {}
            return true;
        }
    }

    // ── ChromeClient ───────────────────────────────────────────────────────────

    private class CobrowChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            progressBar.setProgress(newProgress);
            if (newProgress == 100) progressBar.setVisibility(View.GONE);
        }
    }

    private void updateNavButtons() {
        btnBack.setAlpha(webView.canGoBack() ? 1f : 0.4f);
        btnForward.setAlpha(webView.canGoForward() ? 1f : 0.4f);
    }

    private String extractHost(String url) {
        if (url == null) return null;
        try {
            int s = url.indexOf("://"); if (s < 0) return null; s += 3;
            int e = url.indexOf('/', s);
            return e < 0 ? url.substring(s) : url.substring(s, e);
        } catch (Exception e) { return null; }
    }
}
