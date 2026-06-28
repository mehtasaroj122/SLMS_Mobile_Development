package com.saroj.lmsmobile.data.local.cache

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.reflect.Type

class LocalCacheRepository(
    private val dao: ApiCacheDao,
    private val gson: Gson = Gson()
) {
    suspend fun <T> read(cacheKey: String, clazz: Class<T>): T? = withContext(Dispatchers.IO) {
        dao.get(cacheKey)?.payload?.let { payload ->
            runCatching { gson.fromJson(payload, clazz) }.getOrNull()
        }
    }

    suspend fun <T> read(cacheKey: String, type: Type): T? = withContext(Dispatchers.IO) {
        dao.get(cacheKey)?.payload?.let { payload ->
            runCatching { gson.fromJson<T>(payload, type) }.getOrNull()
        }
    }

    suspend fun write(cacheKey: String, value: Any) = withContext(Dispatchers.IO) {
        dao.upsert(ApiCacheEntity(cacheKey, gson.toJson(value), System.currentTimeMillis()))
    }

    suspend fun delete(cacheKey: String) = withContext(Dispatchers.IO) {
        dao.delete(cacheKey)
    }

    suspend fun deleteByPrefix(prefix: String) = withContext(Dispatchers.IO) {
        dao.deleteByPrefix(prefix)
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        dao.clear()
    }
}
