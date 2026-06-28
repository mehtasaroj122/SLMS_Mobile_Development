package com.saroj.lmsmobile.utils

import com.saroj.lmsmobile.network.NoConnectivityException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object NetworkMessages {
    const val OFFLINE_SAVED_DATA = "You are offline. Showing saved data."
    const val OFFLINE_CANNOT_REFRESH = "You are offline. Cannot refresh right now."
    const val OFFLINE_RETRY = "No internet connection. Please reconnect and try again."
    const val OFFLINE_LOGIN = "No internet connection. Please connect to the internet to login."
    const val BACK_ONLINE_REFRESHING = "Back online. Refreshing data..."
    const val NO_SAVED_DATA = "No saved data available. Connect to internet to load latest data."

    fun forException(throwable: Throwable): String = when (throwable) {
        is NoConnectivityException, is UnknownHostException -> Constants.ERROR_NO_INTERNET
        is SocketTimeoutException -> "Connection is slow. Please try again."
        else -> throwable.message ?: Constants.ERROR_UNKNOWN
    }
}
