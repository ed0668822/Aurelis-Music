// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.data.stats

import android.os.SystemClock
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal data class ListeningCaptureStatus(val pending: Int = 0, val issue: ListeningStoreIssue? = null, val missed: Long = 0)

/** Storage owner only: never holds a Player, Service or Activity, and never blocks a submitter. */
internal class ListeningEventWriter(
    private val sink: ListeningEventSink,
    private val journal: ListeningJournal,
    private val writeAllowed: () -> Boolean,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
    queueCapacity: Int = 128,
    private val pendingLimit: Int = ListeningEventJournal.MAX_EVENTS,
    private val retryDelayMs: Long = 5000L,
) {
    private companion object { val journalLocks = ConcurrentHashMap<String, Mutex>() }
    init { require(queueCapacity > 0 && pendingLimit in 1..ListeningEventJournal.MAX_EVENTS && retryDelayMs > 0) }
    private val incoming = Channel<ListeningEvent>(queueCapacity)
    private val accepting = AtomicBoolean(true)
    private val mutableStatus = MutableStateFlow(ListeningCaptureStatus())
    val status = mutableStatus.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    val completion: Job = scope.launch {
        var entered = false
        try {
            journalLocks.computeIfAbsent(journal.storageKey) { Mutex() }.withLock { entered = true; consume() }
        } catch (cancelled: CancellationException) { throw cancelled }
          catch (_: Exception) { mutableStatus.update { it.copy(issue = ListeningStoreIssue.IO_ERROR) } }
        finally {
            if (!entered) withContext(NonCancellable) {
                accepting.set(false); incoming.close()
                var lost = 0
                while (incoming.tryReceive().isSuccess) lost++
                missed(lost)
                try { sink.close() } catch (_: Exception) { mutableStatus.update { it.copy(issue = ListeningStoreIssue.IO_ERROR) } }
            }
        }
    }.also { it.invokeOnCompletion { scope.cancel() } }

    /** True means queued, not committed. Failures/loss remain explicit in status. */
    fun record(event: ListeningEvent): Boolean {
        if (!accepting.get()) return false
        if (!writeAllowed()) { mutableStatus.update { it.copy(issue = ListeningStoreIssue.PROFILE_READ_ONLY) }; return false }
        if (!event.valid()) { missed(1); mutableStatus.update { it.copy(issue = ListeningStoreIssue.INVALID_EVENT) }; return false }
        if (incoming.trySend(event).isSuccess) return true
        missed(1); return false
    }
    fun close(): Job { accepting.set(false); incoming.close(); return completion }
    private fun missed(count: Int) {
        if (count > 0) mutableStatus.update { it.copy(missed = it.missed.coerceAtMost(Long.MAX_VALUE - count) + count) }
    }
    private suspend fun consume() {
        val pending = mutableListOf<ListeningEvent>()
        val durable = hashSetOf<ListeningEvent>()
        var knownJournal = false
        var dirty = false
        var terminal: ListeningStoreIssue? = null
        var retryAt = 0L
        fun publish(issue: ListeningStoreIssue?) { mutableStatus.update { it.copy(pending = pending.size, issue = issue) } }
        fun add(event: ListeningEvent) {
            if (pending.size < pendingLimit) { pending += event; dirty = true } else missed(1)
        }
        fun drain() { while (true) add(incoming.tryReceive().getOrNull() ?: break) }
        fun checkpoint(): Boolean {
            if (!writeAllowed()) { terminal = ListeningStoreIssue.PROFILE_READ_ONLY; publish(terminal); return false }
            if (!dirty) return true
            return try {
                journal.save(pending.toList()); durable.addAll(pending); dirty = false; true
            } catch (_: IOException) { publish(ListeningStoreIssue.IO_ERROR); false }
        }
        suspend fun flush() {
            if (pending.isEmpty()) return
            if (!checkpoint()) { retryAt = clock() + retryDelayMs; return }
            val result = try { sink.append(pending.toList()) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { ListeningStoreResult.Failure(ListeningStoreIssue.IO_ERROR) }
            when (result) {
                is ListeningStoreResult.Success<*> -> {
                    durable.addAll(pending)
                    try {
                        journal.save(emptyList()); pending.clear(); durable.clear(); dirty = false; retryAt = 0; publish(null)
                    } catch (_: IOException) { retryAt = clock() + retryDelayMs; publish(ListeningStoreIssue.IO_ERROR) }
                }
                is ListeningStoreResult.Failure -> {
                    if (result.issue == ListeningStoreIssue.IO_ERROR) retryAt = clock() + retryDelayMs else terminal = result.issue
                    publish(result.issue)
                }
            }
        }
        try {
            if (!writeAllowed()) { terminal = ListeningStoreIssue.PROFILE_READ_ONLY; return }
            val restored = journal.read()
            if (restored.size > pendingLimit || restored.any { !it.valid() }) throw CorruptListeningJournal()
            pending.addAll(restored); durable.addAll(restored); knownJournal = true
            if (pending.isNotEmpty()) flush()
            while (terminal == null) {
                val next = if (pending.isNotEmpty() && retryAt > 0) {
                    withTimeoutOrNull((retryAt - clock()).coerceAtLeast(1L)) { incoming.receiveCatching() }
                } else incoming.receiveCatching()
                if (next?.isClosed == true) break
                next?.getOrNull()?.let(::add)
                drain()
                if (dirty && !checkpoint()) retryAt = clock() + retryDelayMs
                if (terminal != null) break
                publish(if (retryAt > 0) ListeningStoreIssue.IO_ERROR else null)
                if (pending.isNotEmpty() && clock() >= retryAt) flush()
            }
        } catch (_: NewerListeningJournal) { terminal = ListeningStoreIssue.NEWER_SCHEMA }
          catch (_: CorruptListeningJournal) { terminal = ListeningStoreIssue.CORRUPT }
          catch (cancelled: CancellationException) { throw cancelled }
          catch (_: Exception) { terminal = ListeningStoreIssue.IO_ERROR }
        finally {
            accepting.set(false); incoming.close(); drain()
            withContext(NonCancellable) {
                try {
                    if (knownJournal) {
                        if (terminal == null) flush() else if (dirty) checkpoint()
                    }
                } catch (_: Exception) { terminal = ListeningStoreIssue.IO_ERROR }
                missed(pending.count { it !in durable })
                publish(terminal ?: mutableStatus.value.issue)
                try { sink.close() } catch (_: Exception) { publish(ListeningStoreIssue.IO_ERROR) }
            }
        }
    }
}
