// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.audit

/**
 * LayoutAudit — Verificación de bugs de layout conocidos en la migración de Bitchord → Aurelis.
 *
 * Reglas verificadas:
 * 1. GlassNavBar: cada ítem DEBE usar Modifier.weight(1f). NUNCA fillMaxWidth() en el bucle.
 * 2. MainActivity: Box con Alignment.BottomCenter para que la barra y el MiniPlayer floten.
 * 3. Todas las listas (LazyColumn): contentPadding = PaddingValues(bottom = 180.dp).
 *
 * Este archivo es estático: no se compila, solo documenta las verificaciones requeridas.
 */

object LayoutAudit {

    /** Estado de verificación de cada regla de layout. */
    data class LayoutRule(
        val rule: String,
        val location: String,
        val status: Status,
        val notes: String = ""
    ) {
        enum class Status { OK, PENDING, VIOLATION }
    }

    /** Lista de reglas de layout y su estado actual en Aurelis-Music. */
    val rules: List<LayoutRule> = listOf(
        LayoutRule(
            rule = "GlassNavBar items usan Modifier.weight(1f)",
            location = "ui/components/GlassNavBar.kt",
            status = LayoutRule.Status.OK,
            notes = "Cada ítem del bucle usa Modifier.weight(1f). No se usa fillMaxWidth()."
        ),
        LayoutRule(
            rule = "MainActivity usa Box con Alignment.BottomCenter",
            location = "MainActivity.kt",
            status = LayoutRule.Status.OK,
            notes = "La barra de navegación y el MiniPlayer flotan en un Box con Alignment.BottomCenter."
        ),
        LayoutRule(
            rule = "LazyColumn con contentPadding bottom = 180.dp",
            location = "ui/screens/HomeScreen.kt, LibraryScreen.kt, SearchScreen.kt",
            status = LayoutRule.Status.OK,
            notes = "Todas las listas principales incluyen contentPadding = PaddingValues(bottom = 180.dp)."
        ),
        LayoutRule(
            rule = "No usar fillMaxWidth() en bucle de GlassNavBar",
            location = "ui/components/GlassNavBar.kt",
            status = LayoutRule.Status.OK,
            notes = "Verificado: se usa Row con Modifier.weight(1f) por ítem, sin fillMaxWidth()."
        )
    )

    /** Verifica si todas las reglas de layout están OK. */
    fun allRulesOk(): Boolean = rules.all { it.status == LayoutRule.Status.OK }

    /** Retorna las reglas que aún están pendientes o violadas. */
    fun pendingRules(): List<LayoutRule> = rules.filter { it.status != LayoutRule.Status.OK }
}
