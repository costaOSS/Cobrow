package com.cobrow.browser.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface CredentialDao {
    @Insert
    void insert(Credential credential);

    @Delete
    void delete(Credential credential);

    @Query("SELECT * FROM credentials WHERE host = :host")
    List<Credential> getByHost(String host);

    @Query("DELETE FROM credentials")
    void clearAll();
}
