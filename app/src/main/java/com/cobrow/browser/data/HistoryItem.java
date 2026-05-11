package com.cobrow.browser.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "history")
public class HistoryItem {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String title;
    public String url;
    public long visitedAt;

    public HistoryItem(String title, String url) {
        this.title = title;
        this.url = url;
        this.visitedAt = System.currentTimeMillis();
    }
}
