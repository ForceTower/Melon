package dev.forcetower.unes.ui.feature.licenses.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon

// Search field. Mirrors `LicensesSearchBar` (iOS) and `LicSearch` (JSX) — pill
// border lifts to ink color when focused, leading magnifying glass icon, and
// a trailing close button when there's text. Built on `BasicTextField` so we
// can keep the iOS look (no Material outlined chrome).
@Composable
internal fun LicensesSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine
    val ink = MaterialTheme.colorScheme.onBackground
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(14.dp)

    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val borderWidth by animateDpAsState(
        targetValue = if (isFocused) 1.5.dp else 1.dp,
        animationSpec = tween(durationMillis = 150),
        label = "search-border-width",
    )

    val textStyle = TextStyle(
        fontFamily = LocalTextStyle.current.fontFamily,
        fontSize = 13.sp,
        color = ink,
        letterSpacing = (-0.07).sp,
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(card)
            .border(borderWidth, if (isFocused) ink else cardLine, shape)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LicensesIcon(
            glyph = LicensesGlyph.Search,
            color = ink3,
            modifier = Modifier.size(15.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = stringResource(R.string.licenses_search_placeholder),
                    style = textStyle.copy(color = ink3, textAlign = TextAlign.Start),
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = textStyle,
                cursorBrush = androidx.compose.ui.graphics.SolidColor(accent),
                interactionSource = interactionSource,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            val description = stringResource(R.string.licenses_search_clear)
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onQueryChange("") }
                    .semantics {
                        role = Role.Button
                        contentDescription = description
                    },
                contentAlignment = Alignment.Center,
            ) {
                LicensesIcon(
                    glyph = LicensesGlyph.Close,
                    color = ink3,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
    }
}
