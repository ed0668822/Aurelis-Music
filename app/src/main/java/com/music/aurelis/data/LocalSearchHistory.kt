package com.music.aurelis.data

import android.content.Context
import org.json.JSONArray

/** Bounded, ordered history in the existing private preferences scope. */
class LocalSearchHistory(context: Context) {
    private val prefs = ProfileStorage(context).open(ProfileRepository.profileId, ProfileDataArea.PREFERENCES)
    private var readable = true
    var entries: List<String> = runCatching {
        val saved = JSONArray(prefs.getString("local_search_history_v1", "[]"))
        (0 until saved.length()).map { saved.getString(it) }.take(20)
    }.getOrElse { readable = false; emptyList() }
        private set
    fun submit(query: String) = save(query.trim().take(200).let { value ->
        if (value.isBlank()) entries else listOf(value) + entries.filterNot { LocalSearch.normalize(it) == LocalSearch.normalize(value) }
    }.take(20))
    fun remove(query: String) = save(entries.filterNot { it == query })
    fun clear() = save(emptyList())
    private fun save(values: List<String>) {
        if (!readable || ProfileRepository.state.value.readOnly) return
        prefs.edit().putString("local_search_history_v1", JSONArray(values).toString()).apply()
        entries = values
    }
}
