package com.cobrow.browser.engine;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class AdBlocker {
    private static final String TAG = "AdBlocker";
    private static AdBlocker instance;

    private final Set<String> exactDomainRules = new HashSet<>();
    private final List<Pattern> regexRules = new ArrayList<>();
    private boolean enabled = true;

    private AdBlocker() {}

    public static AdBlocker getInstance() {
        if (instance == null) instance = new AdBlocker();
        return instance;
    }

    public void loadFilters(Context ctx) {
        new Thread(() -> {
            loadFile(ctx, "filters/easylist.txt");
            loadFile(ctx, "filters/ublock_filters.txt");
            Log.d(TAG, "Loaded " + exactDomainRules.size() + " domain rules, " + regexRules.size() + " regex rules");
        }).start();
    }

    private void loadFile(Context ctx, String path) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(ctx.getAssets().open(path)))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("!") || line.startsWith("[")) continue;
                parseLine(line);
            }
        } catch (IOException e) {
            Log.w(TAG, "Could not load " + path + ": " + e.getMessage());
        }
    }

    private void parseLine(String rule) {
        // Skip element hiding rules (##)
        if (rule.contains("##") || rule.contains("#@#") || rule.contains("#?#")) return;
        // Exception rules (@@) - skip for now
        if (rule.startsWith("@@")) return;

        // Domain-only rule: ||example.com^
        if (rule.startsWith("||") && rule.endsWith("^")) {
            String domain = rule.substring(2, rule.length() - 1);
            if (!domain.contains("/") && !domain.contains("*")) {
                exactDomainRules.add(domain);
                return;
            }
        }

        // Convert filter rule to regex
        try {
            String regex = rule
                .replace(".", "\\.")
                .replace("?", "\\?")
                .replace("||", "(?:https?://)?(?:[a-z0-9-]+\\.)*")
                .replace("|", "")
                .replace("^", "(?:[/?#]|$)")
                .replace("*", ".*");
            regexRules.add(Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
        } catch (Exception ignored) {}
    }

    public boolean shouldBlock(String url) {
        if (!enabled || url == null) return false;

        // Fast domain check
        String host = extractHost(url);
        if (host != null) {
            if (exactDomainRules.contains(host)) return true;
            // Check subdomains
            int dot = host.indexOf('.');
            while (dot != -1) {
                String sub = host.substring(dot + 1);
                if (exactDomainRules.contains(sub)) return true;
                dot = host.indexOf('.', dot + 1);
            }
        }

        // Regex check
        for (Pattern p : regexRules) {
            if (p.matcher(url).find()) return true;
        }
        return false;
    }

    private String extractHost(String url) {
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

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isEnabled() { return enabled; }
}
