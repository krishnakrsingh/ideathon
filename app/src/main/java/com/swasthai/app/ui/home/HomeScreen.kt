package com.swasthai.app.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swasthai.app.domain.model.Conversation
import com.swasthai.app.domain.model.IndexingStatus
import com.swasthai.app.domain.model.MedicalDocument
import com.swasthai.app.domain.model.ModelInferenceStatus
import com.swasthai.app.domain.model.StructuredMeasurement
import com.swasthai.app.data.local.entity.PersonProfileEntity
import com.swasthai.app.data.local.entity.MedicationReminderEntity
import com.swasthai.app.data.repository.MenstrualFlow
import com.swasthai.app.data.repository.PersonProfileRepository
import com.swasthai.app.ui.components.DiceBearAvatar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneOffset
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToChat: (Long?) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToVault: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
    var profileMenuExpanded by remember { mutableStateOf(false) }
    var addProfileDialog by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf("") }
    var newProfileRelationship by remember { mutableStateOf("") }
    var newProfileDateOfBirth by remember { mutableStateOf("") }
    var newProfileSexAtBirth by remember { mutableStateOf<String?>(null) }
    var newProfileAvatarSeed by remember { mutableStateOf("avatar-01") }
    var sexMenuExpanded by remember { mutableStateOf(false) }
    var addReminderDialog by remember { mutableStateOf(false) }
    var reminderName by remember { mutableStateOf("") }
    var reminderInstructions by remember { mutableStateOf("") }
    var reminderTime by remember { mutableStateOf("09:00") }
    var cycleLogDialog by remember { mutableStateOf(false) }
    var cycleDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var cycleFlow by remember { mutableStateOf(MenstrualFlow.LIGHT) }
    var cycleFlowMenuExpanded by remember { mutableStateOf(false) }
    var isPeriodStart by remember { mutableStateOf(false) }
    var cycleSymptoms by remember { mutableStateOf(emptySet<String>()) }
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    val activeProfile = uiState.profiles.firstOrNull { it.id == uiState.activeProfileId }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(homeGreeting(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(activeProfile?.displayName?.let { "Hi, $it" } ?: "Your health", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    }
                    Box {
                        androidx.compose.material3.Surface(
                            onClick = { profileMenuExpanded = true },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) { DiceBearAvatar(activeProfile?.avatarSeed ?: "avatar-01", 44.dp, Modifier.padding(2.dp)) }
                        DropdownMenu(expanded = profileMenuExpanded, onDismissRequest = { profileMenuExpanded = false }) {
                            uiState.profiles.forEach { profile ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            DiceBearAvatar(profile.avatarSeed, 32.dp)
                                            Text("${profile.displayName} · ${profile.relationship}")
                                        }
                                    },
                                    onClick = { profileMenuExpanded = false; viewModel.selectProfile(profile.id) }
                                )
                            }
                            DropdownMenuItem(text = { Text("Add family profile") }, onClick = { profileMenuExpanded = false; addProfileDialog = true })
                        }
                    }
                    IconButton(onClick = onNavigateToSettings) { Icon(Icons.Default.Settings, contentDescription = "Settings") }
                }
                if (uiState.profileError != null) {
                    Text(uiState.profileError.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(28.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF103F3A), Color(0xFF176D61), Color(0xFF208C78))))
                            .clickable { viewModel.createNewChat { onNavigateToChat(it) } }
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF9AF0C7)))
                                    Text(modelStatusLabel(uiState.aiModelStatus), style = MaterialTheme.typography.labelMedium, color = Color(0xFFD4F5E7))
                                }
                                Text("Your health,\nin one place", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White, lineHeight = 30.sp)
                                Text("Reports, measurements and care reminders", style = MaterialTheme.typography.bodySmall, color = Color(0xFFD4EDE6))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    androidx.compose.material3.Surface(shape = RoundedCornerShape(50), color = Color.White, onClick = { viewModel.createNewChat { onNavigateToChat(it) } }) {
                                        Text("Ask Gemma  →", Modifier.padding(horizontal = 14.dp, vertical = 9.dp), color = Color(0xFF124D43), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                                    }
                                    androidx.compose.material3.Surface(shape = RoundedCornerShape(50), color = Color(0x33FFFFFF), onClick = onNavigateToVault) {
                                        Text("Add report", Modifier.padding(horizontal = 14.dp, vertical = 9.dp), color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }
                            androidx.compose.material3.Surface(shape = CircleShape, color = Color(0x33FFFFFF), modifier = Modifier.size(66.dp)) {
                                Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFFF93A9), modifier = Modifier.padding(19.dp))
                            }
                        }
                    }
                    SectionHeading(title = "Latest from your reports", action = "Timeline", onAction = onNavigateToHistory)
                    if (uiState.latestMeasurements.isEmpty()) {
                        androidx.compose.material3.Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth().clickable(onClick = onNavigateToVault)
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFD95772), modifier = Modifier.size(23.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Your health metrics will appear here", fontWeight = FontWeight.SemiBold)
                                    Text("Add a report to see values from your records.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                androidx.compose.material3.TextButton(onClick = onNavigateToVault) { Text("Add") }
                            }
                        }
                    } else {
                        uiState.latestMeasurements.take(4).chunked(2).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                row.forEach { measurement ->
                                    MetricTile(measurement = measurement, dateFormat = dateFormat, modifier = Modifier.weight(1f))
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            item {
                SectionHeading(
                    title = "Medicine reminders",
                    action = "Add",
                    onAction = { addReminderDialog = true }
                )
            }
            if (uiState.medicationReminders.isEmpty()) {
                item {
                    EmptyState(
                        text = "Set a daily reminder using the medicine schedule provided by your clinician.",
                        action = "Add reminder",
                        onAction = { addReminderDialog = true }
                    )
                }
            } else {
                items(uiState.medicationReminders, key = { "reminder-${it.id}" }) { reminder ->
                    MedicationReminderRow(
                        reminder = reminder,
                        onDelete = { viewModel.deleteMedicationReminder(reminder.id) }
                    )
                }
            }

            if (activeProfile?.sexAtBirth == "Female") {
                item {
                    SectionHeading(title = "Cycle health", action = "Log day", onAction = { cycleLogDialog = true })
                }
                item {
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Private cycle log", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            val estimate = uiState.cycleSummary.nextPeriodEstimate
                            Text(
                                estimate?.let { "Estimated next period: ${it.format(DateTimeFormatter.ofPattern("d MMM yyyy"))} · based on ${uiState.cycleSummary.intervalsUsed} logged intervals" }
                                    ?: "Log period start dates to see a simple estimate.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text("Estimate only. Not for diagnosis, pregnancy planning, or contraception.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            uiState.cycleSummary.recentLogs.take(5).forEach { log ->
                                Text("${log.date} · ${log.flow.name.lowercase()}${if (log.isPeriodStart) " · period start" else ""}", style = MaterialTheme.typography.bodySmall)
                            }
                            if (uiState.cycleError != null) Text(uiState.cycleError.orEmpty(), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            item {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        androidx.compose.material3.Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.padding(10.dp), tint = MaterialTheme.colorScheme.secondary)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("On-device medical AI", fontWeight = FontWeight.SemiBold)
                            Text(modelStatusLabel(uiState.aiModelStatus), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Model settings")
                        }
                    }
                }
            }

            item {
                SectionHeading(title = "Recent records", action = "View all", onAction = onNavigateToVault)
            }
            if (uiState.recentDocuments.isEmpty()) {
                item { EmptyState(text = "Your reports will appear here after you add them.", action = "Add a record", onAction = onNavigateToVault) }
            } else {
                items(uiState.recentDocuments.take(3), key = { "doc-${it.id}" }) { document ->
                    DocumentRow(document = document, dateFormat = dateFormat, onClick = onNavigateToVault)
                }
            }

            item { SectionHeading(title = "Recent measurements", action = "Timeline", onAction = onNavigateToHistory) }
            if (uiState.latestMeasurements.isEmpty()) {
                item { EmptyState(text = "Measurements will show after readable reports are processed.", action = "Open records", onAction = onNavigateToVault) }
            } else {
                items(uiState.latestMeasurements.take(4), key = { "measurement-${it.id}" }) { measurement ->
                    MeasurementRow(measurement = measurement, dateFormat = dateFormat)
                }
            }

            item { SectionHeading(title = "Recent conversations", action = null, onAction = {}) }
            if (uiState.recentConversations.isEmpty()) {
                item { EmptyState(text = "Your consultations will appear here.", action = "Start a chat", onAction = { viewModel.createNewChat { onNavigateToChat(it) } }) }
            } else {
                items(uiState.recentConversations.take(3), key = { "chat-${it.id}" }) { conversation ->
                    ConversationRow(conversation = conversation, dateFormat = dateFormat, onClick = { onNavigateToChat(conversation.id) })
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }

    if (addProfileDialog) {
        AlertDialog(
            onDismissRequest = {
                addProfileDialog = false
                viewModel.clearProfileError()
            },
            title = { Text("Add an on-device profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("This creates a separate profile on this device only. For a person with their own account, use Settings → Account & Family to join your family ID.")
                    OutlinedTextField(
                        value = newProfileName,
                        onValueChange = { newProfileName = it },
                        label = { Text("Name") },
                        singleLine = true
                    )
                    Text("Choose an avatar", style = MaterialTheme.typography.labelMedium)
                    PersonProfileRepository.AVATAR_SEEDS.chunked(4).forEach { seeds ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            seeds.forEach { seed ->
                                Surface(
                                    onClick = { newProfileAvatarSeed = seed },
                                    shape = CircleShape,
                                    border = BorderStroke(
                                        if (seed == newProfileAvatarSeed) 2.dp else 1.dp,
                                        if (seed == newProfileAvatarSeed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                    )
                                ) {
                                    DiceBearAvatar(seed = seed, size = 48.dp, modifier = Modifier.padding(2.dp))
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = newProfileRelationship,
                        onValueChange = { newProfileRelationship = it },
                        label = { Text("Relationship (for example, parent)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newProfileDateOfBirth,
                        onValueChange = { newProfileDateOfBirth = it },
                        label = { Text("Date of birth (YYYY-MM-DD, optional)") },
                        supportingText = { Text("Used to tailor local explanations; no age-based range is invented.") },
                        singleLine = true
                    )
                    Column {
                        OutlinedButton(onClick = { sexMenuExpanded = true }) {
                            Text("Sex at birth: ${newProfileSexAtBirth ?: "not specified"}")
                        }
                        DropdownMenu(
                            expanded = sexMenuExpanded,
                            onDismissRequest = { sexMenuExpanded = false }
                        ) {
                            listOf("Not specified", "Female", "Male", "Intersex", "Prefer not to say").forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        newProfileSexAtBirth = option.takeUnless { it == "Not specified" }
                                        sexMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    if (uiState.profileError != null) {
                        Text(uiState.profileError.orEmpty(), color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.createProfile(
                        newProfileName,
                        newProfileRelationship,
                        newProfileDateOfBirth,
                        newProfileSexAtBirth,
                        newProfileAvatarSeed
                    ) {
                        newProfileName = ""
                        newProfileRelationship = ""
                        newProfileDateOfBirth = ""
                        newProfileSexAtBirth = null
                        newProfileAvatarSeed = "avatar-01"
                        addProfileDialog = false
                    }
                }) { Text("Create profile") }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    addProfileDialog = false
                    viewModel.clearProfileError()
                }) { Text("Cancel") }
            }
        )
    }

    if (addReminderDialog) {
        AlertDialog(
            onDismissRequest = { addReminderDialog = false },
            title = { Text("Add medicine reminder") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Repeats daily for ${activeProfile?.displayName ?: "Me"}. Enter the schedule exactly as prescribed.")
                    OutlinedTextField(
                        value = reminderName,
                        onValueChange = { reminderName = it },
                        label = { Text("Medicine name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = reminderInstructions,
                        onValueChange = { reminderInstructions = it },
                        label = { Text("Dose note (optional)") },
                        supportingText = { Text("This is a reminder note, not a dose recommendation.") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = reminderTime,
                        onValueChange = { reminderTime = it },
                        label = { Text("Time (24-hour HH:mm)") },
                        singleLine = true
                    )
                    Text("Notifications and exact alarm access are controlled by Android settings.", style = MaterialTheme.typography.bodySmall)
                    if (uiState.reminderError != null) {
                        Text(uiState.reminderError.orEmpty(), color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.createMedicationReminder(reminderName, reminderInstructions, reminderTime) {
                        reminderName = ""
                        reminderInstructions = ""
                        reminderTime = "09:00"
                        addReminderDialog = false
                        if (Build.VERSION.SDK_INT >= 33 &&
                            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }) { Text("Save daily reminder") }
            },
            dismissButton = {
                OutlinedButton(onClick = { addReminderDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (cycleLogDialog && activeProfile?.sexAtBirth == "Female") {
        AlertDialog(
            onDismissRequest = { cycleLogDialog = false; viewModel.clearCycleError() },
            title = { Text("Log cycle day") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = cycleDate, onValueChange = { cycleDate = it }, label = { Text("Date (YYYY-MM-DD)") }, singleLine = true)
                    OutlinedButton(onClick = { cycleFlowMenuExpanded = true }) { Text("Flow: ${cycleFlow.name.lowercase()}") }
                    DropdownMenu(expanded = cycleFlowMenuExpanded, onDismissRequest = { cycleFlowMenuExpanded = false }) {
                        MenstrualFlow.entries.forEach { flow ->
                            DropdownMenuItem(text = { Text(flow.name.lowercase()) }, onClick = { cycleFlow = flow; cycleFlowMenuExpanded = false })
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isPeriodStart, onCheckedChange = { isPeriodStart = it })
                        Text("Period started today")
                    }
                    com.swasthai.app.data.repository.CycleTrackingRepository.SYMPTOM_TAGS.forEach { symptom ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = symptom in cycleSymptoms, onCheckedChange = { checked ->
                                cycleSymptoms = if (checked) cycleSymptoms + symptom else cycleSymptoms - symptom
                            })
                            Text(symptom.replaceFirstChar { it.uppercase() })
                        }
                    }
                    Text("Saved encrypted on this device for this profile.", style = MaterialTheme.typography.bodySmall)
                    if (uiState.cycleError != null) Text(uiState.cycleError.orEmpty(), color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.logCycleDay(cycleDate, cycleFlow, isPeriodStart, cycleSymptoms) {
                        cycleLogDialog = false
                        cycleDate = LocalDate.now().toString()
                        cycleFlow = MenstrualFlow.LIGHT
                        isPeriodStart = false
                        cycleSymptoms = emptySet()
                    }
                }) { Text("Save log") }
            },
            dismissButton = { OutlinedButton(onClick = { cycleLogDialog = false; viewModel.clearCycleError() }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SectionHeading(title: String, action: String?, onAction: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (action != null) Text(action, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable(onClick = onAction).padding(6.dp))
    }
}

@Composable
private fun EmptyState(text: String, action: String, onAction: () -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(action, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable(onClick = onAction).padding(4.dp))
        }
    }
}

@Composable
private fun DocumentRow(document: MedicalDocument, dateFormat: SimpleDateFormat, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(document.fileName, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(dateFormat.format(Date(document.importDate)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(indexingLabel(document.indexingStatus), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun MeasurementRow(measurement: StructuredMeasurement, dateFormat: SimpleDateFormat) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(measurement.biomarkerName, fontWeight = FontWeight.SemiBold)
                val dateHint = if (measurement.testDateSource == "IMPORT_DATE_FALLBACK") " · report date unavailable" else ""
                Text(dateFormat.format(Date(measurement.testDate)) + dateHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                val valuePrefix = when (measurement.valueComparator) {
                    "<" -> "< "
                    "<=" -> "≤ "
                    ">" -> "> "
                    ">=" -> "≥ "
                    else -> ""
                }
                Text("$valuePrefix${measurement.numericValue} ${measurement.unit}", fontWeight = FontWeight.Bold)
                if (measurement.referenceLow != null && measurement.referenceHigh != null) {
                    Text(
                        "Reported range ${measurement.referenceLow}–${measurement.referenceHigh} · verify",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text("Unreviewed extraction", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ConversationRow(conversation: Conversation, dateFormat: SimpleDateFormat, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Column(Modifier.weight(1f)) {
                Text(conversation.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(dateFormat.format(Date(conversation.updatedAt)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MedicationReminderRow(reminder: MedicationReminderEntity, onDelete: () -> Unit) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(reminder.medicationName, fontWeight = FontWeight.SemiBold)
                Text(
                    listOfNotNull(reminder.instructions.takeIf(String::isNotBlank), "Daily · ${"%02d:%02d".format(reminder.hour, reminder.minute)}").joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete reminder")
            }
        }
    }
}

private fun profileAgeLabel(profile: PersonProfileEntity): String? {
    val dob = profile.dateOfBirth ?: return null
    val birthDate = Instant.ofEpochMilli(dob).atZone(ZoneOffset.UTC).toLocalDate()
    val age = Period.between(birthDate, LocalDate.now(ZoneOffset.UTC)).years
    return if (age >= 0) "$age years" else null
}

@Composable
private fun MetricTile(
    measurement: StructuredMeasurement,
    dateFormat: SimpleDateFormat,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFD95772), modifier = Modifier.size(16.dp))
                Text(measurement.biomarkerName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            val prefix = when (measurement.valueComparator) {
                "<" -> "< "
                "<=" -> "≤ "
                ">" -> "> "
                ">=" -> "≥ "
                else -> ""
            }
            Text("$prefix${measurement.numericValue}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(measurement.unit.ifBlank { "Value" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("·", color = MaterialTheme.colorScheme.outline)
                Text(dateFormat.format(Date(measurement.testDate)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            Text("From report · verify source", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun homeGreeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "GOOD MORNING"
    in 12..16 -> "GOOD AFTERNOON"
    else -> "GOOD EVENING"
}

private fun indexingLabel(status: IndexingStatus): String = when (status) {
    IndexingStatus.PENDING -> "Processing"
    IndexingStatus.EXTRACTED -> "Text ready"
    IndexingStatus.INDEXED -> "Indexed"
    IndexingStatus.PARTIAL -> "Partial text"
    IndexingStatus.FAILED -> "Needs review"
}

private fun modelStatusLabel(status: ModelInferenceStatus): String = when (status) {
    ModelInferenceStatus.NOT_INSTALLED -> "Model not installed"
    ModelInferenceStatus.INITIALIZING -> "Loading Gemma 4…"
    ModelInferenceStatus.READY -> "Gemma 4 ready on this device"
    ModelInferenceStatus.GENERATING -> "Gemma 4 is responding"
    ModelInferenceStatus.ERROR -> "Could not start Gemma 4"
}
