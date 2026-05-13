package com.cobrow.browser.data;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class TabsManager {
    private static final String PREF = "tabs_prefs";
    private static final String KEY_TABS = "tabs";
    private static final String KEY_INDEX = "current_index";

    private final SharedPreferences prefs;

    public TabsManager(Context ctx) {
        prefs = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public synchronized List<Tab> getTabs() {
        String s = prefs.getString(KEY_TABS, null);
        List<Tab> out = new ArrayList<>();
        if (s == null) return out;
        try {
            JSONArray a = new JSONArray(s);
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.optJSONObject(i);
                if (o != null) out.add(Tab.fromJson(o));
            }
        } catch (Exception ignored) {}
        return out;
    }

    public synchronized void saveTabs(List<Tab> tabs) {
        JSONArray a = new JSONArray();
        for (Tab t : tabs) a.put(t.toJson());
        prefs.edit().putString(KEY_TABS, a.toString()).apply();
    }

    public synchronized int getCurrentIndex() {
        return prefs.getInt(KEY_INDEX, 0);
    }

    public synchronized void setCurrentIndex(int idx) {
        prefs.edit().putInt(KEY_INDEX, idx).apply();
    }

    public synchronized void addTab(Tab t) {
        List<Tab> tabs = getTabs();
        tabs.add(t);
        saveTabs(tabs);
        setCurrentIndex(tabs.size() - 1);
    }

    public synchronized void addTabAt(int idx, Tab t) {
        List<Tab> tabs = getTabs();
        if (idx < 0) idx = 0;
        if (idx > tabs.size()) idx = tabs.size();
        tabs.add(idx, t);
        saveTabs(tabs);
        setCurrentIndex(idx);
    }

    public synchronized void removeTab(int idx) {
        List<Tab> tabs = getTabs();
        if (idx < 0 || idx >= tabs.size()) return;
        tabs.remove(idx);
        if (tabs.isEmpty()) {
            saveTabs(tabs);
            setCurrentIndex(0);
            return;
        }
        int newIdx = Math.max(0, Math.min(getCurrentIndex(), tabs.size() - 1));
        saveTabs(tabs);
        setCurrentIndex(newIdx);
    }

    public synchronized void updateTab(int idx, Tab t) {
        List<Tab> tabs = getTabs();
        if (idx < 0 || idx >= tabs.size()) return;
        tabs.set(idx, t);
        saveTabs(tabs);
    }
}
