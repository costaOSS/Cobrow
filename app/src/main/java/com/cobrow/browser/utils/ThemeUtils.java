package com.cobrow.browser.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatDelegate;

public class ThemeUtils {
    public static final String PREFS = "cobrow_prefs";
    public static final String PREF_THEME_MODE = "theme_mode";
    public static final String PREF_ACCENT_COLOR = "accent_color";

    public static final String[] THEME_MODE_NAMES = {"System", "Light", "Dark"};
    public static final String[] THEME_MODE_VALUES = {"system", "light", "dark"};

    public static final String[] ACCENT_NAMES = {"Indigo", "Teal", "Green", "Orange", "Red"};
    public static final int[] ACCENT_COLORS = {
            Color.rgb(92, 107, 192),
            Color.rgb(0, 150, 136),
            Color.rgb(67, 160, 71),
            Color.rgb(251, 140, 0),
            Color.rgb(229, 57, 53)
    };

    private ThemeUtils() {}

    public static void applyNightMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String mode = prefs.getString(PREF_THEME_MODE, "system");
        if ("dark".equals(mode)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else if ("light".equals(mode)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    public static int getAccentColor(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getInt(PREF_ACCENT_COLOR, ACCENT_COLORS[0]);
    }

    public static void applySystemBars(Activity activity) {
        int accent = getAccentColor(activity);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.getWindow().setStatusBarColor(darken(accent));
            activity.getWindow().setNavigationBarColor(darken(accent));
        }
    }

    public static void tintProgress(ProgressBar progressBar, int color) {
        if (progressBar == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return;
        progressBar.setProgressTintList(ColorStateList.valueOf(color));
        progressBar.setIndeterminateTintList(ColorStateList.valueOf(color));
    }

    private static int darken(int color) {
        return Color.rgb(
                Math.max(0, (int) (Color.red(color) * 0.78f)),
                Math.max(0, (int) (Color.green(color) * 0.78f)),
                Math.max(0, (int) (Color.blue(color) * 0.78f)));
    }
}
