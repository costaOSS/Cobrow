package com.cobrow.browser.engine;

import android.content.Context;
import android.util.Log;
import android.webkit.WebView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class AdBlocker {
    private static final String TAG = "AdBlocker";
    private static AdBlocker instance;

    // Fast O(1) domain block set
    private final Set<String> blockedDomains = new HashSet<>(50000);
    // Exception rules @@
    private final Set<String> exceptionDomains = new HashSet<>(2000);
    // Regex rules for path-based matching (kept small)
    private final List<Pattern> regexRules = new ArrayList<>(3000);
    // Third-party only domain rules
    private final Set<String> thirdPartyDomains = new HashSet<>(5000);

    private final Set<String> whitelist = new HashSet<>();
    private boolean enabled = true;
    private boolean blockCookieNotices = true;
    private boolean blockThirdPartyTrackers = true;

    // Stats
    private int totalRulesLoaded = 0;

    // Element hiding CSS per domain (for injection)
    private final Map<String, List<String>> elementHideRules = new HashMap<>();
    // Generic element hiding selectors
    private final List<String> genericHideSelectors = new ArrayList<>(500);

    private AdBlocker() {}

    public static AdBlocker getInstance() {
        if (instance == null) instance = new AdBlocker();
        return instance;
    }

    public void loadFilters(Context ctx) {
        new Thread(() -> {
            long start = System.currentTimeMillis();
            loadFile(ctx, "filters/easylist.txt");
            loadFile(ctx, "filters/easyprivacy.txt");
            loadFile(ctx, "filters/peterlowe.txt");
            loadFile(ctx, "filters/ublock_filters.txt");
            loadFile(ctx, "filters/ublock_badware.txt");
            if (blockCookieNotices) loadFile(ctx, "filters/ublock_cookies.txt");
            totalRulesLoaded = blockedDomains.size() + regexRules.size() + thirdPartyDomains.size();
            Log.d(TAG, "Loaded " + totalRulesLoaded + " rules in " + (System.currentTimeMillis() - start) + "ms");
        }).start();
    }

    private void loadFile(Context ctx, String path) {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(ctx.getAssets().open(path)), 65536)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("!") || line.startsWith("[") || line.startsWith("#")) continue;
                parseLine(line);
            }
        } catch (IOException e) {
            Log.w(TAG, "Could not load " + path + ": " + e.getMessage());
        }
    }

    private void parseLine(String rule) {
        // Exception rules — whitelist these
        if (rule.startsWith("@@")) {
            String inner = rule.substring(2);
            if (inner.startsWith("||") && inner.endsWith("^")) {
                String domain = inner.substring(2, inner.length() - 1);
                if (isSimpleDomain(domain)) exceptionDomains.add(domain);
            }
            return;
        }

        // Element hiding rules — collect for CSS injection
        int hashIdx = rule.indexOf("##");
        if (hashIdx >= 0) {
            String selector = rule.substring(hashIdx + 2);
            String domainPart = rule.substring(0, hashIdx);
            if (domainPart.isEmpty()) {
                if (genericHideSelectors.size() < 500) genericHideSelectors.add(selector);
            } else {
                for (String d : domainPart.split(",")) {
                    d = d.trim();
                    if (!d.startsWith("~")) {
                        elementHideRules.computeIfAbsent(d, k -> new ArrayList<>()).add(selector);
                    }
                }
            }
            return;
        }
        if (rule.contains("#@#") || rule.contains("#?#") || rule.contains("#$#")) return;

        // Parse options
        boolean thirdPartyOnly = false;
        int dollarIdx = rule.lastIndexOf('$');
        if (dollarIdx > 0) {
            String opts = rule.substring(dollarIdx + 1);
            if (opts.contains("third-party") || opts.contains("3p")) thirdPartyOnly = true;
            // Skip rules for specific types we don't handle
            if (opts.contains("document") || opts.contains("elemhide")) return;
            rule = rule.substring(0, dollarIdx);
        }

        // Simple domain rule: ||example.com^
        if (rule.startsWith("||") && rule.endsWith("^")) {
            String domain = rule.substring(2, rule.length() - 1);
            if (isSimpleDomain(domain)) {
                if (thirdPartyOnly) thirdPartyDomains.add(domain);
                else blockedDomains.add(domain);
                return;
            }
        }

        // Hosts-file style: 0.0.0.0 example.com
        if (rule.startsWith("0.0.0.0 ") || rule.startsWith("127.0.0.1 ")) {
            String domain = rule.substring(rule.indexOf(' ') + 1).trim();
            if (isSimpleDomain(domain)) { blockedDomains.add(domain); return; }
        }

        // Skip overly broad or complex rules to keep regex list small
        if (rule.length() < 4 || rule.equals("*")) return;

        // Convert to regex (only for path-based rules worth keeping)
        if (regexRules.size() < 3000) {
            try {
                String regex = ruleToRegex(rule);
                if (regex != null) regexRules.add(Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
            } catch (Exception ignored) {}
        }
    }

    private boolean isSimpleDomain(String s) {
        if (s.isEmpty() || s.contains("/") || s.contains("*") || s.contains(" ")) return false;
        // Must look like a domain
        return s.contains(".") && s.length() > 3;
    }

    private String ruleToRegex(String rule) {
        // Skip rules that are just a domain without path (already handled above)
        if (!rule.contains("/") && !rule.contains("*") && !rule.contains("=") && !rule.contains("?")) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rule.length(); i++) {
            char c = rule.charAt(i);
            switch (c) {
                case '*': sb.append(".*"); break;
                case '^': sb.append("(?:[/?#&]|$)"); break;
                case '|':
                    if (i == 0 && i + 1 < rule.length() && rule.charAt(i + 1) == '|') {
                        sb.append("https?://(?:[^/]+\\.)?");
                        i++;
                    } else if (i == 0) {
                        sb.append("^");
                    } else if (i == rule.length() - 1) {
                        sb.append("$");
                    }
                    break;
                case '.': sb.append("\\."); break;
                case '?': sb.append("\\?"); break;
                case '+': sb.append("\\+"); break;
                case '[': sb.append("\\["); break;
                case ']': sb.append("\\]"); break;
                case '(': sb.append("\\("); break;
                case ')': sb.append("\\)"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    public boolean shouldBlock(String url) {
        return shouldBlock(url, null);
    }

    public boolean shouldBlock(String url, String pageHost) {
        if (!enabled || url == null) return false;

        String host = extractHost(url);
        if (host == null) return false;

        // Whitelist check
        if (isWhitelisted(host)) return false;

        // Exception domains (@@rules)
        if (exceptionDomains.contains(host)) return false;

        // Block check — exact + subdomain walk
        if (isDomainBlocked(host, blockedDomains)) return true;

        // Third-party tracker check
        if (blockThirdPartyTrackers && pageHost != null && !isSameDomain(host, pageHost)) {
            if (isDomainBlocked(host, thirdPartyDomains)) return true;
        }

        // Regex check (path-based rules)
        for (Pattern p : regexRules) {
            if (p.matcher(url).find()) return true;
        }
        return false;
    }

    private boolean isDomainBlocked(String host, Set<String> set) {
        if (set.contains(host)) return true;
        // Walk subdomains: sub.sub.example.com → sub.example.com → example.com
        int dot = host.indexOf('.');
        while (dot != -1) {
            String parent = host.substring(dot + 1);
            if (set.contains(parent)) return true;
            dot = host.indexOf('.', dot + 1);
        }
        return false;
    }

    private boolean isSameDomain(String a, String b) {
        if (a == null || b == null) return false;
        // Compare eTLD+1 (simplified: last two parts)
        String ra = rootDomain(a), rb = rootDomain(b);
        return ra.equals(rb);
    }

    private String rootDomain(String host) {
        int dot = host.lastIndexOf('.');
        if (dot <= 0) return host;
        int dot2 = host.lastIndexOf('.', dot - 1);
        return dot2 < 0 ? host : host.substring(dot2 + 1);
    }

    /** Inject element hiding CSS into a WebView for the current page host */
    public void injectElementHiding(WebView webView, String pageUrl) {
        if (!enabled || webView == null) return;
        String host = extractHost(pageUrl);

        StringBuilder css = new StringBuilder();
        // Generic selectors
        for (String sel : genericHideSelectors) css.append(sel).append(",");
        // Domain-specific
        if (host != null) {
            List<String> domainRules = elementHideRules.get(host);
            if (domainRules != null) for (String sel : domainRules) css.append(sel).append(",");
        }
        if (css.length() == 0) return;
        // Remove trailing comma
        css.setLength(css.length() - 1);

        String js = "javascript:(function(){" +
            "var s=document.createElement('style');" +
            "s.id='cobrow-adblock-hide';" +
            "s.innerHTML='" + css.toString().replace("'", "\\'") + "{display:none!important}';" +
            "document.head.appendChild(s);})()";
        webView.loadUrl(js);
    }

    private String extractHost(String url) {
        try {
            int start = url.indexOf("://");
            if (start == -1) return null;
            start += 3;
            int end = url.indexOf('/', start);
            String host = end == -1 ? url.substring(start) : url.substring(start, end);
            // Strip port
            int colon = host.indexOf(':');
            return colon > 0 ? host.substring(0, colon) : host;
        } catch (Exception e) { return null; }
    }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isEnabled() { return enabled; }

    public void addToWhitelist(String domain) { whitelist.add(domain); }
    public void removeFromWhitelist(String domain) { whitelist.remove(domain); }
    public boolean isWhitelisted(String domain) {
        if (whitelist.contains(domain)) return true;
        int dot = domain.indexOf('.');
        while (dot != -1) {
            if (whitelist.contains(domain.substring(dot + 1))) return true;
            dot = domain.indexOf('.', dot + 1);
        }
        return false;
    }
    public Set<String> getWhitelist() { return whitelist; }

    public void setBlockCookieNotices(boolean block) { blockCookieNotices = block; }
    public boolean isBlockCookieNotices() { return blockCookieNotices; }

    public void setBlockThirdPartyTrackers(boolean block) { blockThirdPartyTrackers = block; }
    public boolean isBlockThirdPartyTrackers() { return blockThirdPartyTrackers; }

    public int getTotalRulesLoaded() { return totalRulesLoaded; }
    public int getDomainRulesCount() { return blockedDomains.size(); }
    public int getRegexRulesCount() { return regexRules.size(); }

    public void addCustomRule(String rule) { parseLine(rule); }

    /** Downloads fresh filter lists from the internet and reloads. */
    public void updateFilters(Context ctx, Runnable onDone, java.util.function.Consumer<String> onError) {
        new Thread(() -> {
            String[][] lists = {
                {"filters/easylist.txt",       "https://easylist.to/easylist/easylist.txt"},
                {"filters/easyprivacy.txt",    "https://easylist.to/easylist/easyprivacy.txt"},
                {"filters/ublock_filters.txt", "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/filters.txt"},
                {"filters/ublock_cookies.txt", "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/annoyances-cookies.txt"},
                {"filters/ublock_badware.txt", "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/badware.txt"},
                {"filters/peterlowe.txt",      "https://pgl.yoyo.org/adservers/serverlist.php?hostformat=adblockplus&showintro=0"},
            };
            try {
                java.io.File filesDir = ctx.getFilesDir();
                java.io.File filterDir = new java.io.File(filesDir, "filters");
                filterDir.mkdirs();
                for (String[] entry : lists) {
                    java.net.URL url = new java.net.URL(entry[1]);
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                    conn.setRequestProperty("Accept-Encoding", "gzip");
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(30000);
                    java.io.InputStream in = conn.getInputStream();
                    if ("gzip".equals(conn.getContentEncoding()))
                        in = new java.util.zip.GZIPInputStream(in);
                    java.io.File out = new java.io.File(filterDir, new java.io.File(entry[0]).getName());
                    try (java.io.InputStream is = in;
                         java.io.FileOutputStream fos = new java.io.FileOutputStream(out)) {
                        byte[] buf = new byte[8192]; int n;
                        while ((n = is.read(buf)) != -1) fos.write(buf, 0, n);
                    }
                    conn.disconnect();
                }
                // Reload from updated files
                blockedDomains.clear(); exceptionDomains.clear();
                regexRules.clear(); thirdPartyDomains.clear();
                elementHideRules.clear(); genericHideSelectors.clear();
                for (String[] entry : lists) {
                    java.io.File f = new java.io.File(new java.io.File(filesDir, "filters"),
                            new java.io.File(entry[0]).getName());
                    loadFileFromDisk(f);
                }
                totalRulesLoaded = blockedDomains.size() + regexRules.size() + thirdPartyDomains.size();
                Log.d(TAG, "Updated: " + totalRulesLoaded + " rules");
                if (onDone != null) onDone.run();
            } catch (Exception e) {
                Log.e(TAG, "Update failed: " + e.getMessage());
                if (onError != null) onError.accept(e.getMessage());
            }
        }).start();
    }

    private void loadFileFromDisk(java.io.File file) {
        try (BufferedReader br = new BufferedReader(new java.io.FileReader(file), 65536)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("!") || line.startsWith("[") || line.startsWith("#")) continue;
                parseLine(line);
            }
        } catch (IOException e) {
            Log.w(TAG, "Could not read " + file.getName() + ": " + e.getMessage());
        }
    }
}
