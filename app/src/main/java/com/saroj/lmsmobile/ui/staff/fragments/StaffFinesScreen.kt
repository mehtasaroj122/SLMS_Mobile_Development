package com.saroj.lmsmobile.ui.staff.fragments

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.saroj.lmsmobile.MainApplication
import com.saroj.lmsmobile.R
import com.saroj.lmsmobile.api.RetrofitClient
import com.saroj.lmsmobile.data.models.fine.FineRecord
import com.saroj.lmsmobile.data.models.fine.FineStudentInfo
import com.saroj.lmsmobile.data.models.fine.FineStudentSummary
import com.saroj.lmsmobile.data.models.fine.FineSummaryData
import com.saroj.lmsmobile.data.models.fine.StudentFineSummary
import com.saroj.lmsmobile.data.repository.StaffFinesRepository
import com.saroj.lmsmobile.ui.common.UnauthorizedActivity
import com.saroj.lmsmobile.ui.components.OnlineRefreshable
import com.saroj.lmsmobile.ui.components.runIfOnline
import com.saroj.lmsmobile.ui.staff.StaffDashboardActivity
import com.saroj.lmsmobile.ui.staff.viewmodel.StaffFineDetailViewModel
import com.saroj.lmsmobile.ui.staff.viewmodel.StaffFinesViewModel
import com.saroj.lmsmobile.utils.Constants
import com.saroj.lmsmobile.utils.LmsToast
import com.saroj.lmsmobile.utils.NetworkMessages
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.concurrent.thread
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class StaffFinesScreen : Fragment(), OnlineRefreshable {
    private lateinit var viewModel: StaffFinesViewModel
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var summaryContainer: LinearLayout
    private lateinit var studentsContainer: LinearLayout
    private lateinit var searchInput: EditText
    private lateinit var clearSearchButton: ImageView
    private lateinit var scrollView: NestedScrollView
    private lateinit var topProgress: ProgressBar
    private lateinit var searchProgress: ProgressBar
    private lateinit var loadMoreProgress: ProgressBar
    private lateinit var searchError: TextView
    private var initialLoadStarted = false
    private var allStudents: List<FineStudentSummary> = emptyList()
    private var visibleStudentCount = PAGE_SIZE
    private var loadingMoreStudents = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_staff_fines, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupViewModel()
        bindViews(view)
        setupRefresh()
        setupSearch()
        setupLoadMore()
        setupObservers()
        viewModel.loadInitial()
        initialLoadStarted = true
    }

    override fun onResume() {
        super.onResume()
        if (initialLoadStarted) runIfOnline { viewModel.refresh() }
    }

    private fun setupViewModel() {
        val tokenManager = (requireActivity().application as MainApplication).tokenManager
        val apiService = RetrofitClient.getApiService(tokenManager)
        val repository = StaffFinesRepository(apiService, tokenManager)
        viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return StaffFinesViewModel(repository) as T
                }
            }
        )[StaffFinesViewModel::class.java]
    }

    private fun bindViews(view: View) {
        swipeRefresh = view.findViewById(R.id.staffFinesSwipeRefresh)
        summaryContainer = view.findViewById(R.id.containerFineSummary)
        studentsContainer = view.findViewById(R.id.containerFineStudents)
        searchInput = view.findViewById(R.id.inputFineSearch)
        clearSearchButton = view.findViewById(R.id.buttonClearFineSearch)
        scrollView = view.findViewById(R.id.staffFinesScrollView)
        topProgress = view.findViewById(R.id.progressStaffFinesTop)
        searchProgress = view.findViewById(R.id.progressFineSearch)
        loadMoreProgress = view.findViewById(R.id.progressFineStudentsLoadMore)
        searchError = view.findViewById(R.id.textFineSearchError)
    }

    private fun setupRefresh() {
        swipeRefresh.setColorSchemeResources(
            R.color.student_primary,
            R.color.student_purple,
            R.color.student_green,
            R.color.student_yellow
        )
        swipeRefresh.setOnRefreshListener {
            runIfOnline(onOffline = { swipeRefresh.isRefreshing = false }) { viewModel.refresh() }
        }
    }

    override fun refreshAfterOnline() {
        viewModel.refresh()
    }

    private fun setupSearch() {
        clearSearchButton.setOnClickListener { searchInput.setText("") }
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                clearSearchButton.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
                viewModel.searchFineStudents(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                viewModel.searchFineStudents(searchInput.text?.toString().orEmpty())
                true
            } else {
                false
            }
        }
    }

    private fun setupLoadMore() {
        scrollView.setOnScrollChangeListener { nestedScrollView: NestedScrollView, _, scrollY, _, _ ->
            val content = nestedScrollView.getChildAt(0) ?: return@setOnScrollChangeListener
            val distanceToBottom = content.measuredHeight - nestedScrollView.measuredHeight - scrollY
            if (distanceToBottom <= LOAD_MORE_THRESHOLD_PX) {
                loadMoreStudents()
            }
        }
    }

    private fun setupObservers() {
        viewModel.summary.observe(viewLifecycleOwner) { renderSummary(it) }
        viewModel.students.observe(viewLifecycleOwner) { renderStudents(it) }
        viewModel.isLoadingSummary.observe(viewLifecycleOwner) { updateLoading() }
        viewModel.isLoadingStudents.observe(viewLifecycleOwner) { updateLoading() }
        viewModel.isSearching.observe(viewLifecycleOwner) { searching ->
            searchProgress.visibility = if (searching) View.VISIBLE else View.GONE
            updateLoading()
        }
        viewModel.summaryError.observe(viewLifecycleOwner) { renderSummaryError(it) }
        viewModel.studentsError.observe(viewLifecycleOwner) {
            bindInlineError(searchError, it)
            if (!it.isNullOrBlank()) renderStudentError(it)
        }
        viewModel.actionMessage.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrBlank()) {
                LmsToast.show(requireContext(), message.take(140))
                viewModel.clearActionMessage()
            }
        }
        viewModel.unauthorized.observe(viewLifecycleOwner) { if (it) navigateToUnauthorized() }
    }

    private fun updateLoading() {
        val loading = viewModel.isLoadingSummary.value == true || viewModel.isLoadingStudents.value == true
        topProgress.visibility = if (loading) View.VISIBLE else View.GONE
        swipeRefresh.isRefreshing = loading && viewModel.isSearching.value != true
    }

    private fun renderSummary(summary: FineSummaryData?) {
        if (viewModel.summaryError.value != null && summary == null) return
        summaryContainer.removeAllViews()
        val data = summary ?: FineSummaryData()
        summaryContainer.addView(summaryRow(
            statCard("Total Fines", formatCurrency(data.totalFines), R.drawable.ic_rupee, R.color.my_requests_primary),
            statCard("Collected", formatCurrency(data.collected), R.drawable.ic_check_circle, R.color.my_requests_green)
        ))
        summaryContainer.addView(summaryRow(
            statCard("Pending", formatCurrency(data.pending), R.drawable.ic_clock, R.color.my_requests_red),
            statCard("Waived", formatCurrency(data.waived), R.drawable.ic_shield_check, R.color.my_fines_purple),
            topMargin = 12
        ))
    }

    private fun renderSummaryError(message: String?) {
        if (message.isNullOrBlank()) return
        summaryContainer.removeAllViews()
        val card = defaultStateCard("Unable to load fine summary", message.take(140), R.drawable.ic_warning)
        card.setOnClickListener {
            runIfOnline(NetworkMessages.OFFLINE_RETRY) { viewModel.loadFineSummary() }
        }
        summaryContainer.addView(card)
    }

    private fun renderStudents(students: List<FineStudentSummary>) {
        if (!viewModel.studentsError.value.isNullOrBlank()) return
        allStudents = students
        visibleStudentCount = PAGE_SIZE.coerceAtMost(students.size)
        loadingMoreStudents = false
        loadMoreProgress.visibility = View.GONE
        renderVisibleStudents()
    }

    private fun renderVisibleStudents() {
        studentsContainer.removeAllViews()
        studentsContainer.addView(sectionHeader("Student Fine Records", R.drawable.ic_fine, R.color.my_requests_primary))
        val visibleStudents = allStudents.take(visibleStudentCount)
        when {
            viewModel.isLoadingStudents.value == true && allStudents.isEmpty() -> {
                studentsContainer.addView(defaultStateCard("Loading fine records", "Fetching student-wise fines from backend.", R.drawable.ic_hourglass))
            }
            allStudents.isEmpty() -> {
                studentsContainer.addView(defaultStateCard("No fine records found", "Student fines will appear here when available.", R.drawable.ic_receipt))
            }
            else -> visibleStudents.forEach { studentsContainer.addView(studentFineCard(it)) }
        }
    }

    private fun loadMoreStudents() {
        if (loadingMoreStudents || visibleStudentCount >= allStudents.size) return
        loadingMoreStudents = true
        loadMoreProgress.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            delay(LOAD_MORE_DELAY_MS)
            visibleStudentCount = (visibleStudentCount + PAGE_SIZE).coerceAtMost(allStudents.size)
            loadingMoreStudents = false
            loadMoreProgress.visibility = View.GONE
            renderVisibleStudents()
        }
    }

    private fun renderStudentError(message: String) {
        studentsContainer.removeAllViews()
        val card = defaultStateCard("Unable to load fine records", message.take(140), R.drawable.ic_warning)
        card.setOnClickListener {
            runIfOnline(NetworkMessages.OFFLINE_RETRY) { viewModel.loadFineStudents() }
        }
        studentsContainer.addView(card)
    }

    private fun studentFineCard(student: FineStudentSummary): View {
        val card = baseCard(14)
        val content = vertical(16)
        val header = horizontal()
        header.addView(avatarBox(student.name, student.displayPhoto, 52))
        val titles = vertical()
        titles.addView(text(student.name.orDash(), 17, R.color.student_text_primary, bold = true))
        titles.addView(text("${student.displayIdentifier} | ${student.department.orDash()}", 12, R.color.student_text_muted, topMargin = 3))
        header.addView(titles, weight = 1f)
        header.addView(statusBadge(student.status ?: if (student.pendingAmount > 0) "pending" else "paid"))
        content.addView(header)
        content.addView(metaRow(R.drawable.ic_rupee, "Pending Fine", formatCurrency(student.pendingAmount), R.color.student_danger))
        content.addView(metaRow(R.drawable.ic_receipt, "Total Fine", formatCurrency(student.totalFine), R.color.student_primary, compact = true))
        content.addView(metaRow(R.drawable.ic_check_circle, "Paid / Waived", "${formatCurrency(student.paidAmount)} / ${formatCurrency(student.waivedAmount)}", R.color.student_green, compact = true))
        content.addView(metaRow(R.drawable.ic_clock, "Records", "${student.fineRecordsCount} total | ${student.overdueRecordsCount} overdue", R.color.student_text_muted, compact = true))
        val button = actionText("View Details")
        button.setOnClickListener { (activity as? StaffDashboardActivity)?.openFineDetails(student.studentId) }
        content.addView(button)
        card.addView(content)
        return card
    }

    private fun summaryRow(left: View, right: View, topMargin: Int = 0): LinearLayout =
        horizontal(topMargin = topMargin).apply {
            addView(left, LinearLayout.LayoutParams(0, dp(84), 1f).apply { marginEnd = dp(6) })
            addView(right, LinearLayout.LayoutParams(0, dp(84), 1f).apply { marginStart = dp(6) })
        }

    private fun statCard(title: String, amount: String, icon: Int, tint: Int): View {
        val card = baseCard(12)
        card.cardElevation = dp(2).toFloat()
        card.layoutParams = LinearLayout.LayoutParams(0, dp(84), 1f)
        val content = horizontal()
        content.setPadding(dp(12), 0, dp(10), 0)
        content.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        content.addView(iconBox(icon, tint, 42, 23))
        val column = vertical()
        column.gravity = Gravity.CENTER_VERTICAL
        val amountView = text(amount, 18, R.color.my_requests_text_primary, bold = true)
        amountView.maxLines = 1
        column.addView(amountView)
        val titleView = text(title.uppercase(Locale.US), 10, R.color.my_requests_text_secondary, bold = true, topMargin = 3)
        titleView.letterSpacing = 0.08f
        column.addView(titleView)
        content.addView(column, weight = 1f)
        card.addView(content)
        return card
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

    private fun metaRow(icon: Int, label: String, value: String, tint: Int, compact: Boolean = false): View {
        val row = horizontal(topMargin = if (compact) 8 else 12)
        row.addView(smallIcon(icon, tint))
        row.addView(text(label, if (compact) 11 else 12, R.color.student_text_muted), weight = 0.75f)
        val valueText = text(value, if (compact) 12 else 14, if (tint == R.color.student_text_muted) R.color.student_text_primary else tint, bold = true)
        valueText.gravity = Gravity.END
        row.addView(valueText, weight = 1f)
        return row
    }

    private fun statusBadge(status: String): TextView {
        val normalized = status.lowercase(Locale.US)
        val color = when (normalized) {
            "pending", "overdue" -> R.color.student_danger
            "paid", "no pending" -> R.color.student_green
            "waived" -> R.color.student_purple
            "mixed" -> R.color.student_yellow
            else -> R.color.student_purple
        }
        val label = if (normalized == "no pending") "No Pending" else normalized.titleCase()
        return badge(label, color)
    }

    private fun emptyState(title: String, subtitle: String, icon: Int): View {
        val layout = vertical(14)
        layout.gravity = Gravity.CENTER
        layout.background = rounded(color(R.color.profile_gray_bg), dp(16))
        layout.addView(iconBox(icon, R.color.student_text_muted, 42, 22))
        val titleView = text(title, 14, R.color.student_text_primary, bold = true, topMargin = 8)
        titleView.gravity = Gravity.CENTER
        layout.addView(titleView)
        val subtitleView = text(subtitle, 12, R.color.student_text_muted, topMargin = 3)
        subtitleView.gravity = Gravity.CENTER
        layout.addView(subtitleView)
        return layout
    }

    private fun defaultStateCard(title: String, subtitle: String, icon: Int): View =
        baseCard(14).apply { addView(emptyState(title, subtitle, icon)) }

    private fun actionText(label: String, outline: Boolean = false): TextView =
        TextView(requireContext()).apply {
            text = label
            gravity = Gravity.CENTER
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(color(if (outline) R.color.profile_primary else R.color.white))
            setBackgroundResource(if (outline) R.drawable.bg_profile_outline_button else R.drawable.bg_profile_primary_button)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46)).apply { topMargin = dp(16) }
        }

    private fun iconBox(icon: Int, tint: Int, size: Int = 42, iconSize: Int = 22): FrameLayout {
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

    private fun avatarBox(name: String?, rawUrl: String?, size: Int): FrameLayout {
        val cornerRadius = dp(18)
        val box = FrameLayout(requireContext()).apply {
            background = rounded(color(R.color.profile_blue_bg), cornerRadius)
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

    private fun loadAvatarImage(image: ImageView, initial: TextView, rawUrl: String?) {
        val photoUrl = normalizeMediaUrl(rawUrl)
        image.tag = photoUrl
        if (photoUrl.isNullOrBlank()) {
            image.visibility = View.GONE
            initial.visibility = View.VISIBLE
            return
        }
        thread(name = "fine-student-avatar", isDaemon = true) {
            val bitmap = runCatching {
                URL(photoUrl).openStream().use { BitmapFactory.decodeStream(it) }
            }.onFailure {
                Log.w("StaffFines", "Student photo failed to load: $photoUrl", it)
            }.getOrNull()
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

    private fun normalizeMediaUrl(rawUrl: String?): String? {
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

    private fun baseCard(bottomMargin: Int): CardView =
        CardView(requireContext()).apply {
            radius = dp(18).toFloat()
            cardElevation = dp(2).toFloat()
            setCardBackgroundColor(color(R.color.my_requests_card))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                this.bottomMargin = dp(bottomMargin)
            }
        }

    private fun horizontal(topMargin: Int = 0): LinearLayout =
        LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                this.topMargin = dp(topMargin)
            }
        }

    private fun vertical(padding: Int = 0): LinearLayout =
        LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            if (padding > 0) setPadding(dp(padding), dp(padding), dp(padding), dp(padding))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

    private fun LinearLayout.addView(view: View, weight: Float) {
        addView(view, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight))
    }

    private fun text(value: String, sizeSp: Int, colorRes: Int, bold: Boolean = false, topMargin: Int = 0): TextView =
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

    private fun smallIcon(icon: Int, tint: Int = R.color.student_text_muted): ImageView =
        ImageView(requireContext()).apply {
            setImageResource(icon)
            setColorFilter(color(tint))
            layoutParams = LinearLayout.LayoutParams(dp(18), dp(18)).apply { marginEnd = dp(8) }
        }

    private fun badge(label: String, tint: Int): TextView =
        TextView(requireContext()).apply {
            text = label
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(color(tint))
            background = rounded(badgeBackgroundFor(tint), dp(12))
            setPadding(dp(10), dp(5), dp(10), dp(5))
            gravity = Gravity.CENTER
        }

    private fun rounded(fill: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(fill)
            cornerRadius = radius.toFloat()
        }

    private fun badgeBackgroundFor(tint: Int): Int = when (tint) {
        R.color.student_success, R.color.student_green, R.color.my_requests_green -> color(R.color.my_requests_green_bg)
        R.color.student_danger, R.color.my_requests_red -> color(R.color.my_requests_red_bg)
        R.color.student_yellow, R.color.my_requests_orange -> color(R.color.my_requests_orange_bg)
        R.color.student_purple, R.color.my_fines_purple -> color(R.color.search_badge_purple_bg)
        else -> color(R.color.my_requests_blue_bg)
    }

    private fun formatCurrency(amount: Double): String = "Rs. ${String.format(Locale.US, "%.2f", amount)}"
    private fun color(colorRes: Int): Int = ContextCompat.getColor(requireContext(), colorRes)
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun String?.orDash(): String = if (isNullOrBlank()) "-" else this
    private fun initials(name: String?): String = name.orEmpty().trim().split(Regex("\\s+")).filter { it.isNotBlank() }.take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }.joinToString("").ifBlank { "S" }
    private fun String.titleCase(): String = replace("_", " ").split(" ").filter { it.isNotBlank() }.joinToString(" ") {
        it.lowercase(Locale.US).replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(Locale.US) else char.toString() }
    }
    private fun bindInlineError(view: TextView, message: String?) {
        view.text = message.orEmpty()
        view.visibility = if (message.isNullOrBlank()) View.GONE else View.VISIBLE
    }
    private fun navigateToUnauthorized() {
        if (!isAdded) return
        startActivity(Intent(requireContext(), UnauthorizedActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        requireActivity().finish()
    }

    private companion object {
        const val PAGE_SIZE = 20
        const val LOAD_MORE_DELAY_MS = 1000L
        const val LOAD_MORE_THRESHOLD_PX = 120
    }
}

class StaffFineDetailScreen : Fragment(), OnlineRefreshable {
    private lateinit var viewModel: StaffFineDetailViewModel
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var scrollView: NestedScrollView
    private lateinit var topProgress: ProgressBar
    private lateinit var studentContainer: LinearLayout
    private lateinit var summaryContainer: LinearLayout
    private lateinit var recordsContainer: LinearLayout
    private lateinit var loadMoreProgress: ProgressBar
    private var studentId: Int = 0
    private var allFineRecords: List<FineRecord> = emptyList()
    private var visibleFineRecordCount = DETAIL_PAGE_SIZE
    private var loadingMoreFineRecords = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        studentId = requireArguments().getInt(ARG_STUDENT_ID)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_staff_fine_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupViewModel()
        swipeRefresh = view.findViewById(R.id.staffFineDetailSwipeRefresh)
        scrollView = view.findViewById(R.id.staffFineDetailScrollView)
        topProgress = view.findViewById(R.id.progressFineDetailTop)
        studentContainer = view.findViewById(R.id.containerFineDetailStudent)
        summaryContainer = view.findViewById(R.id.containerFineDetailSummary)
        recordsContainer = view.findViewById(R.id.containerFineRecords)
        loadMoreProgress = view.findViewById(R.id.progressFineRecordsLoadMore)
        view.findViewById<View>(R.id.buttonFineDetailBack).setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        swipeRefresh.setColorSchemeResources(R.color.student_primary, R.color.student_purple, R.color.student_green, R.color.student_yellow)
        swipeRefresh.setOnRefreshListener {
            runIfOnline(onOffline = { swipeRefresh.isRefreshing = false }) { viewModel.refresh() }
        }
        setupLoadMore()
        setupObservers()
        viewModel.loadStudentFineDetails(studentId)
    }

    override fun refreshAfterOnline() {
        viewModel.refresh()
    }

    private fun setupViewModel() {
        val tokenManager = (requireActivity().application as MainApplication).tokenManager
        val apiService = RetrofitClient.getApiService(tokenManager)
        val repository = StaffFinesRepository(apiService, tokenManager)
        viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return StaffFineDetailViewModel(repository) as T
                }
            }
        )[StaffFineDetailViewModel::class.java]
    }

    private fun setupLoadMore() {
        scrollView.setOnScrollChangeListener { nestedScrollView: NestedScrollView, _, scrollY, _, _ ->
            val content = nestedScrollView.getChildAt(0) ?: return@setOnScrollChangeListener
            val distanceToBottom = content.measuredHeight - nestedScrollView.measuredHeight - scrollY
            if (distanceToBottom <= DETAIL_LOAD_MORE_THRESHOLD_PX) {
                loadMoreFineRecords()
            }
        }
    }

    private fun setupObservers() {
        viewModel.student.observe(viewLifecycleOwner) { renderStudentInfo(it, viewModel.summary.value) }
        viewModel.summary.observe(viewLifecycleOwner) {
            renderStudentInfo(viewModel.student.value, it)
            renderSummary(it)
        }
        viewModel.fines.observe(viewLifecycleOwner) { renderRecords(it) }
        viewModel.isLoading.observe(viewLifecycleOwner) { updateLoading() }
        viewModel.isPayingFine.observe(viewLifecycleOwner) { updateLoading() }
        viewModel.isWaivingFine.observe(viewLifecycleOwner) { updateLoading() }
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrBlank()) {
                if (viewModel.student.value == null && viewModel.fines.value.orEmpty().isEmpty()) renderDetailError(message)
                LmsToast.show(requireContext(), message.take(140))
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
        val loading = viewModel.isLoading.value == true || viewModel.isPayingFine.value == true || viewModel.isWaivingFine.value == true
        topProgress.visibility = if (loading) View.VISIBLE else View.GONE
        swipeRefresh.isRefreshing = viewModel.isLoading.value == true
    }

    private fun renderStudentInfo(student: FineStudentInfo?, summary: StudentFineSummary?) {
        studentContainer.removeAllViews()
        if (viewModel.isLoading.value == true && student == null) {
            studentContainer.addView(defaultStateCard("Loading student fines", "Fetching fine detail from backend.", R.drawable.ic_hourglass))
            return
        }
        student ?: return
        val card = baseCard(14)
        val content = vertical(16)
        val header = horizontal()
        header.addView(avatarBox(student.name, student.displayPhoto, 54))
        val titles = vertical()
        titles.addView(text(student.name.orDash(), 18, R.color.student_text_primary, bold = true))
        titles.addView(text("${student.displayIdentifier} | ${student.department.orDash()}", 12, R.color.student_text_muted, topMargin = 3))
        header.addView(titles, weight = 1f)
        content.addView(header)
        content.addView(metaRow(R.drawable.ic_email, "Email", student.email.orDash(), R.color.student_text_muted))
        content.addView(metaRow(R.drawable.ic_receipt, "Total Fine", formatCurrency(summary?.totalFine ?: 0.0), R.color.student_primary))
        content.addView(metaRow(R.drawable.ic_rupee, "Pending Fine", formatCurrency(summary?.pendingAmount ?: 0.0), R.color.student_danger, compact = true))
        content.addView(metaRow(R.drawable.ic_check_circle, "Paid Fine", formatCurrency(summary?.paidAmount ?: 0.0), R.color.student_green, compact = true))
        content.addView(metaRow(R.drawable.ic_shield_check, "Waived Fine", formatCurrency(summary?.waivedAmount ?: 0.0), R.color.student_purple, compact = true))
        card.addView(content)
        studentContainer.addView(card)
    }

    private fun renderSummary(summary: StudentFineSummary?) {
        summaryContainer.removeAllViews()
        val data = summary ?: StudentFineSummary()
        summaryContainer.addView(summaryRow(
            statCard("Total", formatCurrency(data.totalFine), R.drawable.ic_receipt, R.color.my_requests_primary),
            statCard("Pending", formatCurrency(data.pendingAmount), R.drawable.ic_clock, R.color.my_requests_red)
        ))
        summaryContainer.addView(summaryRow(
            statCard("Paid", formatCurrency(data.paidAmount), R.drawable.ic_check_circle, R.color.my_requests_green),
            statCard("Waived", formatCurrency(data.waivedAmount), R.drawable.ic_shield_check, R.color.my_fines_purple),
            topMargin = 12
        ))
    }

    private fun renderRecords(fines: List<FineRecord>) {
        allFineRecords = fines
        visibleFineRecordCount = DETAIL_PAGE_SIZE.coerceAtMost(fines.size)
        loadingMoreFineRecords = false
        loadMoreProgress.visibility = View.GONE
        renderVisibleRecords()
    }

    private fun renderVisibleRecords() {
        recordsContainer.removeAllViews()
        recordsContainer.addView(sectionHeader("Fine Records", R.drawable.ic_receipt, R.color.student_primary))
        val visibleFines = allFineRecords.take(visibleFineRecordCount)
        when {
            viewModel.isLoading.value == true && allFineRecords.isEmpty() -> recordsContainer.addView(defaultStateCard("Loading fine records", "Checking all fines for this student.", R.drawable.ic_hourglass))
            allFineRecords.isEmpty() -> recordsContainer.addView(defaultStateCard("No fine records found", "Student fines will appear here when available.", R.drawable.ic_receipt))
            else -> visibleFines.forEach { recordsContainer.addView(fineRecordCard(it)) }
        }
    }

    private fun loadMoreFineRecords() {
        if (loadingMoreFineRecords || visibleFineRecordCount >= allFineRecords.size) return
        loadingMoreFineRecords = true
        loadMoreProgress.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            delay(DETAIL_LOAD_MORE_DELAY_MS)
            visibleFineRecordCount = (visibleFineRecordCount + DETAIL_PAGE_SIZE).coerceAtMost(allFineRecords.size)
            loadingMoreFineRecords = false
            loadMoreProgress.visibility = View.GONE
            renderVisibleRecords()
        }
    }

    private fun fineRecordCard(fine: FineRecord): View {
        val status = fine.status?.lowercase(Locale.US) ?: "pending"
        val card = baseCard(14)
        val content = vertical(16)
        val header = horizontal()
        header.addView(bookCoverBox(fine.bookTitle, fine.displayCover, 58))
        val titles = vertical()
        titles.addView(text(fine.bookTitle.orDash(), 16, R.color.student_text_primary, bold = true))
        fine.author?.takeIf { it.isNotBlank() }?.let { titles.addView(text(it, 12, R.color.student_text_muted, topMargin = 2)) }
        header.addView(titles, weight = 1f)
        header.addView(statusBadge(status))
        content.addView(header)
        content.addView(metaRow(R.drawable.ic_calendar, "Due Date", formatDate(fine.dueDate), R.color.student_text_muted))
        content.addView(metaRow(R.drawable.ic_refresh, "Return Date", formatDate(fine.returnDate), R.color.student_text_muted, compact = true))
        content.addView(metaRow(R.drawable.ic_clock, "Days Overdue", fine.resolvedDaysOverdue.toString(), R.color.student_danger, compact = true))
        content.addView(metaRow(R.drawable.ic_rupee, "Fine Amount", formatCurrency(fine.amount), if (status == "paid") R.color.student_green else R.color.student_danger, compact = true))
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

    private fun showPayConfirmation(fine: FineRecord) {
        val studentName = viewModel.student.value?.name.orDash()
        AlertDialog.Builder(requireContext())
            .setTitle("Mark Fine as Paid?")
            .setMessage("Record $studentName's payment now?\n\n${formatCurrency(fine.amount)} for \"${fine.bookTitle.orDash()}\" will be marked as paid.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Mark as Paid") { _, _ ->
                runIfOnline(NetworkMessages.OFFLINE_RETRY) { viewModel.markFinePaid(fine.resolvedId) }
            }
            .show()
    }

    private fun showWaiveDialog(fine: FineRecord) {
        val wrapper = vertical(0).apply {
            setPadding(dp(20), dp(20), dp(20), dp(16))
            background = rounded(color(R.color.my_requests_card), dp(18))
        }
        val titleRow = horizontal()
        titleRow.addView(iconBox(R.drawable.ic_shield_check, R.color.my_fines_purple, 42, 22))
        titleRow.addView(text("Waive Fine", 22, R.color.my_requests_text_primary, bold = true), weight = 1f)
        wrapper.addView(titleRow)
        wrapper.addView(text("Add a short reason. This note will be stored with the fine record and shown in activity history.", 13, R.color.my_requests_text_secondary, topMargin = 12))
        val input = EditText(requireContext()).apply {
            hint = "Enter waiver reason..."
            minLines = 4
            gravity = Gravity.TOP
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = roundedStroke(color(R.color.my_requests_card), color(R.color.my_requests_border), dp(14))
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
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
        val dialog = AlertDialog.Builder(requireContext())
            .setView(wrapper)
            .create()
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            cancel.setOnClickListener { dialog.dismiss() }
            waive.setOnClickListener {
                val reason = input.text?.toString().orEmpty().trim()
                if (reason.length < 3) {
                    input.error = "Reason must be at least 3 characters."
                    return@setOnClickListener
                }
                waive.isEnabled = false
                waive.alpha = 0.55f
                waive.text = "Waiving..."
                dialog.dismiss()
                runIfOnline(NetworkMessages.OFFLINE_RETRY) { viewModel.waiveFine(fine.resolvedId, reason) }
            }
        }
        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        dialog.show()
    }

    private fun renderDetailError(message: String) {
        studentContainer.removeAllViews()
        summaryContainer.removeAllViews()
        recordsContainer.removeAllViews()
        val card = defaultStateCard("Unable to load fine details", message.take(140), R.drawable.ic_warning)
        card.setOnClickListener {
            runIfOnline(NetworkMessages.OFFLINE_RETRY) { viewModel.refresh() }
        }
        studentContainer.addView(card)
    }

    private fun summaryRow(left: View, right: View, topMargin: Int = 0): LinearLayout =
        horizontal(topMargin).apply {
            addView(left, LinearLayout.LayoutParams(0, dp(84), 1f).apply { marginEnd = dp(6) })
            addView(right, LinearLayout.LayoutParams(0, dp(84), 1f).apply { marginStart = dp(6) })
        }

    private fun statCard(title: String, amount: String, icon: Int, tint: Int): View {
        val card = baseCard(12)
        val content = horizontal()
        content.setPadding(dp(12), 0, dp(10), 0)
        content.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        content.addView(iconBox(icon, tint, 42, 23))
        val column = vertical()
        column.gravity = Gravity.CENTER_VERTICAL
        val amountView = text(amount, 18, R.color.my_requests_text_primary, bold = true)
        amountView.maxLines = 1
        column.addView(amountView)
        val titleView = text(title.uppercase(Locale.US), 10, R.color.my_requests_text_secondary, bold = true, topMargin = 3)
        titleView.letterSpacing = 0.08f
        column.addView(titleView)
        content.addView(column, weight = 1f)
        card.addView(content)
        return card
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

    private fun metaRow(icon: Int, label: String, value: String, tint: Int, compact: Boolean = false): View {
        val row = horizontal(topMargin = if (compact) 8 else 12)
        row.addView(smallIcon(icon, tint))
        row.addView(text(label, if (compact) 11 else 12, R.color.student_text_muted), weight = 0.72f)
        val valueText = text(value, if (compact) 12 else 13, if (tint == R.color.student_text_muted) R.color.student_text_primary else tint, bold = true)
        valueText.gravity = Gravity.END
        row.addView(valueText, weight = 1f)
        return row
    }

    private fun statusBadge(status: String): TextView {
        val color = when (status.lowercase(Locale.US)) {
            "pending", "overdue" -> R.color.student_danger
            "paid" -> R.color.student_green
            "waived" -> R.color.student_purple
            else -> R.color.student_yellow
        }
        return badge(status.titleCase(), color)
    }

    private fun emptyState(title: String, subtitle: String, icon: Int): View {
        val layout = vertical(14)
        layout.gravity = Gravity.CENTER
        layout.background = rounded(color(R.color.profile_gray_bg), dp(16))
        layout.addView(iconBox(icon, R.color.student_text_muted, 42, 22))
        val titleView = text(title, 14, R.color.student_text_primary, bold = true, topMargin = 8)
        titleView.gravity = Gravity.CENTER
        layout.addView(titleView)
        val subtitleView = text(subtitle, 12, R.color.student_text_muted, topMargin = 3)
        subtitleView.gravity = Gravity.CENTER
        layout.addView(subtitleView)
        return layout
    }

    private fun defaultStateCard(title: String, subtitle: String, icon: Int): View =
        baseCard(14).apply { addView(emptyState(title, subtitle, icon)) }

    private fun actionText(label: String, outline: Boolean = false): TextView =
        TextView(requireContext()).apply {
            text = label
            gravity = Gravity.CENTER
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(color(if (outline) R.color.profile_primary else R.color.white))
            setBackgroundResource(if (outline) R.drawable.bg_profile_outline_button else R.drawable.bg_profile_primary_button)
        }

    private fun iconBox(icon: Int, tint: Int, size: Int = 42, iconSize: Int = 22): FrameLayout {
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

    private fun avatarBox(name: String?, rawUrl: String?, size: Int): FrameLayout {
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

    private fun bookCoverBox(title: String?, rawUrl: String?, size: Int): FrameLayout {
        val box = FrameLayout(requireContext()).apply {
            background = rounded(color(R.color.my_requests_blue_bg), dp(14))
            clipToOutline = true
            layoutParams = LinearLayout.LayoutParams(dp(size), dp(size)).apply { marginEnd = dp(14) }
        }
        val initial = TextView(requireContext()).apply {
            text = title.orEmpty().trim().firstOrNull()?.uppercaseChar()?.toString() ?: "B"
            gravity = Gravity.CENTER
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(color(R.color.my_requests_primary))
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        val image = ImageView(requireContext()).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            visibility = View.GONE
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        box.addView(initial)
        box.addView(image)
        loadCoverImage(image, initial, rawUrl)
        return box
    }

    private fun loadAvatarImage(image: ImageView, initial: TextView, rawUrl: String?) {
        val photoUrl = normalizeMediaUrl(rawUrl)
        image.tag = photoUrl
        if (photoUrl.isNullOrBlank()) {
            image.visibility = View.GONE
            initial.visibility = View.VISIBLE
            return
        }
        thread(name = "fine-detail-avatar", isDaemon = true) {
            val bitmap = runCatching { URL(photoUrl).openStream().use { BitmapFactory.decodeStream(it) } }
                .onFailure { Log.w("StaffFineDetail", "Student photo failed to load: $photoUrl", it) }
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

    private fun loadCoverImage(image: ImageView, initial: TextView, rawUrl: String?) {
        val coverUrl = normalizeMediaUrl(rawUrl)
        image.tag = coverUrl
        if (coverUrl.isNullOrBlank()) {
            image.visibility = View.GONE
            initial.visibility = View.VISIBLE
            return
        }
        thread(name = "fine-book-cover", isDaemon = true) {
            val bitmap = runCatching { URL(coverUrl).openStream().use { BitmapFactory.decodeStream(it) } }
                .onFailure { Log.w("StaffFineDetail", "Book cover failed to load: $coverUrl", it) }
                .getOrNull()
            image.post {
                if (image.tag == coverUrl && bitmap != null) {
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

    private fun normalizeMediaUrl(rawUrl: String?): String? {
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

    private fun baseCard(bottomMargin: Int): CardView =
        CardView(requireContext()).apply {
            radius = dp(18).toFloat()
            cardElevation = dp(2).toFloat()
            setCardBackgroundColor(color(R.color.my_requests_card))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                this.bottomMargin = dp(bottomMargin)
            }
        }

    private fun horizontal(topMargin: Int = 0): LinearLayout =
        LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                this.topMargin = dp(topMargin)
            }
        }

    private fun vertical(padding: Int = 0): LinearLayout =
        LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            if (padding > 0) setPadding(dp(padding), dp(padding), dp(padding), dp(padding))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

    private fun LinearLayout.addView(view: View, weight: Float) {
        addView(view, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight))
    }

    private fun text(value: String, sizeSp: Int, colorRes: Int, bold: Boolean = false, topMargin: Int = 0): TextView =
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

    private fun smallIcon(icon: Int, tint: Int = R.color.student_text_muted): ImageView =
        ImageView(requireContext()).apply {
            setImageResource(icon)
            setColorFilter(color(tint))
            layoutParams = LinearLayout.LayoutParams(dp(18), dp(18)).apply { marginEnd = dp(8) }
        }

    private fun badge(label: String, tint: Int): TextView =
        TextView(requireContext()).apply {
            text = label
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(color(tint))
            background = rounded(badgeBackgroundFor(tint), dp(12))
            setPadding(dp(10), dp(5), dp(10), dp(5))
            gravity = Gravity.CENTER
        }

    private fun rounded(fill: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(fill)
            cornerRadius = radius.toFloat()
        }

    private fun roundedStroke(fill: Int, stroke: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(fill)
            cornerRadius = radius.toFloat()
            setStroke(dp(1), stroke)
        }

    private fun badgeBackgroundFor(tint: Int): Int = when (tint) {
        R.color.student_success, R.color.student_green, R.color.my_requests_green -> color(R.color.my_requests_green_bg)
        R.color.student_danger, R.color.my_requests_red -> color(R.color.my_requests_red_bg)
        R.color.student_yellow, R.color.my_requests_orange -> color(R.color.my_requests_orange_bg)
        R.color.student_purple, R.color.my_fines_purple -> color(R.color.search_badge_purple_bg)
        else -> color(R.color.my_requests_blue_bg)
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
        val patterns = listOf("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd")
        return patterns.firstNotNullOfOrNull { pattern -> runCatching { SimpleDateFormat(pattern, Locale.US).parse(raw) }.getOrNull() }
    }
    private fun color(colorRes: Int): Int = ContextCompat.getColor(requireContext(), colorRes)
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun String?.orDash(): String = if (isNullOrBlank()) "-" else this
    private fun initials(name: String?): String = name.orEmpty().trim().split(Regex("\\s+")).filter { it.isNotBlank() }.take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }.joinToString("").ifBlank { "S" }
    private fun String.titleCase(): String = replace("_", " ").split(" ").filter { it.isNotBlank() }.joinToString(" ") {
        it.lowercase(Locale.US).replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(Locale.US) else char.toString() }
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
        private const val DETAIL_PAGE_SIZE = 20
        private const val DETAIL_LOAD_MORE_DELAY_MS = 1000L
        private const val DETAIL_LOAD_MORE_THRESHOLD_PX = 120
        fun newInstance(studentId: Int): StaffFineDetailScreen =
            StaffFineDetailScreen().apply {
                arguments = Bundle().apply { putInt(ARG_STUDENT_ID, studentId) }
            }
    }
}
