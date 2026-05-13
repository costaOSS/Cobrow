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

    @Query("SELECT * FROM bookmarks WHERE folder = :folder ORDER BY createdAt DESC")
    List<Bookmark> getByFolder(String folder);

    @Query("SELECT * FROM bookmarks WHERE folder IS NULL OR folder = '' ORDER BY createdAt DESC")
    List<Bookmark> getRoot();

    @Query("SELECT DISTINCT folder FROM bookmarks WHERE folder IS NOT NULL AND folder != ''")
    List<String> getFolders();

    @Query("SELECT * FROM bookmarks WHERE title LIKE '%' || :q || '%' OR url LIKE '%' || :q || '%' ORDER BY createdAt DESC")
    List<Bookmark> search(String q);
}
