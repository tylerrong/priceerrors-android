package app.priceerrors.feature.detail

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.priceerrors.R
import app.priceerrors.core.analytics.GrowthAnalytics
import app.priceerrors.core.model.Deal
import app.priceerrors.core.model.DealVote
import app.priceerrors.core.sharing.DealSharing
import app.priceerrors.ui.components.DealArtwork
import app.priceerrors.ui.components.dealVisuals
import app.priceerrors.ui.components.formatPrice
import app.priceerrors.ui.components.relativeDealTime
import app.priceerrors.ui.accessibility.PriceErrorsTestTags
import app.priceerrors.ui.accessibility.rememberAnimationsEnabled
import app.priceerrors.ui.theme.AppDark
import app.priceerrors.ui.theme.NotWorkingRed
import app.priceerrors.ui.theme.SavedPink
import app.priceerrors.ui.theme.SpaceGrotesk
import app.priceerrors.ui.theme.WorkingGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import java.util.Random
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Full deal detail screen. Durable user state is owned by the caller so saves,
 * votes, and claims survive navigation and can be synchronized with the API.
 *
 * Defaults keep the screen source-compatible while the app shell is being
 * migrated; production callers should pass all state and callbacks.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DealDetailScreen(
    deal: Deal,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    isSaved: Boolean = false,
    isClaimed: Boolean = false,
    selectedVote: DealVote? = deal.userVote,
    onSave: () -> Unit = {},
    onVote: (DealVote) -> Unit = {},
    onClaim: () -> Unit = {},
    growthAnalytics: GrowthAnalytics? = null,
    showClaimConfirmation: Boolean = false,
    claimSavings: Double = 0.0,
    isConfirmingClaim: Boolean = false,
    onConfirmClaim: () -> Unit = {},
    onDeclineClaim: () -> Unit = {},
    celebrationSavings: Double? = null,
    onCelebrationFinished: () -> Unit = {},
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val animationsEnabled = rememberAnimationsEnabled()
    val dragOffset = remember { Animatable(0f) }
    val swipeThresholdPx = with(density) { 100.dp.toPx() }
    var showCelebration by remember(deal.id) { mutableStateOf(false) }
    var displayedSavings by remember(deal.id) { mutableStateOf(0.0) }

    LaunchedEffect(celebrationSavings) {
        celebrationSavings?.let {
            displayedSavings = it
            if (animationsEnabled) {
                showCelebration = true
            } else {
                showCelebration = false
                onCelebrationFinished()
            }
        }
    }

    val windowHeight = with(density) { LocalWindowInfo.current.containerSize.height.toDp() }
    val heroHeight = (windowHeight * 0.42f).coerceIn(320.dp, 440.dp)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag(PriceErrorsTestTags.DEAL_DETAIL)
            .offset { IntOffset(dragOffset.value.roundToInt(), 0) }
            .pointerInput(onBack, swipeThresholdPx) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount ->
                        scope.launch {
                            dragOffset.snapTo((dragOffset.value + dragAmount).coerceAtLeast(0f))
                        }
                    },
                    onDragEnd = {
                        if (dragOffset.value > swipeThresholdPx) onBack()
                        scope.launch {
                            if (animationsEnabled) {
                                dragOffset.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 500f),
                                )
                            } else {
                                dragOffset.snapTo(0f)
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            if (animationsEnabled) {
                                dragOffset.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 500f),
                                )
                            } else {
                                dragOffset.snapTo(0f)
                            }
                        }
                    },
                )
            },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            DealHero(
                deal = deal,
                heroHeight = heroHeight,
                saved = isSaved,
                onBack = onBack,
                onSave = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSave()
                },
                onShare = {
                    DealSharing.presentShareSheet(
                        context = context,
                        deal = deal,
                        source = "deal_detail",
                        analytics = growthAnalytics,
                    )
                },
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 140.dp),
            ) {
                item {
                    DealBody(
                        deal = deal,
                        selectedVote = selectedVote,
                        onShare = {
                            DealSharing.presentShareSheet(
                                context = context,
                                deal = deal,
                                source = "deal_detail",
                                analytics = growthAnalytics,
                            )
                        },
                        onVote = { vote ->
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onVote(vote)
                        },
                    )
                }
            }
        }

        ClaimBar(
            claimed = isClaimed,
            hasDealLink = !deal.dealUrl.isNullOrBlank(),
            onClaim = {
                deal.dealUrl?.takeIf(String::isNotBlank)?.let { url ->
                    if (!isClaimed) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClaim()
                    }
                    uriHandler.openUri(url)
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        if (showCelebration) {
            ConfettiOverlay(
                key = deal.id,
                savings = displayedSavings,
                onFinished = {
                    showCelebration = false
                    onCelebrationFinished()
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    if (showClaimConfirmation) {
        ModalBottomSheet(
            onDismissRequest = {},
            sheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = true,
                confirmValueChange = { target -> target != SheetValue.Hidden },
            ),
            containerColor = MaterialTheme.colorScheme.background,
            dragHandle = null,
        ) {
            ClaimConfirmationSheet(
                isConfirming = isConfirmingClaim,
                onConfirm = onConfirmClaim,
                onDecline = onDeclineClaim,
            )
        }
    }
}

@Composable
private fun DealHero(
    deal: Deal,
    heroHeight: Dp,
    saved: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
) {
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val visuals = dealVisuals(deal.category, darkTheme)
    val imageHeight = heroHeight * 0.74f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(heroHeight)
            .background(
                color = visuals.background,
                shape = RoundedCornerShape(bottomStart = 34.dp, bottomEnd = 34.dp),
            ),
    ) {
        DealArtwork(
            deal = deal,
            portrait = false,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(imageHeight)
                .padding(start = 14.dp, top = 60.dp, end = 14.dp)
                .clip(RoundedCornerShape(20.dp)),
            contentScale = ContentScale.Fit,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleChromeButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            CircleChromeButton(
                onClick = onSave,
                modifier = Modifier.testTag(PriceErrorsTestTags.SAVE_DEAL),
            ) {
                Icon(
                    imageVector = if (saved) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (saved) "Remove saved deal" else "Save deal",
                    modifier = Modifier.size(19.dp),
                    tint = if (saved) SavedPink else MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            CircleChromeButton(onClick = onShare) {
                Icon(
                    Icons.Filled.Share,
                    contentDescription = "Share deal",
                    modifier = Modifier.size(19.dp),
                )
            }
        }
    }
}

@Composable
private fun DetailPill(
    text: String,
    background: Color,
    foreground: Color,
) {
    Surface(
        color = background,
        contentColor = foreground,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = if (background.luminance() > 0.5f) 2.dp else 0.dp,
    ) {
        Text(
            text = text.uppercase(),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            lineHeight = 15.sp,
            letterSpacing = 0.9.sp,
        )
    }
}

@Composable
private fun CircleChromeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.size(48.dp),
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = CircleShape,
        shadowElevation = 6.dp,
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
private fun DealBody(
    deal: Deal,
    selectedVote: DealVote?,
    onShare: () -> Unit,
    onVote: (DealVote) -> Unit,
) {
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val visuals = dealVisuals(deal.category, darkTheme)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        Text(
            text = listOf(deal.brand, deal.store)
                .distinctBy { it.trim().lowercase() }
                .joinToString(" · ")
                .uppercase(),
            modifier = Modifier.padding(top = 20.dp),
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            color = visuals.accent,
        )
        Text(
            text = deal.title,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 14.dp),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = formatPrice(deal.priceInCents, deal.currencyCode),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (deal.originalPriceInCents > deal.priceInCents) {
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = formatPrice(deal.originalPriceInCents, deal.currencyCode),
                    modifier = Modifier.padding(bottom = 8.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.40f),
                    textDecoration = TextDecoration.LineThrough,
                )
            }
        }

        Text(
            text = relativeDealTime(deal.postedAt),
            modifier = Modifier.padding(top = 8.dp),
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.42f),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            VoteButton(
                label = "Working",
                count = deal.workingCount,
                selected = selectedVote == DealVote.WORKING,
                color = WorkingGreen,
                working = true,
                onClick = { onVote(DealVote.WORKING) },
                modifier = Modifier.weight(1f),
            )
            VoteButton(
                label = "Not Working",
                count = deal.notWorkingCount,
                selected = selectedVote == DealVote.NOT_WORKING,
                color = NotWorkingRed,
                working = false,
                onClick = { onVote(DealVote.NOT_WORKING) },
                modifier = Modifier.weight(1f),
            )
        }

        Button(
            onClick = onShare,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
            ),
        ) {
            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "Share link",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            )
        }

        Text(
            text = deal.description,
            modifier = Modifier.padding(top = 20.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Text(
            text = "HOW TO CLAIM",
            modifier = Modifier.padding(top = 24.dp, bottom = 10.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            letterSpacing = 2.sp,
        )

        deal.steps.forEachIndexed { index, step ->
            ClaimStep(
                number = index + 1,
                text = step,
                showDivider = index < deal.steps.lastIndex,
            )
        }
    }
}

private fun detailCategoryEmoji(category: String): String = when (category.trim().lowercase()) {
    "food" -> "🍔"
    "tech" -> "💻"
    "beauty" -> "💅"
    "fashion" -> "👟"
    "travel" -> "✈️"
    "event", "events" -> "🎟️"
    "gaming" -> "🕹️"
    "amazon" -> "📦"
    else -> "🔥"
}

@Composable
private fun VoteButton(
    label: String,
    count: Int,
    selected: Boolean,
    color: Color,
    working: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = if (selected) Color.White else color
    Surface(
        onClick = onClick,
        modifier = modifier.semantics {
            this.selected = selected
            role = Role.RadioButton
            contentDescription = "$label, $count votes"
            stateDescription = if (selected) "Selected" else "Not selected"
        },
        color = if (selected) color else color.copy(alpha = 0.12f),
        contentColor = contentColor,
        shape = RoundedCornerShape(16.dp),
        border = if (selected) null else BorderStroke(1.dp, color.copy(alpha = 0.30f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (working) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = contentColor,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = label,
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    color = contentColor,
                )
                Text(
                    text = count.toString(),
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    color = contentColor.copy(alpha = 0.70f),
                )
            }
        }
    }
}

@Composable
private fun ClaimStep(
    number: Int,
    text: String,
    showDivider: Boolean,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = number.toString(),
                    color = Color.White,
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 40.dp),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f),
            )
        }
    }
}

@Composable
private fun ClaimBar(
    claimed: Boolean,
    hasDealLink: Boolean,
    onClaim: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background,
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .navigationBarsPadding()
                .padding(start = 18.dp, end = 18.dp, bottom = 12.dp),
        ) {
            Button(
                onClick = onClaim,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp)
                    .testTag(PriceErrorsTestTags.CLAIM_DEAL)
                    .semantics {
                        stateDescription = if (claimed) "Claimed" else "Not claimed"
                    },
                shape = RoundedCornerShape(22.dp),
                contentPadding = PaddingValues(horizontal = 18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (claimed) WorkingGreen else MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                ),
            ) {
                Text(
                    text = when {
                        claimed -> "✓  GOT IT"
                        hasDealLink -> "GET THIS DEAL →"
                        else -> "FOLLOW INSTRUCTIONS"
                    },
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    letterSpacing = 0.2.sp,
                    color = Color.White,
                )
            }
        }
    }
}

private data class ConfettiPiece(
    val xFraction: Float,
    val delayFraction: Float,
    val color: Color,
    val sizeDp: Float,
    val shape: Int,
    val swayDp: Float,
    val fallFraction: Float,
    val rotation: Float,
)

@Composable
private fun ConfettiOverlay(
    key: String,
    savings: Double,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = remember { Animatable(0f) }
    var active by remember { mutableStateOf(false) }
    val stampScale by animateFloatAsState(
        targetValue = if (active) 1f else 3f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 350f),
        label = "claimStampScale",
    )
    val stampAlpha by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "claimStampAlpha",
    )
    val pieces = remember(key) { createConfettiPieces(key) }

    LaunchedEffect(key) {
        active = true
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2_200, easing = FastOutSlowInEasing),
        )
        delay(300)
        onFinished()
    }

    Box(modifier = modifier.clearAndSetSemantics { }) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            pieces.forEach { piece ->
                val localProgress = (
                    (progress.value - piece.delayFraction) / (1f - piece.delayFraction)
                    ).coerceIn(0f, 1f)
                if (localProgress <= 0f || localProgress >= 1f) return@forEach

                val pieceSize = piece.sizeDp.dp.toPx()
                val x = size.width * piece.xFraction +
                    piece.swayDp.dp.toPx() * sin(localProgress * PI).toFloat()
                val y = size.height * 0.45f + size.height * piece.fallFraction * localProgress
                val particleColor = piece.color.copy(alpha = (1f - localProgress).coerceAtLeast(0f))

                when (piece.shape) {
                    1 -> drawCircle(
                        color = particleColor,
                        radius = pieceSize / 2f,
                        center = Offset(x, y),
                    )
                    2 -> drawRoundRect(
                        color = particleColor,
                        topLeft = Offset(x - pieceSize, y - pieceSize * 0.25f),
                        size = Size(pieceSize * 2f, pieceSize * 0.5f),
                        cornerRadius = CornerRadius(2.dp.toPx()),
                    )
                    else -> drawRoundRect(
                        color = particleColor,
                        topLeft = Offset(x - pieceSize / 2f, y - pieceSize / 2f),
                        size = Size(pieceSize, pieceSize),
                        cornerRadius = CornerRadius(2.dp.toPx()),
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .rotate(-12f)
                .scale(stampScale)
                .alpha(stampAlpha),
            color = Color.White.copy(alpha = 0.92f),
            contentColor = NotWorkingRed,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(5.dp, NotWorkingRed),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "CLAIMED",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp,
                    letterSpacing = 2.sp,
                )
                Text(
                    text = "${NumberFormat.getCurrencyInstance(Locale.US).format(savings)} SAVED",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    letterSpacing = 0.8.sp,
                )
            }
        }
    }
}

@Composable
private fun ClaimConfirmationSheet(
    isConfirming: Boolean,
    onConfirm: () -> Unit,
    onDecline: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 18.dp, end = 18.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Text("🤑", fontSize = 54.sp)
        Text(
            "Did you get it?",
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            letterSpacing = (-0.7).sp,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = onConfirm,
                enabled = !isConfirming,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF22C55E),
                    contentColor = Color.White,
                ),
            ) {
                if (isConfirming) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("YES", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold)
                }
            }
            Button(
                onClick = onDecline,
                enabled = !isConfirming,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF4444),
                    contentColor = Color.White,
                ),
            ) {
                Text("NO", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun createConfettiPieces(key: String): List<ConfettiPiece> {
    val random = Random(key.hashCode().toLong())
    val colors = listOf(
        Color(0xFF7C4DFF),
        Color(0xFFFF7EB6),
        Color(0xFFFFC93D),
        Color(0xFF00C49A),
        Color(0xFFFF3D3D),
    )
    return List(55) { index ->
        ConfettiPiece(
            xFraction = random.nextFloat(),
            delayFraction = random.nextFloat() * 0.14f,
            color = colors[index % colors.size],
            sizeDp = 6f + random.nextFloat() * 8f,
            shape = index % 3,
            swayDp = -80f + random.nextFloat() * 160f,
            fallFraction = 0.60f + random.nextFloat() * 0.30f,
            rotation = -360f + random.nextFloat() * 720f,
        )
    }
}
