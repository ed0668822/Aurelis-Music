// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.data.stats

import android.content.Context
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabaseCorruptException
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import com.music.aurelis.data.model.ProfileId

internal class NewerListeningSchema : SQLiteException("Listening database belongs to a newer schema")

/** Never delegate corruption to the Android handler that deletes the database. */
internal class ListeningDatabase(context: Context, profileId: ProfileId) : SQLiteOpenHelper(
    context.applicationContext, fileName(profileId), null, 1,
    DatabaseErrorHandler { throw SQLiteDatabaseCorruptException("Listening database must be preserved for recovery") },
) {
    companion object {
        fun fileName(profileId: ProfileId) = "aurelis-listening-${profileId.value}.db"
    }
    override fun onConfigure(db: SQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE listening_sessions (
            session_id TEXT PRIMARY KEY NOT NULL,
            song_id TEXT NOT NULL,
            queue_entry_id TEXT NOT NULL
        )""")
        db.execSQL("""CREATE TABLE listening_events (
            session_id TEXT NOT NULL REFERENCES listening_sessions(session_id) ON DELETE CASCADE,
            sequence INTEGER NOT NULL CHECK(sequence >= 0),
            observed_at_ms INTEGER NOT NULL CHECK(observed_at_ms >= 0),
            listened_ms INTEGER NOT NULL CHECK(listened_ms BETWEEN 1 AND 5000),
            duration_ms INTEGER NOT NULL CHECK(duration_ms >= 0),
            PRIMARY KEY(session_id, sequence)
        )""")
        db.execSQL("CREATE INDEX listening_events_observed ON listening_events(observed_at_ms, session_id, sequence)")
        db.execSQL("CREATE INDEX listening_sessions_song ON listening_sessions(song_id)")
    }
    override fun onOpen(db: SQLiteDatabase) {
        // Only after the version check: don't change journal mode of future schemas.
        db.enableWriteAheadLogging()
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        throw SQLiteException("No destructive listening database upgrade is permitted")
    }
    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        throw NewerListeningSchema()
    }
}
