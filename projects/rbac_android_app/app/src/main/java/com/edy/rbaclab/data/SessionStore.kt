package com.edy.rbaclab.data

import android.content.Context
import androidx.core.content.edit
import com.edy.rbaclab.BuildConfig

class SessionStore(context: Context) {
    private val preferences = context.getSharedPreferences("rbac_lab_session", Context.MODE_PRIVATE)

    var baseUrl: String
        get() = preferences.getString(KEY_BASE_URL, BuildConfig.DEFAULT_BASE_URL)
            ?: BuildConfig.DEFAULT_BASE_URL
        set(value) {
            preferences.edit { putString(KEY_BASE_URL, value.trimEnd('/')) }
        }

    var accessToken: String?
        get() = preferences.getString(KEY_ACCESS_TOKEN, null)
        set(value) {
            preferences.edit {
                if (value == null) remove(KEY_ACCESS_TOKEN) else putString(KEY_ACCESS_TOKEN, value)
            }
        }

    companion object {
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_ACCESS_TOKEN = "access_token"
    }
}
