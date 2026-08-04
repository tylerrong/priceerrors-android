package app.priceerrors.feature.detail

import android.content.Intent
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
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
import app.priceerrors.core.model.Deal
import app.priceerrors.core.model.DealVote
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
) {
    val context = LocalContext.current
    val websiteUrl = stringResource(R.string.website_url)
    val uriHandler = LocalUriHandler.current
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val animationsEnabled = rememberAnimationsEnabled()
    val dragOffset = remember { Animatable(0f) }
    val swipeThresholdPx = with(density) { 100.dp.toPx() }
    var showCelebration by remember(deal.id) { mutableStateOf(false) }

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
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "🔥 ${deal.title} for ${formatPrice(deal.priceInCents, deal.currencyCode)}" +
                                " — spotted on PriceErrors!\n" +
                                (deal.dealUrl ?: websiteUrl),
                        )
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share deal"))
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
                if (!isClaimed) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    showCelebration = animationsEnabled
                    onClaim()
                }
                deal.dealUrl?.takeIf(String::isNotBlank)?.let(uriHandler::openUri)
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        if (showCelebration) {
            ConfettiOverlay(
                key = deal.id,
                onFinished = { showCelebration = false },
                modifier = Modifier.fillMaxSize(),
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
                .padding(start = 14.dp, top = 44.dp, end = 14.dp),
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

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                DetailPill(
                    text = deal.tag,
                    background = AppDark.copy(alpha = 0.90f),
                    foreground = MaterialTheme.colorScheme.tertiary,
                )
                DetailPill(
                    text = "${detailCategoryEmoji(deal.category)} ${relativeDealTime(deal.postedAt)}",
                    background = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    foreground = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Surface(
                modifier = Modifier.rotate(-6f),
                color = MaterialTheme.colorScheme.tertiary,
                contentColor = AppDark,
                shape = RoundedCornerShape(14.dp),
                shadowElevation = 8.dp,
            ) {
                Text(
                    text = "-${deal.discountPercent}%",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    letterSpacing = (-0.5).sp,
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
                if (claimed) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(21.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = when {
                        claimed -> "CLAIMED — GOOD LUCK"
                        hasDealLink -> "CLAIM THIS DEAL  →"
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
            Text(
                text = "CLAIMED",
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                letterSpacing = 2.sp,
            )
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
