package com.cobrow.browser.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "credentials")
public class Credential {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String host;
    public String username;
    public String password; // Should ideally be encrypted

    public Credential(String host, String username, String password) {
        this.host = host;
        this.username = username;
        this.password = password;
    }
}
