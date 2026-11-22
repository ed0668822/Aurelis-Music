package com.music.aurelis.playback.cast

import java.util.Locale

/**
 * TV07: conservative device grouping and capability-aware protocol ranking.
 *
 * The same advertised name is NOT enough if multiple Cast or DLNA devices use
 * that name: ambiguous devices stay separate, preventing playback on a
 * different TV. Browser sessions require an explicit one-time pairing and are
 * never silently started or ranked as auto-connectable.
 */
internal object SmartCastRanking {
    enum class Protocol { CAST, DLNA }

    data class AdvertisedDevice(val id: String, val name: String)

    data class Choice(
        val name: String,
        val castId: String? = null,
        val dlnaId: String? = null,
        val preferred: Protocol,
        val fallback: Protocol? = null,
        val lyricsOnTv: Boolean = false,
    ) {
        init {
            require(castId != null || dlnaId != null)
        }
    }

    private fun key(name: String): String =
        name.trim().lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()

    fun choose(
        cast: List<AdvertisedDevice>,
        dlna: List<AdvertisedDevice>,
        customReceiverEnabled: Boolean,
    ): List<Choice> {
        val castCounts = cast.groupingBy { key(it.name) }.eachCount()
        val dlnaGroups = dlna.groupBy { key(it.name) }
        val usedDlnaIds = mutableSetOf<String>()
        val choices = mutableListOf<Choice>()

        for (device in cast) {
            val commonKey = key(device.name)
            val dlnaMatch = dlnaGroups[commonKey]?.singleOrNull()
                ?.takeIf { castCounts[commonKey] == 1 && usedDlnaIds.add(it.id) }
            choices += Choice(
                name = device.name,
                castId = device.id,
                dlnaId = dlnaMatch?.id,
                preferred = Protocol.CAST,
                fallback = if (dlnaMatch != null) Protocol.DLNA else null,
                lyricsOnTv = customReceiverEnabled,
            )
        }
        for (device in dlna) {
            if (device.id in usedDlnaIds) continue
            choices += Choice(
                name = device.name,
                dlnaId = device.id,
                preferred = Protocol.DLNA,
            )
        }
        return choices.sortedWith(
            compareBy<Choice> { it.name.lowercase(Locale.ROOT) }
                .thenBy { if (it.castId != null) 0 else 1 },
        )
    }
}

/**
 * Launches only ONE protocol. An explicitly chosen SmartCast route can try
 * DLNA if a Cast session fails to *start*. There is no automatic fallback on
 * an already playing/suspended Cast session, since that could start two TVs.
 *
 * Never automatically opens a browser or screen-mirroring consent UI.
 */
internal object SmartCastCoordinator {
    private var pendingFallback: DlnaRenderer? = null

    fun connect(choice: SmartCastRanking.Choice, dlnaRenderer: DlnaRenderer?) {
        pendingFallback = null
        if (choice.preferred == SmartCastRanking.Protocol.CAST && choice.castId != null) {
            pendingFallback = if (choice.fallback == SmartCastRanking.Protocol.DLNA) dlnaRenderer else null
            CastController.connect(choice.castId)
        } else if (choice.preferred == SmartCastRanking.Protocol.DLNA && dlnaRenderer != null) {
            DlnaController.connect(dlnaRenderer)
        }
    }

    fun connectDlnaManually(renderer: DlnaRenderer) {
        pendingFallback = null
        DlnaController.connect(renderer)
    }

    fun onCastStarted() {
        pendingFallback = null
    }

    /**
     * Returns true when fallback was started. The Cast framework reports a
     * failed startup before this is called, so no receiver audio is active.
     */
    fun onCastStartFailed(): Boolean {
        val dlna = pendingFallback ?: return false
        pendingFallback = null
        DlnaController.connect(dlna)
        return true
    }

    fun cancel() {
        pendingFallback = null
    }
}
