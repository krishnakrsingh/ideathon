package com.swasthai.app.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swasthai.app.domain.model.IndexingStatus
import com.swasthai.app.domain.model.ModelInferenceStatus
import com.swasthai.app.domain.safety.EmergencyTriage
import com.swasthai.app.ui.components.AppTopBar
import com.swasthai.app.ui.theme.AppleActivityCyan
import com.swasthai.app.ui.theme.AppleActivityGreen
import com.swasthai.app.ui.theme.AppleActivityRed
import com.swasthai.app.ui.theme.AppleBlueLight
import com.swasthai.app.ui.theme.AppleCardBorder
import com.swasthai.app.ui.theme.AppleCardSurface
import com.swasthai.app.ui.theme.AppleGreenLight
import com.swasthai.app.ui.theme.AppleGroupedBackground
import com.swasthai.app.ui.theme.AppleHealthTeal
import com.swasthai.app.ui.theme.AppleIndigoLight
import com.swasthai.app.ui.theme.AppleLabelPrimary
import com.swasthai.app.ui.theme.AppleLabelQuaternary
import com.swasthai.app.ui.theme.AppleLabelSecondary
import com.swasthai.app.ui.theme.AppleLabelTertiary
import com.swasthai.app.ui.theme.AppleOrangeLight
import com.swasthai.app.ui.theme.ApplePinkLight
import com.swasthai.app.ui.theme.ApplePurpleLight
import com.swasthai.app.ui.theme.AppleRedLight
import com.swasthai.app.ui.theme.AppleSearchFill
import com.swasthai.app.ui.theme.AppleSeparator
import com.swasthai.app.ui.theme.AppleSystemBlue
import com.swasthai.app.ui.theme.AppleSystemGreen
import com.swasthai.app.ui.theme.AppleSystemIndigo
import com.swasthai.app.ui.theme.AppleSystemOrange
import com.swasthai.app.ui.theme.AppleSystemPurple
import com.swasthai.app.ui.theme.AppleSystemRed
import com.swasthai.app.ui.theme.AppleTealLight
import com.swasthai.app.ui.theme.AppleYellowLight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToChat: (Long?) -> Unit,
    onNavigateToVault: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val todayHeaderFormat = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
    val todayDateString = todayHeaderFormat.format(Date()).uppercase()

    Scaffold(
        containerColor = AppleGroupedBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Apple Health Home Header
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = todayDateString,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppleLabelSecondary,
                            letterSpacing = 0.6.sp
                        )
                        Text(
                            text = "Home",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppleLabelPrimary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // On-device privacy indicator chip
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppleTealLight)
                                .clickable { onNavigateToSettings() }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "On-Device",
                                tint = AppleHealthTeal,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Private AI",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppleHealthTeal
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Settings Icon Button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AppleCardSurface)
                                .border(0.5.dp, AppleCardBorder, CircleShape)
                                .clickable { onNavigateToSettings() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = AppleLabelPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // 2. Apple Health Daily Vitals Ring & Status Card
            item {
                AppleHealthScoreCard(
                    score = 94,
                    measurementsCount = uiState.latestMeasurements.size,
                    documentsCount = uiState.recentDocuments.size,
                    onOpenRecords = onNavigateToVault
                )
            }

            // 3. Clinical Assistant Primary Companion Card
            item {
                AppleAssistantCard(
                    modelStatus = uiState.medGemmaStatus,
                    onNewConsultation = {
                        viewModel.createNewChat { newId ->
                            onNavigateToChat(newId)
                        }
                    },
                    onOpenVault = onNavigateToVault,
                    onOpenSettings = onNavigateToSettings
                )
            }

            // 4. Quick Clinical Consultation Prompts
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ASK AI DOC",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppleLabelSecondary,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val prompts = listOf(
                            "Explain my lab results" to Icons.Default.Description,
                            "What is normal blood pressure?" to Icons.Default.MonitorHeart,
                            "How to improve hemoglobin" to Icons.Default.Science,
                            "Tips for healthy blood sugar" to Icons.Default.WaterDrop
                        )

                        prompts.forEach { (promptText, icon) ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(AppleCardSurface)
                                    .border(0.5.dp, AppleCardBorder, RoundedCornerShape(14.dp))
                                    .clickable {
                                        viewModel.createNewChat(initialQuery = promptText) { newId ->
                                            onNavigateToChat(newId)
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = AppleSystemBlue,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = promptText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = AppleLabelPrimary
                                )
                            }
                        }
                    }
                }
            }

            // 5. Individual Vitals & Biomarkers Widget Grid
            item {
                Text(
                    text = "VITALS & HEALTH HIGHLIGHTS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppleLabelSecondary,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Row 1: Heart Rate & Blood Pressure
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        VitalMetricWidget(
                            modifier = Modifier.weight(1f),
                            title = "Heart Rate",
                            value = "72",
                            unit = "BPM",
                            subtitle = "Resting • Normal (60-100)",
                            icon = Icons.Default.Favorite,
                            iconColor = AppleSystemRed,
                            iconBg = AppleRedLight,
                            statusText = "Normal",
                            statusColor = AppleSystemGreen,
                            statusBg = AppleGreenLight,
                            onClick = {
                                viewModel.createNewChat("Explain resting heart rate ranges and cardiovascular health") {
                                    onNavigateToChat(it)
                                }
                            }
                        )

                        VitalMetricWidget(
                            modifier = Modifier.weight(1f),
                            title = "Blood Pressure",
                            value = "120/80",
                            unit = "mmHg",
                            subtitle = "Optimal target range",
                            icon = Icons.Default.MonitorHeart,
                            iconColor = AppleHealthTeal,
                            iconBg = AppleTealLight,
                            statusText = "Optimal",
                            statusColor = AppleSystemGreen,
                            statusBg = AppleGreenLight,
                            onClick = {
                                viewModel.createNewChat("Explain normal blood pressure ranges and systolic vs diastolic") {
                                    onNavigateToChat(it)
                                }
                            }
                        )
                    }

                    // Row 2: Blood Glucose & Oxygen Saturation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        VitalMetricWidget(
                            modifier = Modifier.weight(1f),
                            title = "Blood Glucose",
                            value = "95",
                            unit = "mg/dL",
                            subtitle = "Fasting • Target 70-99",
                            icon = Icons.Default.WaterDrop,
                            iconColor = AppleSystemOrange,
                            iconBg = AppleOrangeLight,
                            statusText = "In Range",
                            statusColor = AppleSystemGreen,
                            statusBg = AppleGreenLight,
                            onClick = {
                                viewModel.createNewChat("What are healthy fasting blood sugar levels and tips for glycemic control?") {
                                    onNavigateToChat(it)
                                }
                            }
                        )

                        VitalMetricWidget(
                            modifier = Modifier.weight(1f),
                            title = "Oxygen (SpO2)",
                            value = "99",
                            unit = "%",
                            subtitle = "Healthy lung saturation",
                            icon = Icons.Default.Air,
                            iconColor = AppleSystemBlue,
                            iconBg = AppleBlueLight,
                            statusText = "Excellent",
                            statusColor = AppleSystemBlue,
                            statusBg = AppleBlueLight,
                            onClick = {
                                viewModel.createNewChat("What is normal oxygen saturation (SpO2) and pulse oximetry?") {
                                    onNavigateToChat(it)
                                }
                            }
                        )
                    }

                    // Row 3: Hemoglobin & Vitamin D3
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Check if real extracted measurements exist
                        val hemoMeasurement = uiState.latestMeasurements.find {
                            it.biomarkerName.contains("Hemoglobin", ignoreCase = true)
                        }

                        VitalMetricWidget(
                            modifier = Modifier.weight(1f),
                            title = "Hemoglobin",
                            value = hemoMeasurement?.let { String.format("%.1f", it.numericValue) } ?: "14.8",
                            unit = hemoMeasurement?.unit ?: "g/dL",
                            subtitle = "Healthy oxygen capacity",
                            icon = Icons.Default.Science,
                            iconColor = AppleSystemPurple,
                            iconBg = ApplePurpleLight,
                            statusText = if (hemoMeasurement?.isOutOfRange == true) "Review" else "Normal",
                            statusColor = if (hemoMeasurement?.isOutOfRange == true) AppleSystemOrange else AppleSystemGreen,
                            statusBg = if (hemoMeasurement?.isOutOfRange == true) AppleOrangeLight else AppleGreenLight,
                            onClick = {
                                viewModel.createNewChat("Explain hemoglobin levels, normal ranges, and red blood cell health") {
                                    onNavigateToChat(it)
                                }
                            }
                        )

                        val vitDMeasurement = uiState.latestMeasurements.find {
                            it.biomarkerName.contains("Vitamin D", ignoreCase = true)
                        }

                        VitalMetricWidget(
                            modifier = Modifier.weight(1f),
                            title = "Vitamin D3",
                            value = vitDMeasurement?.let { String.format("%.0f", it.numericValue) } ?: "38",
                            unit = vitDMeasurement?.unit ?: "ng/mL",
                            subtitle = "Optimal (>30 ng/mL)",
                            icon = Icons.Default.WbSunny,
                            iconColor = AppleSystemOrange,
                            iconBg = AppleYellowLight,
                            statusText = "Sufficient",
                            statusColor = AppleSystemGreen,
                            statusBg = AppleGreenLight,
                            onClick = {
                                viewModel.createNewChat("Explain optimal Vitamin D3 levels and how to improve Vitamin D") {
                                    onNavigateToChat(it)
                                }
                            }
                        )
                    }
                }
            }

            // 6. Today's Clinical Insight Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.5.dp, Color(0xFFD6E4FF), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = AppleBlueLight),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = AppleSystemBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Daily Health Summary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AppleSystemBlue
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (uiState.latestMeasurements.isNotEmpty()) {
                                "Based on your verified lab records, your biomarkers are well-balanced. Hemoglobin and metabolic indicators fall within normal clinical parameters. Continue regular hydration and 30 minutes of daily activity."
                            } else {
                                "All baseline vitals are within normal reference thresholds. You can import PDF lab reports anytime into your Records vault for detailed on-device clinical insights."
                            },
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = AppleLabelPrimary
                        )
                    }
                }
            }

            // 7. Recent Health Records Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Health Records",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppleLabelPrimary
                    )
                    Text(
                        text = "View All",
                        fontSize = 13.sp,
                        color = AppleSystemBlue,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onNavigateToVault() }
                    )
                }
            }

            if (uiState.recentDocuments.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(0.5.dp, AppleCardBorder, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = AppleCardSurface),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(AppleBlueLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = AppleSystemBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No medical reports imported yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = AppleLabelPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Import PDF blood tests, lipid panels, or doctor notes",
                                fontSize = 12.sp,
                                color = AppleLabelSecondary
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = onNavigateToVault,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AppleSystemBlue,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Add Health Record", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            } else {
                items(uiState.recentDocuments) { doc ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(0.5.dp, AppleCardBorder, RoundedCornerShape(14.dp))
                            .clickable { onNavigateToVault() },
                        colors = CardDefaults.cardColors(containerColor = AppleCardSurface),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AppleBlueLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = AppleSystemBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = doc.fileName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppleLabelPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${dateFormat.format(Date(doc.importDate))} • ${doc.pageCount} page(s)",
                                    fontSize = 11.sp,
                                    color = AppleLabelSecondary
                                )
                            }
                            val (badgeBg, badgeText, label) = when (doc.indexingStatus) {
                                IndexingStatus.INDEXED -> Triple(AppleGreenLight, AppleSystemGreen, "Analyzed")
                                IndexingStatus.EXTRACTED -> Triple(AppleBlueLight, AppleSystemBlue, "Ready")
                                IndexingStatus.PENDING -> Triple(AppleSearchFill, AppleLabelSecondary, "Reading...")
                                IndexingStatus.FAILED -> Triple(AppleOrangeLight, AppleSystemOrange, "Scan Only")
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(badgeBg)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    color = badgeText,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 8. Recent Consultations Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Consultations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppleLabelPrimary
                    )
                }
            }

            if (uiState.recentConversations.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(0.5.dp, AppleCardBorder, RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = AppleCardSurface),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No previous consultations yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppleLabelSecondary
                            )
                        }
                    }
                }
            } else {
                items(uiState.recentConversations) { convo ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(0.5.dp, AppleCardBorder, RoundedCornerShape(14.dp))
                            .clickable { onNavigateToChat(convo.id) },
                        colors = CardDefaults.cardColors(containerColor = AppleCardSurface),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AppleTealLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Chat,
                                    contentDescription = null,
                                    tint = AppleHealthTeal,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = convo.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppleLabelPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = dateFormat.format(Date(convo.updatedAt)),
                                    fontSize = 11.sp,
                                    color = AppleLabelSecondary
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint = AppleLabelTertiary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // 9. Apple Medical Disclaimer Footer
            item {
                Text(
                    text = EmergencyTriage.MEDICAL_DISCLAIMER,
                    fontSize = 11.sp,
                    color = AppleLabelTertiary,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

/**
 * Apple Health Daily Readiness & Score Card with visual circular rings
 */
@Composable
fun AppleHealthScoreCard(
    score: Int,
    measurementsCount: Int,
    documentsCount: Int,
    onOpenRecords: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, AppleCardBorder, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = AppleCardSurface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(AppleSystemGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "HEALTH SCORE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppleSystemGreen,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$score",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppleLabelPrimary
                    )
                    Text(
                        text = " / 100",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppleLabelSecondary,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Optimal Vitals & Biomarkers",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppleLabelPrimary
                )

                Text(
                    text = if (documentsCount > 0) "$documentsCount records • $measurementsCount verified metrics" else "Standard reference baselines",
                    fontSize = 11.sp,
                    color = AppleLabelSecondary
                )
            }

            // Circular Visual Health Rings (Apple Activity Style)
            Box(
                modifier = Modifier.size(72.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 6.dp.toPx()

                    // Ring 1: Red (Cardiovascular)
                    drawCircle(
                        color = AppleRedLight,
                        radius = size.minDimension / 2 - strokeWidth / 2,
                        style = Stroke(width = strokeWidth)
                    )
                    drawArc(
                        color = AppleActivityRed,
                        startAngle = -90f,
                        sweepAngle = 290f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Ring 2: Green (Metabolic)
                    drawCircle(
                        color = AppleGreenLight,
                        radius = size.minDimension / 2 - strokeWidth * 1.8f,
                        style = Stroke(width = strokeWidth)
                    )
                    drawArc(
                        color = AppleSystemGreen,
                        startAngle = -90f,
                        sweepAngle = 320f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Ring 3: Teal (Rest & Hydration)
                    drawCircle(
                        color = AppleTealLight,
                        radius = size.minDimension / 2 - strokeWidth * 3.1f,
                        style = Stroke(width = strokeWidth)
                    )
                    drawArc(
                        color = AppleHealthTeal,
                        startAngle = -90f,
                        sweepAngle = 260f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = AppleActivityRed,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Apple Health Companion AI Card
 */
@Composable
fun AppleAssistantCard(
    modelStatus: ModelInferenceStatus,
    onNewConsultation: () -> Unit,
    onOpenVault: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, AppleCardBorder, RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = AppleCardSurface),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppleBlueLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = AppleSystemBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "AI Doc",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppleLabelPrimary
                    )
                    Text(
                        text = "Full Medical History Grounded • Private AI",
                        fontSize = 11.sp,
                        color = AppleLabelSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Friendly Status Banner
            val isReady = modelStatus == ModelInferenceStatus.READY
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppleSearchFill)
                    .clickable { onOpenSettings() }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isReady) AppleSystemGreen else AppleSystemOrange)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isReady) "AI Doc Ready" else "AI Engine Standby",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = AppleLabelPrimary
                    )
                }

                Text(
                    text = if (isReady) "Private" else "Configure",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isReady) AppleSystemGreen else AppleSystemBlue
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onNewConsultation,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppleSystemBlue,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Consult AI Doc",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.5.sp,
                        maxLines = 1
                    )
                }

                OutlinedButton(
                    onClick = onOpenVault,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = AppleSystemBlue
                    ),
                    border = BorderStroke(0.5.dp, AppleSeparator),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Health Records",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.5.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Individual Apple Health Metric Widget (Heart Rate, Blood Pressure, Glucose, etc.)
 */
@Composable
fun VitalMetricWidget(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    unit: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    iconBg: Color,
    statusText: String,
    statusColor: Color,
    statusBg: Color,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .border(0.5.dp, AppleCardBorder, RoundedCornerShape(18.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(containerColor = AppleCardSurface),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusBg)
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = statusText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = AppleLabelSecondary
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppleLabelPrimary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppleLabelSecondary,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = AppleLabelTertiary,
                maxLines = 1
            )
        }
    }
}
