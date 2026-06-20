package com.saroj.lmsmobile.ui.staff.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.saroj.lmsmobile.R
import com.saroj.lmsmobile.ui.staff.StaffDashboardActivity

class StaffDashboardScreen : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_staff_dashboard, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.actionIssueBook)?.setOnClickListener {
            (activity as? StaffDashboardActivity)?.openIssueBook()
        }
        view.findViewById<View>(R.id.actionReturnBook)?.setOnClickListener {
            (activity as? StaffDashboardActivity)?.openReturnBook()
        }
    }
}

class StaffIssueBookScreen : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_staff_issue_book, container, false)
}

class StaffReturnBookScreen : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_staff_return_book, container, false)
}

class StaffFinesScreen : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_staff_fines, container, false)
}

class StaffMoreScreen : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_staff_more, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val host = activity as? StaffDashboardActivity
        view.findViewById<View>(R.id.rowBookRequests)?.setOnClickListener {
            host?.openBookRequests()
        }
        view.findViewById<View>(R.id.rowStudents)?.setOnClickListener {
            host?.openStudents()
        }
        view.findViewById<View>(R.id.rowProfile)?.setOnClickListener {
            host?.openProfile()
        }
        view.findViewById<View>(R.id.rowNotifications)?.setOnClickListener {
            host?.openNotifications()
        }
        view.findViewById<View>(R.id.rowLogout)?.setOnClickListener {
            host?.showLogoutPlaceholder()
        }
    }
}

class StaffBookRequestsScreen : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_staff_book_requests, container, false)
}

class StaffStudentsScreen : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_staff_students, container, false)
}

class StaffProfileScreen : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_staff_profile, container, false)
}

class StaffNotificationsScreen : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_staff_notifications, container, false)
}
