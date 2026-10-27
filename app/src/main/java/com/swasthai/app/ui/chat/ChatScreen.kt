package com.swasthai.app.ui.chat

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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swasthai.app.domain.model.ChatMessage
import com.swasthai.app.domain.model.MessageRole
import com.swasthai.app.domain.model.ResponseLanguage
import com.swasthai.app.domain.model.SourceCitation
import com.swasthai.app.ui.components.CitationInspectionDialog
import com.swasthai.app.ui.components.EmergencyBanner
import com.swasthai.app.ui.theme.AppleBlueLight
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

    var inspectedCitation by remember { mutableStateOf<SourceCitation?>(null) }

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
            Column(modifier = Modifier.fillMaxWidth().background(AppleSystemBackground)) {
                TopAppBar(
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "AI Doc",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AppleLabelPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF34C759))
                                )
                            }
                            Text(
                                text = "Medical History Active • 100% On-Device",
                                fontSize = 11.sp,
                                color = AppleLabelSecondary
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.createNewConversation() }) {
                            Icon(Icons.Default.Add, contentDescription = "New Consultation", tint = AppleSystemBlue)
                        }
                        if (uiState.currentConversationId != null) {
                            IconButton(onClick = { viewModel.deleteCurrentConversation() }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = AppleSystemRed)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = AppleSystemBackground
                    )
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
                .imePadding()
        ) {
            // Interactive Medical Record Context Scope Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppleSystemBackground)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(AppleSearchFill)
                        .clickable { viewModel.openContextSheet(true) }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Scope Configuration",
                        tint = AppleSystemBlue,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = uiState.currentScope.formatScopePill(uiState.availableDocuments),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppleLabelPrimary,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

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
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = langLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppleHealthTeal
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(AppleSeparator)
            )

            // Emergency Safety Alert
            if (uiState.emergencyWarning != null) {
                EmergencyBanner(
                    message = uiState.emergencyWarning!!,
                    modifier = Modifier.padding(12.dp)
                )
            }

            // Messages Stream
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(10.dp)) }

                if (uiState.currentMessages.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp, horizontal = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(AppleTealLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HealthAndSafety,
                                    contentDescription = null,
                                    tint = AppleHealthTeal,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Dr. Swasth AI Consultation",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AppleLabelPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Your personal doctor-grade assistant running 100% locally on your Snapdragon 8 Elite.\nChoose your context scope to query saved blood reports, lipid panels, or chat freely.",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppleLabelSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            // Privacy & Architecture Badge
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(AppleCardSurface)
                                    .border(0.5.dp, AppleCardBorder, RoundedCornerShape(20.dp))
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = Color(0xFF34C759),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Zero Cloud Calls • 100% Private",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppleLabelSecondary
                                )
                            }
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

                // Streaming / Generating Indicator
                if (uiState.isStreaming) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = AppleSystemBlue
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Generating clinical guidance...",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppleLabelSecondary
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(10.dp)) }
            }

            // Input Bar with Context Control Button
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
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Clearly Visible Context Control Button
                    IconButton(
                        onClick = { viewModel.openContextSheet(true) },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(AppleSearchFill)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Medical Context Scope",
                            tint = AppleSystemBlue,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        value = uiState.inputText,
                        onValueChange = { viewModel.setInputText(it) },
                        placeholder = {
                            Text(
                                "Ask Dr. Swasth...",
                                color = AppleLabelTertiary,
                                fontSize = 15.sp
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(22.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = AppleSearchFill,
                            unfocusedContainerColor = AppleSearchFill,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = AppleLabelPrimary,
                            unfocusedTextColor = AppleLabelPrimary
                        ),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    val canSend = uiState.inputText.isNotBlank() && !uiState.isStreaming
                    IconButton(
                        onClick = { viewModel.sendMessage() },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (canSend) AppleSystemBlue else AppleSearchFill),
                        enabled = canSend
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (canSend) Color.White else AppleLabelTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
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
            RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
        } else {
            RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
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
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = cleanContent,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor,
                lineHeight = 22.sp
            )
        }

        // Citations Footnotes (Apple Pastel Blue Pills)
        if (message.citations.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                message.citations.forEach { citation ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppleBlueLight)
                            .clickable { onCitationClick(citation) }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = AppleSystemBlue,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = citation.formatDisplay(),
                            fontSize = 11.sp,
                            color = AppleSystemBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
