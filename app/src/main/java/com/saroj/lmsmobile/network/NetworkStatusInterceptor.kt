package com.saroj.lmsmobile.network

import okhttp3.Interceptor
import okhttp3.Response

class NetworkStatusInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        if (!NetworkMonitor.isCurrentlyOnline()) {
            throw NoConnectivityException()
        }
        return chain.proceed(chain.request())
    }
}
