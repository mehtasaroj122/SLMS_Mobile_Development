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
import com.saroj.lmsmobile.ui.student.model.MyRequestStatus
import com.saroj.lmsmobile.ui.student.model.MyRequestUiModel
import java.net.URL
import kotlin.concurrent.thread

class MyRequestsAdapter(
    private val onCancelClick: (MyRequestUiModel) -> Unit,
    private val onDetailsClick: (MyRequestUiModel) -> Unit
) : ListAdapter<MyRequestUiModel, MyRequestsAdapter.MyRequestViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyRequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_request, parent, false)
        return MyRequestViewHolder(view, onCancelClick, onDetailsClick)
    }

    override fun onBindViewHolder(holder: MyRequestViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MyRequestViewHolder(
        itemView: View,
        private val onCancelClick: (MyRequestUiModel) -> Unit,
        private val onDetailsClick: (MyRequestUiModel) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val iconTile: View = itemView.findViewById(R.id.viewMyRequestIconTile)
        private val coverImage: ImageView = itemView.findViewById(R.id.imageMyRequestCover)
        private val icon: ImageView = itemView.findViewById(R.id.imageMyRequestIcon)
        private val initialText: TextView = itemView.findViewById(R.id.textMyRequestInitial)
        private val title: TextView = itemView.findViewById(R.id.textMyRequestTitle)
        private val author: TextView = itemView.findViewById(R.id.textMyRequestAuthor)
        private val statusBadge: TextView = itemView.findViewById(R.id.textMyRequestStatus)
        private val requestDate: TextView = itemView.findViewById(R.id.textMyRequestDate)
        private val processedBy: TextView = itemView.findViewById(R.id.textMyRequestProcessedBy)
        private val message: TextView = itemView.findViewById(R.id.textMyRequestMessage)
        private val actionButton: View = itemView.findViewById(R.id.buttonMyRequestAction)
        private val actionIcon: ImageView = itemView.findViewById(R.id.imageMyRequestAction)
        private val actionText: TextView = itemView.findViewById(R.id.textMyRequestAction)

        fun bind(request: MyRequestUiModel) {
            title.text = request.bookTitle
            author.text = request.author
            requestDate.text = request.requestDate
            processedBy.text = request.processedBy
            message.text = request.message

            bindStatus(request.status)
            bindCover(request)
            bindAction(request)
        }

        private fun bindStatus(status: MyRequestStatus) {
            val style = StatusStyle.from(status)
            statusBadge.text = style.label
            statusBadge.setBackgroundResource(style.badgeBackground)
            statusBadge.setTextColor(color(style.badgeTextColor))
            iconTile.setBackgroundResource(style.iconBackground)
            icon.setImageResource(style.iconDrawable)
            icon.setColorFilter(color(style.iconTint))
            initialText.setTextColor(color(style.iconTint))
        }

        private fun bindCover(request: MyRequestUiModel) {
            val coverUrl = request.coverImageUrl
            coverImage.tag = coverUrl
            coverImage.setImageDrawable(null)
            coverImage.visibility = View.GONE
            icon.visibility = View.GONE
            initialText.text = request.bookTitle.firstOrNull()?.uppercaseChar()?.toString() ?: "B"
            initialText.visibility = View.VISIBLE

            if (coverUrl.isNullOrBlank()) return

            thread(name = "my-request-cover-${request.id}", isDaemon = true) {
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

        private fun bindAction(request: MyRequestUiModel) {
            val isPending = request.status == MyRequestStatus.PENDING
            actionButton.setBackgroundResource(
                if (isPending) R.drawable.bg_cancel_request_button else R.drawable.bg_view_request_button
            )
            actionIcon.setImageResource(if (isPending) R.drawable.ic_trash else R.drawable.ic_chevron_right)
            actionIcon.setColorFilter(color(if (isPending) R.color.my_requests_red else R.color.my_requests_primary))
            actionText.text = if (isPending) "Cancel Request" else "View Details"
            actionText.setTextColor(color(if (isPending) R.color.my_requests_red else R.color.my_requests_primary))
            actionButton.setOnClickListener {
                if (isPending) {
                    onCancelClick(request)
                } else {
                    onDetailsClick(request)
                }
            }
        }

        private fun color(colorRes: Int): Int = ContextCompat.getColor(itemView.context, colorRes)
    }

    private data class StatusStyle(
        val label: String,
        val badgeBackground: Int,
        val badgeTextColor: Int,
        val iconBackground: Int,
        val iconTint: Int,
        val iconDrawable: Int
    ) {
        companion object {
            fun from(status: MyRequestStatus): StatusStyle = when (status) {
                MyRequestStatus.PENDING -> StatusStyle(
                    label = "PENDING",
                    badgeBackground = R.drawable.bg_request_badge_pending,
                    badgeTextColor = R.color.my_requests_orange_dark,
                    iconBackground = R.drawable.bg_icon_tile_request_orange,
                    iconTint = R.color.my_requests_orange,
                    iconDrawable = R.drawable.ic_clock
                )
                MyRequestStatus.APPROVED -> StatusStyle(
                    label = "APPROVED",
                    badgeBackground = R.drawable.bg_request_badge_approved,
                    badgeTextColor = R.color.my_requests_green,
                    iconBackground = R.drawable.bg_icon_tile_request_green,
                    iconTint = R.color.my_requests_green,
                    iconDrawable = R.drawable.ic_check_circle
                )
                MyRequestStatus.REJECTED -> StatusStyle(
                    label = "REJECTED",
                    badgeBackground = R.drawable.bg_request_badge_rejected,
                    badgeTextColor = R.color.my_requests_red_dark,
                    iconBackground = R.drawable.bg_icon_tile_request_red,
                    iconTint = R.color.my_requests_red,
                    iconDrawable = R.drawable.ic_x_circle
                )
                MyRequestStatus.CANCELLED -> StatusStyle(
                    label = "CANCELLED",
                    badgeBackground = R.drawable.bg_request_badge_cancelled,
                    badgeTextColor = R.color.my_requests_gray,
                    iconBackground = R.drawable.bg_icon_tile_request_gray,
                    iconTint = R.color.my_requests_gray,
                    iconDrawable = R.drawable.ic_cancel
                )
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<MyRequestUiModel>() {
        override fun areItemsTheSame(oldItem: MyRequestUiModel, newItem: MyRequestUiModel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MyRequestUiModel, newItem: MyRequestUiModel): Boolean {
            return oldItem == newItem
        }
    }
}
