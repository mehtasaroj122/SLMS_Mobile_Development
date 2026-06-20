package com.saroj.lmsmobile.ui.student

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.saroj.lmsmobile.R
import com.saroj.lmsmobile.ui.components.BaseActivity
import com.saroj.lmsmobile.ui.student.fragments.StudentDashboardFragment
import com.saroj.lmsmobile.ui.student.fragments.StudentMyRequestsFragment
import com.saroj.lmsmobile.ui.student.fragments.StudentMyBooksFragment
import com.saroj.lmsmobile.ui.student.fragments.StudentMyFinesFragment
import com.saroj.lmsmobile.ui.student.fragments.StudentProfileFragment
import com.saroj.lmsmobile.ui.student.fragments.StudentSearchBooksFragment
import com.saroj.lmsmobile.ui.student.notifications.StudentNotificationsFragment

/**
 * StudentDashboardActivity is the main activity for student users.
 *
 * Features:
 * - Bottom navigation to switch between modules
 * - Fragment-based navigation for main modules
 * - Dashboard, Search Books, My Books, My Fines, and More menu sections
 * - Consistent toolbar with logout option
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

        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "Student Dashboard"

        // Initialize views
        initializeViews()

        // Set up navigation
        setupBottomNavigation()
        setupBackStackNavigation()

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(StudentDashboardFragment())
        }
    }

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
                    showMoreBottomSheet()
                    markBottomNavItemChecked(R.id.nav_more)
                    false
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

