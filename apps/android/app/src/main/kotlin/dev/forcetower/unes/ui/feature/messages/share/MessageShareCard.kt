package dev.forcetower.unes.ui.feature.messages.share

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.LocalMelonDarkTheme
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.messages.Message
import dev.forcetower.unes.ui.feature.messages.MessagesFixtures
import dev.forcetower.unes.ui.feature.messages.category
import dev.forcetower.unes.ui.feature.messages.categoryColor
import dev.forcetower.unes.ui.feature.messages.fullTime
import dev.forcetower.unes.ui.feature.messages.originIcon
import dev.forcetower.unes.ui.feature.messages.originKindRes
import dev.forcetower.unes.ui.feature.messages.paragraphs
import dev.forcetower.unes.ui.feature.messages.rememberMessageRoleStrings
import dev.forcetower.unes.ui.feature.messages.toUi

// The file that leaves the app (dc `MessageShareCard`): fixed width, height
// following the text, whole message inside. Everything is placed on the dc's
// 1080-unit grid and scaled by `width`, so this one composable backs both the
// sheet preview and the exported PNG.
//
// `dark` is resolved once and pinned for the whole card, along with the font
// scale: the PNG is a bitmap read on someone else's device, so it has to carry
// one appearance rather than follow the reader's.
@Composable
internal fun MessageShareCard(
    message: Message,
    width: Dp,
    modifier: Modifier = Modifier,
    dark: Boolean = LocalMelonDarkTheme.current,
) {
    val density = LocalDensity.current
    MelonTheme(darkTheme = dark) {
        CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1f)) {
            val grid = ShareCardGrid(width, MaterialTheme.typography.bodyLarge)
            val accent = categoryColor(message.category)

            Column(
                modifier = modifier
                    .width(width)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(
                        start = grid.dp(72),
                        end = grid.dp(72),
                        top = grid.dp(72),
                        bottom = grid.dp(56),
                    ),
            ) {
                BrandRow(grid)

                Rule(grid, top = 40, bottom = 44)

                Row(horizontalArrangement = Arrangement.spacedBy(grid.dp(28))) {
                    Box(
                        modifier = Modifier
                            .size(grid.dp(108))
                            .clip(RoundedCornerShape(grid.dp(32)))
                            .background(accent.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = originIcon(message.origin),
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(grid.dp(54)),
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(grid.dp(12))) {
                        Text(
                            text = message.sender.role.uppercase(),
                            style = grid.style(23, FontWeight.ExtraBold, tracking = 0.08f),
                            color = accent,
                        )
                        Text(
                            text = message.sender.name,
                            style = grid.style(58, FontWeight.ExtraBold, tracking = -0.035f, leading = 1.08f),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = stringResource(originKindRes(message.origin)),
                            style = grid.style(27, FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.outline,
                        )
                        Text(
                            text = fullTime(message.receivedAt),
                            style = grid.style(25, FontWeight.Medium),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                }

                Rule(grid, top = 44, bottom = 44)

                Column(verticalArrangement = Arrangement.spacedBy(grid.dp(34))) {
                    message.paragraphs().forEach { paragraph ->
                        Text(
                            text = paragraph,
                            style = grid.style(34, FontWeight.Normal, leading = 1.58f),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Rule(grid, top = 56, bottom = 32)

                Text(
                    text = stringResource(R.string.messages_share_card_footer),
                    style = grid.style(24, FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
}

@Composable
private fun BrandRow(grid: ShareCardGrid) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(grid.dp(20)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(grid.dp(68))
                .clip(RoundedCornerShape(grid.dp(20)))
                .background(MaterialTheme.melon.brand.coral),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.messages_share_card_monogram),
                // Sits on the fixed coral tile, so it can't follow the plate.
                style = grid.style(36, FontWeight.ExtraBold, tracking = -0.03f),
                color = MaterialTheme.melon.fixed.surfaceLight,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(grid.dp(4))) {
            Text(
                text = stringResource(R.string.messages_share_card_wordmark),
                style = grid.style(32, FontWeight.ExtraBold, tracking = -0.02f),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.messages_share_card_kicker).uppercase(),
                style = grid.style(21, FontWeight.SemiBold, tracking = 0.1f),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}

@Composable
private fun Rule(grid: ShareCardGrid, top: Int, bottom: Int) {
    Box(
        modifier = Modifier
            .padding(top = grid.dp(top), bottom = grid.dp(bottom))
            .fillMaxWidth()
            .height(grid.hairline)
            .background(MaterialTheme.melon.surface.line),
    )
}

// Maps the dc's 1080-unit grid onto the width the card is drawn at. Styles are
// derived from the theme's body role, so the card stays on Manrope.
private class ShareCardGrid(width: Dp, private val base: TextStyle) {
    private val scale = width.value / 1080f

    val hairline: Dp = maxOf(1f, 1.5f * scale).dp

    fun dp(units: Int): Dp = (units * scale).dp

    // `tracking` and `leading` are the dc's em-relative values.
    fun style(
        units: Int,
        weight: FontWeight,
        tracking: Float = 0f,
        leading: Float = 0f,
    ): TextStyle {
        val size = units * scale
        return base.copy(
            fontWeight = weight,
            fontSize = size.sp,
            letterSpacing = (size * tracking).sp,
            lineHeight = if (leading == 0f) TextUnit.Unspecified else (size * leading).sp,
        )
    }
}

@Preview(widthDp = 400, heightDp = 900)
@Composable
private fun MessageShareCardPreview() {
    MelonTheme {
        val roles = rememberMessageRoleStrings()
        val seed = MessagesFixtures.items[1]
        val message = MessagesFixtures.detailById[seed.id]?.toUi(roles) ?: seed.toUi(roles)
        MessageShareCard(message = message, width = 360.dp, dark = false)
    }
}

@Preview(widthDp = 400, heightDp = 900)
@Composable
private fun MessageShareCardDarkPreview() {
    MelonTheme(darkTheme = true) {
        val roles = rememberMessageRoleStrings()
        val seed = MessagesFixtures.items[1]
        val message = MessagesFixtures.detailById[seed.id]?.toUi(roles) ?: seed.toUi(roles)
        MessageShareCard(message = message, width = 360.dp, dark = true)
    }
}
