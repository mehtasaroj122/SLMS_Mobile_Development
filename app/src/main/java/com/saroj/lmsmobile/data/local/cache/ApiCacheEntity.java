package com.saroj.lmsmobile.data.local.cache;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "api_cache")
public class ApiCacheEntity {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "cache_key")
    public String cacheKey;

    @NonNull
    @ColumnInfo(name = "payload")
    public String payload;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    public ApiCacheEntity(@NonNull String cacheKey, @NonNull String payload, long updatedAt) {
        this.cacheKey = cacheKey;
        this.payload = payload;
        this.updatedAt = updatedAt;
    }
}
