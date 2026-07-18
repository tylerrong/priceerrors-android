package app.priceerrors.feature.browse

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.priceerrors.core.model.Deal
import app.priceerrors.ui.components.DealArtwork
import app.priceerrors.ui.components.dealAccessibilityLabel
import app.priceerrors.ui.components.dealVisuals
import app.priceerrors.ui.components.formatPrice
import app.priceerrors.ui.accessibility.PriceErrorsTestTags
import app.priceerrors.ui.theme.SpaceGrotesk

private data class BrowseCategory(
    val name: String,
    val icon: String,
    val color: Color,
)

private val browseCategories = listOf(
    BrowseCategory(name = "All", icon = "🔥", color = Color(0xFF7C4DFF)),
    BrowseCategory(name = "Tech", icon = "💻", color = Color(0xFF2D5BFF)),
    BrowseCategory(name = "Beauty", icon = "💅", color = Color(0xFFFF7EB6)),
    BrowseCategory(name = "Food", icon = "🌮", color = Color(0xFF4CAF50)),
    BrowseCategory(name = "Fashion", icon = "👟", color = Color(0xFFC2185B)),
    BrowseCategory(name = "Travel", icon = "🛫", color = Color(0xFF2D5BFF)),
    BrowseCategory(name = "Events", icon = "🎫", color = Color(0xFFE91E63)),
    BrowseCategory(name = "Gaming", icon = "🕹️", color = Color(0xFFFFC93D)),
)

/**
 * Browse-by-category screen matching the iOS CategoriesView.
 *
 * The floating app navigation remains owned by the root shell, so this screen
 * reserves its final 140dp for that overlay instead of drawing another bar.
 */
@Composable
fun BrowseScreen(
    deals: List<Deal>,
    onDealSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var activeCategory by rememberSaveable { mutableStateOf("All") }
    var searchText by rememberSaveable { mutableStateOf("") }
    var showAll by rememberSaveable { mutableStateOf(false) }

    val filteredDeals = remember(deals, activeCategory, searchText) {
        filterBrowseDeals(deals, activeCategory, searchText)
    }
    val isFiltering = searchText.isNotEmpty() || activeCategory != "All"
    val shownDeals = remember(filteredDeals, isFiltering, showAll) {
        if (isFiltering || showAll) filteredDeals else filteredDeals.take(4)
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .testTag(PriceErrorsTestTags.BROWSE_SCREEN),
        contentPadding = PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            BrowseHeading(dealCount = deals.size)
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            BrowseSearchField(
                value = searchText,
                onValueChange = { searchText = it },
                onClear = { searchText = "" },
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            CategoryGrid(
                deals = deals,
                activeCategory = activeCategory,
                onCategorySelected = { activeCategory = it },
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            BrowseSectionHeader(
                title = when {
                    activeCategory != "All" -> "$activeCategory drops"
                    searchText.isNotEmpty() -> "Results"
                    else -> "Trending now"
                },
                isFiltering = isFiltering,
                matchCount = filteredDeals.size,
                canToggleAll = filteredDeals.size > 4,
                showingAll = showAll,
                onToggleAll = { showAll = !showAll },
            )
        }

        if (shownDeals.isEmpty() && isFiltering) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                NoBrowseResults()
            }
        } else {
            items(
                items = shownDeals,
                key = { deal -> deal.id },
            ) { deal ->
                BrowseDealCard(
                    deal = deal,
                    onClick = { onDealSelected(deal.id) },
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Spacer(modifier = Modifier.height(140.dp))
        }
    }
}

@Composable
private fun BrowseHeading(dealCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 12.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "Browse",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                lineHeight = 34.sp,
                letterSpacing = (-1.2).sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = ".",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                lineHeight = 34.sp,
                letterSpacing = (-1.2).sp,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = "Filter today’s $dealCount drops by vibe",
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun BrowseSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val view = LocalView.current
    val shape = RoundedCornerShape(16.dp)
    val contentColor = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, bottom = 18.dp)
            .shadow(elevation = 4.dp, shape = shape)
            .background(MaterialTheme.colorScheme.surface, shape)
            .testTag(PriceErrorsTestTags.BROWSE_SEARCH)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = contentColor.copy(alpha = 0.4f),
        )

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = "Search deals" },
            singleLine = true,
            textStyle = TextStyle(
                color = contentColor,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            decorationBox = { input ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            text = "Sephora, AirPods, Taylor Swift…",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = contentColor.copy(alpha = 0.22f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    input()
                }
            },
        )

        if (value.isNotEmpty()) {
            IconButton(
                onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        onClear()
                },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Cancel,
                    contentDescription = "Clear search",
                    modifier = Modifier.size(16.dp),
                    tint = contentColor.copy(alpha = 0.35f),
                )
            }
        }
    }
}

@Composable
private fun CategoryGrid(
    deals: List<Deal>,
    activeCategory: String,
    onCategorySelected: (String) -> Unit,
) {
    val view = LocalView.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        browseCategories.chunked(4).forEach { categories ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                categories.forEach { category ->
                    val count = if (category.name == "All") {
                        deals.size
                    } else {
                        deals.count { deal ->
                            deal.category.equals(category.name, ignoreCase = true) ||
                                (category.name == "Events" && deal.category.equals("Event", ignoreCase = true))
                        }
                    }
                    BrowseCategoryButton(
                        category = category,
                        count = count,
                        selected = activeCategory == category.name,
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            onCategorySelected(category.name)
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun BrowseCategoryButton(
    category: BrowseCategory,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeColor = if (category.name == "All") MaterialTheme.colorScheme.primary else category.color
    val containerColor by animateColorAsState(
        targetValue = if (selected) activeColor else MaterialTheme.colorScheme.surface,
        animationSpec = tween(durationMillis = 150),
        label = "browseCategoryContainer",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(durationMillis = 150),
        label = "browseCategoryContent",
    )
    val shape = RoundedCornerShape(18.dp)

    Column(
        modifier = modifier
            .shadow(elevation = if (selected) 8.dp else 2.dp, shape = shape)
            .background(containerColor, shape)
            .clip(shape)
            .semantics(mergeDescendants = true) {
                role = Role.RadioButton
                this.selected = selected
                contentDescription = "${category.name}, $count deals"
                stateDescription = if (selected) "Selected" else "Not selected"
            }
            .testTag("browse_category_${category.name.lowercase()}")
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = category.icon,
            fontSize = 20.sp,
            lineHeight = 22.sp,
        )
        Text(
            text = category.name,
            color = contentColor,
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            maxLines = 1,
        )
        Text(
            text = count.toString(),
            color = contentColor.copy(alpha = 0.7f),
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            lineHeight = 11.sp,
        )
    }
}

@Composable
private fun BrowseSectionHeader(
    title: String,
    isFiltering: Boolean,
    matchCount: Int,
    canToggleAll: Boolean,
    showingAll: Boolean,
    onToggleAll: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onBackground,
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 22.sp,
            letterSpacing = (-0.4).sp,
        )
        Spacer(modifier = Modifier.weight(1f))

        when {
            isFiltering -> Text(
                text = "$matchCount found",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                lineHeight = 15.sp,
            )

            canToggleAll -> Text(
                text = if (showingAll) "Show less ↑" else "See all →",
                modifier = Modifier
                    .size(width = 80.dp, height = 48.dp)
                    .clickable(role = Role.Button, onClick = onToggleAll)
                    .padding(top = 16.dp),
                color = MaterialTheme.colorScheme.primary,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                lineHeight = 15.sp,
            )
        }
    }
}

@Composable
private fun BrowseDealCard(
    deal: Deal,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visuals = dealVisuals(deal.category, isSystemInDarkTheme())
    val shape = RoundedCornerShape(18.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp, shape = shape)
            .background(visuals.background, shape)
            .clip(shape)
            .semantics(mergeDescendants = true) {
                contentDescription = dealAccessibilityLabel(deal)
            }
            .testTag("browse_deal_${deal.id}")
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        DealArtwork(
            deal = deal,
            portrait = false,
            modifier = Modifier
                .fillMaxWidth()
                .height(126.dp)
                .padding(8.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = deal.title,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                letterSpacing = (-0.2).sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (deal.priceInCents == 0L) {
                        "FREE"
                    } else {
                        formatPrice(deal.priceInCents, deal.currencyCode)
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = 19.sp,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "-${deal.discountPercent}%",
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                )
            }
        }
    }
}

@Composable
private fun NoBrowseResults() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "🔍", fontSize = 40.sp, lineHeight = 44.sp)
        Text(
            text = "No deals match",
            color = MaterialTheme.colorScheme.onBackground,
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            lineHeight = 19.sp,
        )
        Text(
            text = "Try a different category or search.",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
    }
}
