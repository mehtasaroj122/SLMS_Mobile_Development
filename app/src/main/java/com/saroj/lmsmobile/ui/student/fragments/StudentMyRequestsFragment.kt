package com.saroj.lmsmobile.ui.student.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.NestedScrollView
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.saroj.lmsmobile.MainApplication
import com.saroj.lmsmobile.R
import com.saroj.lmsmobile.api.RetrofitClient
import com.saroj.lmsmobile.data.models.common.NetworkResult
import com.saroj.lmsmobile.data.repository.StudentMyRequestsRepository
import com.saroj.lmsmobile.ui.common.UnauthorizedActivity
import com.saroj.lmsmobile.ui.components.OnlineRefreshable
import com.saroj.lmsmobile.ui.components.runIfOnline
import com.saroj.lmsmobile.ui.student.adapter.MyRequestsAdapter
import com.saroj.lmsmobile.ui.student.model.MyRequestStatus
import com.saroj.lmsmobile.ui.student.model.MyRequestUiModel
import com.saroj.lmsmobile.ui.student.model.MyRequestsSummaryUiModel
import com.saroj.lmsmobile.ui.student.model.MyRequestsTab
import com.saroj.lmsmobile.ui.student.viewmodel.StudentMyRequestsViewModel
import com.saroj.lmsmobile.utils.LmsToast
import com.saroj.lmsmobile.utils.NetworkMessages

class StudentMyRequestsFragment : Fragment(), OnlineRefreshable {

    private lateinit var viewModel: StudentMyRequestsViewModel
    private lateinit var adapter: MyRequestsAdapter
    private lateinit var searchEditText: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: View
    private lateinit var emptyIcon: ImageView
    private lateinit var emptyTitle: TextView
    private lateinit var emptyMessage: TextView
    private lateinit var emptyActionButton: TextView
    private lateinit var footerText: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var scrollView: NestedScrollView
    private lateinit var loadMoreProgressBar: ProgressBar

    private var selectedTab: MyRequestsTab = MyRequestsTab.ALL

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_student_my_requests, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        searchEditText = view.findViewById(R.id.etMyRequestsSearch)
        recyclerView = view.findViewById(R.id.recyclerViewMyRequests)
        progressBar = view.findViewById(R.id.progressMyRequests)
        emptyState = view.findViewById(R.id.layoutMyRequestsEmpty)
        emptyIcon = view.findViewById(R.id.imageMyRequestsEmpty)
        emptyTitle = view.findViewById(R.id.textMyRequestsEmptyTitle)
        emptyMessage = view.findViewById(R.id.textMyRequestsEmptyMessage)
        emptyActionButton = view.findViewById(R.id.buttonMyRequestsEmptyAction)
        footerText = view.findViewById(R.id.textMyRequestsFooter)
        swipeRefreshLayout = view.findViewById(R.id.myRequestsSwipeRefresh)
        scrollView = view.findViewById(R.id.myRequestsScrollView)
        loadMoreProgressBar = view.findViewById(R.id.progressMyRequestsLoadMore)

        setupViewModel()
        setupBackNavigation(view)
        setupRecyclerView()
        setupSearch()
        setupPullToRefresh()
        setupLoadMore()
        setupEmptyAction()
        setupTabs(view)
        observeMyRequests()
        viewModel.loadInitial()
    }

    private fun setupViewModel() {
        val tokenManager = (requireActivity().application as MainApplication).tokenManager
        val apiService = RetrofitClient.getApiService(tokenManager)
        val repository = StudentMyRequestsRepository(apiService, tokenManager)

        viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return StudentMyRequestsViewModel(repository) as T
                }
            }
        )[StudentMyRequestsViewModel::class.java]
    }

    private fun setupBackNavigation(view: View) {
        view.findViewById<View>(R.id.buttonMyRequestsBack)?.setOnClickListener {
            if (parentFragmentManager.backStackEntryCount > 0) {
                parentFragmentManager.popBackStack()
            } else {
                activity?.findViewById<BottomNavigationView>(R.id.bottomNavigation)
                    ?.selectedItemId = R.id.nav_dashboard
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = MyRequestsAdapter(
            onCancelClick = { request -> showCancelConfirmation(request) },
            onDetailsClick = { request -> viewModel.loadRequestDetail(request) }
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        recyclerView.isNestedScrollingEnabled = false
        recyclerView.itemAnimator = DefaultItemAnimator().apply {
            addDuration = 180
            changeDuration = 160
            moveDuration = 180
            removeDuration = 160
        }
    }

    private fun setupSearch() {
        searchEditText.doAfterTextChanged {
            viewModel.setSearchQuery(it?.toString().orEmpty())
        }
    }

    private fun setupPullToRefresh() {
        swipeRefreshLayout.setColorSchemeResources(
            R.color.my_requests_primary,
            R.color.my_requests_orange,
            R.color.my_requests_green
        )
        swipeRefreshLayout.setOnRefreshListener {
            runIfOnline(onOffline = { swipeRefreshLayout.isRefreshing = false }) {
                viewModel.refresh()
            }
        }
    }

    private fun setupLoadMore() {
        scrollView.setOnScrollChangeListener { nestedScrollView: NestedScrollView, _, scrollY, _, _ ->
            val content = nestedScrollView.getChildAt(0) ?: return@setOnScrollChangeListener
            val distanceToBottom = content.measuredHeight - nestedScrollView.measuredHeight - scrollY
            if (distanceToBottom <= LOAD_MORE_THRESHOLD_PX) {
                runIfOnline("You are offline. More data cannot be loaded right now.") {
                    viewModel.loadNextPage()
                }
            }
        }
    }

    private fun setupEmptyAction() {
        emptyActionButton.setOnClickListener {
            resetFilters(showToast = true)
        }
    }

    private fun setupTabs(view: View) {
        view.findViewById<View>(R.id.tabMyRequestsAll)?.setOnClickListener {
            switchTab(MyRequestsTab.ALL)
        }
        view.findViewById<View>(R.id.tabMyRequestsPending)?.setOnClickListener {
            switchTab(MyRequestsTab.PENDING)
        }
        view.findViewById<View>(R.id.tabMyRequestsApproved)?.setOnClickListener {
            switchTab(MyRequestsTab.APPROVED)
        }
        view.findViewById<View>(R.id.tabMyRequestsRejected)?.setOnClickListener {
            switchTab(MyRequestsTab.REJECTED)
        }
        updateTabStyles()
    }

    private fun observeMyRequests() {
        viewModel.summaryState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is NetworkResult.Success -> updateSummaryCards(result.data)
                is NetworkResult.Error -> {
                    if (!swipeRefreshLayout.isRefreshing) {
                        LmsToast.show(requireContext(), result.message)
                    }
                }
                is NetworkResult.Unauthorized -> navigateToUnauthorized()
                is NetworkResult.Loading -> Unit
            }
        }

        viewModel.requestsState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is NetworkResult.Loading -> showLoading()
                is NetworkResult.Success -> showRequests(result.data)
                is NetworkResult.Error -> showError(result.message)
                is NetworkResult.Unauthorized -> navigateToUnauthorized()
            }
        }

        viewModel.requestDetailState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is NetworkResult.Loading -> Unit
                is NetworkResult.Success -> showRequestDetails(result.data)
                is NetworkResult.Error -> LmsToast.show(
                    requireContext(),
                    "Unable to load request details: ${result.message}"
                )
                is NetworkResult.Unauthorized -> navigateToUnauthorized()
            }
        }

        viewModel.cancelState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is NetworkResult.Loading -> Unit
                is NetworkResult.Success -> LmsToast.show(requireContext(), result.data.message)
                is NetworkResult.Error -> LmsToast.show(
                    requireContext(),
                    "Unable to cancel request: ${result.message}"
                )
                is NetworkResult.Unauthorized -> navigateToUnauthorized()
            }
        }

        viewModel.isLoadingMore.observe(viewLifecycleOwner) { isLoadingMore ->
            loadMoreProgressBar.visibility = if (isLoadingMore) View.VISIBLE else View.GONE
        }
    }

    private fun switchTab(tab: MyRequestsTab) {
        selectedTab = tab
        updateTabStyles()
        viewModel.switchTab(tab)
    }

    private fun showLoading() {
        progressBar.visibility = if (swipeRefreshLayout.isRefreshing) View.GONE else View.VISIBLE
        recyclerView.visibility = View.GONE
        emptyState.visibility = View.GONE
        footerText.visibility = View.GONE
    }

    private fun showRequests(requests: List<MyRequestUiModel>) {
        progressBar.visibility = View.GONE
        swipeRefreshLayout.isRefreshing = false
        adapter.submitList(requests)
        updateEmptyState(requests.isEmpty(), searchEditText.text?.toString().orEmpty().trim())
        updateFooterCount(visibleCount = requests.size, totalCount = viewModel.filteredTotalCount())
    }

    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        swipeRefreshLayout.isRefreshing = false
        recyclerView.visibility = View.GONE
        footerText.visibility = View.GONE
        emptyIcon.setImageResource(R.drawable.ic_error)
        emptyTitle.text = "Unable to load requests"
        emptyMessage.text = message
        emptyActionButton.text = "Retry"
        emptyActionButton.setOnClickListener {
            runIfOnline(NetworkMessages.OFFLINE_RETRY) { viewModel.refresh() }
        }
        emptyState.visibility = View.VISIBLE
    }

    override fun refreshAfterOnline() {
        viewModel.refresh()
    }

    private fun updateSummaryCards(summary: MyRequestsSummaryUiModel) {
        view?.findViewById<TextView>(R.id.textRequestsTotalValue)?.text = summary.totalRequests.toString()
        view?.findViewById<TextView>(R.id.textRequestsPendingValue)?.text = summary.pendingRequests.toString()
        view?.findViewById<TextView>(R.id.textRequestsApprovedValue)?.text = summary.approvedRequests.toString()
        view?.findViewById<TextView>(R.id.textRequestsRejectedValue)?.text = summary.rejectedRequests.toString()
    }

    private fun showRequestDetails(request: MyRequestUiModel) {
        AlertDialog.Builder(requireContext())
            .setTitle(request.bookTitle)
            .setMessage(
                "Author: ${request.author}\n" +
                    "Status: ${request.status.displayLabel()}\n" +
                    "Requested: ${request.requestDate}\n" +
                    "Processed by: ${request.processedBy}\n" +
                    "Message: ${request.message}"
            )
            .setPositiveButton("Close", null)
            .show()
    }

    private fun showCancelConfirmation(request: MyRequestUiModel) {
        AlertDialog.Builder(requireContext())
            .setTitle("Cancel Request?")
            .setMessage("Do you want to cancel your request for '${request.bookTitle}'?")
            .setNegativeButton("No", null)
            .setPositiveButton("Cancel Request") { _, _ ->
                runIfOnline(NetworkMessages.OFFLINE_RETRY) { viewModel.cancelRequest(request) }
            }
            .show()
    }

    private fun updateTabStyles() {
        val styles = listOf(
            MyRequestsTabStyle(
                containerId = R.id.tabMyRequestsAll,
                iconId = R.id.iconTabRequestsAll,
                textId = R.id.textTabRequestsAll,
                tab = MyRequestsTab.ALL,
                inactiveIconColor = R.color.my_requests_text_muted
            ),
            MyRequestsTabStyle(
                containerId = R.id.tabMyRequestsPending,
                iconId = R.id.iconTabRequestsPending,
                textId = R.id.textTabRequestsPending,
                tab = MyRequestsTab.PENDING,
                inactiveIconColor = R.color.my_requests_orange
            ),
            MyRequestsTabStyle(
                containerId = R.id.tabMyRequestsApproved,
                iconId = R.id.iconTabRequestsApproved,
                textId = R.id.textTabRequestsApproved,
                tab = MyRequestsTab.APPROVED,
                inactiveIconColor = R.color.my_requests_green
            ),
            MyRequestsTabStyle(
                containerId = R.id.tabMyRequestsRejected,
                iconId = R.id.iconTabRequestsRejected,
                textId = R.id.textTabRequestsRejected,
                tab = MyRequestsTab.REJECTED,
                inactiveIconColor = R.color.my_requests_red
            )
        )

        styles.forEach { style ->
            val selected = style.tab == selectedTab
            view?.findViewById<View>(style.containerId)?.setBackgroundResource(
                if (selected) R.drawable.bg_requests_segment_active else 0
            )
            view?.findViewById<ImageView>(style.iconId)?.setColorFilter(
                ContextCompat.getColor(
                    requireContext(),
                    if (selected) R.color.white else style.inactiveIconColor
                )
            )
            view?.findViewById<TextView>(style.textId)?.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (selected) R.color.white else R.color.my_requests_text_primary
                )
            )
        }
    }

    private fun updateFooterCount(visibleCount: Int, totalCount: Int) {
        footerText.text = if (totalCount == 0) {
            "Showing 0 of 0 requests"
        } else {
            "Showing 1 to $visibleCount of $totalCount requests"
        }
        footerText.visibility = View.VISIBLE
    }

    private fun updateEmptyState(isEmpty: Boolean, query: String) {
        recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        if (!isEmpty) return

        emptyIcon.setImageResource(if (query.isNotBlank()) R.drawable.ic_search else R.drawable.ic_clipboard)
        emptyActionButton.text = "Reset Filters"
        emptyActionButton.setOnClickListener { resetFilters(showToast = true) }

        if (query.isNotBlank()) {
            emptyTitle.text = "No matching requests"
            emptyMessage.text = "Try another keyword or reset filters."
            return
        }

        when (selectedTab) {
            MyRequestsTab.ALL -> {
                emptyTitle.text = "No book requests yet"
                emptyMessage.text = "Search books and request one from the library catalog."
                emptyActionButton.text = "Search Books"
                emptyActionButton.setOnClickListener {
                    activity?.findViewById<BottomNavigationView>(R.id.bottomNavigation)
                        ?.selectedItemId = R.id.nav_search_books
                }
            }
            MyRequestsTab.PENDING -> {
                emptyTitle.text = "No pending requests"
                emptyMessage.text = "You have no requests waiting for approval."
            }
            MyRequestsTab.APPROVED -> {
                emptyTitle.text = "No approved requests yet"
                emptyMessage.text = "Approved requests will appear here after library review."
            }
            MyRequestsTab.REJECTED -> {
                emptyTitle.text = "No rejected requests"
                emptyMessage.text = "Rejected requests will appear here if a book is unavailable."
            }
        }
    }

    private fun resetFilters(showToast: Boolean) {
        if (searchEditText.text?.isNotEmpty() == true) {
            searchEditText.setText("")
        }
        selectedTab = MyRequestsTab.ALL
        updateTabStyles()
        viewModel.resetFilters()
        if (showToast) {
            LmsToast.show(requireContext(), "Filters reset.")
        }
    }

    private fun navigateToUnauthorized() {
        if (!isAdded) return
        val intent = Intent(requireContext(), UnauthorizedActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }

    private fun MyRequestStatus.displayLabel(): String {
        return when (this) {
            MyRequestStatus.PENDING -> "Pending"
            MyRequestStatus.APPROVED -> "Approved"
            MyRequestStatus.REJECTED -> "Rejected"
            MyRequestStatus.CANCELLED -> "Cancelled"
        }
    }

    private data class MyRequestsTabStyle(
        val containerId: Int,
        val iconId: Int,
        val textId: Int,
        val tab: MyRequestsTab,
        val inactiveIconColor: Int
    )

    private companion object {
        const val LOAD_MORE_THRESHOLD_PX = 96
    }
}
