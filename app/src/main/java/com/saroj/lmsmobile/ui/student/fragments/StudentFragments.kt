package com.saroj.lmsmobile.ui.student.fragments

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.saroj.lmsmobile.MainApplication
import com.saroj.lmsmobile.R
import com.saroj.lmsmobile.api.RetrofitClient
import com.saroj.lmsmobile.data.models.studentdashboard.DashboardIssuedBook
import com.saroj.lmsmobile.data.models.studentdashboard.DashboardNotification
import com.saroj.lmsmobile.data.models.studentdashboard.StudentDashboardData
import com.saroj.lmsmobile.data.models.studentdashboard.StudentDashboardResponse
import com.saroj.lmsmobile.data.models.common.NetworkResult
import com.saroj.lmsmobile.data.repository.BookRepository
import com.saroj.lmsmobile.data.repository.StudentDashboardRepository
import com.saroj.lmsmobile.data.repository.StudentMyBooksRepository
import com.saroj.lmsmobile.ui.common.UnauthorizedActivity
import com.saroj.lmsmobile.ui.student.adapter.MyBooksAdapter
import com.saroj.lmsmobile.ui.student.adapter.StudentSearchBookAdapter
import com.saroj.lmsmobile.ui.student.model.BookRequestState
import com.saroj.lmsmobile.ui.student.model.MyBookStatus
import com.saroj.lmsmobile.ui.student.model.MyBookUiModel
import com.saroj.lmsmobile.ui.student.model.MyBooksFineFilter
import com.saroj.lmsmobile.ui.student.model.MyBooksSortOption
import com.saroj.lmsmobile.ui.student.model.MyBooksStatusFilter
import com.saroj.lmsmobile.ui.student.model.MyBooksSummaryUiModel
import com.saroj.lmsmobile.ui.student.model.MyBooksTab
import com.saroj.lmsmobile.ui.student.model.StudentSearchBookUiModel
import com.saroj.lmsmobile.ui.student.viewmodel.StudentDashboardViewModel
import com.saroj.lmsmobile.ui.student.viewmodel.StudentMyBooksViewModel
import com.saroj.lmsmobile.ui.student.viewmodel.StudentSearchBooksViewModel
import com.saroj.lmsmobile.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class StudentDashboardFragment : Fragment() {
    private lateinit var viewModel: StudentDashboardViewModel
    private var refreshToastPending = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_student_dashboard, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewModel()
        observeDashboard(view)
        setupSwipeRefresh(view)
        setupQuickActions(view)
        setupAnimations(view)
        viewModel.loadDashboard()
    }

    private fun setupViewModel() {
        val tokenManager = (requireActivity().application as MainApplication).tokenManager
        val apiService = RetrofitClient.getApiService(tokenManager)
        val repository = StudentDashboardRepository(apiService, tokenManager)

        viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return StudentDashboardViewModel(repository) as T
                }
            }
        )[StudentDashboardViewModel::class.java]
    }

    private fun observeDashboard(view: View) {
        viewModel.dashboardData.observe(viewLifecycleOwner) { dashboard ->
            bindDashboard(view, dashboard)
            if (refreshToastPending) {
                refreshToastPending = false
                Toast.makeText(requireContext(), "Dashboard refreshed", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            view.findViewById<ProgressBar>(R.id.dashboardLoading)?.visibility =
                if (isLoading) View.VISIBLE else View.GONE
            view.findViewById<SwipeRefreshLayout>(R.id.dashboardSwipeRefresh)?.isRefreshing =
                isLoading && refreshToastPending
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrBlank()) {
                refreshToastPending = false
                view.findViewById<SwipeRefreshLayout>(R.id.dashboardSwipeRefresh)?.isRefreshing = false
                val shortMessage = message.take(120)
                Toast.makeText(
                    requireContext(),
                    "Unable to load dashboard: $shortMessage",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        viewModel.unauthorized.observe(viewLifecycleOwner) { unauthorized ->
            if (unauthorized) {
                refreshToastPending = false
                view.findViewById<SwipeRefreshLayout>(R.id.dashboardSwipeRefresh)?.isRefreshing = false
                navigateToUnauthorized()
            }
        }
    }

    private fun bindDashboard(view: View, dashboard: StudentDashboardResponse) {
        val data = dashboard.content()
        bindStudent(view, data)
        bindStats(view, data)
        bindCurrentlyIssued(view, data.currently_issued.orEmpty(), data.stats?.issued_books)
        bindDueSoon(view, data.due_soon.orEmpty())
        bindNotifications(view, data.latest_notifications.orEmpty())
        bindPrivileges(view, data)
    }

    private fun setupSwipeRefresh(view: View) {
        view.findViewById<SwipeRefreshLayout>(R.id.dashboardSwipeRefresh)?.apply {
            setColorSchemeResources(
                R.color.student_primary,
                R.color.student_purple,
                R.color.student_green,
                R.color.student_yellow
            )
            setOnRefreshListener {
                refreshToastPending = true
                viewModel.refreshDashboard()
            }
        }
    }

    private fun bindStudent(view: View, dashboard: StudentDashboardData) {
        val student = dashboard.student
        val name = student?.name.orFallback("Student")
        view.setText(R.id.textStudentName, name)
        view.setText(R.id.textStudentInitials, getInitials(name))
        view.setText(R.id.textStudentRoll, student?.roll_no.orFallback("STU-001-001"))
        view.setText(R.id.textStudentFaculty, student?.faculty.orFallback("English Literature"))
        view.setText(R.id.textStudentSemester, formatSemester(student?.semester))
        view.setText(R.id.textStudentEmail, student?.email.orFallback("student@example.com"))
        loadProfilePhoto(view, student?.profile_photo_url ?: student?.profile_photo)
    }

    private fun bindStats(view: View, dashboard: StudentDashboardData) {
        val stats = dashboard.stats
        view.setText(R.id.textIssuedBooksValue, (stats?.issued_books ?: 0).toString())
        view.setText(R.id.textReturnedBooksValue, (stats?.returned_books ?: 0).toString())
        view.setText(R.id.textPendingFinesValue, formatCurrency(stats?.pending_fines ?: 0.0))
        view.setText(R.id.textActiveRequestsValue, (stats?.active_requests ?: 0).toString())
    }

    private fun bindCurrentlyIssued(view: View, issuedBooks: List<DashboardIssuedBook>, totalIssuedBooks: Int?) {
        view.setText(R.id.textCurrentlyIssuedCount, (totalIssuedBooks ?: issuedBooks.size).toString())
        view.setVisibility(R.id.textCurrentlyIssuedEmpty, issuedBooks.isEmpty())

        val itemIds = listOf(R.id.currentlyIssuedItem1, R.id.currentlyIssuedItem2, R.id.currentlyIssuedItem3)
        val titleIds = listOf(R.id.textIssuedBookTitle1, R.id.textIssuedBookTitle2, R.id.textIssuedBookTitle3)
        val statusIds = listOf(R.id.textIssuedBookStatus1, R.id.textIssuedBookStatus2, R.id.textIssuedBookStatus3)
        val issueDateIds = listOf(R.id.textIssuedBookIssueDate1, R.id.textIssuedBookIssueDate2, R.id.textIssuedBookIssueDate3)
        val dueDateIds = listOf(R.id.textIssuedBookDueDate1, R.id.textIssuedBookDueDate2, R.id.textIssuedBookDueDate3)

        itemIds.forEachIndexed { index, itemId ->
            val issued = issuedBooks.getOrNull(index)
            view.setVisibility(itemId, issued != null)
            if (issued != null) {
                view.setText(titleIds[index], issued.book?.title.orFallback("Untitled Book"))
                view.setText(issueDateIds[index], "Issued: ${formatDate(issued.issue_date)}")
                view.setText(dueDateIds[index], "Due: ${formatDate(issued.due_date)}")
                bindIssueStatus(view.findViewById(statusIds[index]), issued)
            }
        }
    }

    private fun bindDueSoon(view: View, dueSoonBooks: List<DashboardIssuedBook>) {
        view.setText(R.id.textDueSoonCount, dueSoonBooks.size.toString())
        view.setVisibility(R.id.textDueSoonEmpty, dueSoonBooks.isEmpty())

        val itemIds = listOf(R.id.dueSoonItem1, R.id.dueSoonItem2, R.id.dueSoonItem3)
        val titleIds = listOf(R.id.textDueSoonTitle1, R.id.textDueSoonTitle2, R.id.textDueSoonTitle3)
        val dateIds = listOf(R.id.textDueSoonDate1, R.id.textDueSoonDate2, R.id.textDueSoonDate3)

        itemIds.forEachIndexed { index, itemId ->
            val dueSoon = dueSoonBooks.getOrNull(index)
            view.setVisibility(itemId, dueSoon != null)
            if (dueSoon != null) {
                view.setText(titleIds[index], dueSoon.book?.title.orFallback("Untitled Book"))
                view.setText(dateIds[index], "Due: ${formatDate(dueSoon.due_date)}")
            }
        }
    }

    private fun bindNotifications(view: View, notifications: List<DashboardNotification>) {
        view.setText(R.id.textNotificationsCount, notifications.size.toString())
        view.setVisibility(R.id.textNotificationsEmpty, notifications.isEmpty())

        val itemIds = listOf(R.id.notificationItem1, R.id.notificationItem2, R.id.notificationItem3)
        val titleIds = listOf(R.id.textNotificationTitle1, R.id.textNotificationTitle2, R.id.textNotificationTitle3)
        val messageIds = listOf(R.id.textNotificationMessage1, R.id.textNotificationMessage2, R.id.textNotificationMessage3)
        val timeIds = listOf(R.id.textNotificationTime1, R.id.textNotificationTime2, R.id.textNotificationTime3)
        val unreadIds = listOf(R.id.viewNotificationUnread1, R.id.viewNotificationUnread2, R.id.viewNotificationUnread3)

        itemIds.forEachIndexed { index, itemId ->
            val notification = notifications.getOrNull(index)
            view.setVisibility(itemId, notification != null)
            if (notification != null) {
                view.setText(titleIds[index], notification.title.orFallback("Notification"))
                view.setText(messageIds[index], notification.message.orFallback("-"))
                view.setText(timeIds[index], formatRelativeDate(notification.created_at))
                view.setVisibility(unreadIds[index], notification.read_at.isNullOrBlank())
            }
        }
    }

    private fun bindPrivileges(view: View, dashboard: StudentDashboardData) {
        val privileges = dashboard.privileges
        val maxBooks = privileges?.max_books ?: 0
        val currentUsage = privileges?.current_usage ?: 0
        val remainingBooks = privileges?.remaining_books ?: (maxBooks - currentUsage).coerceAtLeast(0)
        val progress = if (maxBooks > 0) ((currentUsage.toFloat() / maxBooks) * 100).toInt() else 0

        view.setText(R.id.textMaxBooksValue, maxBooks.toString())
        view.setText(R.id.textBorrowDaysValue, (privileges?.borrow_days ?: 0).toString())
        view.setText(R.id.textFinePerDayValue, formatCurrency(privileges?.fine_per_day ?: 0.0))
        view.setText(R.id.textBorrowingStatusValue, privileges?.borrowing_status.orFallback("active").replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
        })
        view.setText(R.id.textCurrentUsage, "$currentUsage / $maxBooks Books")
        view.findViewById<ProgressBar>(R.id.progressCurrentUsage)?.progress = progress.coerceIn(0, 100)
        view.setText(R.id.textRemainingBooks, "You can borrow $remainingBooks more books.")
        bindPrivilegeType(view, privileges)
    }

    private fun setupQuickActions(view: View) {
        view.findViewById<View>(R.id.actionSearchBooks)?.setOnClickListener {
            selectBottomNavItem(R.id.nav_search_books)
        }
        view.findViewById<View>(R.id.actionMyBooks)?.setOnClickListener {
            selectBottomNavItem(R.id.nav_my_books)
        }
        view.findViewById<View>(R.id.actionMyRequests)?.setOnClickListener {
            Toast.makeText(requireContext(), "My Requests tab will be added soon.", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<View>(R.id.actionMyFines)?.setOnClickListener {
            selectBottomNavItem(R.id.nav_my_fines)
        }
        view.findViewById<View>(R.id.actionMyProfile)?.setOnClickListener {
            selectBottomNavItem(R.id.nav_profile)
        }
        view.findViewById<View>(R.id.actionViewAllNotifications)?.setOnClickListener {
            Toast.makeText(requireContext(), "All notifications will be available soon.", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<View>(R.id.actionViewAllIssued)?.setOnClickListener {
            selectBottomNavItem(R.id.nav_my_books)
        }
        view.findViewById<View>(R.id.actionViewAllDueSoon)?.setOnClickListener {
            selectBottomNavItem(R.id.nav_my_books)
        }
    }

    private fun selectBottomNavItem(itemId: Int) {
        activity?.findViewById<BottomNavigationView>(R.id.bottomNavigation)?.selectedItemId = itemId
    }

    private fun navigateToUnauthorized() {
        if (!isAdded) return
        val intent = Intent(requireContext(), UnauthorizedActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }

    private fun setupAnimations(view: View) {
        val header = view.findViewById<View>(R.id.headerCard)
        header?.alpha = 0f
        header?.translationY = 16f
        header?.animate()
            ?.alpha(1f)
            ?.translationY(0f)
            ?.setDuration(260L)
            ?.start()

        val cardIds = listOf(
            R.id.statsGrid,
            R.id.currentlyIssuedCard,
            R.id.dueSoonCard,
            R.id.notificationsCard,
            R.id.quickActionsCard,
            R.id.privilegesCard
        )
        cardIds.forEachIndexed { index, id ->
            view.findViewById<View>(id)?.apply {
                alpha = 0f
                translationY = 24f
                animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(80L + index * 45L)
                    .setDuration(240L)
                    .start()
            }
        }
    }

    private fun getInitials(name: String?): String {
        val parts = name.orEmpty()
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        if (parts.isEmpty()) return "ST"

        return parts
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
            .joinToString("")
            .ifBlank { "ST" }
    }

    private fun formatCurrency(value: Double): String {
        return String.format(Locale.US, "₹%.2f", value)
    }

    private fun formatSemester(semester: String?): String {
        val rawValue = semester.orEmpty().trim()
        if (rawValue.isBlank()) return "Semester 1"
        if (rawValue.startsWith("Semester", ignoreCase = true)) return rawValue

        val semesterNumber = rawValue.toIntOrNull()
        if (semesterNumber != null && semesterNumber > 0) {
            return "Semester $semesterNumber"
        }

        return rawValue
    }

    private fun bindIssueStatus(statusView: TextView?, issued: DashboardIssuedBook) {
        statusView ?: return
        val overdue = isOverdue(issued)
        val label = if (overdue) "OVERDUE" else "ISSUED"

        statusView.text = label
        statusView.setBackgroundResource(if (overdue) R.drawable.bg_status_overdue else R.drawable.bg_status_issued)
        statusView.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (overdue) R.color.student_danger else R.color.student_success
            )
        )
    }

    private fun isOverdue(issued: DashboardIssuedBook): Boolean {
        if (issued.status.equals("overdue", ignoreCase = true)) return true
        if (!issued.return_date.isNullOrBlank()) return false
        val dueDate = issued.due_date.orEmpty().take(10)
        if (dueDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())
            return dueDate < today
        }

        val parsedDueDate = parseDate(issued.due_date) ?: return false
        return parsedDueDate.time < System.currentTimeMillis()
    }

    private fun bindPrivilegeType(view: View, privileges: com.saroj.lmsmobile.data.models.studentdashboard.DashboardPrivileges?) {
        val typeView = view.findViewById<TextView>(R.id.textPrivilegeType) ?: return
        val rawType = privileges?.setting_type ?: privileges?.source ?: privileges?.type
        val isCustom = privileges?.is_custom == true || rawType.equals("custom", ignoreCase = true)

        typeView.text = if (isCustom) "CUSTOM" else "DEFAULT"
        typeView.setBackgroundResource(if (isCustom) R.drawable.bg_badge_custom else R.drawable.bg_badge_default)
        typeView.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (isCustom) R.color.student_purple else R.color.student_text_secondary
            )
        )
    }

    private fun loadProfilePhoto(view: View, rawUrl: String?) {
        val imageView = view.findViewById<ImageView>(R.id.imageStudentProfile) ?: return
        val initialsView = view.findViewById<TextView>(R.id.textStudentInitials) ?: return
        val photoUrl = normalizePhotoUrl(rawUrl)

        imageView.tag = photoUrl
        if (photoUrl.isNullOrBlank()) {
            imageView.visibility = View.GONE
            initialsView.visibility = View.VISIBLE
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                runCatching {
                    URL(photoUrl).openStream().use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }.onFailure {
                    Log.w("StudentDashboard", "Profile photo failed to load: $photoUrl", it)
                }.getOrNull()
            }

            if (imageView.tag == photoUrl && bitmap != null) {
                imageView.setImageBitmap(bitmap)
                imageView.visibility = View.VISIBLE
                initialsView.visibility = View.GONE
            }
        }
    }

    private fun normalizePhotoUrl(rawUrl: String?): String? {
        val value = rawUrl?.trim().orNullOrBlank() ?: return null
        if (!value.startsWith("http", ignoreCase = true)) {
            return apiRootUrl() + value.trimStart('/')
        }

        val apiRoot = apiRootUrl().trimEnd('/')
        return value
            .replace("http://127.0.0.1:8000", apiRoot)
            .replace("http://localhost:8000", apiRoot)
            .replace("https://127.0.0.1:8000", apiRoot)
            .replace("https://localhost:8000", apiRoot)
    }

    private fun apiRootUrl(): String {
        return Constants.BASE_URL.removeSuffix("api/")
    }

    private fun formatDate(rawDate: String?): String {
        val date = parseDate(rawDate) ?: return rawDate.orFallback("-")
        return SimpleDateFormat("MMM dd, yyyy", Locale.US).format(date)
    }

    private fun formatRelativeDate(rawDate: String?): String {
        val date = parseDate(rawDate) ?: return formatDate(rawDate)
        val diff = System.currentTimeMillis() - date.time
        if (diff < 0) return formatDate(rawDate)

        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)

        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "$minutes min ago"
            hours < 24 -> "$hours hr ago"
            days == 1L -> "1 day ago"
            days < 7 -> "$days days ago"
            else -> formatDate(rawDate)
        }
    }

    private fun parseDate(rawDate: String?): java.util.Date? {
        if (rawDate.isNullOrBlank()) return null
        val normalized = rawDate.trim().replace(Regex("\\.\\d+Z$"), "Z")
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd"
        )

        return patterns.firstNotNullOfOrNull { pattern ->
            runCatching {
                SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.parse(normalized)
            }.getOrNull()
        }
    }

    private fun View.setText(id: Int, value: String) {
        findViewById<TextView>(id)?.text = value
    }

    private fun View.setVisibility(id: Int, visible: Boolean) {
        findViewById<View>(id)?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun String?.orFallback(fallback: String): String {
        return if (isNullOrBlank()) fallback else this
    }

    private fun String?.orNullOrBlank(): String? = if (isNullOrBlank()) null else this

    private fun StudentDashboardResponse.content(): StudentDashboardData {
        return data ?: StudentDashboardData(
            student = student,
            stats = stats,
            currently_issued = currently_issued,
            due_soon = due_soon,
            latest_notifications = latest_notifications,
            privileges = privileges
        )
    }
}

class StudentSearchBooksFragment : Fragment() {
    private lateinit var viewModel: StudentSearchBooksViewModel
    private lateinit var adapter: StudentSearchBookAdapter
    private lateinit var foundBooksText: TextView
    private lateinit var emptyState: View
    private lateinit var emptyTitleText: TextView
    private lateinit var emptyMessageText: TextView
    private lateinit var retryButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var bottomProgressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var categoryChipText: TextView
    private lateinit var availableChipText: TextView
    private lateinit var conditionChipText: TextView
    private lateinit var sortChipText: TextView
    private var currentQuery: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_student_search_books, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        foundBooksText = view.findViewById(R.id.textFoundBooks)
        emptyState = view.findViewById(R.id.layoutSearchEmpty)
        emptyTitleText = view.findViewById(R.id.textSearchEmptyTitle)
        emptyMessageText = view.findViewById(R.id.textSearchEmptyMessage)
        retryButton = view.findViewById(R.id.buttonRetryStudentSearch)
        progressBar = view.findViewById(R.id.progressSearchBooks)
        bottomProgressBar = view.findViewById(R.id.progressSearchBooksBottom)
        recyclerView = view.findViewById(R.id.recyclerViewStudentSearchBooks)
        swipeRefreshLayout = view.findViewById(R.id.searchSwipeRefresh)
        categoryChipText = view.findViewById(R.id.textChipCategory)
        availableChipText = view.findViewById(R.id.textChipAvailable)
        conditionChipText = view.findViewById(R.id.textChipCondition)
        sortChipText = view.findViewById(R.id.textChipSort)

        setupViewModel()
        setupRecyclerView()
        setupSearch(view)
        setupFilterChips(view)
        setupPullToRefresh()
        observeBooks()
        viewModel.loadBooks(refresh = true)
    }

    private fun setupViewModel() {
        val tokenManager = (requireActivity().application as MainApplication).tokenManager
        val apiService = RetrofitClient.getApiService(tokenManager)
        val repository = BookRepository(apiService, tokenManager)

        viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return StudentSearchBooksViewModel(repository) as T
                }
            }
        )[StudentSearchBooksViewModel::class.java]
    }

    private fun setupRecyclerView() {
        adapter = StudentSearchBookAdapter { book -> handleRequestClick(book) }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        recyclerView.itemAnimator = DefaultItemAnimator().apply {
            addDuration = 180
            changeDuration = 160
            moveDuration = 180
            removeDuration = 160
        }
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy <= 0) return

                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                val totalItems = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()
                if (totalItems > 0 && lastVisibleItem >= totalItems - 4) {
                    viewModel.loadNextPage()
                }
            }
        })
    }

    private fun setupSearch(view: View) {
        view.findViewById<EditText>(R.id.etBookSearch).doAfterTextChanged { editable ->
            currentQuery = editable?.toString().orEmpty()
            viewModel.searchBooks(currentQuery)
        }
    }

    private fun setupFilterChips(view: View) {
        view.findViewById<View>(R.id.actionFilter)?.setOnClickListener {
            showSortDialog()
        }
        view.findViewById<View>(R.id.chipAllCategories)?.setOnClickListener {
            showCategoryDialog()
        }
        view.findViewById<View>(R.id.chipAvailable)?.setOnClickListener {
            showAvailabilityDialog()
        }
        view.findViewById<View>(R.id.chipCondition)?.setOnClickListener {
            showConditionDialog()
        }
        view.findViewById<View>(R.id.chipSort)?.setOnClickListener {
            showSortDialog()
        }

        view.findViewById<View>(R.id.chipReset)?.setOnClickListener {
            view.findViewById<EditText>(R.id.etBookSearch).text = null
            viewModel.resetFilters()
            updateFilterChips()
            Toast.makeText(requireContext(), "Filters reset.", Toast.LENGTH_SHORT).show()
        }

        retryButton.setOnClickListener {
            if (currentQuery.isBlank()) {
                viewModel.loadBooks(refresh = true)
            } else {
                viewModel.searchBooks(currentQuery)
            }
        }
    }

    private fun setupPullToRefresh() {
        swipeRefreshLayout.setColorSchemeResources(
            R.color.search_primary,
            R.color.search_green,
            R.color.search_orange
        )
        swipeRefreshLayout.setOnRefreshListener {
            viewModel.refresh()
        }
    }

    private fun showCategoryDialog() {
        val categories = viewModel.categories()
        val options = listOf("All Categories") + categories
        AlertDialog.Builder(requireContext())
            .setTitle("Category")
            .setItems(options.toTypedArray()) { _, index ->
                viewModel.setCategory(if (index == 0) null else options[index])
                updateFilterChips()
            }
            .show()
    }

    private fun showConditionDialog() {
        val conditions = viewModel.conditions()
        val options = listOf("All Conditions") + conditions
        AlertDialog.Builder(requireContext())
            .setTitle("Condition")
            .setItems(options.toTypedArray()) { _, index ->
                viewModel.setCondition(if (index == 0) null else options[index])
                updateFilterChips()
            }
            .show()
    }

    private fun showAvailabilityDialog() {
        val options = StudentSearchBooksViewModel.AvailabilityFilter.entries
        AlertDialog.Builder(requireContext())
            .setTitle("Availability")
            .setItems(options.map { it.label }.toTypedArray()) { _, index ->
                viewModel.setAvailabilityFilter(options[index])
                updateFilterChips()
            }
            .show()
    }

    private fun showSortDialog() {
        val options = StudentSearchBooksViewModel.SortOption.entries
        AlertDialog.Builder(requireContext())
            .setTitle("Sort Books")
            .setItems(options.map { it.label }.toTypedArray()) { _, index ->
                viewModel.setSort(options[index])
                updateFilterChips()
            }
            .show()
    }

    private fun updateFilterChips() {
        val state = viewModel.currentFilterState()
        categoryChipText.text = state.category ?: "All Categories"
        availableChipText.text = state.availabilityFilter.label
        conditionChipText.text = state.condition ?: "All Conditions"
        sortChipText.text = state.sortOption.label

        view?.findViewById<View>(R.id.chipAllCategories)
            ?.setBackgroundResource(if (state.category == null) R.drawable.bg_filter_chip_active else R.drawable.bg_filter_chip)
        view?.findViewById<View>(R.id.chipAvailable)
            ?.setBackgroundResource(
                if (state.availabilityFilter != StudentSearchBooksViewModel.AvailabilityFilter.ALL) {
                    R.drawable.bg_filter_chip_active
                } else {
                    R.drawable.bg_filter_chip
                }
            )
        view?.findViewById<View>(R.id.chipCondition)
            ?.setBackgroundResource(if (state.condition != null) R.drawable.bg_filter_chip_active else R.drawable.bg_filter_chip)
        view?.findViewById<View>(R.id.chipSort)
            ?.setBackgroundResource(if (state.sortOption != StudentSearchBooksViewModel.SortOption.TITLE_ASC) R.drawable.bg_filter_chip_active else R.drawable.bg_filter_chip)
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

        viewModel.totalBooks.observe(viewLifecycleOwner) { total ->
            foundBooksText.text = "Found $total books"
        }

        viewModel.bottomLoading.observe(viewLifecycleOwner) { isLoading ->
            bottomProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.requestResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is NetworkResult.Success -> {
                    Toast.makeText(requireContext(), result.data, Toast.LENGTH_SHORT).show()
                }
                is NetworkResult.Error -> {
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                }
                is NetworkResult.Unauthorized -> navigateToUnauthorized()
                is NetworkResult.Loading -> Unit
            }
        }
    }

    private fun showLoading() {
        progressBar.visibility = if (swipeRefreshLayout.isRefreshing) View.GONE else View.VISIBLE
        bottomProgressBar.visibility = View.GONE
        foundBooksText.text = "Loading books..."
        retryButton.visibility = View.GONE
        emptyState.visibility = View.GONE
        recyclerView.visibility = View.GONE
    }

    private fun showBooks(books: List<StudentSearchBookUiModel>) {
        progressBar.visibility = View.GONE
        bottomProgressBar.visibility = View.GONE
        swipeRefreshLayout.isRefreshing = false
        adapter.submitList(books)
        updateFilterChips()
        if (books.isEmpty()) {
            recyclerView.visibility = View.GONE
            retryButton.visibility = View.GONE
            emptyTitleText.text = "No matching books"
            emptyMessageText.text = "Try a different title, author, ISBN, publisher, or category."
            emptyState.visibility = View.VISIBLE
        } else {
            emptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        bottomProgressBar.visibility = View.GONE
        swipeRefreshLayout.isRefreshing = false
        recyclerView.visibility = View.GONE
        emptyTitleText.text = "Unable to load books"
        emptyMessageText.text = message
        retryButton.visibility = View.VISIBLE
        emptyState.visibility = View.VISIBLE
    }

    private fun handleRequestClick(book: StudentSearchBookUiModel) {
        when (book.requestState) {
            BookRequestState.NONE,
            BookRequestState.REJECTED -> showRequestConfirmation(book)
            BookRequestState.UNAVAILABLE -> {
                Toast.makeText(requireContext(), "This book is currently unavailable.", Toast.LENGTH_SHORT).show()
            }
            BookRequestState.PENDING -> {
                Toast.makeText(
                    requireContext(),
                    "You already have a pending request for this book.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            BookRequestState.APPROVED -> {
                Toast.makeText(
                    requireContext(),
                    "Your request for this book is already approved.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            BookRequestState.ALREADY_ISSUED -> {
                Toast.makeText(requireContext(), "This book is already issued to you.", Toast.LENGTH_SHORT).show()
            }
            BookRequestState.LOADING -> Unit
        }
    }

    private fun showRequestConfirmation(book: StudentSearchBookUiModel) {
        AlertDialog.Builder(requireContext())
            .setTitle("Request Book?")
            .setMessage("Do you want to request \"${book.title}\" by ${book.author}?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Request") { _, _ ->
                viewModel.submitRequest(book)
            }
            .show()
    }

    private fun navigateToUnauthorized() {
        if (!isAdded) return
        val intent = Intent(requireContext(), UnauthorizedActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }

    /*
     * Backend connection plan:
     * 1. Books and search are connected through ViewModel LiveData.
     * 2. Pagination is loaded from GET /api/books and GET /api/books/search.
     * 3. Load categories from GET /api/categories when filter UI becomes active.
     * 4. Use backend request_state when available, or combine requests/current books.
     * 5. POST /api/student/requests on request confirmation.
     * 6. Show backend message in Toast/Dialog.
     * 7. Update card state after successful request.
     */
}

class StudentMyBooksFragment : Fragment() {
    private lateinit var viewModel: StudentMyBooksViewModel
    private lateinit var adapter: MyBooksAdapter
    private lateinit var searchEditText: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: View
    private lateinit var emptyTitle: TextView
    private lateinit var emptyMessage: TextView
    private lateinit var emptyActionButton: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var statusChipText: TextView
    private lateinit var fineChipText: TextView
    private lateinit var sortChipText: TextView
    private var selectedTab: MyBooksTab = MyBooksTab.CURRENT

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_student_my_books, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        searchEditText = view.findViewById(R.id.etMyBooksSearch)
        recyclerView = view.findViewById(R.id.recyclerViewMyBooks)
        emptyState = view.findViewById(R.id.layoutMyBooksEmpty)
        emptyTitle = view.findViewById(R.id.textMyBooksEmptyTitle)
        emptyMessage = view.findViewById(R.id.textMyBooksEmptyMessage)
        emptyActionButton = view.findViewById(R.id.buttonResetMyBooksEmpty)
        progressBar = view.findViewById(R.id.progressMyBooks)
        swipeRefreshLayout = view.findViewById(R.id.myBooksSwipeRefresh)
        statusChipText = view.findViewById(R.id.textChipAllStatus)
        fineChipText = view.findViewById(R.id.textChipMyBooksFineStatus)
        sortChipText = view.findViewById(R.id.textChipMyBooksSort)

        markBottomNavActive()
        setupViewModel()
        setupRecyclerView()
        setupSearch()
        setupPullToRefresh()
        setupFilters(view)
        setupTabs(view)
        observeMyBooks()
        viewModel.loadInitial()
    }

    private fun setupViewModel() {
        val tokenManager = (requireActivity().application as MainApplication).tokenManager
        val apiService = RetrofitClient.getApiService(tokenManager)
        val repository = StudentMyBooksRepository(apiService, tokenManager)

        viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return StudentMyBooksViewModel(repository) as T
                }
            }
        )[StudentMyBooksViewModel::class.java]
    }

    private fun setupRecyclerView() {
        adapter = MyBooksAdapter { book -> showBookDetails(book) }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        recyclerView.itemAnimator = DefaultItemAnimator().apply {
            addDuration = 180
            changeDuration = 160
            moveDuration = 180
            removeDuration = 160
        }
    }

    private fun setupSearch() {
        searchEditText.doAfterTextChanged { editable ->
            viewModel.setSearchQuery(editable?.toString().orEmpty())
        }
    }

    private fun setupPullToRefresh() {
        swipeRefreshLayout.setColorSchemeResources(
            R.color.my_books_primary,
            R.color.my_books_green,
            R.color.my_books_orange
        )
        swipeRefreshLayout.setOnRefreshListener {
            viewModel.refresh()
        }
    }

    private fun setupFilters(view: View) {
        view.findViewById<View>(R.id.chipMyBooksAllStatus)?.setOnClickListener {
            showStatusFilterDialog()
        }
        view.findViewById<View>(R.id.chipMyBooksFineStatus)?.setOnClickListener {
            showFineFilterDialog()
        }
        view.findViewById<View>(R.id.chipMyBooksDueDate)?.setOnClickListener {
            showSortDialog()
        }

        val resetAction = View.OnClickListener { resetFilters() }
        view.findViewById<View>(R.id.chipMyBooksReset)?.setOnClickListener(resetAction)
        emptyActionButton.setOnClickListener(resetAction)
    }

    private fun setupTabs(view: View) {
        view.findViewById<View>(R.id.tabMyBooksCurrent)?.setOnClickListener {
            switchTab(MyBooksTab.CURRENT)
        }
        view.findViewById<View>(R.id.tabMyBooksHistory)?.setOnClickListener {
            switchTab(MyBooksTab.HISTORY)
        }
        view.findViewById<View>(R.id.tabMyBooksDueSoon)?.setOnClickListener {
            switchTab(MyBooksTab.DUE_SOON)
        }
    }

    private fun observeMyBooks() {
        viewModel.summaryState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is NetworkResult.Success -> updateSummaryCards(result.data)
                is NetworkResult.Error -> {
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                }
                is NetworkResult.Unauthorized -> navigateToUnauthorized()
                is NetworkResult.Loading -> Unit
            }
        }

        viewModel.booksState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is NetworkResult.Loading -> showLoading()
                is NetworkResult.Success -> showBooks(result.data)
                is NetworkResult.Error -> showError(result.message)
                is NetworkResult.Unauthorized -> navigateToUnauthorized()
            }
        }
    }

    private fun switchTab(tab: MyBooksTab) {
        selectedTab = tab
        updateTabStyles()
        viewModel.switchTab(tab)
    }

    private fun showLoading() {
        progressBar.visibility = if (swipeRefreshLayout.isRefreshing) View.GONE else View.VISIBLE
        recyclerView.visibility = View.GONE
        emptyState.visibility = View.GONE
    }

    private fun showBooks(books: List<MyBookUiModel>) {
        progressBar.visibility = View.GONE
        swipeRefreshLayout.isRefreshing = false
        adapter.submitList(books)
        updateFilterChips()
        updateEmptyState(books.isEmpty(), viewModel.currentFilterState().query)
    }

    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        swipeRefreshLayout.isRefreshing = false
        recyclerView.visibility = View.GONE
        emptyTitle.text = "Unable to load books"
        emptyMessage.text = message
        emptyActionButton.text = "Retry"
        emptyActionButton.setOnClickListener { viewModel.refresh() }
        emptyState.visibility = View.VISIBLE
    }

    private fun updateSummaryCards(summary: MyBooksSummaryUiModel) {
        view?.findViewById<TextView>(R.id.textTotalIssuedValue)?.text = summary.totalIssued.toString()
        view?.findViewById<TextView>(R.id.textCurrentlyBorrowedValue)?.text = summary.currentlyBorrowed.toString()
        view?.findViewById<TextView>(R.id.textOverdueBooksValue)?.text = summary.overdueBooks.toString()
        view?.findViewById<TextView>(R.id.textPendingFineValue)?.text = summary.pendingFine
    }

    private fun updateEmptyState(isEmpty: Boolean, query: String) {
        recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        if (!isEmpty) return

        emptyActionButton.text = "Reset Filters"
        emptyActionButton.setOnClickListener { resetFilters() }
        if (query.isNotBlank()) {
            emptyTitle.text = "No books found"
            emptyMessage.text = "Try changing your search or filters."
            return
        }

        when (selectedTab) {
            MyBooksTab.CURRENT -> {
                emptyTitle.text = "No books currently borrowed."
                emptyMessage.text = "Issued books will appear here once approved by the library."
            }
            MyBooksTab.HISTORY -> {
                emptyTitle.text = "No returned books yet."
                emptyMessage.text = "Your borrowing history will appear here after returns."
            }
            MyBooksTab.DUE_SOON -> {
                emptyTitle.text = "No books due soon. Great job!"
                emptyMessage.text = "Books approaching their due date will appear here."
            }
        }
    }

    private fun showStatusFilterDialog() {
        val options = MyBooksStatusFilter.entries
        val selectedIndex = options.indexOf(viewModel.currentFilterState().statusFilter)
        AlertDialog.Builder(requireContext())
            .setTitle("All Status")
            .setSingleChoiceItems(options.map { it.label }.toTypedArray(), selectedIndex) { dialog, index ->
                viewModel.setStatusFilter(options[index])
                updateFilterChips()
                dialog.dismiss()
            }
            .show()
    }

    private fun showFineFilterDialog() {
        val options = MyBooksFineFilter.entries
        val selectedIndex = options.indexOf(viewModel.currentFilterState().fineFilter)
        AlertDialog.Builder(requireContext())
            .setTitle("All Fine Status")
            .setSingleChoiceItems(options.map { it.label }.toTypedArray(), selectedIndex) { dialog, index ->
                viewModel.setFineFilter(options[index])
                updateFilterChips()
                dialog.dismiss()
            }
            .show()
    }

    private fun showSortDialog() {
        val options = MyBooksSortOption.entries
        val selectedIndex = options.indexOf(viewModel.currentFilterState().sortOption)
        AlertDialog.Builder(requireContext())
            .setTitle("Sort Books")
            .setSingleChoiceItems(options.map { it.label }.toTypedArray(), selectedIndex) { dialog, index ->
                viewModel.setSortOption(options[index])
                updateFilterChips()
                dialog.dismiss()
            }
            .show()
    }

    private fun resetFilters() {
        if (searchEditText.text?.isNotEmpty() == true) {
            searchEditText.text = null
        }
        viewModel.resetFilters()
        updateFilterChips()
        Toast.makeText(requireContext(), "Filters reset.", Toast.LENGTH_SHORT).show()
    }

    private fun updateFilterChips() {
        val state = viewModel.currentFilterState()
        statusChipText.text = state.statusFilter.label
        fineChipText.text = state.fineFilter.label
        sortChipText.text = state.sortOption.label

        view?.findViewById<View>(R.id.chipMyBooksAllStatus)?.setBackgroundResource(
            if (state.statusFilter == MyBooksStatusFilter.ALL) {
                R.drawable.bg_my_books_chip_active
            } else {
                R.drawable.bg_my_books_chip
            }
        )
        view?.findViewById<View>(R.id.chipMyBooksFineStatus)?.setBackgroundResource(
            if (state.fineFilter == MyBooksFineFilter.ALL) {
                R.drawable.bg_my_books_chip
            } else {
                R.drawable.bg_my_books_chip_active
            }
        )
        view?.findViewById<View>(R.id.chipMyBooksDueDate)?.setBackgroundResource(
            if (state.sortOption == MyBooksSortOption.DUE_DATE_ASC) {
                R.drawable.bg_my_books_chip
            } else {
                R.drawable.bg_my_books_chip_active
            }
        )
    }

    private fun showBookDetails(book: MyBookUiModel) {
        AlertDialog.Builder(requireContext())
            .setTitle(book.title)
            .setMessage(
                "Author: ${book.author}\n" +
                    "ISBN: ${book.isbn}\n" +
                    "Status: ${book.status.displayLabel()}\n" +
                    "Issue Date: ${book.issueDate}\n" +
                    "Due Date: ${book.dueDate}\n" +
                    "Return Date: ${book.returnDate ?: "-"}\n" +
                    "Fine: ${book.fineAmount} (${book.fineStatus.displayLabel()})"
            )
            .setPositiveButton("Close", null)
            .show()
    }

    private fun updateTabStyles() {
        val tabStyles = listOf(
            TabStyle(R.id.tabMyBooksCurrent, R.id.iconTabCurrent, R.id.textTabCurrent, MyBooksTab.CURRENT),
            TabStyle(R.id.tabMyBooksHistory, R.id.iconTabHistory, R.id.textTabHistory, MyBooksTab.HISTORY),
            TabStyle(R.id.tabMyBooksDueSoon, R.id.iconTabDueSoon, R.id.textTabDueSoon, MyBooksTab.DUE_SOON)
        )

        tabStyles.forEach { style ->
            val selected = style.tab == selectedTab
            view?.findViewById<View>(style.containerId)?.setBackgroundResource(
                if (selected) R.drawable.bg_segment_active else 0
            )
            view?.findViewById<ImageView>(style.iconId)?.setColorFilter(
                ContextCompat.getColor(
                    requireContext(),
                    if (selected) R.color.white else R.color.my_books_text_muted
                )
            )
            view?.findViewById<TextView>(style.textId)?.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (selected) R.color.white else R.color.my_books_text_muted
                )
            )
        }
    }

    private fun markBottomNavActive() {
        activity
            ?.findViewById<BottomNavigationView>(R.id.bottomNavigation)
            ?.menu
            ?.findItem(R.id.nav_my_books)
            ?.isChecked = true
    }

    private fun MyBookStatus.displayLabel(): String {
        return when (this) {
            MyBookStatus.ISSUED -> "Issued"
            MyBookStatus.DUE_SOON -> "Due Soon"
            MyBookStatus.OVERDUE -> "Overdue"
            MyBookStatus.RETURNED -> "Returned"
        }
    }

    private fun com.saroj.lmsmobile.ui.student.model.FineStatus.displayLabel(): String {
        return when (this) {
            com.saroj.lmsmobile.ui.student.model.FineStatus.NONE -> "None"
            com.saroj.lmsmobile.ui.student.model.FineStatus.UNPAID -> "Unpaid"
            com.saroj.lmsmobile.ui.student.model.FineStatus.PAID -> "Paid"
            com.saroj.lmsmobile.ui.student.model.FineStatus.WAIVED -> "Waived"
        }
    }

    private fun navigateToUnauthorized() {
        if (!isAdded) return
        val intent = Intent(requireContext(), UnauthorizedActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }

    private data class TabStyle(
        val containerId: Int,
        val iconId: Int,
        val textId: Int,
        val tab: MyBooksTab
    )
}

class StudentMyFinesFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_student_my_fines, container, false)
}

class StudentProfileFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_student_profile, container, false)
}

