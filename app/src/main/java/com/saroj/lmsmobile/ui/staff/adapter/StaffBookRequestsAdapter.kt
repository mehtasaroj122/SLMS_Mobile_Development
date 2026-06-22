package com.saroj.lmsmobile.ui.staff.adapter

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.saroj.lmsmobile.R
import com.saroj.lmsmobile.ui.staff.model.StaffBookRequestStatus
import com.saroj.lmsmobile.ui.staff.model.StaffBookRequestUiModel
import java.net.URL
import kotlin.concurrent.thread

class StaffBookRequestsAdapter(
    private val onAcceptClick: (StaffBookRequestUiModel) -> Unit,
    private val onRejectClick: (StaffBookRequestUiModel) -> Unit
) : ListAdapter<StaffBookRequestUiModel, StaffBookRequestsAdapter.StaffBookRequestViewHolder>(DiffCallback()) {

    private var actionRequestId: Int? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StaffBookRequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_staff_book_request, parent, false)
        return StaffBookRequestViewHolder(view, onAcceptClick, onRejectClick)
    }

    override fun onBindViewHolder(holder: StaffBookRequestViewHolder, position: Int) {
        holder.bind(getItem(position), actionRequestId)
    }

    fun setActionRequestId(requestId: Int?) {
        actionRequestId = requestId
        notifyDataSetChanged()
    }

    class StaffBookRequestViewHolder(
        itemView: View,
        private val onAcceptClick: (StaffBookRequestUiModel) -> Unit,
        private val onRejectClick: (StaffBookRequestUiModel) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val avatarImage: ImageView = itemView.findViewById(R.id.imageStaffRequestAvatar)
        private val initials: TextView = itemView.findViewById(R.id.textStaffRequestInitials)
        private val studentName: TextView = itemView.findViewById(R.id.textStaffRequestStudentName)
        private val studentId: TextView = itemView.findViewById(R.id.textStaffRequestStudentId)
        private val statusBadge: TextView = itemView.findViewById(R.id.textStaffRequestStatus)
        private val bookTitle: TextView = itemView.findViewById(R.id.textStaffRequestBookTitle)
        private val author: TextView = itemView.findViewById(R.id.textStaffRequestAuthor)
        private val requestDate: TextView = itemView.findViewById(R.id.textStaffRequestDate)
        private val processedBy: TextView = itemView.findViewById(R.id.textStaffRequestProcessedBy)
        private val processedAt: TextView = itemView.findViewById(R.id.textStaffRequestProcessedAt)
        private val actionRow: View = itemView.findViewById(R.id.layoutStaffRequestActions)
        private val acceptButton: View = itemView.findViewById(R.id.buttonStaffRequestAccept)
        private val rejectButton: View = itemView.findViewById(R.id.buttonStaffRequestReject)

        fun bind(request: StaffBookRequestUiModel, actionRequestId: Int?) {
            studentName.text = request.studentName
            studentId.text = "ID: ${request.studentIdentifier}"
            bookTitle.text = request.bookTitle
            author.text = request.author
            requestDate.text = request.requestDate
            processedBy.text = request.processedBy
            processedAt.text = request.processedAt

            bindAvatar(request)
            bindStatus(request.status)
            bindActions(request, actionRequestId)
        }

        private fun bindAvatar(request: StaffBookRequestUiModel) {
            val photoUrl = request.studentPhotoUrl
            avatarImage.tag = photoUrl
            avatarImage.setImageDrawable(null)
            avatarImage.visibility = View.GONE
            initials.text = initialsFor(request.studentName)
            initials.visibility = View.VISIBLE

            if (photoUrl.isNullOrBlank()) return

            thread(name = "staff-request-avatar-${request.id}", isDaemon = true) {
                val bitmap = runCatching {
                    URL(photoUrl).openStream().use { stream -> BitmapFactory.decodeStream(stream) }
                }.getOrNull()

                itemView.post {
                    if (avatarImage.tag == photoUrl && bitmap != null) {
                        avatarImage.setImageDrawable(
                            RoundedBitmapDrawableFactory.create(itemView.resources, bitmap).apply {
                                cornerRadius = 12f * itemView.resources.displayMetrics.density
                            }
                        )
                        avatarImage.visibility = View.VISIBLE
                        initials.visibility = View.GONE
                    }
                }
            }
        }

        private fun bindStatus(status: StaffBookRequestStatus) {
            val style = StatusStyle.from(status)
            statusBadge.text = style.label
            statusBadge.setBackgroundResource(style.badgeBackground)
            statusBadge.setTextColor(color(style.badgeTextColor))
        }

        private fun bindActions(request: StaffBookRequestUiModel, actionRequestId: Int?) {
            val isPending = request.status == StaffBookRequestStatus.PENDING
            actionRow.visibility = if (isPending) View.VISIBLE else View.GONE
            if (!isPending) return

            val isBusy = actionRequestId == request.id
            acceptButton.isEnabled = !isBusy
            rejectButton.isEnabled = !isBusy
            acceptButton.alpha = if (isBusy) 0.55f else 1f
            rejectButton.alpha = if (isBusy) 0.55f else 1f
            acceptButton.setOnClickListener { onAcceptClick(request) }
            rejectButton.setOnClickListener { onRejectClick(request) }
        }

        private fun initialsFor(name: String): String {
            return name.trim()
                .split(Regex("\\s+"))
                .filter { it.isNotBlank() }
                .take(2)
                .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
                .joinToString("")
                .ifBlank { "ST" }
        }

        private fun color(colorRes: Int): Int = ContextCompat.getColor(itemView.context, colorRes)
    }

    private data class StatusStyle(
        val label: String,
        val badgeBackground: Int,
        val badgeTextColor: Int
    ) {
        companion object {
            fun from(status: StaffBookRequestStatus): StatusStyle = when (status) {
                StaffBookRequestStatus.PENDING -> StatusStyle(
                    label = "PENDING",
                    badgeBackground = R.drawable.bg_request_badge_pending,
                    badgeTextColor = R.color.my_requests_orange_dark
                )
                StaffBookRequestStatus.APPROVED -> StatusStyle(
                    label = "APPROVED",
                    badgeBackground = R.drawable.bg_request_badge_approved,
                    badgeTextColor = R.color.my_requests_green
                )
                StaffBookRequestStatus.REJECTED -> StatusStyle(
                    label = "REJECTED",
                    badgeBackground = R.drawable.bg_request_badge_rejected,
                    badgeTextColor = R.color.my_requests_red_dark
                )
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<StaffBookRequestUiModel>() {
        override fun areItemsTheSame(
            oldItem: StaffBookRequestUiModel,
            newItem: StaffBookRequestUiModel
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: StaffBookRequestUiModel,
            newItem: StaffBookRequestUiModel
        ): Boolean = oldItem == newItem
    }
}
