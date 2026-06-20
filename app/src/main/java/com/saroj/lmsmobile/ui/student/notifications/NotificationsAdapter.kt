package com.saroj.lmsmobile.ui.student.notifications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.saroj.lmsmobile.R
import com.saroj.lmsmobile.data.models.notification.AppNotification
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class NotificationsAdapter(
    private val onNotificationClick: (AppNotification) -> Unit,
    private val onDeleteClick: (AppNotification) -> Unit
) : ListAdapter<AppNotification, NotificationsAdapter.NotificationViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view, onNotificationClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class NotificationViewHolder(
        itemView: View,
        private val onNotificationClick: (AppNotification) -> Unit,
        private val onDeleteClick: (AppNotification) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val accent: View = itemView.findViewById(R.id.viewNotificationAccent)
        private val iconTile: View = itemView.findViewById(R.id.viewNotificationIconTile)
        private val icon: ImageView = itemView.findViewById(R.id.imageNotificationIcon)
        private val unreadDot: View = itemView.findViewById(R.id.viewNotificationUnreadDot)
        private val deleteIcon: ImageView = itemView.findViewById(R.id.imageNotificationDelete)
        private val title: TextView = itemView.findViewById(R.id.textNotificationTitle)
        private val message: TextView = itemView.findViewById(R.id.textNotificationMessage)
        private val time: TextView = itemView.findViewById(R.id.textNotificationTime)
        private val typeBadge: TextView = itemView.findViewById(R.id.textNotificationType)

        fun bind(notification: AppNotification) {
            val unread = notification.isUnread()
            val style = NotificationStyle.from(notification.type)
            itemView.setBackgroundResource(
                if (unread) R.drawable.bg_notification_card_unread else R.drawable.bg_notification_card
            )
            accent.visibility = if (unread) View.VISIBLE else View.INVISIBLE
            unreadDot.visibility = if (unread) View.VISIBLE else View.GONE

            title.text = notification.title?.takeIf { it.isNotBlank() } ?: "Notification"
            message.text = notification.message?.takeIf { it.isNotBlank() } ?: "-"
            time.text = formatTimeAgo(notification.createdAt)
            typeBadge.text = style.badgeLabel
            typeBadge.setBackgroundResource(style.badgeBackground)
            typeBadge.setTextColor(color(style.tintColor))
            iconTile.setBackgroundResource(style.iconBackground)
            icon.setImageResource(style.iconDrawable)
            icon.setColorFilter(color(style.tintColor))

            itemView.setOnClickListener { onNotificationClick(notification) }
            deleteIcon.setOnClickListener { onDeleteClick(notification) }
        }

        private fun color(colorRes: Int): Int = ContextCompat.getColor(itemView.context, colorRes)
    }

    private data class NotificationStyle(
        val badgeLabel: String,
        val iconDrawable: Int,
        val iconBackground: Int,
        val badgeBackground: Int,
        val tintColor: Int
    ) {
        companion object {
            fun from(rawType: String?): NotificationStyle {
                val type = rawType.orEmpty().lowercase(Locale.US)
                return when {
                    type.contains("returned") -> blue("BOOK", R.drawable.ic_book)
                    type.contains("approved") -> green("BOOK", R.drawable.ic_check_circle)
                    type.contains("rejected") -> red("BOOK", R.drawable.ic_x_circle)
                    type.contains("fine") && type.contains("paid") -> green("FINE", R.drawable.ic_rupee)
                    type.contains("fine") -> orange("FINE", R.drawable.ic_warning)
                    type.contains("account") || type.contains("status") -> purple("ACCOUNT", R.drawable.ic_shield)
                    else -> blue("INFO", R.drawable.ic_bell)
                }
            }

            private fun blue(label: String, icon: Int) = NotificationStyle(
                label,
                icon,
                R.drawable.bg_notification_icon_blue,
                R.drawable.bg_notification_unread_badge,
                R.color.notifications_primary
            )

            private fun green(label: String, icon: Int) = NotificationStyle(
                label,
                icon,
                R.drawable.bg_notification_icon_green,
                R.drawable.bg_notification_badge_green,
                R.color.notifications_green
            )

            private fun red(label: String, icon: Int) = NotificationStyle(
                label,
                icon,
                R.drawable.bg_notification_icon_red,
                R.drawable.bg_notification_badge_red,
                R.color.notifications_red
            )

            private fun orange(label: String, icon: Int) = NotificationStyle(
                label,
                icon,
                R.drawable.bg_notification_icon_orange,
                R.drawable.bg_notification_badge_orange,
                R.color.notifications_orange
            )

            private fun purple(label: String, icon: Int) = NotificationStyle(
                label,
                icon,
                R.drawable.bg_notification_icon_purple,
                R.drawable.bg_notification_badge_purple,
                R.color.notifications_purple
            )
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<AppNotification>() {
        override fun areItemsTheSame(oldItem: AppNotification, newItem: AppNotification): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AppNotification, newItem: AppNotification): Boolean {
            return oldItem == newItem
        }
    }

    companion object {
        fun formatTimeAgo(createdAt: String?): String {
            val date = parseDate(createdAt) ?: return createdAt.orEmpty()
            val diffMillis = (System.currentTimeMillis() - date.time).coerceAtLeast(0L)
            if (diffMillis < TimeUnit.MINUTES.toMillis(1)) return "Just now"
            if (diffMillis < TimeUnit.HOURS.toMillis(1)) {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis)
                return if (minutes == 1L) "1 min ago" else "$minutes mins ago"
            }
            if (diffMillis < TimeUnit.DAYS.toMillis(1)) {
                val hours = TimeUnit.MILLISECONDS.toHours(diffMillis)
                return if (hours == 1L) "1 hr ago" else "$hours hrs ago"
            }
            if (diffMillis < TimeUnit.DAYS.toMillis(2)) return "Yesterday"
            return SimpleDateFormat("MMM dd, yyyy", Locale.US).format(date)
        }

        fun formatFullDateTime(createdAt: String?): String {
            val date = parseDate(createdAt) ?: return createdAt.orEmpty()
            return SimpleDateFormat("MMM dd, yyyy, h:mm a", Locale.US).format(date)
        }

        private fun parseDate(rawDate: String?): Date? {
            if (rawDate.isNullOrBlank()) return null
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
                    }.parse(normalized)
                }.getOrNull()
            }?.let { return it }

            return localPatterns.firstNotNullOfOrNull { pattern ->
                runCatching {
                    SimpleDateFormat(pattern, Locale.US).parse(normalized)
                }.getOrNull()
            }
        }
    }
}
