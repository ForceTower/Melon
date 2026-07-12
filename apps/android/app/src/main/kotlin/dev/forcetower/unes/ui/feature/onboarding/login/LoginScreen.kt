package dev.forcetower.unes.ui.feature.onboarding.login

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.Mesh
import dev.forcetower.unes.designsystem.foundation.MeshVariant
import dev.forcetower.unes.designsystem.foundation.fadeInOnAppear
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.mvi.collectAsEffect
import dev.forcetower.unes.ui.feature.onboarding.components.OnboardingPillButton

private enum class LoginField { Id, Password }

@Composable
fun LoginScreen(
    onSubmit: (String) -> Unit,
    onBack: () -> Unit,
    vm: LoginViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    vm.effects.collectAsEffect { effect ->
        when (effect) {
            is LoginEffect.Authenticated -> onSubmit(effect.firstName)
        }
    }

    LoginContent(
        state = state,
        onIntent = vm::onIntent,
        onBack = onBack,
    )
}

@Composable
private fun LoginContent(
    state: LoginUiState,
    onIntent: (LoginIntent) -> Unit,
    onBack: () -> Unit,
) {
    var focused by remember { mutableStateOf<LoginField?>(null) }
    var showForgotPasswordSheet by remember { mutableStateOf(false) }
    val activity = LocalActivity.current

    val ink = MaterialTheme.colorScheme.onBackground
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    val pageBg = MaterialTheme.colorScheme.background
    val line = MaterialTheme.melon.surface.line
    val card = MaterialTheme.melon.surface.card

    Box(
        Modifier
            .fillMaxSize()
            .background(pageBg),
    ) {
        // Soft mesh crown behind the title — fades into the page background.
        Box(
            Modifier
                .fillMaxWidth()
                .height(230.dp),
        ) {
            Mesh(
                variant = MeshVariant.Sun,
                intensity = 0.4f,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.88f to pageBg,
                            1f to pageBg,
                        ),
                    ),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(start = 24.dp, end = 24.dp, top = 116.dp, bottom = 36.dp),
        ) {
            Text(
                text = stringResource(R.string.onboarding_login_eyebrow).uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.78.sp,
                ),
                color = accent,
                modifier = Modifier.fadeUpOnAppear(delayMs = 40, durationMs = 500),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = loginHeadline(ink, accent),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 40.sp,
                    lineHeight = 41.sp,
                    letterSpacing = (-1.6).sp,
                    fontWeight = FontWeight.ExtraBold,
                ),
                modifier = Modifier.fadeUpOnAppear(delayMs = 120),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.onboarding_login_body),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.5.sp,
                    lineHeight = 22.5.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = ink3,
                modifier = Modifier.fadeUpOnAppear(delayMs = 200),
            )
            Spacer(Modifier.height(26.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fadeUpOnAppear(delayMs = 280),
            ) {
                LoginFieldRow(
                    icon = Icons.Filled.Person,
                    label = stringResource(R.string.onboarding_login_id_label),
                    placeholder = stringResource(R.string.onboarding_login_id_placeholder),
                    value = state.username,
                    onValueChange = { onIntent(LoginIntent.UsernameChanged(it)) },
                    isFocused = focused == LoginField.Id,
                    onFocusChanged = { hasFocus ->
                        focused = when {
                            hasFocus -> LoginField.Id
                            focused == LoginField.Id -> null
                            else -> focused
                        }
                    },
                    keyboardType = KeyboardType.Text,
                    visualTransformation = VisualTransformation.None,
                )
                LoginFieldRow(
                    icon = Icons.Filled.Lock,
                    label = stringResource(R.string.onboarding_login_password_label),
                    placeholder = stringResource(R.string.onboarding_login_password_placeholder),
                    value = state.password,
                    onValueChange = { onIntent(LoginIntent.PasswordChanged(it)) },
                    isFocused = focused == LoginField.Password,
                    onFocusChanged = { hasFocus ->
                        focused = when {
                            hasFocus -> LoginField.Password
                            focused == LoginField.Password -> null
                            else -> focused
                        }
                    },
                    keyboardType = KeyboardType.Password,
                    visualTransformation = if (state.showPassword) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailing = {
                        val toggleLabel = stringResource(
                            if (state.showPassword) {
                                R.string.onboarding_login_hide_password
                            } else {
                                R.string.onboarding_login_show_password
                            },
                        )
                        IconButton(onClick = { onIntent(LoginIntent.TogglePasswordVisibility) }) {
                            Icon(
                                imageVector = if (state.showPassword) {
                                    Icons.Filled.VisibilityOff
                                } else {
                                    Icons.Filled.Visibility
                                },
                                contentDescription = toggleLabel,
                                tint = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    },
                )
            }

            Text(
                text = stringResource(R.string.onboarding_login_forgot_password),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = accent,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        role = Role.Button,
                        onClickLabel = stringResource(R.string.onboarding_login_forgot_password_label),
                    ) { showForgotPasswordSheet = true }
                    .padding(vertical = 6.dp, horizontal = 2.dp)
                    .fadeUpOnAppear(delayMs = 340),
            )

            AnimatedVisibility(visible = state.errorRes != null) {
                val errorRes = state.errorRes
                if (errorRes != null) {
                    val message = state.errorArg
                        ?.let { stringResource(errorRes, it) }
                        ?: stringResource(errorRes)
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            OnboardingPillButton(
                text = stringResource(R.string.onboarding_login_submit),
                onClick = { onIntent(LoginIntent.Submit) },
                enabled = state.canSubmit,
                loading = state.isLoading,
                containerColor = accent,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                showArrow = true,
                arrowIcon = Icons.AutoMirrored.Filled.ArrowForward,
                modifier = Modifier
                    .fadeUpOnAppear(delayMs = 400)
                    .shadow(10.dp, CircleShape, spotColor = accent.copy(alpha = 0.35f)),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 18.dp)
                    .fadeInOnAppear(delayMs = 460, durationMs = 500),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.weight(1f).height(1.dp).background(line))
                Text(
                    text = stringResource(R.string.onboarding_login_or_separator).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                    ),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                Box(Modifier.weight(1f).height(1.dp).background(line))
            }

            OnboardingPillButton(
                text = stringResource(R.string.onboarding_login_passkey),
                onClick = {
                    val current = activity ?: return@OnboardingPillButton
                    onIntent(LoginIntent.SubmitPasskey(current))
                },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = ink,
                border = BorderStroke(1.dp, line),
                leadingIcon = Icons.Filled.Key,
                modifier = Modifier.fadeUpOnAppear(delayMs = 520),
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = termsFooter(
                    subtle = MaterialTheme.colorScheme.outlineVariant,
                    strong = MaterialTheme.colorScheme.onSurface,
                ),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.5.sp,
                    lineHeight = 17.5.sp,
                    fontWeight = FontWeight.Medium,
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .fadeInOnAppear(delayMs = 600, durationMs = 500),
            )
        }

        // Back chip — drawn last so it sits above the scrollable column.
        Box(
            Modifier
                .padding(top = 54.dp, start = 16.dp)
                .shadow(2.dp, CircleShape)
                .size(40.dp)
                .clip(CircleShape)
                .background(card)
                .border(1.dp, line, CircleShape)
                .clickable(
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.onboarding_login_back),
                    onClick = onBack,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.onboarding_login_back),
                tint = ink,
                modifier = Modifier.size(22.dp),
            )
        }
    }

    if (showForgotPasswordSheet) {
        ForgotPasswordSheet(onDismiss = { showForgotPasswordSheet = false })
    }
}

// M3 filled text field per the dc spec: surface-2 plate, 14dp radius, static
// mini-label, leading icon, and a 2dp accent underline that lights on focus.
// The stock M3 TextField animates its label between two positions, which the
// design deliberately doesn't do — hence the thin custom row.
@Composable
private fun LoginFieldRow(
    icon: ImageVector,
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    isFocused: Boolean,
    onFocusChanged: (Boolean) -> Unit,
    keyboardType: KeyboardType,
    visualTransformation: VisualTransformation,
    trailing: (@Composable () -> Unit)? = null,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    val accent = MaterialTheme.colorScheme.primary

    val indicator by animateColorAsState(
        targetValue = if (isFocused) accent else Color.Transparent,
        animationSpec = tween(180),
        label = "field-indicator",
    )
    val iconTint by animateColorAsState(
        targetValue = if (isFocused) accent else ink4,
        animationSpec = tween(180),
        label = "field-icon",
    )
    val labelColor by animateColorAsState(
        targetValue = if (isFocused) accent else ink3,
        animationSpec = tween(180),
        label = "field-label",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .drawBehind {
                drawRect(
                    color = indicator,
                    topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - 2.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(size.width, 2.dp.toPx()),
                )
            }
            .padding(start = 16.dp, end = 6.dp, top = 11.dp, bottom = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.44.sp,
                ),
                color = labelColor,
            )
            Spacer(Modifier.height(3.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                cursorBrush = SolidColor(ink),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                visualTransformation = visualTransformation,
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.16).sp,
                    color = ink,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { onFocusChanged(it.isFocused) },
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = LocalTextStyle.current.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = (-0.16).sp,
                                    color = ink4,
                                ),
                            )
                        }
                        inner()
                    }
                },
            )
        }
        if (trailing != null) {
            trailing()
        } else {
            Spacer(Modifier.size(10.dp))
        }
    }
}

@Composable
private fun loginHeadline(ink: Color, accent: Color): AnnotatedString =
    buildAnnotatedString {
        withStyle(SpanStyle(color = ink)) {
            append(stringResource(R.string.onboarding_login_headline_top))
        }
        withStyle(SpanStyle(color = accent)) {
            append(stringResource(R.string.onboarding_login_headline_accent))
        }
    }

@Composable
private fun termsFooter(subtle: Color, strong: Color): AnnotatedString =
    buildAnnotatedString {
        withStyle(SpanStyle(color = subtle)) {
            append(stringResource(R.string.onboarding_login_terms_prefix))
        }
        withStyle(SpanStyle(color = strong, fontWeight = FontWeight.Bold)) {
            append(stringResource(R.string.onboarding_login_terms_link_terms))
        }
        withStyle(SpanStyle(color = subtle)) {
            append(stringResource(R.string.onboarding_login_terms_separator))
        }
        withStyle(SpanStyle(color = strong, fontWeight = FontWeight.Bold)) {
            append(stringResource(R.string.onboarding_login_terms_link_privacy))
        }
        withStyle(SpanStyle(color = subtle)) {
            append(stringResource(R.string.onboarding_login_terms_suffix))
        }
    }

@Preview
@Composable
private fun LoginScreenPreview() {
    MelonTheme {
        LoginContent(
            state = LoginUiState(username = "21345678", password = "secret"),
            onIntent = {},
            onBack = {},
        )
    }
}
