package dev.forcetower.unes.ui.feature.commute

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R

// The setup screens' shared shape: the M3 bar with the system back, the step
// indicator under it, the step counter and large question, the content, and
// a call to action pinned over a fade (dc `MeuTrajetoScreen` · Direção /
// Ponto / Linhas; iOS `CommuteSetupScaffold`).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CommuteSetupScaffold(
    step: Int,
    total: Int,
    barTitle: String,
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    bottomInset: Dp = 0.dp,
    footer: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val background = MaterialTheme.colorScheme.background
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
    ) {
        TopAppBar(
            title = { Text(text = barTitle, maxLines = 1) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.commute_back),
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = background,
                scrolledContainerColor = background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
            ),
        )
        StepIndicator(step = step, total = total)
        Box(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 120.dp + bottomInset),
            ) {
                Text(
                    text = stringResource(R.string.commute_setup_step, step, total).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 30.sp, lineHeight = 33.sp),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 6.dp),
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp, lineHeight = 21.sp),
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Column(modifier = Modifier.padding(top = 18.dp), content = content)
            }
            Column(
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            0f to background.copy(alpha = 0f),
                            0.3f to background,
                        ),
                    )
                    .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp + bottomInset),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = footer,
            )
        }
    }
}

@Composable
private fun StepIndicator(
    step: Int,
    total: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (index < step) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                    ),
            )
        }
    }
}

// Shared section eyebrow for the setup lists ("Até o ponto", "Pontos").
@Composable
internal fun CommuteSetupSectionLabel(
    label: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
) {
    Column(modifier = modifier.padding(horizontal = 4.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.outline,
        )
        if (hint != null) {
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}
