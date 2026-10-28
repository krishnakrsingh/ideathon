package com.swasthai.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.swasthai.app.ui.navigation.Screen
import com.swasthai.app.ui.theme.AppleBlueLight
import com.swasthai.app.ui.theme.AppleCardBorder
import com.swasthai.app.ui.theme.AppleCardSurface
import com.swasthai.app.ui.theme.AppleLabelTertiary
import com.swasthai.app.ui.theme.AppleSystemBlue

@Composable
fun AppBottomNav(
    navController: NavController
) {
    val context = LocalContext.current
    var confirmEmergencyCall by remember { mutableStateOf(false) }
    val items = listOf(
        Screen.Home,
        Screen.Vault,
        Screen.Chat,
        Screen.History
    )

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    if (currentRoute?.startsWith("chat") == true) return

    if (confirmEmergencyCall) {
        AlertDialog(
            onDismissRequest = { confirmEmergencyCall = false },
            title = { Text("Call emergency services?") },
            text = { Text("This opens your phone dialer with 112. Use it when someone needs immediate help.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmEmergencyCall = false
                    openEmergencyDialer(context)
                }) {
                    Text("Open dialer")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmEmergencyCall = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Persistent emergency action sits above the main navigation.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Emergency 112",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFD92D20))
                    .clickable { confirmEmergencyCall = true }
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Floating pill capsule
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 14.dp,
                        shape = RoundedCornerShape(32.dp),
                        spotColor = Color(0x1E000000),
                        ambientColor = Color(0x0F000000)
                    )
                    .clip(RoundedCornerShape(32.dp))
                    .background(AppleCardSurface)
                    .border(1.dp, AppleCardBorder, RoundedCornerShape(32.dp))
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { screen ->
                    val isSelected = currentRoute == screen.route ||
                            (screen == Screen.Chat && currentRoute?.startsWith("chat") == true)

                    val iconTint by animateColorAsState(
                        targetValue = if (isSelected) AppleSystemBlue else AppleLabelTertiary,
                        animationSpec = tween(durationMillis = 200),
                        label = "iconTint"
                    )

                    val pillBgColor by animateColorAsState(
                        targetValue = if (isSelected) AppleBlueLight else Color.Transparent,
                        animationSpec = tween(durationMillis = 200),
                        label = "pillBgColor"
                    )

                    // Individual tab item
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(pillBgColor)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (!isSelected) {
                                    val targetRoute = if (screen is Screen.Chat) screen.createRoute() else screen.route
                                    navController.navigate(targetRoute) {
                                        popUpTo(Screen.Home.route) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title,
                                tint = iconTint,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = screen.title,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = iconTint,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
            }
        }
    }
}
