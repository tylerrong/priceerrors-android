package app.priceerrors.feature.conversion

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.priceerrors.feature.alerts.AlertCategory
import app.priceerrors.feature.alerts.DealWatch
import app.priceerrors.feature.alerts.WatchKind
import app.priceerrors.feature.alerts.alertCategories
import app.priceerrors.ui.components.PigMark
import app.priceerrors.ui.theme.SpaceGrotesk

@Composable
fun PostPurchaseSetupScreen(
    initialCategories: Set<String>,
    initialMinimumDiscount: Int,
    isFinishing: Boolean,
    onFinish: (categories: Set<String>, watch: DealWatch, minimumDiscount: Int) -> Unit =
        { _, _, _ -> },
    modifier: Modifier = Modifier,
    onFinishWatches: ((categories: Set<String>, watches: List<DealWatch>, minimumDiscount: Int) -> Unit)? = null,
) {
    var selectedCategories by remember { mutableStateOf(initialCategories.filter { it != "All" }.toSet()) }
    var watchQuery by remember { mutableStateOf("") }
    var minimumDiscount by remember {
        mutableIntStateOf(initialMinimumDiscount.coerceIn(0, 90))
    }
    val primary = MaterialTheme.colorScheme.primary
    val canFinish = watchQuery.trim().isNotEmpty() && !isFinishing

    BackHandler(enabled = true) {
        // Matches iOS interactiveDismissDisabled: setup must be completed.
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .background(primary, RoundedCornerShape(15.dp)),
                contentAlignment = Alignment.Center,
            ) {
                PigMark(tint = Color.White, size = 34.dp)
            }
            Text(
                "Make Pro work for you.",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                letterSpacing = (-1).sp,
            )
            Text(
                "Choose what you care about, then we'll turn on the right alerts.",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "1. Pick your categories",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                userScrollEnabled = false,
            ) {
                items(alertCategories.filter { it.name != "All" }) { category: AlertCategory ->
                    val selected = category.name in selectedCategories
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (selected) primary else MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(13.dp),
                            )
                            .clickable {
                                selectedCategories = if (selected) {
                                    selectedCategories - category.name
                                } else {
                                    selectedCategories + category.name
                                }
                            }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "${category.icon} ${category.name}",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "2. Create your first watch",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
            TextField(
                value = watchQuery,
                onValueChange = { watchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp)),
                placeholder = { Text("AirPods, Nike, Target…") },
                shape = RoundedCornerShape(14.dp),
                minLines = 1,
                maxLines = 3,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Only alert me at $minimumDiscount%+ off",
                    modifier = Modifier.weight(1f),
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
                DiscountStepperButton(
                    increase = false,
                    enabled = minimumDiscount > 0,
                    onClick = { minimumDiscount -= 10 },
                )
                Spacer(modifier = Modifier.size(8.dp))
                DiscountStepperButton(
                    increase = true,
                    enabled = minimumDiscount < 90,
                    onClick = { minimumDiscount += 10 },
                )
            }
        }

        val finishInteraction = remember { MutableInteractionSource() }
        Button(
            onClick = {
                val watches = parsePostPurchaseWatches(watchQuery, minimumDiscount)
                if (onFinishWatches != null) {
                    onFinishWatches(selectedCategories, watches, minimumDiscount)
                } else {
                    onFinish(selectedCategories, watches.first(), minimumDiscount)
                }
            },
            enabled = canFinish,
            interactionSource = finishInteraction,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .conversionPressScale(finishInteraction),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = primary,
                contentColor = Color.White,
                disabledContainerColor = primary.copy(alpha = 0.45f),
            ),
        ) {
            if (isFinishing) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.size(8.dp))
            }
            Icon(Icons.Filled.Notifications, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                "Enable alerts & finish",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun ReviewMomentScreen(
    onRate: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(120.dp)
                .rotate(-5f)
                .background(primary, RoundedCornerShape(30.dp)),
            contentAlignment = Alignment.Center,
        ) {
            PigMark(tint = Color.White, size = 72.dp)
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            "Hope you found something good.",
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            letterSpacing = (-0.8).sp,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Thanks for using Price Errors.",
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
        )
        Spacer(modifier = Modifier.height(18.dp))
        Row(
            modifier = Modifier.semantics { contentDescription = "Five stars" },
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            repeat(5) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFC93D),
                    modifier = Modifier.size(31.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "A quick Play Store rating helps more deal hunters find us.",
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.50f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
        Spacer(modifier = Modifier.weight(1f))
        val rateInteraction = remember { MutableInteractionSource() }
        Button(
            onClick = onRate,
            interactionSource = rateInteraction,
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .height(56.dp)
                .conversionPressScale(rateInteraction),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = primary,
                contentColor = Color.White,
            ),
        ) {
            Text(
                "Rate Price Errors",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
        }
        TextButton(onClick = onDismiss) {
            Text(
                "Not now",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.50f),
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

fun parsePostPurchaseWatches(
    query: String,
    minimumDiscount: Int,
): List<DealWatch> =
    query
        .split(',', ';', '\n')
        .map(String::trim)
        .filter(String::isNotEmpty)
        .map { term ->
            DealWatch(
                kind = WatchKind.KEYWORD.wire,
                query = term,
                minimumDiscount = minimumDiscount.takeIf { it > 0 },
            )
        }

@Composable
private fun DiscountStepperButton(
    increase: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 0.72f else 0.24f)
    Surface(
        modifier = Modifier
            .size(32.dp)
            .semantics {
                contentDescription = if (increase) "Increase discount" else "Decrease discount"
            }
            .clickable(enabled = enabled, onClick = onClick),
        shape = CircleShape,
        color = Color.Transparent,
        contentColor = contentColor,
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.28f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (increase) Icons.Filled.Add else Icons.Filled.Remove,
                contentDescription = null,
                modifier = Modifier.size(17.dp),
            )
        }
    }
}

@Composable
private fun Modifier.conversionPressScale(
    interactionSource: MutableInteractionSource,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = tween(120),
        label = "conversionPressScale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
