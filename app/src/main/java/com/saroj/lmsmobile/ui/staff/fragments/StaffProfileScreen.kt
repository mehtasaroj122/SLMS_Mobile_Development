package com.saroj.lmsmobile.ui.staff.fragments

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.saroj.lmsmobile.MainApplication
import com.saroj.lmsmobile.R
import com.saroj.lmsmobile.api.RetrofitClient
import com.saroj.lmsmobile.data.models.common.NetworkResult
import com.saroj.lmsmobile.data.repository.StaffProfileRepository
import com.saroj.lmsmobile.ui.auth.LoginActivity
import com.saroj.lmsmobile.ui.staff.model.StaffProfileUiModel
import com.saroj.lmsmobile.ui.staff.model.defaultStaffProfile
import com.saroj.lmsmobile.ui.staff.viewmodel.StaffProfileViewModel
import com.saroj.lmsmobile.ui.student.model.ProfileTab
import com.saroj.lmsmobile.utils.Constants
import com.saroj.lmsmobile.utils.LmsToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.util.Locale

class StaffProfileScreen : Fragment() {
    private lateinit var viewModel: StaffProfileViewModel
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private var selectedTab = ProfileTab.PROFILE
    private var isEditMode = false
    private var currentPasswordVisible = false
    private var newPasswordVisible = false
    private var confirmPasswordVisible = false
    private var selectedPhotoFile: File? = null

    private var profile = defaultStaffProfile()

    private val photoPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@registerForActivityResult
        handleSelectedPhoto(uri)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_staff_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        swipeRefreshLayout = view.findViewById(R.id.profileSwipeRefresh)
        setupViewModel()
        setupHeader()
        setupTabs()
        setupProfileInfoSection()
        setupPhotoSection()
        setupSecuritySection()
        setupDeleteSection()
        setupPullToRefresh()
        observeProfile()
        bindSummary()
        bindProfileFields()
        switchTab(ProfileTab.PROFILE)
        viewModel.loadInitial()
    }

    private fun setupViewModel() {
        val tokenManager = (requireActivity().application as MainApplication).tokenManager
        val apiService = RetrofitClient.getApiService(tokenManager)
        val repository = StaffProfileRepository(apiService, tokenManager)

        viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return StaffProfileViewModel(repository) as T
                }
            }
        )[StaffProfileViewModel::class.java]
    }

    private fun setupPullToRefresh() {
        swipeRefreshLayout.setColorSchemeResources(
            R.color.profile_primary,
            R.color.profile_green,
            R.color.profile_orange
        )
        swipeRefreshLayout.setOnRefreshListener {
            if (isEditMode) exitEditMode(restoreValues = true)
            viewModel.refresh()
        }
    }

    private fun observeProfile() {
        viewModel.profileState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is NetworkResult.Loading -> {
                    if (!swipeRefreshLayout.isRefreshing) {
                        swipeRefreshLayout.isRefreshing = true
                    }
                }
                is NetworkResult.Success -> {
                    swipeRefreshLayout.isRefreshing = false
                    profile = result.data
                    bindSummary()
                    bindProfileFields()
                    if (!isEditMode) exitEditMode(restoreValues = false)
                }
                is NetworkResult.Error -> {
                    swipeRefreshLayout.isRefreshing = false
                    showToast(result.message)
                }
                is NetworkResult.Unauthorized -> navigateToLogin()
            }
        }

        viewModel.profileActionState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is NetworkResult.Loading -> swipeRefreshLayout.isRefreshing = true
                is NetworkResult.Success -> {
                    swipeRefreshLayout.isRefreshing = false
                    selectedPhotoFile = null
                    if (result.data.contains("photo", ignoreCase = true)) {
                        view?.setProfileText(R.id.textSelectedPhotoFile, "No file chosen")
                    }
                    if (result.data.contains("password", ignoreCase = true)) {
                        clearPasswordForm()
                    }
                    showToast(result.data)
                    if (
                        result.data.contains("deleted", ignoreCase = true) ||
                        result.data.contains("deactivated", ignoreCase = true)
                    ) {
                        navigateToLogin()
                    }
                }
                is NetworkResult.Error -> {
                    swipeRefreshLayout.isRefreshing = false
                    showToast(result.message)
                }
                is NetworkResult.Unauthorized -> navigateToLogin()
            }
        }
    }

    private fun setupHeader() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    handleProfileBack()
                }
            }
        )

        view?.findViewById<View>(R.id.buttonProfileBack)?.setOnClickListener {
            handleProfileBack()
        }
    }

    private fun handleProfileBack() {
        if (isEditMode) {
            exitEditMode(restoreValues = true)
            return
        }

        if (parentFragmentManager.backStackEntryCount > 0) {
            parentFragmentManager.popBackStack()
            return
        }

        activity
            ?.findViewById<BottomNavigationView>(R.id.bottomNavigation)
            ?.selectedItemId = R.id.nav_more
    }

    private fun bindSummary() {
        val initials = getInitials(profile.name)
        view?.setProfileText(R.id.textProfileInitials, initials)
        view?.setProfileText(R.id.textPhotoPreviewInitials, initials)
        view?.setProfileText(R.id.textProfileName, profile.name)
        view?.setProfileText(R.id.textProfileEmail, profile.email)
        view?.setProfileText(R.id.textProfileRole, profile.role.uppercase(Locale.US))
        view?.setProfileText(R.id.textProfileUsername, profile.username)
        view?.setProfileText(R.id.textProfileStudentId, profile.staffId)
        view?.setProfileText(R.id.textProfileDepartment, profile.department)
        view?.setProfileText(R.id.textProfileMemberSince, profile.memberSince)
        view?.setProfileText(R.id.textProfileLastLogin, profile.lastLogin)
        loadProfileImages(profile.profilePhotoUrl)
    }

    private fun bindProfileFields() {
        view?.findViewById<EditText>(R.id.editProfileFullName)?.setText(profile.name.takeUnless { it == "-" }.orEmpty())
        view?.findViewById<EditText>(R.id.editProfileEmail)?.setText(profile.email)
        view?.findViewById<EditText>(R.id.editProfilePhone)?.setText(profile.phone)
        view?.findViewById<EditText>(R.id.editProfileUsername)?.setText(profile.username)
        view?.findViewById<EditText>(R.id.editProfileRole)?.setText(profile.role)
        view?.findViewById<EditText>(R.id.editProfileDepartment)?.setText(profile.department)
        view?.findViewById<EditText>(R.id.editProfileAddress)?.setText(profile.address)
    }

    private fun setupTabs() {
        view?.findViewById<View>(R.id.tabProfileInfo)?.setOnClickListener {
            switchTab(ProfileTab.PROFILE)
        }
        view?.findViewById<View>(R.id.tabProfilePhoto)?.setOnClickListener {
            switchTab(ProfileTab.PHOTO)
        }
        view?.findViewById<View>(R.id.tabProfileSecurity)?.setOnClickListener {
            switchTab(ProfileTab.SECURITY)
        }
        view?.findViewById<View>(R.id.tabProfileDelete)?.setOnClickListener {
            switchTab(ProfileTab.DELETE)
        }
    }

    private fun switchTab(tab: ProfileTab) {
        if (selectedTab != tab && isEditMode) {
            exitEditMode(restoreValues = true)
        }

        selectedTab = tab
        view?.findViewById<View>(R.id.sectionProfileInfo)?.visibility =
            if (tab == ProfileTab.PROFILE) View.VISIBLE else View.GONE
        view?.findViewById<View>(R.id.sectionProfilePhoto)?.visibility =
            if (tab == ProfileTab.PHOTO) View.VISIBLE else View.GONE
        view?.findViewById<View>(R.id.sectionProfileSecurity)?.visibility =
            if (tab == ProfileTab.SECURITY) View.VISIBLE else View.GONE
        view?.findViewById<View>(R.id.sectionProfileDelete)?.visibility =
            if (tab == ProfileTab.DELETE) View.VISIBLE else View.GONE
        updateTabStyles()
    }

    private fun updateTabStyles() {
        val styles = listOf(
            ProfileTabStyle(R.id.tabProfileInfo, R.id.iconTabProfileInfo, R.id.textTabProfileInfo, ProfileTab.PROFILE),
            ProfileTabStyle(R.id.tabProfilePhoto, R.id.iconTabProfilePhoto, R.id.textTabProfilePhoto, ProfileTab.PHOTO),
            ProfileTabStyle(R.id.tabProfileSecurity, R.id.iconTabProfileSecurity, R.id.textTabProfileSecurity, ProfileTab.SECURITY),
            ProfileTabStyle(R.id.tabProfileDelete, R.id.iconTabProfileDelete, R.id.textTabProfileDelete, ProfileTab.DELETE)
        )

        styles.forEach { style ->
            val selected = selectedTab == style.tab
            view?.findViewById<View>(style.containerId)?.setBackgroundResource(
                if (selected) R.drawable.bg_profile_segment_active else 0
            )
            view?.findViewById<ImageView>(style.iconId)?.setColorFilter(
                ContextCompat.getColor(
                    requireContext(),
                    if (selected) R.color.white else R.color.profile_text_muted
                )
            )
            view?.findViewById<TextView>(style.textId)?.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (selected) R.color.white else R.color.profile_text_primary
                )
            )
        }
    }

    private fun setupProfileInfoSection() {
        view?.findViewById<View>(R.id.buttonProfileEdit)?.setOnClickListener { enterEditMode() }
        view?.findViewById<View>(R.id.buttonProfileCancel)?.setOnClickListener {
            exitEditMode(restoreValues = true)
        }
        view?.findViewById<View>(R.id.buttonProfileSave)?.setOnClickListener {
            saveProfile()
        }
        exitEditMode(restoreValues = false)
    }

    private fun enterEditMode() {
        isEditMode = true
        view?.findViewById<TextView>(R.id.textProfileInfoStatus)?.apply {
            text = "EDITING"
            setBackgroundResource(R.drawable.bg_profile_editing_badge)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.profile_primary))
        }
        view?.findViewById<View>(R.id.buttonProfileEdit)?.visibility = View.GONE
        view?.findViewById<View>(R.id.profileEditActions)?.visibility = View.VISIBLE

        setFieldEditable(R.id.editProfileFullName, true)
        setFieldEditable(R.id.editProfilePhone, true)
        setFieldEditable(R.id.editProfileAddress, true)
        setFieldEditable(R.id.editProfileEmail, false)
        setFieldEditable(R.id.editProfileUsername, false)
        setFieldEditable(R.id.editProfileRole, false)
        setFieldEditable(R.id.editProfileDepartment, false)
    }

    private fun exitEditMode(restoreValues: Boolean) {
        isEditMode = false
        if (restoreValues) bindProfileFields()

        view?.findViewById<TextView>(R.id.textProfileInfoStatus)?.apply {
            text = "READ ONLY"
            setBackgroundResource(R.drawable.bg_profile_status_badge)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.profile_text_muted))
        }
        view?.findViewById<View>(R.id.buttonProfileEdit)?.visibility = View.VISIBLE
        view?.findViewById<View>(R.id.profileEditActions)?.visibility = View.GONE

        setFieldEditable(R.id.editProfileFullName, false)
        setFieldEditable(R.id.editProfileEmail, false)
        setFieldEditable(R.id.editProfilePhone, false)
        setFieldEditable(R.id.editProfileUsername, false)
        setFieldEditable(R.id.editProfileRole, false)
        setFieldEditable(R.id.editProfileDepartment, false)
        setFieldEditable(R.id.editProfileAddress, false)
    }

    private fun saveProfile() {
        val fullName = view?.findViewById<EditText>(R.id.editProfileFullName)?.text?.toString()?.trim().orEmpty()
        val phone = view?.findViewById<EditText>(R.id.editProfilePhone)?.text?.toString()?.trim().orEmpty()
        val address = view?.findViewById<EditText>(R.id.editProfileAddress)?.text?.toString()?.trim().orEmpty()

        if (!validateProfileForm(fullName, phone)) return

        val updatedProfile = profile.copy(
            name = fullName,
            phone = phone,
            address = address
        )
        exitEditMode(restoreValues = false)
        viewModel.updateProfile(updatedProfile)
    }

    private fun validateProfileForm(fullName: String, phone: String): Boolean {
        if (fullName.isBlank()) {
            showToast("Full name is required")
            return false
        }

        val validPhone = phone.isBlank() || phone.matches(Regex("^[+0-9][0-9\\-\\s()]{6,19}$"))
        if (!validPhone) {
            showToast("Enter a valid phone number")
            return false
        }

        return true
    }

    private fun setupPhotoSection() {
        view?.findViewById<View>(R.id.buttonProfileCamera)?.setOnClickListener {
            switchTab(ProfileTab.PHOTO)
        }
        view?.findViewById<View>(R.id.buttonChoosePhoto)?.setOnClickListener {
            photoPicker.launch("image/*")
        }
        view?.findViewById<View>(R.id.buttonUploadPhoto)?.setOnClickListener {
            val file = selectedPhotoFile
            if (file == null) {
                showToast("Choose a photo first.")
                return@setOnClickListener
            }
            viewModel.uploadPhoto(file)
        }
        view?.findViewById<View>(R.id.buttonRemovePhoto)?.setOnClickListener {
            showRemovePhotoDialog()
        }
    }

    private fun handleSelectedPhoto(uri: Uri) {
        val sourceName = resolveDisplayName(uri)
        if (!isSupportedImage(sourceName)) {
            showToast("Supported image types are JPG, PNG, and WEBP")
            return
        }

        val file = copyUriToCache(uri, sourceName)
        if (file == null) {
            showToast("Unable to read selected photo.")
            return
        }
        if (file.length() > MAX_PHOTO_BYTES) {
            file.delete()
            showToast("Profile photo must be 2MB or smaller")
            return
        }

        selectedPhotoFile = file
        view?.setProfileText(R.id.textSelectedPhotoFile, file.name)
        view?.findViewById<ImageView>(R.id.imagePhotoPreview)?.apply {
            setImageURI(uri)
            visibility = View.VISIBLE
        }
        view?.findViewById<TextView>(R.id.textPhotoPreviewInitials)?.visibility = View.GONE
    }

    private fun resolveDisplayName(uri: Uri): String? {
        return requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) cursor.getString(nameIndex) else null
        }
    }

    private fun isSupportedImage(fileName: String?): Boolean {
        return fileName
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.lowercase(Locale.US) in SUPPORTED_IMAGE_EXTENSIONS
    }

    private fun copyUriToCache(uri: Uri, sourceName: String?): File? {
        val resolver = requireContext().contentResolver
        val extension = sourceName?.substringAfterLast('.', missingDelimiterValue = "jpg") ?: "jpg"
        val file = File(requireContext().cacheDir, "staff_profile_photo_${System.currentTimeMillis()}.$extension")

        return runCatching {
            resolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            file
        }.getOrNull()
    }

    private fun setupSecuritySection() {
        view?.findViewById<View>(R.id.iconToggleCurrentPassword)?.setOnClickListener {
            currentPasswordVisible = !currentPasswordVisible
            togglePasswordVisibility(R.id.editCurrentPassword, R.id.iconToggleCurrentPassword, currentPasswordVisible)
        }
        view?.findViewById<View>(R.id.iconToggleNewPassword)?.setOnClickListener {
            newPasswordVisible = !newPasswordVisible
            togglePasswordVisibility(R.id.editNewPassword, R.id.iconToggleNewPassword, newPasswordVisible)
        }
        view?.findViewById<View>(R.id.iconToggleConfirmPassword)?.setOnClickListener {
            confirmPasswordVisible = !confirmPasswordVisible
            togglePasswordVisibility(R.id.editConfirmPassword, R.id.iconToggleConfirmPassword, confirmPasswordVisible)
        }
        view?.findViewById<EditText>(R.id.editNewPassword)?.doAfterTextChanged {
            updatePasswordRequirements()
        }
        view?.findViewById<EditText>(R.id.editConfirmPassword)?.doAfterTextChanged {
            updatePasswordRequirements()
        }
        view?.findViewById<View>(R.id.buttonCancelPassword)?.setOnClickListener {
            clearPasswordForm()
        }
        view?.findViewById<View>(R.id.buttonSavePassword)?.setOnClickListener {
            if (validatePasswordForm()) {
                val currentPassword = view?.findViewById<EditText>(R.id.editCurrentPassword)?.text?.toString().orEmpty()
                val newPassword = view?.findViewById<EditText>(R.id.editNewPassword)?.text?.toString().orEmpty()
                val confirmPassword = view?.findViewById<EditText>(R.id.editConfirmPassword)?.text?.toString().orEmpty()
                viewModel.changePassword(currentPassword, newPassword, confirmPassword)
            }
        }
        updatePasswordRequirements()
    }

    private fun validatePasswordForm(): Boolean {
        val currentPassword = view?.findViewById<EditText>(R.id.editCurrentPassword)?.text?.toString().orEmpty()
        val newPassword = view?.findViewById<EditText>(R.id.editNewPassword)?.text?.toString().orEmpty()
        val confirmPassword = view?.findViewById<EditText>(R.id.editConfirmPassword)?.text?.toString().orEmpty()
        var valid = true

        if (currentPassword.isBlank()) {
            setPasswordError(R.id.containerCurrentPassword, R.id.errorCurrentPassword, "Current password is required")
            valid = false
        } else {
            clearPasswordError(R.id.containerCurrentPassword, R.id.errorCurrentPassword)
        }

        when {
            newPassword.length < 8 -> {
                setPasswordError(R.id.containerNewPassword, R.id.errorNewPassword, "New password must be at least 8 characters")
                valid = false
            }
            !newPassword.any { it.isUpperCase() } ||
                !newPassword.any { it.isLowerCase() } ||
                !newPassword.any { it.isDigit() } -> {
                setPasswordError(R.id.containerNewPassword, R.id.errorNewPassword, "Password must include uppercase, lowercase, and number")
                valid = false
            }
            newPassword == currentPassword -> {
                setPasswordError(R.id.containerNewPassword, R.id.errorNewPassword, "New password must be different from current password")
                valid = false
            }
            else -> clearPasswordError(R.id.containerNewPassword, R.id.errorNewPassword)
        }

        if (confirmPassword != newPassword || confirmPassword.isBlank()) {
            setPasswordError(R.id.containerConfirmPassword, R.id.errorConfirmPassword, "Passwords must match")
            valid = false
        } else {
            clearPasswordError(R.id.containerConfirmPassword, R.id.errorConfirmPassword)
        }

        updatePasswordRequirements()
        return valid
    }

    private fun setupDeleteSection() {
        bindStaffDeleteSection()
        view?.findViewById<View>(R.id.buttonDeleteAccount)?.setOnClickListener {
            showDeleteAccountDialog()
        }
    }

    private fun bindStaffDeleteSection() {
        view?.setProfileText(R.id.textIssuedBooksRestriction, "Password required")
        view?.setProfileText(R.id.textPendingFinesRestriction, "Type DELETE to confirm")
        view?.setProfileText(R.id.textActiveRequestsRestriction, "Backend restrictions apply")

        view?.findViewById<TextView>(R.id.textDeleteRestrictedBadge)?.apply {
            text = "Confirmation required"
            setBackgroundResource(R.drawable.bg_profile_restricted_badge)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.profile_red_dark))
        }

        view?.findViewById<TextView>(R.id.buttonDeleteAccount)?.apply {
            isEnabled = true
            setBackgroundResource(R.drawable.bg_profile_primary_button)
            alpha = 1f
        }
    }

    private fun showDeleteAccountDialog() {
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 16, 48, 0)
        }
        val passwordInput = EditText(requireContext()).apply {
            hint = "Current password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setSingleLine(true)
        }
        val deleteInput = EditText(requireContext()).apply {
            hint = "Type DELETE"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            setSingleLine(true)
        }
        container.addView(passwordInput)
        container.addView(deleteInput)

        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Account Deletion")
            .setMessage("All your data will be removed and cannot be recovered.\nType DELETE and enter your password to confirm.")
            .setView(container)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete Account", null)
            .create()
            .apply {
                setOnShowListener {
                    val deleteButton = getButton(AlertDialog.BUTTON_POSITIVE)
                    fun updateDeleteButton() {
                        deleteButton.isEnabled = deleteInput.text?.toString()?.trim() == DELETE_CONFIRMATION &&
                            !passwordInput.text?.toString().isNullOrBlank()
                    }
                    deleteButton.isEnabled = false
                    passwordInput.doAfterTextChanged { updateDeleteButton() }
                    deleteInput.doAfterTextChanged { updateDeleteButton() }
                    deleteButton.setOnClickListener {
                        val password = passwordInput.text?.toString().orEmpty()
                        val confirmation = deleteInput.text?.toString()?.trim().orEmpty()
                        if (password.isBlank()) {
                            passwordInput.error = "Current password is required"
                            return@setOnClickListener
                        }
                        if (confirmation != DELETE_CONFIRMATION) {
                            deleteInput.error = "Type DELETE to confirm"
                            return@setOnClickListener
                        }
                        dismiss()
                        viewModel.deleteAccount(password, confirmation)
                    }
                }
            }
            .show()
    }

    private fun showRemovePhotoDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Remove profile photo?")
            .setMessage("This will show your initials until a new photo is uploaded.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Remove") { _, _ ->
                viewModel.removePhoto()
            }
            .show()
    }

    private fun getInitials(name: String): String {
        val parts = name.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() && it != "-" }

        return parts
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
            .joinToString("")
            .ifBlank { "ST" }
    }

    private fun showToast(message: String) {
        LmsToast.show(requireContext(), message)
    }

    private fun loadProfileImages(rawUrl: String?) {
        val summaryImage = view?.findViewById<ImageView>(R.id.imageProfileAvatar)
        val summaryInitials = view?.findViewById<TextView>(R.id.textProfileInitials)
        val previewImage = view?.findViewById<ImageView>(R.id.imagePhotoPreview)
        val previewInitials = view?.findViewById<TextView>(R.id.textPhotoPreviewInitials)
        val photoUrl = normalizeProfilePhotoUrl(rawUrl)

        summaryImage?.tag = photoUrl
        previewImage?.tag = photoUrl

        if (photoUrl.isNullOrBlank()) {
            summaryImage?.visibility = View.GONE
            previewImage?.visibility = View.GONE
            summaryInitials?.visibility = View.VISIBLE
            previewInitials?.visibility = View.VISIBLE
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                runCatching {
                    URL(photoUrl).openStream().use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }.onFailure {
                    Log.w("StaffProfile", "Profile photo failed to load: $photoUrl", it)
                }.getOrNull()
            }

            if (bitmap == null) {
                summaryImage?.visibility = View.GONE
                previewImage?.visibility = View.GONE
                summaryInitials?.visibility = View.VISIBLE
                previewInitials?.visibility = View.VISIBLE
                return@launch
            }

            if (summaryImage?.tag == photoUrl) {
                summaryImage.setImageBitmap(bitmap)
                summaryImage.visibility = View.VISIBLE
                summaryInitials?.visibility = View.GONE
            }

            if (previewImage?.tag == photoUrl) {
                previewImage.setImageBitmap(bitmap)
                previewImage.visibility = View.VISIBLE
                previewInitials?.visibility = View.GONE
            }
        }
    }

    private fun normalizeProfilePhotoUrl(rawUrl: String?): String? {
        val value = rawUrl?.trim()?.takeIf { it.isNotBlank() } ?: return null
        if (!value.startsWith("http", ignoreCase = true)) {
            return Constants.BASE_URL.removeSuffix("api/") + value.trimStart('/')
        }

        val apiRoot = Constants.BASE_URL.removeSuffix("api/").trimEnd('/')
        return value
            .replace("http://127.0.0.1:8000", apiRoot)
            .replace("http://localhost:8000", apiRoot)
            .replace("https://127.0.0.1:8000", apiRoot)
            .replace("https://localhost:8000", apiRoot)
    }

    private fun navigateToLogin() {
        if (!isAdded) return
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }

    private fun setFieldEditable(fieldId: Int, editable: Boolean) {
        view?.findViewById<EditText>(fieldId)?.apply {
            isFocusable = editable
            isFocusableInTouchMode = editable
            isCursorVisible = editable
            isLongClickable = editable
            isEnabled = true
            if (!editable) clearFocus()
        }
    }

    private fun togglePasswordVisibility(fieldId: Int, iconId: Int, visible: Boolean) {
        val field = view?.findViewById<EditText>(fieldId) ?: return
        field.inputType = if (visible) {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        field.setSelection(field.text?.length ?: 0)
        view?.findViewById<ImageView>(iconId)?.setImageResource(
            if (visible) R.drawable.ic_eye_off else R.drawable.ic_eye
        )
    }

    private fun updatePasswordRequirements() {
        val newPassword = view?.findViewById<EditText>(R.id.editNewPassword)?.text?.toString().orEmpty()
        val confirmPassword = view?.findViewById<EditText>(R.id.editConfirmPassword)?.text?.toString().orEmpty()
        updateRequirement(R.id.textReqLength, "At least 8 characters", newPassword.length >= 8)
        updateRequirement(R.id.textReqUppercase, "At least one uppercase letter", newPassword.any { it.isUpperCase() })
        updateRequirement(R.id.textReqLowercase, "At least one lowercase letter", newPassword.any { it.isLowerCase() })
        updateRequirement(R.id.textReqNumber, "At least one number", newPassword.any { it.isDigit() })
        updateRequirement(
            R.id.textReqMatch,
            "Passwords must match",
            newPassword.isNotBlank() && newPassword == confirmPassword
        )
    }

    private fun updateRequirement(textId: Int, label: String, met: Boolean) {
        view?.findViewById<TextView>(textId)?.apply {
            text = "${if (met) "OK" else "--"} $label"
            setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (met) R.color.profile_green else R.color.profile_text_muted
                )
            )
        }
    }

    private fun setPasswordError(containerId: Int, errorId: Int, message: String) {
        view?.findViewById<LinearLayout>(containerId)?.setBackgroundResource(R.drawable.bg_profile_input_error)
        view?.findViewById<TextView>(errorId)?.apply {
            text = message
            visibility = View.VISIBLE
        }
    }

    private fun clearPasswordError(containerId: Int, errorId: Int) {
        view?.findViewById<LinearLayout>(containerId)?.setBackgroundResource(R.drawable.bg_profile_input)
        view?.findViewById<TextView>(errorId)?.visibility = View.GONE
    }

    private fun clearPasswordForm() {
        view?.findViewById<EditText>(R.id.editCurrentPassword)?.text = null
        view?.findViewById<EditText>(R.id.editNewPassword)?.text = null
        view?.findViewById<EditText>(R.id.editConfirmPassword)?.text = null
        clearPasswordError(R.id.containerCurrentPassword, R.id.errorCurrentPassword)
        clearPasswordError(R.id.containerNewPassword, R.id.errorNewPassword)
        clearPasswordError(R.id.containerConfirmPassword, R.id.errorConfirmPassword)
        updatePasswordRequirements()
    }

    private fun View.setProfileText(id: Int, value: String) {
        findViewById<TextView>(id)?.text = value
    }

    private data class ProfileTabStyle(
        val containerId: Int,
        val iconId: Int,
        val textId: Int,
        val tab: ProfileTab
    )

    private companion object {
        const val DELETE_CONFIRMATION = "DELETE"
        const val MAX_PHOTO_BYTES = 2L * 1024L * 1024L
        val SUPPORTED_IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")
    }
}
