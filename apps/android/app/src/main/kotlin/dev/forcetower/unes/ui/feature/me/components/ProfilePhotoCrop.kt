package dev.forcetower.unes.ui.feature.me.components

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.createBitmap
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Fullscreen circular crop — dc `EuScreen` "Ajustar foto" step. Pinch/drag
// position the photo under a fixed circle mask, the slider mirrors the pinch
// zoom, and Concluir bakes the circle's content into a square JPEG handed
// back to the edit sheet. Pure UI state; nothing here touches the network.
@Composable
internal fun ProfilePhotoCropDialog(
    source: Uri,
    onCancel: () -> Unit,
    onConfirm: (ByteArray) -> Unit,
) {
    val context = LocalContext.current
    var bitmap by remember(source) { mutableStateOf<Bitmap?>(null) }
    var loadFailed by remember(source) { mutableStateOf(false) }
    LaunchedEffect(source) {
        val decoded = withContext(Dispatchers.IO) {
            runCatching {
                val src = ImageDecoder.createSource(context.contentResolver, source)
                ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
                    // Software allocation — the confirm step reads pixels back
                    // through a Canvas. Downsample outsized captures so the
                    // gesture math stays cheap.
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val maxSide = max(info.size.width, info.size.height)
                    if (maxSide > MaxDecodeSide) {
                        val scale = MaxDecodeSide.toFloat() / maxSide
                        decoder.setTargetSize(
                            (info.size.width * scale).toInt().coerceAtLeast(1),
                            (info.size.height * scale).toInt().coerceAtLeast(1),
                        )
                    }
                }
            }.getOrNull()
        }
        if (decoded == null) loadFailed = true else bitmap = decoded
    }
    // An unreadable pick (revoked permission, corrupt file) just backs out.
    LaunchedEffect(loadFailed) {
        if (loadFailed) onCancel()
    }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val night = MaterialTheme.melon.fixed.heroNight
        val onNight = MaterialTheme.melon.fixed.onHero
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(night)
                .safeDrawingPadding(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable(
                            role = Role.Button,
                            onClickLabel = stringResource(R.string.me_crop_cancel),
                            onClick = onCancel,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.me_crop_cancel),
                        tint = onNight,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.me_crop_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = onNight,
                )
            }

            val loaded = bitmap
            if (loaded == null) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = onNight)
                }
            } else {
                CropStage(
                    bitmap = loaded,
                    onConfirm = onConfirm,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CropStage(
    bitmap: Bitmap,
    onConfirm: (ByteArray) -> Unit,
    modifier: Modifier = Modifier,
) {
    val night = MaterialTheme.melon.fixed.heroNight
    val veil = MaterialTheme.melon.fixed.heroVeil
    val onNight = MaterialTheme.melon.fixed.onHero
    val accent = MaterialTheme.colorScheme.primary
    val scope = rememberCoroutineScope()
    var exporting by remember { mutableStateOf(false) }

    var zoom by remember(bitmap) { mutableFloatStateOf(InitialZoom) }
    var offset by remember(bitmap) { mutableStateOf(Offset.Zero) }
    var areaSize by remember { mutableStateOf(IntSize.Zero) }

    // Shared by the gesture stage and the export: the mask's diameter in px
    // and the zoom-1 "cover" scale (photo's shorter side spans the circle).
    val circle = min(areaSize.width, areaSize.height) * CircleFraction
    val baseScale = if (circle > 0f) circle / min(bitmap.width, bitmap.height) else 0f

    fun clampOffset(candidate: Offset, atZoom: Float): Offset {
        val scale = baseScale * atZoom
        val maxX = max(0f, (bitmap.width * scale - circle) / 2f)
        val maxY = max(0f, (bitmap.height * scale - circle) / 2f)
        return Offset(
            candidate.x.coerceIn(-maxX, maxX),
            candidate.y.coerceIn(-maxY, maxY),
        )
    }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .onSizeChanged { areaSize = it },
        ) {
            val image = remember(bitmap) { bitmap.asImageBitmap() }
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(bitmap) {
                        detectTransformGestures { _, pan, gestureZoom, _ ->
                            val newZoom = (zoom * gestureZoom).coerceIn(MinZoom, MaxZoom)
                            // Keep the on-screen anchor put: offsets scale with zoom.
                            val zoomed = offset * (newZoom / zoom)
                            zoom = newZoom
                            offset = clampOffset(zoomed + pan, newZoom)
                        }
                    },
            ) {
                val scale = baseScale * zoom
                val topLeft = Offset(
                    (size.width - bitmap.width * scale) / 2f + offset.x,
                    (size.height - bitmap.height * scale) / 2f + offset.y,
                )
                drawImage(
                    image = image,
                    dstOffset = androidx.compose.ui.unit.IntOffset(
                        topLeft.x.toInt(),
                        topLeft.y.toInt(),
                    ),
                    dstSize = androidx.compose.ui.unit.IntSize(
                        (bitmap.width * scale).toInt(),
                        (bitmap.height * scale).toInt(),
                    ),
                )
            }
            // Scrim with the circle punched out — offscreen compositing so
            // BlendMode.Clear only clears the scrim layer, not the photo.
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen),
            ) {
                drawRect(color = veil.copy(alpha = 0.74f))
                drawCircle(
                    color = veil,
                    radius = circle / 2f,
                    center = center,
                    blendMode = BlendMode.Clear,
                )
                drawCircle(
                    color = onNight.copy(alpha = 0.9f),
                    radius = circle / 2f,
                    center = center,
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
            Text(
                text = stringResource(R.string.me_crop_hint),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = onNight.copy(alpha = 0.6f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 18.dp),
            )
        }

        Column(modifier = Modifier.background(night).padding(horizontal = 22.dp)) {
            Spacer(Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Image,
                    contentDescription = null,
                    tint = onNight.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp),
                )
                Slider(
                    value = zoom,
                    onValueChange = { value ->
                        val zoomed = if (zoom > 0f) offset * (value / zoom) else offset
                        zoom = value
                        offset = clampOffset(zoomed, value)
                    },
                    valueRange = MinZoom..MaxZoom,
                    colors = SliderDefaults.colors(
                        thumbColor = accent,
                        activeTrackColor = accent,
                        inactiveTrackColor = onNight.copy(alpha = 0.22f),
                    ),
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Filled.Image,
                    contentDescription = null,
                    tint = onNight.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(50))
                    .background(accent)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = stringResource(R.string.me_crop_done),
                        enabled = !exporting && circle > 0f,
                    ) {
                        exporting = true
                        val snapshotZoom = zoom
                        val snapshotOffset = offset
                        val snapshotCircle = circle
                        scope.launch {
                            val bytes = withContext(Dispatchers.Default) {
                                exportCrop(
                                    bitmap = bitmap,
                                    circle = snapshotCircle,
                                    zoom = snapshotZoom,
                                    offset = snapshotOffset,
                                )
                            }
                            onConfirm(bytes)
                        }
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterHorizontally),
            ) {
                if (exporting) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = stringResource(R.string.me_crop_done),
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private const val MinZoom = 1f
private const val MaxZoom = 2.6f
private const val InitialZoom = 1.24f
private const val CircleFraction = 0.72f
private const val MaxDecodeSide = 2048
private const val OutputSide = 640

private fun exportCrop(bitmap: Bitmap, circle: Float, zoom: Float, offset: Offset): ByteArray {
    val out = createBitmap(OutputSide, OutputSide)
    val canvas = android.graphics.Canvas(out)
    val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    val baseScale = circle / min(bitmap.width, bitmap.height)
    val scale = baseScale * zoom
    // The circle's center sits at `bitmapCenter - offset/scale` in bitmap
    // coordinates; its diameter covers `circle/scale` source pixels.
    val srcHalf = (circle / scale) / 2f
    val cx = bitmap.width / 2f - offset.x / scale
    val cy = bitmap.height / 2f - offset.y / scale
    val src = Rect(
        (cx - srcHalf).toInt().coerceIn(0, bitmap.width),
        (cy - srcHalf).toInt().coerceIn(0, bitmap.height),
        (cx + srcHalf).toInt().coerceIn(0, bitmap.width),
        (cy + srcHalf).toInt().coerceIn(0, bitmap.height),
    )
    canvas.drawBitmap(bitmap, src, Rect(0, 0, OutputSide, OutputSide), paint)
    return ByteArrayOutputStream().use { stream ->
        out.compress(Bitmap.CompressFormat.JPEG, JpegQuality, stream)
        stream.toByteArray()
    }
}

private const val JpegQuality = 88
