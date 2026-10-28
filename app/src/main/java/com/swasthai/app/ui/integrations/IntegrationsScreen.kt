package com.swasthai.app.ui.integrations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swasthai.app.domain.model.ModelInferenceStatus

@Composable
fun IntegrationsScreen(
    localRecordCount: Int,
    modelStatus: ModelInferenceStatus,
    onOpenRecords: () -> Unit
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("DATA SOURCES", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Integrations", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "See what is connected to SwasthAI and what still needs setup.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item { SectionTitle("Available now") }
            item {
                IntegrationCard(
                    icon = Icons.Default.Folder,
                    title = "Medical records",
                    detail = if (localRecordCount == 1) "1 report stored on this device" else "$localRecordCount reports stored on this device",
                    state = "LOCAL",
                    onClick = onOpenRecords
                )
            }
            item {
                IntegrationCard(
                    icon = Icons.Default.AutoAwesome,
                    title = "Gemma 4 E2B",
                    detail = modelStatusText(modelStatus),
                    state = if (modelStatus == ModelInferenceStatus.READY || modelStatus == ModelInferenceStatus.GENERATING) "ON DEVICE" else "SETUP NEEDED"
                )
            }

            item { SectionTitle("Not connected") }
            item {
                IntegrationCard(
                    icon = Icons.Default.HealthAndSafety,
                    title = "Health Connect and wearables",
                    detail = "Device health data sync is not configured yet.",
                    state = "NOT CONNECTED"
                )
            }
            item {
                IntegrationCard(
                    icon = Icons.Default.MedicalServices,
                    title = "Labs and diagnostic booking",
                    detail = "No lab or booking provider is linked to this app yet.",
                    state = "PROVIDER NEEDED"
                )
            }
            item {
                IntegrationCard(
                    icon = Icons.Default.People,
                    title = "Family and clinician access",
                    detail = "Cloud accounts and sharing permissions are not set up yet.",
                    state = "CLOUD SETUP NEEDED"
                )
            }
            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    "External connections will request only the permissions they need. Your reports stay in the current on-device vault until cloud sync is implemented.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun IntegrationCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    detail: String,
    state: String,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.padding(10.dp), tint = MaterialTheme.colorScheme.secondary)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                state,
                fontSize = 9.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .padding(start = 4.dp)
            )
        }
    }
}

private fun modelStatusText(status: ModelInferenceStatus): String = when (status) {
    ModelInferenceStatus.NOT_INSTALLED -> "Model file not found on this device."
    ModelInferenceStatus.INITIALIZING -> "Loading the local model."
    ModelInferenceStatus.READY -> "Ready for on-device chat."
    ModelInferenceStatus.GENERATING -> "Currently answering a chat."
    ModelInferenceStatus.ERROR -> "The local model could not start."
}
