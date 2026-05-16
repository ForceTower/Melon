package dev.forcetower.unes.widgets

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap

// Static mesh-background bitmap used as the widget's hero surface. Glance's
// `Image` lets us pass an `ImageProvider(Bitmap)` and have the system widget
// host scale it into place — Compose's `Canvas` + `LinearGradient` aren't
// available across the RemoteViews boundary, so the field is rasterized on
// the host side and ferried over as a single texture.
//
// Each blob is drawn with a multi-stop radial gradient (solid center →
// transparent edge), the same shape the design-system `Mesh.kt` uses on
// pre-API 31 devices when the runtime blur modifier isn't available. That
// gives the soft blob silhouettes their characteristic cloud look — and,
// critically, keeps them readable when the bitmap is upscaled to fit the
// 338×354dp Large widget. An earlier revision drew solid circles softened
// with `BlurMaskFilter`, which collapsed into a flat wash on upscale.
//
// Canvas is rendered at 400×400 so blob centers stay defined when the
// system widget host stretches into a Medium / Large footprint (~700px on
// a 2x device). 400×400 ARGB8888 is ~640 KB, well under the 1.5 MB
// RemoteViews bitmap-binder budget.
internal object MeshBackgroundBitmap {
    private const val CANVAS_PX = 400

    fun render(theme: WidgetTheme): Bitmap {
        val bmp = createBitmap(CANVAS_PX, CANVAS_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val surfacePaint = Paint().apply { color = theme.surface.toArgb() }
        canvas.drawRect(0f, 0f, CANVAS_PX.toFloat(), CANVAS_PX.toFloat(), surfacePaint)

        val intensity = theme.meshIntensity
        for (blob in blobsFor(theme.meshKind)) {
            drawBlob(canvas, blob, intensity)
        }

        // Top → bottom plum/cream wash that lifts contrast for foreground
        // text. Same stop set as iOS `WidgetTheme.veilTop/Bottom`.
        val veilPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, CANVAS_PX.toFloat(),
                theme.veilTop.toArgb(),
                theme.veilBottom.toArgb(),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, CANVAS_PX.toFloat(), CANVAS_PX.toFloat(), veilPaint)

        // 0.08-alpha highlight in the upper-left quadrant — same film-grain
        // touch the iOS canvas applies on top of the blurred field.
        val highlight = Paint().apply {
            shader = RadialGradient(
                CANVAS_PX * 0.3f,
                CANVAS_PX * 0.2f,
                CANVAS_PX * 0.7f,
                intArrayOf(0x14FFFFFF, 0x00FFFFFF),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, CANVAS_PX.toFloat(), CANVAS_PX.toFloat(), highlight)

        return bmp
    }

    private fun drawBlob(canvas: Canvas, blob: Blob, intensity: Float) {
        val sizePx = blob.sizeFraction * CANVAS_PX
        val left = blob.originX * CANVAS_PX
        val top = blob.originY * CANVAS_PX
        val cx = left + sizePx / 2f
        val cy = top + sizePx / 2f
        val radius = sizePx / 2f

        // Color at each stop carries its own alpha — multiplying by
        // intensity scales the whole blob, matching iOS's
        // `0.85 * intensity` falloff (light dampens, dark plays at full).
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx, cy, radius,
                intArrayOf(
                    blob.colorArgb.withAlpha(0.85f * intensity),
                    blob.colorArgb.withAlpha(0.78f * intensity),
                    blob.colorArgb.withAlpha(0.45f * intensity),
                    blob.colorArgb.withAlpha(0.16f * intensity),
                    blob.colorArgb.withAlpha(0f),
                ),
                floatArrayOf(0f, 0.35f, 0.6f, 0.82f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawCircle(cx, cy, radius, paint)
    }

    // Blob layout for the two variants the widget uses. Same fractional
    // positions, same colors, same blob-size-fraction (≈ iOS 320pt blob in
    // a 158pt card → 2× canvas; we use 1.4–1.6× here, slightly tighter so
    // the blob silhouettes stay distinguishable when the canvas stretches
    // into a wider Medium / Large footprint).
    private fun blobsFor(kind: MeshKind): List<Blob> = when (kind) {
        MeshKind.Sun -> listOf(
            Blob(0xFFC94538.toInt(), -0.10f, -0.15f, 1.6f),
            Blob(0xFFF4A23C.toInt(),  0.50f,  0.35f, 1.5f),
            Blob(0xFFFBD9A8.toInt(), -0.05f,  0.55f, 1.4f),
        )
        MeshKind.Cool -> listOf(
            Blob(0xFF1E3A5F.toInt(), -0.10f, -0.10f, 1.6f),
            Blob(0xFF3B9EAE.toInt(),  0.55f,  0.40f, 1.4f),
            Blob(0xFF88D4C1.toInt(), -0.15f,  0.60f, 1.3f),
        )
    }

    private fun Int.withAlpha(fraction: Float): Int {
        val alpha = (fraction.coerceIn(0f, 1f) * 0xFF).toInt()
        return (alpha shl 24) or (this and 0x00FFFFFF)
    }

    private data class Blob(
        val colorArgb: Int,
        val originX: Float,
        val originY: Float,
        val sizeFraction: Float,
    )
}
