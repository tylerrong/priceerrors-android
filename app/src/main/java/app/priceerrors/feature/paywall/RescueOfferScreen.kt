package app.priceerrors.feature.paywall

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.priceerrors.ui.components.PigMark
import app.priceerrors.ui.theme.SpaceGrotesk

@Composable
fun RescueOfferScreen(
    monthlyPrice: String,
    renewalPrice: String,
    usesIntroductoryPrice: Boolean,
    isLoading: Boolean,
    message: String?,
    onDismiss: () -> Unit,
    onPurchase: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.32f)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(470.dp),
            color = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        ) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Surface(
                        modifier = Modifier.size(34.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Close offer",
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .size(82.dp)
                        .background(primary, RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    PigMark(tint = Color.White, size = 48.dp)
                }
                Text(
                    "Not ready for a year?",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 27.sp,
                    letterSpacing = (-0.8).sp,
                    textAlign = TextAlign.Center,
                )
                Text(
                    if (usesIntroductoryPrice) {
                        "Get your first month for 50% off."
                    } else {
                        "Unlock Price Errors Pro for less."
                    },
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.60f),
                    textAlign = TextAlign.Center,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        monthlyPrice,
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp,
                        color = primary,
                    )
                    Text(
                        if (usesIntroductoryPrice) "first month" else "per month",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.50f),
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                val purchaseInteraction = remember { MutableInteractionSource() }
                Button(
                    onClick = onPurchase,
                    enabled = !isLoading,
                    interactionSource = purchaseInteraction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .rescuePressScale(purchaseInteraction),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primary,
                        contentColor = Color.White,
                    ),
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                    Text(
                        if (usesIntroductoryPrice) "Claim 50% off" else "Unlock Pro",
                        modifier = Modifier.padding(start = if (isLoading) 8.dp else 0.dp),
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                }
                Text(
                    if (usesIntroductoryPrice) {
                        "Then $renewalPrice/month. Cancel anytime."
                    } else {
                        "Renews at $renewalPrice/month. Cancel anytime."
                    },
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                    textAlign = TextAlign.Center,
                )
                if (!message.isNullOrBlank()) {
                    Text(
                        message,
                        color = MaterialTheme.colorScheme.error,
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun Modifier.rescuePressScale(
    interactionSource: MutableInteractionSource,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = tween(120),
        label = "rescuePressScale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
