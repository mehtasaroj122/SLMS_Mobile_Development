package com.saroj.lmsmobile.ui.books

import android.content.Intent
import android.os.Bundle
import android.transition.Fade
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.app.ActivityOptionsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.saroj.lmsmobile.MainApplication
import com.saroj.lmsmobile.R
import com.saroj.lmsmobile.api.RetrofitClient
import com.saroj.lmsmobile.data.models.book.Book
import com.saroj.lmsmobile.data.models.common.NetworkResult
import com.saroj.lmsmobile.data.repository.BookRepository
import com.saroj.lmsmobile.ui.books.adapter.BookAdapter
import com.saroj.lmsmobile.ui.books.viewmodel.BookViewModel
import com.saroj.lmsmobile.ui.common.UnauthorizedActivity
import com.saroj.lmsmobile.utils.Constants

abstract class BaseBooksFragment(
    @param:LayoutRes private val layoutResId: Int,
    private val mode: Mode
) : Fragment() {

    enum class Mode {
        ALL,
        SEARCH
    }

    private lateinit var viewModel: BookViewModel
    private lateinit var adapter: BookAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var stateLayout: LinearLayout
    private lateinit var stateImageView: ImageView
    private lateinit var stateTitleTextView: TextView
    private lateinit var stateMessageTextView: TextView
    private lateinit var retryButton: Button
    private var bottomProgressBar: ProgressBar? = null
    private var subtitleTextView: TextView? = null
    private var searchEditText: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = Fade().apply { duration = Constants.FRAGMENT_TRANSITION_DURATION.toLong() }
        exitTransition = Fade().apply { duration = Constants.FRAGMENT_TRANSITION_DURATION.toLong() }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(layoutResId, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViewModel()
        initializeViews(view)
        setupRecyclerView()
        setupListeners()
        observeBooks()

        if (mode == Mode.ALL) {
            viewModel.loadBooks()
        } else {
            showSearchPrompt()
        }
    }

    private fun initializeViewModel() {
        val app = requireActivity().application as MainApplication
        val apiService = RetrofitClient.getApiService(app.tokenManager)
        val repository = BookRepository(apiService, app.tokenManager)
        viewModel = BookViewModel(repository)
    }

    private fun initializeViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerViewBooks)
        progressBar = view.findViewById(R.id.progressBarBooks)
        stateLayout = view.findViewById(R.id.layoutState)
        stateImageView = view.findViewById(R.id.imageViewState)
        stateTitleTextView = view.findViewById(R.id.textViewStateTitle)
        stateMessageTextView = view.findViewById(R.id.textViewStateMessage)
        retryButton = view.findViewById(R.id.buttonRetryBooks)
        bottomProgressBar = view.findViewById(R.id.progressBarBooksBottom)
        subtitleTextView = view.findViewById(R.id.textViewBooksSubtitle)
        searchEditText = view.findViewById(R.id.editTextSearchBooks)
    }

    private fun setupRecyclerView() {
        adapter = BookAdapter { book -> openBookDetail(book) }
        val layoutManager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter
        recyclerView.setHasFixedSize(true)
        recyclerView.itemAnimator = DefaultItemAnimator().apply {
            addDuration = 180
            changeDuration = 120
            moveDuration = 180
            removeDuration = 120
        }
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy <= 0) return

                val visibleItems = layoutManager.childCount
                val totalItems = layoutManager.itemCount
                val firstVisibleItem = layoutManager.findFirstVisibleItemPosition()

                if (totalItems > 0 && visibleItems + firstVisibleItem >= totalItems - 4) {
                    viewModel.loadNextPage()
                }
            }
        })
    }

    private fun setupListeners() {
        retryButton.setOnClickListener {
            if (mode == Mode.ALL) {
                viewModel.loadBooks(refresh = true)
            } else {
                val query = searchEditText?.text?.toString().orEmpty()
                if (query.isBlank()) showSearchPrompt() else viewModel.searchBooks(query, refresh = true)
            }
        }

        searchEditText?.doAfterTextChanged { text ->
            val query = text?.toString().orEmpty()
            if (query.isBlank()) {
                viewModel.searchBooks(query)
                adapter.submitList(emptyList())
                showSearchPrompt()
            } else {
                viewModel.searchBooks(query, refresh = true)
            }
        }
    }

    private fun observeBooks() {
        viewModel.booksState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is NetworkResult.Loading -> showLoading()
                is NetworkResult.Success -> showBooks(result.data)
                is NetworkResult.Error -> showError(result.message)
                is NetworkResult.Unauthorized -> navigateToUnauthorized()
            }
        }
        viewModel.bottomLoading.observe(viewLifecycleOwner) { isLoading ->
            bottomProgressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        bottomProgressBar?.visibility = View.GONE
        stateLayout.visibility = View.GONE
        recyclerView.visibility = View.GONE
    }

    private fun showBooks(books: List<Book>) {
        val shouldAnimate = adapter.itemCount == 0 && books.isNotEmpty()
        progressBar.visibility = View.GONE
        adapter.submitList(books)
        subtitleTextView?.text = "${books.size} books found"

        if (books.isEmpty()) {
            recyclerView.visibility = View.GONE
            retryButton.visibility = View.GONE
            stateImageView.setImageResource(if (mode == Mode.SEARCH) R.drawable.ic_search else R.drawable.ic_books)
            stateTitleTextView.text = if (mode == Mode.SEARCH) "No matching books" else "No books found"
            stateMessageTextView.text = if (mode == Mode.SEARCH) {
                "Try a different title, author, or ISBN."
            } else {
                "Books will appear here when available."
            }
            stateLayout.visibility = View.VISIBLE
        } else {
            stateLayout.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            if (shouldAnimate) animateRecyclerView()
        }
    }

    private fun showSearchPrompt() {
        progressBar.visibility = View.GONE
        bottomProgressBar?.visibility = View.GONE
        recyclerView.visibility = View.GONE
        retryButton.visibility = View.GONE
        stateImageView.setImageResource(R.drawable.ic_search)
        stateTitleTextView.text = "Search the catalog"
        stateMessageTextView.text = "Enter a title, author, or ISBN to find books."
        stateLayout.visibility = View.VISIBLE
    }

    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        bottomProgressBar?.visibility = View.GONE
        recyclerView.visibility = View.GONE
        retryButton.visibility = View.VISIBLE
        stateImageView.setImageResource(R.drawable.ic_error)
        stateTitleTextView.text = "Unable to load books"
        stateMessageTextView.text = message
        stateLayout.visibility = View.VISIBLE
    }

    private fun animateRecyclerView() {
        recyclerView.animate().cancel()
        recyclerView.alpha = 0f
        recyclerView.translationY = 16f
        recyclerView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(220)
            .start()
    }

    private fun openBookDetail(book: Book) {
        val intent = Intent(requireContext(), BookDetailActivity::class.java).apply {
            putExtra(Constants.KEY_BOOK_ID, book.id)
        }
        val options = ActivityOptionsCompat.makeCustomAnimation(
            requireContext(),
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )
        startActivity(intent, options.toBundle())
    }

    private fun navigateToUnauthorized() {
        val intent = Intent(requireContext(), UnauthorizedActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }
}
