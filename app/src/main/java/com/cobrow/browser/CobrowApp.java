package com.cobrow.browser;

import android.app.Application;
import com.cobrow.browser.engine.AdBlocker;

public class CobrowApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AdBlocker.getInstance().loadFilters(this);
    }
}
