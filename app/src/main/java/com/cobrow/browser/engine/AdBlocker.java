package com.cobrow.browser.engine;

import android.content.Context;
import android.util.Log;
import android.webkit.WebView;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

public class AdBlocker {
    private static final String TAG = "AdBlocker";
    private static final String CACHE_FILE = "adblock_domains.cache";
    private static AdBlocker instance;

    private final Set<String> blockedDomains = new HashSet<>(60000);
    private final Set<String> exceptionDomains = new HashSet<>(1000);
    private final Set<String> thirdPartyDomains = new HashSet<>(5000);
    private final List<Pattern> regexRules = new ArrayList<>(1000);
    private final List<String> genericHideSelectors = new ArrayList<>(200);
    private final Set<String> whitelist = new HashSet<>();

    private boolean enabled = true;
    private boolean blockCookieNotices = true;
    private boolean blockThirdPartyTrackers = true;
    private int totalRulesLoaded = 0;

    private AdBlocker() {}

    public static AdBlocker getInstance() {
        if (instance == null) instance = new AdBlocker();
        return instance;
    }

    public void loadFilters(Context ctx) {
        new Thread(() -> {
            long t = System.currentTimeMillis();
            // Try loading from binary cache first (fast path)
            if (loadFromCache(ctx)) {
                Log.d(TAG, "Loaded from cache: " + blockedDomains.size() + " domains in " + (System.currentTimeMillis() - t) + "ms");
                return;
            }
            // Slow path: parse text files then save cache
            parseAllFiles(ctx);
            saveCache(ctx);
            Log.d(TAG, "Parsed & cached: " + blockedDomains.size() + " domains in " + (System.currentTimeMillis() - t) + "ms");
        }).start();
    }

    private void parseAllFiles(Context ctx) {
        loadAsset(ctx, "filters/easylist.txt");
        loadAsset(ctx, "filters/easyprivacy.txt");
        loadAsset(ctx, "filters/peterlowe.txt");
        loadAsset(ctx, "filters/ublock_filters.txt");
        loadAsset(ctx, "filters/ublock_badware.txt");
        if (blockCookieNotices) loadAsset(ctx, "filters/ublock_cookies.txt");
        // Also load from internal storage if updated
        File filterDir = new File(ctx.getFilesDir(), "filters");
        if (filterDir.exists()) {
            for (File f : Objects.requireNonNull(filterDir.listFiles())) loadFromDisk(f);
        }
        totalRulesLoaded = blockedDomains.size() + regexRules.size();
    }

    @SuppressWarnings("unchecked")
    private boolean loadFromCache(Context ctx) {
        File cache = new File(ctx.getCacheDir(), CACHE_FILE);
        if (!cache.exists()) return false;
        try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(cache)))) {
            Set<String> domains = (Set<String>) ois.readObject();
            Set<String> exceptions = (Set<String>) ois.readObject();
            Set<String> thirdParty = (Set<String>) ois.readObject();
            blockedDomains.addAll(domains);
            exceptionDomains.addAll(exceptions);
            thirdPartyDomains.addAll(thirdParty);
            totalRulesLoaded = blockedDomains.size();
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Cache load failed, reparsing: " + e.getMessage());
            cache.delete();
            return false;
        }
    }

    private void saveCache(Context ctx) {
        File cache = new File(ctx.getCacheDir(), CACHE_FILE);
        try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(cache)))) {
            oos.writeObject(new HashSet<>(blockedDomains));
            oos.writeObject(new HashSet<>(exceptionDomains));
            oos.writeObject(new HashSet<>(thirdPartyDomains));
        } catch (Exception e) {
            Log.w(TAG, "Cache save failed: " + e.getMessage());
        }
    }

    /** Call this after updating filters to force re-parse on next launch */
    public void invalidateCache(Context ctx) {
        new File(ctx.getCacheDir(), CACHE_FILE).delete();
        blockedDomains.clear(); exceptionDomains.clear();
        thirdPartyDomains.clear(); regexRules.clear();
    }

    private void loadAsset(Context ctx, String path) {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(ctx.getAssets().open(path)), 32768)) {
            String line;
            while ((line = br.readLine()) != null) parseLine(line.trim());
        } catch (IOException e) {
            Log.w(TAG, "Missing asset: " + path);
        }
    }

    private void loadFromDisk(File file) {
        try (BufferedReader br = new BufferedReader(new FileReader(file), 32768)) {
            String line;
            while ((line = br.readLine()) != null) parseLine(line.trim());
        } catch (IOException e) {
            Log.w(TAG, "Cannot read: " + file.getName());
        }
    }

    private void parseLine(String rule) {
        if (rule.isEmpty() || rule.startsWith("!") || rule.startsWith("[") || rule.startsWith("#")) return;

        if (rule.startsWith("@@")) {
            String inner = rule.substring(2);
            if (inner.startsWith("||") && inner.endsWith("^")) {
                String d = inner.substring(2, inner.length() - 1);
                if (isSimpleDomain(d)) exceptionDomains.add(d);
            }
            return;
        }

        // Skip element hiding — only collect generic ones (limit size)
        if (rule.contains("##")) {
            int idx = rule.indexOf("##");
            if (idx == 0 && genericHideSelectors.size() < 200)
                genericHideSelectors.add(rule.substring(2));
            return;
        }
        if (rule.contains("#@#") || rule.contains("#?#") || rule.contains("#$#")) return;

        // Strip options
        boolean thirdPartyOnly = false;
        int dollar = rule.lastIndexOf('$');
        if (dollar > 0) {
            String opts = rule.substring(dollar + 1);
            if (opts.contains("third-party") || opts.contains("3p")) thirdPartyOnly = true;
            if (opts.contains("document") || opts.contains("elemhide")) return;
            rule = rule.substring(0, dollar);
        }

        // ||domain^ — most common rule type
        if (rule.startsWith("||") && rule.endsWith("^")) {
            String d = rule.substring(2, rule.length() - 1);
            if (isSimpleDomain(d)) {
                (thirdPartyOnly ? thirdPartyDomains : blockedDomains).add(d);
                return;
            }
        }

        // Hosts file
        if (rule.startsWith("0.0.0.0 ") || rule.startsWith("127.0.0.1 ")) {
            String d = rule.substring(rule.indexOf(' ') + 1).trim();
            if (isSimpleDomain(d)) { blockedDomains.add(d); return; }
        }

        // Regex — only path-based rules, keep list small
        if (regexRules.size() < 1000 && (rule.contains("/") || rule.contains("="))) {
            try {
                String regex = toRegex(rule);
                if (regex != null) regexRules.add(Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
            } catch (Exception ignored) {}
        }
    }

    private boolean isSimpleDomain(String s) {
        return !s.isEmpty() && s.contains(".") && !s.contains("/") && !s.contains("*") && !s.contains(" ") && s.length() > 3;
    }

    private String toRegex(String rule) {
        if (!rule.contains("/") && !rule.contains("=") && !rule.contains("?")) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rule.length(); i++) {
            char c = rule.charAt(i);
            switch (c) {
                case '*': sb.append(".*"); break;
                case '^': sb.append("(?:[/?#&]|$)"); break;
                case '|':
                    if (i == 0 && i+1 < rule.length() && rule.charAt(i+1) == '|') { sb.append("https?://(?:[^/]+\\.)?"); i++; }
                    else if (i == 0) sb.append("^");
                    else if (i == rule.length()-1) sb.append("$");
                    break;
                case '.': sb.append("\\."); break;
                case '?': sb.append("\\?"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    public boolean shouldBlock(String url, String pageHost) {
        if (!enabled || url == null || blockedDomains.isEmpty()) return false;
        String host = extractHost(url);
        if (host == null) return false;
        if (isWhitelisted(host)) return false;
        if (isDomainIn(host, exceptionDomains)) return false;
        if (isDomainIn(host, blockedDomains)) return true;
        if (blockThirdPartyTrackers && pageHost != null && !sameRoot(host, pageHost))
            if (isDomainIn(host, thirdPartyDomains)) return true;
        for (Pattern p : regexRules) if (p.matcher(url).find()) return true;
        return false;
    }

    public boolean shouldBlock(String url) { return shouldBlock(url, null); }

    private boolean isDomainIn(String host, Set<String> set) {
        if (set.contains(host)) return true;
        int dot = host.indexOf('.');
        while (dot != -1) {
            if (set.contains(host.substring(dot + 1))) return true;
            dot = host.indexOf('.', dot + 1);
        }
        return false;
    }

    private boolean sameRoot(String a, String b) {
        return a != null && b != null && rootDomain(a).equals(rootDomain(b));
    }

    private String rootDomain(String h) {
        int d = h.lastIndexOf('.'); if (d <= 0) return h;
        int d2 = h.lastIndexOf('.', d - 1);
        return d2 < 0 ? h : h.substring(d2 + 1);
    }

    public void injectElementHiding(WebView webView, String pageUrl) {
        if (!enabled || webView == null || genericHideSelectors.isEmpty()) return;
        StringBuilder css = new StringBuilder();
        for (String sel : genericHideSelectors) css.append(sel).append(",");
        css.setLength(css.length() - 1);
        String js = "javascript:(function(){var s=document.getElementById('cobrow-hide');" +
            "if(!s){s=document.createElement('style');s.id='cobrow-hide';document.head.appendChild(s);}" +
            "s.textContent='" + css.toString().replace("'", "\\'") + "{display:none!important}';})()";
        webView.loadUrl(js);
    }

    public void updateFilters(Context ctx, Runnable onDone, java.util.function.Consumer<String> onError) {
        new Thread(() -> {
            String[][] lists = {
                {"easylist.txt",       "https://easylist.to/easylist/easylist.txt"},
                {"easyprivacy.txt",    "https://easylist.to/easylist/easyprivacy.txt"},
                {"ublock_filters.txt", "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/filters.txt"},
                {"ublock_cookies.txt", "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/annoyances-cookies.txt"},
                {"ublock_badware.txt", "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/badware.txt"},
                {"peterlowe.txt",      "https://pgl.yoyo.org/adservers/serverlist.php?hostformat=adblockplus&showintro=0"},
            };
            try {
                File filterDir = new File(ctx.getFilesDir(), "filters");
                filterDir.mkdirs();
                for (String[] e : lists) {
                    java.net.HttpURLConnection c = (java.net.HttpURLConnection) new java.net.URL(e[1]).openConnection();
                    c.setRequestProperty("Accept-Encoding", "gzip");
                    c.setConnectTimeout(15000); c.setReadTimeout(30000);
                    InputStream in = c.getInputStream();
                    if ("gzip".equals(c.getContentEncoding())) in = new java.util.zip.GZIPInputStream(in);
                    try (InputStream is = in; FileOutputStream fos = new FileOutputStream(new File(filterDir, e[0]))) {
                        byte[] buf = new byte[8192]; int n;
                        while ((n = is.read(buf)) != -1) fos.write(buf, 0, n);
                    }
                    c.disconnect();
                }
                invalidateCache(ctx);
                parseAllFiles(ctx);
                saveCache(ctx);
                totalRulesLoaded = blockedDomains.size();
                if (onDone != null) onDone.run();
            } catch (Exception e) {
                if (onError != null) onError.accept(e.getMessage());
            }
        }).start();
    }

    public void addCustomRule(String rule) { parseLine(rule); }

    private String extractHost(String url) {
        try {
            int s = url.indexOf("://"); if (s < 0) return null; s += 3;
            int e = url.indexOf('/', s); String h = e < 0 ? url.substring(s) : url.substring(s, e);
            int c = h.indexOf(':'); return c > 0 ? h.substring(0, c) : h;
        } catch (Exception e) { return null; }
    }

    public void setEnabled(boolean v) { enabled = v; }
    public boolean isEnabled() { return enabled; }
    public void addToWhitelist(String d) { whitelist.add(d); }
    public void removeFromWhitelist(String d) { whitelist.remove(d); }
    public boolean isWhitelisted(String domain) {
        if (whitelist.contains(domain)) return true;
        int dot = domain.indexOf('.');
        while (dot != -1) { if (whitelist.contains(domain.substring(dot+1))) return true; dot = domain.indexOf('.', dot+1); }
        return false;
    }
    public Set<String> getWhitelist() { return whitelist; }
    public void setBlockCookieNotices(boolean v) { blockCookieNotices = v; }
    public boolean isBlockCookieNotices() { return blockCookieNotices; }
    public void setBlockThirdPartyTrackers(boolean v) { blockThirdPartyTrackers = v; }
    public boolean isBlockThirdPartyTrackers() { return blockThirdPartyTrackers; }
    public int getTotalRulesLoaded() { return totalRulesLoaded; }
    public int getDomainRulesCount() { return blockedDomains.size(); }
    public int getRegexRulesCount() { return regexRules.size(); }
}
