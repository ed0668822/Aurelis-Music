package com.music.aurelis.data

import android.content.Context
import com.music.aurelis.data.model.LocalSong
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.buffer

/** Device catalogue for the sole local profile. Implementations propagate cancellation. */
fun interface LocalLibrarySource {
    suspend fun songs(filterNonMusic: Boolean): List<LocalSong>
    /** Initial event after subscription, followed by catalogue changes. */
    val changes: Flow<Unit> get() = flowOf(Unit)
}

/** Adapter around the existing scanner; no second scanner or playback engine. */
class MediaStoreLibrarySource(context: Context) : LocalLibrarySource {
    private val appContext = context.applicationContext

    override val changes: Flow<Unit> get() = callbackFlow {
        val resolver = appContext.contentResolver
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { trySend(Unit) }
        }
        resolver.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, observer)
        trySend(Unit) // Register before the first read so changes during it are not lost.
        awaitClose { resolver.unregisterContentObserver(observer) }
    }.buffer(Channel.CONFLATED)

    override suspend fun songs(filterNonMusic: Boolean): List<LocalSong> =
        LocalMediaRepository.getAllSongs(appContext, filterNonMusic)
}
