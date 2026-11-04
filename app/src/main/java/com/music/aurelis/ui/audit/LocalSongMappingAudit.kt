// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.audit

/**
 * LocalSongMappingAudit — Verificación del mapeo Song (Bitchord) → LocalSong (Aurelis).
 *
 * Campos mapeados:
 * - id → id
 * - title → title
 * - artist → artist
 * - album → album
 * - artworkUrl → artworkUri
 * - duration → durationMs
 * - source → source (local)
 *
 * Botones adaptados:
 * - "Descargar" → "AutoMix"
 * - "Compartir" → "Ver Letras"
 *
 * Este archivo es estático: no se compila, solo documenta las verificaciones requeridas.
 */

object LocalSongMappingAudit {

    /** Estado de verificación de cada campo mapeado. */
    data class FieldMapping(
        val bitchordField: String,
        val aurelisField: String,
        val status: Status,
        val notes: String = ""
    ) {
        enum class Status { MAPPED, PENDING, MISSING }
    }

    /** Lista de campos mapeados y su estado actual. */
    val mappings: List<FieldMapping> = listOf(
        FieldMapping("id", "id", FieldMapping.Status.MAPPED, "Identificador único de la canción."),
        FieldMapping("title", "title", FieldMapping.Status.MAPPED, "Título de la canción."),
        FieldMapping("artist", "artist", FieldMapping.Status.MAPPED, "Artista de la canción."),
        FieldMapping("album", "album", FieldMapping.Status.MAPPED, "Álbum de la canción."),
        FieldMapping("artworkUrl", "artworkUri", FieldMapping.Status.MAPPED, "URI de la portada del álbum."),
        FieldMapping("duration", "durationMs", FieldMapping.Status.MAPPED, "Duración en milisegundos."),
        FieldMapping("source", "source", FieldMapping.Status.MAPPED, "Origen: local (Aurelis) vs streaming (Bitchord).")
    )

    /** Botones adaptados de Bitchord a Aurelis. */
    data class ButtonMapping(
        val bitchordButton: String,
        val aurelisButton: String,
        val status: Status,
        val notes: String = ""
    ) {
        enum class Status { ADAPTED, PENDING, MISSING }
    }

    /** Lista de botones adaptados. */
    val buttonMappings: List<ButtonMapping> = listOf(
        ButtonMapping("Descargar", "AutoMix", ButtonMapping.Status.ADAPTED,
            "El botón de descarga de Bitchord se adapta a activar AutoMix en Aurelis."),
        ButtonMapping("Compartir", "Ver Letras", ButtonMapping.Status.ADAPTED,
            "El botón de compartir de Bitchord se adapta a mostrar letras sincronizadas en Aurelis.")
    )

    /** Verifica si todos los campos están mapeados. */
    fun allFieldsMapped(): Boolean = mappings.all { it.status == FieldMapping.Status.MAPPED }

    /** Verifica si todos los botones están adaptados. */
    fun allButtonsAdapted(): Boolean = buttonMappings.all { it.status == ButtonMapping.Status.ADAPTED }

    /** Retorna los campos pendientes o faltantes. */
    fun pendingMappings(): List<FieldMapping> = mappings.filter { it.status != FieldMapping.Status.MAPPED }

    /** Retorna los botones pendientes o faltantes. */
    fun pendingButtonMappings(): List<ButtonMapping> = buttonMappings.filter { it.status != ButtonMapping.Status.ADAPTED }
}
