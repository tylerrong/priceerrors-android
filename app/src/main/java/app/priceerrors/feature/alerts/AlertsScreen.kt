package app.priceerrors.feature.alerts

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.priceerrors.ui.theme.NotWorkingRed
import app.priceerrors.ui.theme.SpaceGrotesk

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    notifyAllDeals: Boolean,
    onNotifyAllDealsChanged: (Boolean) -> Unit,
    watches: List<DealWatch>,
    onAddWatch: (DealWatch) -> Unit,
    onRemoveWatch: (String) -> Unit,
    preferredCategories: Set<String>,
    onPreferredCategoriesChanged: (Set<String>) -> Unit,
    alertMinimumDiscount: Int,
    onAlertMinimumDiscountChanged: (Int) -> Unit,
    recentAlerts: List<RecentDealAlert>,
    onRecentAlertSelected: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    notificationsPermissionGranted: Boolean = true,
    onRequestNotificationPermission: () -> Unit = {},
    onAddWatches: (List<DealWatch>) -> Unit = { additions ->
        additions.forEach(onAddWatch)
    },
) {
    var showAddWatch by rememberSaveable { mutableStateOf(false) }
    val accent = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        AlertsHeader(accent = accent)
        NotifyAllDealsCard(
            notifyAllDeals = notifyAllDeals,
            onNotifyAllDealsChanged = onNotifyAllDealsChanged,
            notificationsPermissionGranted = notificationsPermissionGranted,
            onRequestNotificationPermission = onRequestNotificationPermission,
            accent = accent,
        )
        KeywordWatchlistsSection(
            watches = watches,
            accent = accent,
            onAdd = { showAddWatch = true },
            onRemove = onRemoveWatch,
        )
        CategoryWatchlistsSection(
            preferredCategories = preferredCategories,
            notifyAllDeals = notifyAllDeals,
            accent = accent,
            onPreferredCategoriesChanged = onPreferredCategoriesChanged,
        )
        MinimumDiscountCard(
            value = alertMinimumDiscount,
            onValueChanged = onAlertMinimumDiscountChanged,
        )
        RecentAlertsSection(
            recentAlerts = recentAlerts,
            onRecentAlertSelected = onRecentAlertSelected,
        )
        Spacer(Modifier.height(140.dp))
    }

    if (showAddWatch) {
        AddWatchSheet(
            onDismiss = { showAddWatch = false },
            onAdd = { additions ->
                onAddWatches(additions)
                showAddWatch = false
            },
        )
    }
}

@Composable
fun LockedAlertsPreview(
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .blur(8.dp)
                .padding(horizontal = 16.dp)
                .statusBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Alerts.",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                letterSpacing = (-1.2).sp,
                modifier = Modifier.padding(top = 12.dp),
            )
            listOf(
                "AirPods under $100",
                "Nike · 60%+ off",
                "Target price errors",
            ).forEach { label ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = label,
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(checked = true, onCheckedChange = null)
                    }
                }
            }
        }
        Button(
            onClick = onUpgrade,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
            ),
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = "Unlock custom alerts",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun AlertsHeader(accent: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "Alerts",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                letterSpacing = (-1.2).sp,
            )
            Text(
                text = ".",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                letterSpacing = (-1.2).sp,
                color = accent,
            )
        }
        Text(
            text = "Get every deal, then make the matches you care about stand out.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun NotifyAllDealsCard(
    notifyAllDeals: Boolean,
    onNotifyAllDealsChanged: (Boolean) -> Unit,
    notificationsPermissionGranted: Boolean,
    onRequestNotificationPermission: () -> Unit,
    accent: Color,
) {
    val isEnabled = notifyAllDeals && notificationsPermissionGranted
    val onEnabledChange: (Boolean) -> Unit = { enabled ->
        onNotifyAllDealsChanged(enabled)
        if (enabled && !notificationsPermissionGranted) {
            onRequestNotificationPermission()
        }
    }
    Column {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("all_deal_alerts_toggle")
                .toggleable(
                    value = isEnabled,
                    role = Role.Switch,
                    onValueChange = onEnabledChange,
                ),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "All Deal Alerts",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                    Text(
                        text = when {
                            !notificationsPermissionGranted ->
                                "Notifications are off — tap to enable"
                            notifyAllDeals -> "On - get alerts for every deal"
                            else -> "Off — only your specialized alerts will arrive"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                }
                Switch(
                    checked = isEnabled,
                    onCheckedChange = null,
                )
            }
        }
        Text(
            text = "Watchlists never send duplicates—they give matching deals a special alert instead.",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.42f),
        )
    }
}

@Composable
private fun KeywordWatchlistsSection(
    watches: List<DealWatch>,
    accent: Color,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Keyword watchlists",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = "${watches.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onAdd) {
                Text(
                    text = "Add +",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = accent,
                )
            }
        }

        if (watches.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAdd),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(18.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(28.dp),
                    )
                    Text(
                        text = "Create a keyword watchlist",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                    Text(
                        text = "Track any product, brand, store, or keyword.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                watches.forEach { watch ->
                    WatchRow(
                        watch = watch,
                        accent = accent,
                        onRemove = { onRemove(watch.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun WatchRow(
    watch: DealWatch,
    accent: Color,
    onRemove: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(accent.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = watch.query,
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = watch.subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Remove watch",
                    tint = NotWorkingRed.copy(alpha = 0.75f),
                )
            }
        }
    }
}

@Composable
private fun CategoryWatchlistsSection(
    preferredCategories: Set<String>,
    notifyAllDeals: Boolean,
    accent: Color,
    onPreferredCategoriesChanged: (Set<String>) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Category watchlists",
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
        )
        alertCategories.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { category ->
                    val selected = category.name in preferredCategories
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .semantics {
                                role = Role.Checkbox
                                this.selected = selected
                            }
                            .clickable {
                                val next = preferredCategories.toMutableSet()
                                if (selected) next.remove(category.name) else next.add(category.name)
                                onPreferredCategoriesChanged(next)
                            },
                        color = if (selected) accent else MaterialTheme.colorScheme.surface,
                        contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                        shape = RoundedCornerShape(13.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 11.dp, horizontal = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(category.icon, fontSize = 12.sp)
                            Spacer(Modifier.size(4.dp))
                            Text(
                                text = category.name,
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                repeat(3 - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
        Text(
            text = if (notifyAllDeals) {
                "Matches get a category-specific alert; all other qualifying deals still arrive normally."
            } else {
                "Only selected categories and keyword watchlists will alert you."
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
        )
    }
}

@Composable
private fun MinimumDiscountCard(
    value: Int,
    onValueChanged: (Int) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Minimum discount",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
                Text(
                    text = "For all/category alerts; each watch uses its own limit",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
            DiscountStepper(
                value = value,
                onValueChanged = onValueChanged,
                label = "$value%",
            )
        }
    }
}

@Composable
private fun DiscountStepper(
    value: Int,
    onValueChanged: (Int) -> Unit,
    label: String,
    range: IntRange = 0..90,
    step: Int = 10,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = { onValueChanged((value - step).coerceIn(range)) },
            enabled = value > range.first,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(Icons.Filled.Remove, contentDescription = "Decrease")
        }
        Text(
            text = label,
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
        )
        IconButton(
            onClick = { onValueChanged((value + step).coerceIn(range)) },
            enabled = value < range.last,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Increase")
        }
    }
}

@Composable
private fun RecentAlertsSection(
    recentAlerts: List<RecentDealAlert>,
    onRecentAlertSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Recent alerts",
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
        )
        if (recentAlerts.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    text = "Alerts delivered to this device will appear here.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                )
            }
        } else {
            recentAlerts.take(5).forEach { alert ->
                val dealId = alert.dealId
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (dealId != null) {
                                Modifier.clickable { onRecentAlertSelected(dealId) }
                            } else {
                                Modifier
                            },
                        ),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(15.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(
                                text = alert.title,
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                            )
                            Text(
                                text = alert.body,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (dealId != null) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddWatchSheet(
    onDismiss: () -> Unit,
    onAdd: (List<DealWatch>) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var maxPriceText by rememberSaveable { mutableStateOf("") }
    var minimumDiscount by rememberSaveable { mutableIntStateOf(50) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val trimmed = query.trim()
    val canAdd = keywordWatches(trimmed).isNotEmpty()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "New watch",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                letterSpacing = (-0.4).sp,
            )
            Text(
                text = "What do you want to track?",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("AirPods, Nike, Target…") },
                shape = RoundedCornerShape(12.dp),
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            )
            Text(
                text = "Optional limits",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
            TextField(
                value = maxPriceText,
                onValueChange = { maxPriceText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Maximum price") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp),
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "At least $minimumDiscount% off",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f),
                )
                DiscountStepper(
                    value = minimumDiscount,
                    onValueChanged = { minimumDiscount = it },
                    label = "$minimumDiscount%",
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        onAdd(
                            keywordWatches(
                                input = trimmed,
                                maxPrice = maxPriceText.toDoubleOrNull(),
                                minimumDiscount = minimumDiscount.takeIf { it > 0 },
                            ),
                        )
                    },
                    enabled = canAdd,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                    ),
                ) {
                    Text(
                        text = "Add",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                }
            }
        }
    }
}
