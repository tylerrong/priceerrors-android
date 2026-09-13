package app.priceerrors.feature.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.priceerrors.R
import app.priceerrors.core.model.Deal
import app.priceerrors.ui.components.DealArtwork
import app.priceerrors.ui.components.PigMark
import app.priceerrors.ui.components.dealAccessibilityLabel
import app.priceerrors.ui.components.dealVisuals
import app.priceerrors.ui.components.formatPrice
import app.priceerrors.ui.theme.AppDark
import app.priceerrors.ui.theme.NotWorkingRed
import app.priceerrors.ui.theme.SpaceGrotesk
import app.priceerrors.ui.accessibility.PriceErrorsTestTags
import java.text.NumberFormat
import java.util.Locale

/**
 * Self-contained visual port of the iOS profile surface.
 *
 * The supplied values seed local UI state so the feature can be dropped into
 * the tab shell before account/settings repositories are connected. Every
 * mutation is also surfaced through a callback for straightforward wiring.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    name: String,
    email: String,
    isPro: Boolean,
    dayStreak: Int,
    savedDeals: List<Deal>,
    modifier: Modifier = Modifier,
    myPosts: List<ProfilePost> = emptyList(),
    pushAlertsEnabled: Boolean = true,
    selectedPalette: ProfilePalette = ProfilePalette.MINT,
    selectedFeedLayout: ProfileFeedLayout = ProfileFeedLayout.SWIPE,
    selectedAppearance: ProfileAppearance = ProfileAppearance.SYSTEM,
    onTabSelected: (ProfileTab) -> Unit = {},
    onEditName: (String) -> Unit = {},
    onManageSubscription: () -> Unit = {},
    onUpgrade: () -> Unit = {},
    onDealSelected: (String) -> Unit = {},
    onDeletePost: (String) -> Unit = {},
    onPushAlertsChanged: (Boolean) -> Unit = {},
    onPaletteSelected: (ProfilePalette) -> Unit = {},
    onFeedLayoutSelected: (ProfileFeedLayout) -> Unit = {},
    onAppearanceSelected: (ProfileAppearance) -> Unit = {},
    onSignOut: () -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    onOpenSupport: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onOpenTerms: () -> Unit = {},
    onOpenAccountDeletion: () -> Unit = {},
    onOpenDiscord: () -> Unit = {},
    showNavigation: Boolean = true,
    monthSavings: Double = 0.0,
    lifetimeSavings: Double = 0.0,
    onOpenAlertSettings: () -> Unit = { onTabSelected(ProfileTab.ALERTS) },
) {
    var displayedName by rememberSaveable(name) { mutableStateOf(name) }
    var editNameVisible by rememberSaveable { mutableStateOf(false) }
    var informationSheet by remember { mutableStateOf<ProfileSheet?>(null) }
    var signOutConfirmationVisible by rememberSaveable { mutableStateOf(false) }
    var palette by remember(selectedPalette) { mutableStateOf(selectedPalette) }
    var feedLayout by remember(selectedFeedLayout) { mutableStateOf(selectedFeedLayout) }
    var appearance by remember(selectedAppearance) { mutableStateOf(selectedAppearance) }
    @Suppress("UNUSED_VARIABLE")
    val unusedMyPosts = myPosts
    @Suppress("UNUSED_VARIABLE")
    val unusedOnDeletePost = onDeletePost
    @Suppress("UNUSED_VARIABLE")
    val unusedDayStreak = dayStreak
    @Suppress("UNUSED_VARIABLE")
    val unusedPushAlertsEnabled = pushAlertsEnabled
    @Suppress("UNUSED_VARIABLE")
    val unusedOnPushAlertsChanged = onPushAlertsChanged

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag(PriceErrorsTestTags.PROFILE_SCREEN),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 152.dp),
        ) {
            item { ProfileHeading(accent = palette.primary) }
            item {
                SavingsHero(
                    monthSavings = monthSavings,
                    lifetimeSavings = lifetimeSavings,
                    palette = palette,
                )
            }
            item {
                IdentityCard(
                    name = displayedName,
                    email = email,
                    palette = palette,
                    onEdit = { editNameVisible = true },
                )
            }
            item {
                ProCard(
                    isPro = isPro,
                    palette = palette,
                    onManage = onManageSubscription,
                    onUpgrade = onUpgrade,
                )
            }
            item {
                DiscordJoinCard(
                    accent = palette.primary,
                    onOpenDiscord = onOpenDiscord,
                )
            }
            item {
                SectionHeading(
                    title = "Saved",
                    trailing = "${savedDeals.size} ${if (savedDeals.size == 1) "deal" else "deals"}",
                    accent = palette.primary,
                )
            }
            if (savedDeals.isEmpty()) {
                item { EmptySectionCopy("Tap ♥ on any deal to save it here.") }
            } else {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(savedDeals, key = { it.id }) { deal ->
                            SavedDealCard(
                                deal = deal,
                                onClick = { onDealSelected(deal.id) },
                            )
                        }
                    }
                    Spacer(Modifier.height(22.dp))
                }
            }
            item {
                PreferenceCard(
                    palette = palette,
                    feedLayout = feedLayout,
                    appearance = appearance,
                    onPaletteSelected = {
                        palette = it
                        onPaletteSelected(it)
                    },
                    onFeedLayoutSelected = {
                        feedLayout = it
                        onFeedLayoutSelected(it)
                    },
                    onAppearanceSelected = {
                        appearance = it
                        onAppearanceSelected(it)
                    },
                )
            }
            item {
                SettingsCard(
                    accent = palette.primary,
                    onAlertSettings = onOpenAlertSettings,
                    onHelp = { informationSheet = ProfileSheet.HELP },
                    onPrivacy = { informationSheet = ProfileSheet.PRIVACY },
                    onTerms = { informationSheet = ProfileSheet.TERMS },
                    onSignOut = { signOutConfirmationVisible = true },
                )
            }
        }

        if (showNavigation) {
            ProfileFloatingTabBar(
                accent = palette.primary,
                onTabSelected = onTabSelected,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    if (editNameVisible) {
        EditNameSheet(
            currentName = displayedName,
            onDismiss = { editNameVisible = false },
            onSave = { updatedName ->
                displayedName = updatedName
                editNameVisible = false
                onEditName(updatedName)
            },
        )
    }

    informationSheet?.let { sheet ->
        ProfileInformationSheet(
            sheet = sheet,
            accent = palette.primary,
            onDismiss = { informationSheet = null },
            onDeleteAccount = onDeleteAccount,
            onOpenSupport = onOpenSupport,
            onOpenPrivacy = onOpenPrivacy,
            onOpenTerms = onOpenTerms,
            onOpenAccountDeletion = onOpenAccountDeletion,
        )
    }

    if (signOutConfirmationVisible) {
        AlertDialog(
            onDismissRequest = { signOutConfirmationVisible = false },
            title = { Text("Sign out?") },
            text = { Text("Your account data and saved deals remain synced until you delete your account.") },
            confirmButton = {
                TextButton(onClick = {
                    signOutConfirmationVisible = false
                    onSignOut()
                }) {
                    Text("Sign out", color = NotWorkingRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { signOutConfirmationVisible = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun ProfileHeading(accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 18.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = "You",
            style = MaterialTheme.typography.displaySmall,
        )
        Text(
            text = ".",
            style = MaterialTheme.typography.displaySmall,
            color = accent,
        )
    }
}

@Composable
private fun IdentityCard(
    name: String,
    email: String,
    palette: ProfilePalette,
    onEdit: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(24.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        brush = Brush.linearGradient(listOf(palette.primary, palette.secondary)),
                        shape = RoundedCornerShape(16.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = profileInitials(name),
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = Color.White,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    letterSpacing = (-0.3).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = email.ifBlank { "Signed in with Google" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.50f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                onClick = onEdit,
                modifier = Modifier
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), CircleShape),
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Edit display name",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f),
                )
            }
        }
    }
}

@Composable
private fun ProCard(
    isPro: Boolean,
    palette: ProfilePalette,
    onManage: () -> Unit,
    onUpgrade: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 4.dp)
            .clickable(onClick = if (isPro) onManage else onUpgrade),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(20.dp),
        border = if (isPro) null else BorderStroke(1.5.dp, palette.primary.copy(alpha = 0.50f)),
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        brush = Brush.linearGradient(listOf(palette.primary, palette.secondary)),
                        shape = RoundedCornerShape(14.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (isPro) {
                    CrownMark(Modifier.size(24.dp))
                } else {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color.White,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isPro) "PriceErrors Pro" else "Go Pro",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    letterSpacing = (-0.3).sp,
                )
                Text(
                    text = if (isPro) {
                        "Unlimited deals + custom alerts"
                    } else {
                        "Unlock every deal and personalized alert"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                )
            }
            if (isPro) {
                Surface(
                    color = palette.primary.copy(alpha = 0.10f),
                    contentColor = palette.primary,
                    shape = CircleShape,
                ) {
                    Text(
                        text = "Manage",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        letterSpacing = 0.5.sp,
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.48f),
                )
            }
        }
    }
}

@Composable
private fun CrownMark(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val crown = Path().apply {
            moveTo(size.width * 0.08f, size.height * 0.27f)
            lineTo(size.width * 0.30f, size.height * 0.52f)
            lineTo(size.width * 0.50f, size.height * 0.18f)
            lineTo(size.width * 0.70f, size.height * 0.52f)
            lineTo(size.width * 0.92f, size.height * 0.27f)
            lineTo(size.width * 0.80f, size.height * 0.72f)
            lineTo(size.width * 0.20f, size.height * 0.72f)
            close()
        }
        drawPath(crown, Color.White)
        drawRoundRect(
            color = Color.White,
            topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.20f, size.height * 0.77f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.60f, size.height * 0.12f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height * 0.05f),
        )
    }
}

@Composable
private fun SavingsHero(
    monthSavings: Double,
    lifetimeSavings: Double,
    palette: ProfilePalette,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .padding(bottom = 8.dp),
        color = Color.Transparent,
        contentColor = Color.White,
        shape = RoundedCornerShape(26.dp),
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(palette.primary, palette.secondary)))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                text = "YOUR SAVINGS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.64f),
                letterSpacing = 1.1.sp,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = formatSavings(monthSavings),
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 42.sp,
                    letterSpacing = (-1.7).sp,
                    maxLines = 1,
                )
                Text(
                    text = "saved this month",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.68f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = formatSavings(lifetimeSavings),
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 21.sp,
                        maxLines = 1,
                    )
                    Text(
                        text = "LIFETIME SAVINGS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.55f),
                        letterSpacing = 0.8.sp,
                    )
                }
                Spacer(Modifier.weight(1f))
                if (lifetimeSavings == 0.0) {
                    Text(
                        text = "Confirm your first deal\nto start tracking",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.55f),
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeading(
    title: String,
    trailing: String,
    accent: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            letterSpacing = (-0.4).sp,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = trailing,
            style = MaterialTheme.typography.labelMedium,
            color = accent,
        )
    }
}

@Composable
private fun EmptySectionCopy(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, bottom = 22.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.42f),
    )
}

@Composable
private fun DiscordJoinCard(
    accent: Color,
    onOpenDiscord: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .clickable(
                role = Role.Button,
                onClick = onOpenDiscord,
            ),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(accent.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.discord_symbol_blurple),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(28.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Join our Discord",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
                Text(
                    text = "Get updates and talk deals with other members.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun SavedDealCard(
    deal: Deal,
    onClick: () -> Unit,
) {
    val visuals = dealVisuals(deal.category, isSystemInDarkTheme())
    Column(
        modifier = Modifier
            .width(130.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(visuals.background)
            .semantics(mergeDescendants = true) {
                contentDescription = dealAccessibilityLabel(deal)
            }
            .testTag("saved_deal_${deal.id}")
            .clickable(onClick = onClick),
    ) {
        DealArtwork(
            deal = deal,
            portrait = false,
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .padding(6.dp)
                .clip(RoundedCornerShape(10.dp)),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(start = 10.dp, top = 6.dp, end = 10.dp, bottom = 10.dp),
        ) {
            Text(
                text = deal.title,
                modifier = Modifier.height(30.dp),
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatPrice(deal.priceInCents, deal.currencyCode),
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun SettingsCard(
    accent: Color,
    onAlertSettings: () -> Unit,
    onHelp: () -> Unit,
    onPrivacy: () -> Unit,
    onTerms: () -> Unit,
    onSignOut: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
    ) {
        Column {
            SettingRow(
                icon = Icons.Filled.Notifications,
                title = "Alert settings",
                iconTint = accent,
                onClick = onAlertSettings,
            )
            SettingsDivider()
            SettingRow(
                icon = Icons.AutoMirrored.Filled.Help,
                title = "Help & support",
                iconTint = accent,
                onClick = onHelp,
            )
            SettingsDivider()
            SettingRow(
                icon = Icons.Filled.Lock,
                title = "Privacy Policy",
                iconTint = accent,
                onClick = onPrivacy,
            )
            SettingsDivider()
            SettingRow(
                icon = Icons.Filled.Description,
                title = "Terms of Service",
                iconTint = accent,
                onClick = onTerms,
            )
            SettingsDivider()
            SettingRow(
                icon = Icons.AutoMirrored.Filled.Logout,
                title = "Sign out",
                iconTint = NotWorkingRed,
                titleColor = NotWorkingRed,
                showChevron = false,
                onClick = onSignOut,
            )
        }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    iconTint: Color,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    showChevron: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = iconTint,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            color = titleColor,
        )
        when {
            trailing != null -> trailing()
            showChevron -> Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.30f),
            )
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 48.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
    )
}

@Composable
private fun PreferenceCard(
    palette: ProfilePalette,
    feedLayout: ProfileFeedLayout,
    appearance: ProfileAppearance,
    onPaletteSelected: (ProfilePalette) -> Unit,
    onFeedLayoutSelected: (ProfileFeedLayout) -> Unit,
    onAppearanceSelected: (ProfileAppearance) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, top = 12.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SelectorLabel("THEME")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ProfilePalette.entries.forEach { option ->
                    val shape = RoundedCornerShape(10.dp)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(shape)
                            .background(Brush.linearGradient(listOf(option.primary, option.secondary)))
                            .then(
                                if (option == palette) {
                                    Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, shape)
                                } else {
                                    Modifier
                                },
                            )
                            .semantics(mergeDescendants = true) {
                                role = Role.RadioButton
                                selected = option == palette
                                contentDescription = "${option.label} color theme"
                                stateDescription = if (option == palette) "Selected" else "Not selected"
                            }
                            .testTag("palette_${option.name.lowercase()}")
                            .clickable(role = Role.RadioButton) { onPaletteSelected(option) },
                    )
                }
            }

            SelectorLabel("FEED LAYOUT", Modifier.padding(top = 16.dp))
            SegmentedOptions(
                values = ProfileFeedLayout.entries,
                selected = feedLayout,
                label = ProfileFeedLayout::label,
                onSelected = onFeedLayoutSelected,
            )

            SelectorLabel("APPEARANCE", Modifier.padding(top = 16.dp))
            SegmentedOptions(
                values = ProfileAppearance.entries,
                selected = appearance,
                label = ProfileAppearance::label,
                onSelected = onAppearanceSelected,
            )
        }
    }
}

@Composable
private fun SelectorLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier.padding(horizontal = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f),
        letterSpacing = 1.sp,
    )
}

@Composable
private fun <T> SegmentedOptions(
    values: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        values.forEach { option ->
            val active = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .semantics(mergeDescendants = true) {
                        role = Role.RadioButton
                        this.selected = active
                        contentDescription = label(option)
                        stateDescription = if (active) "Selected" else "Not selected"
                    }
                    .testTag("option_${label(option).lowercase()}")
                    .clickable(role = Role.RadioButton) { onSelected(option) },
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    color = if (active) AppDark else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    contentColor = if (active) Color.White else MaterialTheme.colorScheme.onSurface,
                    shape = RoundedCornerShape(9.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = label(option),
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileFloatingTabBar(
    accent: Color,
    onTabSelected: (ProfileTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            .testTag(PriceErrorsTestTags.MAIN_NAVIGATION),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            modifier = Modifier
                .semantics(mergeDescendants = true) {
                    role = Role.Tab
                    selected = false
                    stateDescription = "Not selected"
                    contentDescription = "Feed tab"
                }
                .testTag(PriceErrorsTestTags.FEED_TAB)
                .clickable(role = Role.Tab) { onTabSelected(ProfileTab.FEED) },
            color = accent,
            contentColor = Color.White,
            shape = CircleShape,
            shadowElevation = 12.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .rotate(-6f)
                        .size(24.dp)
                        .background(Color.White, RoundedCornerShape(7.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    PigMark(tint = accent, size = 16.dp)
                }
                Text(
                    text = "Feed",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    letterSpacing = (-0.3).sp,
                )
            }
        }

        Surface(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = CircleShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
            shadowElevation = 12.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProfileTabIcon(
                    icon = Icons.Filled.Notifications,
                    label = "Alerts",
                    selected = false,
                    testTag = PriceErrorsTestTags.ALERTS_TAB,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    onClick = { onTabSelected(ProfileTab.ALERTS) },
                    modifier = Modifier.weight(1f),
                )
                ProfileTabIcon(
                    icon = Icons.Outlined.LocalOffer,
                    label = "Browse",
                    selected = false,
                    testTag = PriceErrorsTestTags.BROWSE_TAB,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    onClick = { onTabSelected(ProfileTab.BROWSE) },
                    modifier = Modifier.weight(1f),
                )
                ProfileTabIcon(
                    icon = Icons.Filled.AccountCircle,
                    label = "You",
                    selected = true,
                    testTag = PriceErrorsTestTags.PROFILE_TAB,
                    tint = accent,
                    onClick = { onTabSelected(ProfileTab.PROFILE) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ProfileTabIcon(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    testTag: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .height(48.dp)
            .semantics(mergeDescendants = true) {
                role = Role.Tab
                this.selected = selected
                stateDescription = if (selected) "Selected" else "Not selected"
                contentDescription = "$label tab"
            }
            .testTag(testTag),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(25.dp),
            tint = tint,
        )
    }
}

internal fun profileInitials(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter(String::isNotBlank)
    return when {
        parts.size >= 2 -> "${parts.first().first()}${parts.last().first()}".uppercase()
        parts.size == 1 -> parts.first().take(2).uppercase()
        else -> "ME"
    }
}

internal fun formatSavings(amount: Double): String =
    NumberFormat.getCurrencyInstance(Locale.US).format(amount.coerceAtLeast(0.0))
