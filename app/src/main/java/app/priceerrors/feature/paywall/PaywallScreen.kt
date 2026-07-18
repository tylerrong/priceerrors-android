package app.priceerrors.feature.paywall

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.priceerrors.ui.theme.SpaceGrotesk
import app.priceerrors.ui.accessibility.PriceErrorsTestTags

enum class PaywallPlan(
    val title: String,
    val trial: String?,
    val badge: String?,
) {
    YEARLY("Yearly", "7-day free trial", "BEST VALUE"),
    MONTHLY("Monthly", "3-day free trial", null),
    WEEKLY("Weekly", null, null),
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
        PaywallPlan.WEEKLY -> "then $weekly/week"
        PaywallPlan.MONTHLY -> "then $monthly/month"
        PaywallPlan.YEARLY -> "then $yearly/year"
    }
}

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
) {
    var selectedPlan by rememberSaveable { mutableStateOf(PaywallPlan.YEARLY) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.04f))
            .statusBarsPadding()
            .testTag(PriceErrorsTestTags.PAYWALL_SCREEN),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (allowDismiss) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Close",
                            modifier = Modifier.size(17.dp),
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.60f),
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onRestore, enabled = !isLoading) {
                Text(
                    "RESTORE",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.70f),
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(6.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                    Text(
                        "PRICE ERRORS PRO",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium,
                        letterSpacing = 2.sp,
                    )
                }
                Text(
                    "every deal,\nevery day.",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 40.sp,
                    lineHeight = 42.sp,
                    letterSpacing = (-1.8).sp,
                )
                Text(
                    "Subscribe to unlock all deals, the community, and real-time alerts the second something breaks.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Benefit("Full access to every deal, every day")
                Benefit("Community submissions & posting")
                Benefit("Real-time push the moment a glitch drops")
                Benefit("Save deals across devices")
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PaywallPlan.entries.forEach { plan ->
                    PlanCard(
                        plan = plan,
                        displayPrice = pricing.price(plan),
                        displaySubtitle = pricing.subtitle(plan),
                        selected = plan == selectedPlan,
                        onClick = { selectedPlan = plan },
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(
                    onClick = { onPurchase(selectedPlan) },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .testTag(PriceErrorsTestTags.PAYWALL_PURCHASE),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                    ),
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            when (selectedPlan) {
                                PaywallPlan.WEEKLY -> "Start Weekly Plan"
                                PaywallPlan.MONTHLY -> "Start 3-day free trial"
                                PaywallPlan.YEARLY -> "Start 7-day free trial"
                            },
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            letterSpacing = (-0.3).sp,
                        )
                    }
                }
                if (!message.isNullOrBlank()) {
                    Text(
                        text = message,
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                    )
                }
                Text(
                    text = when (selectedPlan) {
                        PaywallPlan.WEEKLY -> "You'll be charged ${pricing.weekly}/week. Subscription auto-renews unless cancelled before renewal. Manage in Google Play → Payments & subscriptions."
                        PaywallPlan.MONTHLY -> "After your 3-day free trial, you'll be charged ${pricing.monthly}/month. Subscription auto-renews unless cancelled before renewal. Manage in Google Play → Payments & subscriptions."
                        PaywallPlan.YEARLY -> "After your 7-day free trial, you'll be charged ${pricing.yearly}/year. Subscription auto-renews unless cancelled before renewal. Manage in Google Play → Payments & subscriptions."
                    },
                    modifier = Modifier.padding(top = 10.dp),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.50f),
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    textAlign = TextAlign.Center,
                )
                Row(
                    modifier = Modifier.padding(top = 10.dp, bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        "Terms",
                        modifier = Modifier.clickable(onClick = onOpenTerms),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text("·", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.30f))
                    Text(
                        "Privacy",
                        modifier = Modifier.clickable(onClick = onOpenPrivacy),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (allowDebugBypass) {
                    TextButton(onClick = onDebugBypass) {
                        Text("Continue in local demo")
                    }
                }
            }
        }
    }
}

@Composable
private fun Benefit(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text,
            modifier = Modifier.weight(1f),
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
        )
    }
}

@Composable
private fun PlanCard(
    plan: PaywallPlan,
    displayPrice: String,
    displaySubtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                role = Role.RadioButton
                this.selected = selected
                contentDescription = listOfNotNull(plan.title, displayPrice, plan.trial, displaySubtitle)
                    .joinToString(", ")
                stateDescription = if (selected) "Selected" else "Not selected"
            }
            .testTag("plan_${plan.name.lowercase()}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(Color.Transparent, CircleShape)
                    .then(
                        Modifier.background(
                            if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            CircleShape,
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (!selected) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = CircleShape,
                        color = Color.Transparent,
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.20f)),
                    ) {}
                } else {
                    Box(Modifier.size(8.dp).background(Color.White, CircleShape))
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(plan.title, style = MaterialTheme.typography.titleLarge, fontSize = 17.sp)
                    plan.badge?.let {
                        Surface(color = MaterialTheme.colorScheme.primary, contentColor = Color.White, shape = CircleShape) {
                            Text(
                                it,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                letterSpacing = 1.2.sp,
                            )
                        }
                    }
                }
                plan.trial?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    )
                }
                Text(
                    displaySubtitle,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.50f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(displayPrice, style = MaterialTheme.typography.titleLarge)
        }
    }
}
