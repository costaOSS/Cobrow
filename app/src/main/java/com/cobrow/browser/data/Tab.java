package com.cobrow.browser.data;

import org.json.JSONObject;

public class Tab {
    public String title;
    public String url;
    public String thumbnail; // base64 PNG
    public boolean incognito;
    public String groupId; // null means no group
    public int groupColor;

    public Tab(String title, String url) { this(title, url, null, false); }

    public Tab(String title, String url, String thumbnail, boolean incognito) {
        this.title = title;
        this.url = url;
        this.thumbnail = thumbnail;
        this.incognito = incognito;
        this.groupId = null;
        this.groupColor = 0;
    }

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("title", title != null ? title : "");
            o.put("url", url != null ? url : "");
            o.put("thumbnail", thumbnail != null ? thumbnail : JSONObject.NULL);
            o.put("incognito", incognito);
            o.put("groupId", groupId != null ? groupId : JSONObject.NULL);
            o.put("groupColor", groupColor);
        } catch (Exception ignored) {}
        return o;
    }

    public static Tab fromJson(JSONObject o) {
        if (o == null) return null;
        String thumb = null;
        try { thumb = o.optString("thumbnail", null); } catch (Exception ignored) {}
        boolean inc = o.optBoolean("incognito", false);
        Tab t = new Tab(o.optString("title", ""), o.optString("url", ""), thumb, inc);
        t.groupId = o.optString("groupId", null);
        t.groupColor = o.optInt("groupColor", 0);
        return t;
    }
}
