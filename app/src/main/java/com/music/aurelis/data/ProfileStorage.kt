package com.music.aurelis.data

import android.content.Context
import android.content.SharedPreferences
import com.music.aurelis.data.model.ProfileId

/** Storage boundaries for repositories; opening a store does not create user data. */
enum class ProfileDataArea(internal val legacyFile: String, internal val key: String) {
    PROFILE("aurelis_profile", "profile"),
    PREFERENCES("aurelis_settings", "preferences"),
    FAVORITES("aurelis_favorites", "favorites"),
    PLAYLISTS("aurelis_playlists", "playlists"),
    HISTORY("aurelis_history", "history"),
    QUEUE("aurelis_queue", "queue"),
}

/**
 * The default profile keeps the existing files, keys and unknown values intact.
 * Other profiles use a separate private file per area; IDs cannot contain dots,
 * so profile and area boundaries cannot collide. Device-wide stores (widget,
 * app metadata), MediaStore and shared media caches do not belong here.
 *
 * This provides storage scopes, not profile creation, switching or deletion.
 */
class ProfileStorage(context: Context) {
    private val appContext = context.applicationContext

    fun open(profileId: ProfileId, area: ProfileDataArea): SharedPreferences {
        val file = if (profileId == ProfileId.DEFAULT) {
            area.legacyFile
        } else {
            "aurelis_profiles.${profileId.value}.${area.key}"
        }
        return appContext.getSharedPreferences(file, Context.MODE_PRIVATE)
    }
}
