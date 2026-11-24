// SPDX-FileCopyrightText: © 2026 Aurelis

package com.music.aurelis

/** True if every visible entry retains the order of its source page. */
internal fun inPageOrder(shown: List<String>, page: List<String>): Boolean {
    val remaining = page.iterator()
    return shown.all { key -> remaining.asSequence().any { it == key } }
}
