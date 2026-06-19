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
import com.saroj.lmsmobile.ui.student.model.BookRequestState
import com.saroj.lmsmobile.ui.student.model.StudentSearchBookUiModel
import com.saroj.lmsmobile.utils.Constants
import java.net.URL
import kotlin.concurrent.thread

class StudentSearchBookAdapter(
    private val onRequestClick: (StudentSearchBookUiModel) -> Unit
) : ListAdapter<StudentSearchBookUiModel, StudentSearchBookAdapter.BookViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student_search_book, parent, false)
        return BookViewHolder(view, onRequestClick)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class BookViewHolder(
        itemView: View,
        private val onRequestClick: (StudentSearchBookUiModel) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val iconTile: View = itemView.findViewById(R.id.viewIconTile)
        private val coverImage: ImageView = itemView.findViewById(R.id.imageBookCover)
        private val initialText: TextView = itemView.findViewById(R.id.textBookInitial)
        private val titleText: TextView = itemView.findViewById(R.id.textBookTitle)
        private val authorText: TextView = itemView.findViewById(R.id.textBookAuthor)
        private val statusBadge: TextView = itemView.findViewById(R.id.textStatusBadge)
        private val requestButton: TextView = itemView.findViewById(R.id.buttonBookRequest)

        fun bind(book: StudentSearchBookUiModel) {
            titleText.text = book.title
            authorText.text = book.author
            bindVisualState(book)
            bindCover(book)
            bindMetadata(book)

            requestButton.setOnClickListener { onRequestClick(book) }
        }

        private fun bindVisualState(book: StudentSearchBookUiModel) {
            val style = BookCardStyle.from(book)
            statusBadge.text = style.badgeText
            statusBadge.setBackgroundResource(style.badgeBackground)
            statusBadge.setTextColor(color(style.badgeTextColor))

            iconTile.setBackgroundResource(style.iconTileBackground)
            initialText.text = book.title.firstOrNull()?.uppercaseChar()?.toString() ?: "B"
            initialText.setTextColor(color(style.iconTint))

            requestButton.text = style.buttonText
            requestButton.setBackgroundResource(style.buttonBackground)
            requestButton.setTextColor(color(style.buttonTextColor))
            requestButton.alpha = if (book.requestState == BookRequestState.LOADING) 0.85f else 1f
            requestButton.compoundDrawablePadding = itemView.resources.getDimensionPixelSize(R.dimen.student_search_button_icon_gap)
            if (style.showArrow) {
                requestButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_right, 0)
            } else {
                requestButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            }
        }

        private fun bindCover(book: StudentSearchBookUiModel) {
            val coverUrl = normalizeCoverUrl(book.coverImageUrl)
            coverImage.tag = coverUrl
            coverImage.setImageDrawable(null)
            coverImage.visibility = View.GONE
            initialText.visibility = View.VISIBLE

            if (coverUrl.isNullOrBlank()) return

            thread(name = "book-cover-${bindingAdapterPosition}", isDaemon = true) {
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
            if (value.startsWith("http", ignoreCase = true)) {
                return value
                    .replace("http://127.0.0.1:8000", apiRootUrl())
                    .replace("http://localhost:8000", apiRootUrl())
                    .replace("https://127.0.0.1:8000", apiRootUrl())
                    .replace("https://localhost:8000", apiRootUrl())
            }

            return apiRootUrl().trimEnd('/') + "/" + value.trimStart('/')
        }

        private fun apiRootUrl(): String {
            return Constants.BASE_URL.removeSuffix("api/").trimEnd('/')
        }

        private fun bindMetadata(book: StudentSearchBookUiModel) {
            bindMeta(R.id.metaCategory, "Category", book.category)
            bindMeta(R.id.metaPublisher, "Publisher", book.publisher)
            bindMeta(R.id.metaCondition, "Condition", book.condition)
            bindMeta(R.id.metaAccession, "ISBN / Accession No.", book.accessionNo)
            bindMeta(R.id.metaLocation, "Location / Rack", book.location)
            bindMeta(
                R.id.metaCopies,
                "Available Copies",
                "${book.availableQuantity} / ${book.quantity}"
            )
        }

        private fun bindMeta(containerId: Int, label: String, value: String) {
            val container = itemView.findViewById<View>(containerId)
            container.findViewById<TextView>(R.id.textMetaLabel).text = label
            container.findViewById<TextView>(R.id.textMetaValue).text = value
        }

        private fun color(colorRes: Int): Int = ContextCompat.getColor(itemView.context, colorRes)
    }

    private data class BookCardStyle(
        val badgeText: String,
        val badgeBackground: Int,
        val badgeTextColor: Int,
        val iconTileBackground: Int,
        val iconTint: Int,
        val buttonText: String,
        val buttonBackground: Int,
        val buttonTextColor: Int,
        val showArrow: Boolean
    ) {
        companion object {
            fun from(book: StudentSearchBookUiModel): BookCardStyle {
                val effectiveState = if (book.availableQuantity <= 0) {
                    BookRequestState.UNAVAILABLE
                } else {
                    book.requestState
                }

                return when (effectiveState) {
                    BookRequestState.NONE -> BookCardStyle(
                        badgeText = "AVAILABLE",
                        badgeBackground = R.drawable.bg_badge_available,
                        badgeTextColor = R.color.search_success,
                        iconTileBackground = R.drawable.bg_icon_tile_blue,
                        iconTint = R.color.search_primary,
                        buttonText = "Request Book",
                        buttonBackground = R.drawable.bg_button_request,
                        buttonTextColor = R.color.white,
                        showArrow = true
                    )
                    BookRequestState.UNAVAILABLE -> BookCardStyle(
                        badgeText = "UNAVAILABLE",
                        badgeBackground = R.drawable.bg_badge_unavailable,
                        badgeTextColor = R.color.search_yellow_text,
                        iconTileBackground = R.drawable.bg_icon_tile_yellow,
                        iconTint = R.color.search_yellow_text,
                        buttonText = "Unavailable",
                        buttonBackground = R.drawable.bg_button_unavailable,
                        buttonTextColor = R.color.search_disabled_text,
                        showArrow = false
                    )
                    BookRequestState.PENDING -> BookCardStyle(
                        badgeText = "PENDING REQUEST",
                        badgeBackground = R.drawable.bg_badge_pending,
                        badgeTextColor = R.color.search_orange_dark,
                        iconTileBackground = R.drawable.bg_icon_tile_orange,
                        iconTint = R.color.search_orange_dark,
                        buttonText = "Pending Request",
                        buttonBackground = R.drawable.bg_button_pending,
                        buttonTextColor = R.color.white,
                        showArrow = false
                    )
                    BookRequestState.APPROVED -> BookCardStyle(
                        badgeText = "APPROVED",
                        badgeBackground = R.drawable.bg_badge_approved,
                        badgeTextColor = R.color.search_success,
                        iconTileBackground = R.drawable.bg_icon_tile_green,
                        iconTint = R.color.search_green,
                        buttonText = "Approved",
                        buttonBackground = R.drawable.bg_button_approved,
                        buttonTextColor = R.color.white,
                        showArrow = false
                    )
                    BookRequestState.ALREADY_ISSUED -> BookCardStyle(
                        badgeText = "ALREADY ISSUED",
                        badgeBackground = R.drawable.bg_badge_issued,
                        badgeTextColor = R.color.search_purple,
                        iconTileBackground = R.drawable.bg_icon_tile_purple,
                        iconTint = R.color.search_purple,
                        buttonText = "Already Issued",
                        buttonBackground = R.drawable.bg_button_issued,
                        buttonTextColor = R.color.white,
                        showArrow = false
                    )
                    BookRequestState.REJECTED -> BookCardStyle(
                        badgeText = "REQUEST AGAIN",
                        badgeBackground = R.drawable.bg_badge_request_again,
                        badgeTextColor = R.color.search_primary,
                        iconTileBackground = R.drawable.bg_icon_tile_red,
                        iconTint = R.color.search_red,
                        buttonText = "Request Again",
                        buttonBackground = R.drawable.bg_button_request,
                        buttonTextColor = R.color.white,
                        showArrow = true
                    )
                    BookRequestState.LOADING -> BookCardStyle(
                        badgeText = "AVAILABLE",
                        badgeBackground = R.drawable.bg_badge_available,
                        badgeTextColor = R.color.search_success,
                        iconTileBackground = R.drawable.bg_icon_tile_blue,
                        iconTint = R.color.search_primary,
                        buttonText = "Requesting...",
                        buttonBackground = R.drawable.bg_button_loading,
                        buttonTextColor = R.color.white,
                        showArrow = false
                    )
                }
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<StudentSearchBookUiModel>() {
        override fun areItemsTheSame(
            oldItem: StudentSearchBookUiModel,
            newItem: StudentSearchBookUiModel
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: StudentSearchBookUiModel,
            newItem: StudentSearchBookUiModel
        ): Boolean = oldItem == newItem
    }
}
