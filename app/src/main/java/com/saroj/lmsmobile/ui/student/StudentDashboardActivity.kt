package com.saroj.lmsmobile.ui.student

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.saroj.lmsmobile.R
import com.saroj.lmsmobile.ui.components.BaseActivity
import com.saroj.lmsmobile.ui.student.fragments.StudentDashboardFragment
import com.saroj.lmsmobile.ui.student.fragments.StudentSearchBooksFragment
import com.saroj.lmsmobile.ui.student.fragments.StudentMyBooksFragment
import com.saroj.lmsmobile.ui.student.fragments.StudentMyFinesFragment
import com.saroj.lmsmobile.ui.student.fragments.StudentProfileFragment

/**
 * StudentDashboardActivity is the main activity for student users.
 *
 * Features:
 * - Bottom navigation to switch between modules
 * - Fragment-based navigation for each module
 * - Dashboard, Search Books, My Books, My Fines, and Profile sections
 * - Consistent toolbar with logout option
 *
 * Navigation:
 * BottomNavigationView switches between fragments:
 * - Dashboard (statistics and summaries)
 * - Search Books (search and browse available books)
 * - My Books (view currently borrowed books)
 * - My Fines (view fines and dues)
 * - Profile (user profile and settings)
 *
 * Architecture:
 * Activity holds fragments and manages bottom navigation.
 * Each fragment has its own ViewModel and Repository.
 */
class StudentDashboardActivity : BaseActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_dashboard)

        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "Student Dashboard"

        // Initialize views
        initializeViews()

        // Set up navigation
        setupBottomNavigation()

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(StudentDashboardFragment())
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
                    loadFragment(StudentDashboardFragment())
                    true
                }
                R.id.nav_search_books -> {
                    loadFragment(StudentSearchBooksFragment())
                    true
                }
                R.id.nav_my_books -> {
                    loadFragment(StudentMyBooksFragment())
                    true
                }
                R.id.nav_my_fines -> {
                    loadFragment(StudentMyFinesFragment())
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(StudentProfileFragment())
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

