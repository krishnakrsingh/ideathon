package com.swasthai.app.ui.vault

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swasthai.app.domain.model.IndexingStatus
import com.swasthai.app.domain.model.MedicalDocument
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
import com.swasthai.app.ui.theme.AppleRedLight
import com.swasthai.app.ui.theme.AppleSearchFill
import com.swasthai.app.ui.theme.AppleSeparator
import com.swasthai.app.ui.theme.AppleSystemBackground
import com.swasthai.app.ui.theme.AppleSystemBlue
import com.swasthai.app.ui.theme.AppleSystemGreen
import com.swasthai.app.ui.theme.AppleSystemOrange
import com.swasthai.app.ui.theme.AppleSystemRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    viewModel: VaultViewModel,
    onNavigateToChatWithDoc: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val items = uris.map { uri ->
                var fileName = "Health_Report_${System.currentTimeMillis()}.pdf"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        fileName = cursor.getString(nameIndex) ?: fileName
                    }
                }
                uri to fileName
            }
            viewModel.importMultiplePdfs(items)
        }
    }

    if (uiState.inspectingDocument != null) {
        AppleDocumentDetailDialog(
            document = uiState.inspectingDocument!!,
            chunks = uiState.selectedDocChunks,
            onDismiss = { viewModel.closeInspector() }
        )
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().background(AppleSystemBackground)) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Health Records",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AppleLabelPrimary
                            )
                            Text(
                                text = "${uiState.documents.size} saved documents • Stored privately on-device",
                                fontSize = 11.sp,
                                color = AppleLabelSecondary
                            )
                        }
                    },
                    actions = {
                        TextButton(onClick = { viewModel.importSampleDocument() }) {
                            Text("Sample Panel", color = AppleHealthTeal, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        TextButton(onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = AppleSystemBlue, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Reports", color = AppleSystemBlue, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                containerColor = AppleSystemBlue,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Health Record")
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

            // Medical History Active Status Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppleCardSurface)
                    .border(0.5.dp, AppleCardBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(AppleSystemGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${uiState.documents.size} Records Synced to AI Doc",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppleLabelPrimary
                    )
                }

                Text(
                    text = "Medical History Active",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppleHealthTeal
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Apple Style Search Field
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = {
                    Text("Search lab results, reports & doctor notes...", color = AppleLabelTertiary, fontSize = 14.sp)
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

            if (uiState.isImporting) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppleBlueLight)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = AppleSystemBlue
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Reading clinical text & extracting vital metrics...",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppleSystemBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (uiState.filteredDocuments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(AppleBlueLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = AppleSystemBlue,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (uiState.searchQuery.isBlank()) "No Health Records Yet" else "No Matching Records",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppleLabelPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Import PDF blood tests or load a verified sample lab panel to test RAG and AI consultation immediately.",
                            fontSize = 13.sp,
                            color = AppleLabelSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.importSampleDocument() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppleHealthTeal,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Load Sample Lab Panel (12+ Tests)", fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(0.5.dp, AppleSeparator),
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = AppleSystemBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Import Custom PDF", color = AppleSystemBlue, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.filteredDocuments) { doc ->
                        AppleHealthDocumentCard(
                            document = doc,
                            dateFormat = dateFormat,
                            onInspect = { viewModel.inspectDocument(doc) },
                            onDelete = { viewModel.deleteDocument(doc.id) },
                            onAskChat = { onNavigateToChatWithDoc(doc.id) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
fun AppleHealthDocumentCard(
    document: MedicalDocument,
    dateFormat: SimpleDateFormat,
    onInspect: () -> Unit,
    onDelete: () -> Unit,
    onAskChat: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, AppleCardBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = AppleCardSurface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppleBlueLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = AppleSystemBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = document.fileName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppleLabelPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${dateFormat.format(Date(document.importDate))} • ${document.pageCount} page(s) • ${(document.fileSize / 1024)} KB",
                        fontSize = 11.sp,
                        color = AppleLabelSecondary
                    )
                }

                val (badgeBg, badgeText, statusLabel) = when (document.indexingStatus) {
                    IndexingStatus.INDEXED -> Triple(AppleGreenLight, AppleSystemGreen, "Indexed for AI Doc")
                    IndexingStatus.EXTRACTED -> Triple(AppleGreenLight, AppleSystemGreen, "Indexed for AI Doc")
                    IndexingStatus.PENDING -> Triple(AppleSearchFill, AppleLabelSecondary, "Analyzing...")
                    IndexingStatus.FAILED -> Triple(AppleOrangeLight, AppleSystemOrange, "Scan Only")
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusLabel,
                        fontSize = 11.sp,
                        color = badgeText,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (document.failureReason != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AppleOrangeLight)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = AppleSystemOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "This document appears to be a scanned image without digital text.",
                        fontSize = 11.sp,
                        color = AppleSystemOrange
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(AppleBlueLight)
                        .clickable { onAskChat() }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = null,
                        tint = AppleSystemBlue,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Consult with AI Doc",
                        color = AppleSystemBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onInspect, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Visibility, contentDescription = "View Details", tint = AppleLabelSecondary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = AppleSystemRed, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AppleDocumentDetailDialog(
    document: MedicalDocument,
    chunks: List<com.swasthai.app.data.local.entity.DocumentChunkEntity>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = document.fileName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppleLabelPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Medical Record Details",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppleSystemBlue
                )
                Text(
                    text = "${chunks.size} extracted text sections • Stored safely on-device",
                    fontSize = 11.sp,
                    color = AppleLabelSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (chunks.isEmpty()) {
                    Text(
                        text = "No readable text sections found in this document.",
                        fontSize = 13.sp,
                        color = AppleLabelSecondary
                    )
                } else {
                    chunks.forEach { chunk ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppleSearchFill)
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Page ${chunk.pageNumber} • Section ${chunk.chunkIndex + 1}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppleSystemBlue
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = chunk.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AppleLabelPrimary,
                                    lineHeight = 19.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = AppleSystemBlue, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        },
        containerColor = AppleSystemBackground,
        shape = RoundedCornerShape(20.dp)
    )
}
