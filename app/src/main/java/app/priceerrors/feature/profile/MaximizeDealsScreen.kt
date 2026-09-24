package app.priceerrors.feature.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.priceerrors.R
import app.priceerrors.core.network.AppLinks
import app.priceerrors.ui.theme.SpaceGrotesk

data class CashbackPartner(
    val id: String,
    val name: String,
    val blurb: String,
    val bonus: String?,
    val url: String,
    val logoRes: Int,
)

private val cashbackPartners = listOf(
    CashbackPartner(
        id = "rakuten",
        name = "Rakuten",
        blurb = "Start at Rakuten, shop at Amazon, Best Buy, and thousands of stores, then earn cash back.",
        bonus = "Get $50",
        url = AppLinks.RAKUTEN,
        logoRes = R.drawable.rakuten_logo,
    ),
    CashbackPartner(
        id = "capital_one_shopping",
        name = "Capital One Shopping",
        blurb = "Browser extension that finds coupons and better prices while you shop.",
        bonus = "Get $80",
        url = AppLinks.CAPITAL_ONE_SHOPPING,
        logoRes = R.drawable.capital_one_shopping_logo,
    ),
    CashbackPartner(
        id = "checkmate",
        name = "Checkmate",
        blurb = "Partner discount codes, guaranteed cashback on eligible orders, and price-drop alerts.",
        bonus = "Get $10",
        url = AppLinks.CHECKMATE,
        logoRes = R.drawable.checkmate_logo,
    ),
)

@Composable
fun MaximizeDealsScreen(
    accent: Color,
    onDismiss: () -> Unit,
    onOpenPartner: (CashbackPartner) -> Unit,
    onOpened: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) { onOpened() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Maximize deals",
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Done",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Stack cashback on top of price errors.",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    letterSpacing = (-0.5).sp,
                )
                Text(
                    text = "Before checkout, open one of these through the link below so tracking starts. Then claim the deal in priceerrors as usual.",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                )
            }

            Column(
                modifier = Modifier.padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                cashbackPartners.forEach { partner ->
                    CashbackPartnerRow(
                        partner = partner,
                        accent = accent,
                        onClick = { onOpenPartner(partner) },
                    )
                }
            }

            Text(
                text = "Use these referral links so signup bonuses can apply. Offers change and usually require new accounts plus qualifying activity (for example Rakuten's $50 spend, or Capital One Shopping's 90-day extension use). Capital One Shopping rewards are typically redeemable as shopping rewards / gift cards, not cash. Tracking isn't guaranteed on every store or price error.",
                modifier = Modifier.padding(horizontal = 18.dp),
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.40f),
            )
        }
    }
}

@Composable
private fun CashbackPartnerRow(
    partner: CashbackPartner,
    accent: Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Image(
                painter = painterResource(partner.logoRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = partner.name,
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                    )
                    partner.bonus?.let { bonus ->
                        Surface(
                            color = accent,
                            contentColor = Color.White,
                            shape = CircleShape,
                        ) {
                            Text(
                                text = bonus,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 0.3.sp,
                            )
                        }
                    }
                }
                Text(
                    text = partner.blurb,
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    maxLines = 3,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
