// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun GlassNavBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp)),
        color = Color(0x991E1E2C),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavigationBarItem(
                selected = selectedTab == 0,
                onClick = { onTabSelected(0) },
                icon = { Icon(painter = androidx.compose.ui.res.painterResource(id = android.R.drawable.ic_menu_compass), contentDescription = "Inicio") },
                label = { Text("Inicio") }
            )
            NavigationBarItem(
                selected = selectedTab == 1,
                onClick = { onTabSelected(1) },
                icon = { Icon(painter = androidx.compose.ui.res.painterResource(id = android.R.drawable.ic_menu_search), contentDescription = "Buscar") },
                label = { Text("Buscar") }
            )
            NavigationBarItem(
                selected = selectedTab == 2,
                onClick = { onTabSelected(2) },
                icon = { Icon(painter = androidx.compose.ui.res.painterResource(id = android.R.drawable.ic_menu_sort_by_size), contentDescription = "Biblioteca") },
                label = { Text("Biblioteca") }
            )
            NavigationBarItem(
                selected = selectedTab == 3,
                onClick = { onTabSelected(3) },
                icon = { Icon(painter = androidx.compose.ui.res.painterResource(id = android.R.drawable.ic_menu_manage), contentDescription = "Cuenta") },
                label = { Text("Cuenta") }
            )
        }
    }
}
