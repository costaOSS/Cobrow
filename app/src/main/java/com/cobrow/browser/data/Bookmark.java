package com.cobrow.browser.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "bookmarks")
public class Bookmark {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String title;
    public String url;
    public long createdAt;

    public Bookmark(String title, String url) {
        this.title = title;
        this.url = url;
        this.createdAt = System.currentTimeMillis();
    }
}
