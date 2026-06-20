package com.saroj.lmsmobile.ui.student.notifications

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saroj.lmsmobile.data.models.common.NetworkResult
import com.saroj.lmsmobile.data.models.notification.AppNotification
import com.saroj.lmsmobile.data.repository.NotificationRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class NotificationTab {
    ALL,
    UNREAD,
    READ
}

class NotificationsViewModel(
    private val repository: NotificationRepository
) : ViewModel() {

    private val _notifications = MutableLiveData<List<AppNotification>>(emptyList())
    val notifications: LiveData<List<AppNotification>> = _notifications

    private val _filteredNotifications = MutableLiveData<List<AppNotification>>(emptyList())
    val filteredNotifications: LiveData<List<AppNotification>> = _filteredNotifications

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _unreadCount = MutableLiveData(0)
    val unreadCount: LiveData<Int> = _unreadCount

    private val _selectedTab = MutableLiveData(NotificationTab.ALL)
    val selectedTab: LiveData<NotificationTab> = _selectedTab

    private val _actionMessage = MutableLiveData<String?>()
    val actionMessage: LiveData<String?> = _actionMessage

    private val _unauthorized = MutableLiveData(false)
    val unauthorized: LiveData<Boolean> = _unauthorized

    fun loadNotifications() {
        viewModelScope.launch {
            repository.getNotifications().collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _isLoading.value = true
                        _errorMessage.value = null
                    }
                    is NetworkResult.Success -> {
                        _isLoading.value = false
                        _notifications.value = result.data.sortedByDescending { parseDateMillis(it.createdAt) }
                        publishUnreadCountFromCache()
                        filterByTab()
                        loadCount()
                    }
                    is NetworkResult.Error -> {
                        _isLoading.value = false
                        _errorMessage.value = result.message
                    }
                    is NetworkResult.Unauthorized -> {
                        _isLoading.value = false
                        _unauthorized.value = true
                    }
                }
            }
        }
    }

    fun refreshNotifications() {
        loadNotifications()
    }

    fun loadCount() {
        viewModelScope.launch {
            repository.getNotificationCount().collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _unreadCount.value = result.data.unreadCount
                            ?: _notifications.value.orEmpty().count { it.isUnread() }
                    }
                    is NetworkResult.Unauthorized -> _unauthorized.value = true
                    else -> Unit
                }
            }
        }
    }

    fun switchTab(tab: NotificationTab) {
        _selectedTab.value = tab
        filterByTab()
    }

    fun markAsRead(notification: AppNotification) {
        val id = notification.id ?: return
        if (!notification.isUnread()) return

        viewModelScope.launch {
            repository.markAsRead(id).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        updateNotificationReadState(id)
                        _actionMessage.value = result.data.message ?: "Notification marked as read."
                    }
                    is NetworkResult.Error -> _actionMessage.value = "Unable to mark as read: ${result.message}"
                    is NetworkResult.Unauthorized -> _unauthorized.value = true
                    is NetworkResult.Loading -> Unit
                }
            }
        }
    }

    fun markAllAsRead() {
        if ((_unreadCount.value ?: 0) == 0) return

        viewModelScope.launch {
            repository.markAllAsRead().collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        val now = currentTimestamp()
                        _notifications.value = _notifications.value.orEmpty().map {
                            if (it.isUnread()) it.copy(readAt = now) else it
                        }
                        _unreadCount.value = 0
                        filterByTab()
                        _actionMessage.value = result.data.message ?: "All notifications marked as read."
                    }
                    is NetworkResult.Error -> _actionMessage.value = "Unable to mark all as read: ${result.message}"
                    is NetworkResult.Unauthorized -> _unauthorized.value = true
                    is NetworkResult.Loading -> Unit
                }
            }
        }
    }

    fun deleteNotification(notification: AppNotification) {
        val id = notification.id ?: return

        viewModelScope.launch {
            repository.deleteNotification(id).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _notifications.value = _notifications.value.orEmpty().filterNot { it.id == id }
                        publishUnreadCountFromCache()
                        filterByTab()
                        _actionMessage.value = result.data.message ?: "Notification deleted."
                    }
                    is NetworkResult.Error -> _actionMessage.value = "Unable to delete notification: ${result.message}"
                    is NetworkResult.Unauthorized -> _unauthorized.value = true
                    is NetworkResult.Loading -> Unit
                }
            }
        }
    }

    fun filterByTab() {
        val tab = _selectedTab.value ?: NotificationTab.ALL
        _filteredNotifications.value = _notifications.value.orEmpty().filter { notification ->
            when (tab) {
                NotificationTab.ALL -> true
                NotificationTab.UNREAD -> notification.isUnread()
                NotificationTab.READ -> !notification.isUnread()
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    private fun updateNotificationReadState(id: Int) {
        val wasUnread = _notifications.value.orEmpty().firstOrNull { it.id == id }?.isUnread() == true
        _notifications.value = _notifications.value.orEmpty().map {
            if (it.id == id) it.copy(readAt = currentTimestamp()) else it
        }
        if (wasUnread) {
            _unreadCount.value = ((_unreadCount.value ?: 0) - 1).coerceAtLeast(0)
        }
        filterByTab()
    }

    private fun publishUnreadCountFromCache() {
        _unreadCount.value = _notifications.value.orEmpty().count { it.isUnread() }
    }

    private fun currentTimestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
    }

    private fun parseDateMillis(rawDate: String?): Long {
        if (rawDate.isNullOrBlank()) return 0L
        val normalized = rawDate.trim()
            .replace(Regex("\\.\\d+Z$"), "Z")
            .replace(Regex("\\.\\d+$"), "")
        val timezonePatterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ssXXX"
        )
        val localPatterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd"
        )

        timezonePatterns.firstNotNullOfOrNull { pattern ->
            runCatching {
                SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.parse(normalized)?.time
            }.getOrNull()
        }?.let { return it }

        return localPatterns.firstNotNullOfOrNull { pattern ->
            runCatching {
                SimpleDateFormat(pattern, Locale.US).parse(normalized)?.time
            }.getOrNull()
        } ?: 0L
    }
}
