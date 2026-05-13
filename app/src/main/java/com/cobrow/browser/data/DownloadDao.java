package com.cobrow.browser.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface DownloadDao {
    @Insert
    void insert(DownloadItem item);

    @Delete
    void delete(DownloadItem item);

    @Query("SELECT * FROM downloads ORDER BY timestamp DESC")
    List<DownloadItem> getAll();

    @Query("DELETE FROM downloads")
    void clearAll();
}
