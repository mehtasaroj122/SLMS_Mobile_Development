package com.saroj.lmsmobile.ui.student.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.saroj.lmsmobile.R
import com.saroj.lmsmobile.ui.books.BaseBooksFragment

class StudentDashboardFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_student_dashboard, container, false)
}

class StudentSearchBooksFragment : BaseBooksFragment(R.layout.fragment_student_search_books, BaseBooksFragment.Mode.SEARCH)

class StudentMyBooksFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_student_my_books, container, false)
}

class StudentMyFinesFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_student_my_fines, container, false)
}

class StudentProfileFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_student_profile, container, false)
}

