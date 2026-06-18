package com.saroj.lmsmobile.ui.admin

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.saroj.lmsmobile.R
import com.saroj.lmsmobile.ui.components.BaseActivity
import com.saroj.lmsmobile.ui.admin.fragments.AdminDashboardFragment
import com.saroj.lmsmobile.ui.admin.fragments.AdminBooksFragment
import com.saroj.lmsmobile.ui.admin.fragments.AdminStudentsFragment
import com.saroj.lmsmobile.ui.admin.fragments.AdminIssuesFragment
import com.saroj.lmsmobile.ui.admin.fragments.AdminFinesFragment

/**
 * AdminDashboardActivity is the main activity for admin users.
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
 * - Books (manage library books)
 * - Students (manage student accounts)
 * - Issues (manage book borrowing)
 * - Fines (manage overdue fines)
 *
 * Architecture:
 * Activity holds fragments and manages bottom navigation.
 * Each fragment has its own ViewModel and Repository.
 */
class AdminDashboardActivity : BaseActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "Admin Dashboard"

        // Initialize views
        initializeViews()

        // Set up navigation
        setupBottomNavigation()

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(AdminDashboardFragment())
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
                    loadFragment(AdminDashboardFragment())
                    true
                }
                R.id.nav_books -> {
                    loadFragment(AdminBooksFragment())
                    true
                }
                R.id.nav_students -> {
                    loadFragment(AdminStudentsFragment())
                    true
                }
                R.id.nav_issues -> {
                    loadFragment(AdminIssuesFragment())
                    true
                }
                R.id.nav_fines -> {
                    loadFragment(AdminFinesFragment())
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

