package com.swasthai.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swasthai.app.domain.model.ModelInferenceStatus
import com.swasthai.app.ui.theme.AppleBlueLight
import com.swasthai.app.ui.theme.AppleGreenLight
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
import com.swasthai.app.ui.theme.AppleTealLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    modelStatus: ModelInferenceStatus,
    onStatusClick: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth().background(AppleSystemBackground)) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppleLabelPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Apple-style privacy pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppleTealLight)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Air-Gapped",
                            tint = AppleHealthTeal,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "100% On-Device",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppleHealthTeal
                        )
                    }
                }
            },
            actions = {
                val (pillBg, dotColor, statusText) = when (modelStatus) {
                    ModelInferenceStatus.READY -> Triple(AppleGreenLight, AppleSystemGreen, "Assistant Ready")
                    ModelInferenceStatus.GENERATING -> Triple(AppleBlueLight, AppleSystemBlue, "Consulting...")
                    ModelInferenceStatus.INITIALIZING -> Triple(AppleOrangeLight, AppleSystemOrange, "Starting AI...")
                    ModelInferenceStatus.NOT_INSTALLED -> Triple(AppleSearchFill, AppleLabelTertiary, "Setup AI")
                    ModelInferenceStatus.ERROR -> Triple(AppleOrangeLight, AppleSystemOrange, "Tap to Setup")
                }

                Row(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(pillBg)
                        .clickable { onStatusClick() }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = statusText,
                        fontSize = 12.sp,
                        color = dotColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = AppleSystemBackground,
                titleContentColor = AppleLabelPrimary
            )
        )
        // Hairline separator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(AppleSeparator)
        )
    }
}
