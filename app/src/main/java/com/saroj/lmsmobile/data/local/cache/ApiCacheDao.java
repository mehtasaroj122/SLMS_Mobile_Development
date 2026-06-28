package com.saroj.lmsmobile.data.local.cache;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface ApiCacheDao {
    @Query("SELECT * FROM api_cache WHERE cache_key = :cacheKey LIMIT 1")
    ApiCacheEntity get(String cacheKey);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(ApiCacheEntity entity);

    @Query("DELETE FROM api_cache WHERE cache_key = :cacheKey")
    void delete(String cacheKey);

    @Query("DELETE FROM api_cache WHERE cache_key LIKE :prefix || '%'")
    void deleteByPrefix(String prefix);

    @Query("DELETE FROM api_cache")
    void clear();
}
