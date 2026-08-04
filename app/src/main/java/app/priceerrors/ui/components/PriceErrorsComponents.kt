package app.priceerrors.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.priceerrors.R
import app.priceerrors.core.model.Deal
import app.priceerrors.navigation.MainTab
import app.priceerrors.ui.accessibility.PriceErrorsTestTags
import app.priceerrors.ui.accessibility.rememberAnimationsEnabled
import app.priceerrors.ui.theme.AppDark
import app.priceerrors.ui.theme.Mint
import app.priceerrors.ui.theme.isAppInDarkTheme
import app.priceerrors.ui.theme.NotWorkingRed
import app.priceerrors.ui.theme.SpaceGrotesk
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.random.Random

@Composable
fun PriceErrorsLogo(
    modifier: Modifier = Modifier,
    accent: Color = Mint,
    color: Color = MaterialTheme.colorScheme.onBackground,
) {
    Row(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "PriceErrors"
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier
                .rotate(-6f)
                .size(24.dp)
                .background(accent, RoundedCornerShape(7.dp)),
            contentAlignment = Alignment.Center,
        ) {
            PigMark(tint = Color.White, size = 17.5.dp)
        }

        Text(
            text = "priceerrors",
            color = color,
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            letterSpacing = (-0.7).sp,
        )
    }
}

/**
 * The brand pig, drawn from the shared iOS asset. Rendered as an [Icon] so only
 * the alpha channel is used and [tint] drives the color — the Android
 * equivalent of the template rendering mode iOS applies to the same PNG.
 */
@Composable
fun PigMark(
    tint: Color,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    Icon(
        painter = painterResource(R.drawable.pig),
        contentDescription = null,
        modifier = modifier.size(size),
        tint = tint,
    )
}

@Composable
fun LiveTicker(modifier: Modifier = Modifier) {
    var claimingCount by remember { mutableIntStateOf(2_847) }
    val animationsEnabled = rememberAnimationsEnabled()
    val dotAlpha = if (animationsEnabled) {
        val pulse = rememberInfiniteTransition(label = "tickerPulse")
        val animatedAlpha by pulse.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 700),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "tickerDot",
        )
        animatedAlpha
    } else {
        1f
    }

    LaunchedEffect(animationsEnabled) {
        if (!animationsEnabled) return@LaunchedEffect
        while (true) {
            delay(1_200)
            claimingCount += Random.nextInt(from = 1, until = 5)
        }
    }

    Surface(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = String.format(Locale.US, "%,d people claiming deals", claimingCount)
        },
        color = AppDark.copy(alpha = 0.75f),
        contentColor = Color.White,
        shape = CircleShape,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .alpha(dotAlpha)
                    .background(NotWorkingRed, CircleShape),
            )
            Text(
                text = String.format(Locale.US, "%,d CLAIMING", claimingCount),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                letterSpacing = 0.2.sp,
            )
        }
    }
}

@Composable
fun DealArtwork(
    deal: Deal,
    portrait: Boolean,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    Image(
        painter = painterResource(dealArtworkResource(deal.category, portrait)),
        contentDescription = deal.title,
        modifier = modifier,
        contentScale = contentScale,
    )
}

@DrawableRes
fun dealArtworkResource(category: String, portrait: Boolean): Int {
    val normalizedCategory = category.trim().lowercase()
    return if (portrait) {
        when (normalizedCategory) {
            "amazon" -> R.drawable.amazon_2
            "beauty" -> R.drawable.beauty_2
            "event", "events" -> R.drawable.event_2
            "fashion" -> R.drawable.fashion_2
            "food" -> R.drawable.food_2
            "gaming" -> R.drawable.gaming_2
            "tech" -> R.drawable.tech_2
            "travel" -> R.drawable.travel_2
            else -> R.drawable.deals_2
        }
    } else {
        when (normalizedCategory) {
            "amazon" -> R.drawable.amazon_1
            "beauty" -> R.drawable.beauty_1
            "event", "events" -> R.drawable.event_1
            "fashion" -> R.drawable.fashion_1
            "food" -> R.drawable.food_1
            "gaming" -> R.drawable.gaming_1
            "tech" -> R.drawable.tech_1
            "travel" -> R.drawable.travel_1
            else -> R.drawable.deals_1
        }
    }
}

@Composable
fun DealPill(
    text: String,
    modifier: Modifier = Modifier,
    dark: Boolean = false,
    darkTextColor: Color = Color.White,
) {
    Surface(
        modifier = modifier,
        color = if (dark) AppDark.copy(alpha = 0.86f) else Color.White.copy(alpha = 0.92f),
        contentColor = if (dark) darkTextColor else AppDark,
        shape = RoundedCornerShape(10.dp),
        shadowElevation = if (dark) 0.dp else 1.dp,
    ) {
        Text(
            text = text.uppercase(),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 0.9.sp,
        )
    }
}

/**
 * Soft tinted capsule in the category's own color — lighter than the solid
 * black pill it replaced, and it ties the tag to the category color used on the
 * thumbnail.
 */
@Composable
fun CategoryTag(
    category: String,
    modifier: Modifier = Modifier,
) {
    val darkTheme = isAppInDarkTheme
    val color = dealVisuals(category, darkTheme).accent
    Text(
        text = category.uppercase(),
        modifier = modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.22f), CircleShape)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelSmall,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.8.sp,
        color = legibleOnCard(color, darkTheme),
        maxLines = 1,
    )
}

fun relativeDealTime(postedAt: Instant, now: Instant = Instant.now()): String {
    val elapsedMinutes = Duration.between(postedAt, now).toMinutes().coerceAtLeast(0)
    val zone = ZoneId.systemDefault()
    val postedDateTime = postedAt.atZone(zone)
    val nowDateTime = now.atZone(zone)
    val postedDate = postedDateTime.toLocalDate()
    val today = nowDateTime.toLocalDate()
    val clock = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    return when {
        elapsedMinutes < 1 -> "JUST NOW"
        elapsedMinutes < 60 -> "${elapsedMinutes}M AGO"
        elapsedMinutes < 360 -> "${elapsedMinutes / 60}H AGO"
        postedDate == today -> "TODAY · ${postedDateTime.format(clock)}"
        postedDate == today.minus(1, ChronoUnit.DAYS) -> "YESTERDAY · ${postedDateTime.format(clock)}"
        else -> postedDateTime.format(DateTimeFormatter.ofPattern("MMM d", Locale.US)).uppercase(Locale.US)
    }
}

@Composable
fun FloatingTabBar(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .zIndex(10f)
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            .testTag(PriceErrorsTestTags.MAIN_NAVIGATION),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier
                .semantics(mergeDescendants = true) {
                    role = Role.Tab
                    selected = selectedTab == MainTab.FEED
                    stateDescription = if (selectedTab == MainTab.FEED) "Selected" else "Not selected"
                    contentDescription = "Feed tab"
                }
                .testTag(PriceErrorsTestTags.FEED_TAB),
            onClick = { onTabSelected(MainTab.FEED) },
            color = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            shape = CircleShape,
            shadowElevation = 12.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 15.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .rotate(-6f)
                        .size(24.dp)
                        .background(Color.White, RoundedCornerShape(7.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    PigMark(tint = MaterialTheme.colorScheme.primary, size = 17.5.dp)
                }
                Text(
                    text = "Feed",
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 17.sp,
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Surface(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = CircleShape,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
            ),
            shadowElevation = 12.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FloatingTabIcon(
                    label = "Community",
                    testTag = PriceErrorsTestTags.COMMUNITY_TAB,
                    selected = selectedTab == MainTab.COMMUNITY,
                    onClick = { onTabSelected(MainTab.COMMUNITY) },
                    activeIcon = { Icon(Icons.Filled.People, contentDescription = null) },
                    inactiveIcon = { Icon(Icons.Outlined.People, contentDescription = null) },
                )
                FloatingTabIcon(
                    label = "Browse",
                    testTag = PriceErrorsTestTags.BROWSE_TAB,
                    selected = selectedTab == MainTab.BROWSE,
                    onClick = { onTabSelected(MainTab.BROWSE) },
                    activeIcon = { Icon(Icons.Filled.LocalOffer, contentDescription = null) },
                    inactiveIcon = { Icon(Icons.Outlined.LocalOffer, contentDescription = null) },
                )
                FloatingTabIcon(
                    label = "Profile",
                    testTag = PriceErrorsTestTags.PROFILE_TAB,
                    selected = selectedTab == MainTab.PROFILE,
                    onClick = { onTabSelected(MainTab.PROFILE) },
                    activeIcon = { Icon(Icons.Filled.AccountCircle, contentDescription = null) },
                    inactiveIcon = { Icon(Icons.Outlined.AccountCircle, contentDescription = null) },
                )
            }
        }
    }
}

@Composable
private fun FloatingTabIcon(
    label: String,
    testTag: String,
    selected: Boolean,
    onClick: () -> Unit,
    activeIcon: @Composable () -> Unit,
    inactiveIcon: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .semantics(mergeDescendants = true) {
                role = Role.Tab
                this.selected = selected
                stateDescription = if (selected) "Selected" else "Not selected"
                contentDescription = "$label tab"
            }
            .testTag(testTag),
    ) {
        Box(
            modifier = Modifier.size(21.dp),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                },
            ) {
                if (selected) activeIcon() else inactiveIcon()
            }
        }
    }
}
