package com.saroj.lmsmobile.ui.staff

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.saroj.lmsmobile.R
import com.saroj.lmsmobile.ui.components.BaseActivity
import com.saroj.lmsmobile.ui.staff.fragments.StaffDashboardFragment
import com.saroj.lmsmobile.ui.staff.fragments.StaffBooksFragment
import com.saroj.lmsmobile.ui.staff.fragments.StaffStudentsFragment
import com.saroj.lmsmobile.ui.staff.fragments.StaffIssuesFragment
import com.saroj.lmsmobile.ui.staff.fragments.StaffFinesFragment

/**
 * StaffDashboardActivity is the main activity for staff users.
 *
 * Features:
 * - Bottom navigation to switch between modules
 * - Fragment-based navigation for each module
 * - Dashboard, Books, Students, Issues, and Fines sections
 * - Consistent toolbar with logout option
 *
 * Navigation:
 * BottomNavigationView switches between fragments:
 * - Dashboard (statistics and summaries)
 * - Books (view and manage library books)
 * - Students (view student details)
 * - Issues (issue and return books)
 * - Fines (manage student fines)
 *
 * Architecture:
 * Activity holds fragments and manages bottom navigation.
 * Each fragment has its own ViewModel and Repository.
 */
class StaffDashboardActivity : BaseActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_staff_dashboard)

        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "Staff Dashboard"

        // Initialize views
        initializeViews()

        // Set up navigation
        setupBottomNavigation()

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(StaffDashboardFragment())
        }
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
                    loadFragment(StaffDashboardFragment())
                    true
                }
                R.id.nav_books -> {
                    loadFragment(StaffBooksFragment())
                    true
                }
                R.id.nav_students -> {
                    loadFragment(StaffStudentsFragment())
                    true
                }
                R.id.nav_issues -> {
                    loadFragment(StaffIssuesFragment())
                    true
                }
                R.id.nav_fines -> {
                    loadFragment(StaffFinesFragment())
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Loads a fragment into the container.
     * @param fragment The fragment to load
     */
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}

