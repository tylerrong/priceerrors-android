package app.priceerrors.feature.community

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.priceerrors.ui.theme.AppDark
import kotlinx.coroutines.launch

private sealed interface CommunityDestination {
    data object Feed : CommunityDestination
    data object Post : CommunityDestination
    data class Detail(val dealId: String) : CommunityDestination
}

@Composable
fun CommunityScreen(
    isPro: Boolean,
    currentUserName: String,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier,
    onNestedDestinationChanged: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val moderationStore = remember(context.applicationContext) {
        CommunityModerationStore(context.applicationContext)
    }
    val deals = remember {
        mutableStateListOf<CommunityDeal>().apply { addAll(sampleCommunityDeals()) }
    }
    var destination: CommunityDestination by remember { mutableStateOf(CommunityDestination.Feed) }
    var moderationRevision by remember { mutableIntStateOf(0) }
    var termsAccepted by remember { mutableStateOf(moderationStore.termsAccepted) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val reportedDealIds = remember(moderationRevision) { moderationStore.reportedDealIds }
    val blockedUserKeys = remember(moderationRevision) { moderationStore.blockedUserKeys }
    val pendingReportCount = remember(moderationRevision) { moderationStore.pendingReportCount }
    val visibleDeals = visibleCommunityDeals(deals, reportedDealIds, blockedUserKeys)
    val blockedUsers = blockedUserKeys.map { key ->
        key to (deals.firstOrNull {
            canonicalCommunityUserKey(it.userDisplayName) == key
        }?.userDisplayName ?: key)
    }

    LaunchedEffect(destination) {
        onNestedDestinationChanged(destination != CommunityDestination.Feed)
    }

    BackHandler(enabled = destination != CommunityDestination.Feed) {
        destination = CommunityDestination.Feed
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val current = destination) {
            CommunityDestination.Feed -> CommunityFeed(
                deals = visibleDeals,
                isPro = isPro,
                pendingReportCount = pendingReportCount,
                blockedUsers = blockedUsers,
                onUpgrade = onUpgrade,
                onPost = { destination = CommunityDestination.Post },
                onUnblock = { key ->
                    moderationStore.unblockUser(key)
                    moderationRevision += 1
                    scope.launch { snackbarHostState.showSnackbar("User unblocked on this device.") }
                },
                onDealSelected = { destination = CommunityDestination.Detail(it.id) },
                modifier = modifier,
            )

            CommunityDestination.Post -> PostCommunityDealScreen(
                currentUserName = currentUserName,
                termsAccepted = termsAccepted,
                onTermsAcceptedChanged = { accepted ->
                    termsAccepted = accepted
                    moderationStore.termsAccepted = accepted
                },
                onClose = { destination = CommunityDestination.Feed },
                onPosted = { deal ->
                    deals.add(0, deal)
                    destination = CommunityDestination.Feed
                    scope.launch {
                        snackbarHostState.showSnackbar("Deal posted locally. Server sync is not connected yet.")
                    }
                },
                modifier = modifier,
            )

            is CommunityDestination.Detail -> {
                val deal = deals.firstOrNull { it.id == current.dealId }
                if (deal == null) {
                    destination = CommunityDestination.Feed
                } else {
                    CommunityDealDetailScreen(
                        deal = deal,
                        currentUserName = currentUserName,
                        onBack = { destination = CommunityDestination.Feed },
                        onReport = { reason ->
                            moderationStore.queueReport(deal, reason)
                            moderationRevision += 1
                            destination = CommunityDestination.Feed
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "Post hidden. Report queued locally and not yet sent.",
                                )
                            }
                        },
                        onBlockUser = {
                            moderationStore.blockUser(deal.userDisplayName)
                            moderationRevision += 1
                            destination = CommunityDestination.Feed
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "@${deal.userDisplayName} blocked on this device.",
                                )
                            }
                        },
                        modifier = modifier,
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 112.dp),
        )
    }
}

@Composable
private fun CommunityFeed(
    deals: List<CommunityDeal>,
    isPro: Boolean,
    pendingReportCount: Int,
    blockedUsers: List<Pair<String, String>>,
    onUpgrade: () -> Unit,
    onPost: () -> Unit,
    onUnblock: (String) -> Unit,
    onDealSelected: (CommunityDeal) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showBlockedUsers by remember { mutableStateOf(false) }
    val adminDeals = deals.filter { it.isAdmin }
    val communityDeals = deals.filterNot { it.isAdmin }
    val uriHandler = LocalUriHandler.current
    val subtitle = if (communityDeals.isEmpty()) {
        "Be the first to post a deal."
    } else {
        "${communityDeals.size} live · posted by the community"
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "Community",
                        style = MaterialTheme.typography.displaySmall,
                        fontSize = 32.sp,
                        lineHeight = 34.sp,
                        letterSpacing = (-1.2).sp,
                    )
                    Text(
                        text = ".",
                        style = MaterialTheme.typography.displaySmall,
                        fontSize = 32.sp,
                        lineHeight = 34.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.50f),
                )
            }
        }

        item {
            DiscordBanner(
                onClick = {
                    runCatching { uriHandler.openUri("https://discord.gg/ugKbqEG6hG") }
                },
                modifier = Modifier.padding(horizontal = 14.dp),
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        item {
            PostDealBanner(
                isPro = isPro,
                onClick = if (isPro) onPost else onUpgrade,
                modifier = Modifier.padding(horizontal = 14.dp),
            )
            Spacer(modifier = Modifier.height(18.dp))
        }

        if (pendingReportCount > 0 || blockedUsers.isNotEmpty()) {
            item {
                LocalSafetyStatus(
                    pendingReportCount = pendingReportCount,
                    blockedUserCount = blockedUsers.size,
                    onManage = { showBlockedUsers = true },
                    modifier = Modifier.padding(horizontal = 14.dp),
                )
                Spacer(modifier = Modifier.height(18.dp))
            }
        }

        if (adminDeals.isNotEmpty()) {
            item {
                TeamSectionHeader()
                Spacer(modifier = Modifier.height(10.dp))
            }
            items(adminDeals, key = { it.id }) { deal ->
                CommunityDealRow(
                    deal = deal,
                    isLocked = !isPro,
                    onClick = { if (isPro) onDealSelected(deal) else onUpgrade() },
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                )
            }
            item { Spacer(modifier = Modifier.height(17.dp)) }
        }

        item {
            CommunitySectionHeader()
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (communityDeals.isEmpty()) {
            item {
                EmptyCommunity(isPro = isPro)
            }
        } else {
            items(communityDeals, key = { it.id }) { deal ->
                CommunityDealRow(
                    deal = deal,
                    isLocked = !isPro,
                    onClick = { if (isPro) onDealSelected(deal) else onUpgrade() },
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                )
            }
        }

        item { Spacer(modifier = Modifier.height(140.dp)) }
    }

    if (showBlockedUsers) {
        BlockedUsersDialog(
            blockedUsers = blockedUsers,
            onUnblock = onUnblock,
            onDismiss = { showBlockedUsers = false },
        )
    }
}

@Composable
private fun DiscordBanner(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.SportsEsports,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = Color(0xFF5865F2),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "Join our Discord",
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 15.sp,
                    letterSpacing = (-0.3).sp,
                )
                Text(
                    text = "Get the latest updates, more deal monitors, and hang out with other deal hunters!",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = "Open Discord",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.40f),
            )
        }
    }
}

@Composable
private fun PostDealBanner(
    isPro: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = if (isPro) {
                    "Post your own community deal"
                } else {
                    "Post your own community deal, PriceErrors Pro required"
                }
            }
            .shadow(12.dp, shape)
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary),
                ),
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = if (isPro) "💎" else "🔒",
            fontSize = 36.sp,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "Post your own deal",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontSize = 18.sp,
                lineHeight = 22.sp,
                letterSpacing = (-0.4).sp,
            )
            Text(
                text = if (isPro) {
                    "Share something to the community."
                } else {
                    "Pro feature · tap to upgrade"
                },
                color = Color.White.copy(alpha = 0.90f),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = if (isPro) Icons.Filled.ChevronRight else Icons.Filled.Lock,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = Color.White.copy(alpha = 0.90f),
        )
    }
}

@Composable
private fun LocalSafetyStatus(
    pendingReportCount: Int,
    blockedUserCount: Int,
    onManage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reportLabel = when (pendingReportCount) {
        0 -> null
        1 -> "1 report pending local sync"
        else -> "$pendingReportCount reports pending local sync"
    }
    val blockedLabel = when (blockedUserCount) {
        0 -> null
        1 -> "1 blocked account"
        else -> "$blockedUserCount blocked accounts"
    }
    val status = listOfNotNull(reportLabel, blockedLabel).joinToString(" · ")
    val shape = RoundedCornerShape(16.dp)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(if (blockedUserCount > 0) Modifier.clickable(onClick = onManage) else Modifier)
            .semantics(mergeDescendants = true) {
                contentDescription = if (blockedUserCount > 0) {
                    "$status. Manage blocked accounts"
                } else {
                    "$status. Reports have not been sent"
                }
            },
        shape = shape,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.09f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Block,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "Local safety controls",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                )
                if (pendingReportCount > 0) {
                    Text(
                        text = "Reports are stored on this device and have not been sent.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            if (blockedUserCount > 0) {
                Text(
                    text = "Manage",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun BlockedUsersDialog(
    blockedUsers: List<Pair<String, String>>,
    onUnblock: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Blocked accounts") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Blocks only apply on this device and are not synced to your account.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                )
                blockedUsers.forEach { (key, displayName) ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "@$displayName",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            TextButton(
                                onClick = {
                                    onUnblock(key)
                                    if (blockedUsers.size == 1) onDismiss()
                                },
                                modifier = Modifier.semantics {
                                    contentDescription = "Unblock $displayName on this device"
                                },
                            ) {
                                Text("Unblock")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
    )
}

@Composable
private fun TeamSectionHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Verified,
            contentDescription = null,
            modifier = Modifier.size(15.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Deals by Price Errors Team",
            style = MaterialTheme.typography.labelLarge,
            fontSize = 13.sp,
            letterSpacing = (-0.2).sp,
        )
    }
}

@Composable
private fun CommunitySectionHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Groups,
            contentDescription = null,
            modifier = Modifier.size(15.dp),
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
        )
        Text(
            text = "Deals by the community",
            style = MaterialTheme.typography.labelLarge,
            fontSize = 13.sp,
            letterSpacing = (-0.2).sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.70f),
        )
    }
}

@Composable
private fun CommunityDealRow(
    deal: CommunityDeal,
    isLocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visual = deal.categoryVisual
    Box(modifier = modifier.fillMaxWidth()) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    contentDescription = buildString {
                        append(deal.title)
                        append(", ")
                        append(deal.priceLabel)
                        append(", posted by ")
                        append(deal.userDisplayName)
                        if (isLocked) append(", PriceErrors Pro required")
                    }
                }
                .shadow(if (deal.isAdmin) 6.dp else 4.dp, RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = if (deal.isAdmin) {
                BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
            } else {
                null
            },
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .then(
                            if (deal.isAdmin) {
                                Modifier.background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                                        ),
                                    ),
                                )
                            } else {
                                Modifier.background(visual.color.copy(alpha = 0.15f))
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = visual.emoji, fontSize = 36.sp)
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        CommunityPill(
                            text = deal.category,
                            background = AppDark,
                            foreground = MaterialTheme.colorScheme.tertiary,
                        )
                        if (deal.isAdmin) {
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = "Verified Price Errors team deal",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Text(
                        text = deal.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 15.sp,
                        letterSpacing = (-0.3).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = deal.brand?.uppercase() ?: if (deal.isAdmin) "PRICE ERRORS" else "COMMUNITY",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                        letterSpacing = 0.5.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.50f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = relativeCommunityTime(deal.createdAtMillis),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                    )
                    Text(
                        text = deal.priceLabel,
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 20.sp,
                        lineHeight = 24.sp,
                        letterSpacing = (-0.5).sp,
                    )
                }
            }
        }

        if (isLocked) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Pro deal",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp).size(12.dp),
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun CommunityPill(
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

@Composable
private fun EmptyCommunity(isPro: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(text = "🛒", fontSize = 44.sp)
        Text(
            text = "No community posts yet",
            style = MaterialTheme.typography.titleLarge,
            fontSize = 16.sp,
            lineHeight = 20.sp,
        )
        Text(
            text = if (isPro) {
                "Tap the banner above to post the first one."
            } else {
                "Upgrade to Pro to post and browse deals."
            },
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.50f),
            textAlign = TextAlign.Center,
        )
    }
}
