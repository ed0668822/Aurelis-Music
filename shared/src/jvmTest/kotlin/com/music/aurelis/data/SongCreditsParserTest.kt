// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive

class SongCreditsParserTest {
    @Test fun readsExplicitCreditRolesFromMusicDialog() {
        val json = Json.parseToJsonElement("""
            {"sections":[
              {"dismissableDialogContentSectionRenderer":{
                "title":{"runs":[{"text":"Produced by"}]},
                "subtitle":{"runs":[{"text":"Alex Producer"}]}
              }},
              {"dismissableDialogContentSectionRenderer":{
                "title":{"simpleText":"Written by"},
                "subtitle":{"runs":[{"text":"Ana Composer"},{"text":"\n"},{"text":"Ben Writer"}]}
              }}
            ]}
        """.trimIndent())
        val found = SongCreditsParser.parsePage(json)
        assertEquals(listOf("Produced by", "Written by"), found.map { it.role })
        assertEquals(listOf("Ana Composer", "Ben Writer"), found[1].names)
    }

    @Test fun readsResponsiveRowsAndDoesNotInventMissingCredits() {
        val json = Json.parseToJsonElement("""
            {"contents":[
              {"musicResponsiveListItemRenderer":{"flexColumns":[
                {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"Performed by"}]}}},
                {"musicResponsiveListItemFlexColumnRenderer":{"text":{"simpleText":"A. Artist"}}}
              ]}},
              {"musicResponsiveListItemRenderer":{"flexColumns":[
                {"musicResponsiveListItemFlexColumnRenderer":{"text":{"simpleText":"Produced by"}}},
                {"musicResponsiveListItemFlexColumnRenderer":{"text":{"simpleText":""}}}
              ]}}
            ]}
        """.trimIndent())
        assertEquals(listOf(SongCredit("Performed by", listOf("A. Artist"))), SongCreditsParser.parsePage(json))
    }

    @Test fun creditsMenuMustBelongToRequestedTrack() {
        val json = Json.parseToJsonElement("""
            {"playlist":[
              {"playlistPanelVideoRenderer":{"videoId":"badtrack123",
                "menuNavigationItemRenderer":{"text":{"simpleText":"View song credits"},
                  "navigationEndpoint":{"browseEndpoint":{"browseId":"MPTCbadtrack123"}}}}},
              {"playlistPanelVideoRenderer":{"videoId":"abcdefghijk",
                "menuNavigationItemRenderer":{"text":{"simpleText":"View song credits"},
                  "navigationEndpoint":{"browseEndpoint":{"browseId":"MPTCabcdefghijk"}}}}}
            ]}
        """.trimIndent())
        val endpoint = SongCreditsParser.findEndpoint(json, "abcdefghijk")
        assertEquals("MPTCabcdefghijk", (endpoint?.get("browseId") as JsonPrimitive).content)
        assertNull(SongCreditsParser.findEndpoint(json, "notpresent1"))
    }

    @Test fun acceptsOnlyAttributedWatchPageMetadata() {
        val page = Json.parseToJsonElement("""
            {"videoDescriptionMusicSectionRenderer":{
              "infoRowRenderer":{"title":{"simpleText":"Composer"},
                "defaultMetadata":{"runs":[{"text":"M. Composer"}]}},
              "other":{"title":{"simpleText":"Guess"}}
            }}
        """.trimIndent()) as kotlinx.serialization.json.JsonObject
        assertEquals(listOf(SongCredit("Composer", listOf("M. Composer"))),
            SongCreditsParser.parseWatchMetadata(page))
        assertEquals(emptyList(), SongCreditsParser.parseWatchMetadata(null))
    }
}
