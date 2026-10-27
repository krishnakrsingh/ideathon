package com.swasthai.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swasthai.app.domain.model.MedicalDocument
import com.swasthai.app.domain.model.ResponseLanguage
import com.swasthai.app.domain.model.ScopeMode
import com.swasthai.app.domain.model.TimeRangeFilter
import com.swasthai.app.ui.theme.AppleBlueLight
import com.swasthai.app.ui.theme.AppleCardBorder
import com.swasthai.app.ui.theme.AppleCardSurface
import com.swasthai.app.ui.theme.AppleHealthTeal
import com.swasthai.app.ui.theme.AppleLabelPrimary
import com.swasthai.app.ui.theme.AppleLabelSecondary
import com.swasthai.app.ui.theme.AppleLabelTertiary
import com.swasthai.app.ui.theme.AppleSearchFill
import com.swasthai.app.ui.theme.AppleSeparator
import com.swasthai.app.ui.theme.AppleSystemBackground
import com.swasthai.app.ui.theme.AppleSystemBlue
import com.swasthai.app.ui.theme.AppleTealLight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextConfigSheet(
    uiState: ChatUiState,
    viewModel: ChatViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = uiState.currentScope
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppleSystemBackground,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AppleBlueLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = AppleSystemBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Medical Context Scope",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppleLabelPrimary
                        )
                        Text(
                            text = "Control what records the AI may access",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppleLabelSecondary
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = AppleLabelTertiary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(AppleSeparator)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item { Spacer(modifier = Modifier.height(6.dp)) }

                // Section 1: Scope Mode (Conversation Only vs All Records vs Specific Docs)
                item {
                    Text(
                        text = "RETRIEVAL SCOPE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppleLabelTertiary,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(AppleCardSurface)
                            .border(0.5.dp, AppleCardBorder, RoundedCornerShape(16.dp))
                    ) {
                        ScopeOptionRow(
                            title = "Search All Medical History",
                            subtitle = "Access all indexed lab reports and documents in vault",
                            selected = scope.mode == ScopeMode.ALL_HISTORY,
                            onClick = { viewModel.setScopeMode(ScopeMode.ALL_HISTORY) }
                        )

                        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(AppleSeparator))

                        ScopeOptionRow(
                            title = "Specific Documents",
                            subtitle = "Restrict retrieval strictly to selected reports",
                            selected = scope.mode == ScopeMode.SPECIFIC_DOCS,
                            onClick = { viewModel.setScopeMode(ScopeMode.SPECIFIC_DOCS) }
                        )

                        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(AppleSeparator))

                        ScopeOptionRow(
                            title = "Conversation Only",
                            subtitle = "Do not search medical records; standard clinical chat",
                            selected = scope.mode == ScopeMode.CONVERSATION_ONLY,
                            onClick = { viewModel.setScopeMode(ScopeMode.CONVERSATION_ONLY) }
                        )
                    }
                }

                // Section 2: Specific Documents Multi-select (if specific docs mode)
                if (scope.mode == ScopeMode.SPECIFIC_DOCS) {
                    item {
                        Text(
                            text = "SELECT MEDICAL DOCUMENTS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppleLabelTertiary,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (uiState.availableDocuments.isEmpty()) {
                            Text(
                                text = "No medical reports saved in vault yet.",
                                fontSize = 13.sp,
                                color = AppleLabelSecondary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(AppleCardSurface)
                                    .border(0.5.dp, AppleCardBorder, RoundedCornerShape(16.dp))
                            ) {
                                uiState.availableDocuments.forEachIndexed { idx, doc ->
                                    val isChecked = scope.selectedDocumentIds.contains(doc.id)
                                    val dateText = if (doc.reportDate != null && doc.reportDate > 0) {
                                        "Report: ${dateFormat.format(Date(doc.reportDate))}"
                                    } else {
                                        "Imported: ${dateFormat.format(Date(doc.importDate))}"
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.toggleDocumentSelection(doc.id) }
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { viewModel.toggleDocumentSelection(doc.id) },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = AppleSystemBlue,
                                                checkmarkColor = Color.White
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = doc.fileName,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = AppleLabelPrimary,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = "$dateText • ${doc.pageCount} page(s)",
                                                fontSize = 12.sp,
                                                color = AppleLabelSecondary
                                            )
                                        }
                                    }

                                    if (idx < uiState.availableDocuments.size - 1) {
                                        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(AppleSeparator))
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 3: Time Range Filter (Active for all or specific docs)
                if (scope.mode != ScopeMode.CONVERSATION_ONLY) {
                    item {
                        Text(
                            text = "TIME RANGE FILTER",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppleLabelTertiary,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TimeFilterChip(
                                label = "All Time",
                                selected = scope.timeRange == TimeRangeFilter.ALL_TIME,
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.setTimeRange(TimeRangeFilter.ALL_TIME) }
                            )
                            TimeFilterChip(
                                label = "Last 30D",
                                selected = scope.timeRange == TimeRangeFilter.LAST_30_DAYS,
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.setTimeRange(TimeRangeFilter.LAST_30_DAYS) }
                            )
                            TimeFilterChip(
                                label = "Last 3M",
                                selected = scope.timeRange == TimeRangeFilter.LAST_3_MONTHS,
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.setTimeRange(TimeRangeFilter.LAST_3_MONTHS) }
                            )
                            TimeFilterChip(
                                label = "Last 12M",
                                selected = scope.timeRange == TimeRangeFilter.LAST_12_MONTHS,
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.setTimeRange(TimeRangeFilter.LAST_12_MONTHS) }
                            )
                        }
                    }
                }

                // Section 4: Structured Records & Biomarkers Toggle
                item {
                    Text(
                        text = "HEALTH DATA INTEGRATION",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppleLabelTertiary,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(AppleCardSurface)
                            .border(0.5.dp, AppleCardBorder, RoundedCornerShape(16.dp))
                            .clickable { viewModel.toggleStructuredVitals() }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(AppleTealLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MonitorHeart,
                                    contentDescription = null,
                                    tint = AppleHealthTeal,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Structured Biomarkers",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppleLabelPrimary
                                )
                                Text(
                                    text = "Include recorded vitals (HbA1c, BP, glucose) in context",
                                    fontSize = 12.sp,
                                    color = AppleLabelSecondary
                                )
                            }
                        }

                        Switch(
                            checked = scope.includeStructuredVitals,
                            onCheckedChange = { viewModel.toggleStructuredVitals() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AppleSystemBlue
                            )
                        )
                    }
                }

                // Section 5: Response Language (Auto, English, Hindi)
                item {
                    Text(
                        text = "RESPONSE LANGUAGE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppleLabelTertiary,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LanguageFilterChip(
                            label = "Auto Detect",
                            selected = scope.responseLanguage == ResponseLanguage.AUTO,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.setResponseLanguage(ResponseLanguage.AUTO) }
                        )
                        LanguageFilterChip(
                            label = "English",
                            selected = scope.responseLanguage == ResponseLanguage.ENGLISH,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.setResponseLanguage(ResponseLanguage.ENGLISH) }
                        )
                        LanguageFilterChip(
                            label = "हिन्दी (Hindi)",
                            selected = scope.responseLanguage == ResponseLanguage.HINDI,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.setResponseLanguage(ResponseLanguage.HINDI) }
                        )
                    }
                }

                // Apply button
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(AppleSystemBlue)
                            .clickable { onDismiss() }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Apply Context Scope",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScopeOptionRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppleLabelPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = AppleLabelSecondary
            )
        }

        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = AppleSystemBlue,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun TimeFilterChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) AppleSystemBlue else AppleSearchFill)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else AppleLabelPrimary
        )
    }
}

@Composable
private fun LanguageFilterChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) AppleSystemBlue else AppleSearchFill)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else AppleLabelPrimary
        )
    }
}
