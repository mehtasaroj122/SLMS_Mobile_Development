package com.saroj.lmsmobile.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

val Context.lmsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "lms_mobile_app_prefs"
)
