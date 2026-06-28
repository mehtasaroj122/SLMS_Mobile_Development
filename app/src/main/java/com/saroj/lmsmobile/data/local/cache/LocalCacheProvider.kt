package com.saroj.lmsmobile.data.local.cache

object LocalCacheProvider {
    @Volatile
    var cache: LocalCacheRepository? = null
        private set

    fun initialize(repository: LocalCacheRepository) {
        cache = repository
    }
}
