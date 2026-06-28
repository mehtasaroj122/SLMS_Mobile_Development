package com.saroj.lmsmobile.ui.components

import androidx.fragment.app.Fragment
import com.saroj.lmsmobile.network.NetworkMonitor
import com.saroj.lmsmobile.utils.LmsToast
import com.saroj.lmsmobile.utils.NetworkMessages

fun Fragment.isOnlineNow(): Boolean = NetworkMonitor.isCurrentlyOnline()

fun Fragment.showOfflineUiMessage(message: String = NetworkMessages.OFFLINE_CANNOT_REFRESH) {
    if (isAdded) {
        LmsToast.show(requireContext(), message)
    }
}

fun Fragment.runIfOnline(
    offlineMessage: String = NetworkMessages.OFFLINE_CANNOT_REFRESH,
    onOffline: () -> Unit = {},
    block: () -> Unit
) {
    if (isOnlineNow()) {
        block()
    } else {
        onOffline()
        showOfflineUiMessage(offlineMessage)
    }
}
