// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * NavigationController gestiona las rutas de navegación de Aurelis Music.
 *
 * Rutas disponibles:
 * - 0: Home
 * - 1: Explore
 * - 2: Library
 * - 3: Search
 * - 4: NowPlaying
 * - 5: Queue
 * - 6: Settings
 * - 7: Replay
 */
sealed class NavigationRoute(val index: Int) {
    object Home : NavigationRoute(0)
    object Explore : NavigationRoute(1)
    object Library : NavigationRoute(2)
    object Search : NavigationRoute(3)
    object NowPlaying : NavigationRoute(4)
    object Queue : NavigationRoute(5)
    object Settings : NavigationRoute(6)
    object Replay : NavigationRoute(7)
}

/**
 * NavigationController es un componente que gestiona el estado de navegación.
 *
 * @param initialRoute La ruta inicial.
 * @param onRouteChanged Callback cuando cambia la ruta.
 */
@Composable
fun rememberNavigationController(
    initialRoute: NavigationRoute = NavigationRoute.Home,
    onRouteChanged: (NavigationRoute) -> Unit = {},
): NavigationController {
    var selectedIndex by remember { mutableIntStateOf(initialRoute.index) }
    val controller = remember { NavigationController(selectedIndex, onRouteChanged) }
    return controller
}

/**
 * NavigationController es una clase que gestiona el estado de navegación.
 *
 * @param selectedIndex El índice de la ruta seleccionada.
 * @param onRouteChanged Callback cuando cambia la ruta.
 */
class NavigationController(
    selectedIndex: Int,
    private val onRouteChanged: (NavigationRoute) -> Unit,
) {
    private var _selectedIndex by mutableIntStateOf(selectedIndex)

    val selectedIndex: Int
        get() = _selectedIndex

    /**
     * Cambia la ruta seleccionada.
     *
     * @param route La nueva ruta.
     */
    fun navigateTo(route: NavigationRoute) {
        if (_selectedIndex != route.index) {
            _selectedIndex = route.index
            onRouteChanged(route)
        }
    }

    /**
     * Cambia la ruta seleccionada por su índice.
     *
     * @param index El índice de la ruta.
     */
    fun navigateTo(index: Int) {
        if (_selectedIndex != index) {
            _selectedIndex = index
            onRouteChanged(NavigationRoute.values().firstOrNull { it.index == index }
                ?: NavigationRoute.Home)
        }
    }

    /**
     * Regresa a la ruta anterior.
     */
    fun navigateBack() {
        if (_selectedIndex > 0) {
            _selectedIndex--
            onRouteChanged(NavigationRoute.values().firstOrNull { it.index == _selectedIndex }
                ?: NavigationRoute.Home)
        }
    }

    /**
     * Verifica si la ruta está seleccionada.
     *
     * @param route La ruta a verificar.
     * @return true si la ruta está seleccionada, false en caso contrario.
     */
    fun isSelected(route: NavigationRoute): Boolean {
        return _selectedIndex == route.index
    }

    /**
     * Verifica si la ruta está seleccionada por su índice.
     *
     * @param index El índice de la ruta.
     * @return true si la ruta está seleccionada, false en caso contrario.
     */
    fun isSelected(index: Int): Boolean {
        return _selectedIndex == index
    }
}