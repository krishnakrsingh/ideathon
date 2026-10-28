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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swasthai.app.domain.model.ModelInferenceStatus
import com.swasthai.app.ai.ModelArtifactDownloadStatus
import com.swasthai.app.ui.theme.AppleBlueLight
import com.swasthai.app.ui.theme.AppleCardBorder
import com.swasthai.app.ui.theme.AppleCardSurface
import com.swasthai.app.ui.theme.AppleGroupedBackground
import com.swasthai.app.ui.theme.AppleLabelPrimary
import com.swasthai.app.ui.theme.AppleLabelSecondary
import com.swasthai.app.ui.theme.AppleLabelTertiary
import com.swasthai.app.ui.theme.AppleSearchFill
import com.swasthai.app.ui.theme.AppleSeparator
import com.swasthai.app.ui.theme.AppleSystemBackground
import com.swasthai.app.ui.theme.AppleSystemBlue
import com.swasthai.app.ui.theme.AppleSystemRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()

    var showClearConfirm by remember { mutableStateOf(false) }
    var memberToRemove by remember { mutableStateOf<Pair<String, String>?>(null) }
    var familyToLeave by remember { mutableStateOf<String?>(null) }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Delete local reports?", fontWeight = FontWeight.Bold, color = AppleLabelPrimary) },
            text = {
                Text(
                    "This removes this profile's encrypted PDFs, extracted text, embeddings, and measurement candidates from this device. It does not delete chats or server data.",
                    fontSize = 13.sp,
                    color = AppleLabelSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirm = false
                        viewModel.clearLocalReports()
                    },
                    enabled = !uiState.isClearingRecords,
                    colors = ButtonDefaults.buttonColors(containerColor = AppleSystemRed)
                ) {
                    Text(if (uiState.isClearingRecords) "Deleting…" else "Delete reports", color = Color.White)
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

    memberToRemove?.let { (familyId, userId) ->
        AlertDialog(
            onDismissRequest = { memberToRemove = null },
            title = { Text("Remove family member?") },
            text = { Text("This revokes future family access for this account. It cannot erase data or keys already copied to an authorized device.") },
            confirmButton = {
                TextButton(onClick = {
                    memberToRemove = null
                    viewModel.removeFamilyMember(familyId, userId)
                }) { Text("Remove", color = AppleSystemRed) }
            },
            dismissButton = { TextButton(onClick = { memberToRemove = null }) { Text("Cancel") } }
        )
    }

    familyToLeave?.let { familyId ->
        AlertDialog(
            onDismissRequest = { familyToLeave = null },
            title = { Text("Leave this family?") },
            text = { Text("You will lose future access to this family's records and need the current family credentials to rejoin.") },
            confirmButton = {
                TextButton(onClick = {
                    familyToLeave = null
                    viewModel.leaveFamily(familyId)
                }) { Text("Leave", color = AppleSystemRed) }
            },
            dismissButton = { TextButton(onClick = { familyToLeave = null }) { Text("Cancel") } }
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

            SettingsSectionHeader(title = "ACCOUNT & FAMILY")
            Card(
                modifier = Modifier.fillMaxWidth().border(0.5.dp, AppleCardBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = AppleCardSurface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Secure account services", fontWeight = FontWeight.Bold, color = AppleLabelPrimary)
                    Text(
                        "Local records work without an account. Sign in to manage family membership and inspect cloud profile access. Record sync is not connected. Use HTTPS for remote servers; HTTP is accepted only for local development.",
                        fontSize = 12.sp,
                        color = AppleLabelSecondary
                    )
                    OutlinedTextField(
                        value = uiState.authUrl,
                        onValueChange = viewModel::setAuthUrl,
                        label = { Text("Better Auth server origin") },
                        placeholder = { Text("https://auth.example") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uiState.recordsApiUrl,
                        onValueChange = viewModel::setRecordsApiUrl,
                        label = { Text("Encrypted records API origin") },
                        placeholder = { Text("https://api.example") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (uiState.signedInEmail == null) {
                        OutlinedTextField(
                            value = uiState.accountName,
                            onValueChange = viewModel::setAccountName,
                            label = { Text("Name for a new account") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.accountEmail,
                            onValueChange = viewModel::setAccountEmail,
                            label = { Text("Email") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.accountPassword,
                            onValueChange = viewModel::setAccountPassword,
                            label = { Text("Password") },
                            supportingText = { Text("Use at least 12 characters.") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = viewModel::signIn, enabled = !uiState.accountBusy) { Text("Sign in") }
                            OutlinedButton(onClick = viewModel::signUp, enabled = !uiState.accountBusy) { Text("Create account") }
                        }
                    } else {
                        Text("Signed in as ${uiState.signedInEmail}", fontWeight = FontWeight.SemiBold, color = AppleLabelPrimary)
                        Text("Account ID: ${uiState.signedInUserId}", fontSize = 11.sp, color = AppleLabelSecondary)
                        TextButton(onClick = viewModel::signOut, enabled = !uiState.accountBusy) { Text("Sign out") }
                    }
                    uiState.accountError?.let { Text(it, color = AppleSystemRed, fontSize = 12.sp) }
                    uiState.accountMessage?.let { Text(it, color = AppleSystemBlue, fontSize = 12.sp) }

                    if (uiState.signedInEmail != null) {
                        SettingsDivider()
                        Text("Family", fontWeight = FontWeight.Bold, color = AppleLabelPrimary)
                        Text("Family membership and roster actions use the records API. The server can expose member ciphertext to the head, but Android profile mapping, key grants, decryption, and record sync are not connected; family reports are not readable in chat yet.", fontSize = 12.sp, color = AppleLabelSecondary)
                        Button(onClick = viewModel::createFamily, enabled = !uiState.accountBusy) { Text("Create family ID and password") }
                        uiState.oneTimeFamilyCredentials?.let { credentials ->
                            Column(modifier = Modifier.fillMaxWidth().background(AppleBlueLight, RoundedCornerShape(10.dp)).padding(12.dp)) {
                                Text("Save these credentials now; the password is only returned once.", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                SelectionContainer {
                                    Column {
                                        Text("Family ID: ${credentials.familyId}", fontSize = 12.sp)
                                        Text("Family password: ${credentials.joinPassword}", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        OutlinedTextField(
                            value = uiState.familyIdInput,
                            onValueChange = viewModel::setFamilyIdInput,
                            label = { Text("Family ID") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.familyPasswordInput,
                            onValueChange = viewModel::setFamilyPasswordInput,
                            label = { Text("Family password") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = viewModel::joinFamily, enabled = !uiState.accountBusy) { Text("Join family") }
                            TextButton(onClick = viewModel::refreshFamilies, enabled = !uiState.accountBusy) { Text("Refresh") }
                        }
                        uiState.families.forEach { family ->
                            TextButton(onClick = { viewModel.selectFamily(family.familyId) }) {
                                Text("${family.yourRole.uppercase()} · ${family.familyId}", color = AppleSystemBlue)
                            }
                        }
                        Text("Cloud profiles visible to this account", fontWeight = FontWeight.SemiBold, color = AppleLabelPrimary)
                        if (uiState.accessibleCloudProfiles.isEmpty()) {
                            Text("No cloud profiles are available from the records API.", fontSize = 12.sp, color = AppleLabelSecondary)
                        }
                        uiState.accessibleCloudProfiles.forEach { profile ->
                            val roleLabel = when (profile.accessRole) {
                                "owner" -> "Owned by this account"
                                "family_head" -> "Family member profile"
                                else -> "Shared profile · ${profile.accessRole}"
                            }
                            val accessDetail = when {
                                profile.accessRole == "owner" -> "Local and cloud record sync are not connected."
                                profile.hasDecryptionGrant -> "A key grant exists; Android decryption and import are not connected."
                                else -> "The server can provide ciphertext, but this account has no decryption grant."
                            }
                            Column(modifier = Modifier.fillMaxWidth().background(AppleSystemBackground, RoundedCornerShape(10.dp)).padding(10.dp)) {
                                Text("$roleLabel · ${profile.profileId.take(8)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AppleLabelPrimary)
                                Text(accessDetail, fontSize = 11.sp, color = AppleLabelSecondary)
                            }
                        }
                        uiState.selectedFamily?.let { family ->
                            Text("Members", fontWeight = FontWeight.SemiBold, color = AppleLabelPrimary)
                            family.members.forEach { member ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(member.userId, fontSize = 12.sp, color = AppleLabelPrimary)
                                        Text(member.role, fontSize = 11.sp, color = AppleLabelSecondary)
                                    }
                                    if (family.yourRole == "head" && member.role != "head") {
                                        TextButton(onClick = { memberToRemove = family.familyId to member.userId }) { Text("Remove", color = AppleSystemRed) }
                                    }
                                }
                            }
                            if (family.yourRole == "head" && family.blockedMembers.isNotEmpty()) {
                                Text("Blocked accounts", fontWeight = FontWeight.SemiBold, color = AppleLabelPrimary)
                                family.blockedMembers.forEach { member ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(member.userId, modifier = Modifier.weight(1f), fontSize = 12.sp, color = AppleLabelSecondary)
                                        TextButton(
                                            onClick = { viewModel.unblockFamilyMember(family.familyId, member.userId) },
                                            enabled = !uiState.accountBusy
                                        ) { Text("Allow to rejoin") }
                                    }
                                }
                            }
                            if (family.yourRole == "head") {
                                TextButton(onClick = { viewModel.rotateJoinPassword(family.familyId) }, enabled = !uiState.accountBusy) {
                                    Text("Rotate family password")
                                }
                            } else {
                                TextButton(onClick = { familyToLeave = family.familyId }, enabled = !uiState.accountBusy) {
                                    Text("Leave family", color = AppleSystemRed)
                                }
                            }
                        }
                    }
                }
            }

            // Show implemented security facts; unsupported switches must not imply a working control.
            SettingsSectionHeader(title = "PRIVACY & CONNECTIONS")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, AppleCardBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = AppleCardSurface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Local report vault", fontWeight = FontWeight.SemiBold, color = AppleLabelPrimary)
                    Text("${uiState.localReportCount} report${if (uiState.localReportCount == 1) "" else "s"} on this device. New PDFs are encrypted with AES-GCM; records sync is not connected.", fontSize = 12.sp, color = AppleLabelSecondary)
                    SettingsDivider()
                    Text("AI processing", fontWeight = FontWeight.SemiBold, color = AppleLabelPrimary)
                    Text("Chat, OCR, and embeddings run on device when their model/runtime is available. Account and family APIs handle identity and membership only.", fontSize = 12.sp, color = AppleLabelSecondary)
                    SettingsDivider()
                    Text("Not configured", fontWeight = FontWeight.SemiBold, color = AppleLabelPrimary)
                    Text("Biometric app lock, wearable sync, and cloud report sync are not connected.", fontSize = 12.sp, color = AppleLabelSecondary)
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
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("On-device medical assistant", fontWeight = FontWeight.Bold, color = AppleLabelPrimary)
                        Text(
                            "One Gemma 4 E2B conversation chooses scoped record tools and writes the answer from their results. No thinking channel is enabled, and only one generator is loaded.",
                            fontSize = 12.sp,
                            color = AppleLabelSecondary
                        )
                        Text("${uiState.modelName} · ${uiState.modelStatus}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        if (!uiState.isModelArtifactAvailable) {
                            Text(
                                "The model file is about 2.4 GiB. Download uses Wi-Fi only and needs roughly 3.1 GB free storage.",
                                fontSize = 12.sp,
                                color = AppleLabelSecondary
                            )
                            val download = uiState.modelDownload
                            when (download?.status) {
                                ModelArtifactDownloadStatus.PENDING,
                                ModelArtifactDownloadStatus.RUNNING,
                                ModelArtifactDownloadStatus.PAUSED -> {
                                    LinearProgressIndicator(
                                        progress = { (download.percent ?: 0) / 100f },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text(
                                        "Downloading Gemma 4: ${download.percent ?: 0}%",
                                        fontSize = 12.sp,
                                        color = AppleLabelSecondary
                                    )
                                    OutlinedButton(onClick = viewModel::cancelGemmaModelDownload) {
                                        Text("Cancel download")
                                    }
                                }
                                ModelArtifactDownloadStatus.FAILED -> {
                                    Text(download.message.orEmpty(), fontSize = 12.sp, color = AppleSystemRed)
                                    Button(onClick = viewModel::downloadGemmaModel) { Text("Retry model download") }
                                }
                                else -> Button(onClick = viewModel::downloadGemmaModel) {
                                    Text("Download Gemma 4 · 2.4 GiB")
                                }
                            }
                        }
                        OutlinedTextField(
                            value = uiState.toolModelPath,
                            onValueChange = viewModel::setToolModelPath,
                            label = { Text("Gemma 4 E2B .litertlm path") },
                            supportingText = { Text("Thinking mode is disabled. Tool schemas are enforced by the app.") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (uiState.toolModelError != null) Text(uiState.toolModelError.orEmpty(), color = AppleSystemRed, fontSize = 12.sp)
                        Button(onClick = viewModel::initializeToolModel, enabled = !uiState.isInitializing) {
                            Text(if (uiState.isInitializing) "Initializing…" else "Initialize Gemma 4")
                        }
                        if (uiState.statusMessage != null) Text(uiState.statusMessage.orEmpty(), fontSize = 12.sp, color = AppleSystemBlue)
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
                                text = "Local report data",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.5.sp,
                                color = AppleLabelPrimary
                            )
                            Text(
                                text = "${uiState.localReportCount} report${if (uiState.localReportCount == 1) "" else "s"} for the active profile",
                                fontSize = 11.sp,
                                color = AppleLabelSecondary
                            )
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
                        Text("Delete local reports", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
                if (uiState.recordsOperationMessage != null) Text(uiState.recordsOperationMessage.orEmpty(), modifier = Modifier.padding(horizontal = 16.dp), fontSize = 12.sp, color = AppleSystemBlue)
                if (uiState.recordsOperationError != null) Text(uiState.recordsOperationError.orEmpty(), modifier = Modifier.padding(horizontal = 16.dp), fontSize = 12.sp, color = AppleSystemRed)
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
                                text = "SwasthAI",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = AppleLabelPrimary
                            )
                            Text(
                                text = "Local-first health records and on-device assistant",
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
fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(AppleSeparator)
    )
}
