package com.swasthai.app.ui.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swasthai.app.domain.model.ChatMessage
import com.swasthai.app.domain.model.MessageRole
import com.swasthai.app.domain.model.ModelInferenceStatus
import com.swasthai.app.domain.model.ResponseLanguage
import com.swasthai.app.domain.model.SourceCitation
import com.swasthai.app.ui.components.CitationInspectionDialog
import com.swasthai.app.ui.components.EmergencyBanner
import com.swasthai.app.ui.components.openEmergencyDialer
import com.swasthai.app.ui.theme.AppleBlueLight
import com.swasthai.app.ui.theme.AppleOrangeLight
import com.swasthai.app.ui.theme.AppleCardBorder
import com.swasthai.app.ui.theme.AppleCardSurface
import com.swasthai.app.ui.theme.AppleGroupedBackground
import com.swasthai.app.ui.theme.AppleHealthTeal
import com.swasthai.app.ui.theme.AppleIMessageBlue
import com.swasthai.app.ui.theme.AppleIMessageGray
import com.swasthai.app.ui.theme.AppleIMessageText
import com.swasthai.app.ui.theme.AppleLabelPrimary
import com.swasthai.app.ui.theme.AppleLabelSecondary
import com.swasthai.app.ui.theme.AppleLabelTertiary
import com.swasthai.app.ui.theme.AppleSearchFill
import com.swasthai.app.ui.theme.AppleSeparator
import com.swasthai.app.ui.theme.AppleSystemBackground
import com.swasthai.app.ui.theme.AppleSystemBlue
import com.swasthai.app.ui.theme.AppleSystemRed
import com.swasthai.app.ui.theme.AppleTealLight

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    initialConversationId: Long? = null,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    var inspectedCitation by remember { mutableStateOf<SourceCitation?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.selectImage(uri.toString())
        }
    }

    LaunchedEffect(initialConversationId) {
        if (initialConversationId != null && initialConversationId > 0) {
            viewModel.selectConversation(initialConversationId)
        }
    }

    LaunchedEffect(uiState.currentMessages.size) {
        if (uiState.currentMessages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.currentMessages.size - 1)
        }
    }

    if (inspectedCitation != null) {
        CitationInspectionDialog(
            citation = inspectedCitation!!,
            onDismiss = { inspectedCitation = null }
        )
    }

    if (uiState.isContextSheetOpen) {
        ContextConfigSheet(
            uiState = uiState,
            viewModel = viewModel,
            onDismiss = { viewModel.openContextSheet(false) }
        )
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppleSystemBackground)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Health assistant", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppleLabelPrimary)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(7.dp).clip(CircleShape).background(if (uiState.modelStatus == ModelInferenceStatus.READY || uiState.modelStatus == ModelInferenceStatus.GENERATING) Color(0xFF34C759) else AppleSystemRed))
                            Text("Gemma 4 E2B · on this device", style = MaterialTheme.typography.labelSmall, color = AppleLabelSecondary)
                        }
                    }
                    IconButton(onClick = { viewModel.createNewConversation() }) {
                        Icon(Icons.Default.Add, contentDescription = "New chat", tint = AppleSystemBlue)
                    }
                    IconButton(onClick = { openEmergencyDialer(context) }) {
                        Icon(Icons.Default.Call, contentDescription = "Call emergency services", tint = AppleSystemRed)
                    }
                    if (uiState.currentConversationId != null) {
                        IconButton(onClick = { viewModel.deleteCurrentConversation() }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete chat", tint = AppleLabelSecondary)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(AppleSearchFill)
                            .clickable { viewModel.openContextSheet(true) }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = AppleSystemBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(
                            text = uiState.currentScope.formatScopePill(uiState.availableDocuments),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppleLabelPrimary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    val langLabel = when (uiState.currentScope.responseLanguage) {
                        ResponseLanguage.AUTO -> "Auto"
                        ResponseLanguage.ENGLISH -> "EN"
                        ResponseLanguage.HINDI -> "हिन्दी"
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(AppleTealLight)
                            .clickable { viewModel.openContextSheet(true) }
                            .padding(horizontal = 12.dp, vertical = 9.dp)
                    ) {
                        Text(
                            text = langLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppleHealthTeal
                        )
                    }
                }
            }
        },
        containerColor = AppleGroupedBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            if (uiState.modelStatus != ModelInferenceStatus.READY) {
                Text(
                    text = "Gemma 4 E2B is not ready. Local AI chat and record search are unavailable until the model is initialized. Tap to open setup.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppleOrangeLight)
                        .clickable(onClick = onNavigateToSettings)
                        .padding(12.dp),
                    color = AppleLabelPrimary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            // Emergency Safety Alert
            if (uiState.emergencyWarning != null) {
                EmergencyBanner(
                    message = uiState.emergencyWarning!!,
                    onCallEmergency = { openEmergencyDialer(context) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            if (uiState.isStreaming) {
                val activity = uiState.currentMessages.lastOrNull { it.isStreaming }?.activityLabel
                val detail = activity ?: "Thinking through your question"
                val headline = when {
                    detail.contains("report", ignoreCase = true) || detail.contains("record", ignoreCase = true) -> "Looking through your records"
                    detail.contains("search", ignoreCase = true) || detail.contains("brows", ignoreCase = true) -> "Searching your health history"
                    detail.contains("writing", ignoreCase = true) -> "Preparing your answer"
                    else -> "Gemma is thinking"
                }
                androidx.compose.material3.Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = AppleTealLight
                ) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = AppleHealthTeal)
                        Column(Modifier.weight(1f)) {
                            Text(headline, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = AppleLabelPrimary)
                            Text(detail, style = MaterialTheme.typography.bodySmall, color = AppleLabelSecondary, maxLines = 1)
                        }
                        Text("ON-DEVICE", style = MaterialTheme.typography.labelSmall, color = AppleHealthTeal, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Messages Stream
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                if (uiState.currentMessages.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp, horizontal = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(AppleTealLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HealthAndSafety,
                                    contentDescription = null,
                                    tint = AppleHealthTeal,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Dr. Swasth AI Consultation",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AppleLabelPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "100% on-device clinical AI. Type your question or attach lab photos.",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppleLabelSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(uiState.currentMessages) { message ->
                        AppleChatMessageBubble(
                            message = message,
                            onCitationClick = { inspectedCitation = it }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(4.dp)) }
            }

            // ATTACHED IMAGE PREVIEW (shows when user picked a lab report / prescription photo)
            if (uiState.selectedImageUri != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppleSystemBackground)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, AppleCardBorder, RoundedCornerShape(8.dp))
                    ) {
                        MessageImageThumbnail(imageUri = uiState.selectedImageUri!!)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Medical Image Attached",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppleLabelPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewModel.clearSelectedImage() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove attached image",
                            tint = AppleLabelTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // ULTRA COMPACT INPUT BAR (saves space, fits tightly above keyboard)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppleSystemBackground)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(AppleSeparator)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Image / Document Camera Picker Button
                    IconButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Attach Medical Document or Image",
                            tint = if (uiState.selectedImageUri != null) AppleHealthTeal else AppleSystemBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Ultra-compact BasicTextField (zero wasted padding when keyboard is open)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(AppleSearchFill)
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (uiState.inputText.isEmpty()) {
                            Text(
                                text = if (uiState.selectedImageUri != null) "Ask about this image..." else "Ask Dr. Swasth...",
                                color = AppleLabelTertiary,
                                fontSize = 13.5.sp
                            )
                        }
                        BasicTextField(
                            value = uiState.inputText,
                            onValueChange = { viewModel.setInputText(it) },
                            textStyle = TextStyle(
                                fontSize = 13.5.sp,
                                color = AppleLabelPrimary,
                                fontWeight = FontWeight.Normal
                            ),
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    val canSend = (uiState.inputText.isNotBlank() || uiState.selectedImageUri != null) && !uiState.isStreaming
                    IconButton(
                        onClick = { viewModel.sendMessage() },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (canSend) AppleSystemBlue else AppleSearchFill),
                        enabled = canSend
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (canSend) Color.White else AppleLabelTertiary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageImageThumbnail(
    imageUri: String,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .heightIn(max = 200.dp)
        .clip(RoundedCornerShape(12.dp))
) {
    val context = LocalContext.current
    val bitmap = remember(imageUri) {
        try {
            val uri = Uri.parse(imageUri)
            val inputStream = context.contentResolver.openInputStream(uri)
            android.graphics.BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "Attached Medical Document",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppleChatMessageBubble(
    message: ChatMessage,
    onCitationClick: (SourceCitation) -> Unit
) {
    val isUser = message.role == MessageRole.USER
    val alignment = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        val bubbleColor = if (isUser) AppleIMessageBlue else AppleCardSurface
        val textColor = if (isUser) Color.White else AppleIMessageText
        val shape = if (isUser) {
            RoundedCornerShape(16.dp, 16.dp, 3.dp, 16.dp)
        } else {
            RoundedCornerShape(16.dp, 16.dp, 16.dp, 3.dp)
        }

        val cleanContent = if (message.content.startsWith("null")) {
            message.content.removePrefix("null")
        } else {
            message.content
        }

        Box(
            modifier = Modifier
                .clip(shape)
                .background(bubbleColor)
                .then(
                    if (!isUser) Modifier.border(0.5.dp, AppleCardBorder, shape) else Modifier
                )
                .padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            Column {
                // If message contains an attached image, render it inside bubble!
                if (!message.imageUri.isNullOrBlank()) {
                    MessageImageThumbnail(imageUri = message.imageUri)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (cleanContent.isNotBlank()) {
                    Text(
                        text = cleanContent,
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor,
                        fontSize = 15.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // Citations Footnotes
        if (message.citations.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.padding(horizontal = 2.dp)
            ) {
                message.citations.forEach { citation ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(AppleBlueLight)
                            .clickable { onCitationClick(citation) }
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = AppleSystemBlue,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = citation.formatDisplay(),
                            fontSize = 10.sp,
                            color = AppleSystemBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
