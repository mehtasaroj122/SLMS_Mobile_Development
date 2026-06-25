package com.saroj.lmsmobile.ui.staff.fragments

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.saroj.lmsmobile.MainApplication
import com.saroj.lmsmobile.R
import com.saroj.lmsmobile.api.RetrofitClient
import com.saroj.lmsmobile.data.models.common.NetworkResult
import com.saroj.lmsmobile.data.models.staffdashboard.DueTodayItem
import com.saroj.lmsmobile.data.models.staffdashboard.OverdueBookItem
import com.saroj.lmsmobile.data.models.staffdashboard.PendingRequestItem
import com.saroj.lmsmobile.data.models.staffdashboard.RecentIssueItem
import com.saroj.lmsmobile.data.models.staffdashboard.StaffDashboardData
import com.saroj.lmsmobile.data.repository.NotificationRepository
import com.saroj.lmsmobile.data.repository.StaffDashboardRepository
import com.saroj.lmsmobile.data.repository.StaffProfileRepository
import com.saroj.lmsmobile.ui.common.UnauthorizedActivity
import com.saroj.lmsmobile.ui.staff.StaffDashboardActivity
import com.saroj.lmsmobile.ui.staff.model.StaffProfileUiModel
import com.saroj.lmsmobile.ui.staff.viewmodel.StaffDashboardViewModel
import com.saroj.lmsmobile.utils.Constants
import com.saroj.lmsmobile.utils.LmsToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class StaffDashboardScreen : Fragment() {
    private lateinit var viewModel: StaffDashboardViewModel
    private lateinit var notificationRepository: NotificationRepository
    private var hasDashboardData = false
    private var refreshToastPending = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_staff_dashboard, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupViewModel()
        setupSwipeRefresh(view)
        setupQuickActions(view)
        observeDashboard(view)
        viewModel.loadDashboard()
        loadNotificationCount()
    }

    override fun onResume() {
        super.onResume()
        loadNotificationCount()
    }

    private fun setupViewModel() {
        val tokenManager = (requireActivity().application as MainApplication).tokenManager
        val apiService = RetrofitClient.getApiService(tokenManager)
        val repository = StaffDashboardRepository(apiService, tokenManager)
        notificationRepository = NotificationRepository(apiService, tokenManager)

        viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return StaffDashboardViewModel(repository) as T
                }
            }
        )[StaffDashboardViewModel::class.java]
    }

    private fun setupSwipeRefresh(view: View) {
        view.findViewById<SwipeRefreshLayout>(R.id.staffDashboardSwipeRefresh)?.apply {
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

    private fun setupQuickActions(view: View) {
        val host = activity as? StaffDashboardActivity
        view.findViewById<View>(R.id.actionIssueBook)?.setOnClickListener {
            host?.openIssueBook()
        }
        view.findViewById<View>(R.id.actionReturnBook)?.setOnClickListener {
            host?.openReturnBook()
        }
        view.findViewById<View>(R.id.actionReviewRequests)?.setOnClickListener {
            host?.openBookRequests()
        }
        view.findViewById<View>(R.id.actionManageFines)?.setOnClickListener {
            host?.openFines()
        }
        view.findViewById<View>(R.id.actionSearchStudent)?.setOnClickListener {
            host?.openStudents()
        }
        view.findViewById<View>(R.id.buttonHeaderNotifications)?.setOnClickListener {
            host?.openNotifications()
        }
    }

    private fun observeDashboard(view: View) {
        viewModel.dashboardData.observe(viewLifecycleOwner) { dashboard ->
            hasDashboardData = true
            view.findViewById<View>(R.id.staffDashboardErrorCard)?.visibility = View.GONE
            bindDashboard(view, dashboard)
            if (refreshToastPending) {
                refreshToastPending = false
                LmsToast.show(requireContext(), "Dashboard refreshed")
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            view.findViewById<ProgressBar>(R.id.staffDashboardLoading)?.visibility =
                if (isLoading) View.VISIBLE else View.GONE
            view.findViewById<SwipeRefreshLayout>(R.id.staffDashboardSwipeRefresh)?.isRefreshing =
                isLoading && refreshToastPending
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrBlank()) {
                refreshToastPending = false
                view.findViewById<SwipeRefreshLayout>(R.id.staffDashboardSwipeRefresh)?.isRefreshing = false
                if (hasDashboardData) {
                    LmsToast.show(requireContext(), message.take(120))
                } else {
                    view.findViewById<View>(R.id.staffDashboardErrorCard)?.visibility = View.VISIBLE
                    view.findViewById<TextView>(R.id.textStaffDashboardError)?.text =
                        "Unable to load dashboard: ${message.take(100)}"
                }
            }
        }

        viewModel.actionMessage.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrBlank()) {
                LmsToast.show(requireContext(), message)
                viewModel.clearActionMessage()
            }
        }

        viewModel.unauthorized.observe(viewLifecycleOwner) { unauthorized ->
            if (unauthorized) {
                navigateToUnauthorized()
            }
        }

        view.findViewById<View>(R.id.buttonStaffDashboardRetry)?.setOnClickListener {
            view.findViewById<View>(R.id.staffDashboardErrorCard)?.visibility = View.GONE
            viewModel.refreshDashboard()
        }
    }

    private fun bindDashboard(view: View, dashboard: StaffDashboardData) {
        val staffName = dashboard.staff?.name.orFallback("Staff")
        view.setText(R.id.textStaffGreeting, greetingForCurrentTime())
        view.setText(R.id.textStaffName, staffName)
        view.setText(R.id.textStaffInitials, getInitials(staffName))
        view.setText(R.id.textStaffId, dashboard.staff?.staffId.orFallback("Staff"))
        view.setText(R.id.textStaffDepartment, dashboard.staff?.department.orFallback("Department not assigned"))
        view.setText(R.id.textStaffEmail, dashboard.staff?.email.orFallback("Email not available"))
        loadProfilePhoto(view, dashboard.profilePhotoUrl ?: dashboard.profilePhoto)

        bindStat(
            view.findViewById(R.id.cardIssuedBooks),
            value = (dashboard.summary?.currentlyIssued ?: dashboard.issuedBooks ?: 0).toString(),
            title = "ISSUED BOOKS",
            icon = R.drawable.ic_book,
            color = R.color.student_primary,
            background = R.drawable.bg_stat_icon_blue
        )
        bindStat(
            view.findViewById(R.id.cardPendingRequests),
            value = (dashboard.summary?.pendingRequests ?: dashboard.pendingRequestsCount ?: 0).toString(),
            title = "PENDING REQUESTS",
            icon = R.drawable.ic_request,
            color = R.color.student_purple,
            background = R.drawable.bg_stat_icon_purple
        )
        bindStat(
            view.findViewById(R.id.cardPendingFines),
            value = formatCurrency(dashboard.summary?.pendingFinesAmount ?: dashboard.pendingFines ?: 0.0),
            title = "PENDING FINES",
            icon = R.drawable.ic_rupee,
            color = R.color.student_yellow,
            background = R.drawable.bg_stat_icon_yellow
        )
        bindStat(
            view.findViewById(R.id.cardOverdueBooks),
            value = (dashboard.summary?.overdue ?: dashboard.overdueBooksCount ?: 0).toString(),
            title = "OVERDUE BOOKS",
            icon = R.drawable.ic_clock,
            color = R.color.student_danger,
            background = R.drawable.bg_stat_icon_red
        )

        bindQuickAction(view.findViewById(R.id.actionIssueBook), "Issue Book", R.drawable.ic_issue, R.color.student_primary, R.drawable.bg_action_search)
        bindQuickAction(view.findViewById(R.id.actionReturnBook), "Return Book", R.drawable.ic_refresh, R.color.student_green, R.drawable.bg_action_books)
        bindQuickAction(view.findViewById(R.id.actionReviewRequests), "Review Requests", R.drawable.ic_clipboard, R.color.student_purple, R.drawable.bg_action_requests)
        bindQuickAction(view.findViewById(R.id.actionManageFines), "Manage Fines", R.drawable.ic_rupee, R.color.student_danger, R.drawable.bg_action_fines)
        bindQuickAction(view.findViewById(R.id.actionSearchStudent), "Search Student", R.drawable.ic_student, R.color.student_text_muted, R.drawable.bg_action_profile)

        bindPendingRequests(view.findViewById(R.id.sectionPendingRequests), dashboard.pendingRequests.orEmpty())
        bindDueToday(view.findViewById(R.id.sectionDueToday), dashboard.dueToday.orEmpty())
        bindOverdueBooks(view.findViewById(R.id.sectionOverdueBooks), dashboard.overdueBooks.orEmpty())
    }

    private fun bindStat(root: View?, value: String, title: String, icon: Int, color: Int, background: Int) {
        root ?: return
        val tint = ContextCompat.getColor(requireContext(), color)
        root.findViewById<View>(R.id.containerStatIcon)?.setBackgroundResource(background)
        root.findViewById<ImageView>(R.id.imageStatIcon)?.apply {
            setImageResource(icon)
            setColorFilter(tint)
        }
        root.findViewById<TextView>(R.id.textStatValue)?.text = value
        root.findViewById<TextView>(R.id.textStatTitle)?.text = title
    }

    private fun bindQuickAction(root: View?, label: String, icon: Int, color: Int, background: Int) {
        root ?: return
        root.setBackgroundResource(background)
        root.findViewById<ImageView>(R.id.imageActionIcon)?.apply {
            setImageResource(icon)
            setColorFilter(ContextCompat.getColor(requireContext(), color))
        }
        root.findViewById<TextView>(R.id.textActionTitle)?.text = label
    }

    private fun bindMiniStat(root: View?, value: String, label: String) {
        root ?: return
        root.findViewById<TextView>(R.id.textMiniValue)?.text = value
        root.findViewById<TextView>(R.id.textMiniLabel)?.text = label
    }

    private fun loadNotificationCount() {
        if (!::notificationRepository.isInitialized) return
        lifecycleScope.launch {
            notificationRepository.getNotificationCount().collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        val unreadCount = result.data.unreadCount ?: 0
                        view?.let { updateNotificationBadge(it, unreadCount) }
                    }
                    is NetworkResult.Unauthorized -> navigateToUnauthorized()
                    else -> Unit
                }
            }
        }
    }

    private fun updateNotificationBadge(view: View, unreadCount: Int) {
        view.findViewById<TextView>(R.id.textHeaderNotificationBadge)?.apply {
            text = if (unreadCount > 99) "99+" else unreadCount.toString()
            visibility = if (unreadCount > 0) View.VISIBLE else View.GONE
        }
    }

    private fun bindPendingRequests(section: View?, requests: List<PendingRequestItem>) {
        section ?: return
        section.findViewById<TextView>(R.id.textSectionTitle)?.text = "Latest Pending Requests"
        bindSectionIcon(section, R.drawable.ic_clipboard, R.color.student_purple)
        section.findViewById<TextView>(R.id.textSectionEmpty)?.text = "No pending requests"
        section.findViewById<TextView>(R.id.textSectionEmptySubtitle)?.text = "New book requests will appear here."
        bindEmptyIcon(section, R.drawable.ic_request, R.color.student_purple)
        section.findViewById<View>(R.id.buttonSectionViewAll)?.apply {
            visibility = View.VISIBLE
            setOnClickListener { (activity as? StaffDashboardActivity)?.openBookRequests() }
        }
        bindRows(section, requests.take(3).map { request ->
            DashboardRow(
                title = request.bookTitle.orFallback("Untitled Book"),
                meta = "${request.studentName.orFallback("Unknown Student")} | ${request.symbolNo ?: request.studentId ?: "-"}",
                date = "Requested: ${formatDate(request.requestDate)}",
                badge = request.status.orFallback("Pending").titleCase(),
                badgeColor = R.color.student_yellow,
                badgeBackground = R.drawable.bg_badge_pending,
                showActions = true,
                onPrimary = { request.id?.let(viewModel::approveRequest) },
                onSecondary = { request.id?.let(viewModel::rejectRequest) }
            )
        })
    }

    private fun bindDueToday(section: View?, books: List<DueTodayItem>) {
        section ?: return
        section.findViewById<TextView>(R.id.textSectionTitle)?.text = "Due Today Books"
        bindSectionIcon(section, R.drawable.ic_calendar, R.color.student_green)
        section.findViewById<TextView>(R.id.textSectionEmpty)?.text = "No books due today"
        section.findViewById<TextView>(R.id.textSectionEmptySubtitle)?.text = "All clear for today."
        bindEmptyIcon(section, R.drawable.ic_check_circle, R.color.student_green)
        section.findViewById<View>(R.id.buttonSectionViewAll)?.visibility = View.GONE
        bindRows(section, books.take(3).map { item ->
            DashboardRow(
                title = item.bookTitle.orFallback("Untitled Book"),
                meta = item.studentName.orFallback("Unknown Student"),
                date = "Due: ${formatDate(item.dueDate)}",
                badge = "Due Today",
                badgeColor = R.color.student_green,
                badgeBackground = R.drawable.bg_badge_due_soon,
                showReturn = true,
                onReturn = { (activity as? StaffDashboardActivity)?.openReturnBook(item.issueId) }
            )
        })
    }

    private fun bindOverdueBooks(section: View?, books: List<OverdueBookItem>) {
        section ?: return
        section.findViewById<TextView>(R.id.textSectionTitle)?.text = "Top Overdue Books"
        bindSectionIcon(section, R.drawable.ic_clock, R.color.student_danger)
        section.findViewById<TextView>(R.id.textSectionEmpty)?.text = "No overdue books"
        section.findViewById<TextView>(R.id.textSectionEmptySubtitle)?.text = "Everything is on time."
        bindEmptyIcon(section, R.drawable.ic_check_circle, R.color.student_green)
        section.findViewById<View>(R.id.buttonSectionViewAll)?.apply {
            visibility = View.VISIBLE
            setOnClickListener { (activity as? StaffDashboardActivity)?.openFines() }
        }
        bindRows(section, books.take(3).map { item ->
            DashboardRow(
                title = item.bookTitle.orFallback("Untitled Book"),
                meta = item.studentName.orFallback("Unknown Student"),
                date = "Due: ${formatDate(item.dueDate)}",
                badge = "${item.daysOverdue ?: 0} days overdue",
                badgeColor = R.color.student_danger,
                badgeBackground = R.drawable.bg_badge_overdue
            )
        })
    }

    private fun bindRows(section: View, rows: List<DashboardRow>) {
        section.findViewById<View>(R.id.emptyState)?.visibility =
            if (rows.isEmpty()) View.VISIBLE else View.GONE

        val cardIds = listOf(R.id.itemCard1, R.id.itemCard2, R.id.itemCard3)
        val titleIds = listOf(R.id.textItemTitle1, R.id.textItemTitle2, R.id.textItemTitle3)
        val metaIds = listOf(R.id.textItemMeta1, R.id.textItemMeta2, R.id.textItemMeta3)
        val dateIds = listOf(R.id.textItemDate1, R.id.textItemDate2, R.id.textItemDate3)
        val badgeIds = listOf(R.id.textItemBadge1, R.id.textItemBadge2, R.id.textItemBadge3)
        val actionRowIds = listOf(R.id.actionRow1, R.id.actionRow2, R.id.actionRow3)
        val primaryIds = listOf(R.id.buttonPrimary1, R.id.buttonPrimary2, R.id.buttonPrimary3)
        val secondaryIds = listOf(R.id.buttonSecondary1, R.id.buttonSecondary2, R.id.buttonSecondary3)
        val returnIds = listOf(R.id.buttonReturn1, R.id.buttonReturn2, R.id.buttonReturn3)

        cardIds.forEachIndexed { index, cardId ->
            val row = rows.getOrNull(index)
            section.findViewById<View>(cardId)?.visibility = if (row == null) View.GONE else View.VISIBLE
            if (row != null) {
                section.findViewById<TextView>(titleIds[index])?.text = row.title
                section.findViewById<TextView>(metaIds[index])?.text = row.meta
                section.findViewById<TextView>(dateIds[index])?.text = row.date
                section.findViewById<TextView>(badgeIds[index])?.apply {
                    text = row.badge
                    setBackgroundResource(row.badgeBackground)
                    setTextColor(ContextCompat.getColor(requireContext(), row.badgeColor))
                    visibility = View.VISIBLE
                }
                section.findViewById<View>(actionRowIds[index])?.visibility =
                    if (row.showActions) View.VISIBLE else View.GONE
                section.findViewById<View>(returnIds[index])?.visibility =
                    if (row.showReturn) View.VISIBLE else View.GONE
                section.findViewById<View>(primaryIds[index])?.setOnClickListener { row.onPrimary?.invoke() }
                section.findViewById<View>(secondaryIds[index])?.setOnClickListener { row.onSecondary?.invoke() }
                section.findViewById<View>(returnIds[index])?.setOnClickListener { row.onReturn?.invoke() }
            }
        }
    }

    private fun bindSectionIcon(section: View, icon: Int, color: Int) {
        section.findViewById<ImageView>(R.id.imageSectionIcon)?.apply {
            setImageResource(icon)
            setColorFilter(ContextCompat.getColor(requireContext(), color))
        }
        section.findViewById<TextView>(R.id.buttonSectionViewAll)?.setTextColor(
            ContextCompat.getColor(requireContext(), color)
        )
    }

    private fun bindEmptyIcon(section: View, icon: Int, color: Int) {
        section.findViewById<ImageView>(R.id.imageEmptyIcon)?.apply {
            setImageResource(icon)
            setColorFilter(ContextCompat.getColor(requireContext(), color))
        }
    }

    private fun greetingForCurrentTime(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    private fun formatCurrency(amount: Double): String = "Rs. ${String.format(Locale.US, "%.2f", amount)}"

    private fun formatAverage(value: Double?): String {
        val average = value ?: 0.0
        return if (average % 1.0 == 0.0) average.toInt().toString() else String.format(Locale.US, "%.1f", average)
    }

    private fun formatDate(raw: String?): String {
        if (raw.isNullOrBlank()) return "-"
        val patterns = listOf("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd")
        for (pattern in patterns) {
            val parsed = runCatching {
                SimpleDateFormat(pattern, Locale.US).parse(raw)
            }.getOrNull()
            if (parsed != null) {
                return SimpleDateFormat("MMM dd, yyyy", Locale.US).format(parsed)
            }
        }
        return raw.take(16)
    }

    private fun loadProfilePhoto(view: View, rawUrl: String?) {
        val image = view.findViewById<ImageView>(R.id.imageStaffProfile) ?: return
        val initials = view.findViewById<TextView>(R.id.textStaffInitials)
        val photoUrl = normalizeProfilePhotoUrl(rawUrl)

        image.tag = photoUrl
        if (photoUrl.isNullOrBlank()) {
            image.visibility = View.GONE
            initials?.visibility = View.VISIBLE
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                runCatching {
                    URL(photoUrl).openStream().use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }.onFailure {
                    Log.w("StaffDashboard", "Profile photo failed to load: $photoUrl", it)
                }.getOrNull()
            }

            if (bitmap != null && image.tag == photoUrl) {
                image.setImageBitmap(bitmap)
                image.visibility = View.VISIBLE
                initials?.visibility = View.GONE
            } else {
                image.visibility = View.GONE
                initials?.visibility = View.VISIBLE
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

    private fun getInitials(name: String): String {
        return name.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
            .joinToString("")
            .ifBlank { "ST" }
    }

    private fun navigateToUnauthorized() {
        if (!isAdded) return
        val intent = Intent(requireContext(), UnauthorizedActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }

    private fun View.setText(id: Int, value: String) {
        findViewById<TextView>(id)?.text = value
    }

    private fun String?.orFallback(fallback: String): String =
        if (isNullOrBlank()) fallback else this

    private fun String.titleCase(): String =
        replace("_", " ").split(" ").filter { it.isNotBlank() }.joinToString(" ") {
            it.lowercase(Locale.US).replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.US) else char.toString()
            }
        }

    private data class DashboardRow(
        val title: String,
        val meta: String,
        val date: String,
        val badge: String,
        val badgeColor: Int,
        val badgeBackground: Int,
        val showActions: Boolean = false,
        val showReturn: Boolean = false,
        val onPrimary: (() -> Unit)? = null,
        val onSecondary: (() -> Unit)? = null,
        val onReturn: (() -> Unit)? = null
    )
}

class StaffMoreScreen : Fragment() {
    private var profilePhotoTag: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_staff_more, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bindCachedProfile(view)
        loadProfile(view)
        loadNotificationBadge(view)

        val host = activity as? StaffDashboardActivity
        view.findViewById<View>(R.id.rowBookRequests)?.setOnClickListener {
            host?.openBookRequests()
        }
        view.findViewById<View>(R.id.rowStudents)?.setOnClickListener {
            host?.openStudents()
        }
        view.findViewById<View>(R.id.rowProfile)?.setOnClickListener {
            host?.openProfile()
        }
        view.findViewById<View>(R.id.rowNotifications)?.setOnClickListener {
            host?.openNotifications()
        }
        view.findViewById<View>(R.id.rowLogout)?.setOnClickListener {
            host?.showLogoutConfirmation()
        }
    }

    override fun onResume() {
        super.onResume()
        view?.let {
            loadProfile(it)
            loadNotificationBadge(it)
        }
    }

    private fun bindCachedProfile(view: View) {
        val tokenManager = (requireActivity().application as MainApplication).tokenManager
        viewLifecycleOwner.lifecycleScope.launch {
            val name = tokenManager.getUserName().firstOrNull().orFallback("Staff Member")
            val email = tokenManager.getUserEmail().firstOrNull().orFallback("Email not available")
            val role = tokenManager.getUserRole().firstOrNull().orFallback("Staff").titleCase()
            bindProfile(
                view,
                StaffProfileUiModel(
                    name = name,
                    email = email,
                    role = role,
                    username = "-",
                    staffId = "-",
                    department = "-",
                    designation = "-",
                    memberSince = "-",
                    lastLogin = "-",
                    phone = "",
                    address = "",
                    profilePhotoUrl = null
                )
            )
        }
    }

    private fun loadProfile(view: View) {
        val tokenManager = (requireActivity().application as MainApplication).tokenManager
        val apiService = RetrofitClient.getApiService(tokenManager)
        val repository = StaffProfileRepository(apiService, tokenManager)
        viewLifecycleOwner.lifecycleScope.launch {
            repository.getProfile().collect { result ->
                when (result) {
                    is NetworkResult.Success -> bindProfile(view, result.data)
                    is NetworkResult.Unauthorized -> navigateToUnauthorized()
                    else -> Unit
                }
            }
        }
    }

    private fun loadNotificationBadge(view: View) {
        val badge = view.findViewById<TextView>(R.id.textMoreNotificationsBadge) ?: return
        badge.visibility = View.GONE

        val tokenManager = (requireActivity().application as MainApplication).tokenManager
        val apiService = RetrofitClient.getApiService(tokenManager)
        val repository = NotificationRepository(apiService, tokenManager)
        viewLifecycleOwner.lifecycleScope.launch {
            repository.getNotificationCount().collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        val unreadCount = result.data.unreadCount ?: 0
                        badge.text = if (unreadCount > 99) "99+" else unreadCount.toString()
                        badge.visibility = if (unreadCount > 0) View.VISIBLE else View.GONE
                    }
                    is NetworkResult.Unauthorized -> navigateToUnauthorized()
                    else -> badge.visibility = View.GONE
                }
            }
        }
    }

    private fun bindProfile(view: View, profile: StaffProfileUiModel) {
        val name = profile.name.orFallback("Staff Member")
        view.findViewById<TextView>(R.id.textStaffMoreName)?.text = name
        view.findViewById<TextView>(R.id.textStaffMoreEmail)?.text = profile.email.orFallback("Email not available")
        view.findViewById<TextView>(R.id.textStaffMoreInitials)?.text = getInitials(name)
        loadMoreProfilePhoto(view, profile.profilePhotoUrl)
    }

    private fun loadMoreProfilePhoto(view: View, rawUrl: String?) {
        val image = view.findViewById<ImageView>(R.id.imageStaffMoreProfile) ?: return
        val initials = view.findViewById<TextView>(R.id.textStaffMoreInitials)
        val photoUrl = normalizeProfilePhotoUrl(rawUrl)

        profilePhotoTag = photoUrl
        image.tag = photoUrl
        if (photoUrl.isNullOrBlank()) {
            image.visibility = View.GONE
            initials?.visibility = View.VISIBLE
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                runCatching {
                    URL(photoUrl).openStream().use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }.onFailure {
                    Log.w("StaffMore", "Profile photo failed to load: $photoUrl", it)
                }.getOrNull()
            }

            if (bitmap != null && image.tag == photoUrl && profilePhotoTag == photoUrl) {
                image.setImageBitmap(bitmap)
                image.visibility = View.VISIBLE
                initials?.visibility = View.GONE
            } else {
                image.visibility = View.GONE
                initials?.visibility = View.VISIBLE
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

    private fun getInitials(name: String): String {
        return name.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
            .joinToString("")
            .ifBlank { "ST" }
    }

    private fun navigateToUnauthorized() {
        if (!isAdded) return
        val intent = Intent(requireContext(), UnauthorizedActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }

    private fun String?.orFallback(fallback: String): String =
        if (isNullOrBlank() || this == "-") fallback else this

    private fun String.titleCase(): String =
        replace("_", " ").split(" ").filter { it.isNotBlank() }.joinToString(" ") {
            it.lowercase(Locale.US).replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.US) else char.toString()
            }
        }
}

