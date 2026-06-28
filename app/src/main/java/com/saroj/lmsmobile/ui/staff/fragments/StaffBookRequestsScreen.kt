package com.saroj.lmsmobile.ui.staff.fragments

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
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
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
import com.saroj.lmsmobile.data.repository.StaffBookRequestRepository
import com.saroj.lmsmobile.ui.common.UnauthorizedActivity
import com.saroj.lmsmobile.ui.components.OnlineRefreshable
import com.saroj.lmsmobile.ui.components.runIfOnline
import com.saroj.lmsmobile.ui.staff.adapter.StaffBookRequestsAdapter
import com.saroj.lmsmobile.ui.staff.model.StaffBookRequestStatus
import com.saroj.lmsmobile.ui.staff.model.StaffBookRequestUiModel
import com.saroj.lmsmobile.ui.staff.model.StaffBookRequestsSummaryUiModel
import com.saroj.lmsmobile.ui.staff.model.StaffBookRequestsTab
import com.saroj.lmsmobile.ui.staff.viewmodel.StaffBookRequestsViewModel
import com.saroj.lmsmobile.utils.LmsToast
import com.saroj.lmsmobile.utils.NetworkMessages

class StaffBookRequestsScreen : Fragment(), OnlineRefreshable {

    private lateinit var viewModel: StaffBookRequestsViewModel
    private lateinit var adapter: StaffBookRequestsAdapter
    private lateinit var searchEditText: EditText
    private lateinit var searchClearButton: ImageView
    private lateinit var scrollView: NestedScrollView
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var searchProgress: ProgressBar
    private lateinit var loadMoreProgress: ProgressBar
    private lateinit var emptyState: View
    private lateinit var emptyIcon: ImageView
    private lateinit var emptyTitle: TextView
    private lateinit var emptyMessage: TextView
    private lateinit var retryButton: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private var selectedTab: StaffBookRequestsTab = StaffBookRequestsTab.ALL

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_staff_book_requests, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        setupViewModel()
        setupBackNavigation(view)
        setupRecyclerView()
        setupSearch()
        setupLoadMoreScroll()
        setupPullToRefresh()
        setupTabs(view)
        observeBookRequests()
        viewModel.loadInitial()
    }

    private fun bindViews(view: View) {
        searchEditText = view.findViewById(R.id.etStaffBookRequestsSearch)
        searchClearButton = view.findViewById(R.id.buttonClearStaffBookRequestsSearch)
        scrollView = view.findViewById(R.id.staffBookRequestsScrollView)
        recyclerView = view.findViewById(R.id.recyclerViewStaffBookRequests)
        progressBar = view.findViewById(R.id.progressStaffBookRequests)
        searchProgress = view.findViewById(R.id.progressStaffBookRequestsSearch)
        loadMoreProgress = view.findViewById(R.id.progressStaffBookRequestsLoadMore)
        emptyState = view.findViewById(R.id.layoutStaffBookRequestsEmpty)
        emptyIcon = view.findViewById(R.id.imageStaffBookRequestsEmpty)
        emptyTitle = view.findViewById(R.id.textStaffBookRequestsEmptyTitle)
        emptyMessage = view.findViewById(R.id.textStaffBookRequestsEmptyMessage)
        retryButton = view.findViewById(R.id.buttonStaffBookRequestsRetry)
        swipeRefreshLayout = view.findViewById(R.id.staffBookRequestsSwipeRefresh)
    }

    private fun setupViewModel() {
        val tokenManager = (requireActivity().application as MainApplication).tokenManager
        val apiService = RetrofitClient.getApiService(tokenManager)
        val repository = StaffBookRequestRepository(apiService, tokenManager)

        viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return StaffBookRequestsViewModel(repository) as T
                }
            }
        )[StaffBookRequestsViewModel::class.java]
    }

    private fun setupBackNavigation(view: View) {
        view.findViewById<View>(R.id.buttonStaffBookRequestsBack)?.setOnClickListener {
            if (parentFragmentManager.backStackEntryCount > 0) {
                parentFragmentManager.popBackStack()
            } else {
                activity?.findViewById<BottomNavigationView>(R.id.bottomNavigation)
                    ?.selectedItemId = R.id.nav_more
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = StaffBookRequestsAdapter(
            onAcceptClick = ::showAcceptConfirmation,
            onRejectClick = ::showRejectConfirmation
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
        searchClearButton.setOnClickListener {
            searchEditText.setText("")
        }
        searchEditText.doAfterTextChanged {
            val text = it?.toString().orEmpty()
            searchClearButton.visibility = if (text.isBlank()) View.GONE else View.VISIBLE
            viewModel.searchRequests(text)
        }
    }

    private fun setupLoadMoreScroll() {
        scrollView.setOnScrollChangeListener { nestedScrollView: NestedScrollView, _, scrollY, _, _ ->
            val content = nestedScrollView.getChildAt(0) ?: return@setOnScrollChangeListener
            val distanceFromBottom = content.measuredHeight - nestedScrollView.measuredHeight - scrollY
            if (distanceFromBottom <= 96) {
                runIfOnline(
                    offlineMessage = "You are offline. More data cannot be loaded right now.",
                    onOffline = { loadMoreProgress.visibility = View.GONE }
                ) { viewModel.loadNextPage() }
            }
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

    override fun refreshAfterOnline() {
        viewModel.refresh()
    }

    private fun setupTabs(view: View) {
        view.findViewById<View>(R.id.tabStaffRequestsAll)?.setOnClickListener {
            switchTab(StaffBookRequestsTab.ALL)
        }
        view.findViewById<View>(R.id.tabStaffRequestsPending)?.setOnClickListener {
            switchTab(StaffBookRequestsTab.PENDING)
        }
        view.findViewById<View>(R.id.tabStaffRequestsApproved)?.setOnClickListener {
            switchTab(StaffBookRequestsTab.APPROVED)
        }
        view.findViewById<View>(R.id.tabStaffRequestsRejected)?.setOnClickListener {
            switchTab(StaffBookRequestsTab.REJECTED)
        }
        updateTabStyles()
    }

    private fun observeBookRequests() {
        viewModel.summaryState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is NetworkResult.Success -> updateSummaryCards(result.data)
                is NetworkResult.Error -> LmsToast.show(requireContext(), result.message)
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

        viewModel.actionState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is NetworkResult.Loading -> Unit
                is NetworkResult.Success -> {
                    LmsToast.show(requireContext(), result.data.message)
                    viewModel.clearActionMessage()
                }
                is NetworkResult.Error -> LmsToast.show(requireContext(), result.message)
                is NetworkResult.Unauthorized -> navigateToUnauthorized()
                null -> Unit
            }
        }

        viewModel.isSearching.observe(viewLifecycleOwner) { isSearching ->
            searchProgress.visibility = if (isSearching) View.VISIBLE else View.GONE
        }

        viewModel.isLoadingMore.observe(viewLifecycleOwner) { isLoadingMore ->
            loadMoreProgress.visibility = if (isLoadingMore) View.VISIBLE else View.GONE
        }

        viewModel.actionRequestId.observe(viewLifecycleOwner) { requestId ->
            adapter.setActionRequestId(requestId)
        }
    }

    private fun switchTab(tab: StaffBookRequestsTab) {
        selectedTab = tab
        updateTabStyles()
        viewModel.changeStatusChip(tab)
    }

    private fun showLoading() {
        progressBar.visibility = if (swipeRefreshLayout.isRefreshing) View.GONE else View.VISIBLE
        loadMoreProgress.visibility = View.GONE
        recyclerView.visibility = View.GONE
        emptyState.visibility = View.GONE
    }

    private fun showRequests(requests: List<StaffBookRequestUiModel>) {
        progressBar.visibility = View.GONE
        loadMoreProgress.visibility = View.GONE
        swipeRefreshLayout.isRefreshing = false
        adapter.submitList(requests)
        updateEmptyState(requests.isEmpty(), viewModel.currentQuery())
    }

    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        loadMoreProgress.visibility = View.GONE
        swipeRefreshLayout.isRefreshing = false
        recyclerView.visibility = View.GONE
        emptyIcon.setImageResource(R.drawable.ic_error)
        emptyTitle.text = "Unable to load requests"
        emptyMessage.text = message
        retryButton.visibility = View.VISIBLE
        retryButton.setOnClickListener {
            runIfOnline(NetworkMessages.OFFLINE_RETRY) { viewModel.refresh() }
        }
        emptyState.visibility = View.VISIBLE
    }

    private fun updateSummaryCards(summary: StaffBookRequestsSummaryUiModel) {
        view?.findViewById<TextView>(R.id.textStaffRequestsTotalValue)?.text = summary.totalRequests.toString()
        view?.findViewById<TextView>(R.id.textStaffRequestsPendingValue)?.text = summary.pendingRequests.toString()
        view?.findViewById<TextView>(R.id.textStaffRequestsApprovedValue)?.text = summary.approvedRequests.toString()
        view?.findViewById<TextView>(R.id.textStaffRequestsRejectedValue)?.text = summary.rejectedRequests.toString()
    }

    private fun updateEmptyState(isEmpty: Boolean, query: String) {
        recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        if (!isEmpty) return

        retryButton.visibility = View.GONE
        if (query.isNotBlank()) {
            emptyIcon.setImageResource(R.drawable.ic_search)
            emptyTitle.text = "No matching requests"
            emptyMessage.text = "Try another student, book, date, or status keyword."
            return
        }

        when (selectedTab) {
            StaffBookRequestsTab.ALL -> {
                emptyIcon.setImageResource(R.drawable.ic_clipboard)
                emptyTitle.text = "No book requests found"
                emptyMessage.text = "Book requests will appear here."
            }
            StaffBookRequestsTab.PENDING -> {
                emptyIcon.setImageResource(R.drawable.ic_clock)
                emptyTitle.text = "No pending requests"
                emptyMessage.text = "All requests are handled."
            }
            StaffBookRequestsTab.APPROVED -> {
                emptyIcon.setImageResource(R.drawable.ic_check_circle)
                emptyTitle.text = "No approved requests"
                emptyMessage.text = "Approved requests will appear here."
            }
            StaffBookRequestsTab.REJECTED -> {
                emptyIcon.setImageResource(R.drawable.ic_x_circle)
                emptyTitle.text = "No rejected requests"
                emptyMessage.text = "Rejected requests will appear here."
            }
        }
        emptyIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.my_requests_text_hint))
    }

    private fun showAcceptConfirmation(request: StaffBookRequestUiModel) {
        AlertDialog.Builder(requireContext())
            .setTitle("Accept Request?")
            .setMessage(
                "Approve ${request.studentName}'s request for \"${request.bookTitle}\"?\n" +
                    "The student will be notified once this request is accepted.\n\n" +
                    "Request Date: ${request.requestDate}\n" +
                    "Current Status: ${StaffBookRequestStatus.PENDING.displayLabel()}"
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Accept") { _, _ ->
                runIfOnline(NetworkMessages.OFFLINE_RETRY) { viewModel.approveRequest(request) }
            }
            .show()
    }

    private fun showRejectConfirmation(request: StaffBookRequestUiModel) {
        AlertDialog.Builder(requireContext())
            .setTitle("Reject Request?")
            .setMessage(
                "Reject ${request.studentName}'s request for \"${request.bookTitle}\"?\n" +
                    "The student will be notified once this request is rejected.\n\n" +
                    "Request Date: ${request.requestDate}\n" +
                    "Current Status: ${StaffBookRequestStatus.PENDING.displayLabel()}"
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Reject") { _, _ ->
                runIfOnline(NetworkMessages.OFFLINE_RETRY) { viewModel.rejectRequest(request) }
            }
            .show()
    }

    private fun updateTabStyles() {
        val styles = listOf(
            StaffRequestsTabStyle(
                containerId = R.id.tabStaffRequestsAll,
                iconId = R.id.iconTabStaffRequestsAll,
                textId = R.id.textTabStaffRequestsAll,
                tab = StaffBookRequestsTab.ALL,
                inactiveIconColor = R.color.my_requests_text_muted
            ),
            StaffRequestsTabStyle(
                containerId = R.id.tabStaffRequestsPending,
                iconId = R.id.iconTabStaffRequestsPending,
                textId = R.id.textTabStaffRequestsPending,
                tab = StaffBookRequestsTab.PENDING,
                inactiveIconColor = R.color.my_requests_orange
            ),
            StaffRequestsTabStyle(
                containerId = R.id.tabStaffRequestsApproved,
                iconId = R.id.iconTabStaffRequestsApproved,
                textId = R.id.textTabStaffRequestsApproved,
                tab = StaffBookRequestsTab.APPROVED,
                inactiveIconColor = R.color.my_requests_green
            ),
            StaffRequestsTabStyle(
                containerId = R.id.tabStaffRequestsRejected,
                iconId = R.id.iconTabStaffRequestsRejected,
                textId = R.id.textTabStaffRequestsRejected,
                tab = StaffBookRequestsTab.REJECTED,
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

    private fun navigateToUnauthorized() {
        if (!isAdded) return
        val intent = Intent(requireContext(), UnauthorizedActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }

    private data class StaffRequestsTabStyle(
        val containerId: Int,
        val iconId: Int,
        val textId: Int,
        val tab: StaffBookRequestsTab,
        val inactiveIconColor: Int
    )
}
