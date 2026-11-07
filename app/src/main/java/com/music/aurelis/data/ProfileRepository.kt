// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.data

import android.content.Context
import android.os.Looper
import com.music.aurelis.data.model.ProfileId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class LocalProfile(val id: ProfileId, val name: String, val avatar: String?)
data class LocalProfileState(
    val profile: LocalProfile = LocalProfile(ProfileId.DEFAULT, "Aurelis User", null),
    val readOnly: Boolean = false,
    val error: String? = null,
)

/** A single local profile. Its immutable storage ID can come from an older installation. */
object ProfileRepository {
    private lateinit var app: Context
    private val mutableState = MutableStateFlow(LocalProfileState())
    val state = mutableState.asStateFlow()
    val profileId get() = state.value.profile.id

    fun init(context: Context) {
        app = context.applicationContext
        val migration = SingleProfileMigration(app)
        mutableState.value = try {
            LocalProfileState(readProfile(migration.resolve()))
        } catch (_: Exception) {
            LocalProfileState(readProfile(migration.readableId()), readOnly = true,
                error = "No se pudo verificar el respaldo del perfil. Tus datos se han conservado sin sobrescribirlos.")
        }
    }
    private fun readProfile(id: ProfileId): LocalProfile {
        val prefs = ProfileStorage(app).open(id, ProfileDataArea.PROFILE)
        return LocalProfile(id, runCatching { prefs.getString("user_name", null) }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Aurelis User",
            runCatching { prefs.getString("user_photo", null) }.getOrNull())
    }
    private fun writable() {
        check(Looper.myLooper() == Looper.getMainLooper()) { "Profile operations must run on the main thread" }
        check(!state.value.readOnly) { "El perfil está en modo de solo lectura." }
    }
    fun rename(name: String) {
        writable()
        val value = name.trim(); require(value.isNotEmpty() && value.length <= 60) { "Usa un nombre de 1 a 60 caracteres." }
        check(ProfileStorage(app).open(profileId, ProfileDataArea.PROFILE).edit().putString("user_name", value).commit())
        refresh()
    }
    private fun refresh() { mutableState.value = state.value.copy(profile = readProfile(profileId), error = null) }
    internal fun avatarDirectory(id: ProfileId) = File(app.filesDir, "profiles/${id.value}")
    internal fun setAvatar(id: ProfileId, uri: String?) {
        writable(); require(id == profileId)
        check(ProfileStorage(app).open(id, ProfileDataArea.PROFILE).edit().putString("user_photo", uri).commit())
        refresh()
    }
    var userName: String
        get() = if (::app.isInitialized) readProfile(profileId).name else "Aurelis User"
        set(value) { rename(value) }
    var userPhotoUri: String?
        get() = if (::app.isInitialized) readProfile(profileId).avatar else null
        set(value) { setAvatar(profileId, value) }
}
