// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.data

import android.content.Context
import android.net.Uri
import com.music.aurelis.data.model.LocalSong
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.merge
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

    override val changes: Flow<Unit> get() = merge(
        SettingsRepository.libraryFolderRevision.drop(1).map { Unit },
        callbackFlow {
        val resolver = appContext.contentResolver
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            // Some providers call the legacy callback and others pass a changed URI.
            // Both must invalidate the same MediaStore catalogue without polling.
            override fun onChange(selfChange: Boolean) { trySend(Unit) }
            override fun onChange(selfChange: Boolean, uri: Uri?) { trySend(Unit) }
        }
        resolver.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, observer)
        trySend(Unit) // Register before the first read so changes during it are not lost.
        awaitClose { resolver.unregisterContentObserver(observer) }
    }.buffer(Channel.CONFLATED))

    override suspend fun songs(filterNonMusic: Boolean): List<LocalSong> =
        LocalMediaRepository.getLibrarySongs(appContext, filterNonMusic)
}
