package app.priceerrors.feature.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.priceerrors.R
import app.priceerrors.ui.components.PriceErrorsLogo
import app.priceerrors.ui.accessibility.PriceErrorsTestTags
import app.priceerrors.ui.accessibility.rememberAnimationsEnabled
import app.priceerrors.ui.theme.AppDark
import app.priceerrors.ui.theme.Mint
import app.priceerrors.ui.theme.Pink
import app.priceerrors.ui.theme.SpaceGrotesk
import app.priceerrors.ui.theme.Yellow

private data class OnboardingSlide(
    val background: Color,
    val foreground: Color,
    val artwork: Int,
    val kicker: String,
    val title: String,
    val body: String,
)

private val slides = listOf(
    OnboardingSlide(
        background = Mint,
        foreground = Color.White,
        artwork = R.drawable.ob_gift,
        kicker = "WELCOME",
        title = "price errors, glitches & freebies — daily.",
        body = "Aggregated from every corner of the internet. Built for students. Start with a 7-day free trial.",
    ),
    OnboardingSlide(
        background = Yellow,
        foreground = AppDark,
        artwork = R.drawable.ob_flash,
        kicker = "LIVE DEALS",
        title = "10–20 broken\nprices. every day.",
        body = "AirPods for $24. Grande Matcha on the house. Concert floor seats for dorm prices.",
    ),
    OnboardingSlide(
        background = Pink,
        foreground = AppDark,
        artwork = R.drawable.ob_bell,
        kicker = "GO GO GO",
        title = "alerts the second\nsomething breaks.",
        body = "When Target messes up, we notify you before they notice. Be first in line before it's fixed.",
    ),
)

@Composable
fun OnboardingScreen(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    val slide = slides[step]
    val animationsEnabled = rememberAnimationsEnabled()

    AnimatedContent(
        targetState = slide,
        modifier = modifier
            .fillMaxSize()
            .testTag(PriceErrorsTestTags.ONBOARDING_SCREEN),
        transitionSpec = {
            if (animationsEnabled) {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
                ) + fadeIn() togetherWith
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left) + fadeOut()
            } else {
                fadeIn(animationSpec = tween(0)) togetherWith fadeOut(animationSpec = tween(0))
            }
        },
        label = "onboardingSlide",
    ) { current ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(current.background),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 140.dp, y = (-175).dp)
                    .size(400.dp)
                    .background(Color.White.copy(alpha = 0.12f), CircleShape),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-140).dp, y = 200.dp)
                    .size(280.dp)
                    .background(Color.Black.copy(alpha = 0.08f), CircleShape),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PriceErrorsLogo(
                        accent = if (current.foreground == Color.White) Yellow else Mint,
                        color = current.foreground,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "SKIP",
                        modifier = Modifier
                            .size(width = 56.dp, height = 48.dp)
                            .clickable(role = Role.Button, onClick = onContinue)
                            .padding(top = 15.dp),
                        color = current.foreground.copy(alpha = 0.70f),
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp,
                    )
                }

                androidx.compose.foundation.Image(
                    painter = painterResource(current.artwork),
                    contentDescription = "${current.kicker.lowercase()} illustration",
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 54.dp, vertical = 4.dp)
                        .rotate(-6f),
                    contentScale = ContentScale.Fit,
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = 22.dp, end = 22.dp, bottom = 28.dp),
                ) {
                    Surface(
                        color = current.foreground.copy(alpha = 0.15f),
                        contentColor = current.foreground,
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            current.foreground.copy(alpha = 0.35f),
                        ),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(6.dp).background(current.foreground, CircleShape))
                            Text(
                                current.kicker,
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 2.sp,
                            )
                        }
                    }

                    Text(
                        text = current.title,
                        modifier = Modifier.padding(top = 16.dp),
                        color = current.foreground,
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 38.sp,
                        lineHeight = 41.sp,
                        letterSpacing = (-1.8).sp,
                    )
                    Text(
                        text = current.body,
                        modifier = Modifier.padding(top = 14.dp),
                        color = current.foreground.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 21.sp,
                    )
                    Row(
                        modifier = Modifier
                            .padding(top = 22.dp, bottom = 16.dp)
                            .semantics(mergeDescendants = true) {
                                contentDescription = "Page ${step + 1} of ${slides.size}"
                            },
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        slides.indices.forEach { index ->
                            Box(
                                Modifier
                                    .width(if (index == step) 28.dp else 8.dp)
                                    .height(8.dp)
                                    .background(
                                        if (index == step) current.foreground else current.foreground.copy(alpha = 0.30f),
                                        CircleShape,
                                    ),
                            )
                        }
                    }
                    ChunkyButton(
                        title = if (step == slides.lastIndex) "PICK MY DEALS →" else "NEXT",
                        background = AppDark,
                        foreground = Color.White,
                        modifier = Modifier.testTag(PriceErrorsTestTags.ONBOARDING_CONTINUE),
                        onClick = {
                            if (step == slides.lastIndex) onContinue() else step += 1
                        },
                    )
                }
            }
        }
    }
}

private data class PreferenceCategory(val name: String, val emoji: String, val color: Color)

private val preferenceCategories = listOf(
    PreferenceCategory("Tech", "💻", Color(0xFF2D5BFF)),
    PreferenceCategory("Fashion", "👟", Color(0xFF1A1A1A)),
    PreferenceCategory("Food", "🌮", Color(0xFF4CAF50)),
    PreferenceCategory("Beauty", "💅", Pink),
    PreferenceCategory("Gaming", "🕹️", Yellow),
    PreferenceCategory("Events", "🎫", Color(0xFFE91E63)),
    PreferenceCategory("Travel", "🛫", Color(0xFF00B4D8)),
    PreferenceCategory("All", "🔥", Color(0xFF7C4DFF)),
)

@Composable
fun PreferencesScreen(
    onContinue: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by rememberSaveable { mutableStateOf(setOf<String>()) }
    val haptics = LocalHapticFeedback.current
    val animationsEnabled = rememberAnimationsEnabled()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag(PriceErrorsTestTags.PREFERENCES_SCREEN),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 160.dp, y = (-160).dp)
                .size(360.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-130).dp, y = 100.dp)
                .size(260.dp)
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f), CircleShape),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            Column(
                modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 34.dp, bottom = 28.dp),
            ) {
                Row {
                    Text(
                        "What's your vibe",
                        style = MaterialTheme.typography.headlineMedium,
                        letterSpacing = (-1.2).sp,
                    )
                    Text(
                        "?",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    "Pick the deals you actually care about.\nWe'll show them first every time.",
                    modifier = Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.50f),
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(preferenceCategories, key = { it.name }) { category ->
                    val isSelected = category.name in selected
                    Surface(
                        onClick = {
                            selected = if (isSelected) selected - category.name else selected + category.name
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        modifier = Modifier
                            .shadow(
                                if (isSelected) 10.dp else 4.dp,
                                RoundedCornerShape(18.dp),
                                ambientColor = category.color.copy(alpha = if (isSelected) 0.35f else 0.05f),
                            )
                            .scale(if (isSelected && animationsEnabled) 1.02f else 1f)
                            .semantics(mergeDescendants = true) {
                                role = Role.Checkbox
                                this.selected = isSelected
                                contentDescription = "${category.name} deals"
                                stateDescription = if (isSelected) "Selected" else "Not selected"
                            }
                            .testTag("preference_${category.name.lowercase()}"),
                        color = if (isSelected) category.color else MaterialTheme.colorScheme.surface,
                        contentColor = if (isSelected) Color.White else AppDark,
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        if (isSelected) Color.White.copy(alpha = 0.25f) else category.color.copy(alpha = 0.10f),
                                        RoundedCornerShape(12.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(category.emoji, fontSize = 25.sp)
                            }
                            Text(
                                category.name,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 10.dp),
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                maxLines = 1,
                            )
                            if (isSelected) {
                                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (selected.isEmpty()) {
                        "Pick at least one to personalize your feed"
                    } else {
                        "${selected.size} selected · feed personalized 🎯"
                    },
                    color = if (selected.isEmpty()) {
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.40f)
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                ChunkyButton(
                    title = if (selected.isEmpty()) "SKIP FOR NOW" else "LET ME IN →",
                    background = if (selected.isEmpty()) Color(0xFFCCCCCC) else MaterialTheme.colorScheme.primary,
                    foreground = Color.White,
                    modifier = Modifier
                        .padding(horizontal = 22.dp, vertical = 10.dp)
                        .testTag(PriceErrorsTestTags.PREFERENCES_CONTINUE),
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onContinue(selected)
                    },
                )
            }
        }
    }
}

@Composable
private fun ChunkyButton(
    title: String,
    background: Color,
    foreground: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
        shape = RoundedCornerShape(22.dp),
        colors = ButtonDefaults.buttonColors(containerColor = background, contentColor = foreground),
    ) {
        Text(
            title,
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            letterSpacing = (-0.2).sp,
        )
    }
}
