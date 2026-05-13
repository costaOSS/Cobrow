package com.cobrow.browser.utils;

import android.webkit.URLUtil;

public class UrlUtils {
    private static final String DEFAULT_SEARCH_ENGINE = "https://www.google.com/search?q=";

    public static String toUrl(String input) {
        return toUrl(input, DEFAULT_SEARCH_ENGINE);
    }

    public static String toUrl(String input, String searchEngine) {
        if (input == null || input.trim().isEmpty()) return "file:///android_asset/new_tab.html";
        input = input.trim();
        if (input.equalsIgnoreCase("cobrow://newtab")) return "file:///android_asset/new_tab.html";
        if (input.startsWith("cobrow://search?q=")) {
            String q = input.substring("cobrow://search?q=".length());
            return toUrl(android.net.Uri.decode(q), searchEngine);
        }
        if (input.startsWith("about:") || input.startsWith("file:")) return input;
        if (URLUtil.isValidUrl(input)) return input;
        // Has a dot and no spaces → likely a URL
        if (!input.contains(" ") && input.contains(".")) {
            return "https://" + input;
        }
        // Search query
        String engine = (searchEngine != null && !searchEngine.isEmpty()) ? searchEngine : DEFAULT_SEARCH_ENGINE;
        return engine + android.net.Uri.encode(input);
    }
}
