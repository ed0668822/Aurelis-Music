// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Looper
import com.music.aurelis.data.model.ProfileId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class LocalProfile(val id: ProfileId, val name: String, val avatar: String?)
data class LocalProfilesState(val profiles: List<LocalProfile> = emptyList(), val activeId: ProfileId = ProfileId.DEFAULT,
    val readOnly: Boolean = false, val error: String? = null)

interface ProfileSessionListener {
    fun beforeProfileChange(previous: ProfileId, next: ProfileId) {}
    fun afterProfileChange(active: ProfileId) {}
}

/** Registry is device-wide. Existing default-profile files/keys are never migrated or copied. */
object ProfileRepository {
    private lateinit var app: Context
    private lateinit var registry: SharedPreferences
    private val listeners = linkedSetOf<ProfileSessionListener>()
    private var deleting = emptyList<ProfileId>()
    private var changing = false
    private val mutableState = MutableStateFlow(LocalProfilesState())
    val state = mutableState.asStateFlow()
    val profileId get() = state.value.activeId
    fun addListener(listener: ProfileSessionListener) { listeners.add(listener) }
    fun removeListener(listener: ProfileSessionListener) { listeners.remove(listener) }

    fun init(context: Context) {
        if (!::app.isInitialized || app !== context.applicationContext) listeners.clear()
        app = context.applicationContext
        registry = app.getSharedPreferences("aurelis_profiles_registry", Context.MODE_PRIVATE)
        deleting = emptyList()
        mutableState.value = try {
            val raw = registry.getString("profiles_v1", null)
            if (raw == null) {
                persist(listOf(ProfileId.DEFAULT), ProfileId.DEFAULT)
                LocalProfilesState(listOf(readProfile(ProfileId.DEFAULT)))
            } else {
                val root = JSONObject(raw); require(root.getInt("version") == 1)
                val ids = ids(root.getJSONArray("ids")); require(ids.isNotEmpty() && ids.distinct().size == ids.size)
                val active = ProfileId(root.getString("active")); require(active in ids)
                deleting = root.optJSONArray("deleting")?.let(::ids).orEmpty()
                require(deleting.none { it in ids })
                LocalProfilesState(ids.map(::readProfile), active)
            }
        } catch (_: Exception) {
            LocalProfilesState(listOf(readProfile(ProfileId.DEFAULT)), readOnly = true,
                error = "No se pudieron leer los perfiles. Los datos guardados se han conservado sin sobrescribirlos.")
        }
        if (!state.value.readOnly && deleting.isNotEmpty()) cleanupPending()
    }
    private fun ids(array: JSONArray) = (0 until array.length()).map { ProfileId(array.getString(it)) }
    private fun readProfile(id: ProfileId): LocalProfile {
        val prefs = ProfileStorage(app).open(id, ProfileDataArea.PROFILE)
        return LocalProfile(id, runCatching { prefs.getString("user_name", null) }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Aurelis User",
            runCatching { prefs.getString("user_photo", null) }.getOrNull())
    }
    private fun persist(ids: List<ProfileId>, active: ProfileId, pending: List<ProfileId> = deleting) {
        check(registry.edit().putString("profiles_v1", JSONObject().put("version", 1)
            .put("ids", JSONArray(ids.map { it.value })).put("active", active.value)
            .put("deleting", JSONArray(pending.map { it.value })).toString()).commit()) { "No se pudieron guardar los perfiles." }
    }
    private fun writable() {
        check(Looper.myLooper() == Looper.getMainLooper()) { "Profile operations must run on the main thread" }
        check(!state.value.readOnly && !changing) { "Los perfiles no están disponibles para cambios." }
    }
    private fun title(name: String, excluding: ProfileId? = null): String {
        val value = name.trim(); require(value.isNotEmpty() && value.length <= 60) { "Usa un nombre de 1 a 60 caracteres." }
        require(state.value.profiles.none { it.id != excluding && it.name.equals(value, true) }) { "Ya existe un perfil con ese nombre." }
        return value
    }
    fun create(name: String): ProfileId {
        writable(); check(state.value.profiles.size < 20) { "Puedes guardar hasta 20 perfiles locales." }
        val value = title(name); val id = ProfileId(UUID.randomUUID().toString())
        val prefs = ProfileStorage(app).open(id, ProfileDataArea.PROFILE)
        check(prefs.edit().putString("user_name", value).commit()) { "No se pudo crear el perfil." }
        try { persist(state.value.profiles.map { it.id } + id, profileId) }
        catch (error: Exception) { app.deleteSharedPreferences("aurelis_profiles.${id.value}.profile"); throw error }
        mutableState.value = state.value.copy(profiles = state.value.profiles + readProfile(id), error = null)
        return id
    }
    fun rename(id: ProfileId, name: String) {
        writable(); require(state.value.profiles.any { it.id == id })
        val value = title(name, id)
        check(ProfileStorage(app).open(id, ProfileDataArea.PROFILE).edit().putString("user_name", value).commit())
        refresh()
    }
    fun switchTo(id: ProfileId) {
        writable(); require(state.value.profiles.any { it.id == id })
        if (id == profileId) return
        val previous = state.value; val callbacks = listeners.toList(); changing = true
        try {
            callbacks.forEach { it.beforeProfileChange(previous.activeId, id) }
            persist(previous.profiles.map { it.id }, id)
            mutableState.value = previous.copy(activeId = id, error = null)
            callbacks.forEach { it.afterProfileChange(id) }
        } catch (error: Exception) {
            runCatching { persist(previous.profiles.map { it.id }, previous.activeId) }
            mutableState.value = previous.copy(readOnly = true, error = "No se pudo completar el cambio. Reinicia la app antes de modificar los perfiles.")
            callbacks.forEach { runCatching { it.afterProfileChange(previous.activeId) } }
            throw IllegalStateException(state.value.error, error)
        } finally { changing = false }
    }
    /** Called only after the UI has confirmed the named profile and its data. */
    fun delete(id: ProfileId) {
        writable(); require(state.value.profiles.any { it.id == id })
        check(state.value.profiles.size > 1) { "Crea otro perfil antes de eliminar el último." }
        if (id == profileId) switchTo(state.value.profiles.first { it.id != id }.id)
        val remaining = state.value.profiles.filterNot { it.id == id }
        val pending = (deleting + id).distinct()
        persist(remaining.map { it.id }, profileId, pending)
        deleting = pending; mutableState.value = state.value.copy(profiles = remaining)
        cleanupPending()
    }
    private fun cleanupPending() {
        val failed = deleting.filter { id ->
            runCatching {
                ProfileDataArea.entries.forEach { area -> check(ProfileStorage(app).open(id, area).edit().clear().commit()) }
                val folder = avatarDirectory(id)
                check(!folder.exists() || folder.deleteRecursively())
            }.isFailure
        }
        val saved = runCatching { persist(state.value.profiles.map { it.id }, profileId, failed) }.isSuccess
        if (saved) deleting = failed
        if (!saved || failed.isNotEmpty()) mutableState.value = state.value.copy(error = "Quedaron datos pendientes de limpieza. Se reintentará al abrir la app.")
    }
    private fun refresh() { mutableState.value = state.value.copy(profiles = state.value.profiles.map { readProfile(it.id) }, error = null) }
    internal fun avatarDirectory(id: ProfileId) = File(app.filesDir, "profiles/${id.value}")
    internal fun setAvatar(id: ProfileId, uri: String?) {
        writable(); require(state.value.profiles.any { it.id == id })
        check(ProfileStorage(app).open(id, ProfileDataArea.PROFILE).edit().putString("user_photo", uri).commit())
        refresh()
    }
    var userName: String
        get() = if (::app.isInitialized) readProfile(profileId).name else "Aurelis User"
        set(value) { rename(profileId, value) }
    var userPhotoUri: String?
        get() = if (::app.isInitialized) readProfile(profileId).avatar else null
        set(value) { setAvatar(profileId, value) }
}
