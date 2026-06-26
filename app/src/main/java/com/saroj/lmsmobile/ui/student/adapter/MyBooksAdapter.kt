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
import com.saroj.lmsmobile.ui.student.model.FineStatus
import com.saroj.lmsmobile.ui.student.model.MyBookStatus
import com.saroj.lmsmobile.ui.student.model.MyBookUiModel
import com.saroj.lmsmobile.utils.Constants
import java.net.URL
import kotlin.concurrent.thread

class MyBooksAdapter(
    private val onDetailsClick: (MyBookUiModel) -> Unit
) : ListAdapter<MyBookUiModel, MyBooksAdapter.MyBookViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyBookViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_book, parent, false)
        return MyBookViewHolder(view, onDetailsClick)
    }

    override fun onBindViewHolder(holder: MyBookViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MyBookViewHolder(
        itemView: View,
        private val onDetailsClick: (MyBookUiModel) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val iconTile: View = itemView.findViewById(R.id.viewMyBookIconTile)
        private val coverImage: ImageView = itemView.findViewById(R.id.imageMyBookCover)
        private val icon: ImageView = itemView.findViewById(R.id.imageMyBookIcon)
        private val initialText: TextView = itemView.findViewById(R.id.textMyBookInitial)
        private val title: TextView = itemView.findViewById(R.id.textMyBookTitle)
        private val author: TextView = itemView.findViewById(R.id.textMyBookAuthor)
        private val isbn: TextView = itemView.findViewById(R.id.textMyBookIsbn)
        private val statusBadge: TextView = itemView.findViewById(R.id.textMyBookStatus)
        private val issueDate: TextView = itemView.findViewById(R.id.textMyBookIssueDate)
        private val dueDate: TextView = itemView.findViewById(R.id.textMyBookDueDate)
        private val returnDate: TextView = itemView.findViewById(R.id.textMyBookReturnDate)
        private val fineAmount: TextView = itemView.findViewById(R.id.textMyBookFineAmount)
        private val fineStatus: TextView = itemView.findViewById(R.id.textMyBookFineStatus)
        private val detailsButton: View = itemView.findViewById(R.id.buttonViewMyBookDetails)
        private val detailsChevron: ImageView = itemView.findViewById(R.id.imageViewDetailsChevron)

        fun bind(book: MyBookUiModel) {
            title.text = book.title
            author.text = book.author
            isbn.text = "ISBN: ${book.isbn}"
            issueDate.text = book.issueDate
            dueDate.text = book.dueDate
            returnDate.text = book.returnDate ?: "-"
            fineAmount.text = book.fineAmount

            bindStatus(book.status)
            bindFineStatus(book.fineStatus)
            bindCover(book)
            detailsChevron.setColorFilter(color(R.color.my_books_primary))
            detailsButton.setOnClickListener { onDetailsClick(book) }
        }

        private fun bindStatus(status: MyBookStatus) {
            val style = StatusStyle.from(status)
            statusBadge.text = style.label
            statusBadge.setBackgroundResource(style.badgeBackground)
            statusBadge.setTextColor(color(style.badgeTextColor))
            iconTile.setBackgroundResource(style.iconBackground)
            icon.setColorFilter(color(style.iconTint))
            initialText.setTextColor(color(style.iconTint))
        }

        private fun bindFineStatus(status: FineStatus) {
            val style = FineStyle.from(status)
            fineStatus.text = style.label
            fineStatus.setBackgroundResource(style.background)
            fineStatus.setTextColor(color(style.textColor))
        }

        private fun bindCover(book: MyBookUiModel) {
            val coverUrl = normalizeCoverUrl(book.coverImageUrl)
            coverImage.tag = coverUrl
            coverImage.setImageDrawable(null)
            coverImage.visibility = View.GONE
            icon.visibility = View.GONE
            initialText.text = book.title.firstOrNull()?.uppercaseChar()?.toString() ?: "B"
            initialText.visibility = View.VISIBLE

            if (coverUrl.isNullOrBlank()) return

            thread(name = "my-book-cover-${book.issueId}", isDaemon = true) {
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
                    .let { Constants.normalizeLaravelAssetUrl(it) ?: it }
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
        val iconTint: Int
    ) {
        companion object {
            fun from(status: MyBookStatus): StatusStyle = when (status) {
                MyBookStatus.ISSUED -> StatusStyle(
                    label = "ISSUED",
                    badgeBackground = R.drawable.bg_my_books_badge_issued,
                    badgeTextColor = R.color.my_books_primary,
                    iconBackground = R.drawable.bg_my_books_icon_tile_blue,
                    iconTint = R.color.my_books_primary
                )
                MyBookStatus.DUE_SOON -> StatusStyle(
                    label = "DUE SOON",
                    badgeBackground = R.drawable.bg_badge_due_soon,
                    badgeTextColor = R.color.my_books_orange,
                    iconBackground = R.drawable.bg_my_books_icon_tile_orange,
                    iconTint = R.color.my_books_orange
                )
                MyBookStatus.OVERDUE -> StatusStyle(
                    label = "OVERDUE",
                    badgeBackground = R.drawable.bg_badge_overdue,
                    badgeTextColor = R.color.my_books_red_dark,
                    iconBackground = R.drawable.bg_my_books_icon_tile_red,
                    iconTint = R.color.my_books_red
                )
                MyBookStatus.RETURNED -> StatusStyle(
                    label = "RETURNED",
                    badgeBackground = R.drawable.bg_badge_returned,
                    badgeTextColor = R.color.my_books_green,
                    iconBackground = R.drawable.bg_my_books_icon_tile_green,
                    iconTint = R.color.my_books_green
                )
            }
        }
    }

    private data class FineStyle(
        val label: String,
        val background: Int,
        val textColor: Int
    ) {
        companion object {
            fun from(status: FineStatus): FineStyle = when (status) {
                FineStatus.NONE -> FineStyle("NONE", R.drawable.bg_fine_none, R.color.my_books_text_muted)
                FineStatus.UNPAID -> FineStyle("UNPAID", R.drawable.bg_fine_pending, R.color.my_books_orange_dark)
                FineStatus.PAID -> FineStyle("PAID", R.drawable.bg_fine_paid, R.color.my_books_green)
                FineStatus.WAIVED -> FineStyle("WAIVED", R.drawable.bg_fine_waived, R.color.my_books_purple)
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<MyBookUiModel>() {
        override fun areItemsTheSame(oldItem: MyBookUiModel, newItem: MyBookUiModel): Boolean {
            return oldItem.issueId == newItem.issueId
        }

        override fun areContentsTheSame(oldItem: MyBookUiModel, newItem: MyBookUiModel): Boolean {
            return oldItem == newItem
        }
    }
}
