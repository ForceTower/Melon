package dev.forcetower.unes.ui.feature.overview.components

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.melon.core.analytics.Screens
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.components.MelonGhostButton
import dev.forcetower.unes.designsystem.components.MelonPrimaryButton
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.mvi.collectAsEffect
import dev.forcetower.unes.ui.feature.onboarding.login.LoginEffect
import dev.forcetower.unes.ui.feature.onboarding.login.LoginIntent
import dev.forcetower.unes.ui.feature.onboarding.login.LoginViewModel

/**
 * Sign back in without losing the app: the refresh token was spent, but the
 * local mirror is intact, so this swaps the tokens in place instead of routing
 * through logout. Reuses [LoginViewModel] at sheet scale.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SessionExpiredSheet(
    onDismiss: () -> Unit,
    onSignedIn: () -> Unit,
    vm: LoginViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val activity = LocalActivity.current

    LaunchedEffect(Unit) { vm.onIntent(LoginIntent.Started(Screens.SESSION_EXPIRED)) }

    vm.effects.collectAsEffect { effect ->
        when (effect) {
            is LoginEffect.Authenticated -> onSignedIn()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(top = 4.dp, bottom = 28.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile()
                Spacer(Modifier.size(12.dp))
                Text(
                    text = stringResource(R.string.session_expired_sheet_title),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 22.sp,
                        letterSpacing = (-0.33).sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.session_expired_sheet_description),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 19.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = state.username,
                onValueChange = { vm.onIntent(LoginIntent.UsernameChanged(it)) },
                label = { Text(stringResource(R.string.session_expired_sheet_username)) },
                singleLine = true,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = state.password,
                onValueChange = { vm.onIntent(LoginIntent.PasswordChanged(it)) },
                label = { Text(stringResource(R.string.session_expired_sheet_password)) },
                singleLine = true,
                enabled = !state.isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (state.showPassword) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = { vm.onIntent(LoginIntent.TogglePasswordVisibility) }) {
                        Icon(
                            imageVector = if (state.showPassword) {
                                Icons.Filled.VisibilityOff
                            } else {
                                Icons.Filled.Visibility
                            },
                            contentDescription = null,
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            val errorRes = state.errorRes
            if (errorRes != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = state.errorArg?.let { stringResource(errorRes, it) } ?: stringResource(errorRes),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.melon.status.bad,
                )
            }

            Spacer(Modifier.height(20.dp))

            MelonPrimaryButton(
                text = stringResource(R.string.session_expired_sheet_confirm),
                onClick = { vm.onIntent(LoginIntent.Submit) },
                enabled = state.canSubmit,
                isLoading = state.isLoading,
            )

            Spacer(Modifier.height(10.dp))

            // The ViewModel drops the intent while a login is already running.
            if (activity != null) {
                MelonGhostButton(
                    text = stringResource(R.string.session_expired_sheet_passkey),
                    onClick = { vm.onIntent(LoginIntent.SubmitPasskey(activity)) },
                )
                Spacer(Modifier.height(10.dp))
            }

            MelonGhostButton(
                text = stringResource(R.string.session_expired_sheet_cancel),
                onClick = onDismiss,
            )
        }
    }
}

@Composable
private fun IconTile() {
    val bad = MaterialTheme.melon.status.bad
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bad.copy(alpha = 0.12f))
            .border(1.dp, bad.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = bad,
            modifier = Modifier.size(18.dp),
        )
    }
}
