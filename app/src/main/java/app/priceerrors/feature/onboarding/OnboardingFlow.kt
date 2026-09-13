package app.priceerrors.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.priceerrors.R
import app.priceerrors.ui.accessibility.PriceErrorsTestTags
import app.priceerrors.ui.accessibility.rememberAnimationsEnabled
import app.priceerrors.ui.theme.AppDark
import app.priceerrors.ui.theme.Mint
import app.priceerrors.ui.theme.SpaceGrotesk
import app.priceerrors.ui.theme.Yellow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val OnboardingOrange = Color(0xFFFF8A3D)
private val PreferencesPurple = Color(0xFF7C4DFF)
private val PriceRed = Color(0xFFFF3D3D)

private data class OnboardingSlide(
    val background: Color,
    val buttonTitle: String,
)

private val slides = listOf(
    OnboardingSlide(background = Mint, buttonTitle = "Get Started"),
    OnboardingSlide(background = OnboardingOrange, buttonTitle = "Keep Going"),
)

/**
 * Shared progress treatment for every onboarding step. Keeping it public lets
 * the app shell use one indicator across preferences, notifications, auth, and
 * the paywall without duplicating the visual contract.
 */
@Composable
fun OnboardingProgressHeader(
    total: Int,
    current: Int,
    modifier: Modifier = Modifier,
    onSkip: (() -> Unit)? = null,
) {
    val normalizedTotal = total.coerceAtLeast(1)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .width(148.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription =
                        "Page ${current.coerceIn(0, normalizedTotal - 1) + 1} of $normalizedTotal"
                },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(normalizedTotal) { index ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(
                            AppDark.copy(alpha = if (index <= current) 1f else 0.16f),
                            CircleShape,
                        ),
                )
            }
        }
        Spacer(Modifier.weight(1f))
        if (onSkip != null) {
            Button(
                onClick = onSkip,
                modifier = Modifier.height(36.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = AppDark.copy(alpha = 0.55f),
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp),
            ) {
                Text(
                    "Skip",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
            }
        } else {
            Spacer(Modifier.size(36.dp))
        }
    }
}

@Composable
fun OnboardingScreen(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    totalSteps: Int = 6,
    onStepCompleted: (Int) -> Unit = {},
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    val animationsEnabled = rememberAnimationsEnabled()
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val dragOffset = remember { Animatable(0f) }
    val swipeThreshold = with(density) { 72.dp.toPx() }
    val scope = rememberCoroutineScope()
    fun goBack() {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (step > 0) step-- else onBack?.invoke()
    }

    BackHandler(enabled = step > 0 || onBack != null, onBack = ::goBack)

    AnimatedContent(
        targetState = step,
        modifier = modifier
            .fillMaxSize()
            .testTag(PriceErrorsTestTags.ONBOARDING_SCREEN)
            .offset { IntOffset(dragOffset.value.roundToInt(), 0) }
            .pointerInput(step, swipeThreshold) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, amount ->
                        scope.launch {
                            dragOffset.snapTo(dragOffset.value + amount)
                        }
                    },
                    onDragEnd = {
                        when {
                            dragOffset.value < -swipeThreshold -> {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onStepCompleted(step)
                                if (step < slides.lastIndex) step++ else onContinue()
                            }
                            dragOffset.value > swipeThreshold && (step > 0 || onBack != null) -> goBack()
                        }
                        scope.launch {
                            if (animationsEnabled) {
                                dragOffset.animateTo(
                                    0f,
                                    spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMedium),
                                )
                            } else {
                                dragOffset.snapTo(0f)
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            if (animationsEnabled) {
                                dragOffset.animateTo(0f, spring())
                            } else {
                                dragOffset.snapTo(0f)
                            }
                        }
                    },
                )
            },
        transitionSpec = {
            if (animationsEnabled) {
                val direction = if (targetState > initialState) {
                    AnimatedContentTransitionScope.SlideDirection.Left
                } else {
                    AnimatedContentTransitionScope.SlideDirection.Right
                }
                slideIntoContainer(direction, spring(dampingRatio = 0.86f)) + fadeIn() togetherWith
                    slideOutOfContainer(direction, spring(dampingRatio = 0.86f)) + fadeOut()
            } else {
                fadeIn(tween(0)) togetherWith fadeOut(tween(0))
            }
        },
        label = "onboarding-slide",
    ) { page ->
        val slide = slides[page]
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(slide.background)
                .statusBarsPadding(),
        ) {
            OnboardingProgressHeader(
                total = totalSteps,
                current = page,
                modifier = Modifier.padding(horizontal = 24.dp),
            )

            if (page == 0) {
                ProductRainSlide(
                    animationsEnabled = animationsEnabled,
                    buttonTitle = slide.buttonTitle,
                    onAdvance = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onStepCompleted(step)
                        step++
                    },
                )
            } else {
                GlitchExplainerSlide(
                    animationsEnabled = animationsEnabled,
                    buttonTitle = slide.buttonTitle,
                    onAdvance = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onStepCompleted(step)
                        onContinue()
                    },
                )
            }
        }
    }
}

@Composable
private fun ProductRainSlide(
    animationsEnabled: Boolean,
    buttonTitle: String,
    onAdvance: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 10.dp),
            verticalArrangement = Arrangement.spacedBy((-2).dp),
        ) {
            HeadlineText("Save")
            StickerWord("HUNDREDS", fill = Color(0xFFFF4D6A), foreground = Color.White)
            HeadlineText("of dollars")
            HeadlineText("every month", singleLine = true)
        }

        ProductRainScene(
            animationsEnabled = animationsEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp),
        )

        Text(
            "Find price errors, massive discounts, and freebies before they’re gone.",
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            color = AppDark.copy(alpha = 0.58f),
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            lineHeight = 20.sp,
        )
        OnboardingPrimaryButton(
            title = buttonTitle,
            onClick = onAdvance,
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .testTag(PriceErrorsTestTags.ONBOARDING_CONTINUE),
        )
    }
}

@Composable
private fun GlitchExplainerSlide(
    animationsEnabled: Boolean,
    buttonTitle: String,
    onAdvance: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
    ) {
        GlitchExplainerScene(
            animationsEnabled = animationsEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 18.dp),
        )
        OnboardingPrimaryButton(
            title = buttonTitle,
            onClick = onAdvance,
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .testTag(PriceErrorsTestTags.ONBOARDING_CONTINUE),
        )
    }
}

@Composable
private fun HeadlineText(text: String, singleLine: Boolean = false) {
    Text(
        text,
        color = AppDark,
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 38.sp,
        lineHeight = 41.sp,
        letterSpacing = (-1.5).sp,
        maxLines = if (singleLine) 1 else Int.MAX_VALUE,
    )
}

@Composable
private fun StickerWord(
    text: String,
    fill: Color,
    foreground: Color,
    modifier: Modifier = Modifier,
    fontSize: Int = 34,
) {
    Text(
        text,
        modifier = modifier
            .graphicsLayer { rotationZ = -5f }
            .shadow(6.dp, RoundedCornerShape(12.dp))
            .background(fill, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        color = foreground,
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Bold,
        fontSize = fontSize.sp,
        letterSpacing = (-0.5).sp,
    )
}

private data class RainDeal(
    val asset: Int,
    val title: String,
    val current: String,
    val original: String,
    val delayMillis: Long,
    val rotation: Float,
)

private val rainDeals = listOf(
    RainDeal(R.drawable.onboarding_burrito, "CHIPOTLE", "FREE", "$10+", 20, -6f),
    RainDeal(R.drawable.onboarding_eros, "VERSACE EROS", "$39", "$120", 180, 6f),
    RainDeal(R.drawable.onboarding_ps5, "PS5", "$90", "$599.99", 340, 4f),
    RainDeal(R.drawable.onboarding_watch, "APPLE WATCH", "$374", "$649", 500, -6f),
)

@Composable
private fun ProductRainScene(
    animationsEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription =
                "Deals raining in, including a free Chipotle burrito, Versace Eros for 39 dollars, " +
                    "a 90 dollar PS5, and a 374 dollar Apple Watch"
        },
        verticalArrangement = Arrangement.Center,
    ) {
        rainDeals.chunked(2).forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { deal ->
                    RainDealCard(
                        deal = deal,
                        animationsEnabled = animationsEnabled,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun RainDealCard(
    deal: RainDeal,
    animationsEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val fall = remember(deal.title) { Animatable(if (animationsEnabled) -470f else 0f) }
    LaunchedEffect(animationsEnabled, deal.title) {
        if (!animationsEnabled) {
            fall.snapTo(0f)
        } else {
            fall.snapTo(-470f)
            delay(deal.delayMillis)
            fall.animateTo(
                0f,
                spring(dampingRatio = 0.48f, stiffness = Spring.StiffnessMediumLow),
            )
        }
    }

    Box(
        modifier = modifier.graphicsLayer {
            translationY = fall.value
            rotationZ = deal.rotation
        },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(deal.asset),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 26.dp, start = 12.dp, end = 12.dp),
            contentScale = ContentScale.Fit,
        )
        PriceBadge(
            title = deal.title,
            current = deal.current,
            original = deal.original,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun PriceBadge(
    title: String?,
    current: String,
    original: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .shadow(7.dp, RoundedCornerShape(13.dp))
            .background(Color.White.copy(alpha = 0.96f), RoundedCornerShape(13.dp))
            .border(1.dp, AppDark.copy(alpha = 0.10f), RoundedCornerShape(13.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (title != null) {
            Text(
                title,
                color = AppDark.copy(alpha = 0.48f),
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                letterSpacing = 0.45.sp,
                maxLines = 1,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                current,
                color = AppDark,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = if (title == null) 17.sp else 20.sp,
            )
            Text(
                original,
                color = AppDark.copy(alpha = 0.44f),
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = if (title == null) 10.sp else 12.sp,
                textDecoration = TextDecoration.LineThrough,
            )
        }
    }
}

@Composable
private fun GlitchExplainerScene(
    animationsEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription =
                "Price glitches happen all the time. We send them instantly, saving you hundreds " +
                    "of dollars every single month."
        },
    ) {
        FloatingDealCard(
            asset = R.drawable.onboarding_computer,
            current = "$144",
            original = "$2,153",
            travel = -7f,
            animationsEnabled = animationsEnabled,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 4.dp, y = 34.dp)
                .size(width = 164.dp, height = 142.dp)
                .graphicsLayer { rotationZ = -7f },
        )
        FloatingDealCard(
            asset = R.drawable.onboarding_dunkin,
            current = "FREE",
            original = "$7",
            travel = 9f,
            animationsEnabled = animationsEnabled,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 5.dp, y = 48.dp)
                .size(width = 108.dp, height = 126.dp)
                .graphicsLayer { rotationZ = 8f },
        )
        FloatingDealCard(
            asset = R.drawable.onboarding_headphones,
            current = "$179",
            original = "$500",
            travel = 8f,
            animationsEnabled = animationsEnabled,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 4.dp, y = (-40).dp)
                .size(width = 128.dp, height = 132.dp)
                .graphicsLayer { rotationZ = 7f },
        )
        FloatingDealCard(
            asset = R.drawable.onboarding_camera,
            current = "$59",
            original = "$105",
            travel = -9f,
            animationsEnabled = animationsEnabled,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 2.dp, y = (-34).dp)
                .size(width = 122.dp, height = 128.dp)
                .graphicsLayer { rotationZ = -8f },
        )

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(width = 330.dp, height = 390.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(AppDark.copy(alpha = 0.16f), Color.Transparent),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(
                "Price glitches happen\nall the time.",
                color = Color.White,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                lineHeight = 30.sp,
                letterSpacing = (-1).sp,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    shadow = Shadow(
                        color = AppDark.copy(alpha = 0.32f),
                        offset = Offset(0f, 2f),
                        blurRadius = 5f,
                    ),
                ),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                ExplainerText("We send them to you")
                StickerWord(
                    "INSTANTLY,",
                    fill = Color.White,
                    foreground = Color(0xFF3652D9),
                    modifier = Modifier.padding(start = 5.dp),
                    fontSize = 20,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                ExplainerText("saving you")
                StickerWord(
                    "HUNDREDS",
                    fill = Color(0xFFFF4D6A),
                    foreground = Color.White,
                    modifier = Modifier.padding(start = 7.dp),
                    fontSize = 23,
                )
            }
            ExplainerText("of dollars every single month.")
        }
    }
}

@Composable
private fun ExplainerText(text: String) {
    Text(
        text,
        color = Color.White,
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        textAlign = TextAlign.Center,
        style = TextStyle(
            shadow = Shadow(
                color = AppDark.copy(alpha = 0.32f),
                offset = Offset(0f, 2f),
                blurRadius = 5f,
            ),
        ),
    )
}

@Composable
private fun FloatingDealCard(
    asset: Int,
    current: String,
    original: String,
    travel: Float,
    animationsEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val floatOffset = remember(asset) { Animatable(if (animationsEnabled) -travel else 0f) }
    LaunchedEffect(asset, animationsEnabled) {
        if (!animationsEnabled) {
            floatOffset.snapTo(0f)
        } else {
            while (true) {
                floatOffset.animateTo(travel, tween(2600))
                floatOffset.animateTo(-travel, tween(2600))
            }
        }
    }
    Box(
        modifier = modifier.graphicsLayer { translationY = floatOffset.value },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(asset),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
        PriceBadge(
            title = null,
            current = current,
            original = original,
            modifier = Modifier.align(Alignment.TopEnd),
        )
    }
}

private data class PreferenceCategory(val name: String, val emoji: String, val color: Color)

private val preferenceCategories = listOf(
    PreferenceCategory("Tech", "💻", Color(0xFF2D5BFF)),
    PreferenceCategory("Fashion", "👟", Color(0xFF1A1A1A)),
    PreferenceCategory("Food", "🌮", Color(0xFF4CAF50)),
    PreferenceCategory("Beauty", "💅", Color(0xFFFF7EB6)),
    PreferenceCategory("Gaming", "🕹️", Yellow),
    PreferenceCategory("Events", "🎫", Color(0xFFE91E63)),
    PreferenceCategory("Travel", "🛫", Color(0xFF00B4D8)),
    PreferenceCategory("All", "🛍️", Mint),
)

@Composable
fun PreferencesScreen(
    onContinue: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    totalSteps: Int = 6,
    currentStep: Int = 2,
) {
    var selected by rememberSaveable { mutableStateOf(setOf<String>()) }
    val haptics = LocalHapticFeedback.current

    BackHandler(enabled = onBack != null) { onBack?.invoke() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PreferencesPurple)
            .statusBarsPadding()
            .edgeBackGesture(onBack, haptics)
            .testTag(PriceErrorsTestTags.PREFERENCES_SCREEN),
    ) {
        OnboardingProgressHeader(
            total = totalSteps,
            current = currentStep,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Column(
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Curate your feed.",
                color = Color.White,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp,
                lineHeight = 40.sp,
                letterSpacing = (-1.4).sp,
            )
            Text(
                "Pick categories you love and we’ll create specialized alerts for every match.",
                color = Color.White.copy(alpha = 0.76f),
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                lineHeight = 20.sp,
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        ) {
            items(preferenceCategories, key = { it.name }) { category ->
                val isSelected = category.name in selected
                Surface(
                    onClick = {
                        selected = when {
                            category.name == "All" && isSelected -> emptySet()
                            category.name == "All" -> setOf("All")
                            isSelected -> selected.minus(category.name).minus("All")
                            else -> selected.minus("All").plus(category.name)
                        }
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    modifier = Modifier
                        .aspectRatio(1f)
                        .shadow(
                            elevation = if (isSelected) 10.dp else 6.dp,
                            shape = RoundedCornerShape(20.dp),
                            ambientColor = Color.Black.copy(alpha = 0.12f),
                        )
                        .then(
                            if (isSelected) {
                                Modifier.border(3.5.dp, Color.White, RoundedCornerShape(20.dp))
                            } else {
                                Modifier
                            },
                        )
                        .semantics(mergeDescendants = true) {
                            role = Role.Checkbox
                            this.selected = isSelected
                            stateDescription = if (isSelected) "Selected" else "Not selected"
                            contentDescription = "${category.name} deals"
                        }
                        .testTag("preference_${category.name.lowercase()}"),
                    color = category.color,
                    contentColor = if (category.name == "Gaming") AppDark else Color.White,
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(category.emoji, fontSize = 34.sp)
                        Text(
                            category.name,
                            modifier = Modifier.padding(top = 8.dp),
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = (-0.3).sp,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 8.dp, bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = when {
                    selected.isEmpty() -> "Choose at least one category to continue"
                    "All" in selected -> "All deal alerts ready"
                    else -> "${selected.size} category alert${if (selected.size == 1) "" else "s"} ready"
                },
                color = Color.White.copy(alpha = 0.72f),
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
            OnboardingPrimaryButton(
                title = "Continue",
                enabled = selected.isNotEmpty(),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onContinue(selected)
                },
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .testTag(PriceErrorsTestTags.PREFERENCES_CONTINUE),
            )
        }
    }
}

/**
 * Pre-prompt for Android's system notification dialog. [onEnableAlerts] may
 * suspend until the permission result is known; the flow only advances after
 * that callback completes.
 */
@Composable
fun NotificationPermissionStep(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    onEnableAlerts: suspend () -> Unit = {},
    onBack: (() -> Unit)? = null,
    totalSteps: Int = 6,
    currentStep: Int = 3,
) {
    var isRequesting by rememberSaveable { mutableStateOf(false) }
    val animationsEnabled = rememberAnimationsEnabled()
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    BackHandler(enabled = onBack != null && !isRequesting) { onBack?.invoke() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Yellow)
            .statusBarsPadding()
            .edgeBackGesture(if (isRequesting) null else onBack, haptics)
            .testTag("notification_permission_screen"),
    ) {
        OnboardingProgressHeader(
            total = totalSteps,
            current = currentStep,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Column(
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "Turn on deal alerts",
                color = AppDark,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 34.sp,
                lineHeight = 38.sp,
                letterSpacing = (-1.4).sp,
            )
            Text(
                "Customize alerts by category and keyword to find deals you’ll love, then get notified the moment a match drops.",
                color = AppDark.copy(alpha = 0.55f),
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 22.sp,
            )
        }

        AlertBurstScene(
            animationsEnabled = animationsEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
        )
        Spacer(Modifier.weight(1f))

        if (isRequesting) {
            Box(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(AppDark, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.dp,
                )
            }
        } else {
            OnboardingPrimaryButton(
                title = "Enable alerts",
                onClick = {
                    if (isRequesting) return@OnboardingPrimaryButton
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    isRequesting = true
                    scope.launch {
                        try {
                            onEnableAlerts()
                        } finally {
                            isRequesting = false
                            onContinue()
                        }
                    }
                },
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .testTag("notification_enable"),
            )
        }
    }
}

@Composable
private fun AlertBurstScene(
    animationsEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    var shown by remember { mutableStateOf(!animationsEnabled) }
    LaunchedEffect(animationsEnabled) {
        if (animationsEnabled) {
            delay(150)
            shown = true
        }
    }
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "Push alerts when a price breaks"
        },
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.height(140.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(if (shown) 138.dp else 82.dp)
                    .border(2.dp, AppDark.copy(alpha = 0.10f), CircleShape),
            )
            Image(
                painter = painterResource(R.drawable.ob_bell),
                contentDescription = null,
                modifier = Modifier
                    .size(112.dp)
                    .shadow(18.dp, CircleShape),
            )
        }
        AlertToast(
            store = "Target",
            line = "AirPods Pro just hit $24",
            ago = "now",
            modifier = Modifier.graphicsLayer {
                alpha = if (shown) 1f else 0f
                translationX = if (shown) 0f else 40.dp.toPx()
            },
        )
        AlertToast(
            store = "Starbucks",
            line = "Grande Matcha is free today",
            ago = "12s",
            modifier = Modifier.graphicsLayer {
                alpha = if (shown) 1f else 0f
                translationX = if (shown) 0f else 40.dp.toPx()
            },
        )
    }
}

@Composable
private fun AlertToast(
    store: String,
    line: String,
    ago: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(18.dp))
            .background(Color.White, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).background(PriceRed, CircleShape))
        Column {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(store, fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(
                    "· $ago",
                    color = AppDark.copy(alpha = 0.40f),
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                )
            }
            Text(
                line,
                color = AppDark.copy(alpha = 0.80f),
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun OnboardingPrimaryButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppDark,
            contentColor = Color.White,
            disabledContainerColor = AppDark.copy(alpha = 0.50f),
            disabledContentColor = Color.White.copy(alpha = 0.72f),
        ),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                letterSpacing = (-0.3).sp,
            )
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

private fun Modifier.edgeBackGesture(
    onBack: (() -> Unit)?,
    haptics: HapticFeedback,
): Modifier {
    if (onBack == null) return this
    return pointerInput(onBack) {
        var startedAtEdge = false
        var horizontal = 0f
        var vertical = 0f
        val edge = 44.dp.toPx()
        val threshold = 72.dp.toPx()
        detectDragGestures(
            onDragStart = {
                startedAtEdge = it.x <= edge
                horizontal = 0f
                vertical = 0f
            },
            onDrag = { change, amount ->
                horizontal += amount.x
                vertical += amount.y
                change.consume()
            },
            onDragEnd = {
                if (
                    startedAtEdge &&
                    horizontal > threshold &&
                    horizontal > kotlin.math.abs(vertical) * 1.2f
                ) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onBack()
                }
            },
        )
    }
}
