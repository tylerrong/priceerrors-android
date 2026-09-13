package app.priceerrors.feature.auth

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.priceerrors.R
import app.priceerrors.feature.onboarding.OnboardingProgressHeader
import app.priceerrors.ui.theme.AppDark
import app.priceerrors.ui.theme.SpaceGrotesk
import app.priceerrors.ui.accessibility.PriceErrorsTestTags
import app.priceerrors.core.auth.AuthConfigurationException
import app.priceerrors.core.auth.AuthIdentity
import app.priceerrors.core.auth.EmailAuthError
import app.priceerrors.core.auth.GoogleOAuthPending
import app.priceerrors.core.auth.GoogleSignInCancelled
import app.priceerrors.core.analytics.PostHogAnalytics
import kotlinx.coroutines.launch

private enum class AuthMode { SIGN_UP, SIGN_IN }

/** Both auth buttons share a height so they read as one stack. */
private val AuthButtonHeight = 56.dp
private val AuthCobalt = Color(0xFF4F6EF7)

@Composable
fun AuthScreen(
    onAuthenticated: (name: String, email: String) -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Local-only email sign-in for builds with no backend, where the sample feed
     * stands in for the server. Ignored whenever [onEmailSignIn] is wired up.
     */
    allowLocalEmailAuth: Boolean = true,
    /**
     * Whether a successful [onGoogleAuthenticate] may complete sign-in. False
     * when no account backend is configured, in which case a Google credential
     * alone cannot establish a real session.
     */
    canCompleteGoogleSignIn: Boolean = true,
    onGoogleAuthenticate: suspend () -> Result<AuthIdentity> = {
        Result.failure(AuthConfigurationException("Google sign-in is not configured."))
    },
    /** Null when this build has no account service to authenticate against. */
    onEmailSignIn: (suspend (email: String, password: String) -> Result<AuthIdentity>)? = null,
    /**
     * Returns null on success-pending-confirmation, so the screen can tell the
     * user to check their inbox instead of claiming they are signed in.
     */
    onEmailSignUp: (
        suspend (name: String, email: String, password: String) -> Result<AuthIdentity?>
    )? = null,
    onBack: (() -> Unit)? = null,
    showOnboardingProgress: Boolean = false,
    onboardingTotalSteps: Int = 6,
    onboardingCurrentStep: Int = 4,
) {
    var mode by rememberSaveable { mutableStateOf(AuthMode.SIGN_UP) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf("") }
    // Advice rather than failure — e.g. the address exists under Google, or the
    // account needs confirming. Shown in a calmer style than an error.
    var hintMessage by rememberSaveable { mutableStateOf("") }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    BackHandler(enabled = onBack != null && !isLoading) { onBack?.invoke() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AuthCobalt)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp)
            .authEdgeBackGesture(if (isLoading) null else onBack, haptics)
            .testTag(PriceErrorsTestTags.AUTH_SCREEN),
    ) {
        if (showOnboardingProgress) {
            OnboardingProgressHeader(
                total = onboardingTotalSteps,
                current = onboardingCurrentStep,
            )
        }
        Text(
            text = if (mode == AuthMode.SIGN_UP) "Never miss a\nprice error." else "Welcome\nback.",
            modifier = Modifier.padding(top = if (showOnboardingProgress) 20.dp else 56.dp),
            color = Color.White,
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 38.sp,
            lineHeight = 41.sp,
            letterSpacing = (-1.5).sp,
        )
        Text(
            text = if (mode == AuthMode.SIGN_UP) {
                "Save deals, get alerts, and catch price\nerrors before they're gone."
            } else {
                "Sign back in to your account."
            },
            modifier = Modifier.padding(top = 6.dp, bottom = 36.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.78f),
        )

        // ── Google button — the fastest path, so it leads ────────────
        Button(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                errorMessage = ""
                hintMessage = ""
                isLoading = true
                scope.launch {
                    onGoogleAuthenticate()
                        .onSuccess { identity ->
                            isLoading = false
                            if (canCompleteGoogleSignIn) {
                                onAuthenticated(identity.displayName, identity.email)
                            } else {
                                errorMessage = "Google identity received. Finishing sign-in requires the account service connection."
                            }
                        }
                        .onFailure { error ->
                            isLoading = false
                            if (error !is GoogleOAuthPending && error !is GoogleSignInCancelled) {
                                PostHogAnalytics.captureException(
                                    error,
                                    mapOf(
                                        "flow" to "google_sign_in",
                                        "error_type" to error::class.simpleName.orEmpty(),
                                    ),
                                )
                            }
                            when (error) {
                                is GoogleOAuthPending -> {
                                    hintMessage = error.message.orEmpty()
                                    errorMessage = ""
                                }
                                is GoogleSignInCancelled -> errorMessage = ""
                                is AuthConfigurationException ->
                                    errorMessage = error.message.orEmpty()
                                else -> {
                                    errorMessage = error.message
                                        ?.takeIf(String::isNotBlank)
                                        ?: "Google sign-in couldn't be completed. Try again."
                                }
                            }
                        }
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(AuthButtonHeight)
                .testTag(PriceErrorsTestTags.GOOGLE_AUTH),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_google_g),
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp)
                        .testTag(PriceErrorsTestTags.GOOGLE_AUTH_LOGO),
                )
                Text(
                    if (mode == AuthMode.SIGN_UP) "Sign up with Google" else "Sign in with Google",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
            }
        }

        // ── Divider ──────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DividerLine(modifier = Modifier.weight(1f))
            Text(
                text = "or use email",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.72f),
            )
            DividerLine(modifier = Modifier.weight(1f))
        }

        // ── Form fields ──────────────────────────────────────────────
        AnimatedVisibility(visible = mode == AuthMode.SIGN_UP) {
            AuthField(
                value = name,
                onValueChange = { name = it },
                placeholder = "Full name",
                leadingIcon = Icons.Outlined.Person,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
        AuthField(
            value = email,
            onValueChange = { email = it },
            placeholder = "Email",
            leadingIcon = Icons.Outlined.Email,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        )
        AuthField(
            value = password,
            onValueChange = { password = it },
            placeholder = "Password",
            leadingIcon = Icons.Outlined.Lock,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            isPassword = true,
            modifier = Modifier.padding(top = 12.dp),
        )

        if (errorMessage.isNotEmpty()) {
            Text(
                errorMessage,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .semantics { liveRegion = LiveRegionMode.Assertive },
                color = Color(0xFFFFD0D6),
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
        }

        if (hintMessage.isNotEmpty()) {
            Text(
                hintMessage,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                color = Color.White.copy(alpha = 0.82f),
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
            )
        }

        Button(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                hintMessage = ""
                errorMessage = when {
                    mode == AuthMode.SIGN_UP && name.isBlank() -> "Enter your full name."
                    !email.contains('@') -> "Enter a valid email address."
                    password.length < 6 -> "Password must be at least 6 characters."
                    else -> ""
                }
                if (errorMessage.isNotEmpty()) return@Button

                val signIn = onEmailSignIn
                val signUp = onEmailSignUp
                when {
                    // Real account service: authenticate against Supabase.
                    signIn != null && signUp != null -> {
                        isLoading = true
                        scope.launch {
                            val outcome = if (mode == AuthMode.SIGN_UP) {
                                signUp(name, email, password)
                            } else {
                                signIn(email, password).map { it }
                            }
                            isLoading = false
                            outcome
                                .onSuccess { identity ->
                                    if (identity == null) {
                                        // Sign-up succeeded but needs confirming.
                                        hintMessage =
                                            "Account created. Check your inbox to confirm your email, then sign in."
                                        mode = AuthMode.SIGN_IN
                                        password = ""
                                    } else {
                                        onAuthenticated(identity.displayName, identity.email)
                                    }
                                }
                                .onFailure { error ->
                                    when (error) {
                                        is EmailAuthError.AlreadyRegistered -> {
                                            hintMessage = error.message.orEmpty()
                                            mode = AuthMode.SIGN_IN
                                        }
                                        is EmailAuthError.UseSocialSignIn,
                                        is EmailAuthError.EmailNotConfirmed,
                                        -> hintMessage = error.message.orEmpty()
                                        else -> {
                                            PostHogAnalytics.captureException(
                                                error,
                                                mapOf(
                                                    "flow" to "email_auth",
                                                    "error_type" to error::class.simpleName.orEmpty(),
                                                ),
                                            )
                                            errorMessage = error.message
                                                ?.takeIf(String::isNotBlank)
                                                ?: "Sign in failed. Try again."
                                        }
                                    }
                                }
                        }
                    }
                    // No backend in this build — the sample feed stands in.
                    allowLocalEmailAuth ->
                        onAuthenticated(name.ifBlank { email.substringBefore('@') }, email.trim())

                    else ->
                        errorMessage = "Email accounts will be enabled when the account service is connected."
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .height(AuthButtonHeight)
                .testTag(PriceErrorsTestTags.AUTH_SUBMIT),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = AppDark),
            shape = RoundedCornerShape(16.dp),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = AppDark,
                    modifier = Modifier.height(22.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    if (mode == AuthMode.SIGN_UP) "Create Account" else "Sign In",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }
        }

        TextButton(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                mode = if (mode == AuthMode.SIGN_UP) AuthMode.SIGN_IN else AuthMode.SIGN_UP
                errorMessage = ""
                hintMessage = ""
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        ) {
            Text(
                if (mode == AuthMode.SIGN_UP) "Already have an account? Log in" else "Don't have an account? Sign up",
                color = Color.White.copy(alpha = 0.82f),
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
            )
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun DividerLine(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.30f)),
    )
}

@Composable
private fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    keyboardOptions: KeyboardOptions,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = placeholder },
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyLarge) },
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = AppDark.copy(alpha = 0.42f),
            )
        },
        singleLine = true,
        keyboardOptions = keyboardOptions,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        textStyle = MaterialTheme.typography.bodyLarge,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = AppDark,
            unfocusedTextColor = AppDark,
            cursorColor = AppDark,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = AppDark.copy(alpha = 0.26f),
            unfocusedBorderColor = AppDark.copy(alpha = 0.10f),
            focusedPlaceholderColor = AppDark.copy(alpha = 0.48f),
            unfocusedPlaceholderColor = AppDark.copy(alpha = 0.48f),
        ),
    )
}

private fun Modifier.authEdgeBackGesture(
    onBack: (() -> Unit)?,
    haptics: HapticFeedback,
): Modifier {
    if (onBack == null) return this
    return pointerInput(onBack) {
        var startedAtEdge = false
        var horizontal = 0f
        var vertical = 0f
        val edge = 44.dp.toPx()
        val threshold = 72.dp.toPx()
        detectDragGestures(
            onDragStart = {
                startedAtEdge = it.x <= edge
                horizontal = 0f
                vertical = 0f
            },
            onDrag = { change, amount ->
                horizontal += amount.x
                vertical += amount.y
                change.consume()
            },
            onDragEnd = {
                if (
                    startedAtEdge &&
                    horizontal > threshold &&
                    horizontal > kotlin.math.abs(vertical) * 1.2f
                ) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onBack()
                }
            },
        )
    }
}
