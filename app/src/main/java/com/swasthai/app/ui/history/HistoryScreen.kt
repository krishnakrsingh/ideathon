package com.swasthai.app.ui.history

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swasthai.app.domain.model.HealthTimelineItem
import com.swasthai.app.domain.model.StructuredMeasurement
import com.swasthai.app.ui.theme.AppleBlueLight
import com.swasthai.app.ui.theme.AppleCardBorder
import com.swasthai.app.ui.theme.AppleCardSurface
import com.swasthai.app.ui.theme.AppleGreenLight
import com.swasthai.app.ui.theme.AppleGroupedBackground
import com.swasthai.app.ui.theme.AppleLabelPrimary
import com.swasthai.app.ui.theme.AppleLabelSecondary
import com.swasthai.app.ui.theme.AppleLabelTertiary
import com.swasthai.app.ui.theme.AppleOrangeLight
import com.swasthai.app.ui.theme.ApplePurpleLight
import com.swasthai.app.ui.theme.AppleRedLight
import com.swasthai.app.ui.theme.AppleSearchFill
import com.swasthai.app.ui.theme.AppleSeparator
import com.swasthai.app.ui.theme.AppleSystemBackground
import com.swasthai.app.ui.theme.AppleSystemBlue
import com.swasthai.app.ui.theme.AppleSystemGreen
import com.swasthai.app.ui.theme.AppleSystemOrange
import com.swasthai.app.ui.theme.AppleSystemPurple
import com.swasthai.app.ui.theme.AppleSystemRed
import com.swasthai.app.ui.theme.AppleYellowLight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "Blood Count", "Metabolic", "Vitamins", "Reports")

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().background(AppleSystemBackground)) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Health Trends",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AppleLabelPrimary
                            )
                            Text(
                                text = "Timeline of verified lab biomarkers & vitals",
                                fontSize = 11.sp,
                                color = AppleLabelSecondary
                            )
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
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Apple Style Search Field
            OutlinedTextField(
                value = uiState.filterQuery,
                onValueChange = { viewModel.setFilterQuery(it) },
                placeholder = {
                    Text("Filter vitals by biomarker or report...", color = AppleLabelTertiary, fontSize = 14.sp)
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = AppleLabelTertiary, modifier = Modifier.size(18.dp))
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = AppleSearchFill,
                    unfocusedContainerColor = AppleSearchFill,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = AppleLabelPrimary,
                    unfocusedTextColor = AppleLabelPrimary
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Category Filter Pills
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) AppleSystemBlue else AppleCardSurface)
                            .border(
                                width = if (isSelected) 0.dp else 0.5.dp,
                                color = if (isSelected) Color.Transparent else AppleCardBorder,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = cat,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else AppleLabelPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Filter items based on category
            val displayedItems = uiState.filteredItems.filter { item ->
                when (selectedCategory) {
                    "All" -> true
                    "Reports" -> item is HealthTimelineItem.ReportImported
                    "Blood Count" -> item is HealthTimelineItem.MeasurementRecorded &&
                            (item.measurement.biomarkerName.contains("Hemoglobin", ignoreCase = true) ||
                                    item.measurement.biomarkerName.contains("Platelet", ignoreCase = true) ||
                                    item.measurement.biomarkerName.contains("WBC", ignoreCase = true) ||
                                    item.measurement.biomarkerName.contains("RBC", ignoreCase = true))
                    "Metabolic" -> item is HealthTimelineItem.MeasurementRecorded &&
                            (item.measurement.biomarkerName.contains("Glucose", ignoreCase = true) ||
                                    item.measurement.biomarkerName.contains("Cholesterol", ignoreCase = true) ||
                                    item.measurement.biomarkerName.contains("ALT", ignoreCase = true) ||
                                    item.measurement.biomarkerName.contains("Creatinine", ignoreCase = true))
                    "Vitamins" -> item is HealthTimelineItem.MeasurementRecorded &&
                            (item.measurement.biomarkerName.contains("Vitamin", ignoreCase = true) ||
                                    item.measurement.biomarkerName.contains("Calcium", ignoreCase = true) ||
                                    item.measurement.biomarkerName.contains("Iron", ignoreCase = true))
                    else -> true
                }
            }

            if (displayedItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(AppleBlueLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ShowChart,
                                contentDescription = null,
                                tint = AppleSystemBlue,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No Trends Recorded Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppleLabelPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Import lab reports into Health Records to visualize your vitals",
                            fontSize = 13.sp,
                            color = AppleLabelSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(displayedItems) { item ->
                        when (item) {
                            is HealthTimelineItem.ReportImported -> {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(0.5.dp, AppleCardBorder, RoundedCornerShape(16.dp)),
                                    colors = CardDefaults.cardColors(containerColor = AppleCardSurface),
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(12.dp))
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
                                                text = item.documentTitle,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = AppleLabelPrimary
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Imported Report • ${dateFormat.format(Date(item.timestamp))} • ${item.pageCount} page(s)",
                                                fontSize = 11.sp,
                                                color = AppleLabelSecondary
                                            )
                                        }
                                    }
                                }
                            }

                            is HealthTimelineItem.MeasurementRecorded -> {
                                AppleMeasurementTrendCard(
                                    measurement = item.measurement,
                                    documentTitle = item.documentTitle,
                                    dateFormat = dateFormat
                                )
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
fun AppleMeasurementTrendCard(
    measurement: StructuredMeasurement,
    documentTitle: String,
    dateFormat: SimpleDateFormat
) {
    val isOut = measurement.isOutOfRange
    val name = measurement.biomarkerName

    val (iconVector, iconTint, iconBg) = when {
        name.contains("Heart", ignoreCase = true) || name.contains("Pulse", ignoreCase = true) ->
            Triple(Icons.Default.Favorite, AppleSystemRed, AppleRedLight)
        name.contains("Pressure", ignoreCase = true) || name.contains("BP", ignoreCase = true) ->
            Triple(Icons.Default.MonitorHeart, AppleSystemBlue, AppleBlueLight)
        name.contains("Glucose", ignoreCase = true) || name.contains("Sugar", ignoreCase = true) ->
            Triple(Icons.Default.WaterDrop, AppleSystemOrange, AppleOrangeLight)
        name.contains("Vitamin", ignoreCase = true) ->
            Triple(Icons.Default.WbSunny, AppleSystemOrange, AppleYellowLight)
        else ->
            Triple(Icons.Default.Science, AppleSystemPurple, ApplePurpleLight)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, AppleCardBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = AppleCardSurface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = measurement.biomarkerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppleLabelPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${dateFormat.format(Date(measurement.testDate))} • $documentTitle",
                        fontSize = 11.sp,
                        color = AppleLabelSecondary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${measurement.numericValue}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOut) AppleSystemOrange else AppleLabelPrimary
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = measurement.unit,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppleLabelSecondary,
                            modifier = Modifier.padding(bottom = 1.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isOut) AppleOrangeLight else AppleGreenLight)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isOut) "Review" else "Normal",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOut) AppleSystemOrange else AppleSystemGreen
                        )
                    }
                }
            }

            // Reference Range Indicator Bar
            if (measurement.referenceLow != null && measurement.referenceHigh != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Ref: ${measurement.referenceLow} - ${measurement.referenceHigh} ${measurement.unit}",
                        fontSize = 11.sp,
                        color = AppleLabelTertiary
                    )
                    Text(
                        text = if (isOut) "Outside Target" else "Within Standard Target",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isOut) AppleSystemOrange else AppleSystemGreen
                    )
                }
            }
        }
    }
}
