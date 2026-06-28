package com.saroj.lmsmobile.ui.student.fragments

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.saroj.lmsmobile.MainApplication
import com.saroj.lmsmobile.R
import com.saroj.lmsmobile.api.RetrofitClient
import com.saroj.lmsmobile.data.models.common.NetworkResult
import com.saroj.lmsmobile.data.repository.NotificationRepository
import com.saroj.lmsmobile.data.repository.StudentProfileRepository
import com.saroj.lmsmobile.ui.common.UnauthorizedActivity
import com.saroj.lmsmobile.ui.student.StudentDashboardActivity
import com.saroj.lmsmobile.ui.student.model.StudentProfileUiModel
import com.saroj.lmsmobile.ui.theme.DarkModeToggleBinder
import com.saroj.lmsmobile.utils.Constants
import com.saroj.lmsmobile.utils.NotificationRefreshBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.Locale

class StudentMoreFragment : Fragment() {
    private var profilePhotoTag: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_staff_more, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.rowStudents)?.visibility = View.GONE
        renameFirstRowText(view.findViewById(R.id.rowBookRequests), "My Requests")

        bindCachedProfile(view)
        loadProfile(view)
        loadNotificationBadge(view)
        setupNotificationRefreshObserver(view)
        DarkModeToggleBinder.bind(view, viewLifecycleOwner, viewLifecycleOwner.lifecycleScope)

        val host = activity as? StudentDashboardActivity
        view.findViewById<View>(R.id.rowBookRequests)?.setOnClickListener { host?.openMyRequests() }
        view.findViewById<View>(R.id.rowProfile)?.setOnClickListener { host?.openProfile() }
        view.findViewById<View>(R.id.rowNotifications)?.setOnClickListener { host?.openNotifications() }
        view.findViewById<View>(R.id.rowLogout)?.setOnClickListener { host?.showLogoutConfirmationFromMore() }
    }

    override fun onResume() {
        super.onResume()
        view?.let {
            loadProfile(it)
            loadNotificationBadge(it)
        }
    }

    private fun bindCachedProfile(view: View) {
        val tokenManager = (requireActivity().application as MainApplication).tokenManager
        viewLifecycleOwner.lifecycleScope.launch {
            val name = tokenManager.getUserName().firstOrNull().orFallback("Student")
            val email = tokenManager.getUserEmail().firstOrNull().orFallback("Email not available")
            bindProfile(
                view,
                StudentProfileUiModel(
                    name = name,
                    email = email,
                    role = tokenManager.getUserRole().firstOrNull().orFallback("Student").titleCase(),
                    username = "-",
                    studentId = "-",
                    department = "-",
                    memberSince = "-",
                    lastLogin = "-",
                    phone = "",
                    gender = "",
                    address = "",
                    profilePhotoUrl = null
                )
            )
        }
    }

    private fun renameFirstRowText(row: View?, label: String) {
        if (row !is ViewGroup) return
        for (index in 0 until row.childCount) {
            val child = row.getChildAt(index)
            if (child is TextView) {
                child.text = label
                return
            }
        }
    }

    private fun loadProfile(view: View) {
        val tokenManager = (requireActivity().application as MainApplication).tokenManager
        val apiService = RetrofitClient.getApiService(tokenManager)
        val repository = StudentProfileRepository(apiService, tokenManager)
        viewLifecycleOwner.lifecycleScope.launch {
            repository.getProfile().collect { result ->
                when (result) {
                    is NetworkResult.Success -> bindProfile(view, result.data)
                    is NetworkResult.Unauthorized -> navigateToUnauthorized()
                    else -> Unit
                }
            }
        }
    }

    private fun loadNotificationBadge(view: View) {
        val badge = view.findViewById<TextView>(R.id.textMoreNotificationsBadge) ?: return
        badge.visibility = View.GONE

        val tokenManager = (requireActivity().application as MainApplication).tokenManager
        val apiService = RetrofitClient.getApiService(tokenManager)
        val repository = NotificationRepository(apiService, tokenManager)
        viewLifecycleOwner.lifecycleScope.launch {
            repository.getNotificationCount().collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        val unreadCount = result.data.unreadCount ?: 0
                        badge.text = if (unreadCount > 99) "99+" else unreadCount.toString()
                        badge.visibility = if (unreadCount > 0) View.VISIBLE else View.GONE
                    }
                    is NetworkResult.Unauthorized -> navigateToUnauthorized()
                    else -> badge.visibility = View.GONE
                }
            }
        }
    }

    private fun setupNotificationRefreshObserver(view: View) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                NotificationRefreshBus.events.collect {
                    loadNotificationBadge(view)
                }
            }
        }
    }

    private fun bindProfile(view: View, profile: StudentProfileUiModel) {
        val name = profile.name.orFallback("Student")
        view.findViewById<TextView>(R.id.textStaffMoreName)?.text = name
        view.findViewById<TextView>(R.id.textStaffMoreEmail)?.text = profile.email.orFallback("Email not available")
        view.findViewById<TextView>(R.id.textStaffMoreInitials)?.text = getInitials(name)
        loadMoreProfilePhoto(view, profile.profilePhotoUrl)
    }

    private fun loadMoreProfilePhoto(view: View, rawUrl: String?) {
        val image = view.findViewById<ImageView>(R.id.imageStaffMoreProfile) ?: return
        val initials = view.findViewById<TextView>(R.id.textStaffMoreInitials)
        val photoUrl = normalizeProfilePhotoUrl(rawUrl)
        profilePhotoTag = photoUrl
        image.tag = photoUrl
        if (photoUrl.isNullOrBlank()) {
            image.visibility = View.GONE
            initials?.visibility = View.VISIBLE
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                runCatching { URL(photoUrl).openStream().use { BitmapFactory.decodeStream(it) } }
                    .onFailure { Log.w("StudentMore", "Profile photo failed to load: $photoUrl", it) }
                    .getOrNull()
            }
            if (bitmap != null && image.tag == photoUrl && profilePhotoTag == photoUrl) {
                image.setImageBitmap(bitmap)
                image.visibility = View.VISIBLE
                initials?.visibility = View.GONE
            } else {
                image.visibility = View.GONE
                initials?.visibility = View.VISIBLE
            }
        }
    }

    private fun normalizeProfilePhotoUrl(rawUrl: String?): String? {
        val value = rawUrl?.trim()?.takeIf { it.isNotBlank() } ?: return null
        if (!value.startsWith("http", ignoreCase = true)) {
            return Constants.BASE_URL.removeSuffix("api/") + value.trimStart('/')
        }
        return Constants.normalizeLaravelAssetUrl(value) ?: value
    }

    private fun getInitials(name: String): String =
        name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
            .joinToString("").ifBlank { "ST" }

    private fun navigateToUnauthorized() {
        if (!isAdded) return
        startActivity(Intent(requireContext(), UnauthorizedActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        requireActivity().finish()
    }

    private fun String?.orFallback(fallback: String): String =
        if (isNullOrBlank() || this == "-") fallback else this

    private fun String.titleCase(): String =
        replace("_", " ").split(" ").filter { it.isNotBlank() }.joinToString(" ") {
            it.lowercase(Locale.US).replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.US) else char.toString()
            }
        }
}
