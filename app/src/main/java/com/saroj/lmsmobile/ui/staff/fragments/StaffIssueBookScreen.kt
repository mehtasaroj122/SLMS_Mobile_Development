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
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.saroj.lmsmobile.MainApplication
import com.saroj.lmsmobile.R
import com.saroj.lmsmobile.api.RetrofitClient
import com.saroj.lmsmobile.data.models.issue.IssueBookItem
import com.saroj.lmsmobile.data.models.issue.IssuePrivilegesData
import com.saroj.lmsmobile.data.models.issue.IssueStudent
import com.saroj.lmsmobile.data.repository.StaffIssueBookRepository
import com.saroj.lmsmobile.ui.common.UnauthorizedActivity
import com.saroj.lmsmobile.ui.staff.viewmodel.StaffIssueBookViewModel
import com.saroj.lmsmobile.utils.Constants
import com.saroj.lmsmobile.utils.LmsToast
import java.net.URL
import kotlin.concurrent.thread

class StaffIssueBookScreen : Fragment() {
    private lateinit var viewModel: StaffIssueBookViewModel
    private lateinit var studentInput: EditText
    private lateinit var bookInput: EditText
    private lateinit var studentClearButton: ImageView
    private lateinit var bookClearButton: ImageView
    private lateinit var studentProgress: ProgressBar
    private lateinit var bookProgress: ProgressBar
    private lateinit var studentLoadMoreProgress: ProgressBar
    private lateinit var bookLoadMoreProgress: ProgressBar
    private lateinit var topProgress: ProgressBar
    private lateinit var studentError: TextView
    private lateinit var bookError: TextView
    private lateinit var studentResults: LinearLayout
    private lateinit var bookResults: LinearLayout
    private lateinit var studentInfoContainer: LinearLayout
    private lateinit var privilegesContainer: LinearLayout
    private lateinit var selectedBooksContainer: LinearLayout
    private lateinit var summaryContainer: LinearLayout
    private lateinit var issueButton: TextView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var scrollView: NestedScrollView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_staff_issue_book, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupViewModel()
        bindViews(view)
        setupPullToRefresh()
        setupInputs()
        setupLoadMoreScroll()
        setupObservers()
        renderAll()
    }

    private fun setupViewModel() {
        val tokenManager = (requireActivity().application as MainApplication).tokenManager
        val apiService = RetrofitClient.getApiService(tokenManager)
        val repository = StaffIssueBookRepository(apiService, tokenManager)
        viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return StaffIssueBookViewModel(repository) as T
                }
            }
        )[StaffIssueBookViewModel::class.java]
    }

    private fun bindViews(view: View) {
        studentInput = view.findViewById(R.id.inputStudentSearch)
        bookInput = view.findViewById(R.id.inputBookSearch)
        studentClearButton = view.findViewById(R.id.buttonClearStudentSearch)
        bookClearButton = view.findViewById(R.id.buttonClearBookSearch)
        studentProgress = view.findViewById(R.id.progressStudentSearch)
        bookProgress = view.findViewById(R.id.progressBookSearch)
        studentLoadMoreProgress = view.findViewById(R.id.progressStudentSearchLoadMore)
        bookLoadMoreProgress = view.findViewById(R.id.progressBookSearchLoadMore)
        topProgress = view.findViewById(R.id.progressIssueTop)
        studentError = view.findViewById(R.id.textStudentSearchError)
        bookError = view.findViewById(R.id.textBookSearchError)
        studentResults = view.findViewById(R.id.containerStudentResults)
        bookResults = view.findViewById(R.id.containerBookResults)
        studentInfoContainer = view.findViewById(R.id.containerStudentInfo)
        privilegesContainer = view.findViewById(R.id.containerPrivileges)
        selectedBooksContainer = view.findViewById(R.id.containerSelectedBooks)
        summaryContainer = view.findViewById(R.id.containerIssueSummary)
        issueButton = view.findViewById(R.id.buttonIssueBooks)
        swipeRefresh = view.findViewById(R.id.issueBookSwipeRefresh)
        scrollView = view.findViewById(R.id.issueBookScrollView)
        issueButton.setOnClickListener { showConfirmDialog() }
    }

    private fun setupPullToRefresh() {
        swipeRefresh.setColorSchemeResources(
            R.color.student_primary,
            R.color.student_purple,
            R.color.student_green,
            R.color.student_yellow
        )
        swipeRefresh.setOnRefreshListener {
            val studentQuery = studentInput.text?.toString().orEmpty()
            val bookQuery = bookInput.text?.toString().orEmpty()
            if (viewModel.selectedStudent.value == null && studentQuery.trim().length < 2 && bookQuery.trim().length < 2) {
                swipeRefresh.isRefreshing = false
                LmsToast.show(requireContext(), "Search or select a student first.")
                return@setOnRefreshListener
            }
            viewModel.refreshPage(
                studentQuery = studentQuery,
                bookQuery = bookQuery
            )
        }
    }

    private fun setupInputs() {
        studentClearButton.setOnClickListener { studentInput.setText("") }
        bookClearButton.setOnClickListener { bookInput.setText("") }
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

        bookInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                bookClearButton.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
                viewModel.searchBooks(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        bookInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                viewModel.searchBooks(bookInput.text?.toString().orEmpty())
                true
            } else {
                false
            }
        }
    }

    private fun setupLoadMoreScroll() {
        scrollView.setOnScrollChangeListener { nestedScrollView: NestedScrollView, _, scrollY, _, _ ->
            val content = nestedScrollView.getChildAt(0) ?: return@setOnScrollChangeListener
            val distanceFromBottom = content.measuredHeight - nestedScrollView.measuredHeight - scrollY
            if (distanceFromBottom > dp(96)) return@setOnScrollChangeListener

            if (viewModel.selectedStudent.value == null) {
                viewModel.loadMoreStudents()
            } else {
                viewModel.loadMoreBooks()
            }
        }
    }

    private fun setupObservers() {
        viewModel.studentSearchResults.observe(viewLifecycleOwner) { renderStudentResults(it) }
        viewModel.bookSearchResults.observe(viewLifecycleOwner) { renderBookResults(it) }
        viewModel.selectedStudent.observe(viewLifecycleOwner) {
            if (it == null && studentInput.text.isNotEmpty()) studentInput.setText("")
            renderAll()
        }
        viewModel.issuePrivileges.observe(viewLifecycleOwner) { renderAll() }
        viewModel.selectedBooks.observe(viewLifecycleOwner) {
            renderAll()
            renderBookResults(viewModel.bookSearchResults.value.orEmpty())
        }
        viewModel.isSearchingStudents.observe(viewLifecycleOwner) {
            studentProgress.visibility = if (it) View.VISIBLE else View.GONE
            updateRefreshState()
        }
        viewModel.isSearchingBooks.observe(viewLifecycleOwner) {
            bookProgress.visibility = if (it) View.VISIBLE else View.GONE
            updateRefreshState()
        }
        viewModel.isLoadingPrivileges.observe(viewLifecycleOwner) {
            topProgress.visibility = if (it) View.VISIBLE else View.GONE
            renderPrivileges()
            updateRefreshState()
        }
        viewModel.isIssuing.observe(viewLifecycleOwner) {
            topProgress.visibility = if (it) View.VISIBLE else View.GONE
            updateIssueButton()
            updateRefreshState()
        }
        viewModel.studentSearchError.observe(viewLifecycleOwner) {
            bindInlineError(studentError, it)
        }
        viewModel.bookSearchError.observe(viewLifecycleOwner) {
            bindInlineError(bookError, it)
        }
        viewModel.privilegeError.observe(viewLifecycleOwner) { renderPrivileges() }
        viewModel.actionMessage.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrBlank()) {
                LmsToast.show(requireContext(), message.take(140))
                viewModel.clearActionMessage()
            }
        }
        viewModel.issueSuccess.observe(viewLifecycleOwner) { response ->
            response?.let {
                val message = it.message?.takeIf { value -> value.isNotBlank() } ?: "Books issued successfully."
                bookInput.setText("")
                studentInput.setText("")
                LmsToast.show(requireContext(), message.take(140))
                viewModel.clearIssueSuccess()
            }
        }
        viewModel.unauthorized.observe(viewLifecycleOwner) { unauthorized ->
            if (unauthorized) navigateToUnauthorized()
        }
    }

    private fun updateRefreshState() {
        swipeRefresh.isRefreshing =
            viewModel.isSearchingStudents.value == true ||
                viewModel.isSearchingBooks.value == true ||
                viewModel.isLoadingPrivileges.value == true
    }

    private fun renderAll() {
        renderStudentInfo()
        renderPrivileges()
        renderSelectedBooks()
        renderIssueSummary()
        updateBookSearchEnabled()
        updateIssueButton()
    }

    private fun renderStudentResults(students: List<IssueStudent>) {
        studentResults.removeAllViews()
        val query = studentInput.text?.toString().orEmpty().trim()
        if (students.isEmpty()) {
            if (query.length >= 2 && viewModel.isSearchingStudents.value != true) {
                studentResults.addView(emptyState("No students found", "Try another name, ID, or email.", R.drawable.ic_student))
            }
            return
        }

        students.forEach { student ->
            studentResults.addView(studentResultCard(student))
        }
    }

    private fun renderStudentInfo() {
        studentInfoContainer.removeAllViews()
        val student = viewModel.selectedStudent.value
        if (student == null) {
            studentInfoContainer.addView(defaultStateCard("Select a student first", "Student information and issue privileges will appear here.", R.drawable.ic_student))
            return
        }

        val privileges = viewModel.issuePrivileges.value
        val card = baseCard(bottomMargin = 14)
        val content = vertical(padding = 16)
        val header = horizontal()
        header.addView(avatarBox(student, size = 52))
        val headerText = vertical()
        headerText.addView(text("Student Information", 18, R.color.student_text_primary, bold = true))
        headerText.addView(text(student.name, 13, R.color.student_text_muted))
        header.addView(headerText, weight = 1f)
        content.addView(header)
        content.addView(metaRow(R.drawable.ic_user, "Name", student.name))
        content.addView(metaRow(R.drawable.ic_id_card, "Student ID", (student.studentId ?: student.displayIdentifier).ifBlank { "-" }))
        content.addView(metaRow(R.drawable.ic_building, "Department", student.department.orDash()))
        content.addView(metaRow(R.drawable.ic_email, "Email", student.email.orDash()))

        val alreadyIssued = privileges?.alreadyIssued ?: student.currentIssued
        val maxBooks = privileges?.maxBooks ?: student.maxBooks
        val issuedText = if (alreadyIssued != null && maxBooks != null) "$alreadyIssued / $maxBooks" else "-"
        content.addView(metaRow(R.drawable.ic_books, "Books Issued", issuedText))
        if (alreadyIssued != null && maxBooks != null && maxBooks > 0) {
            content.addView(progressBarRow(alreadyIssued, maxBooks))
        }

        val button = actionText("Clear / Change Student", outline = true)
        button.setOnClickListener {
            bookInput.setText("")
            viewModel.clearSelectedStudent()
        }
        content.addView(button)
        card.addView(content)
        studentInfoContainer.addView(card)
    }

    private fun renderPrivileges() {
        privilegesContainer.removeAllViews()
        val student = viewModel.selectedStudent.value
        if (student == null) return

        if (viewModel.isLoadingPrivileges.value == true) {
            privilegesContainer.addView(defaultStateCard("Loading issue privileges", "Checking backend limits and due date settings.", R.drawable.ic_hourglass))
            return
        }

        val error = viewModel.privilegeError.value
        if (!error.isNullOrBlank()) {
            val card = defaultStateCard("Unable to load issue privileges", error.take(140), R.drawable.ic_warning)
            card.setOnClickListener { viewModel.refreshStudentPrivileges() }
            privilegesContainer.addView(card)
            return
        }

        val privileges = viewModel.issuePrivileges.value
        if (privileges == null) {
            privilegesContainer.addView(defaultStateCard("Issue privileges unavailable", "Select a student to load current backend rules.", R.drawable.ic_info))
            return
        }

        val card = baseCard(bottomMargin = 14)
        val content = vertical(padding = 16)
        val header = horizontal()
        header.addView(sectionHeader("Issue Privileges", R.drawable.ic_shield_check, R.color.student_green), weight = 1f)
        header.addView(badge(if (privileges.allowed) "Allowed" else "Not Allowed", if (privileges.allowed) R.color.student_success else R.color.student_danger))
        content.addView(header)

        if (!privileges.allowed) {
            content.addView(warningCard(privileges.reason ?: "This student is not eligible to issue books."))
        } else if (privileges.backendValidated && !privileges.settingsAvailable) {
            content.addView(infoCard(privileges.reason ?: "Backend will validate final issue rules."))
        }

        content.addView(statGrid(privileges))
        content.addView(slotIndicator(privileges.slotLimit, viewModel.selectedBooks.value.orEmpty().size))
        card.addView(content)
        privilegesContainer.addView(card)
    }

    private fun renderBookResults(books: List<IssueBookItem>) {
        bookResults.removeAllViews()
        val query = bookInput.text?.toString().orEmpty().trim()
        if (books.isEmpty()) {
            if (query.length >= 2 && viewModel.isSearchingBooks.value != true) {
                bookResults.addView(emptyState("No books found", "Try another title, author, ISBN, or accession number.", R.drawable.ic_book))
            }
            return
        }

        books.forEach { book ->
            bookResults.addView(bookResultCard(book))
        }
    }

    private fun renderSelectedBooks() {
        selectedBooksContainer.removeAllViews()
        val books = viewModel.selectedBooks.value.orEmpty()
        val limit = viewModel.issuePrivileges.value?.slotLimit ?: 0
        val card = baseCard(bottomMargin = 14)
        val content = vertical(padding = 16)
        content.addView(sectionHeader("Selected Books ${books.size}/$limit", R.drawable.ic_book_add, R.color.student_purple))

        if (books.isEmpty()) {
            content.addView(emptyState("No books selected", "Search and select books to issue.", R.drawable.ic_book_add))
        } else {
            books.forEach { book ->
                content.addView(selectedBookRow(book))
            }
        }

        card.addView(content)
        selectedBooksContainer.addView(card)
    }

    private fun renderIssueSummary() {
        summaryContainer.removeAllViews()
        val student = viewModel.selectedStudent.value
        val privileges = viewModel.issuePrivileges.value
        val books = viewModel.selectedBooks.value.orEmpty()
        val card = baseCard(bottomMargin = 14)
        val content = vertical(padding = 16)
        content.addView(sectionHeader("Issue Summary", R.drawable.ic_clipboard, R.color.student_primary))
        content.addView(metaRow(R.drawable.ic_student, "Student Name", student?.name ?: "-"))
        content.addView(metaRow(R.drawable.ic_books, "Total Selected Books", books.size.toString()))
        content.addView(metaRow(R.drawable.ic_calendar, "Issue Date", privileges?.issueDate.orDash()))
        content.addView(metaRow(R.drawable.ic_calendar, "Due Date", privileges?.dueDate.orDash()))
        content.addView(metaRow(R.drawable.ic_clock, "Duration", privileges?.durationDays?.let { "$it days" } ?: "-"))
        content.addView(metaRow(R.drawable.ic_rupee, "Fine Rate", privileges?.fineRate.orDash()))
        card.addView(content)
        summaryContainer.addView(card)
    }

    private fun updateBookSearchEnabled() {
        val studentSelected = viewModel.selectedStudent.value != null
        val privileges = viewModel.issuePrivileges.value
        val enabled = studentSelected && privileges?.allowed == true && privileges.slotLimit > 0 && viewModel.isLoadingPrivileges.value != true
        bookInput.isEnabled = enabled
        bookInput.hint = when {
            !studentSelected -> "Select a student first"
            privileges == null -> "Loading issue privileges"
            !privileges.allowed -> "Issue not allowed for this student"
            privileges.slotLimit <= 0 -> "Book issue limit reached"
            else -> "Search books by title, author, ISBN, accession no, or category"
        }
    }

    private fun updateIssueButton() {
        val privileges = viewModel.issuePrivileges.value
        val selectedCount = viewModel.selectedBooks.value.orEmpty().size
        val limit = privileges?.slotLimit ?: 0
        val issuing = viewModel.isIssuing.value == true
        val enabled = viewModel.selectedStudent.value != null &&
            privileges?.allowed == true &&
            selectedCount > 0 &&
            selectedCount <= limit &&
            !issuing
        issueButton.text = if (issuing) "Issuing..." else "Issue Books ($selectedCount/$limit)"
        issueButton.isEnabled = enabled
        issueButton.alpha = if (enabled) 1f else 0.55f
    }

    private fun showConfirmDialog() {
        val student = viewModel.selectedStudent.value ?: return
        val privileges = viewModel.issuePrivileges.value
        val count = viewModel.selectedBooks.value.orEmpty().size
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Issue")
            .setMessage(
                buildString {
                    append("Issue $count book(s) to ${student.name}?")
                    privileges?.dueDate?.takeIf { it.isNotBlank() }?.let {
                        append("\n\nDue date: $it")
                    }
                }
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Confirm Issue") { _, _ -> viewModel.issueSelectedBooks() }
            .show()
    }

    private fun studentResultCard(student: IssueStudent): View {
        val card = baseCard(bottomMargin = 10)
        val content = vertical(padding = 14)
        val titleRow = horizontal()
        titleRow.addView(avatarBox(student, size = 44))
        val titleColumn = vertical()
        titleColumn.addView(text(student.name, 15, R.color.student_text_primary, bold = true))
        titleColumn.addView(text(student.displayIdentifier.ifBlank { "ID not available" }, 12, R.color.student_text_muted))
        titleRow.addView(titleColumn, weight = 1f)
        student.status?.takeIf { it.isNotBlank() }?.let {
            titleRow.addView(badge(it.titleCase(), statusColor(it)))
        }
        content.addView(titleRow)
        content.addView(metaRow(R.drawable.ic_building, "Department", student.department.orDash(), compact = true))
        content.addView(metaRow(R.drawable.ic_email, "Email", student.email.orDash(), compact = true))
        card.addView(content)
        card.setOnClickListener {
            studentInput.setText("")
            viewModel.selectStudent(student)
        }
        return card
    }

    private fun bookResultCard(book: IssueBookItem): View {
        val selected = viewModel.selectedBooks.value.orEmpty().any { it.requestId == book.requestId }
        val overLimit = viewModel.selectedBooks.value.orEmpty().size >= (viewModel.issuePrivileges.value?.slotLimit ?: 0)
        val canTap = book.canSelect && !selected && !overLimit
        val card = baseCard(bottomMargin = 10)
        card.alpha = if (book.canSelect && !selected) 1f else 0.68f
        val content = vertical(padding = 14)
        val titleRow = horizontal()
        val bookTint = when {
            selected -> R.color.student_purple
            book.isAlreadyIssued -> R.color.student_yellow
            book.canSelect -> R.color.student_success
            else -> R.color.student_danger
        }
        titleRow.addView(bookCoverBox(book, bookTint))
        val titleColumn = vertical()
        titleColumn.addView(text(book.title, 15, R.color.student_text_primary, bold = true))
        titleColumn.addView(text(book.author.orDash(), 12, R.color.student_text_muted))
        titleRow.addView(titleColumn, weight = 1f)
        titleRow.addView(
            badge(
                when {
                    selected -> "Selected"
                    book.isAlreadyIssued -> "Already Issued"
                    book.canSelect -> "Available"
                    else -> "Unavailable"
                },
                when {
                    selected -> R.color.student_purple
                    book.isAlreadyIssued -> R.color.student_yellow
                    book.canSelect -> R.color.student_success
                    else -> R.color.student_danger
                }
            )
        )
        content.addView(titleRow)
        content.addView(metaRow(R.drawable.ic_barcode, "ISBN / Accession No.", listOfNotNull(book.isbn, book.accessionNo).firstOrNull().orDash(), compact = true))
        content.addView(metaRow(R.drawable.ic_category, "Category", book.category.orDash(), compact = true))
        content.addView(metaRow(R.drawable.ic_check_circle, "Available Copies", book.availableCopies?.toString() ?: "-", compact = true))
        card.addView(content)
        card.isEnabled = book.canSelect && !selected
        card.setOnClickListener {
            if (overLimit && !selected) {
                LmsToast.show(requireContext(), "Book issue limit reached.")
            } else {
                viewModel.selectBook(book)
            }
        }
        return card
    }

    private fun selectedBookRow(book: IssueBookItem): View {
        val row = horizontal()
        row.setPadding(0, dp(12), 0, 0)
        row.addView(bookCoverBox(book, R.color.student_primary))
        val detail = vertical()
        detail.addView(text(book.title, 14, R.color.student_text_primary, bold = true))
        detail.addView(text(listOfNotNull(book.author, book.category, book.accessionNo).firstOrNull().orDash(), 12, R.color.student_text_muted))
        row.addView(detail, weight = 1f)

        val remove = TextView(requireContext()).apply {
            text = "Remove"
            setTextColor(color(R.color.student_danger))
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(8), dp(10), dp(8))
            setOnClickListener { viewModel.removeSelectedBook(book) }
        }
        row.addView(remove)
        return row
    }

    private fun statGrid(privileges: IssuePrivilegesData): View {
        val wrapper = vertical()
        val first = horizontal(topMargin = 12)
        first.addView(statMini("Max Books", privileges.maxBooks?.toString() ?: "-", R.drawable.ic_books, R.color.student_primary), weight = 1f)
        first.addView(statMini("Already Issued", privileges.alreadyIssued?.toString() ?: "-", R.drawable.ic_read, R.color.student_purple), weight = 1f)
        wrapper.addView(first)
        val second = horizontal(topMargin = 10)
        second.addView(statMini("Can Issue", privileges.canIssue?.toString() ?: "-", R.drawable.ic_book_add, R.color.student_green), weight = 1f)
        second.addView(statMini("Duration", privileges.durationDays?.let { "$it days" } ?: "-", R.drawable.ic_clock, R.color.student_green), weight = 1f)
        wrapper.addView(second)
        val third = horizontal(topMargin = 10)
        third.addView(statMini("Fine Rate", privileges.fineRate.orDash(), R.drawable.ic_rupee, R.color.student_yellow), weight = 1f)
        third.addView(statMini("Due Date", privileges.dueDate.orDash(), R.drawable.ic_calendar, R.color.student_primary), weight = 1f)
        wrapper.addView(third)
        return wrapper
    }

    private fun statMini(label: String, value: String, icon: Int, tint: Int): View {
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(color(R.color.profile_gray_bg), dp(14))
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }
        card.addView(iconBox(icon, tint, size = 36, iconSize = 18))
        val column = vertical()
        column.addView(text(label, 11, R.color.student_text_muted))
        column.addView(text(value, 14, R.color.student_text_primary, bold = true))
        card.addView(column, weight = 1f)
        return card
    }

    private fun slotIndicator(limit: Int, selectedCount: Int): View {
        val wrapper = vertical()
        wrapper.addView(text("Book Selection Slots $selectedCount/$limit", 13, R.color.student_text_primary, bold = true, topMargin = 14))
        val scroll = HorizontalScrollView(requireContext()).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(10)
            }
        }
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        if (limit <= 0) {
            row.addView(text("No slots available", 12, R.color.student_danger, bold = true))
        } else {
            repeat(limit) { index ->
                val filled = index < selectedCount
                val slot = TextView(requireContext()).apply {
                    text = (index + 1).toString()
                    gravity = Gravity.CENTER
                    textSize = 13f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(if (filled) color(R.color.white) else color(R.color.student_primary))
                    background = if (filled) {
                        rounded(color(R.color.student_primary), dp(14))
                    } else {
                        roundedStroke(Color.TRANSPARENT, color(R.color.student_primary), dp(14))
                    }
                }
                row.addView(slot, LinearLayout.LayoutParams(dp(34), dp(34)).apply {
                    marginEnd = dp(8)
                })
            }
        }
        scroll.addView(row)
        wrapper.addView(scroll)
        return wrapper
    }

    private fun progressBarRow(current: Int, max: Int): View {
        val progress = ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal).apply {
            this.max = max
            this.progress = current.coerceIn(0, max)
            progressDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.progress_usage_drawable)
        }
        return progress.apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(8)).apply {
                topMargin = dp(8)
                bottomMargin = dp(12)
            }
        }
    }

    private fun sectionHeader(title: String, icon: Int, tint: Int): LinearLayout {
        val row = horizontal()
        row.addView(iconBox(icon, tint, size = 38, iconSize = 20))
        row.addView(text(title, 18, R.color.student_text_primary, bold = true), weight = 1f)
        return row
    }

    private fun metaRow(icon: Int, label: String, value: String, compact: Boolean = false): View {
        val row = horizontal(topMargin = if (compact) 8 else 12)
        row.addView(smallIcon(icon))
        val labelText = text(label, if (compact) 11 else 12, R.color.student_text_muted)
        row.addView(labelText, weight = 0.75f)
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
            addView(text(message, 13, R.color.student_danger, bold = true), weight = 1f)
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
            addView(text(message, 13, R.color.student_primary, bold = true), weight = 1f)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(12)
            }
        }

    private fun emptyState(title: String, subtitle: String, icon: Int): View {
        val layout = vertical(padding = 14)
        layout.gravity = Gravity.CENTER
        layout.background = rounded(color(R.color.profile_gray_bg), dp(16))
        layout.addView(iconBox(icon, R.color.student_text_muted, size = 42, iconSize = 22))
        val titleView = text(title, 14, R.color.student_text_primary, bold = true, topMargin = 8)
        titleView.gravity = Gravity.CENTER
        layout.addView(titleView)
        val subtitleView = text(subtitle, 12, R.color.student_text_muted, topMargin = 3)
        subtitleView.gravity = Gravity.CENTER
        layout.addView(subtitleView)
        return layout
    }

    private fun defaultStateCard(title: String, subtitle: String, icon: Int): View {
        val card = baseCard(bottomMargin = 14)
        val content = emptyState(title, subtitle, icon)
        card.addView(content)
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

    private fun avatarBox(student: IssueStudent, size: Int = 44): FrameLayout {
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

    private fun bookCoverBox(book: IssueBookItem, tint: Int, size: Int = 46): FrameLayout {
        val cornerRadius = dp(14)
        val box = FrameLayout(requireContext()).apply {
            background = rounded(color(R.color.profile_blue_bg), cornerRadius)
            clipToOutline = true
            layoutParams = LinearLayout.LayoutParams(dp(size), dp(size)).apply {
                marginEnd = dp(10)
            }
        }
        val initial = TextView(requireContext()).apply {
            text = book.title.trim().split(Regex("\\s+")).firstOrNull()
                ?.take(4)
                ?.uppercase()
                ?: "BOOK"
            gravity = Gravity.CENTER
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(color(tint))
            maxLines = 1
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
        loadBookCoverImage(image, initial, book.coverImageUrl)
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

        thread(name = "issue-student-avatar", isDaemon = true) {
            val bitmap = runCatching {
                URL(photoUrl).openStream().use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }.onFailure {
                Log.w("StaffIssueBook", "Profile photo failed to load: $photoUrl", it)
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
            .replace("http://127.0.0.1:8000", apiRoot)
            .replace("http://localhost:8000", apiRoot)
            .replace("https://127.0.0.1:8000", apiRoot)
            .replace("https://localhost:8000", apiRoot)
    }

    private fun loadBookCoverImage(image: ImageView, initial: TextView, rawUrl: String?) {
        val coverUrl = normalizeCoverUrl(rawUrl)
        image.tag = coverUrl
        if (coverUrl.isNullOrBlank()) {
            image.visibility = View.GONE
            initial.visibility = View.VISIBLE
            return
        }

        thread(name = "issue-book-cover", isDaemon = true) {
            val bitmap = runCatching {
                URL(coverUrl).openStream().use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }.onFailure {
                Log.w("StaffIssueBook", "Book cover failed to load: $coverUrl", it)
            }.getOrNull()

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

    private fun normalizeCoverUrl(rawUrl: String?): String? {
        val value = rawUrl?.trim()?.takeIf { it.isNotBlank() } ?: return null
        if (value.startsWith("data:", ignoreCase = true)) return null
        if (!value.startsWith("http", ignoreCase = true)) {
            return apiRootUrl() + "/" + normalizeStoragePath(value)
        }
        val apiRoot = apiRootUrl()
        return value
            .replace("http://127.0.0.1:8000", apiRoot)
            .replace("http://localhost:8000", apiRoot)
            .replace("https://127.0.0.1:8000", apiRoot)
            .replace("https://localhost:8000", apiRoot)
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

    private fun statusColor(status: String): Int = when (status.lowercase()) {
        "active", "allowed", "available" -> R.color.student_success
        "inactive", "blocked", "suspended", "not allowed" -> R.color.student_danger
        "pending", "limited" -> R.color.student_yellow
        else -> R.color.student_purple
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
            setStroke(dp(1), stroke)
        }

    private fun color(colorRes: Int): Int = ContextCompat.getColor(requireContext(), colorRes)

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun String?.orDash(): String = if (isNullOrBlank()) "-" else this

    private fun String.titleCase(): String =
        replace("_", " ").split(" ").filter { it.isNotBlank() }.joinToString(" ") {
            it.lowercase().replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase() else char.toString()
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
