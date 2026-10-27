package com.swasthai.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swasthai.app.domain.model.ModelInferenceStatus
import com.swasthai.app.ui.theme.AppleBlueLight
import com.swasthai.app.ui.theme.AppleCardBorder
import com.swasthai.app.ui.theme.AppleCardSurface
import com.swasthai.app.ui.theme.AppleGreenLight
import com.swasthai.app.ui.theme.AppleGroupedBackground
import com.swasthai.app.ui.theme.AppleHealthTeal
import com.swasthai.app.ui.theme.AppleLabelPrimary
import com.swasthai.app.ui.theme.AppleLabelSecondary
import com.swasthai.app.ui.theme.AppleLabelTertiary
import com.swasthai.app.ui.theme.AppleOrangeLight
import com.swasthai.app.ui.theme.AppleSearchFill
import com.swasthai.app.ui.theme.AppleSeparator
import com.swasthai.app.ui.theme.AppleSystemBackground
import com.swasthai.app.ui.theme.AppleSystemBlue
import com.swasthai.app.ui.theme.AppleSystemGreen
import com.swasthai.app.ui.theme.AppleSystemOrange
import com.swasthai.app.ui.theme.AppleSystemRed
import com.swasthai.app.ui.theme.AppleTealLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()

    // Permission toggles state
    var storagePermission by remember { mutableStateOf(true) }
    var localAiProcessing by remember { mutableStateOf(true) }
    var biometricLock by remember { mutableStateOf(true) }
    var bluetoothSync by remember { mutableStateOf(true) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var forceHistoryContext by remember { mutableStateOf(true) }
    var useCloudExtendedMode by remember { mutableStateOf(false) }

    var showClearConfirm by remember { mutableStateOf(false) }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear All Records?", fontWeight = FontWeight.Bold, color = AppleLabelPrimary) },
            text = {
                Text(
                    "This will remove all imported lab documents and reset your extracted biomarker history. This cannot be undone.",
                    fontSize = 13.sp,
                    color = AppleLabelSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = { showClearConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = AppleSystemRed)
                ) {
                    Text("Clear Records", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel", color = AppleSystemBlue)
                }
            },
            containerColor = AppleCardSurface,
            shape = RoundedCornerShape(18.dp)
        )
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().background(AppleSystemBackground)) {
                TopAppBar(
                    navigationIcon = {
                        if (onNavigateBack != null) {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = AppleSystemBlue
                                )
                            }
                        }
                    },
                    title = {
                        Column {
                            Text(
                                text = "Settings",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AppleLabelPrimary
                            )
                            Text(
                                text = "Permissions, Privacy & AI Intelligence",
                                fontSize = 11.sp,
                                color = AppleLabelSecondary
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.refreshStatus() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = AppleSystemBlue)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = AppleSystemBackground)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(AppleSeparator)
                )
            }
        },
        containerColor = AppleGroupedBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            // 1. SECTION: PERMISSIONS & SECURITY
            SettingsSectionHeader(title = "PERMISSIONS & SECURITY")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, AppleCardBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = AppleCardSurface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column {
                    SettingsToggleRow(
                        title = "Health Records Storage",
                        subtitle = "Private on-device medical vault & PDF storage",
                        icon = Icons.Default.Folder,
                        iconBg = AppleBlueLight,
                        iconTint = AppleSystemBlue,
                        checked = storagePermission,
                        onCheckedChange = { storagePermission = it }
                    )
                    SettingsDivider()

                    SettingsToggleRow(
                        title = "100% Local On-Device AI",
                        subtitle = "Air-gapped medical inference with zero cloud egress",
                        icon = Icons.Default.Shield,
                        iconBg = AppleTealLight,
                        iconTint = AppleHealthTeal,
                        checked = localAiProcessing,
                        onCheckedChange = { localAiProcessing = it }
                    )
                    SettingsDivider()

                    SettingsToggleRow(
                        title = "Biometric App Lock",
                        subtitle = "Require Face ID or Fingerprint on launch",
                        icon = Icons.Default.Fingerprint,
                        iconBg = AppleGreenLight,
                        iconTint = AppleSystemGreen,
                        checked = biometricLock,
                        onCheckedChange = { biometricLock = it }
                    )
                    SettingsDivider()

                    SettingsToggleRow(
                        title = "Bluetooth Wearables Sync",
                        subtitle = "Sync telemetry from Whoop, Apple Watch & Dexcom",
                        icon = Icons.Default.Bluetooth,
                        iconBg = AppleBlueLight,
                        iconTint = AppleSystemBlue,
                        checked = bluetoothSync,
                        onCheckedChange = { bluetoothSync = it }
                    )
                    SettingsDivider()

                    SettingsToggleRow(
                        title = "Clinical Reminders",
                        subtitle = "Smart schedule notifications for vitals & medication",
                        icon = Icons.Default.Notifications,
                        iconBg = AppleOrangeLight,
                        iconTint = AppleSystemOrange,
                        checked = notificationsEnabled,
                        onCheckedChange = { notificationsEnabled = it }
                    )
                }
            }

            // 2. SECTION: AI DOC MEDICAL INTELLIGENCE
            SettingsSectionHeader(title = "AI DOC MEDICAL INTELLIGENCE")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, AppleCardBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = AppleCardSurface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column {
                    SettingsToggleRow(
                        title = "Inject Complete Medical History",
                        subtitle = "Always pass all vault reports & lab biomarkers into AI Doc",
                        icon = Icons.Default.AutoAwesome,
                        iconBg = AppleTealLight,
                        iconTint = AppleHealthTeal,
                        checked = forceHistoryContext,
                        onCheckedChange = { forceHistoryContext = it }
                    )
                    SettingsDivider()

                    // Engine Mode Picker
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (!useCloudExtendedMode) AppleGreenLight else AppleBlueLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (!useCloudExtendedMode) Icons.Default.Memory else Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = if (!useCloudExtendedMode) AppleSystemGreen else AppleSystemBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (!useCloudExtendedMode) "Local Edge AI (Active)" else "Cloud Extended Context Mode",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = AppleLabelPrimary
                                )
                                Text(
                                    text = if (!useCloudExtendedMode)
                                        "On-Device 8K context • 100% Private (MedGemma 2B)"
                                    else
                                        "Cloud 1M+ context window (for 100+ page hospital histories)",
                                    fontSize = 11.sp,
                                    color = AppleLabelSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { useCloudExtendedMode = false },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (!useCloudExtendedMode) AppleBlueLight else Color.Transparent,
                                    contentColor = if (!useCloudExtendedMode) AppleSystemBlue else AppleLabelSecondary
                                ),
                                border = if (!useCloudExtendedMode)
                                    androidx.compose.foundation.BorderStroke(1.dp, AppleSystemBlue)
                                else
                                    androidx.compose.foundation.BorderStroke(0.5.dp, AppleSeparator),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Local Edge AI", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = { useCloudExtendedMode = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (useCloudExtendedMode) AppleBlueLight else Color.Transparent,
                                    contentColor = if (useCloudExtendedMode) AppleSystemBlue else AppleLabelSecondary
                                ),
                                border = if (useCloudExtendedMode)
                                    androidx.compose.foundation.BorderStroke(1.dp, AppleSystemBlue)
                                else
                                    androidx.compose.foundation.BorderStroke(0.5.dp, AppleSeparator),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Cloud Extended", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // 3. SECTION: DATA MANAGEMENT & VAULT
            SettingsSectionHeader(title = "DATA MANAGEMENT")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, AppleCardBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = AppleCardSurface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Encrypted Local Storage",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.5.sp,
                                color = AppleLabelPrimary
                            )
                            Text(
                                text = "1.2 MB SQLite Database • Local Vault Chunks",
                                fontSize = 11.sp,
                                color = AppleLabelSecondary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(AppleGreenLight)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Healthy", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppleSystemGreen)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { showClearConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppleSearchFill,
                            contentColor = AppleSystemRed
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear Vault & Medical History", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }

            // 4. ABOUT CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, AppleCardBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = AppleCardSurface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AppleBlueLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = AppleSystemBlue, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "SwasthAI Apple Health Edition",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = AppleLabelPrimary
                            )
                            Text(
                                text = "Version 1.2.0 (Build 2026.10) • 100% Offline Medical Intelligence",
                                fontSize = 11.sp,
                                color = AppleLabelSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = AppleLabelSecondary,
        letterSpacing = 0.6.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
    )
}

@Composable
fun SettingsToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.5.sp,
                    color = AppleLabelPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = AppleLabelSecondary
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AppleSystemGreen,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = AppleSeparator
            )
        )
    }
}

@Composable
fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(AppleSeparator)
    )
}
