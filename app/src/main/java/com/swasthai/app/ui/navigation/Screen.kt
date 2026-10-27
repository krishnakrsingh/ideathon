package com.swasthai.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Watch
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Vault : Screen("vault", "Records", Icons.Default.Folder)
    object Chat : Screen("chat?conversationId={conversationId}", "AI Doc", Icons.Default.AutoAwesome) {
        fun createRoute(conversationId: Long? = null) = if (conversationId != null) "chat?conversationId=$conversationId" else "chat"
    }
    object Integrations : Screen("integrations", "Integrations", Icons.Default.Watch)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)

    // Legacy alias to preserve backwards compatibility
    val History: Screen get() = Integrations
}
