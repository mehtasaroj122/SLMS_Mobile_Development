package com.saroj.lmsmobile.ui.student

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.saroj.lmsmobile.R
import com.saroj.lmsmobile.api.RetrofitClient
import com.saroj.lmsmobile.data.models.common.NetworkResult
import com.saroj.lmsmobile.data.repository.NotificationRepository
import com.saroj.lmsmobile.ui.components.BaseActivity
import com.saroj.lmsmobile.ui.student.fragments.StudentDashboardFragment
import com.saroj.lmsmobile.ui.student.fragments.StudentMyRequestsFragment
import com.saroj.lmsmobile.ui.student.fragments.StudentMyBooksFragment
import com.saroj.lmsmobile.ui.student.fragments.StudentMyFinesFragment
import com.saroj.lmsmobile.ui.student.fragments.StudentProfileFragment
import com.saroj.lmsmobile.ui.student.fragments.StudentMoreFragment
import com.saroj.lmsmobile.ui.student.fragments.StudentSearchBooksFragment
import com.saroj.lmsmobile.ui.student.notifications.StudentNotificationsFragment
import com.saroj.lmsmobile.ui.theme.DarkModeToggleBinder
import com.saroj.lmsmobile.utils.NotificationRefreshBus
import kotlinx.coroutines.launch

/**
 * StudentDashboardActivity is the main activity for student users.
 *
 * Features:
 * - Bottom navigation to switch between modules
 * - Fragment-based navigation for main modules
 * - Dashboard, Search Books, My Books, My Fines, and More menu sections
 * - Consistent student portal header
 *
 * Navigation:
 * BottomNavigationView switches between fragments:
 * - Dashboard (statistics and summaries)
 * - Search Books (search and browse available books)
 * - My Books (view currently borrowed books)
 * - My Fines (view fines and dues)
 * - More (opens account and library options)
 *
 * Architecture:
 * Activity holds fragments and manages bottom navigation.
 * Each fragment has its own ViewModel and Repository.
 */
class StudentDashboardActivity : BaseActivity() {

    private lateinit var bottomNavigation: BottomNavigationView
    private var lastSelectedNavItemId = R.id.nav_dashboard

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_dashboard)
        lastSelectedNavItemId = savedInstanceState?.getInt(KEY_LAST_SELECTED_NAV_ITEM)
            ?: R.id.nav_dashboard

        // Initialize views
        initializeViews()
        setupStudentHeader()
        setupHeaderNotifications()

        // Set up navigation
        setupBottomNavigation()
        setupBackStackNavigation()
        if (savedInstanceState != null) {
            markBottomNavItemChecked(lastSelectedNavItemId)
        }

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(StudentDashboardFragment())
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean = false

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_LAST_SELECTED_NAV_ITEM, lastSelectedNavItemId)
        super.onSaveInstanceState(outState)
    }

    /**
     * Initializes UI components.
     */
    private fun initializeViews() {
        bottomNavigation = findViewById(R.id.bottomNavigation)
    }

    @Suppress("DEPRECATION")
    private fun setupStudentHeader() {
        window.statusBarColor = ContextCompat.getColor(this, R.color.student_portal_header_start)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        val header = findViewById<View>(R.id.studentPortalHeader)
        val initialHeaderLeft = header.paddingLeft
        val initialHeaderTop = header.paddingTop
        val initialHeaderRight = header.paddingRight
        val initialHeaderBottom = header.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(header) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                initialHeaderLeft,
                initialHeaderTop + systemBars.top,
                initialHeaderRight,
                initialHeaderBottom
            )
            insets
        }

        val initialNavLeft = bottomNavigation.paddingLeft
        val initialNavTop = bottomNavigation.paddingTop
        val initialNavRight = bottomNavigation.paddingRight
        val initialNavBottom = bottomNavigation.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigation) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                initialNavLeft,
                initialNavTop,
                initialNavRight,
                initialNavBottom + systemBars.bottom
            )
            insets
        }

        ViewCompat.requestApplyInsets(header)
        ViewCompat.requestApplyInsets(bottomNavigation)
    }

    private fun setupHeaderNotifications() {
        findViewById<View>(R.id.buttonHeaderNotifications)?.setOnClickListener {
            openNotifications()
        }
        loadHeaderNotificationCount()
        activityScope.launch {
            NotificationRefreshBus.events.collect {
                loadHeaderNotificationCount()
            }
        }
    }

    private fun loadHeaderNotificationCount() {
        val apiService = RetrofitClient.getApiService(tokenManager)
        val repository = NotificationRepository(apiService, tokenManager)
        activityScope.launch {
            repository.getNotificationCount().collect { result ->
                when (result) {
                    is NetworkResult.Success -> updateHeaderNotificationBadge(result.data.unreadCount ?: 0)
                    is NetworkResult.Error,
                    is NetworkResult.Unauthorized -> updateHeaderNotificationBadge(0)
                    is NetworkResult.Loading -> Unit
                }
            }
        }
    }

    private fun updateHeaderNotificationBadge(unreadCount: Int) {
        findViewById<TextView>(R.id.textHeaderNotificationBadge)?.apply {
            text = if (unreadCount > 99) "99+" else unreadCount.toString()
            visibility = if (unreadCount > 0) View.VISIBLE else View.GONE
        }
    }

    /**
     * Sets up bottom navigation listener.
     */
    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_dashboard -> {
                    lastSelectedNavItemId = menuItem.itemId
                    loadFragment(StudentDashboardFragment())
                    true
                }
                R.id.nav_search_books -> {
                    lastSelectedNavItemId = menuItem.itemId
                    loadFragment(StudentSearchBooksFragment())
                    true
                }
                R.id.nav_my_books -> {
                    lastSelectedNavItemId = menuItem.itemId
                    loadFragment(StudentMyBooksFragment())
                    true
                }
                R.id.nav_my_fines -> {
                    lastSelectedNavItemId = menuItem.itemId
                    loadFragment(StudentMyFinesFragment())
                    true
                }
                R.id.nav_more -> {
                    lastSelectedNavItemId = menuItem.itemId
                    loadFragment(StudentMoreFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun setupBackStackNavigation() {
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                markBottomNavItemChecked(lastSelectedNavItemId)
            }
        }
    }

    /**
     * Opens the More menu as a bottom sheet without replacing the current fragment.
     */
    private fun showMoreBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_student_more, null)
        var openedSecondaryScreen = false
        dialog.setContentView(view)
        updateMoreNotificationsBadge(view)
        DarkModeToggleBinder.bind(view, this, activityScope)

        view.findViewById<View>(R.id.rowProfile).setOnClickListener {
            openedSecondaryScreen = true
            dialog.dismiss()
            openProfile()
        }

        view.findViewById<View>(R.id.rowNotifications).setOnClickListener {
            openedSecondaryScreen = true
            dialog.dismiss()
            openNotifications()
        }

        view.findViewById<View>(R.id.rowMyRequests).setOnClickListener {
            openedSecondaryScreen = true
            dialog.dismiss()
            openMyRequests()
        }

        view.findViewById<View>(R.id.rowLogout).setOnClickListener {
            dialog.dismiss()
            showLogoutConfirmation()
        }

        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                ?.setBackgroundColor(Color.TRANSPARENT)
        }

        dialog.setOnDismissListener {
            if (!openedSecondaryScreen) {
                markBottomNavItemChecked(lastSelectedNavItemId)
            }
        }

        dialog.show()
    }

    private fun updateMoreNotificationsBadge(view: View) {
        val badge = view.findViewById<TextView>(R.id.textMoreNotificationsBadge)
        badge.visibility = View.GONE

        val apiService = RetrofitClient.getApiService(tokenManager)
        val repository = NotificationRepository(apiService, tokenManager)
        activityScope.launch {
            repository.getNotificationCount().collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        val unreadCount = result.data.unreadCount ?: 0
                        badge.text = if (unreadCount > 99) "99+" else unreadCount.toString()
                        badge.visibility = if (unreadCount > 0) View.VISIBLE else View.GONE
                    }
                    is NetworkResult.Error -> badge.visibility = View.GONE
                    is NetworkResult.Unauthorized -> badge.visibility = View.GONE
                    is NetworkResult.Loading -> Unit
                }
            }
        }
    }

    /**
     * Confirms logout before using the shared BaseActivity logout flow.
     */
    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout?")
            .setMessage("Are you sure you want to sign out from this device?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Logout") { _, _ ->
                logout()
            }
            .show()
    }

    fun showLogoutConfirmationFromMore() {
        showLogoutConfirmation()
    }

    /**
     * Loads a fragment into the container.
     * @param fragment The fragment to load
     */
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    /**
     * Loads a screen opened outside the bottom navigation while keeping the nav visible.
     */
    private fun loadSecondaryFragment(fragment: Fragment) {
        if (fragment is StudentMyRequestsFragment ||
            fragment is StudentProfileFragment ||
            fragment is StudentNotificationsFragment
        ) {
            markBottomNavItemChecked(R.id.nav_more)
        }
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(fragment::class.java.simpleName)
            .commit()
    }

    fun openMyRequests() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (currentFragment is StudentMyRequestsFragment) {
            markBottomNavItemChecked(R.id.nav_more)
            return
        }
        loadSecondaryFragment(StudentMyRequestsFragment())
    }

    fun openProfile() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (currentFragment is StudentProfileFragment) {
            markBottomNavItemChecked(R.id.nav_more)
            return
        }
        loadSecondaryFragment(StudentProfileFragment())
    }

    fun openNotifications() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (currentFragment is StudentNotificationsFragment) {
            markBottomNavItemChecked(R.id.nav_more)
            return
        }
        loadSecondaryFragment(StudentNotificationsFragment())
    }

    private fun markBottomNavItemChecked(itemId: Int) {
        bottomNavigation.menu.findItem(itemId)?.isChecked = true
    }

    private companion object {
        const val KEY_LAST_SELECTED_NAV_ITEM = "last_selected_nav_item"
    }
}

