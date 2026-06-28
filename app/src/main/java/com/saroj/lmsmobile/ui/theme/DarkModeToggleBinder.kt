package com.saroj.lmsmobile.ui.theme

import android.widget.CompoundButton
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.switchmaterial.SwitchMaterial
import com.saroj.lmsmobile.R
import com.saroj.lmsmobile.storage.ThemePreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import android.view.View

object DarkModeToggleBinder {
    fun bind(root: View, lifecycleOwner: LifecycleOwner, scope: CoroutineScope) {
        val row = root.findViewById<View>(R.id.rowDarkMode) ?: return
        val switch = root.findViewById<SwitchMaterial>(R.id.switchDarkMode) ?: return
        var updatingFromPreference = false

        val listener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
            if (!updatingFromPreference) {
                scope.launch {
                    ThemePreferenceManager.setDarkModeEnabled(root.context, isChecked)
                }
            }
        }

        switch.setOnCheckedChangeListener(listener)
        row.setOnClickListener {
            switch.isChecked = !switch.isChecked
        }

        scope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                ThemePreferenceManager.isDarkModeEnabled(root.context).collect { enabled ->
                    if (switch.isChecked != enabled) {
                        updatingFromPreference = true
                        switch.isChecked = enabled
                        updatingFromPreference = false
                    }
                }
            }
        }
    }
}
