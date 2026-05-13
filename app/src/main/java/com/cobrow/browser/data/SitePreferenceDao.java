package com.cobrow.browser.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface SitePreferenceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(SitePreference preference);

    @Query("SELECT * FROM site_preferences WHERE host = :host LIMIT 1")
    SitePreference getByHost(String host);

    @Query("DELETE FROM site_preferences WHERE host = :host")
    void deleteByHost(String host);
}
