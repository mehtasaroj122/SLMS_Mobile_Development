package com.saroj.lmsmobile.ui.books.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.saroj.lmsmobile.R
import com.saroj.lmsmobile.data.models.book.Book

class BookAdapter(
    private val onBookClick: (Book) -> Unit
) : ListAdapter<Book, BookAdapter.BookViewHolder>(BookDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_book, parent, false)
        return BookViewHolder(view, onBookClick)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class BookViewHolder(
        itemView: View,
        private val onBookClick: (Book) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val titleTextView: TextView = itemView.findViewById(R.id.textViewBookTitle)
        private val authorTextView: TextView = itemView.findViewById(R.id.textViewBookAuthor)
        private val isbnTextView: TextView = itemView.findViewById(R.id.textViewBookIsbn)
        private val categoryTextView: TextView = itemView.findViewById(R.id.textViewBookCategory)
        private val copiesTextView: TextView = itemView.findViewById(R.id.textViewBookCopies)
        private val statusTextView: TextView = itemView.findViewById(R.id.textViewBookStatus)
        private val initialTextView: TextView = itemView.findViewById(R.id.textViewBookInitial)

        fun bind(book: Book) {
            titleTextView.text = book.title.ifBlank { "Untitled book" }
            authorTextView.text = book.author?.takeIf { it.isNotBlank() } ?: "Unknown author"
            isbnTextView.text = book.isbn?.takeIf { it.isNotBlank() }?.let { "ISBN $it" } ?: "ISBN not available"
            categoryTextView.text = book.category?.takeIf { it.isNotBlank() } ?: "General"

            val availableQty = book.available_quantity ?: 0
            val totalQty = book.quantity ?: 0
            copiesTextView.text = "Total Copies: $totalQty\nAvailable: $availableQty"

            val isAvailable = availableQty > 0
            statusTextView.text = if (isAvailable) "Available" else "Unavailable"
            statusTextView.setTextColor(
                ContextCompat.getColor(
                    itemView.context,
                    if (isAvailable) R.color.success_color else R.color.error_color
                )
            )

            initialTextView.text = book.title.firstOrNull()?.uppercaseChar()?.toString() ?: "B"
            itemView.setOnClickListener { onBookClick(book) }
        }
    }

    private class BookDiffCallback : DiffUtil.ItemCallback<Book>() {
        override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean = oldItem == newItem
    }
}
