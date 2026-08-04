package app.priceerrors.feature.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.res.painterResource
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
import app.priceerrors.ui.theme.AppDark
import app.priceerrors.ui.theme.Mint
import app.priceerrors.ui.theme.SpaceGrotesk
import app.priceerrors.ui.accessibility.PriceErrorsTestTags
import app.priceerrors.core.auth.AuthConfigurationException
import app.priceerrors.core.auth.AuthIdentity
import kotlinx.coroutines.launch

private enum class AuthMode { SIGN_UP, SIGN_IN }

/** Both auth buttons share a height so they read as one stack. */
private val AuthButtonHeight = 56.dp

@Composable
fun AuthScreen(
    onAuthenticated: (name: String, email: String) -> Unit,
    modifier: Modifier = Modifier,
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
) {
    var mode by rememberSaveable { mutableStateOf(AuthMode.SIGN_UP) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf("") }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp)
            .testTag(PriceErrorsTestTags.AUTH_SCREEN),
    ) {
        Text(
            text = if (mode == AuthMode.SIGN_UP) "Never miss a\nprice error." else "Welcome\nback.",
            modifier = Modifier.padding(top = 56.dp),
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
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.50f),
        )

        // ── Google button — the fastest path, so it leads ────────────
        Button(
            onClick = {
                errorMessage = ""
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
                            errorMessage = when (error) {
                                is AuthConfigurationException -> error.message.orEmpty()
                                else -> error.message
                                    ?.takeIf(String::isNotBlank)
                                    ?: "Google sign-in couldn't be completed. Try again."
                            }
                        }
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(AuthButtonHeight)
                .testTag(PriceErrorsTestTags.GOOGLE_AUTH),
            colors = ButtonDefaults.buttonColors(containerColor = AppDark, contentColor = Color.White),
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
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.40f),
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
                color = MaterialTheme.colorScheme.error,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
        }

        Button(
            onClick = {
                errorMessage = when {
                    mode == AuthMode.SIGN_UP && name.isBlank() -> "Enter your full name."
                    !email.contains('@') -> "Enter a valid email address."
                    password.length < 6 -> "Password must be at least 6 characters."
                    else -> ""
                }
                if (errorMessage.isEmpty()) {
                    if (allowLocalEmailAuth) {
                        onAuthenticated(name.ifBlank { email.substringBefore('@') }, email.trim())
                    } else {
                        errorMessage = "Email accounts will be enabled when the account service is connected."
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .height(AuthButtonHeight)
                .testTag(PriceErrorsTestTags.AUTH_SUBMIT),
            colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Color.White),
            shape = RoundedCornerShape(16.dp),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
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
                mode = if (mode == AuthMode.SIGN_UP) AuthMode.SIGN_IN else AuthMode.SIGN_UP
                errorMessage = ""
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        ) {
            Text(
                if (mode == AuthMode.SIGN_UP) "Already have an account? Log in" else "Don't have an account? Sign up",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
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
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)),
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
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
            )
        },
        singleLine = true,
        keyboardOptions = keyboardOptions,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        textStyle = MaterialTheme.typography.bodyLarge,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
        ),
    )
}
