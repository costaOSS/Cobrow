package com.cobrow.browser.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "downloads")
public class DownloadItem {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String fileName;
    public String url;
    public String path;
    public long size;
    public String mimeType;
    public long timestamp;

    public DownloadItem(String fileName, String url, String path, long size, String mimeType) {
        this.fileName = fileName;
        this.url = url;
        this.path = path;
        this.size = size;
        this.mimeType = mimeType;
        this.timestamp = System.currentTimeMillis();
    }
}
