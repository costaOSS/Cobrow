package com.cobrow.browser.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface BookmarkDao {
    @Insert
    void insert(Bookmark bookmark);

    @Delete
    void delete(Bookmark bookmark);

    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    List<Bookmark> getAll();

    @Query("SELECT * FROM bookmarks WHERE url = :url LIMIT 1")
    Bookmark findByUrl(String url);
}
