package com.cobrow.browser.data;

import org.json.JSONObject;

public class Tab {
    public String title;
    public String url;

    public Tab(String title, String url) {
        this.title = title;
        this.url = url;
    }

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("title", title != null ? title : "");
            o.put("url", url != null ? url : "");
        } catch (Exception ignored) {}
        return o;
    }

    public static Tab fromJson(JSONObject o) {
        if (o == null) return null;
        return new Tab(o.optString("title", ""), o.optString("url", ""));
    }
}
