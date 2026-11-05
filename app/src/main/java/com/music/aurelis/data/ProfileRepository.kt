// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.data

import androidx.core.content.edit
import android.content.Context
import android.content.SharedPreferences
import com.music.aurelis.data.model.ProfileId

object ProfileRepository {
    val profileId: ProfileId = ProfileId.DEFAULT
    private const val KEY_NAME = "user_name"
    private const val KEY_PHOTO = "user_photo"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = ProfileStorage(context).open(profileId, ProfileDataArea.PROFILE)
    }

    var userName: String
        get() = if (::prefs.isInitialized) prefs.getString(KEY_NAME, "Aurelis User") ?: "Aurelis User" else "Aurelis User"
        set(value) = prefs.edit { putString(KEY_NAME, value) }

    var userPhotoUri: String?
        get() = if (::prefs.isInitialized) prefs.getString(KEY_PHOTO, null) else null
        set(value) = prefs.edit { putString(KEY_PHOTO, value) }
}
