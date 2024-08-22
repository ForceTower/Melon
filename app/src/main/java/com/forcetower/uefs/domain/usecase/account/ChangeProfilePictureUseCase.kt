package com.forcetower.uefs.domain.usecase.account

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.net.Uri
import android.util.Base64
import com.forcetower.uefs.core.storage.repository.cloud.EdgeAccountRepository
import dagger.Reusable
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Reusable
class ChangeProfilePictureUseCase @Inject constructor(
    private val repository: EdgeAccountRepository,
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke(uri: Uri) = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val stream = resolver.openInputStream(uri) ?: return@withContext

        val image = BitmapFactory.decodeStream(stream)
        image ?: return@withContext

        val bitmap = ThumbnailUtils.extractThumbnail(image, 1080, 1080)

        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
        val data = baos.toByteArray()

        val encoded = Base64.encodeToString(data, Base64.DEFAULT)

        repository.uploadPicture(encoded)
    }
}
