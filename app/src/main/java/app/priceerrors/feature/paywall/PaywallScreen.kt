package app.priceerrors.feature.paywall

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.priceerrors.R
import app.priceerrors.core.analytics.MetaMeasurement
import app.priceerrors.ui.accessibility.PriceErrorsTestTags
import app.priceerrors.ui.accessibility.rememberAnimationsEnabled
import app.priceerrors.ui.theme.SpaceGrotesk
import app.priceerrors.ui.theme.isAppInDarkTheme
import java.util.Locale
import kotlinx.coroutines.delay

enum class PaywallPlan(
    val title: String,
) {
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly"),
}

data class PaywallPricing(
    val weekly: String = "$4.99",
    val monthly: String = "$9.99",
    val yearly: String = "$49.99",
) {
    fun price(plan: PaywallPlan): String = when (plan) {
        PaywallPlan.WEEKLY -> weekly
        PaywallPlan.MONTHLY -> monthly
        PaywallPlan.YEARLY -> yearly
    }

    fun subtitle(plan: PaywallPlan): String = when (plan) {
        PaywallPlan.WEEKLY -> "Billed weekly"
        PaywallPlan.MONTHLY -> "Billed monthly"
        PaywallPlan.YEARLY -> yearlyPerWeekLabel()
    }

    fun footer(plan: PaywallPlan): String = when (plan) {
        PaywallPlan.WEEKLY -> "Billed $weekly weekly · Cancel anytime."
        PaywallPlan.MONTHLY -> "Billed $monthly monthly · Cancel anytime."
        PaywallPlan.YEARLY -> "Billed $yearly yearly · Cancel anytime."
    }

    fun yearlyBadge(): String {
        val weeklyAmount = parsePrice(weekly) ?: return "BEST VALUE"
        val yearlyAmount = parsePrice(yearly) ?: return "BEST VALUE"
        if (weeklyAmount <= 0.0) return "BEST VALUE"
        val annualized = weeklyAmount * 52.0
        val savings = (((annualized - yearlyAmount) / annualized) * 100.0).toInt().coerceAtLeast(0)
        return if (savings >= 8) "SAVE $savings%" else "BEST VALUE"
    }

    private fun yearlyPerWeekLabel(): String {
        val yearlyAmount = parsePrice(yearly) ?: return "Best value"
        if (yearlyAmount <= 0.0) return "Best value"
        return "${formatCurrency(yearlyAmount / 52.0)} / week"
    }
}

private data class PaywallShowcaseDeal(
    val id: String,
    val imageRes: Int,
    val title: String,
    val listPrice: String,
    val discountLabel: String,
)

private val ShowcaseDeals = listOf(
    PaywallShowcaseDeal("ps5", R.drawable.onboarding_ps5, "PlayStation 5", "$599", "85% OFF"),
    PaywallShowcaseDeal("chipotle", R.drawable.onboarding_burrito, "Chipotle Burrito", "$10+", "FREE"),
    PaywallShowcaseDeal("watch", R.drawable.onboarding_watch, "Apple Watch", "$649", "42% OFF"),
    PaywallShowcaseDeal("computer", R.drawable.onboarding_computer, "MacBook Deal", "$2,153", "93% OFF"),
    PaywallShowcaseDeal("headphones", R.drawable.onboarding_headphones, "JBL Headphones", "$500", "64% OFF"),
    PaywallShowcaseDeal("camera", R.drawable.onboarding_camera, "Instax Camera", "$105", "44% OFF"),
    PaywallShowcaseDeal("eros", R.drawable.onboarding_eros, "Versace Eros", "$120", "68% OFF"),
    PaywallShowcaseDeal("dunkin", R.drawable.onboarding_dunkin, "Dunkin Drink", "$7", "FREE"),
)

private val Accent = Color(0xFF00C897)
private val AccentDark = Color(0xFF0A5C47)
private val SoftTop = Color(0xFFDDF6EC)
private val SoftMid = Color(0xFFEEFBF6)

@Composable
fun PaywallScreen(
    allowDismiss: Boolean,
    onDismiss: () -> Unit,
    onPurchase: (PaywallPlan) -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
    pricing: PaywallPricing = PaywallPricing(),
    isLoading: Boolean = false,
    message: String? = null,
    allowDebugBypass: Boolean = false,
    onDebugBypass: () -> Unit = {},
    onOpenTerms: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    analyticsSource: String = "paywall",
    onPlanSelected: (PaywallPlan) -> Unit = {},
    onboardingStep: Int? = null,
    onboardingTotalSteps: Int? = null,
) {
    var selectedPlan by rememberSaveable { mutableStateOf(PaywallPlan.YEARLY) }
    val appBackground = MaterialTheme.colorScheme.background
    val gradientColors = if (isAppInDarkTheme) {
        listOf(
            lerp(appBackground, Accent, 0.18f),
            lerp(appBackground, Accent, 0.08f),
            appBackground,
        )
    } else {
        listOf(SoftTop, SoftMid, appBackground)
    }

    LaunchedEffect(analyticsSource) {
        MetaMeasurement.logPaywallViewed(analyticsSource)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(gradientColors))
            .statusBarsPadding()
            .testTag(PriceErrorsTestTags.PAYWALL_SCREEN),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (allowDismiss) {
                Surface(
                    modifier = Modifier.size(34.dp),
                    shape = CircleShape,
                    color = AccentDark.copy(alpha = 0.08f),
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Close",
                            modifier = Modifier.size(15.dp),
                            tint = AccentDark.copy(alpha = 0.55f),
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.size(34.dp))
            }
            if (
                onboardingStep != null &&
                onboardingTotalSteps != null &&
                onboardingTotalSteps > 0
            ) {
                Spacer(modifier = Modifier.width(12.dp))
                Row(
                    modifier = Modifier
                        .width(112.dp)
                        .semantics {
                            contentDescription =
                                "Onboarding progress " +
                                "${(onboardingStep.coerceIn(0, onboardingTotalSteps - 1)) + 1} " +
                                "of $onboardingTotalSteps"
                        }
                        .testTag("paywall_progress"),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    repeat(onboardingTotalSteps) { progressIndex ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .background(
                                    AccentDark.copy(
                                        alpha = if (progressIndex <= onboardingStep) 1f else 0.16f,
                                    ),
                                    CircleShape,
                                ),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.size(34.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Members save hundreds every month.",
                    color = AccentDark,
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    lineHeight = 32.sp,
                    letterSpacing = (-0.8).sp,
                )
                Text(
                    "Unlimited deals, instant alerts, and the link to buy.",
                    color = AccentDark.copy(alpha = 0.62f),
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
            }

            PaywallShowcaseCarousel()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("❧", color = Accent.copy(alpha = 0.85f), fontSize = 25.sp)
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "1,000+",
                        color = AccentDark,
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                    )
                    Text(
                        "people are saving with PriceErrors",
                        color = AccentDark.copy(alpha = 0.58f),
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                    )
                }
                Text(
                    "❧",
                    modifier = Modifier.graphicsLayer { scaleX = -1f },
                    color = Accent.copy(alpha = 0.85f),
                    fontSize = 25.sp,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    PaywallPlan.entries.forEach { plan ->
                        CompactPlanCard(
                            plan = plan,
                            price = pricing.price(plan),
                            subtitle = pricing.subtitle(plan),
                            badge = if (plan == PaywallPlan.YEARLY) pricing.yearlyBadge() else null,
                            selected = plan == selectedPlan,
                            onClick = {
                                selectedPlan = plan
                                onPlanSelected(plan)
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Text(
                    pricing.footer(selectedPlan),
                    modifier = Modifier.fillMaxWidth(),
                    color = AccentDark.copy(alpha = 0.45f),
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val purchaseInteraction = remember { MutableInteractionSource() }
                Button(
                    onClick = { onPurchase(selectedPlan) },
                    enabled = !isLoading,
                    interactionSource = purchaseInteraction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .pressScale(purchaseInteraction)
                        .testTag(PriceErrorsTestTags.PAYWALL_PURCHASE),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Accent,
                        contentColor = Color.White,
                    ),
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        if (isLoading) "Working…" else "Unlock my deals",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                    )
                }
                if (!message.isNullOrBlank()) {
                    Text(
                        text = message,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        color = Color(0xFFB00020),
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Terms of Use",
                        modifier = Modifier.clickable(onClick = onOpenTerms),
                        color = AccentDark.copy(alpha = 0.45f),
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                    )
                    Text("·", color = AccentDark.copy(alpha = 0.30f))
                    Text(
                        "Privacy Policy",
                        modifier = Modifier.clickable(onClick = onOpenPrivacy),
                        color = AccentDark.copy(alpha = 0.45f),
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                    )
                    Text("·", color = AccentDark.copy(alpha = 0.30f))
                    Text(
                        "Restore purchase",
                        modifier = Modifier.clickable(enabled = !isLoading, onClick = onRestore),
                        color = AccentDark.copy(alpha = 0.45f),
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                    )
                }
                if (allowDebugBypass) {
                    TextButton(onClick = onDebugBypass) {
                        Text("Continue in local demo", color = AccentDark.copy(alpha = 0.70f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactPlanCard(
    plan: PaywallPlan,
    price: String,
    subtitle: String,
    badge: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
                .semantics(mergeDescendants = true) {
                    role = Role.RadioButton
                    this.selected = selected
                    contentDescription = listOf(plan.title, price, subtitle).joinToString(", ")
                    stateDescription = if (selected) "Selected" else "Not selected"
                }
                .testTag("plan_${plan.name.lowercase()}")
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            border = BorderStroke(
                if (selected) 2.dp else 1.dp,
                if (selected) AccentDark else AccentDark.copy(alpha = 0.12f),
            ),
            shadowElevation = if (selected) 8.dp else 0.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        plan.title,
                        color = AccentDark,
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(
                        imageVector = if (selected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                        contentDescription = null,
                        tint = if (selected) AccentDark else AccentDark.copy(alpha = 0.22f),
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    price,
                    color = AccentDark,
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    subtitle,
                    color = AccentDark.copy(alpha = 0.55f),
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (badge != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-8).dp, y = (-10).dp),
                color = AccentDark,
                contentColor = Color.White,
                shape = CircleShape,
            ) {
                Text(
                    badge,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    letterSpacing = 0.6.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun PaywallShowcaseCarousel() {
    var index by remember { mutableIntStateOf(0) }
    val animationsEnabled = rememberAnimationsEnabled()

    LaunchedEffect(ShowcaseDeals.size, animationsEnabled) {
        if (!animationsEnabled) return@LaunchedEffect
        while (ShowcaseDeals.size > 1) {
            delay(3_500)
            index = (index + 1) % ShowcaseDeals.size
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = AccentDark.copy(alpha = 0.06f),
                spotColor = AccentDark.copy(alpha = 0.08f),
            )
            .background(Color.White.copy(alpha = 0.94f), RoundedCornerShape(22.dp))
            .border(1.dp, Accent.copy(alpha = 0.18f), RoundedCornerShape(22.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AnimatedContent(
            targetState = index,
            modifier = Modifier.fillMaxWidth(),
            transitionSpec = {
                (
                    slideInHorizontally(
                        animationSpec = tween(350),
                        initialOffsetX = { it / 5 },
                    ) + fadeIn(tween(350))
                    ).togetherWith(
                    slideOutHorizontally(
                        animationSpec = tween(350),
                        targetOffsetX = { -it / 5 },
                    ) + fadeOut(tween(350)),
                )
            },
            label = "showcaseDeal",
        ) { dealIndex ->
            PaywallShowcaseDealContent(ShowcaseDeals[dealIndex])
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShowcaseDeals.indices.forEach { dot ->
                val width by animateDpAsState(
                    targetValue = if (dot == index) 16.dp else 6.dp,
                    label = "dotWidth",
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .width(width)
                        .height(6.dp)
                        .background(
                            if (dot == index) AccentDark else AccentDark.copy(alpha = 0.16f),
                            CircleShape,
                        ),
                )
            }
        }
    }
}

@Composable
private fun PaywallShowcaseDealContent(deal: PaywallShowcaseDeal) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(deal.imageRes),
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            contentScale = ContentScale.Fit,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                deal.title,
                color = AccentDark,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    deal.listPrice,
                    color = AccentDark.copy(alpha = 0.35f),
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    textDecoration = TextDecoration.LineThrough,
                )
                Box(
                    modifier = Modifier
                        .width(58.dp)
                        .height(24.dp)
                        .background(Accent.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .border(
                            1.dp,
                            Accent.copy(alpha = 0.45f),
                            RoundedCornerShape(8.dp),
                        ),
                )
                Surface(
                    color = Accent,
                    contentColor = AccentDark,
                    shape = CircleShape,
                ) {
                    Text(
                        deal.discountLabel,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 0.4.sp,
                        maxLines = 1,
                    )
                }
            }
            Text(
                "Unlock to see price + buy link",
                color = AccentDark.copy(alpha = 0.58f),
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = tween(120),
        label = "pressScale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

private fun parsePrice(string: String): Double? {
    val digits = string.filter { it.isDigit() || it == '.' || it == ',' }
    if (digits.isEmpty()) return null
    return digits.replace(',', '.').toDoubleOrNull()
}

private fun formatCurrency(value: Double): String =
    if (value >= 10.0) {
        String.format(Locale.US, "$%.0f", value)
    } else {
        String.format(Locale.US, "$%.2f", value)
    }
