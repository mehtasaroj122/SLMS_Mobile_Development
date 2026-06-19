package com.saroj.lmsmobile.ui.student.adapter

import android.graphics.BitmapFactory
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
import com.saroj.lmsmobile.ui.student.model.MyFineStatus
import com.saroj.lmsmobile.ui.student.model.MyFineUiModel
import com.saroj.lmsmobile.utils.Constants
import java.net.URL
import kotlin.concurrent.thread

class MyFinesAdapter(
    private val onFineClick: (MyFineUiModel) -> Unit
) : ListAdapter<MyFineUiModel, MyFinesAdapter.MyFineViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyFineViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_fine, parent, false)
        return MyFineViewHolder(view, onFineClick)
    }

    override fun onBindViewHolder(holder: MyFineViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MyFineViewHolder(
        itemView: View,
        private val onFineClick: (MyFineUiModel) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val iconTile: View = itemView.findViewById(R.id.viewFineIconTile)
        private val coverImage: ImageView = itemView.findViewById(R.id.imageFineCover)
        private val icon: ImageView = itemView.findViewById(R.id.imageFineIcon)
        private val initialText: TextView = itemView.findViewById(R.id.textFineInitial)
        private val title: TextView = itemView.findViewById(R.id.textFineBookTitle)
        private val reason: TextView = itemView.findViewById(R.id.textFineReason)
        private val statusBadge: TextView = itemView.findViewById(R.id.textFineStatusBadge)
        private val dueDate: TextView = itemView.findViewById(R.id.textFineDueDate)
        private val daysOverdue: TextView = itemView.findViewById(R.id.textFineDaysOverdue)
        private val amount: TextView = itemView.findViewById(R.id.textFineAmount)

        fun bind(fine: MyFineUiModel) {
            title.text = fine.bookTitle
            reason.text = fine.reason
            dueDate.text = fine.dueDate
            daysOverdue.text = fine.daysOverdue
            amount.text = fine.amount
            bindStatus(fine.status)
            bindCoverOrInitial(fine)
            itemView.setOnClickListener { onFineClick(fine) }
        }

        private fun bindStatus(status: MyFineStatus) {
            val style = StatusStyle.from(status)
            statusBadge.text = style.label
            statusBadge.setBackgroundResource(style.badgeBackground)
            statusBadge.setTextColor(color(style.badgeTextColor))
            iconTile.setBackgroundResource(style.iconBackground)
            icon.setImageResource(style.icon)
            icon.setColorFilter(color(style.iconTint))
            initialText.setTextColor(color(style.iconTint))
        }

        private fun bindCoverOrInitial(fine: MyFineUiModel) {
            val coverUrl = normalizeCoverUrl(fine.coverImageUrl)
            coverImage.tag = coverUrl
            coverImage.setImageDrawable(null)
            coverImage.visibility = View.GONE
            icon.visibility = View.GONE
            initialText.text = fine.bookTitle.firstOrNull()?.uppercaseChar()?.toString() ?: "B"
            initialText.visibility = View.VISIBLE

            if (coverUrl.isNullOrBlank()) return

            thread(name = "my-fine-cover-${fine.id}", isDaemon = true) {
                val bitmap = runCatching {
                    URL(coverUrl).openStream().use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }.getOrNull()

                itemView.post {
                    if (coverImage.tag == coverUrl && bitmap != null) {
                        coverImage.setImageBitmap(bitmap)
                        coverImage.visibility = View.VISIBLE
                        initialText.visibility = View.GONE
                    }
                }
            }
        }

        private fun normalizeCoverUrl(rawCoverUrl: String?): String? {
            val value = rawCoverUrl?.trim()?.takeIf { it.isNotBlank() } ?: return null
            val apiRoot = Constants.BASE_URL.removeSuffix("api/").trimEnd('/')
            if (value.startsWith("http", ignoreCase = true)) {
                return value
                    .replace("http://127.0.0.1:8000", apiRoot)
                    .replace("http://localhost:8000", apiRoot)
                    .replace("https://127.0.0.1:8000", apiRoot)
                    .replace("https://localhost:8000", apiRoot)
            }

            val normalizedPath = value.trimStart('/')
            val storagePath = if (normalizedPath.startsWith("storage/", ignoreCase = true)) {
                normalizedPath
            } else {
                "storage/$normalizedPath"
            }
            return "$apiRoot/$storagePath"
        }

        private fun color(colorRes: Int): Int = ContextCompat.getColor(itemView.context, colorRes)
    }

    private data class StatusStyle(
        val label: String,
        val badgeBackground: Int,
        val badgeTextColor: Int,
        val iconBackground: Int,
        val iconTint: Int,
        val icon: Int
    ) {
        companion object {
            fun from(status: MyFineStatus): StatusStyle = when (status) {
                MyFineStatus.PENDING -> StatusStyle(
                    label = "PENDING",
                    badgeBackground = R.drawable.bg_fine_badge_pending,
                    badgeTextColor = R.color.my_fines_orange_dark,
                    iconBackground = R.drawable.bg_icon_tile_fine_red,
                    iconTint = R.color.my_fines_red,
                    icon = R.drawable.ic_exclamation_circle
                )
                MyFineStatus.PAID -> StatusStyle(
                    label = "PAID",
                    badgeBackground = R.drawable.bg_fine_badge_paid,
                    badgeTextColor = R.color.my_fines_green,
                    iconBackground = R.drawable.bg_icon_tile_fine_blue,
                    iconTint = R.color.my_fines_primary,
                    icon = R.drawable.ic_book
                )
                MyFineStatus.WAIVED -> StatusStyle(
                    label = "WAIVED",
                    badgeBackground = R.drawable.bg_fine_badge_waived,
                    badgeTextColor = R.color.my_fines_yellow,
                    iconBackground = R.drawable.bg_icon_tile_fine_yellow,
                    iconTint = R.color.my_fines_yellow,
                    icon = R.drawable.ic_shield_check
                )
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<MyFineUiModel>() {
        override fun areItemsTheSame(oldItem: MyFineUiModel, newItem: MyFineUiModel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MyFineUiModel, newItem: MyFineUiModel): Boolean {
            return oldItem == newItem
        }
    }
}
