package app.priceerrors.feature.profile

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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

@Composable
private fun HelpSupportSheet(
    accent: Color,
    onDismiss: () -> Unit,
    onDeleteAccount: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenAccountDeletion: () -> Unit,
) {
    var expandedIndex by remember { mutableIntStateOf(-1) }
    var showDeleteAlert by remember { mutableStateOf(false) }
    @Suppress("UNUSED_VARIABLE")
    val unusedOnOpenAccountDeletion = onOpenAccountDeletion

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenSupport),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Column {
                            SectionKicker("CONTACT")
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Email,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Email support",
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                    Text(
                                        text = "priceerrorsapp@gmail.com",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                    modifier = Modifier.size(16.dp),
                                )
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
                                        text = "Delete Account & Data",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = NotWorkingRed,
                                    )
                                    Text(
                                        text = "Permanently removes your profile and saved deals",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f),
                                    )
                                }
                            }
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
            title = { Text("Delete Account") },
            text = {
                Text("This will permanently delete your account and all associated data. This cannot be undone.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteAlert = false
                    onDeleteAccount()
                    onDismiss()
                }) {
                    Text("Delete", color = NotWorkingRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAlert = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun PolicySheet(
    title: String,
    sections: List<PolicySectionData>,
    accent: Color,
    onDismiss: () -> Unit,
    onOpenOnlinePolicy: () -> Unit,
) {
    @Suppress("UNUSED_VARIABLE")
    val unusedOnOpenOnlinePolicy = onOpenOnlinePolicy
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
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
                        Text(
                            text = "Last updated: August 2026",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                        )
                        Spacer(Modifier.height(32.dp))
                    }
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
    "What is a price error?" to "A price error is when a retailer accidentally lists a product far below its intended price — a typo, system glitch, or misconfigured discount. They're usually fixed within hours, so you need to act fast.",
    "How do I claim a deal?" to "Tap any deal to open it, then follow the steps shown. Most price errors apply automatically at checkout — no code needed. Complete your order before the retailer notices and fixes the price.",
    "Will the retailer honor my order?" to "Most retailers honor price errors to protect their reputation, especially once payment is processed. There's no guarantee, but the majority of orders go through. Always use a credit card so you can dispute if needed.",
    "How often are new deals posted?" to "New deals are posted as they're discovered — usually several times a day. Enable push notifications so you're alerted the moment a new error drops.",
    "How do push notifications work?" to "Turn on All Deal Alerts for every qualifying drop. Category and keyword watchlists make matching notifications stand out without sending duplicates. Turn All Deal Alerts off to receive only specialized matches.",
    "Why did a deal disappear?" to "Deals are removed once the price error is fixed by the retailer or after 24 hours. If a deal is gone, the window has closed — but new ones drop regularly.",
    "Is this app free?" to "PriceErrors is a subscription app — a membership unlocks the full feed of price errors and personalized real-time alerts. You can cancel any time from Google Play → profile icon → Payments & subscriptions → Subscriptions.",
)

private val privacySections = listOf(
    PolicySectionData(
        "Overview",
        "PriceErrors is built with your privacy in mind. We collect only what's necessary to make the app work, and we don't sell your data.",
    ),
    PolicySectionData(
        "What We Collect",
        "When you sign in with Google, Google Credential Manager provides your name, email address, and an ID token after you choose an account. The token is exchanged with the PriceErrors account service solely to identify your account.\n\nWe store your saved deals, category preferences, custom watchlists, and alert settings so your experience stays personalized.\n\nWe store your device's Firebase Cloud Messaging (FCM) token to deliver deal alerts.",
    ),
    PolicySectionData(
        "What We Don't Collect",
        "We do not collect your location.\nWe do not track your browsing or purchase behavior outside the app.\nWe do not sell your data.",
    ),
    PolicySectionData(
        "Advertising Measurement",
        "If you install PriceErrors after seeing one of our ads on Facebook or Instagram, the Meta App Events SDK may send app activation, registration, checkout, trial, and subscription events to measure whether the ad led to an install or subscription. These events are used for advertising measurement and do not include your PriceErrors name or email.",
    ),
    PolicySectionData(
        "Product Analytics",
        "We use PostHog, a product-analytics service, to understand how people use the app — for example which deals are viewed, whether onboarding is completed, and which features are used — so we can improve PriceErrors. Usage events may be linked to your PriceErrors account ID, but we do not include your name or email in them. PostHog screen recording is disabled, and we do not use PostHog to track you across other companies' apps or websites. This analytics data is processed by PostHog on servers located in the United States.",
    ),
    PolicySectionData(
        "How Your Data Is Stored",
        "Your account data is stored securely in Supabase, a hosted database platform. Local settings and session data are kept in this app's private Android storage with backup disabled. Data is encrypted in transit, and you can request deletion of your account and associated data at any time from Help & Support.",
    ),
    PolicySectionData(
        "Push Notifications",
        "If you grant notification permission, we send deal alerts through Firebase Cloud Messaging (FCM). Your device token and alert preferences are stored on our server solely to deliver these notifications. You can disable notifications at any time in Android Settings.",
    ),
    PolicySectionData(
        "Purchases",
        "Google Play Billing processes subscription purchases. PriceErrors receives purchase status and transaction information needed to unlock and manage your subscription; Google handles your payment details.",
    ),
    PolicySectionData(
        "Data Deletion",
        "You can delete your account and all associated data at any time from You → Help & Support → Delete Account & Data. This permanently removes your profile, saved deals, and preferences.",
    ),
    PolicySectionData(
        "Children's Privacy",
        "PriceErrors is not directed at children under 13. We do not knowingly collect personal information from children.",
    ),
    PolicySectionData(
        "Changes to This Policy",
        "We may update this privacy policy from time to time. Continued use of the app after changes constitutes acceptance of the updated policy.",
    ),
)

private val termsSections = listOf(
    PolicySectionData(
        "Acceptance of Terms",
        "By downloading or using PriceErrors, you agree to these Terms of Service. If you do not agree, please do not use the app. We may update these terms at any time; continued use of the app constitutes acceptance of any changes.",
    ),
    PolicySectionData(
        "What PriceErrors Is",
        "PriceErrors is an informational app that surfaces deal and price-error alerts aggregated from public sources. We are not a retailer, marketplace, financial advisor, or intermediary. We do not sell products, process payments, or guarantee any transaction.",
    ),
    PolicySectionData(
        "Scraped Deals — No Warranty",
        "Deals sourced automatically from public sources (the Feed) are provided for informational purposes only. We do not verify their accuracy, availability, or that any retailer will honor a listed price. Prices shown may be outdated, incorrect, or already fixed by the retailer by the time you view them.\n\nYou act on any deal entirely at your own risk. PriceErrors makes no representation that a retailer will fulfill, ship, or honor any order placed at a price-error or discounted price.",
    ),
    PolicySectionData(
        "No Financial or Legal Advice",
        "Nothing in this app constitutes financial, legal, or purchasing advice. Deal alerts are informational only. You should independently verify any deal before making a purchase. PriceErrors is not liable for purchase decisions made based on app content.",
    ),
    PolicySectionData(
        "Limitation of Liability",
        "To the maximum extent permitted by applicable law, PriceErrors and its operators shall not be liable for any direct, indirect, incidental, special, or consequential damages arising from:\n• Deals that are inaccurate, expired, or not honored by retailers.\n• Any purchase or transaction made in reliance on app content.\n• Any unauthorized access to or alteration of your data.\n\nYour sole remedy is to stop using the app.",
    ),
    PolicySectionData(
        "Prohibited Uses",
        "You may not use PriceErrors to:\n• Attempt to spam, manipulate, disrupt, or abuse the service or other users.\n• Circumvent any paywall, rate limit, access control, or security measure.\n• Use the app for any commercial purpose without our written consent.",
    ),
    PolicySectionData(
        "Account Termination",
        "We reserve the right to suspend or terminate your account at any time if we determine, in our sole discretion, that you have violated these terms or used the app in a harmful way.",
    ),
    PolicySectionData(
        "Governing Law",
        "These terms are governed by the laws of the State of California, without regard to conflict-of-law principles. Any disputes shall be resolved in the courts of Los Angeles County, California.",
    ),
    PolicySectionData(
        "Contact",
        "Questions about these terms? Reach us through the Help & Support page in the app.",
    ),
)
