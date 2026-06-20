package com.saroj.lmsmobile.ui.staff

import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.saroj.lmsmobile.R
import com.saroj.lmsmobile.ui.components.BaseActivity
import com.saroj.lmsmobile.ui.staff.fragments.StaffBookRequestsScreen
import com.saroj.lmsmobile.ui.staff.fragments.StaffDashboardScreen
import com.saroj.lmsmobile.ui.staff.fragments.StaffFinesScreen
import com.saroj.lmsmobile.ui.staff.fragments.StaffIssueBookScreen
import com.saroj.lmsmobile.ui.staff.fragments.StaffMoreScreen
import com.saroj.lmsmobile.ui.staff.fragments.StaffNotificationsScreen
import com.saroj.lmsmobile.ui.staff.fragments.StaffProfileScreen
import com.saroj.lmsmobile.ui.staff.fragments.StaffReturnBookScreen
import com.saroj.lmsmobile.ui.staff.fragments.StaffStudentsScreen

/**
 * StaffDashboardActivity is the main activity for staff users.
 *
 * Features:
 * - Bottom navigation to switch between modules
 * - Fragment-based navigation for each module
 * - Staff Portal header matching the Student Portal design system
 * - Static UI-only screens with placeholder content
 *
 * Navigation:
 * BottomNavigationView switches between fragments:
 * - Dashboard
 * - Issue Book
 * - Return Book
 * - Fines
 * - More
 */
class StaffDashboardActivity : BaseActivity() {

    private lateinit var bottomNavigation: BottomNavigationView
    private var lastSelectedNavItemId = R.id.nav_dashboard

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_staff_dashboard)
        lastSelectedNavItemId = savedInstanceState?.getInt(KEY_LAST_SELECTED_NAV_ITEM)
            ?: R.id.nav_dashboard

        // Initialize views
        initializeViews()
        setupStaffHeader()

        // Set up navigation
        setupBottomNavigation()
        setupBackStackNavigation()

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(StaffDashboardScreen())
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
    private fun setupStaffHeader() {
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

    /**
     * Sets up bottom navigation listener.
     */
    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_dashboard -> {
                    lastSelectedNavItemId = menuItem.itemId
                    loadFragment(StaffDashboardScreen())
                    true
                }
                R.id.nav_issue_book -> {
                    lastSelectedNavItemId = menuItem.itemId
                    loadFragment(StaffIssueBookScreen())
                    true
                }
                R.id.nav_return_book -> {
                    lastSelectedNavItemId = menuItem.itemId
                    loadFragment(StaffReturnBookScreen())
                    true
                }
                R.id.nav_fines -> {
                    lastSelectedNavItemId = menuItem.itemId
                    loadFragment(StaffFinesScreen())
                    true
                }
                R.id.nav_more -> {
                    lastSelectedNavItemId = menuItem.itemId
                    loadFragment(StaffMoreScreen())
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

    private fun loadSecondaryFragment(fragment: Fragment) {
        markBottomNavItemChecked(R.id.nav_more)
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

    fun openIssueBook() {
        bottomNavigation.selectedItemId = R.id.nav_issue_book
    }

    fun openReturnBook() {
        bottomNavigation.selectedItemId = R.id.nav_return_book
    }

    fun openBookRequests() {
        loadSecondaryFragment(StaffBookRequestsScreen())
    }

    fun openStudents() {
        loadSecondaryFragment(StaffStudentsScreen())
    }

    fun openProfile() {
        loadSecondaryFragment(StaffProfileScreen())
    }

    fun openNotifications() {
        loadSecondaryFragment(StaffNotificationsScreen())
    }

    fun showLogoutPlaceholder() {
        Toast.makeText(this, "Logout action placeholder", Toast.LENGTH_SHORT).show()
    }

    private fun markBottomNavItemChecked(itemId: Int) {
        bottomNavigation.menu.findItem(itemId)?.isChecked = true
    }

    private companion object {
        const val KEY_LAST_SELECTED_NAV_ITEM = "last_selected_nav_item"
    }
}

