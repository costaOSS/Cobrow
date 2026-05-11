package com.cobrow.browser.utils;

import android.webkit.URLUtil;

public class UrlUtils {
    private static final String SEARCH_ENGINE = "https://www.google.com/search?q=";

    public static String toUrl(String input) {
        if (input == null || input.trim().isEmpty()) return "about:blank";
        input = input.trim();
        if (input.startsWith("about:") || input.startsWith("file:")) return input;
        if (URLUtil.isValidUrl(input)) return input;
        // Has a dot and no spaces → likely a URL
        if (!input.contains(" ") && input.contains(".")) {
            return "https://" + input;
        }
        // Search query
        return SEARCH_ENGINE + android.net.Uri.encode(input);
    }
}
