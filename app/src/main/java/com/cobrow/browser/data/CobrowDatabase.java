package com.cobrow.browser.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Bookmark.class, HistoryItem.class}, version = 1, exportSchema = false)
public abstract class CobrowDatabase extends RoomDatabase {
    private static CobrowDatabase instance;

    public abstract BookmarkDao bookmarkDao();
    public abstract HistoryDao historyDao();

    public static synchronized CobrowDatabase get(Context ctx) {
        if (instance == null) {
            instance = Room.databaseBuilder(ctx.getApplicationContext(), CobrowDatabase.class, "cobrow.db")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}
