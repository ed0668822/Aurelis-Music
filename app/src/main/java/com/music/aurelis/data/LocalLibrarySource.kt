package com.music.aurelis.data

import android.content.Context
import com.music.aurelis.data.model.LocalSong

/** Device library shared across profiles. Implementations propagate cancellation. */
fun interface LocalLibrarySource {
    suspend fun songs(filterNonMusic: Boolean): List<LocalSong>
}

/** Adapter around the existing scanner; no second scanner or playback engine. */
class MediaStoreLibrarySource(context: Context) : LocalLibrarySource {
    private val appContext = context.applicationContext

    override suspend fun songs(filterNonMusic: Boolean): List<LocalSong> =
        LocalMediaRepository.getAllSongs(appContext, filterNonMusic)
}
