package com.swasthai.app.ui.integrations

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.swasthai.app.ui.theme.AppleTealLight

data class HealthIntegrationDevice(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val icon: ImageVector,
    val iconColor: Color,
    val iconBg: Color,
    val initialConnected: Boolean,
    val lastSyncTime: String,
    val telemetryItems: List<Pair<String, String>>
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IntegrationsScreen() {
    var selectedCategory by remember { mutableStateOf("All") }
    var isSyncingAll by remember { mutableStateOf(false) }

    val connectionStates = remember {
        mutableStateMapOf(
            "whoop" to true,
            "apple_watch" to true,
            "dexcom" to true,
            "withings" to true,
            "omron" to true,
            "oura" to false,
            "garmin" to false
        )
    }

    val devices = remember {
        listOf(
            HealthIntegrationDevice(
                id = "whoop",
                name = "WHOOP 4.0",
                category = "Wearables",
                description = "Continuous physiological telemetry strap",
                icon = Icons.Default.DirectionsRun,
                iconColor = AppleSystemOrange,
                iconBg = AppleOrangeLight,
                initialConnected = true,
                lastSyncTime = "2 mins ago",
                telemetryItems = listOf(
                    "Recovery" to "84% (Optimal)",
                    "Day Strain" to "14.2 / 21",
                    "Sleep Performance" to "7h 42m (91%)",
                    "HRV" to "68 ms"
                )
            ),
            HealthIntegrationDevice(
                id = "apple_watch",
                name = "Apple Watch Ultra 2",
                category = "Wearables",
                description = "ECG, SpO2, and continuous heart monitoring",
                icon = Icons.Default.Watch,
                iconColor = AppleSystemBlue,
                iconBg = AppleBlueLight,
                initialConnected = true,
                lastSyncTime = "Just now",
                telemetryItems = listOf(
                    "Resting Heart Rate" to "62 bpm",
                    "ECG Rhythm" to "Normal Sinus",
                    "Blood Oxygen" to "99% SpO2",
                    "Cardio Fitness" to "46.5 VO2 Max"
                )
            ),
            HealthIntegrationDevice(
                id = "dexcom",
                name = "Dexcom G7 CGM",
                category = "Metabolic",
                description = "Continuous interstitial glucose monitoring sensor",
                icon = Icons.Default.WaterDrop,
                iconColor = AppleHealthTeal,
                iconBg = AppleTealLight,
                initialConnected = true,
                lastSyncTime = "5 mins ago",
                telemetryItems = listOf(
                    "Current Glucose" to "94 mg/dL ➔",
                    "Time in Range" to "96% (70-140)",
                    "24h Average" to "98 mg/dL",
                    "Sensor Lifetime" to "6 days remaining"
                )
            ),
            HealthIntegrationDevice(
                id = "withings",
                name = "Withings Body Scan",
                category = "Vitals",
                description = "Clinical segmental body composition & vascular scale",
                icon = Icons.Default.MonitorWeight,
                iconColor = AppleSystemGreen,
                iconBg = AppleGreenLight,
                initialConnected = true,
                lastSyncTime = "Today, 7:15 AM",
                telemetryItems = listOf(
                    "Body Weight" to "68.4 kg",
                    "Body Fat" to "16.2%",
                    "Vascular Age" to "Optimal (25-30)",
                    "Muscle Mass" to "54.8 kg"
                )
            ),
            HealthIntegrationDevice(
                id = "omron",
                name = "Omron Evolv Wireless BPM",
                category = "Vitals",
                description = "Upper arm clinical wireless blood pressure monitor",
                icon = Icons.Default.MonitorHeart,
                iconColor = AppleSystemBlue,
                iconBg = AppleBlueLight,
                initialConnected = true,
                lastSyncTime = "Today, 8:30 AM",
                telemetryItems = listOf(
                    "Blood Pressure" to "118 / 78 mmHg",
                    "Pulse" to "64 bpm",
                    "Category" to "Normal Physiological",
                    "Cuff Fit" to "Secure"
                )
            ),
            HealthIntegrationDevice(
                id = "oura",
                name = "Oura Ring Gen 3",
                category = "Wearables",
                description = "Circadian sleep stage & skin temperature smart ring",
                icon = Icons.Default.Favorite,
                iconColor = AppleHealthTeal,
                iconBg = AppleTealLight,
                initialConnected = false,
                lastSyncTime = "Available to pair",
                telemetryItems = listOf(
                    "Readiness Score" to "88 / 100",
                    "Deep Sleep" to "1h 52m",
                    "Body Temperature" to "-0.2 °C (Optimal)"
                )
            ),
            HealthIntegrationDevice(
                id = "garmin",
                name = "Garmin Connect",
                category = "Wearables",
                description = "High-end endurance & autonomic training ecosystem",
                icon = Icons.Default.DirectionsRun,
                iconColor = AppleSystemOrange,
                iconBg = AppleOrangeLight,
                initialConnected = false,
                lastSyncTime = "Available to pair",
                telemetryItems = listOf(
                    "Body Battery" to "78 / 100",
                    "Stress Level" to "22 (Low)",
                    "Training Readiness" to "Prime"
                )
            )
        )
    }

    val filteredDevices = remember(selectedCategory, devices) {
        if (selectedCategory == "All") devices else devices.filter { it.category == selectedCategory }
    }

    val activeCount = connectionStates.values.count { it }

    // Sync animation
    val infiniteTransition = rememberInfiniteTransition(label = "sync")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppleGroupedBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header Bar
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DEVICES & SENSORS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppleLabelSecondary,
                        letterSpacing = 0.6.sp
                    )
                    Text(
                        text = "Integrations",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppleLabelPrimary
                    )
                }

                // Sync All Button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(AppleCardSurface)
                        .border(0.5.dp, AppleCardBorder, RoundedCornerShape(14.dp))
                        .clickable {
                            isSyncingAll = true
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sync",
                        tint = AppleSystemBlue,
                        modifier = Modifier
                            .size(16.dp)
                            .then(if (isSyncingAll) Modifier.rotate(rotation) else Modifier)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSyncingAll) "Syncing..." else "Sync All",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppleSystemBlue
                    )
                }
            }
        }

        // 2. Telemetry Overview Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, AppleCardBorder, RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = AppleCardSurface),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppleTealLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = AppleHealthTeal,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "$activeCount Active Sensors Connected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AppleLabelPrimary
                            )
                            Text(
                                text = "Continuous telemetry feed into AI Doc",
                                fontSize = 11.sp,
                                color = AppleLabelSecondary
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppleGreenLight)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Live Sync",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppleSystemGreen
                        )
                    }
                }
            }
        }

        // 3. Category Filter Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Wearables", "Metabolic", "Vitals").forEach { category ->
                    val isSelected = selectedCategory == category
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) AppleSystemBlue else AppleCardSurface)
                            .border(0.5.dp, if (isSelected) AppleSystemBlue else AppleCardBorder, RoundedCornerShape(14.dp))
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = category,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else AppleLabelPrimary
                        )
                    }
                }
            }
        }

        // 4. Device Cards List
        items(filteredDevices) { device ->
            val isConnected = connectionStates[device.id] ?: false

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, AppleCardBorder, RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = AppleCardSurface),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header row: Icon, Name, Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(device.iconBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = device.icon,
                                    contentDescription = device.name,
                                    tint = device.iconColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = device.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AppleLabelPrimary
                                )
                                Text(
                                    text = if (isConnected) "Synced ${device.lastSyncTime}" else device.lastSyncTime,
                                    fontSize = 11.sp,
                                    color = if (isConnected) AppleSystemGreen else AppleLabelSecondary
                                )
                            }
                        }

                        // Toggle or Connect Button
                        if (isConnected) {
                            Switch(
                                checked = true,
                                onCheckedChange = {
                                    connectionStates[device.id] = false
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = AppleSystemGreen
                                )
                            )
                        } else {
                            Button(
                                onClick = {
                                    connectionStates[device.id] = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AppleSystemBlue,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Connect", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = device.description,
                        fontSize = 12.sp,
                        color = AppleLabelSecondary
                    )

                    // Telemetry Grid if connected
                    AnimatedVisibility(visible = isConnected) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(0.5.dp)
                                    .background(AppleSeparator)
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                device.telemetryItems.forEach { (label, value) ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(AppleSearchFill)
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = label,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = AppleLabelSecondary
                                            )
                                            Text(
                                                text = value,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AppleLabelPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Footer Note
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "All physiological telemetry data is strictly processed on-device.",
                    fontSize = 11.sp,
                    color = AppleLabelTertiary
                )
            }
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}
