package app.priceerrors.feature.feed

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import app.priceerrors.core.model.Deal
import app.priceerrors.ui.components.DealArtwork
import app.priceerrors.ui.components.DealPill
import app.priceerrors.ui.components.LiveTicker
import app.priceerrors.ui.components.PriceErrorsLogo
import app.priceerrors.ui.components.dealVisuals
import app.priceerrors.ui.components.dealAccessibilityLabel
import app.priceerrors.ui.components.formatPrice
import app.priceerrors.ui.components.relativeDealTime
import app.priceerrors.ui.theme.AppDark
import app.priceerrors.ui.theme.FeedLayoutOption
import app.priceerrors.ui.theme.SpaceGrotesk
import app.priceerrors.ui.accessibility.PriceErrorsTestTags
import app.priceerrors.ui.accessibility.rememberAnimationsEnabled

/**
 * The feed owns its content only. The floating application navigation is drawn by the app root so
 * it remains in exactly the same position while switching between Feed, Community, Browse and You.
 */
@Composable
fun FeedScreen(
    uiState: FeedUiState,
    onDealSelected: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    layout: FeedLayoutOption = FeedLayoutOption.SWIPE,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag(PriceErrorsTestTags.FEED_SCREEN),
    ) {
        when {
            uiState.isLoading && uiState.deals.isEmpty() -> FeedLoading()

            uiState.deals.isEmpty() -> EmptyFeed(
                isError = uiState.errorMessage != null,
                onRetry = onRetry,
            )

            layout == FeedLayoutOption.SWIPE -> SwipeFeed(
                deals = uiState.deals,
                isRefreshing = uiState.isRefreshing,
                errorMessage = uiState.errorMessage,
                onDealSelected = onDealSelected,
                onRefresh = onRetry,
            )

            layout == FeedLayoutOption.LIST -> ListFeed(
                deals = uiState.deals,
                isRefreshing = uiState.isRefreshing,
                errorMessage = uiState.errorMessage,
                onDealSelected = onDealSelected,
                onRefresh = onRetry,
            )

            else -> GridFeed(
                deals = uiState.deals,
                errorMessage = uiState.errorMessage,
                onDealSelected = onDealSelected,
                onRefresh = onRetry,
            )
        }
    }
}

@Composable
private fun SwipeFeed(
    deals: List<Deal>,
    isRefreshing: Boolean,
    errorMessage: String?,
    onDealSelected: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    var currentIndex by rememberSaveable { mutableIntStateOf(0) }
    val safeIndex = currentIndex.coerceIn(0, deals.lastIndex)
    val currentDeal = deals[safeIndex]
    var dragTargetX by remember { mutableFloatStateOf(0f) }
    var rawDragX by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val animationsEnabled = rememberAnimationsEnabled()
    val thresholdPx = with(density) { 70.dp.toPx() }
    val dragX by animateFloatAsState(
        targetValue = dragTargetX,
        animationSpec = if (isDragging || !animationsEnabled) {
            snap()
        } else {
            spring(
                dampingRatio = 0.8f,
                stiffness = Spring.StiffnessMediumLow,
            )
        },
        label = "feedCardDrag",
    )

    LaunchedEffect(deals.size) {
        currentIndex = currentIndex.coerceIn(0, deals.lastIndex)
    }

    fun moveTo(index: Int) {
        if (index !in deals.indices || index == safeIndex) return
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        currentIndex = index
    }

    val dragState = rememberDraggableState { delta ->
        rawDragX += delta
        val pastStart = safeIndex == 0 && rawDragX > 0
        val pastEnd = safeIndex == deals.lastIndex && rawDragX < 0
        dragTargetX = if (pastStart || pastEnd) rawDragX / 4f else rawDragX
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        FeedHeader(
            isRefreshing = isRefreshing,
            showRefresh = true,
            onRefresh = onRefresh,
        )

        Text(
            text = "${safeIndex + 1} / ${deals.size} · SWIPE FOR NEXT",
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.40f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
        )

        if (errorMessage != null) {
            InlineError(onRetry = onRefresh)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 10.dp, bottom = 140.dp),
            contentAlignment = Alignment.Center,
        ) {
            SwipeDealCard(
                deal = currentDeal,
                onClick = { onDealSelected(currentDeal.id) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp)
                    .graphicsLayer {
                        translationX = dragX
                        rotationZ = dragX / 40f
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = dragState,
                        onDragStarted = { isDragging = true },
                        onDragStopped = {
                            when {
                                rawDragX < -thresholdPx -> moveTo(safeIndex + 1)
                                rawDragX > thresholdPx -> moveTo(safeIndex - 1)
                            }
                            isDragging = false
                            rawDragX = 0f
                            dragTargetX = 0f
                        },
                    ),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CardArrow(
                    pointsForward = false,
                    enabled = safeIndex > 0,
                    onClick = { moveTo(safeIndex - 1) },
                )
                CardArrow(
                    pointsForward = true,
                    enabled = safeIndex < deals.lastIndex,
                    onClick = { moveTo(safeIndex + 1) },
                )
            }
        }
    }
}

@Composable
private fun FeedHeader(
    isRefreshing: Boolean,
    showRefresh: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 18.dp,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = horizontalPadding,
                end = horizontalPadding,
                top = if (showRefresh) 24.dp else 12.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PriceErrorsLogo(accent = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.weight(1f))
        if (showRefresh) {
            IconButton(
                onClick = onRefresh,
                enabled = !isRefreshing,
                modifier = Modifier
                    .size(48.dp)
                    .testTag(PriceErrorsTestTags.REFRESH_DEALS),
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(17.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh deals",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.40f),
                    )
                }
            }
            Spacer(modifier = Modifier.width(5.dp))
        }
        LiveTicker()
    }
}

@Composable
private fun SwipeDealCard(
    deal: Deal,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val visuals = dealVisuals(deal.category, darkTheme)
    val outerShape = RoundedCornerShape(34.dp)

    Column(
        modifier = modifier
            .shadow(
                elevation = 22.dp,
                shape = outerShape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.14f),
                spotColor = Color.Black.copy(alpha = 0.14f),
            )
            .clip(outerShape)
            .semantics(mergeDescendants = true) {
                contentDescription = dealAccessibilityLabel(deal)
            }
            .testTag("${PriceErrorsTestTags.FEED_DEAL}_${deal.id}")
            .background(visuals.background)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DealPill(
                text = deal.tag,
                dark = true,
                darkTextColor = MaterialTheme.colorScheme.tertiary,
            )
            DealPill(text = "⏰ ${relativeDealTime(deal.postedAt)}")
        }

        DealArtwork(
            deal = deal,
            portrait = true,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 6.dp)
                .clip(RoundedCornerShape(20.dp)),
            contentScale = ContentScale.Fit,
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            shadowElevation = 6.dp,
        ) {
            Row(
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = deal.brand.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = visuals.accent,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                    )
                    Text(
                        text = deal.title,
                        modifier = Modifier.padding(top = 2.dp),
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        lineHeight = 22.sp,
                        letterSpacing = (-0.5).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    PriceWithOriginal(
                        deal = deal,
                        priceSize = 26,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                DiscountBadge(discount = deal.discountPercent)
            }
        }
    }
}

@Composable
private fun ListFeed(
    deals: List<Deal>,
    isRefreshing: Boolean,
    errorMessage: String?,
    onDealSelected: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            FeedHeader(
                isRefreshing = isRefreshing,
                showRefresh = false,
                onRefresh = onRefresh,
            )
        }
        item {
            Column(
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 2.dp, bottom = 2.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                FeedTitle()
                Text(
                    text = "${deals.size} live · ${if (isRefreshing) "refreshing…" else "refreshed just now"}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.50f),
                )
            }
        }
        if (errorMessage != null) {
            item { InlineError(onRetry = onRefresh) }
        }
        items(items = deals, key = { it.id }) { deal ->
            ListDealRow(
                deal = deal,
                onClick = { onDealSelected(deal.id) },
                modifier = Modifier.padding(horizontal = 14.dp),
            )
        }
    }
}

@Composable
private fun ListDealRow(
    deal: Deal,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(22.dp))
            .semantics(mergeDescendants = true) {
                contentDescription = dealAccessibilityLabel(deal)
            }
            .testTag("${PriceErrorsTestTags.FEED_DEAL}_${deal.id}"),
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DealArtwork(
                deal = deal,
                portrait = false,
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(18.dp)),
                contentScale = ContentScale.Crop,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SmallDealPill(text = deal.tag)
                    Spacer(modifier = Modifier.width(5.dp))
                    AnimatedHeat(text = deal.heat.take(4))
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "⏰ ${relativeDealTime(deal.postedAt)}",
                        fontFamily = SpaceGrotesk,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                        maxLines = 1,
                    )
                }
                Text(
                    text = deal.title,
                    fontFamily = SpaceGrotesk,
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    letterSpacing = (-0.3).sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = deal.brand.uppercase(),
                    fontFamily = SpaceGrotesk,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.50f),
                    maxLines = 1,
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = formatPrice(deal.priceInCents, deal.currencyCode),
                    fontFamily = SpaceGrotesk,
                    fontSize = 20.sp,
                    letterSpacing = (-0.5).sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    text = "-${deal.discountPercent}%",
                    fontFamily = SpaceGrotesk,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun GridFeed(
    deals: List<Deal>,
    errorMessage: String?,
    onDealSelected: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 140.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            FeedHeader(
                isRefreshing = false,
                showRefresh = false,
                onRefresh = onRefresh,
                horizontalPadding = 4.dp,
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            FeedTitle(modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 2.dp, bottom = 2.dp))
        }
        if (errorMessage != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                InlineError(onRetry = onRefresh)
            }
        }
        items(items = deals, key = { it.id }) { deal ->
            GridDealCard(
                deal = deal,
                onClick = { onDealSelected(deal.id) },
            )
        }
    }
}

@Composable
private fun GridDealCard(
    deal: Deal,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val visuals = dealVisuals(deal.category, darkTheme)
    val shape = RoundedCornerShape(20.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, shape)
            .clip(shape)
            .background(visuals.background)
            .semantics(mergeDescendants = true) {
                contentDescription = dealAccessibilityLabel(deal)
            }
            .testTag("${PriceErrorsTestTags.FEED_DEAL}_${deal.id}")
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(10.dp)
                .clip(RoundedCornerShape(14.dp)),
        ) {
            DealArtwork(
                deal = deal,
                portrait = false,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            SmallDealPill(
                text = deal.tag,
                modifier = Modifier.padding(10.dp),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = deal.title,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontFamily = SpaceGrotesk,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatPrice(deal.priceInCents, deal.currencyCode),
                    fontFamily = SpaceGrotesk,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "-${deal.discountPercent}%",
                    fontFamily = SpaceGrotesk,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun FeedTitle(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = "Today's drops ",
            fontFamily = SpaceGrotesk,
            fontSize = 32.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1.2).sp,
        )
        Text(
            text = ".",
            fontFamily = SpaceGrotesk,
            fontSize = 32.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun PriceWithOriginal(
    deal: Deal,
    priceSize: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = formatPrice(deal.priceInCents, deal.currencyCode),
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = priceSize.sp,
            lineHeight = priceSize.sp,
            letterSpacing = (-(priceSize * 0.04)).sp,
            maxLines = 1,
        )
        if (deal.originalPriceInCents > deal.priceInCents) {
            Text(
                text = formatPrice(deal.originalPriceInCents, deal.currencyCode),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.40f),
                textDecoration = TextDecoration.LineThrough,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun DiscountBadge(discount: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.rotate(-4f),
        color = MaterialTheme.colorScheme.tertiary,
        contentColor = AppDark,
        shape = RoundedCornerShape(10.dp),
    ) {
        Text(
            text = "-$discount%",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            fontFamily = SpaceGrotesk,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SmallDealPill(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = AppDark.copy(alpha = 0.86f),
        contentColor = MaterialTheme.colorScheme.tertiary,
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = text.uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            fontFamily = SpaceGrotesk,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.3.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun AnimatedHeat(text: String) {
    val animationsEnabled = rememberAnimationsEnabled()
    val rotation: Float
    val scale: Float
    if (animationsEnabled) {
        val transition = rememberInfiniteTransition(label = "heatBadge")
        val animatedRotation by transition.animateFloat(
            initialValue = -3f,
            targetValue = 3f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 600),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "heatRotation",
        )
        val animatedScale by transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 600),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "heatScale",
        )
        rotation = animatedRotation
        scale = animatedScale
    } else {
        rotation = 0f
        scale = 1f
    }
    Text(
        text = text,
        modifier = Modifier.graphicsLayer {
            rotationZ = rotation
            scaleX = scale
            scaleY = scale
        },
        fontSize = 14.sp,
        maxLines = 1,
    )
}

@Composable
private fun CardArrow(
    pointsForward: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .size(48.dp)
            .alpha(if (enabled) 1f else 0.50f),
        color = AppDark,
        contentColor = Color.White.copy(alpha = if (enabled) 1f else 0.40f),
        shape = CircleShape,
        shadowElevation = if (enabled) 8.dp else 0.dp,
    ) {
        IconButton(onClick = onClick, enabled = enabled) {
            Icon(
                imageVector = if (pointsForward) {
                    Icons.AutoMirrored.Filled.ArrowForward
                } else {
                    Icons.AutoMirrored.Filled.ArrowBack
                },
                contentDescription = if (pointsForward) "Next deal" else "Previous deal",
                modifier = Modifier.size(19.dp),
            )
        }
    }
}

@Composable
private fun InlineError(onRetry: () -> Unit) {
    Surface(
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Couldn't refresh deals.",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = onRetry) { Text("RETRY") }
        }
    }
}

@Composable
private fun FeedLoading() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 140.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.65f),
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.dp,
                )
                Text(
                    text = "Fetching today's deals…",
                    fontFamily = SpaceGrotesk,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun EmptyFeed(
    isError: Boolean,
    onRetry: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 32.dp, end = 32.dp, bottom = 140.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = if (isError) "😬" else "🏜️", fontSize = 64.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isError) "Couldn't load deals" else "No deals right now",
            fontFamily = SpaceGrotesk,
            fontSize = 22.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isError) {
                "Check your connection and try again."
            } else {
                "The internet is searching. Check back in a few minutes."
            },
            fontFamily = SpaceGrotesk,
            fontSize = 13.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.50f),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Surface(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onRetry()
            },
            color = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(
                text = "TRY AGAIN",
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 13.dp),
                fontFamily = SpaceGrotesk,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
            )
        }
    }
}
