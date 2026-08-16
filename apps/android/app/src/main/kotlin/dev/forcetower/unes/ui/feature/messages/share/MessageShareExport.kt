package dev.forcetower.unes.ui.feature.messages.share

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.core.content.FileProvider
import androidx.core.content.getSystemService
import java.io.File

// 1080 px wide, matching the dc grid the card is drawn on.
private const val ExportWidthPx = 1080
private const val ShareDirectory = "share"
private const val ShareFileName = "mensagem-unes.png"
private const val MimePng = "image/png"
private const val MimeText = "text/plain"

// Records the card into its own graphics layer so the export is rasterized from
// the same composition the user is previewing. Replaying a Picture onto a
// canvas we allocate looks equivalent but silently captures nothing: Compose's
// `drawContent()` stays bound to the screen canvas.
internal fun Modifier.recordShareCard(layer: GraphicsLayer): Modifier = drawWithContent {
    layer.record { this@drawWithContent.drawContent() }
    drawLayer(layer)
}

// The dp width to lay the card out at so its pixel size lands on the dc's 1080
// grid — makes the export a 1:1 rasterization rather than a resample.
internal fun exportCardWidth(density: Float): Float = ExportWidthPx / density

internal suspend fun GraphicsLayer.toShareBitmap(): Bitmap? = runCatching {
    val captured = toImageBitmap().asAndroidBitmap()
    // Layer captures can come back hardware-backed, which neither scales nor
    // compresses; copy down before touching it.
    val software = if (captured.config == Bitmap.Config.HARDWARE) {
        captured.copy(Bitmap.Config.ARGB_8888, false)
    } else {
        captured
    }
    if (software.width == ExportWidthPx) {
        software
    } else {
        val height = (software.height * ExportWidthPx.toFloat() / software.width).toInt().coerceAtLeast(1)
        Bitmap.createScaledBitmap(software, ExportWidthPx, height, true)
    }
}.getOrNull()

internal fun shareImage(context: Context, bitmap: Bitmap, chooserTitle: String) {
    val directory = File(context.cacheDir, ShareDirectory).apply { mkdirs() }
    val file = File(directory, ShareFileName)
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = MimePng
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}

internal fun shareText(context: Context, text: String, chooserTitle: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = MimeText
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}

internal fun copyText(context: Context, text: String) {
    context.getSystemService<ClipboardManager>()
        ?.setPrimaryClip(ClipData.newPlainText(null, text))
}

// Android 13 raises its own "copied" confirmation, so repeating it in a
// snackbar would stack two toasts saying the same thing.
internal val platformConfirmsCopy: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

// MediaStore's scoped Downloads collection needs no permission, but it only
// exists from Q up — below that the sheet hides the save action rather than
// asking for storage access just for this.
internal val canSaveToDownloads: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

internal fun saveImageToDownloads(context: Context, bitmap: Bitmap): Boolean {
    if (!canSaveToDownloads) return false
    val values = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, ShareFileName)
        put(MediaStore.Downloads.MIME_TYPE, MimePng)
        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        put(MediaStore.Downloads.IS_PENDING, 1)
    }
    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
    return runCatching {
        resolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            ?: error("no output stream for $uri")
        resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
        true
    }.getOrElse {
        resolver.delete(uri, null, null)
        false
    }
}
