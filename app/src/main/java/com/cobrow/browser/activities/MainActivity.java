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
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.cobrow.browser.R;
import com.cobrow.browser.data.CobrowDatabase;
import com.cobrow.browser.data.HistoryItem;
import com.cobrow.browser.engine.AdBlocker;
import com.cobrow.browser.utils.UrlUtils;

import java.io.ByteArrayInputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private EditText urlBar;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefresh;
    private ImageButton btnBack, btnForward, btnRefresh, btnHome, btnMenu;
    private LinearLayout bottomBar;
    private TextView blockedCount;

    private SharedPreferences prefs;
    private int adsBlocked = 0;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private static final String HOME_URL = "https://www.google.com";
    private static final String PREF_HOME = "home_url";
    private static final String PREF_UA = "user_agent";
    private static final String PREF_JS = "javascript";
    private static final String PREF_ADBLOCK = "adblock";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("cobrow_prefs", MODE_PRIVATE);

        webView = findViewById(R.id.webView);
        urlBar = findViewById(R.id.urlBar);
        progressBar = findViewById(R.id.progressBar);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        btnBack = findViewById(R.id.btnBack);
        btnForward = findViewById(R.id.btnForward);
        btnRefresh = findViewById(R.id.btnRefresh);
        btnHome = findViewById(R.id.btnHome);
        btnMenu = findViewById(R.id.btnMenu);
        blockedCount = findViewById(R.id.blockedCount);

        setupWebView();
        setupUrlBar();
        setupButtons();
        setupSwipeRefresh();

        // Handle intent URL or load home
        String intentUrl = getIntent().getDataString();
        loadUrl(intentUrl != null ? intentUrl : prefs.getString(PREF_HOME, HOME_URL));
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

        String ua = prefs.getString(PREF_UA, null);
        if (ua != null && !ua.isEmpty()) s.setUserAgentString(ua);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false);

        webView.setWebViewClient(new CobrowWebViewClient());
        webView.setWebChromeClient(new CobrowChromeClient());

        // Download listener
        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url));
            req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                    URLUtil.guessFileName(url, contentDisposition, mimeType));
            req.addRequestHeader("User-Agent", userAgent);
            req.setMimeType(mimeType);
            ((DownloadManager) getSystemService(DOWNLOAD_SERVICE)).enqueue(req);
            Toast.makeText(this, "Downloading...", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupUrlBar() {
        urlBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                loadUrl(urlBar.getText().toString().trim());
                hideKeyboard();
                return true;
            }
            return false;
        });
        urlBar.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) urlBar.selectAll();
        });
    }

    private void setupButtons() {
        btnBack.setOnClickListener(v -> { if (webView.canGoBack()) webView.goBack(); });
        btnForward.setOnClickListener(v -> { if (webView.canGoForward()) webView.goForward(); });
        btnRefresh.setOnClickListener(v -> {
            if (webView.getProgress() < 100) webView.stopLoading();
            else webView.reload();
        });
        btnHome.setOnClickListener(v -> loadUrl(prefs.getString(PREF_HOME, HOME_URL)));
        btnMenu.setOnClickListener(v -> showMenu());
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener(() -> {
            webView.reload();
            swipeRefresh.setRefreshing(false);
        });
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
    }

    public void loadUrl(String input) {
        String url = UrlUtils.toUrl(input);
        webView.loadUrl(url);
    }

    private void showMenu() {
        BottomMenuSheet sheet = new BottomMenuSheet(this, webView, prefs, adsBlocked);
        sheet.show();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(urlBar.getWindowToken(), 0);
        urlBar.clearFocus();
    }

    public void addCurrentPageBookmark() {
        executor.execute(() -> {
            com.cobrow.browser.data.Bookmark bm = new com.cobrow.browser.data.Bookmark(
                    webView.getTitle(), webView.getUrl());
            CobrowDatabase.get(this).bookmarkDao().insert(bm);
            runOnUiThread(() -> Toast.makeText(this, "Bookmarked!", Toast.LENGTH_SHORT).show());
        });
    }

    private void saveToHistory(String title, String url) {
        executor.execute(() -> {
            CobrowDatabase.get(this).historyDao().insert(new HistoryItem(title, url));
        });
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
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
        private static final String EMPTY_RESPONSE = "";

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            if (prefs.getBoolean(PREF_ADBLOCK, true) && AdBlocker.getInstance().shouldBlock(url)) {
                adsBlocked++;
                runOnUiThread(() -> blockedCount.setText(String.valueOf(adsBlocked)));
                return new WebResourceResponse("text/plain", "utf-8",
                        new ByteArrayInputStream(EMPTY_RESPONSE.getBytes()));
            }
            return super.shouldInterceptRequest(view, request);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
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
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            if (url.startsWith("http") || url.startsWith("https")) return false;
            // Handle tel:, mailto:, intent: etc.
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } catch (Exception ignored) {}
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

        @Override
        public void onReceivedTitle(WebView view, String title) {
            // Could update tab title here
        }
    }

    private void updateNavButtons() {
        btnBack.setAlpha(webView.canGoBack() ? 1f : 0.4f);
        btnForward.setAlpha(webView.canGoForward() ? 1f : 0.4f);
    }
}
