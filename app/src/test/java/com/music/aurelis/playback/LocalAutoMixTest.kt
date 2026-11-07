package com.music.aurelis.playback

import com.music.aurelis.data.*
import com.music.aurelis.data.model.LocalSong
import org.junit.Assert.*
import org.junit.Test

class LocalAutoMixTest {
    private fun song(id: String, artist: String = "Artist $id", genre: String? = null) = LocalSong(id, "Track $id", artist,
        "Album", "content://media/external/audio/media/$id", null, 60_000, null, 1, 2026, genre = genre)
    @Test fun candidatesAreOnlyUniqueAvailableLocalAudioAndLimitsAreBounded() {
        val songs = listOf(song("1"), song("1"), song("remote").copy(contentUri = "https://stream.example/audio"),
            song("short").copy(durationMs = 0), song("").copy(id = ""))
        assertEquals(listOf("1"), LocalAutoMix.build(songs, LocalUserLibraryState()).map { it.id })
        assertTrue(LocalAutoMix.build(songs, LocalUserLibraryState(), limit = 0).isEmpty())
        assertTrue(LocalAutoMix.build(emptyList(), LocalUserLibraryState()).isEmpty())
        assertEquals(50, LocalAutoMix.build((1..100).map { song(it.toString()) }, LocalUserLibraryState(), limit = 999).size)
    }
    @Test fun genreAffinityAndFavoritesInfluenceRealCandidateOrder() {
        val seed = song("seed", "A", "Pop")
        val sameGenre = song("genre", "B", "Pop")
        val other = song("other", "C", "Rock")
        assertEquals("genre", LocalAutoMix.build(listOf(seed, other, sameGenre), LocalUserLibraryState(), "seed")[1].id)
        val missingGenre = listOf(seed.copy(genre = null), sameGenre.copy(genre = null), other.copy(genre = null))
        assertEquals("other", LocalAutoMix.build(missingGenre, LocalUserLibraryState(favorites = setOf("other")), "seed")[1].id)
    }
    @Test fun recentTracksWaitBehindFreshTracksAndSmallLibrariesCanStillMakeAMix() {
        val history = mapOf("2" to LocalListening(30_000, 1, 99_999))
        val user = LocalUserLibraryState(listening = history, favorites = setOf("2"))
        val mix = LocalAutoMix.build(listOf(song("1"), song("2"), song("3")), user, "1", nowMs = 100_000)
        assertEquals(listOf("1", "3", "2"), mix.map { it.id })
        assertEquals(listOf("1", "2"), LocalAutoMix.build(listOf(song("1"), song("2")), user, "1", nowMs = 100_000).map { it.id })
    }
    @Test fun artistCapsIncludeSeedAndCollaborationAndRecordingsNeverRepeat() {
        val songs = (1..8).map { song("a$it", "A") } + (1..6).map { song("b$it", "B") } +
            listOf(song("dupe", "A").copy(title = "Track a1 (Official Audio)"))
        val mix = LocalAutoMix.build(songs, LocalUserLibraryState(), "a1", limit = 50)
        assertEquals(4, mix.count { it.artist == "A" }); assertEquals(2, mix.count { it.artist == "B" })
        assertFalse(mix.any { it.id == "dupe" })
        assertEquals(mix.size, mix.map { it.id }.distinct().size)
        assertTrue(QueueBuilder.extend((1..4).map { song("s$it", "A") }, listOf(song("next", "A & B")), 1).isEmpty())
    }
    @Test fun absentGenresAndUnknownArtistsDoNotCreateFakeFeaturesOrStarveTheQueue() {
        val songs = (1..7).map { song(it.toString(), "Artista desconocido") }
        val mix = LocalAutoMix.build(songs, LocalUserLibraryState(), "1")
        assertEquals(7, mix.size); assertTrue(mix.all { it.genre == null })
        assertEquals("2", LocalAutoMix.build(songs, LocalUserLibraryState(listening = mapOf("missing" to LocalListening(lastPlayedAt = 10), "2" to LocalListening(lastPlayedAt = 5)))).first().id)
    }
    @Test fun sameInputsAreDeterministicAndExplicitRegenerationVariesTies() {
        val songs = (1..20).map { song(it.toString()) }
        val user = LocalUserLibraryState()
        val first = LocalAutoMix.build(songs, user, "1", variation = 1)
        assertEquals(first, LocalAutoMix.build(songs.reversed(), user, "1", variation = 1))
        val variations = (1L..5L).map { LocalAutoMix.build(songs, user, "1", variation = it).map { row -> row.id } }.toSet()
        assertTrue(variations.size > 1)
    }
}
