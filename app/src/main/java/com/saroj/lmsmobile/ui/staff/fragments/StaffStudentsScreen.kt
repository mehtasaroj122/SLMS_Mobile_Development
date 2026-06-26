package com.saroj.lmsmobile.ui.staff.fragments

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.saroj.lmsmobile.MainApplication
import com.saroj.lmsmobile.R
import com.saroj.lmsmobile.api.RetrofitClient
import com.saroj.lmsmobile.data.models.staffstudents.StaffStudentItem
import com.saroj.lmsmobile.data.models.staffstudents.StudentBookRequestItem
import com.saroj.lmsmobile.data.models.staffstudents.StudentFineItem
import com.saroj.lmsmobile.data.models.staffstudents.StudentIssuedBookItem
import com.saroj.lmsmobile.data.repository.StaffStudentsRepository
import com.saroj.lmsmobile.ui.common.UnauthorizedActivity
import com.saroj.lmsmobile.ui.staff.StaffDashboardActivity
import com.saroj.lmsmobile.ui.staff.viewmodel.StaffStudentDetailTab
import com.saroj.lmsmobile.ui.staff.viewmodel.StaffStudentDetailViewModel
import com.saroj.lmsmobile.ui.staff.viewmodel.StaffStudentsStatus
import com.saroj.lmsmobile.ui.staff.viewmodel.StaffStudentsViewModel
import com.saroj.lmsmobile.utils.Constants
import com.saroj.lmsmobile.utils.LmsToast
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.concurrent.thread

class StaffStudentsScreen : Fragment() {
    private lateinit var viewModel: StaffStudentsViewModel
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var studentsContainer: LinearLayout
    private lateinit var searchInput: EditText
    private lateinit var clearSearchButton: ImageView
    private lateinit var topProgress: ProgressBar
    private lateinit var searchProgress: ProgressBar
    private var initialLoadStarted = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_staff_students, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupViewModel()
        bindViews(view)
        setupRefresh()
        setupSearch()
        setupTabs(view)
        setupObservers(view)
        view.findViewById<View>(R.id.buttonStaffStudentsBack)?.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        viewModel.loadInitial()
        initialLoadStarted = true
    }

    override fun onResume() {
        super.onResume()
        if (initialLoadStarted) viewModel.refresh()
    }

    private fun setupViewModel() {
        val tokenManager = (requireActivity().application as MainApplication).tokenManager
        val apiService = RetrofitClient.getApiService(tokenManager)
        val repository = StaffStudentsRepository(apiService, tokenManager)
        viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return StaffStudentsViewModel(repository) as T
                }
            }
        )[StaffStudentsViewModel::class.java]
    }

    private fun bindViews(view: View) {
        swipeRefresh = view.findViewById(R.id.staffStudentsSwipeRefresh)
        studentsContainer = view.findViewById(R.id.containerStaffStudents)
        searchInput = view.findViewById(R.id.inputStaffStudentsSearch)
        clearSearchButton = view.findViewById(R.id.buttonClearStaffStudentsSearch)
        topProgress = view.findViewById(R.id.progressStaffStudentsTop)
        searchProgress = view.findViewById(R.id.progressStaffStudentsSearch)
    }

    private fun setupRefresh() {
        swipeRefresh.setColorSchemeResources(
            R.color.student_primary,
            R.color.student_purple,
            R.color.student_green,
            R.color.student_yellow
        )
        swipeRefresh.setOnRefreshListener { viewModel.refresh() }
    }

    private fun setupSearch() {
        clearSearchButton.setOnClickListener { searchInput.setText("") }
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                clearSearchButton.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
                viewModel.searchStudents(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                viewModel.searchStudents(searchInput.text?.toString().orEmpty())
                true
            } else {
                false
            }
        }
    }

    private fun setupTabs(view: View) {
        view.findViewById<View>(R.id.tabStaffStudentsAll)?.setOnClickListener {
            viewModel.changeStatusChip(StaffStudentsStatus.ALL)
        }
        view.findViewById<View>(R.id.tabStaffStudentsActive)?.setOnClickListener {
            viewModel.changeStatusChip(StaffStudentsStatus.ACTIVE)
        }
        view.findViewById<View>(R.id.tabStaffStudentsInactive)?.setOnClickListener {
            viewModel.changeStatusChip(StaffStudentsStatus.INACTIVE)
        }
    }

    private fun setupObservers(view: View) {
        viewModel.students.observe(viewLifecycleOwner) { renderStudents(it) }
        viewModel.selectedStatusChip.observe(viewLifecycleOwner) { updateStatusTabs(view, it) }
        viewModel.isLoading.observe(viewLifecycleOwner) { updateLoading() }
        viewModel.isSearching.observe(viewLifecycleOwner) { updateLoading() }
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrBlank()) {
                renderError(message)
                LmsToast.show(requireContext(), message.take(140))
                viewModel.clearError()
            }
        }
        viewModel.unauthorized.observe(viewLifecycleOwner) { if (it) navigateToUnauthorized() }
    }

    private fun updateLoading() {
        val isLoading = viewModel.isLoading.value == true
        val isSearching = viewModel.isSearching.value == true
        topProgress.visibility = if (isLoading && !isSearching) View.VISIBLE else View.GONE
        searchProgress.visibility = if (isSearching) View.VISIBLE else View.GONE
        swipeRefresh.isRefreshing = isLoading && !isSearching
    }

    private fun renderStudents(students: List<StaffStudentItem>) {
        if (!viewModel.errorMessage.value.isNullOrBlank()) return
        studentsContainer.removeAllViews()
        when {
            viewModel.isLoading.value == true && students.isEmpty() -> {
                studentsContainer.addView(defaultStateCard("Loading students", "Fetching student records from backend.", R.drawable.ic_hourglass))
            }
            students.isEmpty() -> {
                studentsContainer.addView(defaultStateCard("No students found", "Try searching with another name, email, or roll number.", R.drawable.ic_student))
            }
            else -> students.forEach { studentsContainer.addView(studentCard(it)) }
        }
    }

    private fun renderError(message: String) {
        studentsContainer.removeAllViews()
        val card = defaultStateCard("Unable to load students", message.take(140), R.drawable.ic_warning)
        card.setOnClickListener { viewModel.refresh() }
        studentsContainer.addView(card)
    }

    private fun updateStatusTabs(view: View, selected: StaffStudentsStatus) {
        bindStatusTab(view, R.id.tabStaffStudentsAll, R.id.iconTabStaffStudentsAll, R.id.textTabStaffStudentsAll, StaffStudentsStatus.ALL, selected, R.color.student_primary)
        bindStatusTab(view, R.id.tabStaffStudentsActive, R.id.iconTabStaffStudentsActive, R.id.textTabStaffStudentsActive, StaffStudentsStatus.ACTIVE, selected, R.color.student_green)
        bindStatusTab(view, R.id.tabStaffStudentsInactive, R.id.iconTabStaffStudentsInactive, R.id.textTabStaffStudentsInactive, StaffStudentsStatus.INACTIVE, selected, R.color.student_danger)
    }

    private fun bindStatusTab(view: View, rootId: Int, iconId: Int, labelId: Int, tab: StaffStudentsStatus, selected: StaffStudentsStatus, tint: Int) {
        val active = tab == selected
        view.findViewById<View>(rootId)?.setBackgroundResource(if (active) R.drawable.bg_requests_segment_active else 0)
        val color = if (active) R.color.white else tint
        view.findViewById<ImageView>(iconId)?.setColorFilter(color(color))
        view.findViewById<TextView>(labelId)?.setTextColor(color(if (active) R.color.white else R.color.my_requests_text_primary))
    }

    private fun studentCard(student: StaffStudentItem): View {
        val card = baseCard(14)
        val content = vertical(16)
        val header = horizontal()
        header.addView(avatarBox(student.name, student.displayPhoto, 56))
        val titles = vertical()
        titles.addView(text(student.name.orDash(), 17, R.color.student_text_primary, bold = true))
        titles.addView(text(student.displayIdentifier, 12, R.color.student_text_muted, topMargin = 3))
        header.addView(titles, weight = 1f)
        header.addView(statusBadge(student.normalizedStatus))
        content.addView(header)
        content.addView(metaRow(R.drawable.ic_email, "Email", student.email.orDash(), R.color.student_text_muted))
        content.addView(metaRow(R.drawable.ic_phone, "Phone", student.phone.orDash(), R.color.student_text_muted, compact = true))
        content.addView(metaRow(R.drawable.ic_graduation_cap, "Department", student.displayDepartment, R.color.student_purple, compact = true))
        val action = actionText("View Details")
        action.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_chevron_right, 0)
        action.compoundDrawablePadding = dp(6)
        action.setOnClickListener { (activity as? StaffDashboardActivity)?.openStudentDetails(student.id) }
        content.addView(action, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46)).apply { topMargin = dp(14) })
        card.addView(content)
        return card
    }

    private fun defaultStateCard(title: String, subtitle: String, icon: Int): View =
        baseCard(14).apply { addView(emptyState(title, subtitle, icon)) }

    private fun emptyState(title: String, subtitle: String, icon: Int): View {
        val layout = vertical(18)
        layout.gravity = Gravity.CENTER
        layout.background = rounded(color(R.color.profile_gray_bg), dp(16))
        layout.addView(iconBox(icon, R.color.student_text_muted, 44, 23))
        val titleView = text(title, 15, R.color.student_text_primary, bold = true, topMargin = 8)
        titleView.gravity = Gravity.CENTER
        layout.addView(titleView)
        val subtitleView = text(subtitle, 12, R.color.student_text_muted, topMargin = 4)
        subtitleView.gravity = Gravity.CENTER
        layout.addView(subtitleView)
        return layout
    }

    private fun actionText(label: String): TextView =
        TextView(requireContext()).apply {
            text = label
            gravity = Gravity.CENTER
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(color(R.color.white))
            setBackgroundResource(R.drawable.bg_profile_primary_button)
        }

    private fun statusBadge(status: String): TextView {
        val tint = when (status.lowercase(Locale.US)) {
            "active" -> R.color.student_green
            "inactive", "disabled", "blocked" -> R.color.student_danger
            else -> R.color.student_text_muted
        }
        return badge(status.titleCase(), tint)
    }

    private fun navigateToUnauthorized() {
        if (!isAdded) return
        startActivity(Intent(requireContext(), UnauthorizedActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        requireActivity().finish()
    }
}

class StaffStudentDetailScreen : Fragment() {
    private lateinit var viewModel: StaffStudentDetailViewModel
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var topProgress: ProgressBar
    private lateinit var profileContainer: LinearLayout
    private lateinit var tabContentContainer: LinearLayout
    private var studentId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        studentId = requireArguments().getInt(ARG_STUDENT_ID)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_staff_student_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupViewModel()
        swipeRefresh = view.findViewById(R.id.staffStudentDetailSwipeRefresh)
        topProgress = view.findViewById(R.id.progressStaffStudentDetailTop)
        profileContainer = view.findViewById(R.id.containerStudentDetailProfile)
        tabContentContainer = view.findViewById(R.id.containerStudentDetailTabContent)
        view.findViewById<View>(R.id.buttonStaffStudentDetailBack).setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        swipeRefresh.setColorSchemeResources(R.color.student_primary, R.color.student_purple, R.color.student_green, R.color.student_yellow)
        swipeRefresh.setOnRefreshListener { viewModel.refreshAll() }
        setupTabs(view)
        setupObservers(view)
        viewModel.loadStudentDetails(studentId)
    }

    private fun setupViewModel() {
        val tokenManager = (requireActivity().application as MainApplication).tokenManager
        val apiService = RetrofitClient.getApiService(tokenManager)
        val repository = StaffStudentsRepository(apiService, tokenManager)
        viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return StaffStudentDetailViewModel(repository) as T
                }
            }
        )[StaffStudentDetailViewModel::class.java]
    }

    private fun setupTabs(view: View) {
        view.findViewById<TextView>(R.id.textTabStudentIssuedBooks)?.text = "Issued Books"
        view.findViewById<TextView>(R.id.textTabStudentFines)?.text = "Fine Overview"
        view.findViewById<TextView>(R.id.textTabStudentBookRequests)?.text = "Book Requests"
        view.findViewById<View>(R.id.tabStudentIssuedBooks)?.setOnClickListener {
            viewModel.changeTab(StaffStudentDetailTab.ISSUED_BOOKS)
        }
        view.findViewById<View>(R.id.tabStudentFines)?.setOnClickListener {
            viewModel.changeTab(StaffStudentDetailTab.FINE_OVERVIEW)
        }
        view.findViewById<View>(R.id.tabStudentBookRequests)?.setOnClickListener {
            viewModel.changeTab(StaffStudentDetailTab.BOOK_REQUESTS)
        }
    }

    private fun setupObservers(view: View) {
        viewModel.student.observe(viewLifecycleOwner) { renderStudentInfo(it) }
        viewModel.selectedTab.observe(viewLifecycleOwner) {
            updateDetailTabs(view, it)
            renderCurrentTab()
        }
        viewModel.issuedBooks.observe(viewLifecycleOwner) { if (viewModel.selectedTab.value == StaffStudentDetailTab.ISSUED_BOOKS) renderIssuedBooks(it) }
        viewModel.fineSummary.observe(viewLifecycleOwner) { if (viewModel.selectedTab.value == StaffStudentDetailTab.FINE_OVERVIEW) renderFines(viewModel.fines.value.orEmpty()) }
        viewModel.fines.observe(viewLifecycleOwner) { if (viewModel.selectedTab.value == StaffStudentDetailTab.FINE_OVERVIEW) renderFines(it) }
        viewModel.bookRequests.observe(viewLifecycleOwner) { if (viewModel.selectedTab.value == StaffStudentDetailTab.BOOK_REQUESTS) renderBookRequests(it) }
        listOf(
            viewModel.isLoadingStudent,
            viewModel.isLoadingIssuedBooks,
            viewModel.isLoadingFines,
            viewModel.isLoadingBookRequests,
            viewModel.isPayingFine,
            viewModel.isWaivingFine,
            viewModel.isApprovingRequest,
            viewModel.isRejectingRequest
        ).forEach { liveData -> liveData.observe(viewLifecycleOwner) { updateLoading() } }
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrBlank()) {
                LmsToast.show(requireContext(), message.take(140))
                if (viewModel.student.value == null) renderProfileError(message)
                viewModel.clearMessages()
            }
        }
        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrBlank()) {
                LmsToast.show(requireContext(), message.take(140))
                viewModel.clearMessages()
            }
        }
        viewModel.unauthorized.observe(viewLifecycleOwner) { if (it) navigateToUnauthorized() }
    }

    private fun updateLoading() {
        val loading = viewModel.isLoadingStudent.value == true ||
            viewModel.isLoadingIssuedBooks.value == true ||
            viewModel.isLoadingFines.value == true ||
            viewModel.isLoadingBookRequests.value == true ||
            viewModel.isPayingFine.value == true ||
            viewModel.isWaivingFine.value == true ||
            viewModel.isApprovingRequest.value == true ||
            viewModel.isRejectingRequest.value == true
        topProgress.visibility = if (loading) View.VISIBLE else View.GONE
        swipeRefresh.isRefreshing = viewModel.isLoadingStudent.value == true
    }

    private fun renderCurrentTab() {
        when (viewModel.selectedTab.value ?: StaffStudentDetailTab.ISSUED_BOOKS) {
            StaffStudentDetailTab.ISSUED_BOOKS -> renderIssuedBooks(viewModel.issuedBooks.value.orEmpty())
            StaffStudentDetailTab.FINE_OVERVIEW -> renderFines(viewModel.fines.value.orEmpty())
            StaffStudentDetailTab.BOOK_REQUESTS -> renderBookRequests(viewModel.bookRequests.value.orEmpty())
        }
    }

    private fun renderStudentInfo(student: StaffStudentItem?) {
        profileContainer.removeAllViews()
        if (viewModel.isLoadingStudent.value == true && student == null) {
            profileContainer.addView(defaultStateCard("Loading student details", "Fetching profile from backend.", R.drawable.ic_hourglass))
            return
        }
        student ?: return
        val card = baseCard(16)
        val content = vertical(16)
        val header = horizontal()
        header.addView(avatarBox(student.name, student.displayPhoto, 62))
        val titles = vertical()
        titles.addView(text(student.name.orDash(), 19, R.color.student_text_primary, bold = true))
        titles.addView(text(student.displayIdentifier, 12, R.color.student_text_muted, topMargin = 3))
        header.addView(titles, weight = 1f)
        header.addView(statusBadge(student.normalizedStatus))
        content.addView(header)
        content.addView(metaRow(R.drawable.ic_email, "Email", student.email.orDash(), R.color.student_text_muted))
        content.addView(metaRow(R.drawable.ic_phone, "Phone", student.phone.orDash(), R.color.student_text_muted, compact = true))
        content.addView(metaRow(R.drawable.ic_graduation_cap, "Department", student.displayDepartment, R.color.student_purple, compact = true))
        content.addView(metaRow(R.drawable.ic_calendar, "Batch", student.batch.orDash(), R.color.student_primary, compact = true))
        content.addView(metaRow(R.drawable.ic_id_card, "Student ID", student.displayIdentifier, R.color.student_text_muted, compact = true))
        card.addView(content)
        profileContainer.addView(card)
    }

    private fun renderProfileError(message: String) {
        profileContainer.removeAllViews()
        val card = defaultStateCard("Unable to load student details", message.take(140), R.drawable.ic_warning)
        card.setOnClickListener { viewModel.loadStudentDetails(studentId) }
        profileContainer.addView(card)
    }

    private fun renderIssuedBooks(books: List<StudentIssuedBookItem>) {
        tabContentContainer.removeAllViews()
        tabContentContainer.addView(sectionHeader("Issued Books", R.drawable.ic_book, R.color.student_primary))
        when {
            viewModel.isLoadingIssuedBooks.value == true && books.isEmpty() ->
                tabContentContainer.addView(defaultStateCard("Loading issued books", "Checking this student's circulation records.", R.drawable.ic_hourglass))
            books.isEmpty() ->
                tabContentContainer.addView(defaultStateCard("No issued books", "This student has no circulation records.", R.drawable.ic_book))
            else -> books.forEach { tabContentContainer.addView(issuedBookCard(it)) }
        }
    }

    private fun renderFines(fines: List<StudentFineItem>) {
        tabContentContainer.removeAllViews()
        tabContentContainer.addView(sectionHeader("Fine Records", R.drawable.ic_rupee, R.color.student_danger))
        when {
            viewModel.isLoadingFines.value == true && fines.isEmpty() ->
                tabContentContainer.addView(defaultStateCard("Loading fines", "Fetching this student's fine records.", R.drawable.ic_hourglass))
            fines.isEmpty() ->
                tabContentContainer.addView(defaultStateCard("No fines found", "This student has no fine records.", R.drawable.ic_receipt))
            else -> fines.forEach { tabContentContainer.addView(fineRecordCard(it)) }
        }
    }

    private fun renderBookRequests(requests: List<StudentBookRequestItem>) {
        tabContentContainer.removeAllViews()
        tabContentContainer.addView(sectionHeader("Book Requests", R.drawable.ic_clipboard, R.color.student_purple))
        when {
            viewModel.isLoadingBookRequests.value == true && requests.isEmpty() ->
                tabContentContainer.addView(defaultStateCard("Loading book requests", "Fetching this student's request history.", R.drawable.ic_hourglass))
            requests.isEmpty() ->
                tabContentContainer.addView(defaultStateCard("No book requests", "This student has no book request history.", R.drawable.ic_request))
            else -> requests.forEach { tabContentContainer.addView(bookRequestCard(it)) }
        }
    }

    private fun issuedBookCard(book: StudentIssuedBookItem): View {
        val status = book.status?.lowercase(Locale.US) ?: "issued"
        val card = baseCard(14)
        val content = vertical(16)
        val header = horizontal()
        header.addView(iconBox(R.drawable.ic_book, statusTint(status), 52, 26))
        val titles = vertical()
        titles.addView(text(book.displayTitle, 16, R.color.student_text_primary, bold = true))
        titles.addView(text(book.author.orDash(), 12, R.color.student_text_muted, topMargin = 3))
        header.addView(titles, weight = 1f)
        header.addView(statusBadge(status))
        content.addView(header)
        content.addView(metaRow(R.drawable.ic_barcode, "ISBN / Accession", listOf(book.isbn, book.accessionNo).firstOrNull { !it.isNullOrBlank() }.orDash(), R.color.student_text_muted))
        content.addView(metaRow(R.drawable.ic_calendar, "Issued Date", formatDate(book.displayIssueDate), R.color.student_primary, compact = true))
        content.addView(metaRow(R.drawable.ic_calendar, "Due Date", formatDate(book.dueDate), R.color.student_yellow, compact = true))
        content.addView(metaRow(R.drawable.ic_refresh, "Return Date", formatDate(book.returnDate), R.color.student_green, compact = true))
        content.addView(metaRow(R.drawable.ic_rupee, "Fine Amount", formatCurrency(book.fineAmount), if (book.fineAmount > 0) R.color.student_danger else R.color.student_green, compact = true))
        card.addView(content)
        return card
    }

    private fun fineRecordCard(fine: StudentFineItem): View {
        val status = fine.status?.lowercase(Locale.US) ?: "pending"
        val card = baseCard(14)
        val content = vertical(16)
        val header = horizontal()
        header.addView(iconBox(R.drawable.ic_rupee, fineStatusTint(status), 52, 25))
        val titles = vertical()
        titles.addView(text(fine.bookTitle.orDash(), 16, R.color.student_text_primary, bold = true))
        fine.author?.takeIf { it.isNotBlank() }?.let { titles.addView(text(it, 12, R.color.student_text_muted, topMargin = 3)) }
        header.addView(titles, weight = 1f)
        header.addView(statusBadge(status))
        content.addView(header)
        content.addView(metaRow(R.drawable.ic_calendar, "Due Date", formatDate(fine.dueDate), R.color.student_text_muted))
        content.addView(metaRow(R.drawable.ic_refresh, "Return Date", formatDate(fine.returnDate), R.color.student_text_muted, compact = true))
        content.addView(metaRow(R.drawable.ic_clock, "Days Late", fine.resolvedDaysLate.toString(), R.color.student_danger, compact = true))
        content.addView(metaRow(R.drawable.ic_rupee, "Amount", formatCurrency(fine.amount), fineStatusTint(status), compact = true))
        content.addView(metaRow(R.drawable.ic_info, "Reason", fine.resolvedReason.orDash(), R.color.student_text_muted, compact = true))
        if (status == "paid") content.addView(metaRow(R.drawable.ic_check_circle, "Paid Date", formatDateTime(fine.paidAt ?: fine.paidDate), R.color.student_green, compact = true))
        if (status == "waived") {
            content.addView(metaRow(R.drawable.ic_shield_check, "Waived Date", formatDateTime(fine.waivedAt), R.color.student_purple, compact = true))
            content.addView(metaRow(R.drawable.ic_document, "Waive Reason", fine.waiveReason.orDash(), R.color.student_purple, compact = true))
        }
        if (status == "pending") {
            val row = horizontal(topMargin = 14)
            val pay = actionText("Mark Paid")
            val waive = actionText("Waive", outline = true)
            pay.setOnClickListener { showPayConfirmation(fine) }
            waive.setOnClickListener { showWaiveDialog(fine) }
            row.addView(pay, LinearLayout.LayoutParams(0, dp(46), 1f).apply { marginEnd = dp(6) })
            row.addView(waive, LinearLayout.LayoutParams(0, dp(46), 1f).apply { marginStart = dp(6) })
            content.addView(row)
        }
        card.addView(content)
        return card
    }

    private fun bookRequestCard(request: StudentBookRequestItem): View {
        val status = request.status?.lowercase(Locale.US) ?: "pending"
        val card = baseCard(14)
        val content = vertical(16)
        val header = horizontal()
        header.addView(iconBox(R.drawable.ic_clipboard, requestStatusTint(status), 52, 25))
        val titles = vertical()
        titles.addView(text(request.displayBookTitle, 16, R.color.student_text_primary, bold = true))
        titles.addView(text(request.displayAuthor.orDash(), 12, R.color.student_text_muted, topMargin = 3))
        header.addView(titles, weight = 1f)
        header.addView(statusBadge(status))
        content.addView(header)
        content.addView(metaRow(R.drawable.ic_calendar, "Request Date", formatDate(request.displayRequestDate), R.color.student_primary))
        content.addView(metaRow(R.drawable.ic_user, "Processed By", request.displayProcessedBy.orDash(), R.color.student_text_muted, compact = true))
        content.addView(metaRow(R.drawable.ic_calendar, "Processed Date", formatDateTime(request.displayProcessedAt), R.color.student_text_muted, compact = true))
        request.message?.takeIf { it.isNotBlank() }?.let {
            content.addView(metaRow(R.drawable.ic_message, "Message", it, R.color.student_text_muted, compact = true))
        }
        if (status == "pending") {
            val row = horizontal(topMargin = 14)
            val accept = actionText("Accept")
            val reject = actionText("Reject", outline = true)
            accept.setOnClickListener { showAcceptConfirmation(request) }
            reject.setOnClickListener { showRejectConfirmation(request) }
            row.addView(accept, LinearLayout.LayoutParams(0, dp(46), 1f).apply { marginEnd = dp(6) })
            row.addView(reject, LinearLayout.LayoutParams(0, dp(46), 1f).apply { marginStart = dp(6) })
            content.addView(row)
        }
        card.addView(content)
        return card
    }

    private fun showPayConfirmation(fine: StudentFineItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("Mark Fine as Paid?")
            .setMessage("${formatCurrency(fine.amount)} for \"${fine.bookTitle.orDash()}\" will be marked as paid.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Mark Paid") { _, _ -> viewModel.markFinePaid(fine.resolvedId) }
            .show()
    }

    private fun showWaiveDialog(fine: StudentFineItem) {
        val wrapper = vertical(0).apply {
            setPadding(dp(20), dp(20), dp(20), dp(16))
            background = rounded(color(R.color.my_requests_card), dp(18))
        }
        val titleRow = horizontal()
        titleRow.addView(iconBox(R.drawable.ic_shield_check, R.color.my_fines_purple, 42, 22))
        titleRow.addView(text("Waive Fine", 22, R.color.my_requests_text_primary, bold = true), weight = 1f)
        wrapper.addView(titleRow)
        wrapper.addView(text("Add a short reason. This note will be saved with the fine record.", 13, R.color.my_requests_text_secondary, topMargin = 12))
        val input = EditText(requireContext()).apply {
            hint = "Enter waiver reason..."
            minLines = 4
            gravity = Gravity.TOP
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = roundedStroke(color(R.color.my_requests_card), color(R.color.my_requests_border), dp(14))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setTextColor(color(R.color.my_requests_text_primary))
            setHintTextColor(color(R.color.my_requests_text_hint))
            textSize = 15f
        }
        wrapper.addView(input, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(124)).apply { topMargin = dp(14) })
        val actions = horizontal(topMargin = 16)
        val cancel = actionText("Cancel", outline = true)
        val waive = actionText("Waive Fine")
        actions.addView(cancel, LinearLayout.LayoutParams(0, dp(46), 1f).apply { marginEnd = dp(6) })
        actions.addView(waive, LinearLayout.LayoutParams(0, dp(46), 1f).apply { marginStart = dp(6) })
        wrapper.addView(actions)
        val dialog = AlertDialog.Builder(requireContext()).setView(wrapper).create()
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            cancel.setOnClickListener { dialog.dismiss() }
            waive.setOnClickListener {
                val reason = input.text?.toString().orEmpty().trim()
                if (reason.length < 3) {
                    input.error = "Reason must be at least 3 characters."
                    return@setOnClickListener
                }
                dialog.dismiss()
                viewModel.waiveFine(fine.resolvedId, reason)
            }
        }
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        dialog.show()
    }

    private fun showAcceptConfirmation(request: StudentBookRequestItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("Accept Request?")
            .setMessage("\"${request.displayBookTitle}\" will be accepted for this student.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Accept") { _, _ -> viewModel.approveBookRequest(request.resolvedId) }
            .show()
    }

    private fun showRejectConfirmation(request: StudentBookRequestItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("Reject Request?")
            .setMessage("\"${request.displayBookTitle}\" will be rejected.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Reject") { _, _ -> viewModel.rejectBookRequest(request.resolvedId) }
            .show()
    }

    private fun updateDetailTabs(view: View, selected: StaffStudentDetailTab) {
        bindDetailTab(view, R.id.tabStudentIssuedBooks, R.id.iconTabStudentIssuedBooks, R.id.textTabStudentIssuedBooks, StaffStudentDetailTab.ISSUED_BOOKS, selected, R.color.student_primary)
        bindDetailTab(view, R.id.tabStudentFines, R.id.iconTabStudentFines, R.id.textTabStudentFines, StaffStudentDetailTab.FINE_OVERVIEW, selected, R.color.student_danger)
        bindDetailTab(view, R.id.tabStudentBookRequests, R.id.iconTabStudentBookRequests, R.id.textTabStudentBookRequests, StaffStudentDetailTab.BOOK_REQUESTS, selected, R.color.student_purple)
    }

    private fun bindDetailTab(view: View, rootId: Int, iconId: Int, labelId: Int, tab: StaffStudentDetailTab, selected: StaffStudentDetailTab, tint: Int) {
        val active = tab == selected
        view.findViewById<View>(rootId)?.setBackgroundResource(if (active) R.drawable.bg_requests_segment_active else 0)
        val color = if (active) R.color.white else tint
        view.findViewById<ImageView>(iconId)?.setColorFilter(color(color))
        view.findViewById<TextView>(labelId)?.setTextColor(color(if (active) R.color.white else R.color.my_requests_text_primary))
    }

    private fun sectionHeader(title: String, icon: Int, tint: Int): View {
        val row = horizontal()
        row.addView(iconBox(icon, tint, 38, 20))
        row.addView(text(title, 18, R.color.student_text_primary, bold = true), weight = 1f)
        row.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(4)
            bottomMargin = dp(12)
        }
        return row
    }

    private fun actionText(label: String, outline: Boolean = false): TextView =
        TextView(requireContext()).apply {
            text = label
            gravity = Gravity.CENTER
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(color(if (outline) R.color.profile_primary else R.color.white))
            setBackgroundResource(if (outline) R.drawable.bg_profile_outline_button else R.drawable.bg_profile_primary_button)
        }

    private fun defaultStateCard(title: String, subtitle: String, icon: Int): View =
        baseCard(14).apply { addView(emptyState(title, subtitle, icon)) }

    private fun emptyState(title: String, subtitle: String, icon: Int): View {
        val layout = vertical(18)
        layout.gravity = Gravity.CENTER
        layout.background = rounded(color(R.color.profile_gray_bg), dp(16))
        layout.addView(iconBox(icon, R.color.student_text_muted, 44, 23))
        val titleView = text(title, 15, R.color.student_text_primary, bold = true, topMargin = 8)
        titleView.gravity = Gravity.CENTER
        layout.addView(titleView)
        val subtitleView = text(subtitle, 12, R.color.student_text_muted, topMargin = 4)
        subtitleView.gravity = Gravity.CENTER
        layout.addView(subtitleView)
        return layout
    }

    private fun statusBadge(status: String): TextView =
        badge(status.titleCase(), when (status.lowercase(Locale.US)) {
            "active", "approved", "returned", "paid" -> R.color.student_green
            "inactive", "rejected", "overdue" -> R.color.student_danger
            "pending", "issued" -> R.color.student_yellow
            "waived", "cancelled" -> R.color.student_purple
            else -> R.color.student_text_muted
        })

    private fun statusTint(status: String): Int = when (status.lowercase(Locale.US)) {
        "issued" -> R.color.student_primary
        "returned" -> R.color.student_green
        "overdue" -> R.color.student_danger
        else -> R.color.student_text_muted
    }

    private fun fineStatusTint(status: String): Int = when (status.lowercase(Locale.US)) {
        "paid" -> R.color.student_green
        "waived" -> R.color.student_purple
        else -> R.color.student_danger
    }

    private fun requestStatusTint(status: String): Int = when (status.lowercase(Locale.US)) {
        "approved", "accepted" -> R.color.student_green
        "rejected" -> R.color.student_danger
        "cancelled", "canceled" -> R.color.student_text_muted
        else -> R.color.student_yellow
    }

    private fun navigateToUnauthorized() {
        if (!isAdded) return
        startActivity(Intent(requireContext(), UnauthorizedActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        requireActivity().finish()
    }

    companion object {
        private const val ARG_STUDENT_ID = "student_id"
        fun newInstance(studentId: Int): StaffStudentDetailScreen =
            StaffStudentDetailScreen().apply {
                arguments = Bundle().apply { putInt(ARG_STUDENT_ID, studentId) }
            }
    }
}

private fun Fragment.baseCard(bottomMargin: Int): CardView =
    CardView(requireContext()).apply {
        radius = dp(18).toFloat()
        cardElevation = dp(2).toFloat()
        setCardBackgroundColor(color(R.color.my_requests_card))
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            this.bottomMargin = dp(bottomMargin)
        }
    }

private fun Fragment.horizontal(topMargin: Int = 0): LinearLayout =
    LinearLayout(requireContext()).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            this.topMargin = dp(topMargin)
        }
    }

private fun Fragment.vertical(padding: Int = 0): LinearLayout =
    LinearLayout(requireContext()).apply {
        orientation = LinearLayout.VERTICAL
        if (padding > 0) setPadding(dp(padding), dp(padding), dp(padding), dp(padding))
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

private fun LinearLayout.addView(view: View, weight: Float) {
    addView(view, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight))
}

private fun Fragment.text(value: String, sizeSp: Int, colorRes: Int, bold: Boolean = false, topMargin: Int = 0): TextView =
    TextView(requireContext()).apply {
        text = value
        textSize = sizeSp.toFloat()
        setTextColor(color(colorRes))
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        includeFontPadding = true
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            this.topMargin = dp(topMargin)
        }
    }

private fun Fragment.metaRow(icon: Int, label: String, value: String, tint: Int, compact: Boolean = false): View {
    val row = horizontal(topMargin = if (compact) 8 else 12)
    row.addView(smallIcon(icon, tint))
    row.addView(text(label, if (compact) 11 else 12, R.color.student_text_muted), weight = 0.72f)
    val valueText = text(value, if (compact) 12 else 13, if (tint == R.color.student_text_muted) R.color.student_text_primary else tint, bold = true)
    valueText.gravity = Gravity.END
    row.addView(valueText, weight = 1f)
    return row
}

private fun Fragment.smallIcon(icon: Int, tint: Int): ImageView =
    ImageView(requireContext()).apply {
        setImageResource(icon)
        setColorFilter(color(tint))
        layoutParams = LinearLayout.LayoutParams(dp(18), dp(18)).apply { marginEnd = dp(8) }
    }

private fun Fragment.iconBox(icon: Int, tint: Int, size: Int = 42, iconSize: Int = 22): FrameLayout {
    val box = FrameLayout(requireContext()).apply {
        background = rounded(badgeBackgroundFor(tint), dp(14))
        layoutParams = LinearLayout.LayoutParams(dp(size), dp(size)).apply { marginEnd = dp(10) }
    }
    box.addView(ImageView(requireContext()).apply {
        setImageResource(icon)
        setColorFilter(color(tint))
        layoutParams = FrameLayout.LayoutParams(dp(iconSize), dp(iconSize), Gravity.CENTER)
    })
    return box
}

private fun Fragment.avatarBox(name: String?, rawUrl: String?, size: Int): FrameLayout {
    val box = FrameLayout(requireContext()).apply {
        background = rounded(color(R.color.profile_blue_bg), dp(18))
        clipToOutline = true
        layoutParams = LinearLayout.LayoutParams(dp(size), dp(size)).apply { marginEnd = dp(10) }
    }
    val initial = TextView(requireContext()).apply {
        text = initials(name)
        gravity = Gravity.CENTER
        textSize = 18f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(color(R.color.student_primary))
        layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }
    val image = ImageView(requireContext()).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        visibility = View.GONE
        layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }
    box.addView(initial)
    box.addView(image)
    loadAvatarImage(image, initial, rawUrl)
    return box
}

private fun Fragment.loadAvatarImage(image: ImageView, initial: TextView, rawUrl: String?) {
    val photoUrl = normalizeMediaUrl(rawUrl)
    image.tag = photoUrl
    if (photoUrl.isNullOrBlank()) {
        image.visibility = View.GONE
        initial.visibility = View.VISIBLE
        return
    }
    thread(name = "staff-student-avatar", isDaemon = true) {
        val bitmap = runCatching { URL(photoUrl).openStream().use { BitmapFactory.decodeStream(it) } }
            .onFailure { Log.w("StaffStudents", "Student photo failed to load: $photoUrl", it) }
            .getOrNull()
        image.post {
            if (image.tag == photoUrl && bitmap != null) {
                image.setImageBitmap(bitmap)
                image.visibility = View.VISIBLE
                initial.visibility = View.GONE
            } else {
                image.visibility = View.GONE
                initial.visibility = View.VISIBLE
            }
        }
    }
}

private fun Fragment.badge(label: String, tint: Int): TextView =
    TextView(requireContext()).apply {
        text = label
        textSize = 11f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(color(tint))
        background = rounded(badgeBackgroundFor(tint), dp(12))
        setPadding(dp(10), dp(5), dp(10), dp(5))
        gravity = Gravity.CENTER
    }

private fun Fragment.rounded(fill: Int, radius: Int): GradientDrawable =
    GradientDrawable().apply {
        setColor(fill)
        cornerRadius = radius.toFloat()
    }

private fun Fragment.roundedStroke(fill: Int, stroke: Int, radius: Int): GradientDrawable =
    GradientDrawable().apply {
        setColor(fill)
        cornerRadius = radius.toFloat()
        setStroke(dp(1), stroke)
    }

private fun Fragment.badgeBackgroundFor(tint: Int): Int = when (tint) {
    R.color.student_success, R.color.student_green, R.color.my_requests_green -> color(R.color.my_requests_green_bg)
    R.color.student_danger, R.color.my_requests_red -> color(R.color.my_requests_red_bg)
    R.color.student_yellow, R.color.my_requests_orange -> color(R.color.my_requests_orange_bg)
    R.color.student_purple, R.color.my_fines_purple -> color(R.color.search_badge_purple_bg)
    R.color.student_text_muted -> color(R.color.my_requests_gray_bg)
    else -> color(R.color.my_requests_blue_bg)
}

private fun Fragment.normalizeMediaUrl(rawUrl: String?): String? {
    val value = rawUrl?.trim()?.takeIf { it.isNotBlank() } ?: return null
    if (value.startsWith("data:", ignoreCase = true)) return null
    val apiRoot = Constants.BASE_URL.removeSuffix("api/").trimEnd('/')
    if (!value.startsWith("http", ignoreCase = true)) {
        val path = value.trimStart('/').removePrefix("public/").removePrefix("storage/app/public/")
        return "$apiRoot/${if (path.startsWith("storage/")) path else "storage/$path"}"
    }
    return value
        .let { Constants.normalizeLaravelAssetUrl(it) ?: it }
}

private fun formatCurrency(amount: Double): String = "Rs. ${String.format(Locale.US, "%.2f", amount)}"

private fun formatDate(raw: String?): String {
    if (raw.isNullOrBlank()) return "-"
    val parsed = parseDate(raw) ?: return raw.take(10)
    return SimpleDateFormat("MMM dd, yyyy", Locale.US).format(parsed)
}

private fun formatDateTime(raw: String?): String {
    if (raw.isNullOrBlank()) return "-"
    val parsed = parseDate(raw) ?: return raw.take(16)
    return SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.US).format(parsed)
}

private fun parseDate(raw: String?): java.util.Date? {
    if (raw.isNullOrBlank()) return null
    val normalized = raw.trim().replace(Regex("\\.\\d+Z$"), "Z")
    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd"
    )
    return patterns.firstNotNullOfOrNull { pattern ->
        runCatching { SimpleDateFormat(pattern, Locale.US).parse(normalized) }.getOrNull()
    }
}

private fun Fragment.color(colorRes: Int): Int = ContextCompat.getColor(requireContext(), colorRes)
private fun Fragment.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
private fun String?.orDash(): String = if (isNullOrBlank()) "-" else this
private fun initials(name: String?): String = name.orEmpty().trim().split(Regex("\\s+")).filter { it.isNotBlank() }.take(2)
    .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }.joinToString("").ifBlank { "S" }
private fun String.titleCase(): String = replace("_", " ").split(" ").filter { it.isNotBlank() }.joinToString(" ") {
    it.lowercase(Locale.US).replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(Locale.US) else char.toString() }
}
