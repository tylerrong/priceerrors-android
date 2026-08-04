package app.priceerrors.feature.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.priceerrors.ui.theme.AppDark
import app.priceerrors.ui.theme.NotWorkingRed
import app.priceerrors.ui.theme.SpaceGrotesk

internal enum class ProfileSheet {
    HELP,
    PRIVACY,
    TERMS,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditNameSheet(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var draft by remember(currentName) { mutableStateOf(currentName) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Display name",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                letterSpacing = (-0.4).sp,
            )
            TextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Your name") },
                shape = RoundedCornerShape(12.dp),
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            )
            Button(
                onClick = { onSave(draft.trim()) },
                enabled = draft.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppDark,
                    contentColor = Color.White,
                    disabledContainerColor = AppDark.copy(alpha = 0.5f),
                ),
            ) {
                Text(
                    text = "Save",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
            }
        }
    }
}

@Composable
internal fun ProfileInformationSheet(
    sheet: ProfileSheet,
    accent: Color,
    onDismiss: () -> Unit,
    onDeleteAccount: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenAccountDeletion: () -> Unit,
) {
    when (sheet) {
        ProfileSheet.HELP -> HelpSupportSheet(
            accent = accent,
            onDismiss = onDismiss,
            onDeleteAccount = onDeleteAccount,
            onOpenSupport = onOpenSupport,
            onOpenAccountDeletion = onOpenAccountDeletion,
        )
        ProfileSheet.PRIVACY -> PolicySheet(
            title = "Privacy Policy",
            sections = privacySections,
            accent = accent,
            onDismiss = onDismiss,
            onOpenOnlinePolicy = onOpenPrivacy,
        )
        ProfileSheet.TERMS -> PolicySheet(
            title = "Terms of Service",
            sections = termsSections,
            accent = accent,
            onDismiss = onDismiss,
            onOpenOnlinePolicy = onOpenTerms,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HelpSupportSheet(
    accent: Color,
    onDismiss: () -> Unit,
    onDeleteAccount: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenAccountDeletion: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var expandedIndex by remember { mutableIntStateOf(-1) }
    var showDeleteAlert by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f),
        ) {
            SheetHeader(title = "Help & Support", accent = accent, onDismiss = onDismiss)
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = 40.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Button(
                        onClick = onOpenSupport,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor = Color.White,
                        ),
                    ) {
                        Text("OPEN SUPPORT")
                    }
                }
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Column {
                            SectionKicker("FAQ")
                            faqItems.forEachIndexed { index, faq ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            expandedIndex = if (expandedIndex == index) -1 else index
                                        }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = faq.first,
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.labelLarge,
                                        )
                                        Icon(
                                            imageVector = if (expandedIndex == index) {
                                                Icons.Filled.KeyboardArrowUp
                                            } else {
                                                Icons.Filled.KeyboardArrowDown
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                                        )
                                    }
                                    AnimatedVisibility(visible = expandedIndex == index) {
                                        Text(
                                            text = faq.second,
                                            modifier = Modifier.padding(top = 10.dp),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                                            lineHeight = 19.sp,
                                        )
                                    }
                                }
                                if (index < faqItems.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 16.dp),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Column {
                            SectionKicker("ACCOUNT")
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showDeleteAlert = true }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = null,
                                    tint = NotWorkingRed,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Delete Local Profile",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = NotWorkingRed,
                                    )
                                    Text(
                                        text = "Removes profile and saved state from this Android device",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f),
                                    )
                                }
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 16.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                            )
                            TextButton(
                                onClick = onOpenAccountDeletion,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("ONLINE ACCOUNT-DELETION HELP")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteAlert) {
        AlertDialog(
            onDismissRequest = { showDeleteAlert = false },
            title = { Text("Delete your account?") },
            text = {
                Text(
                    "This deletes your PriceErrors account and its server-side data, " +
                        "and removes everything this app saved on this device. " +
                        "This can't be undone.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteAlert = false
                    onDeleteAccount()
                    onDismiss()
                }) {
                    Text("Delete Local Data", color = NotWorkingRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAlert = false }) { Text("Cancel") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PolicySheet(
    title: String,
    sections: List<PolicySectionData>,
    accent: Color,
    onDismiss: () -> Unit,
    onOpenOnlinePolicy: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f),
        ) {
            SheetHeader(title = title, accent = accent, onDismiss = onDismiss)
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                itemsIndexed(sections) { _, section ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = section.title,
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Text(
                                text = section.content,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                                lineHeight = 20.sp,
                            )
                        }
                    }
                }
                item {
                    Button(
                        onClick = onOpenOnlinePolicy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor = Color.White,
                        ),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text("VIEW CURRENT POLICY ONLINE")
                    }
                }
                item {
                    Text(
                        text = "Last updated: April 2026",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                    )
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun SheetHeader(
    title: String,
    accent: Color,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(48.dp))
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        )
        TextButton(onClick = onDismiss) {
            Text(
                text = "Done",
                color = accent,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SectionKicker(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(start = 16.dp, top = 18.dp, end = 16.dp, bottom = 8.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f),
        letterSpacing = 1.5.sp,
    )
}

private data class PolicySectionData(
    val title: String,
    val content: String,
)

private val faqItems = listOf(
    "What is a price error?" to "A price error is when a retailer accidentally lists a product far below its intended price. They're usually fixed within hours, so you need to act fast.",
    "How do I claim a deal?" to "Tap any deal to open it, then follow the steps shown. Complete your order before the retailer notices and fixes the price.",
    "Will the retailer honor my order?" to "There is no guarantee, but many retailers honor orders after payment is processed. Always use a payment method with purchase protection.",
    "How often are new deals posted?" to "New deals are posted as they're discovered, usually several times a day. Enable push notifications for time-sensitive drops.",
    "How do push notifications work?" to "When a new deal is found, PriceErrors sends an alert. Keep notifications enabled in your device settings so you do not miss it.",
    "Why did a deal disappear?" to "Deals are removed once the price is fixed or after 24 hours. If a deal is gone, the window has closed.",
    "What do the fire emojis mean?" to "🔥 is a solid deal, 🔥🔥 is getting attention, and 🔥🔥🔥 is blowing up.",
    "Is this app free?" to "PriceErrors is a subscription app — a membership unlocks the full feed of price errors, the community, and real-time alerts. New members start with a free trial, and you can cancel any time from Google Play → Menu → Subscriptions.",
)

private val privacySections = listOf(
    PolicySectionData("Overview", "PriceErrors is built with your privacy in mind. We collect only what is necessary to make the app work, we do not sell your data, and we do not run ads."),
    PolicySectionData("Current Android Build", "Your account, the deals you open, and your working/not-working votes are synchronized with the PriceErrors account service. Appearance preferences, saved deals, blocks, and pending community reports remain on this device. Debug demo accounts are not real PriceErrors accounts."),
    PolicySectionData("Google Sign-In", "Credential Manager obtains a Google ID token only after you choose a Google account. That token is exchanged with the PriceErrors account service, which creates your account and issues the session used for all server requests."),
    PolicySectionData("Community", "Community sample posts and posts created in the current Android build are device-local. Reports are queued locally and clearly marked as not yet delivered to PriceErrors staff. Do not include private personal information."),
    PolicySectionData("What We Don't Collect", "We do not collect your location. We do not track browsing or purchases outside the app. We do not use advertising SDKs that profile you, and we do not sell your data."),
    PolicySectionData("How Your Data Is Stored", "Local Android data is stored in private application storage with backup disabled, including your session token. Account and deal-activity data is stored in Supabase. See the published online policy for full details."),
    PolicySectionData("Push Notifications", "If you enable alerts and Firebase is configured, an FCM token is generated and retained locally pending backend registration. You can disable alerts at any time."),
    PolicySectionData("Purchases", "Google Play processes subscription purchases. The app reads purchase state and acknowledges completed purchases; server-side verification must be connected before production."),
    PolicySectionData("Data Deletion", "Delete Account removes your PriceErrors account and its server-side data, and clears this device's local profile and deal state. The online account-deletion page offers the same process from the web."),
    PolicySectionData("Children's Privacy", "PriceErrors is not directed at children under 13 and does not knowingly collect their personal information."),
    PolicySectionData("Changes to This Policy", "We may update this policy from time to time. Continued use after changes constitutes acceptance of the updated policy."),
)

private val termsSections = listOf(
    PolicySectionData("Acceptance of Terms", "By downloading or using PriceErrors, you agree to these Terms of Service. If you do not agree, do not use the app."),
    PolicySectionData("What PriceErrors Is", "PriceErrors is an informational app for deal and price-error alerts. We are not a retailer, marketplace, financial advisor, or payment intermediary."),
    PolicySectionData("Scraped Deals — No Warranty", "Feed deals are informational only. We do not guarantee accuracy, availability, or that a retailer will honor a listed price."),
    PolicySectionData("User-Posted Deals — No Verification", "Community content may be inaccurate, expired, misleading, or fraudulent. Interact with it at your own risk."),
    PolicySectionData("Your Responsibility for Content You Post", "You must not post unlawful, misleading, harmful, or rights-infringing content. You are solely responsible for what you submit."),
    PolicySectionData("No Financial or Legal Advice", "Nothing in PriceErrors constitutes financial, legal, or purchasing advice. Independently verify every deal before purchasing."),
    PolicySectionData("Limitation of Liability", "To the maximum extent permitted by law, PriceErrors is not liable for losses arising from inaccurate deals, user content, purchases, or unauthorized access."),
    PolicySectionData("Prohibited Uses", "Do not post fraudulent deals, harmful content, spam, manipulate the platform, bypass security, or use the app commercially without permission."),
    PolicySectionData("Account Termination", "We may suspend or terminate accounts that violate these terms or use the app in a harmful way."),
    PolicySectionData("Governing Law", "These terms are governed by the laws of California. Disputes shall be resolved in Los Angeles County."),
    PolicySectionData("Contact", "Questions about these terms can be sent through Help & Support."),
)
