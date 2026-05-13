package com.cobrow.browser.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "site_preferences")
public class SitePreference {
    @PrimaryKey
    @NonNull
    public String host;
    public boolean javascriptEnabled;
    public boolean cameraAllowed;
    public boolean locationAllowed;

    public SitePreference(@NonNull String host, boolean javascriptEnabled, boolean cameraAllowed, boolean locationAllowed) {
        this.host = host;
        this.javascriptEnabled = javascriptEnabled;
        this.cameraAllowed = cameraAllowed;
        this.locationAllowed = locationAllowed;
    }
}
