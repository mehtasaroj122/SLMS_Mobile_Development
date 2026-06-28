package com.saroj.lmsmobile.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.saroj.lmsmobile.data.local.cache.ApiCacheDao;
import com.saroj.lmsmobile.data.local.cache.ApiCacheEntity;

@Database(
    entities = {ApiCacheEntity.class},
    version = 1,
    exportSchema = false
)
public abstract class LmsDatabase extends RoomDatabase {
    private static volatile LmsDatabase instance;

    public abstract ApiCacheDao apiCacheDao();

    public static LmsDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (LmsDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            LmsDatabase.class,
                            "lms_mobile_cache.db"
                        )
                        .build();
                }
            }
        }
        return instance;
    }
}
