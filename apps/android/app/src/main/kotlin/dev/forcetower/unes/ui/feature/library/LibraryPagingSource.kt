package dev.forcetower.unes.ui.feature.library

import androidx.paging.PagingSource
import androidx.paging.PagingState
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.library.domain.model.LibrarySearchPage
import dev.forcetower.melon.feature.library.domain.model.LibrarySearchRequest
import dev.forcetower.melon.feature.library.domain.model.LibraryWork
import dev.forcetower.melon.feature.library.domain.usecase.SearchLibraryUseCase

// Offset-keyed source over `api/library/search`. The server owns sorting,
// faceting and the total; each load is one page of 25. The result set can
// shift between pages when the server-side cache refreshes mid-scroll, so ids
// already emitted by this source instance are dropped — same posture as the
// iOS append filter.
internal class LibraryPagingSource(
    private val search: SearchLibraryUseCase,
    private val request: LibrarySearchRequest,
    private val onPage: (LibrarySearchPage) -> Unit,
) : PagingSource<Int, LibraryWork>() {

    private val seenIds = mutableSetOf<String>()

    override suspend fun load(
        params: LoadParams<Int>,
    ): LoadResult<Int, LibraryWork> {
        val offset = params.key ?: 0
        val page = when (val outcome = search(request.copy(offset = offset))) {
            is Outcome.Ok -> outcome.value
            is Outcome.Err -> return LoadResult.Error(LibrarySearchException())
        }
        onPage(page)
        val fresh = page.works.filter { seenIds.add(it.id) }
        val loadedThrough = offset + page.works.size
        val nextKey = loadedThrough.takeIf { page.works.isNotEmpty() && it < page.total }
        return LoadResult.Page(
            data = fresh,
            prevKey = null,
            nextKey = nextKey,
        )
    }

    // Facet/sort changes rebuild the pager wholesale, so a refresh only
    // happens on retry — restart from the top rather than mid-set.
    override fun getRefreshKey(
        state: PagingState<Int, LibraryWork>,
    ): Int? = null
}

internal class LibrarySearchException : Exception("library search failed")
