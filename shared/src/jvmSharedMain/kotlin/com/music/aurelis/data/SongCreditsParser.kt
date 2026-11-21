// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.data

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.util.Locale

/** Explicitly attributed people; never infer an unreported role. */
data class SongCredit(val role: String, val names: List<String>) {
    val text: String get() = names.joinToString(", ")
}

/** Parses only role-labeled credits belonging to the requested song. */
internal object SongCreditsParser {
    fun parsePage(page: JsonElement): List<SongCredit> {
        val sections = nodes(page, "dismissableDialogContentSectionRenderer")
            .mapNotNull { section ->
                credit(textOf(section["title"]), textOf(section["subtitle"]))
            }.toList()
        if (sections.isNotEmpty()) return unique(sections)

        val entries = nodes(page, "musicResponsiveListItemRenderer")
            .mapNotNull { row ->
                val flex = row["flexColumns"] as? JsonArray
                val values = flex?.map(::textOf).orEmpty()
                if (values.isNotEmpty()) {
                    credit(values.first(), values.drop(1).joinToString("\n"))
                } else {
                    credit(textOf(row["title"]), textOf(row["subtitle"]))
                }
            }.toList()
        return unique(entries)
    }

    /** Restrict menu traversal to the actual queued video, not recommendations. */
    fun findEndpoint(next: JsonElement, videoId: String): JsonObject? {
        val matchingRows = nodes(next, "playlistPanelVideoRenderer")
            .filter { string(it["videoId"]) == videoId }.toList()
        for (row in matchingRows) {
            val navigation = nodes(row, "menuNavigationItemRenderer").mapNotNull { item ->
                val endpoint = ((item["navigationEndpoint"] as? JsonObject)
                    ?.get("browseEndpoint") as? JsonObject) ?: return@mapNotNull null
                val id = string(endpoint["browseId"])
                val config = (endpoint["browseEndpointContextSupportedConfigs"] as? JsonObject)
                    ?.get("browseEndpointContextMusicConfig") as? JsonObject
                val pageType = string(config?.get("pageType"))
                if (id.startsWith("MPTC") || pageType.contains("TRACK_CREDITS") ||
                    textOf(item["text"]).contains("credits", ignoreCase = true)
                ) endpoint else null
            }.firstOrNull()
            if (navigation != null) return navigation
        }
        // A direct video-specific endpoint can be used even without a queue row.
        return nodes(next, "browseEndpoint")
            .firstOrNull { string(it["browseId"]) == "MPTC$videoId" }
    }

    /** Public watch pages sometimes provide credits when the Music browse page cannot. */
    fun parseWatchMetadata(data: JsonObject?): List<SongCredit> {
        if (data == null) return emptyList()
        val credits = mutableListOf<SongCredit>()
        for (section in nodes(data, "videoDescriptionMusicSectionRenderer")) {
            for (row in nodes(section, "infoRowRenderer") +
                    nodes(section, "metadataRowRenderer") +
                    nodes(section, "premiumMetadataRowRenderer")) {
                val role = textOf(row["title"])
                val names = listOfNotNull(
                    row["expandedMetadata"], row["defaultMetadata"], row["contents"], row["content"]
                ).map(::textOf).firstOrNull { it.isNotBlank() }.orEmpty()
                credit(role, names)?.let(credits::add)
            }
        }
        return unique(credits)
    }

    private fun credit(role: String, rawNames: String): SongCredit? {
        val title = role.trim()
        val names = rawNames.split(Regex("\\r?\\n+|\\s*[•·]\\s*|,\\s+(?=[\\p{Lu}])"))
            .map(String::trim).filter(String::isNotEmpty)
        return if (title.isBlank() || names.isEmpty()) null else SongCredit(title, names)
    }

    private fun unique(credits: List<SongCredit>): List<SongCredit> =
        credits.distinctBy { it.role.lowercase(Locale.ROOT) to it.text.lowercase(Locale.ROOT) }

    private fun string(element: JsonElement?): String =
        (element as? JsonPrimitive)?.contentOrNull.orEmpty()

    private fun textOf(element: JsonElement?): String = when (element) {
        is JsonPrimitive -> element.contentOrNull.orEmpty()
        is JsonArray -> element.joinToString("") { textOf(it) }
        is JsonObject -> {
            string(element["simpleText"]).ifBlank {
                (element["runs"] as? JsonArray)?.joinToString("") { run ->
                    textOf((run as? JsonObject)?.get("text"))
                }?.takeIf(String::isNotBlank) ?: listOf(
                    "text", "title", "subtitle", "musicResponsiveListItemFlexColumnRenderer",
                    "defaultMetadata", "expandedMetadata"
                ).firstNotNullOfOrNull { key ->
                    element[key]?.let(::textOf)?.takeIf(String::isNotBlank)
                }.orEmpty()
            }
        }
        else -> ""
    }

    private fun nodes(root: JsonElement, key: String): Sequence<JsonObject> = sequence {
        when (root) {
            is JsonObject -> for ((name, child) in root) {
                if (name == key && child is JsonObject) yield(child)
                yieldAll(nodes(child, key))
            }
            is JsonArray -> for (child in root) yieldAll(nodes(child, key))
            else -> Unit
        }
    }
}
