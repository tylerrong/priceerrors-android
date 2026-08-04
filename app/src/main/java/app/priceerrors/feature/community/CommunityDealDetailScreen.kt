package app.priceerrors.feature.community

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.priceerrors.ui.components.CategoryTag

@Composable
internal fun CommunityDealDetailScreen(
    deal: CommunityDeal,
    currentUserName: String,
    onBack: () -> Unit,
    onReport: (CommunityReportReason) -> Unit,
    onBlockUser: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val normalizedLink = remember(deal.url) { normalizeCommunityLink(deal.url) }
    var linkError by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var selectedReportReason by remember { mutableStateOf<CommunityReportReason?>(null) }
    var showBlockDialog by remember { mutableStateOf(false) }
    val canModerate = !deal.isAdmin &&
        canonicalCommunityUserKey(deal.userDisplayName) != canonicalCommunityUserKey(currentUserName)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(104.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 60.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CategoryTag(category = deal.category)
                    DetailPill(
                        text = "COMMUNITY",
                        background = MaterialTheme.colorScheme.primary,
                        foreground = Color.White,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = relativeCommunityTime(deal.createdAtMillis),
                        style = MaterialTheme.typography.labelMedium,
                        fontSize = 11.sp,
                        letterSpacing = 0.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.40f),
                    )
                }

                Text(
                    text = deal.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontSize = 28.sp,
                    lineHeight = 32.sp,
                    letterSpacing = (-0.7).sp,
                )

                Text(
                    text = deal.priceLabel,
                    style = MaterialTheme.typography.headlineLarge,
                    fontSize = 34.sp,
                    lineHeight = 38.sp,
                    letterSpacing = (-1).sp,
                )

                if (!deal.brand.isNullOrBlank()) {
                    Text(
                        text = deal.brand.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.50f),
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f),
                )

                Text(
                    text = deal.description,
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                )

                Text(
                    text = "Posted by @${deal.userDisplayName}",
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.50f),
                )

                if (canModerate) {
                    HorizontalDivider(
                        modifier = Modifier.padding(top = 6.dp),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f),
                    )
                    Text(
                        text = "SAFETY",
                        style = MaterialTheme.typography.labelMedium,
                        fontSize = 11.sp,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.50f),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = { showReportDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .semantics { contentDescription = "Report this community deal" },
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Flag,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp),
                            )
                            Spacer(modifier = Modifier.size(7.dp))
                            Text("Report")
                        }
                        OutlinedButton(
                            onClick = { showBlockDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .semantics {
                                    contentDescription = "Block ${deal.userDisplayName} on this device"
                                },
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Block,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp),
                            )
                            Spacer(modifier = Modifier.size(7.dp))
                            Text("Block user")
                        }
                    }
                }

                if (normalizedLink != null) {
                    Text(
                        text = "External community links are unverified. Check the destination before entering personal or payment information.",
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.52f),
                    )
                    Button(
                        onClick = {
                            linkError = runCatching { uriHandler.openUri(normalizedLink) }.isFailure
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .height(58.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White,
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                    ) {
                        Text(
                            text = "OPEN LINK",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 15.sp,
                            letterSpacing = 0.5.sp,
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    if (linkError) {
                        Text(
                            text = "Couldn't open this link.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }

        Surface(
            onClick = onBack,
            modifier = Modifier
                .padding(start = 16.dp, top = 12.dp)
                .size(40.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Report this deal?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Choose a reason. The post will be hidden immediately on this device.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    CommunityReportReason.entries.forEach { reason ->
                        Surface(
                            onClick = { selectedReportReason = reason },
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics(mergeDescendants = true) {
                                    contentDescription = "Report reason: ${reason.label}"
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedReportReason == reason) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                            } else {
                                Color.Transparent
                            },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = selectedReportReason == reason,
                                    onClick = null,
                                )
                                Text(
                                    text = reason.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                    Text(
                        text = "Pending local sync: this report is saved on this device and has not been sent to Price Errors staff.",
                        modifier = Modifier.padding(top = 6.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedReportReason?.let(onReport)
                        showReportDialog = false
                    },
                    enabled = selectedReportReason != null,
                ) {
                    Text("Hide & queue report")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            title = { Text("Block @${deal.userDisplayName}?") },
            text = {
                Text(
                    text = "All posts from this user will be hidden on this device. This block is local and is not synced to your account.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBlockDialog = false
                        onBlockUser()
                    },
                ) {
                    Text("Block user", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun DetailPill(
    text: String,
    background: Color,
    foreground: Color,
) {
    Text(
        text = text.uppercase(),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelSmall,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.3.sp,
        color = foreground,
        maxLines = 1,
    )
}
