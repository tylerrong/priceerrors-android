package app.priceerrors.feature.community

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun PostCommunityDealScreen(
    currentUserName: String,
    termsAccepted: Boolean,
    onTermsAcceptedChanged: (Boolean) -> Unit,
    onClose: () -> Unit,
    onPosted: (CommunityDeal) -> Unit,
    modifier: Modifier = Modifier,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var priceText by rememberSaveable { mutableStateOf("") }
    var brand by rememberSaveable { mutableStateOf("") }
    var link by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("Tech") }
    var isSubmitting by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val parsedPrice = remember(priceText) {
        priceText.removePrefix("\$").trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
    }
    val hasValidTitle = title.trim().length in 4..120
    val hasValidDescription = description.trim().length in 10..1_000
    val hasValidPrice = parsedPrice != null && parsedPrice.isFinite() && parsedPrice in 0.0..1_000_000.0
    val hasValidLink = link.isBlank() || normalizeCommunityLink(link) != null
    val canAttemptSubmit = hasValidTitle && hasValidDescription && hasValidPrice && hasValidLink && !isSubmitting
    val validation = validateCommunityPost(title, description, parsedPrice, link, termsAccepted)
    val canSubmit = validation.isValid && !isSubmitting
    val priceError = priceText.isNotEmpty() && !hasValidPrice
    val linkError = link.isNotBlank() && !hasValidLink

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                onClick = onClose,
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(19.dp),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Post a deal",
                style = MaterialTheme.typography.titleMedium,
                fontSize = 15.sp,
                letterSpacing = (-0.3).sp,
            )
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.size(40.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            CommunityFormField(label = "Title") {
                CommunityTextField(
                    value = title,
                    onValueChange = {
                        title = it.take(120)
                        errorMessage = null
                    },
                    placeholder = "e.g. Used iPad Pro 11\" 2022",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next,
                    ),
                )
                Text(
                    text = "${title.length}/120",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.42f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                )
            }

            CommunityFormField(label = "Description") {
                CommunityTextField(
                    value = description,
                    onValueChange = {
                        description = it.take(1_000)
                        errorMessage = null
                    },
                    placeholder = "What did you find, and how can someone claim it?",
                    minLines = 4,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Default,
                    ),
                )
                Text(
                    text = "${description.length}/1000",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.42f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CommunitySectionLabel("Category")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(communityCategories, key = { it.name }) { item ->
                        val selected = item.name == category
                        Surface(
                            onClick = { category = item.name },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) item.color else MaterialTheme.colorScheme.surface,
                            contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(text = item.emoji)
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }

            CommunityFormField(label = "Price") {
                CommunityTextField(
                    value = priceText,
                    onValueChange = {
                        priceText = formatCommunityPriceInput(it)
                        errorMessage = null
                    },
                    placeholder = "\$0.00",
                    singleLine = true,
                    isError = priceError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next,
                    ),
                )
                if (priceError) {
                    Text(
                        text = "Enter a price from \$0 to \$1,000,000 with up to two decimal places.",
                        modifier = Modifier.padding(start = 4.dp, top = 6.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            CommunityFormField(label = "Brand (optional)") {
                CommunityTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    placeholder = "e.g. Apple, Nike",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    ),
                )
            }

            CommunityFormField(label = "Link (optional)") {
                CommunityTextField(
                    value = link,
                    onValueChange = {
                        link = it
                        errorMessage = null
                    },
                    placeholder = "amazon.com/… or google.com/…",
                    singleLine = true,
                    isError = linkError,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done,
                    ),
                )
                if (linkError) {
                    Text(
                        text = "Use a valid http or https link without sign-in information.",
                        modifier = Modifier.padding(start = 4.dp, top = 6.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Surface(
                onClick = {
                    onTermsAcceptedChanged(!termsAccepted)
                    errorMessage = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {
                        contentDescription = "Agree to the Community Guidelines and Terms of Service"
                        stateDescription = if (termsAccepted) "Agreed" else "Not agreed"
                    },
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Checkbox(
                        checked = termsAccepted,
                        onCheckedChange = null,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "I agree to the Community Guidelines and Terms of Service.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 19.sp,
                        )
                        Text(
                            text = "No spam, scams, unsafe content, harassment, or personal information. Verify every price and link before sharing.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage.orEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Button(
                onClick = {
                    if (!canSubmit) {
                        errorMessage = validation.message
                        return@Button
                    }
                    val price = checkNotNull(parsedPrice)

                    isSubmitting = true
                    errorMessage = null
                    scope.launch {
                        try {
                            delay(650)
                            onPosted(
                                CommunityDeal(
                                    id = "local-${System.currentTimeMillis()}",
                                    userDisplayName = currentUserName.trim().ifEmpty { "Anonymous" },
                                    title = title.trim(),
                                    description = description.trim(),
                                    category = category,
                                    brand = brand.trim().takeIf { it.isNotEmpty() },
                                    url = normalizeCommunityLink(link),
                                    price = price,
                                    createdAtMillis = System.currentTimeMillis(),
                                ),
                            )
                        } catch (_: Throwable) {
                            isSubmitting = false
                            errorMessage = "Couldn't post this deal. Please try again."
                        }
                    }
                },
                enabled = canAttemptSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    disabledContainerColor = Color.Gray.copy(alpha = 0.40f),
                    disabledContentColor = Color.White,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = if (canAttemptSubmit) 8.dp else 0.dp),
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(19.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Text(
                    text = if (isSubmitting) "Posting…" else "Post deal",
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 16.sp,
                    letterSpacing = (-0.3).sp,
                )
            }
        }
    }
}

@Composable
private fun CommunityFormField(
    label: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CommunitySectionLabel(label)
        content()
    }
}

@Composable
private fun CommunitySectionLabel(label: String) {
    Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.2.sp,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.50f),
    )
}

@Composable
private fun CommunityTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    minLines: Int = 1,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = placeholder,
                maxLines = if (singleLine) 1 else Int.MAX_VALUE,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            )
        },
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
        ),
        singleLine = singleLine,
        minLines = minLines,
        isError = isError,
        keyboardOptions = keyboardOptions,
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surface,
            errorContainerColor = MaterialTheme.colorScheme.surface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
        ),
    )
}

private fun formatCommunityPriceInput(input: String): String {
    val raw = input.replace("\$", "")
    val result = StringBuilder()
    var seenDecimal = false
    var decimalCount = 0

    raw.forEach { character ->
        when {
            character.isDigit() && (!seenDecimal || decimalCount < 2) -> {
                result.append(character)
                if (seenDecimal) decimalCount += 1
            }

            character == '.' && !seenDecimal -> {
                result.append(character)
                seenDecimal = true
            }
        }
    }

    return if (result.isEmpty()) "" else "\$$result"
}
