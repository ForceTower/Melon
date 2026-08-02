package dev.forcetower.melon.feature.library.domain.usecase

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.library.data.network.LibraryService
import dev.forcetower.melon.feature.library.domain.model.LibraryAvailabilitySnapshot
import dev.forcetower.melon.feature.library.domain.model.LibraryError
import dev.forcetower.melon.feature.library.domain.model.LibraryOverview
import dev.forcetower.melon.feature.library.domain.model.LibrarySearchPage
import dev.forcetower.melon.feature.library.domain.model.LibrarySearchRequest
import dev.zacsweers.metro.Inject

// Biblioteca is online-only, so every use case is a live request — no flows,
// no mirror. Grouped in one file because each is a one-line delegation; the
// behavioral contract lives on `LibraryService`.

// Search-entry payload: server-kept recent searches plus the "novas no
// acervo" shelf (degraded record shape — availability comes on open).
@Inject
class GetLibraryOverviewUseCase internal constructor(
    private val service: LibraryService,
) {
    suspend operator fun invoke(): Outcome<LibraryOverview, LibraryError> = service.overview()
}

// One page of the catalogue search — sorting, faceting and pagination run
// server-side; facet counts are identical on every page of the same query.
@Inject
class SearchLibraryUseCase internal constructor(
    private val service: LibraryService,
) {
    suspend operator fun invoke(
        request: LibrarySearchRequest,
    ): Outcome<LibrarySearchPage, LibraryError> = service.search(request)
}

// The per-work circulation consultation. Never fails: Pergamum going quiet
// degrades to `LibraryReading.Unavailable`, which the screens narrate.
@Inject
class CheckLibraryAvailabilityUseCase internal constructor(
    private val service: LibraryService,
) {
    suspend operator fun invoke(workId: String): LibraryAvailabilitySnapshot =
        service.availability(workId)
}

// Wipes the server-kept recent searches; callers hide them optimistically.
@Inject
class ClearLibraryRecentsUseCase internal constructor(
    private val service: LibraryService,
) {
    suspend operator fun invoke(): Outcome<Unit, LibraryError> = service.clearRecents()
}
