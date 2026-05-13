package com.cobrow.browser;

import android.app.Application;
import com.cobrow.browser.engine.AdBlocker;
import com.cobrow.browser.utils.ThemeUtils;

public class CobrowApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ThemeUtils.applyNightMode(this);
        AdBlocker.getInstance().loadFilters(this);
    }
}
