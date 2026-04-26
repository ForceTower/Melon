package dev.forcetower.unes.ui.feature.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.designsystem.components.MelonGhostButton
import dev.forcetower.unes.designsystem.components.MelonPrimaryButton
import dev.forcetower.unes.designsystem.foundation.Mesh
import dev.forcetower.unes.designsystem.foundation.MeshVariant
import dev.forcetower.unes.designsystem.foundation.fadeInOnAppear
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.theme.melon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class LoginField { Id, Password }

@Composable
fun LoginScreen(
    onSubmit: (String) -> Unit,
    onBack: () -> Unit,
) {
    var id by remember { mutableStateOf("") }
    var pw by remember { mutableStateOf("") }
    var showPw by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf<LoginField?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val ink = MaterialTheme.colorScheme.onBackground
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val ink4 = MaterialTheme.colorScheme.outline
    val accent = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    val card = MaterialTheme.melon.surface.card
    val line = MaterialTheme.melon.surface.line
    val surface2 = MaterialTheme.colorScheme.surfaceVariant

    fun submit() {
        if (loading || id.isBlank() || pw.isBlank()) return
        loading = true
        scope.launch {
            delay(900)
            onSubmit(id)
        }
    }

    fun passkey() {
        if (loading) return
        loading = true
        scope.launch {
            delay(1400)
            onSubmit("passkey")
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(surface),
    ) {
        // Soft mesh hero behind the title — fades into surface vertically.
        Box(
            Modifier
                .fillMaxWidth()
                .height(340.dp),
        ) {
            Mesh(
                variant = MeshVariant.Warm,
                intensity = 0.55f,
                modifier = Modifier.fillMaxSize(),
            )
            // Vertical fade-out approximating the iOS LinearGradient mask.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.85f to surface,
                            1f to surface,
                        ),
                    ),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 28.dp, end = 28.dp, top = 120.dp, bottom = 40.dp),
        ) {
            Text(
                text = "◦ UEFS · SAGRES",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 12.sp,
                    letterSpacing = 1.4.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = ink3,
                modifier = Modifier.fadeUpOnAppear(delayMs = 50, durationMs = 500),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = loginHeadline(ink, accent),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 42.sp,
                    lineHeight = 42.sp,
                    letterSpacing = (-1.05).sp,
                    fontWeight = FontWeight.Normal,
                ),
                modifier = Modifier.fadeUpOnAppear(delayMs = 150),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "As mesmas credenciais que você usa pra entrar no SAGRES. Nada fica no nosso servidor.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    letterSpacing = (-0.08).sp,
                ),
                color = ink3,
                modifier = Modifier.fadeUpOnAppear(delayMs = 250),
            )
            Spacer(Modifier.height(28.dp))

            InputGroup(
                focused = focused,
                line = line,
                ink = ink,
                cardBg = card,
                modifier = Modifier.fadeUpOnAppear(delayMs = 350),
            ) {
                    InputRow(
                        label = "Matrícula",
                        placeholder = "202300000",
                        value = id,
                        onValueChange = { id = it },
                        onFocusChanged = { f ->
                            focused = when {
                                f -> LoginField.Id
                                focused == LoginField.Id -> null
                                else -> focused
                            }
                        },
                        keyboardType = KeyboardType.Number,
                        isPassword = false,
                        showPassword = false,
                        ink = ink,
                        ink3 = ink3,
                        ink4 = ink4,
                        trailing = {
                            Box(
                                Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(surface2),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AccountBox,
                                    contentDescription = null,
                                    tint = ink3,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        },
                    )
                    Box(Modifier.fillMaxWidth().height(1.dp).background(line))
                    InputRow(
                        label = "Senha",
                        placeholder = "••••••••",
                        value = pw,
                        onValueChange = { pw = it },
                        onFocusChanged = { f ->
                            focused = when {
                                f -> LoginField.Password
                                focused == LoginField.Password -> null
                                else -> focused
                            }
                        },
                        keyboardType = KeyboardType.Password,
                        isPassword = true,
                        showPassword = showPw,
                        ink = ink,
                        ink3 = ink3,
                        ink4 = ink4,
                        trailing = {
                            val toggleLabel =
                                if (showPw) "Ocultar senha" else "Mostrar senha"
                            Box(
                                Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(surface2)
                                    .clickable(
                                        role = Role.Button,
                                        onClickLabel = toggleLabel,
                                    ) { showPw = !showPw },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = if (showPw) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = toggleLabel,
                                    tint = ink3,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        },
                    )
                }

            Text(
                text = "Esqueci minha senha",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = ink3,
                modifier = Modifier
                    .padding(top = 10.dp, start = 0.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        role = Role.Button,
                        onClickLabel = "Recuperar senha",
                    ) {}
                    .padding(vertical = 8.dp)
                    .fadeUpOnAppear(delayMs = 450),
            )

            Spacer(Modifier.height(24.dp))

            MelonPrimaryButton(
                text = "Entrar",
                onClick = ::submit,
                enabled = id.isNotBlank() && pw.isNotBlank() && !loading,
                isLoading = loading,
                modifier = Modifier.fadeUpOnAppear(delayMs = 500),
            )

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fadeInOnAppear(delayMs = 550, durationMs = 500),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.weight(1f).height(1.dp).background(line))
                Text(
                    text = "OU",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        letterSpacing = 1.5.sp,
                        fontFamily = FontFamily.Monospace,
                    ),
                    color = ink4,
                )
                Box(Modifier.weight(1f).height(1.dp).background(line))
            }

            Spacer(Modifier.height(18.dp))

            MelonGhostButton(
                text = "Entrar com passkey",
                onClick = ::passkey,
                leading = {
                    Icon(
                        imageVector = Icons.Filled.Key,
                        contentDescription = null,
                        tint = ink,
                        modifier = Modifier.size(20.dp),
                    )
                },
                modifier = Modifier.fadeUpOnAppear(delayMs = 600),
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = termsFooter(ink4, MaterialTheme.colorScheme.onSurface),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .fadeInOnAppear(delayMs = 700, durationMs = 500),
            )
        }

        // Back chip — drawn last so it sits above the scrollable column.
        Box(
            Modifier
                .padding(top = 58.dp, start = 14.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(surface.copy(alpha = 0.6f))
                .border(1.dp, line, CircleShape)
                .clickable(role = Role.Button, onClickLabel = "Voltar", onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Voltar",
                tint = ink,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun InputGroup(
    focused: LoginField?,
    line: Color,
    ink: Color,
    cardBg: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val borderColor by animateDpAsState(
        targetValue = if (focused != null) 1.5.dp else 1.dp,
        animationSpec = tween(200),
        label = "border-w",
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(
                width = borderColor,
                color = if (focused != null) ink else line,
                shape = RoundedCornerShape(18.dp),
            ),
    ) { content() }
}

@Composable
private fun InputRow(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    keyboardType: KeyboardType,
    isPassword: Boolean,
    showPassword: Boolean,
    ink: Color,
    ink3: Color,
    ink4: Color,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    letterSpacing = 0.9.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = ink3,
            )
            Spacer(Modifier.height(4.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                cursorBrush = SolidColor(ink),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                visualTransformation = if (isPassword && !showPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 17.sp,
                    color = ink,
                    letterSpacing = (-0.17).sp,
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
                                    fontSize = 17.sp,
                                    color = ink4,
                                    letterSpacing = (-0.17).sp,
                                ),
                            )
                        }
                        inner()
                    }
                },
            )
        }
        Spacer(Modifier.size(10.dp))
        trailing()
    }
}

private fun loginHeadline(ink: Color, accent: Color): AnnotatedString =
    buildAnnotatedString {
        withStyle(SpanStyle(color = ink)) { append("Entre com sua\n") }
        withStyle(SpanStyle(color = accent, fontStyle = FontStyle.Italic)) {
            append("matrícula.")
        }
    }

private fun termsFooter(ink4: Color, ink2: Color): AnnotatedString =
    buildAnnotatedString {
        withStyle(SpanStyle(color = ink4)) {
            append("Ao continuar, você concorda com nossos ")
        }
        withStyle(SpanStyle(color = ink2, textDecoration = TextDecoration.Underline)) {
            append("Termos")
        }
        withStyle(SpanStyle(color = ink4)) { append(" e ") }
        withStyle(SpanStyle(color = ink2, textDecoration = TextDecoration.Underline)) {
            append("Privacidade")
        }
        withStyle(SpanStyle(color = ink4)) { append(".") }
    }
