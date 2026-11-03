// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.data.scrobbling

/**
 * Stub vacío de ScrobbleManager.
 *
 * Aurelis es un reproductor 100% local sin login ni backend, por lo que no hay
 * scrobbling a servicios remotos (Last.fm, etc.). Se mantiene la clase para que
 * cualquier referencia existente compile, pero todas las operaciones son no-op.
 */
object ScrobbleManager {
    fun scrobble(songId: String, playedSeconds: Int) {
        // No-op: sin cuenta ni backend remoto.
    }

    fun nowPlaying(songId: String) {
        // No-op.
    }
}
