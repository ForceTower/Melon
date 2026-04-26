package dev.forcetower.unes.feature.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.designsystem.components.MelonPrimaryButton
import dev.forcetower.unes.designsystem.foundation.Mesh
import dev.forcetower.unes.designsystem.foundation.MeshVariant
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.foundation.scaleInOnAppear
import dev.forcetower.unes.feature.onboarding.illustrations.GradesIllustration
import dev.forcetower.unes.feature.onboarding.illustrations.MessagesIllustration
import dev.forcetower.unes.feature.onboarding.illustrations.NotificationsIllustration
import dev.forcetower.unes.feature.onboarding.illustrations.ScheduleIllustration

private enum class IntroSlide(
    val eyebrow: String,
    val variant: MeshVariant,
    val body: String,
) {
    Schedule(
        eyebrow = "horário",
        variant = MeshVariant.Cool,
        body = "A grade da UEFS puxada direto do SAGRES. Aulas canceladas, salas trocadas e provas — tudo em tempo real.",
    ),
    Grades(
        eyebrow = "notas",
        variant = MeshVariant.Sun,
        body = "Notas parciais, coeficiente e histórico. Sem precisar entrar no SAGRES pelo navegador toda semana.",
    ),
    Messages(
        eyebrow = "recados",
        variant = MeshVariant.Rose,
        body = "Recados de professores, coordenação e DCE — sem perder prazos nem assembleias importantes.",
    ),
    Notifications(
        eyebrow = "notificações",
        variant = MeshVariant.Warm,
        body = "Nota nova, recado de professor, material publicado, sala trocada — um toque no bolso antes de você abrir o app.",
    ),
}

@Composable
fun IntroCarouselScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    val slides = remember { IntroSlide.entries.toList() }
    var index by remember { mutableIntStateOf(0) }
    var contentKey by remember { mutableIntStateOf(0) }
    val slide = slides[index]

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar — back / dots / skip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 62.dp, start = 20.dp, end = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "Voltar",
                        ) {
                            if (index > 0) {
                                index -= 1
                                contentKey += 1
                            } else {
                                onBack()
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    // contentDescription on the icon merges into the parent
                    // clickable's semantics, giving TalkBack "Voltar, botão".
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }

                // Dots are visual progress only; merge into a single
                // page-position announcement instead of N separate nodes.
                val pageDescription = "Página ${index + 1} de ${slides.size}"
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.semantics(mergeDescendants = true) {
                        contentDescription = pageDescription
                    },
                ) {
                    slides.forEachIndexed { i, _ ->
                        val active = i == index
                        val width by animateDpAsState(
                            targetValue = if (active) 24.dp else 6.dp,
                            animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
                            label = "dot-w",
                        )
                        Box(
                            Modifier
                                .width(width)
                                .height(6.dp)
                                .clip(if (active) RoundedCornerShape(3.dp) else CircleShape)
                                .background(
                                    if (active) {
                                        MaterialTheme.colorScheme.onBackground
                                    } else {
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                    },
                                ),
                        )
                    }
                }

                Text(
                    text = "Pular",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "Pular introdução",
                        ) { onDone() }
                        .padding(8.dp),
                )
            }

            // Illustration area — mesh backdrop + slide-specific art.
            // The whole region is wrapped in `key(contentKey)` so a slide change
            // remounts the children, which re-runs the on-appear modifiers.
            key(contentKey) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(30.dp)
                            .scaleInOnAppear(durationMs = 500)
                            .clip(RoundedCornerShape(40.dp)),
                    ) {
                        Mesh(
                            variant = slide.variant,
                            intensity = 0.65f,
                            modifier = Modifier.fillMaxSize(),
                        )
                        // Soft surface overlay so the illustration sits clean.
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)),
                        )
                    }
                    when (slide) {
                        IntroSlide.Schedule -> ScheduleIllustration()
                        IntroSlide.Grades -> GradesIllustration()
                        IntroSlide.Messages -> MessagesIllustration()
                        IntroSlide.Notifications -> NotificationsIllustration()
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Copy + CTA
            key(contentKey) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 28.dp, end = 28.dp, bottom = 50.dp),
                ) {
                    Text(
                        text = "◦ ${slide.eyebrow}".uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 12.sp,
                            letterSpacing = 1.4.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fadeUpOnAppear(delayMs = 100, durationMs = 500),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = slideHeadline(
                            slide,
                            ink = MaterialTheme.colorScheme.onBackground,
                            accent = MaterialTheme.colorScheme.primary,
                        ),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 44.sp,
                            lineHeight = 44.sp,
                            letterSpacing = (-1.1).sp,
                            fontWeight = FontWeight.Normal,
                        ),
                        modifier = Modifier.fadeUpOnAppear(delayMs = 200),
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = slide.body,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            letterSpacing = (-0.08).sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fadeUpOnAppear(delayMs = 300),
                    )
                    Spacer(Modifier.height(28.dp))
                    MelonPrimaryButton(
                        text = if (index == slides.lastIndex) "Entrar na conta" else "Continuar",
                        onClick = {
                            if (index < slides.lastIndex) {
                                index += 1
                                contentKey += 1
                            } else {
                                onDone()
                            }
                        },
                        modifier = Modifier.fadeUpOnAppear(delayMs = 400),
                    )
                }
            }
        }
    }
}

private fun slideHeadline(slide: IntroSlide, ink: Color, accent: Color): AnnotatedString {
    val italicAccent = SpanStyle(color = accent, fontStyle = FontStyle.Italic)
    val plainInk = SpanStyle(color = ink)
    return buildAnnotatedString {
        when (slide) {
            IntroSlide.Schedule -> {
                withStyle(plainInk) { append("Sua semana,\n") }
                withStyle(italicAccent) { append("organizada.") }
            }
            IntroSlide.Grades -> {
                withStyle(plainInk) { append("Acompanhe\nseu ") }
                withStyle(italicAccent) { append("desempenho.") }
            }
            IntroSlide.Messages -> {
                withStyle(plainInk) { append("Tudo o que\nvocê ") }
                withStyle(italicAccent) { append("precisa saber.") }
            }
            IntroSlide.Notifications -> {
                withStyle(plainInk) { append("Avisa no ") }
                withStyle(italicAccent) { append("instante") }
                withStyle(plainInk) { append("\nque acontece.") }
            }
        }
    }
}
