package com.saroj.lmsmobile.data.repository

import com.saroj.lmsmobile.data.models.common.NetworkResult
import com.saroj.lmsmobile.network.NetworkMonitor
import com.saroj.lmsmobile.utils.NetworkMessages
import kotlinx.coroutines.flow.FlowCollector

internal suspend fun <T> FlowCollector<NetworkResult<T>>.stopIfOffline(cached: T?): Boolean {
    if (NetworkMonitor.isCurrentlyOnline()) return false
    if (cached == null) {
        emit(NetworkResult.Error(NetworkMessages.NO_SAVED_DATA))
    }
    return true
}

internal suspend fun <T> FlowCollector<NetworkResult<T>>.emitOfflineActionError(): Boolean {
    if (NetworkMonitor.isCurrentlyOnline()) return false
    emit(NetworkResult.Error(NetworkMessages.OFFLINE_RETRY))
    return true
}

internal fun Throwable.toRepositoryMessage(): String =
    NetworkMessages.forException(this)
