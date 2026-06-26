package com.saroj.lmsmobile.ui.staff.fragments

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.saroj.lmsmobile.MainApplication
import com.saroj.lmsmobile.R
import com.saroj.lmsmobile.api.RetrofitClient
import com.saroj.lmsmobile.data.models.returnbook.ActiveIssueItem
import com.saroj.lmsmobile.data.models.returnbook.ConditionFines
import com.saroj.lmsmobile.data.models.returnbook.ProcessReturnResponse
import com.saroj.lmsmobile.data.models.returnbook.ReturnPreviewData
import com.saroj.lmsmobile.data.models.returnbook.ReturnPreviewItem
import com.saroj.lmsmobile.data.models.returnbook.ReturnRulesData
import com.saroj.lmsmobile.data.models.returnbook.ReturnStudent
import com.saroj.lmsmobile.data.repository.StaffReturnBookRepository
import com.saroj.lmsmobile.ui.common.UnauthorizedActivity
import com.saroj.lmsmobile.ui.staff.StaffDashboardActivity
import com.saroj.lmsmobile.ui.staff.viewmodel.StaffReturnBookViewModel
import com.saroj.lmsmobile.utils.Constants
import com.saroj.lmsmobile.utils.LmsToast
import java.net.URL
import java.util.Locale
import kotlin.concurrent.thread

class StaffReturnBookScreen : Fragment() {
    private lateinit var viewModel: StaffReturnBookViewModel
    private lateinit var studentInput: EditText
    private lateinit var studentClearButton: ImageView
    private lateinit var studentProgress: ProgressBar
    private lateinit var topProgress: ProgressBar
    private lateinit var studentError: TextView
    private lateinit var studentResults: LinearLayout
    private lateinit var rulesContainer: LinearLayout
    private lateinit var studentInfoContainer: LinearLayout
    private lateinit var activeIssuesContainer: LinearLayout
    private lateinit var conditionsContainer: LinearLayout
    private lateinit var finePreviewContainer: LinearLayout
    private lateinit var totalFineContainer: LinearLayout
    private lateinit var processButton: TextView
    private lateinit var swipeRefresh: SwipeRefreshLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_staff_return_book, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupViewModel()
        bindViews(view)
        setupPullToRefresh()
        setupInput()
        setupObservers()
        renderAll()
    }

    private fun setupViewModel() {
        val tokenManager = (requireActivity().application as MainApplication).tokenManager
        val apiService = RetrofitClient.getApiService(tokenManager)
        val repository = StaffReturnBookRepository(apiService, tokenManager)
        viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return StaffReturnBookViewModel(repository) as T
                }
            }
        )[StaffReturnBookViewModel::class.java]
    }

    private fun bindViews(view: View) {
        studentInput = view.findViewById(R.id.inputReturnStudentSearch)
        studentClearButton = view.findViewById(R.id.buttonClearReturnStudentSearch)
        studentProgress = view.findViewById(R.id.progressReturnStudentSearch)
        topProgress = view.findViewById(R.id.progressReturnTop)
        studentError = view.findViewById(R.id.textReturnStudentSearchError)
        studentResults = view.findViewById(R.id.containerReturnStudentResults)
        rulesContainer = view.findViewById(R.id.containerReturnRules)
        studentInfoContainer = view.findViewById(R.id.containerReturnStudentInfo)
        activeIssuesContainer = view.findViewById(R.id.containerActiveIssues)
        conditionsContainer = view.findViewById(R.id.containerReturnConditions)
        finePreviewContainer = view.findViewById(R.id.containerFinePreview)
        totalFineContainer = view.findViewById(R.id.containerTotalFine)
        processButton = view.findViewById(R.id.buttonProcessReturn)
        swipeRefresh = view.findViewById(R.id.returnBookSwipeRefresh)
        processButton.setOnClickListener { showConfirmDialog() }
    }

    private fun setupPullToRefresh() {
        swipeRefresh.setColorSchemeResources(
            R.color.student_primary,
            R.color.student_purple,
            R.color.student_green,
            R.color.student_yellow
        )
        swipeRefresh.setOnRefreshListener { viewModel.refreshStudentReturnData() }
    }

    private fun setupInput() {
        studentClearButton.setOnClickListener { studentInput.setText("") }
        studentInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                studentClearButton.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
                viewModel.searchStudents(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        studentInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                viewModel.searchStudents(studentInput.text?.toString().orEmpty())
                true
            } else {
                false
            }
        }
    }

    private fun setupObservers() {
        viewModel.returnSettings.observe(viewLifecycleOwner) { renderRules() }
        viewModel.returnRules.observe(viewLifecycleOwner) { renderRules() }
        viewModel.studentSearchResults.observe(viewLifecycleOwner) { renderStudentResults(it) }
        viewModel.selectedStudent.observe(viewLifecycleOwner) {
            if (it == null && studentInput.text.isNotEmpty()) studentInput.setText("")
            renderAll()
        }
        viewModel.activeIssues.observe(viewLifecycleOwner) { renderAll() }
        viewModel.selectedIssueIds.observe(viewLifecycleOwner) { renderAll() }
        viewModel.selectedCondition.observe(viewLifecycleOwner) { renderAll() }
        viewModel.finePreview.observe(viewLifecycleOwner) { renderAll() }
        viewModel.isLoadingReturnSettings.observe(viewLifecycleOwner) {
            updateLoading()
            renderRules()
        }
        viewModel.isSearchingStudents.observe(viewLifecycleOwner) {
            studentProgress.visibility = if (it) View.VISIBLE else View.GONE
            updateLoading()
        }
        viewModel.isLoadingStudentReturnData.observe(viewLifecycleOwner) {
            updateLoading()
            renderStudentInfo()
            renderActiveIssues()
        }
        viewModel.isPreviewingFine.observe(viewLifecycleOwner) {
            updateLoading()
            renderFinePreview()
            updateProcessButton()
        }
        viewModel.isProcessingReturn.observe(viewLifecycleOwner) {
            updateLoading()
            updateProcessButton()
        }
        viewModel.settingsError.observe(viewLifecycleOwner) { renderRules() }
        viewModel.studentSearchError.observe(viewLifecycleOwner) { bindInlineError(studentError, it) }
        viewModel.studentReturnDataError.observe(viewLifecycleOwner) {
            renderStudentInfo()
            renderActiveIssues()
        }
        viewModel.previewError.observe(viewLifecycleOwner) {
            renderFinePreview()
            updateProcessButton()
        }
        viewModel.actionMessage.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrBlank()) {
                LmsToast.show(requireContext(), message.take(140))
                viewModel.clearActionMessage()
            }
        }
        viewModel.returnSuccess.observe(viewLifecycleOwner) { response ->
            response?.let {
                showSuccessDialog(it)
                viewModel.clearReturnSuccess()
            }
        }
        viewModel.unauthorized.observe(viewLifecycleOwner) { unauthorized ->
            if (unauthorized) navigateToUnauthorized()
        }
    }

    private fun updateLoading() {
        val loading = viewModel.isLoadingReturnSettings.value == true ||
            viewModel.isLoadingStudentReturnData.value == true ||
            viewModel.isPreviewingFine.value == true ||
            viewModel.isProcessingReturn.value == true
        topProgress.visibility = if (loading) View.VISIBLE else View.GONE
        swipeRefresh.isRefreshing = viewModel.isLoadingStudentReturnData.value == true ||
            viewModel.isLoadingReturnSettings.value == true
    }

    private fun renderAll() {
        renderRules()
        renderStudentInfo()
        renderActiveIssues()
        renderConditions()
        renderFinePreview()
        renderTotalFine()
        updateProcessButton()
    }

    private fun renderRules() {
        rulesContainer.removeAllViews()
        if (viewModel.isLoadingReturnSettings.value == true && viewModel.selectedStudent.value == null) {
            rulesContainer.addView(defaultStateCard("Loading return rules", "Fetching library return settings from backend.", R.drawable.ic_hourglass))
            return
        }
        val error = viewModel.settingsError.value
        if (!error.isNullOrBlank() && viewModel.returnRules.value == null) {
            val card = defaultStateCard("Unable to load return rules", error.take(140), R.drawable.ic_warning)
            card.setOnClickListener { viewModel.loadReturnSettings() }
            rulesContainer.addView(card)
            return
        }
        val rules = viewModel.returnRules.value
        if (rules == null) {
            rulesContainer.addView(defaultStateCard("Return rules unavailable", "Backend settings will appear here when available.", R.drawable.ic_info))
            return
        }
        val title = if (viewModel.selectedStudent.value == null) "Default Return Rules" else "Effective Return Rules"
        val card = baseCard(14)
        val content = vertical(16)
        val header = horizontal()
        header.addView(sectionHeader(title, R.drawable.ic_shield_check, R.color.student_primary), weight = 1f)
        rules.source?.takeIf { it.isNotBlank() }?.let { header.addView(badge(it.titleCase(), R.color.student_purple)) }
        content.addView(header)
        if (rules.hasCustomRules) {
            content.addView(infoCard("Custom privilege rules are applied for this borrower."))
        }
        val grid = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(10)
            }
        }
        grid.addView(ruleRow(
            ruleMiniCard(R.drawable.ic_calendar, "Issue Duration", rules.issueDurationDays?.let { "$it days" } ?: "-", R.color.student_primary),
            ruleMiniCard(R.drawable.ic_rupee, "Late Fine", rules.lateFinePerDay?.let(::formatCurrency) ?: "-", R.color.student_danger)
        ))
        grid.addView(ruleRow(
            ruleMiniCard(R.drawable.ic_clock, "Grace Period", rules.graceDays?.let { "$it days" } ?: "-", R.color.student_green),
            ruleMiniCard(R.drawable.ic_shield, "Max Fine Per Book", rules.fineCapPerBook?.let(::formatCurrency) ?: "-", R.color.student_purple)
        ))
        grid.addView(ruleRow(
            ruleMiniCard(R.drawable.ic_info, "Fair Fine", rules.conditionFines.fair?.let(::formatCurrency) ?: "-", R.color.student_yellow),
            ruleMiniCard(R.drawable.ic_warning, "Damaged Fine", rules.conditionFines.damaged?.let(::formatCurrency) ?: "-", R.color.student_danger)
        ))
        grid.addView(ruleRow(
            ruleMiniCard(R.drawable.ic_x_circle, "Lost Fine", rules.conditionFines.lost?.let(::formatCurrency) ?: "-", R.color.student_danger),
            ruleMiniCard(R.drawable.ic_check_circle, "Good Fine", rules.conditionFines.good?.let(::formatCurrency) ?: "-", R.color.student_green)
        ))
        content.addView(grid)
        card.addView(content)
        rulesContainer.addView(card)
    }

    private fun renderStudentResults(students: List<ReturnStudent>) {
        studentResults.removeAllViews()
        val query = studentInput.text?.toString().orEmpty().trim()
        if (students.isEmpty()) {
            if (query.length >= 2 && viewModel.isSearchingStudents.value != true) {
                studentResults.addView(emptyState("No students found", "Try another name, ID, or email.", R.drawable.ic_student))
            }
            return
        }
        students.forEach { student -> studentResults.addView(studentResultCard(student)) }
    }

    private fun renderStudentInfo() {
        studentInfoContainer.removeAllViews()
        val student = viewModel.selectedStudent.value
        if (student == null) {
            studentInfoContainer.addView(defaultStateCard("Select a student first", "Student information and active issued books will appear here.", R.drawable.ic_student))
            return
        }
        if (viewModel.isLoadingStudentReturnData.value == true) {
            studentInfoContainer.addView(defaultStateCard("Loading student return data", "Fetching active issues and effective rules.", R.drawable.ic_hourglass))
            return
        }
        val error = viewModel.studentReturnDataError.value
        if (!error.isNullOrBlank()) {
            val card = defaultStateCard("Unable to load student return data", error.take(140), R.drawable.ic_warning)
            card.setOnClickListener { viewModel.refreshStudentReturnData() }
            studentInfoContainer.addView(card)
            return
        }

        val card = baseCard(14)
        val content = vertical(16)
        val header = horizontal()
        header.addView(avatarBox(student, 52))
        val heading = vertical()
        heading.addView(text("Student Information", 18, R.color.student_text_primary, bold = true))
        heading.addView(text(student.name, 13, R.color.student_text_muted))
        header.addView(heading, weight = 1f)
        content.addView(header)
        content.addView(metaRow(R.drawable.ic_user, "Name", student.name))
        content.addView(metaRow(R.drawable.ic_id_card, "Student ID", student.displayIdentifier.ifBlank { "-" }))
        content.addView(metaRow(R.drawable.ic_building, "Department", student.department.orDash()))
        content.addView(metaRow(R.drawable.ic_email, "Email", student.email.orDash()))
        content.addView(metaRow(R.drawable.ic_books, "Books Issued", (student.activeIssuesCount ?: viewModel.activeIssues.value.orEmpty().size).toString()))
        val clear = actionText("Clear / Change Student", outline = true)
        clear.setOnClickListener { viewModel.clearSelectedStudent() }
        content.addView(clear)
        card.addView(content)
        studentInfoContainer.addView(card)
    }

    private fun renderActiveIssues() {
        activeIssuesContainer.removeAllViews()
        val student = viewModel.selectedStudent.value
        if (student == null) return
        val card = baseCard(14)
        val content = vertical(16)
        val issues = viewModel.activeIssues.value.orEmpty()
        val selected = viewModel.selectedIssueIds.value.orEmpty()
        val header = horizontal()
        header.addView(sectionHeader("Active Issued Books", R.drawable.ic_books, R.color.student_green), weight = 1f)
        header.addView(badge("${selected.size} of ${issues.size} selected", R.color.student_primary))
        content.addView(header)
        if (issues.size > 1) {
            val selectAll = actionText(if (selected.size == issues.size) "Clear Selection" else "Select All", outline = true)
            selectAll.setOnClickListener { viewModel.selectAllIssues() }
            content.addView(selectAll)
        }
        when {
            viewModel.isLoadingStudentReturnData.value == true -> {
                content.addView(emptyState("Loading active issued books", "Checking books this student can return.", R.drawable.ic_hourglass))
            }
            issues.isEmpty() -> {
                content.addView(emptyState("No active issued books", "This student has no books to return.", R.drawable.ic_check_circle))
            }
            else -> issues.forEach { issue ->
                content.addView(activeIssueCard(issue, selected.contains(issue.issueId)))
            }
        }
        card.addView(content)
        activeIssuesContainer.addView(card)
    }

    private fun renderConditions() {
        conditionsContainer.removeAllViews()
        if (viewModel.selectedIssueIds.value.orEmpty().isEmpty()) return
        val rules = viewModel.returnRules.value
        val selected = viewModel.selectedCondition.value
        val card = baseCard(14)
        val content = vertical(16)
        content.addView(sectionHeader("Book Condition & Fine", R.drawable.ic_warning, R.color.student_yellow))
        content.addView(conditionRow(
            conditionCard("good", "Good", R.drawable.ic_check_circle, R.color.student_green, rules?.conditionFines, selected),
            conditionCard("fair", "Fair", R.drawable.ic_info, R.color.student_yellow, rules?.conditionFines, selected)
        ))
        content.addView(conditionRow(
            conditionCard("damaged", "Damaged", R.drawable.ic_warning, R.color.student_danger, rules?.conditionFines, selected),
            conditionCard("lost", "Lost", R.drawable.ic_x_circle, R.color.student_danger, rules?.conditionFines, selected)
        ))
        card.addView(content)
        conditionsContainer.addView(card)
    }

    private fun renderFinePreview() {
        finePreviewContainer.removeAllViews()
        if (viewModel.selectedIssueIds.value.orEmpty().isEmpty() || viewModel.selectedCondition.value.isNullOrBlank()) return
        val card = baseCard(14)
        val content = vertical(16)
        content.addView(sectionHeader("Fine Preview", R.drawable.ic_receipt, R.color.student_primary))
        when {
            viewModel.isPreviewingFine.value == true -> content.addView(emptyState("Calculating fine...", "Backend is calculating late and condition fines.", R.drawable.ic_hourglass))
            !viewModel.previewError.value.isNullOrBlank() -> content.addView(warningCard(viewModel.previewError.value.orEmpty().take(180)))
            viewModel.finePreview.value == null -> content.addView(emptyState("Fine preview required", "Select a condition to fetch backend-calculated fine.", R.drawable.ic_rupee))
            else -> bindFinePreview(content, viewModel.finePreview.value!!)
        }
        card.addView(content)
        finePreviewContainer.addView(card)
    }

    private fun bindFinePreview(content: LinearLayout, preview: ReturnPreviewData) {
        content.addView(metaRow(R.drawable.ic_rupee, "Total Fine", formatCurrency(preview.totalFine)))
        content.addView(metaRow(R.drawable.ic_books, "Selected Books", preview.selectedCount.toString()))
        content.addView(metaRow(R.drawable.ic_warning, "Condition", preview.condition.orDash().titleCase()))
        preview.summary?.takeIf { it.isNotBlank() }?.let { content.addView(infoCard(it)) }
        if (preview.items.isNotEmpty()) {
            content.addView(text("Calculation Details", 15, R.color.student_text_primary, bold = true, topMargin = 14))
            preview.items.forEach { content.addView(previewItemCard(it)) }
        }
    }

    private fun renderTotalFine() {
        totalFineContainer.removeAllViews()
        val preview = viewModel.finePreview.value
        val count = viewModel.selectedIssueIds.value.orEmpty().size
        val condition = viewModel.selectedCondition.value
        val card = baseCard(14)
        val content = vertical(16)
        content.background = rounded(color(R.color.profile_blue_bg), dp(18))
        content.addView(sectionHeader("TOTAL FINE", R.drawable.ic_rupee, R.color.student_danger))
        content.addView(text(formatCurrency(preview?.totalFine ?: 0.0), 28, R.color.student_text_primary, bold = true, topMargin = 8))
        val subtitle = if (preview == null) {
            "Select books and a return condition to preview the payable fine."
        } else {
            "$count selected book(s) with ${condition.orDash().titleCase()} condition."
        }
        content.addView(text(subtitle, 13, R.color.student_text_secondary, topMargin = 4))
        card.addView(content)
        totalFineContainer.addView(card)
    }

    private fun updateProcessButton() {
        val count = viewModel.selectedIssueIds.value.orEmpty().size
        val processing = viewModel.isProcessingReturn.value == true
        val enabled = viewModel.selectedStudent.value != null &&
            count > 0 &&
            !viewModel.selectedCondition.value.isNullOrBlank() &&
            viewModel.finePreview.value != null &&
            viewModel.previewError.value.isNullOrBlank() &&
            !processing &&
            viewModel.isPreviewingFine.value != true
        processButton.text = when {
            processing -> "Processing..."
            count == 1 -> "Process Return (1 book)"
            count > 1 -> "Process Return ($count books)"
            else -> "Process Return"
        }
        processButton.isEnabled = enabled
        processButton.alpha = if (enabled) 1f else 0.55f
    }

    private fun showConfirmDialog() {
        val student = viewModel.selectedStudent.value ?: return
        val count = viewModel.selectedIssueIds.value.orEmpty().size
        val condition = viewModel.selectedCondition.value.orDash().titleCase()
        val total = formatCurrency(viewModel.finePreview.value?.totalFine ?: 0.0)
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Return")
            .setMessage("Return $count book(s) from ${student.name}?\n\nCondition: $condition\nTotal Fine: $total\nSelected books: $count")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Confirm Return") { _, _ -> viewModel.processReturn() }
            .show()
    }

    private fun showSuccessDialog(response: ProcessReturnResponse) {
        val data = response.data
        val message = response.message?.takeIf { it.isNotBlank() } ?: "Book return processed successfully."
        AlertDialog.Builder(requireContext())
            .setTitle("Return Successful")
            .setMessage("$message\n\nReturned: ${data?.returnedCount ?: 0}\nTotal Fine: ${formatCurrency(data?.totalFine ?: 0.0)}")
            .setNegativeButton("Go to Dashboard") { _, _ -> (activity as? StaffDashboardActivity)?.openDashboard() }
            .setPositiveButton("Return More", null)
            .show()
    }

    private fun studentResultCard(student: ReturnStudent): View {
        val row = vertical(14)
        row.background = rounded(color(R.color.profile_gray_bg), dp(16))
        row.setOnClickListener {
            studentInput.setText("")
            viewModel.selectStudent(student)
        }
        row.addView(text(student.name, 15, R.color.student_text_primary, bold = true))
        row.addView(metaRow(R.drawable.ic_id_card, "ID / Roll", student.displayIdentifier.ifBlank { "-" }, compact = true))
        row.addView(metaRow(R.drawable.ic_building, "Department", student.department.orDash(), compact = true))
        row.addView(metaRow(R.drawable.ic_email, "Email", student.email.orDash(), compact = true))
        row.addView(metaRow(R.drawable.ic_books, "Active Issued", student.activeIssuesCount?.toString() ?: "-", compact = true))
        return row.apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(10)
            }
        }
    }

    private fun activeIssueCard(issue: ActiveIssueItem, checked: Boolean): View {
        val card = vertical(12)
        card.background = roundedStroke(
            if (checked) color(R.color.profile_blue_bg) else color(R.color.profile_gray_bg),
            if (checked) color(R.color.student_primary) else Color.TRANSPARENT,
            dp(16)
        )
        val header = horizontal()
        val checkbox = CheckBox(requireContext()).apply {
            isChecked = checked
            setOnClickListener { viewModel.toggleIssueSelection(issue.issueId) }
        }
        header.addView(checkbox, LinearLayout.LayoutParams(dp(42), dp(42)))
        val titleColumn = vertical()
        titleColumn.addView(text(issue.title, 15, R.color.student_text_primary, bold = true))
        titleColumn.addView(text(issue.author.orDash(), 12, R.color.student_text_muted))
        header.addView(titleColumn, weight = 1f)
        header.addView(statusBadge(issue))
        card.addView(header)
        card.addView(metaRow(R.drawable.ic_calendar, "Issued Date", issue.issuedDate.orDash(), compact = true))
        card.addView(metaRow(R.drawable.ic_calendar, "Due Date", issue.dueDate.orDash(), compact = true))
        issue.overdueDays?.let {
            if (it > 0) card.addView(metaRow(R.drawable.ic_warning, "Overdue Days", it.toString(), compact = true))
        }
        issue.estimatedLateFine?.let {
            card.addView(metaRow(R.drawable.ic_rupee, "Estimated Late Fine", formatCurrency(it), compact = true))
        }
        card.setOnClickListener { viewModel.toggleIssueSelection(issue.issueId) }
        return card.apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(12)
            }
        }
    }

    private fun conditionCard(key: String, label: String, icon: Int, tint: Int, fines: ConditionFines?, selected: String?): View {
        val isSelected = selected.equals(key, ignoreCase = true)
        val amount = fines?.amountFor(key)
        val card = vertical(12)
        card.gravity = Gravity.CENTER
        card.background = roundedStroke(
            if (isSelected) color(R.color.profile_blue_bg) else color(R.color.profile_gray_bg),
            if (isSelected) color(R.color.student_primary) else Color.TRANSPARENT,
            dp(16)
        )
        card.addView(iconBox(icon, tint, 40, 21))
        val title = text(label, 14, R.color.student_text_primary, bold = true)
        title.gravity = Gravity.CENTER
        card.addView(title)
        val valueText = when {
            key == "good" -> "No Fine"
            amount != null -> "${formatCurrency(amount)} Fine"
            else -> "-"
        }
        val value = text(valueText, 12, R.color.student_text_muted)
        value.gravity = Gravity.CENTER
        card.addView(value)
        card.setOnClickListener { viewModel.selectCondition(key) }
        return card
    }

    private fun previewItemCard(item: ReturnPreviewItem): View {
        val card = vertical(12)
        card.background = rounded(color(R.color.profile_gray_bg), dp(16))
        card.addView(text(item.bookTitle, 14, R.color.student_text_primary, bold = true))
        card.addView(metaRow(R.drawable.ic_calendar, "Issued Date", item.issuedDate.orDash(), compact = true))
        card.addView(metaRow(R.drawable.ic_calendar, "Due Date", item.dueDate.orDash(), compact = true))
        card.addView(metaRow(R.drawable.ic_warning, "Overdue Days", item.overdueDays?.toString() ?: "-", compact = true))
        card.addView(metaRow(R.drawable.ic_clock, "Grace Deduction", item.graceDays?.toString() ?: "-", compact = true))
        card.addView(metaRow(R.drawable.ic_clock, "Chargeable Days", item.chargeableOverdueDays?.toString() ?: "-", compact = true))
        card.addView(metaRow(R.drawable.ic_rupee, "Late Fine", item.lateFine?.let(::formatCurrency) ?: "-", compact = true))
        card.addView(metaRow(R.drawable.ic_rupee, "Condition Fine", item.conditionFine?.let(::formatCurrency) ?: "-", compact = true))
        card.addView(metaRow(R.drawable.ic_receipt, "Book Total", item.bookTotal?.let(::formatCurrency) ?: "-", compact = true))
        return card.apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(10)
            }
        }
    }

    private fun ruleRow(left: View, right: View): LinearLayout =
        horizontal(topMargin = 8).apply {
            addView(left, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(5) })
            addView(right, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(5) })
        }

    private fun conditionRow(left: View, right: View): LinearLayout =
        horizontal(topMargin = 12).apply {
            addView(left, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(5) })
            addView(right, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(5) })
        }

    private fun ruleMiniCard(icon: Int, label: String, value: String, tint: Int): View =
        LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(10))
            background = rounded(color(R.color.profile_gray_bg), dp(14))
            addView(iconBox(icon, tint, 34, 17))
            val column = vertical()
            column.addView(text(label, 11, R.color.student_text_muted))
            column.addView(text(value, 13, R.color.student_text_primary, bold = true))
            addView(column, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }

    private fun statusBadge(issue: ActiveIssueItem): TextView {
        val overdue = issue.overdueDays ?: 0
        val label = when {
            overdue > 0 -> "Overdue"
            issue.status.equals("due_today", ignoreCase = true) -> "Due Today"
            else -> issue.status?.titleCase()?.takeIf { it.isNotBlank() } ?: "On Time"
        }
        val color = when {
            overdue > 0 -> R.color.student_danger
            label.equals("Due Today", ignoreCase = true) -> R.color.student_yellow
            else -> R.color.student_green
        }
        return badge(label, color)
    }

    private fun sectionHeader(title: String, icon: Int, tint: Int): LinearLayout {
        val row = horizontal()
        row.addView(iconBox(icon, tint, 38, 20))
        row.addView(text(title, 18, R.color.student_text_primary, bold = true), weight = 1f)
        return row
    }

    private fun metaRow(icon: Int, label: String, value: String, compact: Boolean = false): View {
        val row = horizontal(topMargin = if (compact) 8 else 12)
        row.addView(smallIcon(icon))
        row.addView(text(label, if (compact) 11 else 12, R.color.student_text_muted), weight = 0.78f)
        val valueText = text(value, if (compact) 12 else 13, R.color.student_text_primary, bold = !compact)
        valueText.gravity = Gravity.END
        row.addView(valueText, weight = 1f)
        return row
    }

    private fun warningCard(message: String): View =
        LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = roundedStroke(color(R.color.profile_red_bg), color(R.color.student_danger), dp(14))
            addView(smallIcon(R.drawable.ic_warning, R.color.student_danger))
            addView(text(message, 13, R.color.student_danger, bold = true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(12)
            }
        }

    private fun infoCard(message: String): View =
        LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = roundedStroke(color(R.color.profile_blue_bg), color(R.color.student_primary), dp(14))
            addView(smallIcon(R.drawable.ic_info, R.color.student_primary))
            addView(text(message, 13, R.color.student_primary, bold = true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(12)
            }
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

    private fun defaultStateCard(title: String, subtitle: String, icon: Int): View {
        val card = baseCard(14)
        card.addView(emptyState(title, subtitle, icon))
        return card
    }

    private fun actionText(label: String, outline: Boolean = false): TextView =
        TextView(requireContext()).apply {
            text = label
            gravity = Gravity.CENTER
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(color(if (outline) R.color.profile_primary else R.color.white))
            setBackgroundResource(if (outline) R.drawable.bg_profile_outline_button else R.drawable.bg_profile_primary_button)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46)).apply {
                topMargin = dp(16)
            }
        }

    private fun iconBox(icon: Int, tint: Int, size: Int = 42, iconSize: Int = 22): FrameLayout {
        val box = FrameLayout(requireContext()).apply {
            background = rounded(color(R.color.profile_blue_bg), dp(14))
            layoutParams = LinearLayout.LayoutParams(dp(size), dp(size)).apply {
                marginEnd = dp(10)
            }
        }
        box.addView(ImageView(requireContext()).apply {
            setImageResource(icon)
            setColorFilter(color(tint))
            layoutParams = FrameLayout.LayoutParams(dp(iconSize), dp(iconSize), Gravity.CENTER)
        })
        return box
    }

    private fun avatarBox(student: ReturnStudent, size: Int = 44): FrameLayout {
        val cornerRadius = if (size >= 52) dp(18) else dp(15)
        val box = FrameLayout(requireContext()).apply {
            background = rounded(color(R.color.profile_blue_bg), cornerRadius)
            clipToOutline = true
            layoutParams = LinearLayout.LayoutParams(dp(size), dp(size)).apply {
                marginEnd = dp(10)
            }
        }
        val initial = TextView(requireContext()).apply {
            text = student.name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "S"
            gravity = Gravity.CENTER
            textSize = if (size >= 52) 18f else 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(color(R.color.student_primary))
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        val image = ImageView(requireContext()).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            visibility = View.GONE
            background = rounded(Color.TRANSPARENT, cornerRadius)
            clipToOutline = true
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        box.addView(initial)
        box.addView(image)
        loadAvatarImage(image, initial, student.profilePhotoUrl)
        return box
    }

    private fun loadAvatarImage(image: ImageView, initial: TextView, rawUrl: String?) {
        val photoUrl = normalizeProfilePhotoUrl(rawUrl)
        image.tag = photoUrl
        if (photoUrl.isNullOrBlank()) {
            image.visibility = View.GONE
            initial.visibility = View.VISIBLE
            return
        }

        thread(name = "return-student-avatar", isDaemon = true) {
            val bitmap = runCatching {
                URL(photoUrl).openStream().use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }.onFailure {
                Log.w("StaffReturnBook", "Profile photo failed to load: $photoUrl", it)
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

    private fun normalizeProfilePhotoUrl(rawUrl: String?): String? {
        val value = rawUrl?.trim()?.takeIf { it.isNotBlank() } ?: return null
        if (value.startsWith("data:", ignoreCase = true)) return null
        if (!value.startsWith("http", ignoreCase = true)) {
            return apiRootUrl() + "/" + normalizeStoragePath(value)
        }
        val apiRoot = apiRootUrl()
        return value
            .let { Constants.normalizeLaravelAssetUrl(it) ?: it }
    }

    private fun normalizeStoragePath(rawPath: String): String {
        val path = rawPath
            .trim()
            .trimStart('/')
            .removePrefix("public/")
            .removePrefix("storage/app/public/")
            .removePrefix("app/public/")
        return if (path.startsWith("storage/", ignoreCase = true)) path else "storage/$path"
    }

    private fun apiRootUrl(): String =
        Constants.BASE_URL.removeSuffix("api/").trimEnd('/')

    private fun smallIcon(icon: Int, tint: Int = R.color.student_text_muted): ImageView =
        ImageView(requireContext()).apply {
            setImageResource(icon)
            setColorFilter(color(tint))
            layoutParams = LinearLayout.LayoutParams(dp(18), dp(18)).apply {
                marginEnd = dp(8)
            }
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

    private fun baseCard(bottomMargin: Int): CardView =
        CardView(requireContext()).apply {
            radius = dp(20).toFloat()
            cardElevation = dp(2).toFloat()
            setCardBackgroundColor(color(R.color.student_card))
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

    private fun text(
        value: String,
        sizeSp: Int,
        colorRes: Int,
        bold: Boolean = false,
        topMargin: Int = 0
    ): TextView =
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

    private fun bindInlineError(view: TextView, message: String?) {
        view.text = message.orEmpty()
        view.visibility = if (message.isNullOrBlank()) View.GONE else View.VISIBLE
    }

    private fun badgeBackgroundFor(tint: Int): Int = when (tint) {
        R.color.student_success, R.color.student_green -> color(R.color.profile_green_bg)
        R.color.student_danger -> color(R.color.profile_red_bg)
        R.color.student_yellow -> color(R.color.my_books_yellow_bg)
        R.color.student_purple -> color(R.color.search_badge_purple_bg)
        else -> color(R.color.profile_blue_bg)
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
        }

    private fun roundedStroke(fill: Int, stroke: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(fill)
            cornerRadius = radius.toFloat()
            if (stroke != Color.TRANSPARENT) setStroke(dp(1), stroke)
        }

    private fun formatCurrency(amount: Double): String = "Rs. ${String.format(Locale.US, "%.2f", amount)}"

    private fun color(colorRes: Int): Int = ContextCompat.getColor(requireContext(), colorRes)

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun String?.orDash(): String = if (isNullOrBlank()) "-" else this

    private fun String.titleCase(): String =
        replace("_", " ").split(" ").filter { it.isNotBlank() }.joinToString(" ") {
            it.lowercase(Locale.US).replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.US) else char.toString()
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
}
