package com.saroj.lmsmobile.ui.student.notifications

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.saroj.lmsmobile.MainApplication
import com.saroj.lmsmobile.R
import com.saroj.lmsmobile.api.RetrofitClient
import com.saroj.lmsmobile.data.models.notification.AppNotification
import com.saroj.lmsmobile.data.repository.NotificationRepository
import com.saroj.lmsmobile.ui.common.UnauthorizedActivity
import com.saroj.lmsmobile.ui.components.OnlineRefreshable
import com.saroj.lmsmobile.ui.components.runIfOnline
import com.saroj.lmsmobile.utils.NotificationRefreshBus
import com.saroj.lmsmobile.utils.LmsToast
import com.saroj.lmsmobile.utils.NetworkMessages
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.Locale

class StudentNotificationsFragment : Fragment(), OnlineRefreshable {

    private lateinit var viewModel: NotificationsViewModel
    private lateinit var adapter: NotificationsAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var stateLayout: View
    private lateinit var stateIcon: ImageView
    private lateinit var stateTitle: TextView
    private lateinit var stateMessage: TextView
    private lateinit var stateAction: TextView
    private lateinit var unreadBadge: TextView
    private lateinit var markAllReadButton: View
    private lateinit var markAllReadText: TextView
    private lateinit var markAllReadIcon: ImageView

    private var selectedTab = NotificationTab.ALL

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_student_notifications, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        setupViewModel()
        setupBackNavigation(view)
        setupRecyclerView()
        setupPullToRefresh()
        setupActions(view)
        setupTabs(view)
        observeNotifications()
        setupNotificationRefreshObserver()
        viewModel.loadNotifications()
    }

    private fun bindViews(view: View) {
        recyclerView = view.findViewById(R.id.rvNotifications)
        swipeRefreshLayout = view.findViewById(R.id.notificationsSwipeRefresh)
        progressBar = view.findViewById(R.id.progressNotifications)
        stateLayout = view.findViewById(R.id.layoutNotificationsState)
        stateIcon = view.findViewById(R.id.imageNotificationsState)
        stateTitle = view.findViewById(R.id.textNotificationsStateTitle)
        stateMessage = view.findViewById(R.id.textNotificationsStateMessage)
        stateAction = view.findViewById(R.id.buttonNotificationsStateAction)
        unreadBadge = view.findViewById(R.id.textNotificationsUnreadBadge)
        markAllReadButton = view.findViewById(R.id.buttonMarkAllRead)
        markAllReadText = view.findViewById(R.id.textMarkAllRead)
        markAllReadIcon = view.findViewById(R.id.iconMarkAllRead)
    }

    private fun setupViewModel() {
        val tokenManager = (requireActivity().application as MainApplication).tokenManager
        val apiService = RetrofitClient.getApiService(tokenManager)
        val repository = NotificationRepository(apiService, tokenManager)

        viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return NotificationsViewModel(repository) as T
                }
            }
        )[NotificationsViewModel::class.java]
    }

    private fun setupBackNavigation(view: View) {
        val goBack = {
            if (parentFragmentManager.backStackEntryCount > 0) {
                parentFragmentManager.popBackStack()
            } else {
                activity?.findViewById<BottomNavigationView>(R.id.bottomNavigation)
                    ?.selectedItemId = R.id.nav_dashboard
            }
        }

        view.findViewById<View>(R.id.buttonNotificationsBack)?.setOnClickListener { goBack() }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    goBack()
                }
            }
        )
    }

    private fun setupRecyclerView() {
        adapter = NotificationsAdapter(
            onNotificationClick = { notification ->
                showNotificationDetails(notification)
                if (notification.isUnread()) {
                    viewModel.markAsRead(notification)
                }
            },
            onDeleteClick = { notification -> showDeleteConfirmation(notification) }
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        recyclerView.isNestedScrollingEnabled = false
        recyclerView.itemAnimator = DefaultItemAnimator().apply {
            addDuration = 180
            changeDuration = 160
            moveDuration = 180
            removeDuration = 160
        }
    }

    private fun setupPullToRefresh() {
        swipeRefreshLayout.setColorSchemeResources(
            R.color.notifications_primary,
            R.color.notifications_green,
            R.color.notifications_orange
        )
        swipeRefreshLayout.setOnRefreshListener {
            runIfOnline(onOffline = { swipeRefreshLayout.isRefreshing = false }) {
                viewModel.refreshNotifications()
            }
        }
    }

    private fun setupActions(view: View) {
        view.findViewById<View>(R.id.buttonNotificationsRefresh)?.setOnClickListener {
            runIfOnline { viewModel.refreshNotifications() }
        }
        markAllReadButton.setOnClickListener {
            val unread = viewModel.unreadCount.value ?: 0
            if (unread <= 0) return@setOnClickListener
            AlertDialog.Builder(requireContext())
                .setTitle("Mark all notifications as read?")
                .setMessage("This will remove unread indicators from all notifications.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Mark all") { _, _ ->
                    runIfOnline(NetworkMessages.OFFLINE_RETRY) { viewModel.markAllAsRead() }
                }
                .show()
        }
        stateAction.setOnClickListener {
            runIfOnline(NetworkMessages.OFFLINE_RETRY) { viewModel.refreshNotifications() }
        }
    }

    override fun refreshAfterOnline() {
        viewModel.refreshNotifications()
    }

    private fun setupTabs(view: View) {
        view.findViewById<View>(R.id.tabNotificationsAll)?.setOnClickListener {
            switchTab(NotificationTab.ALL)
        }
        view.findViewById<View>(R.id.tabNotificationsUnread)?.setOnClickListener {
            switchTab(NotificationTab.UNREAD)
        }
        view.findViewById<View>(R.id.tabNotificationsRead)?.setOnClickListener {
            switchTab(NotificationTab.READ)
        }
        updateTabStyles()
    }

    private fun observeNotifications() {
        viewModel.filteredNotifications.observe(viewLifecycleOwner) { notifications ->
            adapter.submitList(notifications)
            updateContentState(notifications)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading && !swipeRefreshLayout.isRefreshing) View.VISIBLE else View.GONE
            if (isLoading && adapter.currentList.isEmpty()) {
                recyclerView.visibility = View.GONE
                stateLayout.visibility = View.GONE
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            swipeRefreshLayout.isRefreshing = false
            if (!message.isNullOrBlank()) {
                showErrorState(message)
            }
        }

        viewModel.unreadCount.observe(viewLifecycleOwner) { count ->
            unreadBadge.text = "$count unread"
            updateMarkAllButton(count)
        }

        viewModel.selectedTab.observe(viewLifecycleOwner) { tab ->
            selectedTab = tab
            updateTabStyles()
        }

        viewModel.actionMessage.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrBlank()) {
                LmsToast.show(requireContext(), message)
                viewModel.clearActionMessage()
            }
        }

        viewModel.unauthorized.observe(viewLifecycleOwner) { unauthorized ->
            if (unauthorized) navigateToUnauthorized()
        }
    }

    private fun setupNotificationRefreshObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                NotificationRefreshBus.events.collect {
                    viewModel.refreshNotifications()
                }
            }
        }
    }

    private fun switchTab(tab: NotificationTab) {
        selectedTab = tab
        updateTabStyles()
        viewModel.switchTab(tab)
    }

    private fun updateContentState(notifications: List<AppNotification>) {
        progressBar.visibility = View.GONE
        swipeRefreshLayout.isRefreshing = false
        recyclerView.visibility = if (notifications.isEmpty()) View.GONE else View.VISIBLE
        stateLayout.visibility = if (notifications.isEmpty()) View.VISIBLE else View.GONE
        stateAction.visibility = View.GONE
        stateIcon.setImageResource(R.drawable.ic_bell)
        stateIcon.setColorFilter(color(R.color.notifications_text_muted))

        when (selectedTab) {
            NotificationTab.ALL -> {
                stateTitle.text = "No notifications yet"
                stateMessage.text = "Library updates and alerts will appear here."
            }
            NotificationTab.UNREAD -> {
                stateTitle.text = "No unread notifications"
                stateMessage.text = "You are all caught up."
            }
            NotificationTab.READ -> {
                stateTitle.text = "No read notifications yet"
                stateMessage.text = "Notifications you have opened will appear here."
            }
        }
    }

    private fun showErrorState(message: String) {
        progressBar.visibility = View.GONE
        recyclerView.visibility = View.GONE
        stateLayout.visibility = View.VISIBLE
        stateAction.visibility = View.VISIBLE
        stateIcon.setImageResource(R.drawable.ic_warning)
        stateIcon.setColorFilter(color(R.color.notifications_orange))
        stateTitle.text = "Unable to load notifications"
        stateMessage.text = message.ifBlank { "Please try again." }
        stateAction.text = "Retry"
    }

    private fun updateMarkAllButton(unreadCount: Int) {
        val enabled = unreadCount > 0
        markAllReadButton.isEnabled = enabled
        markAllReadButton.alpha = if (enabled) 1f else 0.55f
        val textColor = if (enabled) R.color.notifications_primary else R.color.notifications_text_muted
        markAllReadText.setTextColor(color(textColor))
        markAllReadIcon.setColorFilter(color(textColor))
    }

    private fun updateTabStyles() {
        val styles = listOf(
            NotificationTabStyle(
                containerId = R.id.tabNotificationsAll,
                iconId = R.id.iconTabNotificationsAll,
                textId = R.id.textTabNotificationsAll,
                tab = NotificationTab.ALL
            ),
            NotificationTabStyle(
                containerId = R.id.tabNotificationsUnread,
                iconId = R.id.iconTabNotificationsUnread,
                textId = R.id.textTabNotificationsUnread,
                tab = NotificationTab.UNREAD
            ),
            NotificationTabStyle(
                containerId = R.id.tabNotificationsRead,
                iconId = R.id.iconTabNotificationsRead,
                textId = R.id.textTabNotificationsRead,
                tab = NotificationTab.READ
            )
        )

        styles.forEach { style ->
            val selected = style.tab == selectedTab
            view?.findViewById<View>(style.containerId)?.setBackgroundResource(
                if (selected) R.drawable.bg_notifications_segment_active else 0
            )
            view?.findViewById<ImageView>(style.iconId)?.setColorFilter(
                color(if (selected) R.color.white else R.color.notifications_text_muted)
            )
            view?.findViewById<TextView>(style.textId)?.setTextColor(
                color(if (selected) R.color.white else R.color.notifications_text_primary)
            )
        }
    }

    private fun showNotificationDetails(notification: AppNotification) {
        val dialog = BottomSheetDialog(requireContext())
        val detailView = layoutInflater.inflate(R.layout.bottom_sheet_notification_detail, null)
        val style = DetailStyle.from(notification.type)

        detailView.findViewById<TextView>(R.id.textDetailTitle).text =
            notification.title?.takeIf { it.isNotBlank() } ?: "Notification"
        detailView.findViewById<TextView>(R.id.textDetailMessage).text =
            notification.message?.takeIf { it.isNotBlank() } ?: "-"
        detailView.findViewById<TextView>(R.id.textDetailDate).text =
            NotificationsAdapter.formatFullDateTime(notification.createdAt).ifBlank { "-" }
        detailView.findViewById<TextView>(R.id.textDetailType).text = style.label
        detailView.findViewById<TextView>(R.id.textDetailTypeBadge).apply {
            text = style.label
            setBackgroundResource(style.badgeBackground)
            setTextColor(color(style.tintColor))
        }
        detailView.findViewById<View>(R.id.viewDetailIconTile).setBackgroundResource(style.iconBackground)
        detailView.findViewById<ImageView>(R.id.imageDetailIcon).apply {
            setImageResource(style.iconDrawable)
            setColorFilter(color(style.tintColor))
        }

        detailView.findViewById<View>(R.id.buttonNotificationDetailClose).setOnClickListener {
            dialog.dismiss()
        }
        detailView.findViewById<View>(R.id.buttonNotificationDetailOk).setOnClickListener {
            dialog.dismiss()
        }

        dialog.setContentView(detailView)
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                ?.setBackgroundColor(Color.TRANSPARENT)
        }
        dialog.show()
    }

    private fun showDeleteConfirmation(notification: AppNotification) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete notification?")
            .setMessage("This notification will be removed from your list.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                runIfOnline(NetworkMessages.OFFLINE_RETRY) { viewModel.deleteNotification(notification) }
            }
            .show()
    }

    private fun navigateToUnauthorized() {
        if (!isAdded) return
        val intent = Intent(requireContext(), UnauthorizedActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }

    private fun color(colorRes: Int): Int = ContextCompat.getColor(requireContext(), colorRes)

    private data class NotificationTabStyle(
        val containerId: Int,
        val iconId: Int,
        val textId: Int,
        val tab: NotificationTab
    )

    private data class DetailStyle(
        val label: String,
        val iconDrawable: Int,
        val iconBackground: Int,
        val badgeBackground: Int,
        val tintColor: Int
    ) {
        companion object {
            fun from(rawType: String?): DetailStyle {
                val type = rawType.orEmpty().lowercase(Locale.US)
                return when {
                    type.contains("returned") -> blue("BOOK", R.drawable.ic_book)
                    type.contains("approved") -> green("BOOK", R.drawable.ic_check_circle)
                    type.contains("rejected") -> red("BOOK", R.drawable.ic_x_circle)
                    type.contains("fine") && type.contains("paid") -> green("FINE", R.drawable.ic_rupee)
                    type.contains("fine") -> orange("FINE", R.drawable.ic_warning)
                    type.contains("account") || type.contains("status") -> purple("ACCOUNT", R.drawable.ic_shield)
                    else -> blue("INFO", R.drawable.ic_bell)
                }
            }

            private fun blue(label: String, icon: Int) = DetailStyle(
                label,
                icon,
                R.drawable.bg_notification_icon_blue,
                R.drawable.bg_notification_unread_badge,
                R.color.notifications_primary
            )

            private fun green(label: String, icon: Int) = DetailStyle(
                label,
                icon,
                R.drawable.bg_notification_icon_green,
                R.drawable.bg_notification_badge_green,
                R.color.notifications_green
            )

            private fun red(label: String, icon: Int) = DetailStyle(
                label,
                icon,
                R.drawable.bg_notification_icon_red,
                R.drawable.bg_notification_badge_red,
                R.color.notifications_red
            )

            private fun orange(label: String, icon: Int) = DetailStyle(
                label,
                icon,
                R.drawable.bg_notification_icon_orange,
                R.drawable.bg_notification_badge_orange,
                R.color.notifications_orange
            )

            private fun purple(label: String, icon: Int) = DetailStyle(
                label,
                icon,
                R.drawable.bg_notification_icon_purple,
                R.drawable.bg_notification_badge_purple,
                R.color.notifications_purple
            )
        }
    }
}
