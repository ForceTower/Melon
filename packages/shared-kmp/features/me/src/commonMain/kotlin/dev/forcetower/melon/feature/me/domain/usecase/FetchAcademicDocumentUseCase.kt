package dev.forcetower.melon.feature.me.domain.usecase

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.me.data.network.DocumentsService
import dev.forcetower.melon.feature.me.domain.model.AcademicDocument
import dev.forcetower.melon.feature.me.domain.model.DocumentFetchError
import dev.forcetower.melon.feature.me.domain.model.FetchedAcademicDocument
import dev.zacsweers.metro.Inject

// Requests an official PDF through the backend, optionally carrying the
// solved reCAPTCHA token when remote config gates the portal behind one.
@Inject
class FetchAcademicDocumentUseCase internal constructor(
    private val service: DocumentsService,
) {
    suspend operator fun invoke(
        document: AcademicDocument,
        captchaToken: String? = null,
    ): Outcome<FetchedAcademicDocument, DocumentFetchError> =
        service.fetch(document, captchaToken)
}
