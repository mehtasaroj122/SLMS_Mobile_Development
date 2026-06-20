package com.saroj.lmsmobile.ui.student.fragments

import android.content.Intent
import android.graphics.Color
import android.graphics.BitmapFactory
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
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
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.saroj.lmsmobile.MainApplication
import com.saroj.lmsmobile.R
import com.saroj.lmsmobile.api.RetrofitClient
import com.saroj.lmsmobile.data.models.studentdashboard.DashboardIssuedBook
import com.saroj.lmsmobile.data.models.studentdashboard.DashboardNotification
import com.saroj.lmsmobile.data.models.studentdashboard.StudentDashboardData
import com.saroj.lmsmobile.data.models.studentdashboard.StudentDashboardResponse
import com.saroj.lmsmobile.data.models.common.NetworkResult
import com.saroj.lmsmobile.data.repository.BookRepository
import com.saroj.lmsmobile.data.repository.NotificationRepository
import com.saroj.lmsmobile.data.repository.StudentDashboardRepository
import com.saroj.lmsmobile.data.repository.StudentMyBooksRepository
import com.saroj.lmsmobile.data.repository.StudentMyFinesRepository
import com.saroj.lmsmobile.data.repository.StudentProfileRepository
import com.saroj.lmsmobile.ui.common.UnauthorizedActivity
import com.saroj.lmsmobile.ui.auth.LoginActivity
import com.saroj.lmsmobile.ui.student.StudentDashboardActivity
import com.saroj.lmsmobile.ui.student.adapter.MyFinesAdapter
import com.saroj.lmsmobile.ui.student.adapter.MyBooksAdapter
import com.saroj.lmsmobile.ui.student.adapter.StudentSearchBookAdapter
import com.saroj.lmsmobile.ui.student.notifications.NotificationsAdapter
import com.saroj.lmsmobile.ui.student.model.BookRequestState
import com.saroj.lmsmobile.ui.student.model.MyFineStatus
import com.saroj.lmsmobile.ui.student.model.MyFineUiModel
import com.saroj.lmsmobile.ui.student.model.MyFinesTab
import com.saroj.lmsmobile.ui.student.model.MyFinesSummaryUiModel
import com.saroj.lmsmobile.ui.student.model.MyBookStatus
import com.saroj.lmsmobile.ui.student.model.MyBookUiModel
import com.saroj.lmsmobile.ui.student.model.MyBooksFineFilter
import com.saroj.lmsmobile.ui.student.model.MyBooksSortOption
import com.saroj.lmsmobile.ui.student.model.MyBooksStatusFilter
import com.saroj.lmsmobile.ui.student.model.MyBooksSummaryUiModel
import com.saroj.lmsmobile.ui.student.model.MyBooksTab
import com.saroj.lmsmobile.ui.student.model.DeleteEligibilityUiModel
import com.saroj.lmsmobile.ui.student.model.ProfileTab
import com.saroj.lmsmobile.ui.student.model.StudentSearchBookUiModel
import com.saroj.lmsmobile.ui.student.model.StudentProfileUiModel
import com.saroj.lmsmobile.ui.student.viewmodel.StudentDashboardViewModel
import com.saroj.lmsmobile.ui.student.viewmodel.StudentMyBooksViewModel
import com.saroj.lmsmobile.ui.student.viewmodel.StudentMyFinesViewModel
import com.saroj.lmsmobile.ui.student.viewmodel.StudentProfileViewModel
import com.saroj.lmsmobile.ui.student.viewmodel.StudentSearchBooksViewModel
import com.saroj.lmsmobile.utils.Constants
import com.saroj.lmsmobile.utils.LmsToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class StudentDashboardFragment : Fragment() {
    private lateinit var viewModel: StudentDashboardViewModel
    private lateinit var notificationRepository: NotificationRepository
    private var refreshToastPending = false
    private var dashboardNotifications: List<DashboardNotification> = emptyList()
    private var dashboardUnreadCount = 0

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
        notificationRepository = NotificationRepository(apiService, tokenManager)

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
                LmsToast.show(requireContext(), "Dashboard refreshed")
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
                LmsToast.show(requireContext(), "Unable to load dashboard: $shortMessage")
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
        loadNotificationCount()
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
        view.setText(R.id.textDashboardGreeting, greetingForCurrentTime())
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
        dashboardNotifications = notifications
        if (dashboardUnreadCount == 0) {
            dashboardUnreadCount = notifications.count { it.read_at.isNullOrBlank() }
        }
        updateNotificationBadges(view, dashboardUnreadCount)
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
                view.findViewById<View>(itemId)?.apply {
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        showDashboardNotificationDetails(notification)
                        markDashboardNotificationAsRead(notification)
                    }
                }
            }
        }
    }

    private fun loadNotificationCount() {
        if (!::notificationRepository.isInitialized) return
        lifecycleScope.launch {
            notificationRepository.getNotificationCount().collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        dashboardUnreadCount = result.data.unreadCount
                            ?: dashboardNotifications.count { it.read_at.isNullOrBlank() }
                        view?.let { updateNotificationBadges(it, dashboardUnreadCount) }
                    }
                    is NetworkResult.Unauthorized -> navigateToUnauthorized()
                    else -> Unit
                }
            }
        }
    }

    private fun updateNotificationBadges(view: View, unreadCount: Int) {
        view.setText(R.id.textNotificationsCount, unreadCount.toString())
        view.findViewById<TextView>(R.id.textHeaderNotificationBadge)?.apply {
            text = if (unreadCount > 99) "99+" else unreadCount.toString()
            visibility = if (unreadCount > 0) View.VISIBLE else View.GONE
        }
    }

    private fun showDashboardNotificationDetails(notification: DashboardNotification) {
        val dialog = BottomSheetDialog(requireContext())
        val detailView = layoutInflater.inflate(R.layout.bottom_sheet_notification_detail, null)
        val style = DashboardNotificationStyle.from(notification.type)

        detailView.findViewById<TextView>(R.id.textDetailTitle).text =
            notification.title.orFallback("Notification")
        detailView.findViewById<TextView>(R.id.textDetailMessage).text =
            notification.message.orFallback("-")
        detailView.findViewById<TextView>(R.id.textDetailDate).text =
            NotificationsAdapter.formatFullDateTime(notification.created_at).ifBlank { "-" }
        detailView.findViewById<TextView>(R.id.textDetailType).text = style.label
        detailView.findViewById<TextView>(R.id.textDetailTypeBadge).apply {
            text = style.label
            setBackgroundResource(style.badgeBackground)
            setTextColor(ContextCompat.getColor(requireContext(), style.tintColor))
        }
        detailView.findViewById<View>(R.id.viewDetailIconTile).setBackgroundResource(style.iconBackground)
        detailView.findViewById<ImageView>(R.id.imageDetailIcon).apply {
            setImageResource(style.iconDrawable)
            setColorFilter(ContextCompat.getColor(requireContext(), style.tintColor))
        }

        detailView.findViewById<View>(R.id.buttonNotificationDetailClose).setOnClickListener {
            dialog.dismiss()
        }
        detailView.findViewById<View>(R.id.buttonNotificationDetailOk).setOnClickListener {
            dialog.dismiss()
        }

        dialog.setContentView(detailView)
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                ?.setBackgroundColor(Color.TRANSPARENT)
        }
        dialog.show()
    }

    private fun markDashboardNotificationAsRead(notification: DashboardNotification) {
        val id = notification.id ?: return
        if (!notification.read_at.isNullOrBlank() || !::notificationRepository.isInitialized) return

        lifecycleScope.launch {
            notificationRepository.markAsRead(id).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        dashboardNotifications = dashboardNotifications.map {
                            if (it.id == id) it.copy(read_at = currentTimestamp()) else it
                        }
                        dashboardUnreadCount = (dashboardUnreadCount - 1).coerceAtLeast(0)
                        view?.let {
                            bindNotifications(it, dashboardNotifications)
                            updateNotificationBadges(it, dashboardUnreadCount)
                        }
                    }
                    is NetworkResult.Error -> LmsToast.show(
                        requireContext(),
                        "Unable to mark notification as read: ${result.message}"
                    )
                    is NetworkResult.Unauthorized -> navigateToUnauthorized()
                    is NetworkResult.Loading -> Unit
                }
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
            (activity as? StudentDashboardActivity)?.openMyRequests()
        }
        view.findViewById<View>(R.id.actionMyFines)?.setOnClickListener {
            selectBottomNavItem(R.id.nav_my_fines)
        }
        view.findViewById<View>(R.id.actionMyProfile)?.setOnClickListener {
            (activity as? StudentDashboardActivity)?.openProfile()
        }
        view.findViewById<View>(R.id.buttonHeaderNotifications)?.setOnClickListener {
            (activity as? StudentDashboardActivity)?.openNotifications()
        }
        view.findViewById<View>(R.id.actionViewAllNotifications)?.setOnClickListener {
            (activity as? StudentDashboardActivity)?.openNotifications()
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
        val diff = (System.currentTimeMillis() - date.time).coerceAtLeast(0L)

        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)

        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> if (minutes == 1L) "1 min ago" else "$minutes mins ago"
            hours < 24 -> if (hours == 1L) "1 hr ago" else "$hours hrs ago"
            days == 1L -> "Yesterday"
            else -> formatDate(rawDate)
        }
    }

    private fun parseDate(rawDate: String?): java.util.Date? {
        if (rawDate.isNullOrBlank()) return null
        val normalized = rawDate.trim()
            .replace(Regex("\\.\\d+Z$"), "Z")
            .replace(Regex("\\.\\d+$"), "")
        val timezonePatterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ssXXX"
        )
        val localPatterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd"
        )

        timezonePatterns.firstNotNullOfOrNull { pattern ->
            runCatching {
                SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.parse(normalized)
            }.getOrNull()
        }?.let { return it }

        return localPatterns.firstNotNullOfOrNull { pattern ->
            runCatching {
                SimpleDateFormat(pattern, Locale.US).parse(normalized)
            }.getOrNull()
        }
    }

    private fun currentTimestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(java.util.Date())
    }

    private fun greetingForCurrentTime(): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..20 -> "Good Evening"
            else -> "Good Night"
        }
    }

    private data class DashboardNotificationStyle(
        val label: String,
        val iconDrawable: Int,
        val iconBackground: Int,
        val badgeBackground: Int,
        val tintColor: Int
    ) {
        companion object {
            fun from(rawType: String?): DashboardNotificationStyle {
                val type = rawType.orEmpty().lowercase(Locale.US)
                return when {
                    type.contains("returned") -> blue("BOOK", R.drawable.ic_book)
                    type.contains("approved") -> green("BOOK", R.drawable.ic_check_circle)
                    type.contains("rejected") -> red("BOOK", R.drawable.ic_x_circle)
                    type.contains("fine") && type.contains("paid") -> green("FINE", R.drawable.ic_rupee)
                    type.contains("fine") -> orange("FINE", R.drawable.ic_warning)
                    type.contains("account") || type.contains("status") -> purple("ACCOUNT", R.drawable.ic_shield)
                    else -> blue("INFO", R.drawable.ic_bell)
                }
            }

            private fun blue(label: String, icon: Int) = DashboardNotificationStyle(
                label,
                icon,
                R.drawable.bg_notification_icon_blue,
                R.drawable.bg_notification_unread_badge,
                R.color.notifications_primary
            )

            private fun green(label: String, icon: Int) = DashboardNotificationStyle(
                label,
                icon,
                R.drawable.bg_notification_icon_green,
                R.drawable.bg_notification_badge_green,
                R.color.notifications_green
            )

            private fun red(label: String, icon: Int) = DashboardNotificationStyle(
                label,
                icon,
                R.drawable.bg_notification_icon_red,
                R.drawable.bg_notification_badge_red,
                R.color.notifications_red
            )

            private fun orange(label: String, icon: Int) = DashboardNotificationStyle(
                label,
                icon,
                R.drawable.bg_notification_icon_orange,
                R.drawable.bg_notification_badge_orange,
                R.color.notifications_orange
            )

            private fun purple(label: String, icon: Int) = DashboardNotificationStyle(
                label,
                icon,
                R.drawable.bg_notification_icon_purple,
                R.drawable.bg_notification_badge_purple,
                R.color.notifications_purple
            )
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
            LmsToast.show(requireContext(), "Filters reset.")
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
                    LmsToast.show(requireContext(), result.data)
                }
                is NetworkResult.Error -> {
                    LmsToast.show(requireContext(), result.message)
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
                LmsToast.show(requireContext(), "This book is currently unavailable.")
            }
            BookRequestState.PENDING -> {
                LmsToast.show(requireContext(), "You already have a pending request for this book.")
            }
            BookRequestState.APPROVED -> {
                LmsToast.show(requireContext(), "Your request for this book is already approved.")
            }
            BookRequestState.ALREADY_ISSUED -> {
                LmsToast.show(requireContext(), "This book is already issued to you.")
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
                    LmsToast.show(requireContext(), result.message)
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
        LmsToast.show(requireContext(), "Filters reset.")
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
    private lateinit var viewModel: StudentMyFinesViewModel
    private lateinit var adapter: MyFinesAdapter
    private lateinit var searchEditText: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: View
    private lateinit var emptyTitle: TextView
    private lateinit var emptyMessage: TextView
    private lateinit var emptyActionButton: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var footerText: TextView
    private var selectedTab: MyFinesTab = MyFinesTab.ALL

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_student_my_fines, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        searchEditText = view.findViewById(R.id.etMyFinesSearch)
        recyclerView = view.findViewById(R.id.recyclerViewMyFines)
        emptyState = view.findViewById(R.id.layoutMyFinesEmpty)
        emptyTitle = view.findViewById(R.id.textMyFinesEmptyTitle)
        emptyMessage = view.findViewById(R.id.textMyFinesEmptyMessage)
        emptyActionButton = view.findViewById(R.id.buttonResetMyFinesEmpty)
        progressBar = view.findViewById(R.id.progressMyFines)
        swipeRefreshLayout = view.findViewById(R.id.myFinesSwipeRefresh)
        footerText = view.findViewById(R.id.textMyFinesFooter)

        markBottomNavActive()
        setupViewModel()
        setupRecyclerView()
        setupSearch()
        setupPullToRefresh()
        setupFilters(view)
        setupTabs(view)
        observeMyFines()
        viewModel.loadInitial()
    }

    private fun setupViewModel() {
        val tokenManager = (requireActivity().application as MainApplication).tokenManager
        val apiService = RetrofitClient.getApiService(tokenManager)
        val repository = StudentMyFinesRepository(apiService, tokenManager)

        viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return StudentMyFinesViewModel(repository) as T
                }
            }
        )[StudentMyFinesViewModel::class.java]
    }

    private fun setupRecyclerView() {
        adapter = MyFinesAdapter { fine -> viewModel.loadFineDetail(fine) }
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
        searchEditText.doAfterTextChanged { editable ->
            viewModel.setSearchQuery(editable?.toString().orEmpty())
        }
    }

    private fun setupPullToRefresh() {
        swipeRefreshLayout.setColorSchemeResources(
            R.color.my_fines_primary,
            R.color.my_fines_green,
            R.color.my_fines_orange
        )
        swipeRefreshLayout.setOnRefreshListener {
            viewModel.refresh()
        }
    }

    private fun setupFilters(view: View) {
        emptyActionButton.setOnClickListener { resetFilters() }
    }

    private fun setupTabs(view: View) {
        view.findViewById<View>(R.id.tabMyFinesAll)?.setOnClickListener {
            switchTab(MyFinesTab.ALL)
        }
        view.findViewById<View>(R.id.tabMyFinesPending)?.setOnClickListener {
            switchTab(MyFinesTab.PENDING)
        }
        view.findViewById<View>(R.id.tabMyFinesPaid)?.setOnClickListener {
            switchTab(MyFinesTab.PAID)
        }
        view.findViewById<View>(R.id.tabMyFinesWaived)?.setOnClickListener {
            switchTab(MyFinesTab.WAIVED)
        }
        updateTabStyles()
    }

    private fun observeMyFines() {
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

        viewModel.finesState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is NetworkResult.Loading -> showLoading()
                is NetworkResult.Success -> showFines(result.data)
                is NetworkResult.Error -> showError(result.message)
                is NetworkResult.Unauthorized -> navigateToUnauthorized()
            }
        }

        viewModel.fineDetailState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is NetworkResult.Loading -> Unit
                is NetworkResult.Success -> showFineDetails(result.data)
                is NetworkResult.Error -> LmsToast.show(
                    requireContext(),
                    "Unable to load fine details: ${result.message}"
                )
                is NetworkResult.Unauthorized -> navigateToUnauthorized()
            }
        }
    }

    private fun switchTab(tab: MyFinesTab) {
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

    private fun showFines(fines: List<MyFineUiModel>) {
        progressBar.visibility = View.GONE
        swipeRefreshLayout.isRefreshing = false
        adapter.submitList(fines)
        updateEmptyState(fines.isEmpty(), searchEditText.text?.toString().orEmpty().trim())
        updateFooterCount(fines.size)
    }

    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        swipeRefreshLayout.isRefreshing = false
        recyclerView.visibility = View.GONE
        footerText.visibility = View.GONE
        emptyTitle.text = "Unable to load fines"
        emptyMessage.text = message
        emptyActionButton.text = "Retry"
        emptyActionButton.setOnClickListener { viewModel.refresh() }
        emptyState.visibility = View.VISIBLE
    }

    private fun updateSummaryCards(summary: MyFinesSummaryUiModel) {
        view?.findViewById<TextView>(R.id.textOutstandingValue)?.text = summary.outstandingAmount
        view?.findViewById<TextView>(R.id.textPaidValue)?.text = summary.paidAmount
        view?.findViewById<TextView>(R.id.textWaivedValue)?.text = summary.waivedAmount
        view?.findViewById<TextView>(R.id.textOverdueBooksValue)?.text = summary.overdueBooks.toString()
    }

    private fun showFineDetails(fine: MyFineUiModel) {
        AlertDialog.Builder(requireContext())
            .setTitle(fine.bookTitle)
            .setMessage(
                "Reason: ${fine.reason}\n" +
                    "Due Date: ${fine.dueDate}\n" +
                    "Days Overdue: ${fine.daysOverdue}\n" +
                    "Fine Amount: ${fine.amount}\n" +
                    "Status: ${fine.status.displayLabel()}"
            )
            .setPositiveButton("Close", null)
            .show()
    }

    private fun updateTabStyles() {
        val styles = listOf(
            MyFinesTabStyle(
                containerId = R.id.tabMyFinesAll,
                iconId = R.id.iconTabFinesAll,
                textId = R.id.textTabFinesAll,
                tab = MyFinesTab.ALL,
                inactiveIconColor = R.color.my_fines_text_muted
            ),
            MyFinesTabStyle(
                containerId = R.id.tabMyFinesPending,
                iconId = R.id.iconTabFinesPending,
                textId = R.id.textTabFinesPending,
                tab = MyFinesTab.PENDING,
                inactiveIconColor = R.color.my_fines_red
            ),
            MyFinesTabStyle(
                containerId = R.id.tabMyFinesPaid,
                iconId = R.id.iconTabFinesPaid,
                textId = R.id.textTabFinesPaid,
                tab = MyFinesTab.PAID,
                inactiveIconColor = R.color.my_fines_green
            ),
            MyFinesTabStyle(
                containerId = R.id.tabMyFinesWaived,
                iconId = R.id.iconTabFinesWaived,
                textId = R.id.textTabFinesWaived,
                tab = MyFinesTab.WAIVED,
                inactiveIconColor = R.color.my_fines_yellow
            )
        )

        styles.forEach { style ->
            val selected = style.tab == selectedTab
            view?.findViewById<View>(style.containerId)?.setBackgroundResource(
                if (selected) R.drawable.bg_fines_segment_active else 0
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
                    if (selected) R.color.white else R.color.my_fines_text_primary
                )
            )
        }
    }

    private fun updateFooterCount(count: Int) {
        footerText.text = if (count == 0) {
            "Showing 0 of 0 fines"
        } else {
            "Showing 1 to $count of $count fines"
        }
        footerText.visibility = View.VISIBLE
    }

    private fun updateEmptyState(isEmpty: Boolean, query: String) {
        recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        if (!isEmpty) return

        emptyActionButton.text = "Reset Filters"
        emptyActionButton.setOnClickListener { resetFilters() }
        if (query.isNotBlank()) {
            emptyTitle.text = "No matching fines"
            emptyMessage.text = "Try another keyword or reset filters."
            return
        }

        when (selectedTab) {
            MyFinesTab.ALL -> {
                emptyTitle.text = "No fines found"
                emptyMessage.text = "You have no library fines. Great job!"
            }
            MyFinesTab.PENDING -> {
                emptyTitle.text = "No pending fines"
                emptyMessage.text = "You have cleared all dues."
            }
            MyFinesTab.PAID -> {
                emptyTitle.text = "No paid fines yet"
                emptyMessage.text = "Paid fines will appear here after payment."
            }
            MyFinesTab.WAIVED -> {
                emptyTitle.text = "No waived fines"
                emptyMessage.text = "Waived fines will appear here after library approval."
            }
        }
    }

    private fun resetFilters() {
        if (searchEditText.text?.isNotEmpty() == true) {
            searchEditText.setText("")
        }
        selectedTab = MyFinesTab.ALL
        updateTabStyles()
        viewModel.resetFilters()
        viewModel.switchTab(MyFinesTab.ALL)
        LmsToast.show(requireContext(), "Filters reset.")
    }

    private fun markBottomNavActive() {
        activity
            ?.findViewById<BottomNavigationView>(R.id.bottomNavigation)
            ?.menu
            ?.findItem(R.id.nav_my_fines)
            ?.isChecked = true
    }

    private fun MyFineStatus.displayLabel(): String {
        return when (this) {
            MyFineStatus.PENDING -> "Pending"
            MyFineStatus.PAID -> "Paid"
            MyFineStatus.WAIVED -> "Waived"
        }
    }

    private fun navigateToUnauthorized() {
        if (!isAdded) return
        val intent = Intent(requireContext(), UnauthorizedActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }

    private data class MyFinesTabStyle(
        val containerId: Int,
        val iconId: Int,
        val textId: Int,
        val tab: MyFinesTab,
        val inactiveIconColor: Int
    )
}

class StudentProfileFragment : Fragment() {
    private lateinit var viewModel: StudentProfileViewModel
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private var selectedTab = ProfileTab.PROFILE
    private var isEditMode = false
    private var currentPasswordVisible = false
    private var newPasswordVisible = false
    private var confirmPasswordVisible = false
    private var selectedPhotoFile: File? = null
    private val genderOptions = listOf("Male", "Female", "Others")

    private var profile = StudentProfileUiModel(
        name = "Saroj Mehta",
        email = "mehtasaroj315@gmail.com",
        role = "Student",
        username = "mehtasaroj315",
        studentId = "STU-001-001",
        department = "English Literature",
        memberSince = "April 28, 2026",
        lastLogin = "Jun 19, 2026 23:49",
        phone = "+9779807006324",
        gender = "Male",
        address = "Kathmandu, Nepal",
        profilePhotoUrl = null
    )

    private val deleteEligibility = DeleteEligibilityUiModel(
        canDelete = false,
        issuedBooks = 3,
        pendingFines = "\u20B950",
        activeRequests = 2,
        reasons = listOf(
            "Return all issued books",
            "Clear pending fines",
            "Resolve active requests"
        )
    )

    private var currentDeleteEligibility = deleteEligibility

    private val photoPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@registerForActivityResult
        handleSelectedPhoto(uri)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_student_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        swipeRefreshLayout = view.findViewById(R.id.profileSwipeRefresh)
        setupViewModel()
        setupHeader()
        setupTabs()
        setupProfileInfoSection()
        setupPhotoSection()
        setupSecuritySection()
        setupDeleteSection()
        setupPullToRefresh()
        observeProfile()
        setupDummyProfile()
        switchTab(ProfileTab.PROFILE)
        viewModel.loadInitial()
    }

    private fun setupViewModel() {
        val tokenManager = (requireActivity().application as MainApplication).tokenManager
        val apiService = RetrofitClient.getApiService(tokenManager)
        val repository = StudentProfileRepository(apiService, tokenManager)

        viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return StudentProfileViewModel(repository) as T
                }
            }
        )[StudentProfileViewModel::class.java]
    }

    private fun setupPullToRefresh() {
        swipeRefreshLayout.setColorSchemeResources(
            R.color.profile_primary,
            R.color.profile_green,
            R.color.profile_orange
        )
        swipeRefreshLayout.setOnRefreshListener {
            if (isEditMode) exitEditMode(restoreValues = true)
            viewModel.refresh()
        }
    }

    private fun observeProfile() {
        viewModel.profileState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is NetworkResult.Loading -> {
                    if (!swipeRefreshLayout.isRefreshing) {
                        swipeRefreshLayout.isRefreshing = true
                    }
                }
                is NetworkResult.Success -> {
                    swipeRefreshLayout.isRefreshing = false
                    profile = result.data
                    bindSummary()
                    bindProfileFields()
                    if (!isEditMode) exitEditMode(restoreValues = false)
                }
                is NetworkResult.Error -> {
                    swipeRefreshLayout.isRefreshing = false
                    showToast(result.message)
                }
                is NetworkResult.Unauthorized -> navigateToLogin()
            }
        }

        viewModel.deleteEligibilityState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is NetworkResult.Success -> {
                    currentDeleteEligibility = result.data
                    bindDeleteEligibility(result.data)
                }
                is NetworkResult.Error -> showToast(result.message)
                is NetworkResult.Unauthorized -> navigateToLogin()
                is NetworkResult.Loading -> Unit
            }
        }

        viewModel.profileActionState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is NetworkResult.Loading -> swipeRefreshLayout.isRefreshing = true
                is NetworkResult.Success -> {
                    swipeRefreshLayout.isRefreshing = false
                    selectedPhotoFile = null
                    if (result.data.contains("photo", ignoreCase = true)) {
                        view?.setProfileText(R.id.textSelectedPhotoFile, "No file chosen")
                    }
                    if (result.data.contains("password", ignoreCase = true)) {
                        clearPasswordForm()
                    }
                    showToast(result.data)
                    if (result.data.contains("deleted", ignoreCase = true)) {
                        navigateToLogin()
                    }
                }
                is NetworkResult.Error -> {
                    swipeRefreshLayout.isRefreshing = false
                    showToast(result.message)
                }
                is NetworkResult.Unauthorized -> navigateToLogin()
            }
        }
    }

    private fun setupHeader() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    handleProfileBack()
                }
            }
        )

        view?.findViewById<View>(R.id.buttonProfileBack)?.setOnClickListener {
            handleProfileBack()
        }
    }

    private fun handleProfileBack() {
        if (isEditMode) {
            exitEditMode(restoreValues = true)
            return
        }

        if (parentFragmentManager.backStackEntryCount > 0) {
            parentFragmentManager.popBackStack()
            return
        }

        activity
            ?.findViewById<BottomNavigationView>(R.id.bottomNavigation)
            ?.selectedItemId = R.id.nav_dashboard
    }

    private fun setupDummyProfile() {
        bindSummary()
        bindProfileFields()
    }

    private fun bindSummary() {
        val initials = getInitials(profile.name)
        view?.setProfileText(R.id.textProfileInitials, initials)
        view?.setProfileText(R.id.textPhotoPreviewInitials, initials)
        view?.setProfileText(R.id.textProfileName, profile.name)
        view?.setProfileText(R.id.textProfileEmail, profile.email)
        view?.setProfileText(R.id.textProfileRole, profile.role.uppercase(Locale.US))
        view?.setProfileText(R.id.textProfileUsername, profile.username)
        view?.setProfileText(R.id.textProfileStudentId, profile.studentId)
        view?.setProfileText(R.id.textProfileDepartment, profile.department)
        view?.setProfileText(R.id.textProfileMemberSince, profile.memberSince)
        view?.setProfileText(R.id.textProfileLastLogin, profile.lastLogin)
        loadProfileImages(profile.profilePhotoUrl)
    }

    private fun bindProfileFields() {
        view?.findViewById<EditText>(R.id.editProfileFullName)?.setText(profile.name)
        view?.findViewById<EditText>(R.id.editProfileEmail)?.setText(profile.email)
        view?.findViewById<EditText>(R.id.editProfilePhone)?.setText(profile.phone)
        setGenderSelection(profile.gender)
        view?.findViewById<EditText>(R.id.editProfileDepartment)?.setText(profile.department)
        view?.findViewById<EditText>(R.id.editProfileAddress)?.setText(profile.address)
    }

    private fun setupTabs() {
        view?.findViewById<View>(R.id.tabProfileInfo)?.setOnClickListener {
            switchTab(ProfileTab.PROFILE)
        }
        view?.findViewById<View>(R.id.tabProfilePhoto)?.setOnClickListener {
            switchTab(ProfileTab.PHOTO)
        }
        view?.findViewById<View>(R.id.tabProfileSecurity)?.setOnClickListener {
            switchTab(ProfileTab.SECURITY)
        }
        view?.findViewById<View>(R.id.tabProfileDelete)?.setOnClickListener {
            switchTab(ProfileTab.DELETE)
        }
    }

    private fun switchTab(tab: ProfileTab) {
        if (selectedTab != tab && isEditMode) {
            exitEditMode(restoreValues = true)
        }

        selectedTab = tab
        view?.findViewById<View>(R.id.sectionProfileInfo)?.visibility =
            if (tab == ProfileTab.PROFILE) View.VISIBLE else View.GONE
        view?.findViewById<View>(R.id.sectionProfilePhoto)?.visibility =
            if (tab == ProfileTab.PHOTO) View.VISIBLE else View.GONE
        view?.findViewById<View>(R.id.sectionProfileSecurity)?.visibility =
            if (tab == ProfileTab.SECURITY) View.VISIBLE else View.GONE
        view?.findViewById<View>(R.id.sectionProfileDelete)?.visibility =
            if (tab == ProfileTab.DELETE) View.VISIBLE else View.GONE
        updateTabStyles()
    }

    private fun updateTabStyles() {
        val styles = listOf(
            ProfileTabStyle(R.id.tabProfileInfo, R.id.iconTabProfileInfo, R.id.textTabProfileInfo, ProfileTab.PROFILE),
            ProfileTabStyle(R.id.tabProfilePhoto, R.id.iconTabProfilePhoto, R.id.textTabProfilePhoto, ProfileTab.PHOTO),
            ProfileTabStyle(R.id.tabProfileSecurity, R.id.iconTabProfileSecurity, R.id.textTabProfileSecurity, ProfileTab.SECURITY),
            ProfileTabStyle(R.id.tabProfileDelete, R.id.iconTabProfileDelete, R.id.textTabProfileDelete, ProfileTab.DELETE)
        )

        styles.forEach { style ->
            val selected = selectedTab == style.tab
            view?.findViewById<View>(style.containerId)?.setBackgroundResource(
                if (selected) R.drawable.bg_profile_segment_active else 0
            )
            view?.findViewById<ImageView>(style.iconId)?.setColorFilter(
                ContextCompat.getColor(
                    requireContext(),
                    if (selected) R.color.white else R.color.profile_text_muted
                )
            )
            view?.findViewById<TextView>(style.textId)?.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (selected) R.color.white else R.color.profile_text_primary
                )
            )
        }
    }

    private fun setupProfileInfoSection() {
        setupGenderSpinner()
        view?.findViewById<View>(R.id.buttonProfileEdit)?.setOnClickListener { enterEditMode() }
        view?.findViewById<View>(R.id.buttonProfileCancel)?.setOnClickListener {
            exitEditMode(restoreValues = true)
        }
        view?.findViewById<View>(R.id.buttonProfileSave)?.setOnClickListener {
            saveDummyProfile()
        }
        exitEditMode(restoreValues = false)
    }

    private fun enterEditMode() {
        isEditMode = true
        view?.findViewById<TextView>(R.id.textProfileInfoStatus)?.apply {
            text = "EDITING"
            setBackgroundResource(R.drawable.bg_profile_editing_badge)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.profile_primary))
        }
        view?.findViewById<View>(R.id.buttonProfileEdit)?.visibility = View.GONE
        view?.findViewById<View>(R.id.profileEditActions)?.visibility = View.VISIBLE

        setFieldEditable(R.id.editProfileFullName, true)
        setFieldEditable(R.id.editProfilePhone, true)
        setFieldEditable(R.id.editProfileAddress, true)
        setFieldEditable(R.id.editProfileEmail, true)
        setFieldEditable(R.id.editProfileDepartment, false)
        setGenderEditable(true)
    }

    private fun exitEditMode(restoreValues: Boolean) {
        isEditMode = false
        if (restoreValues) bindProfileFields()

        view?.findViewById<TextView>(R.id.textProfileInfoStatus)?.apply {
            text = "READ ONLY"
            setBackgroundResource(R.drawable.bg_profile_status_badge)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.profile_text_muted))
        }
        view?.findViewById<View>(R.id.buttonProfileEdit)?.visibility = View.VISIBLE
        view?.findViewById<View>(R.id.profileEditActions)?.visibility = View.GONE

        setFieldEditable(R.id.editProfileFullName, false)
        setFieldEditable(R.id.editProfileEmail, false)
        setFieldEditable(R.id.editProfilePhone, false)
        setFieldEditable(R.id.editProfileDepartment, false)
        setFieldEditable(R.id.editProfileAddress, false)
        setGenderEditable(false)
    }

    private fun saveDummyProfile() {
        val fullName = view?.findViewById<EditText>(R.id.editProfileFullName)?.text?.toString()?.trim().orEmpty()
        val email = view?.findViewById<EditText>(R.id.editProfileEmail)?.text?.toString()?.trim().orEmpty()
        val phone = view?.findViewById<EditText>(R.id.editProfilePhone)?.text?.toString()?.trim().orEmpty()
        val gender = view?.findViewById<Spinner>(R.id.spinnerProfileGender)?.selectedItem?.toString().orEmpty()
        val address = view?.findViewById<EditText>(R.id.editProfileAddress)?.text?.toString()?.trim().orEmpty()

        val updatedProfile = profile.copy(
            name = fullName.ifBlank { profile.name },
            email = email.ifBlank { profile.email },
            phone = phone,
            gender = gender,
            address = address
        )
        exitEditMode(restoreValues = false)
        viewModel.updateProfile(updatedProfile)
    }

    private fun setupGenderSpinner() {
        val spinner = view?.findViewById<Spinner>(R.id.spinnerProfileGender) ?: return
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.item_profile_spinner_selected,
            genderOptions
        ).apply {
            setDropDownViewResource(R.layout.item_profile_spinner_dropdown)
        }
        spinner.adapter = adapter
        setGenderSelection(profile.gender)
        setGenderEditable(false)
    }

    private fun setGenderSelection(value: String) {
        val spinner = view?.findViewById<Spinner>(R.id.spinnerProfileGender) ?: return
        val normalizedValue = normalizeGenderForDisplay(value)
        val index = genderOptions.indexOfFirst { it.equals(normalizedValue, ignoreCase = true) }
            .takeIf { it >= 0 }
            ?: 0
        spinner.setSelection(index)
    }

    private fun normalizeGenderForDisplay(value: String): String {
        return when (value.trim().lowercase(Locale.US)) {
            "male" -> "Male"
            "female" -> "Female"
            "other", "others" -> "Others"
            else -> value
        }
    }

    private fun setGenderEditable(editable: Boolean) {
        view?.findViewById<Spinner>(R.id.spinnerProfileGender)?.apply {
            isEnabled = editable
            isClickable = editable
            alpha = if (editable) 1f else 0.8f
        }
    }

    private fun setupPhotoSection() {
        view?.findViewById<View>(R.id.buttonProfileCamera)?.setOnClickListener {
            switchTab(ProfileTab.PHOTO)
        }
        view?.findViewById<View>(R.id.buttonChoosePhoto)?.setOnClickListener {
            photoPicker.launch("image/*")
        }
        view?.findViewById<View>(R.id.buttonUploadPhoto)?.setOnClickListener {
            val file = selectedPhotoFile
            if (file == null) {
                showToast("Choose a photo first.")
                return@setOnClickListener
            }
            viewModel.uploadPhoto(file)
        }
        view?.findViewById<View>(R.id.buttonRemovePhoto)?.setOnClickListener {
            showRemovePhotoDialog()
        }
    }

    private fun handleSelectedPhoto(uri: Uri) {
        val file = copyUriToCache(uri)
        if (file == null) {
            showToast("Unable to read selected photo.")
            return
        }

        selectedPhotoFile = file
        view?.setProfileText(R.id.textSelectedPhotoFile, file.name)
        view?.findViewById<ImageView>(R.id.imagePhotoPreview)?.apply {
            setImageURI(uri)
            visibility = View.VISIBLE
        }
        view?.findViewById<TextView>(R.id.textPhotoPreviewInitials)?.visibility = View.GONE
    }

    private fun copyUriToCache(uri: Uri): File? {
        val resolver = requireContext().contentResolver
        val sourceName = resolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) cursor.getString(nameIndex) else null
        }
        val extension = sourceName?.substringAfterLast('.', missingDelimiterValue = "jpg") ?: "jpg"
        val file = File(requireContext().cacheDir, "profile_photo_${System.currentTimeMillis()}.$extension")

        return runCatching {
            resolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            file
        }.getOrNull()
    }

    private fun setupSecuritySection() {
        view?.findViewById<View>(R.id.iconToggleCurrentPassword)?.setOnClickListener {
            currentPasswordVisible = !currentPasswordVisible
            togglePasswordVisibility(R.id.editCurrentPassword, R.id.iconToggleCurrentPassword, currentPasswordVisible)
        }
        view?.findViewById<View>(R.id.iconToggleNewPassword)?.setOnClickListener {
            newPasswordVisible = !newPasswordVisible
            togglePasswordVisibility(R.id.editNewPassword, R.id.iconToggleNewPassword, newPasswordVisible)
        }
        view?.findViewById<View>(R.id.iconToggleConfirmPassword)?.setOnClickListener {
            confirmPasswordVisible = !confirmPasswordVisible
            togglePasswordVisibility(R.id.editConfirmPassword, R.id.iconToggleConfirmPassword, confirmPasswordVisible)
        }
        view?.findViewById<EditText>(R.id.editNewPassword)?.doAfterTextChanged {
            updatePasswordRequirements()
        }
        view?.findViewById<EditText>(R.id.editConfirmPassword)?.doAfterTextChanged {
            updatePasswordRequirements()
        }
        view?.findViewById<View>(R.id.buttonCancelPassword)?.setOnClickListener {
            clearPasswordForm()
        }
        view?.findViewById<View>(R.id.buttonSavePassword)?.setOnClickListener {
            if (validatePasswordForm()) {
                val currentPassword = view?.findViewById<EditText>(R.id.editCurrentPassword)?.text?.toString().orEmpty()
                val newPassword = view?.findViewById<EditText>(R.id.editNewPassword)?.text?.toString().orEmpty()
                val confirmPassword = view?.findViewById<EditText>(R.id.editConfirmPassword)?.text?.toString().orEmpty()
                viewModel.changePassword(currentPassword, newPassword, confirmPassword)
            }
        }
        updatePasswordRequirements()
    }

    private fun validatePasswordForm(): Boolean {
        val currentPassword = view?.findViewById<EditText>(R.id.editCurrentPassword)?.text?.toString().orEmpty()
        val newPassword = view?.findViewById<EditText>(R.id.editNewPassword)?.text?.toString().orEmpty()
        val confirmPassword = view?.findViewById<EditText>(R.id.editConfirmPassword)?.text?.toString().orEmpty()
        var valid = true

        if (currentPassword.isBlank()) {
            setPasswordError(R.id.containerCurrentPassword, R.id.errorCurrentPassword, "Current password is required")
            valid = false
        } else {
            clearPasswordError(R.id.containerCurrentPassword, R.id.errorCurrentPassword)
        }

        if (newPassword.length < 8) {
            setPasswordError(R.id.containerNewPassword, R.id.errorNewPassword, "New password must be at least 8 characters")
            valid = false
        } else {
            clearPasswordError(R.id.containerNewPassword, R.id.errorNewPassword)
        }

        if (confirmPassword != newPassword || confirmPassword.isBlank()) {
            setPasswordError(R.id.containerConfirmPassword, R.id.errorConfirmPassword, "Passwords must match")
            valid = false
        } else {
            clearPasswordError(R.id.containerConfirmPassword, R.id.errorConfirmPassword)
        }

        updatePasswordRequirements()
        return valid
    }

    private fun setupDeleteSection() {
        bindDeleteEligibility(currentDeleteEligibility)
        view?.findViewById<View>(R.id.buttonDeleteAccount)?.setOnClickListener {
            if (!currentDeleteEligibility.canDelete) {
                showToast("Account deletion is restricted.")
                return@setOnClickListener
            }
            showDeleteAccountDialog()
        }
    }

    private fun bindDeleteEligibility(eligibility: DeleteEligibilityUiModel) {
        currentDeleteEligibility = eligibility
        view?.setProfileText(R.id.textIssuedBooksRestriction, "${eligibility.issuedBooks} issued books")
        view?.setProfileText(R.id.textPendingFinesRestriction, "${eligibility.pendingFines} pending fines")
        view?.setProfileText(R.id.textActiveRequestsRestriction, "${eligibility.activeRequests} active requests")

        view?.findViewById<TextView>(R.id.textDeleteRestrictedBadge)?.apply {
            text = if (eligibility.canDelete) "Eligible" else "Action restricted"
            setBackgroundResource(
                if (eligibility.canDelete) R.drawable.bg_profile_editing_badge else R.drawable.bg_profile_restricted_badge
            )
            setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (eligibility.canDelete) R.color.profile_primary else R.color.profile_red_dark
                )
            )
        }

        view?.findViewById<TextView>(R.id.buttonDeleteAccount)?.apply {
            isEnabled = true
            setBackgroundResource(
                if (eligibility.canDelete) R.drawable.bg_profile_primary_button else R.drawable.bg_profile_disabled_button
            )
            alpha = if (eligibility.canDelete) 1f else 0.85f
        }
    }

    private fun showDeleteAccountDialog() {
        val input = EditText(requireContext()).apply {
            hint = "Type DELETE"
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            setPadding(32, 16, 32, 16)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Delete account?")
            .setMessage("This action cannot be undone. Type DELETE to confirm.")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete", null)
            .create()
            .apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        if (input.text?.toString()?.trim() == "DELETE") {
                            dismiss()
                            viewModel.deleteAccount()
                        } else {
                            input.error = "Type DELETE to confirm"
                        }
                    }
                }
            }
            .show()
    }

    private fun showRemovePhotoDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Remove profile photo?")
            .setMessage("This will show your initials until a new photo is uploaded.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Remove") { _, _ ->
                viewModel.removePhoto()
            }
            .show()
    }

    private fun getInitials(name: String): String {
        val parts = name.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        return parts
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
            .joinToString("")
            .ifBlank { "ST" }
    }

    private fun showToast(message: String) {
        LmsToast.show(requireContext(), message)
    }

    private fun loadProfileImages(rawUrl: String?) {
        val summaryImage = view?.findViewById<ImageView>(R.id.imageProfileAvatar)
        val summaryInitials = view?.findViewById<TextView>(R.id.textProfileInitials)
        val previewImage = view?.findViewById<ImageView>(R.id.imagePhotoPreview)
        val previewInitials = view?.findViewById<TextView>(R.id.textPhotoPreviewInitials)
        val photoUrl = normalizeProfilePhotoUrl(rawUrl)

        summaryImage?.tag = photoUrl
        previewImage?.tag = photoUrl

        if (photoUrl.isNullOrBlank()) {
            summaryImage?.visibility = View.GONE
            previewImage?.visibility = View.GONE
            summaryInitials?.visibility = View.VISIBLE
            previewInitials?.visibility = View.VISIBLE
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                runCatching {
                    URL(photoUrl).openStream().use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }.onFailure {
                    Log.w("StudentProfile", "Profile photo failed to load: $photoUrl", it)
                }.getOrNull()
            }

            if (bitmap == null) {
                summaryImage?.visibility = View.GONE
                previewImage?.visibility = View.GONE
                summaryInitials?.visibility = View.VISIBLE
                previewInitials?.visibility = View.VISIBLE
                return@launch
            }

            if (summaryImage?.tag == photoUrl) {
                summaryImage.setImageBitmap(bitmap)
                summaryImage.visibility = View.VISIBLE
                summaryInitials?.visibility = View.GONE
            }

            if (previewImage?.tag == photoUrl) {
                previewImage.setImageBitmap(bitmap)
                previewImage.visibility = View.VISIBLE
                previewInitials?.visibility = View.GONE
            }
        }
    }

    private fun normalizeProfilePhotoUrl(rawUrl: String?): String? {
        val value = rawUrl?.trim()?.takeIf { it.isNotBlank() } ?: return null
        if (!value.startsWith("http", ignoreCase = true)) {
            return Constants.BASE_URL.removeSuffix("api/") + value.trimStart('/')
        }

        val apiRoot = Constants.BASE_URL.removeSuffix("api/").trimEnd('/')
        return value
            .replace("http://127.0.0.1:8000", apiRoot)
            .replace("http://localhost:8000", apiRoot)
            .replace("https://127.0.0.1:8000", apiRoot)
            .replace("https://localhost:8000", apiRoot)
    }

    private fun navigateToLogin() {
        if (!isAdded) return
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }

    private fun setFieldEditable(fieldId: Int, editable: Boolean) {
        view?.findViewById<EditText>(fieldId)?.apply {
            isFocusable = editable
            isFocusableInTouchMode = editable
            isCursorVisible = editable
            isLongClickable = editable
            isEnabled = true
            if (!editable) clearFocus()
        }
    }

    private fun togglePasswordVisibility(fieldId: Int, iconId: Int, visible: Boolean) {
        val field = view?.findViewById<EditText>(fieldId) ?: return
        field.inputType = if (visible) {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        field.setSelection(field.text?.length ?: 0)
        view?.findViewById<ImageView>(iconId)?.setImageResource(
            if (visible) R.drawable.ic_eye_off else R.drawable.ic_eye
        )
    }

    private fun updatePasswordRequirements() {
        val newPassword = view?.findViewById<EditText>(R.id.editNewPassword)?.text?.toString().orEmpty()
        val confirmPassword = view?.findViewById<EditText>(R.id.editConfirmPassword)?.text?.toString().orEmpty()
        updateRequirement(R.id.textReqLength, "At least 8 characters", newPassword.length >= 8)
        updateRequirement(R.id.textReqUppercase, "At least one uppercase letter", newPassword.any { it.isUpperCase() })
        updateRequirement(R.id.textReqLowercase, "At least one lowercase letter", newPassword.any { it.isLowerCase() })
        updateRequirement(R.id.textReqNumber, "At least one number", newPassword.any { it.isDigit() })
        updateRequirement(
            R.id.textReqMatch,
            "Passwords must match",
            newPassword.isNotBlank() && newPassword == confirmPassword
        )
    }

    private fun updateRequirement(textId: Int, label: String, met: Boolean) {
        view?.findViewById<TextView>(textId)?.apply {
            text = "${if (met) "OK" else "--"} $label"
            setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (met) R.color.profile_green else R.color.profile_text_muted
                )
            )
        }
    }

    private fun setPasswordError(containerId: Int, errorId: Int, message: String) {
        view?.findViewById<LinearLayout>(containerId)?.setBackgroundResource(R.drawable.bg_profile_input_error)
        view?.findViewById<TextView>(errorId)?.apply {
            text = message
            visibility = View.VISIBLE
        }
    }

    private fun clearPasswordError(containerId: Int, errorId: Int) {
        view?.findViewById<LinearLayout>(containerId)?.setBackgroundResource(R.drawable.bg_profile_input)
        view?.findViewById<TextView>(errorId)?.visibility = View.GONE
    }

    private fun clearPasswordForm() {
        view?.findViewById<EditText>(R.id.editCurrentPassword)?.text = null
        view?.findViewById<EditText>(R.id.editNewPassword)?.text = null
        view?.findViewById<EditText>(R.id.editConfirmPassword)?.text = null
        clearPasswordError(R.id.containerCurrentPassword, R.id.errorCurrentPassword)
        clearPasswordError(R.id.containerNewPassword, R.id.errorNewPassword)
        clearPasswordError(R.id.containerConfirmPassword, R.id.errorConfirmPassword)
        updatePasswordRequirements()
    }

    private fun View.setProfileText(id: Int, value: String) {
        findViewById<TextView>(id)?.text = value
    }

    private data class ProfileTabStyle(
        val containerId: Int,
        val iconId: Int,
        val textId: Int,
        val tab: ProfileTab
    )
}

