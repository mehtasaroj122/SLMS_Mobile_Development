package com.saroj.lmsmobile.ui.books

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.saroj.lmsmobile.MainApplication
import com.saroj.lmsmobile.R
import com.saroj.lmsmobile.api.RetrofitClient
import com.saroj.lmsmobile.data.models.book.Book
import com.saroj.lmsmobile.data.models.common.NetworkResult
import com.saroj.lmsmobile.data.repository.BookRepository
import com.saroj.lmsmobile.ui.books.viewmodel.BookViewModel
import com.saroj.lmsmobile.ui.common.UnauthorizedActivity
import com.saroj.lmsmobile.ui.components.BaseActivity
import com.saroj.lmsmobile.ui.components.OnlineRefreshable
import com.saroj.lmsmobile.network.NetworkMonitor
import com.saroj.lmsmobile.utils.Constants
import com.saroj.lmsmobile.utils.NetworkMessages

class BookDetailActivity : BaseActivity(), OnlineRefreshable {

    private lateinit var viewModel: BookViewModel
    private lateinit var detailScrollView: ScrollView
    private lateinit var progressBar: ProgressBar
    private lateinit var stateLayout: View
    private lateinit var retryButton: Button
    private lateinit var stateTitleTextView: TextView
    private lateinit var stateMessageTextView: TextView
    private lateinit var initialTextView: TextView
    private lateinit var titleTextView: TextView
    private lateinit var authorTextView: TextView
    private lateinit var statusTextView: TextView
    private lateinit var isbnTextView: TextView
    private lateinit var categoryTextView: TextView
    private lateinit var publisherTextView: TextView
    private lateinit var editionTextView: TextView
    private lateinit var publicationYearTextView: TextView
    private lateinit var copiesTextView: TextView
    private lateinit var descriptionTextView: TextView

    private var bookId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_detail)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "Book Detail"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        bookId = intent.getIntExtra(Constants.KEY_BOOK_ID, -1)
        initializeViews()
        initializeViewModel()
        setupListeners()
        observeBookDetail()

        if (bookId == -1) {
            showError("Book ID was not provided.")
        } else {
            viewModel.loadBookDetail(bookId)
        }
    }

    private fun initializeViews() {
        detailScrollView = findViewById(R.id.scrollViewBookDetail)
        progressBar = findViewById(R.id.progressBarBookDetail)
        stateLayout = findViewById(R.id.layoutDetailState)
        retryButton = findViewById(R.id.buttonRetryBookDetail)
        stateTitleTextView = findViewById(R.id.textViewDetailStateTitle)
        stateMessageTextView = findViewById(R.id.textViewDetailStateMessage)
        initialTextView = findViewById(R.id.textViewDetailInitial)
        titleTextView = findViewById(R.id.textViewDetailTitle)
        authorTextView = findViewById(R.id.textViewDetailAuthor)
        statusTextView = findViewById(R.id.textViewDetailStatus)
        isbnTextView = findViewById(R.id.textViewDetailIsbn)
        categoryTextView = findViewById(R.id.textViewDetailCategory)
        publisherTextView = findViewById(R.id.textViewDetailPublisher)
        editionTextView = findViewById(R.id.textViewDetailEdition)
        publicationYearTextView = findViewById(R.id.textViewDetailPublicationYear)
        copiesTextView = findViewById(R.id.textViewDetailCopies)
        descriptionTextView = findViewById(R.id.textViewDetailDescription)
    }

    private fun initializeViewModel() {
        val app = application as MainApplication
        val apiService = RetrofitClient.getApiService(app.tokenManager)
        val repository = BookRepository(apiService, app.tokenManager)
        viewModel = BookViewModel(repository)
    }

    private fun setupListeners() {
        retryButton.setOnClickListener {
            if (!NetworkMonitor.isCurrentlyOnline()) {
                showNetworkMessage(NetworkMessages.OFFLINE_RETRY)
                return@setOnClickListener
            }
            if (bookId != -1) viewModel.loadBookDetail(bookId)
        }
    }

    override fun refreshAfterOnline() {
        if (bookId != -1) viewModel.loadBookDetail(bookId)
    }

    private fun observeBookDetail() {
        viewModel.bookDetailState.observe(this) { result ->
            when (result) {
                is NetworkResult.Loading -> showLoading()
                is NetworkResult.Success -> showBook(result.data)
                is NetworkResult.Error -> showError(result.message)
                is NetworkResult.Unauthorized -> navigateToUnauthorized()
            }
        }
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        detailScrollView.visibility = View.GONE
        stateLayout.visibility = View.GONE
    }

    private fun showBook(book: Book) {
        progressBar.visibility = View.GONE
        stateLayout.visibility = View.GONE
        detailScrollView.visibility = View.VISIBLE

        supportActionBar?.title = book.title.ifBlank { "Book Detail" }
        initialTextView.text = book.title.firstOrNull()?.uppercaseChar()?.toString() ?: "B"
        titleTextView.text = book.title.ifBlank { "Untitled book" }
        authorTextView.text = book.author?.takeIf { it.isNotBlank() } ?: "Unknown author"

        val availableCopies = book.availableCopies ?: 0
        val totalCopies = book.totalCopies ?: 0
        val isAvailable = availableCopies > 0
        statusTextView.text = if (isAvailable) "Available" else "Unavailable"
        statusTextView.setTextColor(
            ContextCompat.getColor(
                this,
                if (isAvailable) R.color.success_color else R.color.error_color
            )
        )

        isbnTextView.text = "ISBN: ${book.isbn.orDash()}"
        categoryTextView.text = "Category: ${book.category.orDash()}"
        publisherTextView.text = "Publisher: ${book.publisher.orDash()}"
        editionTextView.text = "Edition: ${book.edition.orDash()}"
        publicationYearTextView.text = "Publication Year: ${book.publicationYear?.toString() ?: "-"}"
        copiesTextView.text = "Copies: $availableCopies of $totalCopies available"
        descriptionTextView.text = book.description?.takeIf { it.isNotBlank() } ?: "No description available."

        detailScrollView.alpha = 0f
        detailScrollView.translationY = 18f
        detailScrollView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(220)
            .start()
    }

    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        detailScrollView.visibility = View.GONE
        stateTitleTextView.text = "Unable to load book"
        stateMessageTextView.text = message
        stateLayout.visibility = View.VISIBLE
    }

    private fun navigateToUnauthorized() {
        val intent = Intent(this, UnauthorizedActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun String?.orDash(): String = this?.takeIf { it.isNotBlank() } ?: "-"
}
