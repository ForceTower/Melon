package dev.forcetower.melon.feature.library.data.dto

import dev.forcetower.melon.feature.library.domain.model.LibraryAvailabilitySnapshot
import dev.forcetower.melon.feature.library.domain.model.LibraryBranch
import dev.forcetower.melon.feature.library.domain.model.LibraryCopy
import dev.forcetower.melon.feature.library.domain.model.LibraryCopyStatus
import dev.forcetower.melon.feature.library.domain.model.LibraryFacetGroup
import dev.forcetower.melon.feature.library.domain.model.LibraryFacetValue
import dev.forcetower.melon.feature.library.domain.model.LibraryOverview
import dev.forcetower.melon.feature.library.domain.model.LibraryReading
import dev.forcetower.melon.feature.library.domain.model.LibraryRecentSearch
import dev.forcetower.melon.feature.library.domain.model.LibraryRecordField
import dev.forcetower.melon.feature.library.domain.model.LibrarySearchPage
import dev.forcetower.melon.feature.library.domain.model.LibrarySearchScope
import dev.forcetower.melon.feature.library.domain.model.LibraryWork
import dev.forcetower.melon.feature.library.domain.model.LibraryWorkTitle
import dev.forcetower.melon.feature.library.domain.model.LibraryWorkType
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.serialization.Serializable

// Wire bodies for `api/library/*`. Field names/optionality mirror the iOS
// DTOs in `LibraryRepository+Live.swift` — that file defined the contract
// client-first, so both platforms must decode the same shapes.

@Serializable
internal data class LibraryBranchBody(
    val code: String? = null,
    val name: String? = null,
    val slug: String? = null,
) {
    /**
     * The known slugs resolve to the local registry (which knows the campus
     * and near-ness); anything unmapped is carried through with its upstream
     * identity so no holding silently disappears.
     */
    fun toDomain(campus: String?): LibraryBranch {
        val known = slug?.let { s -> LibraryBranch.known.firstOrNull { it.id == s } }
        if (known != null) return known
        val name = name?.takeIf { it.isNotEmpty() } ?: code ?: ""
        return LibraryBranch(
            id = slug?.takeIf { it.isNotEmpty() } ?: code?.takeIf { it.isNotEmpty() } ?: name,
            sigla = sigla(name),
            name = name,
            campus = campus,
            isNear = false,
        )
    }

    companion object {
        /** Initials of the significant words — "Biblioteca do Campus" → "BC". */
        private fun sigla(name: String): String {
            val initials = name.split(" ")
                .filter { it.length > 2 }
                .mapNotNull { it.firstOrNull() }
                .take(4)
            return if (initials.isEmpty()) name.take(3).uppercase()
            else initials.joinToString("").uppercase()
        }
    }
}

@Serializable
internal data class LibraryCopyBody(
    val branch: LibraryBranchBody? = null,
    val campus: String? = null,
    val area: String? = null,
    val callNumber: String? = null,
    val status: String,
    val situation: String? = null,
    val dueDate: String? = null,
) {
    fun toDomain(workCallNumber: String?): LibraryCopy {
        val status = when (status) {
            "available" -> LibraryCopyStatus.Available
            "on_loan" -> LibraryCopyStatus.OnLoan(due = parseInstant(dueDate))
            "missing" -> LibraryCopyStatus.Missing
            else -> LibraryCopyStatus.LocalUse(note = situation ?: "")
        }
        return LibraryCopy(
            branch = (branch ?: LibraryBranchBody()).toDomain(campus),
            area = area ?: "",
            callNumber = callNumber ?: workCallNumber ?: "",
            status = status,
        )
    }
}

@Serializable
internal data class LibraryRecordFieldBody(
    val label: String,
    val value: String,
)

@Serializable
internal data class LibraryParsedTitleBody(
    val title: String,
    val subtitle: String? = null,
    val edition: String? = null,
    val junkYear: String? = null,
) {
    fun toDomain(): LibraryWorkTitle = LibraryWorkTitle(
        title = title,
        subtitle = subtitle,
        edition = edition,
        junkYear = junkYear,
    )
}

@Serializable
internal data class LibraryWorkBody(
    val id: String,
    val title: String,
    val parsedTitle: LibraryParsedTitleBody? = null,
    val callNumber: String? = null,
    val type: String? = null,
    val year: String? = null,
    val authors: List<String>? = null,
    val subjects: List<String>? = null,
    val branches: List<LibraryBranchBody>? = null,
    val isbn: String? = null,
    val language: String? = null,
    val edition: String? = null,
    val collection: String? = null,
    val series: String? = null,
    val reference: String? = null,
    val record: List<LibraryRecordFieldBody>? = null,
    val copies: List<LibraryCopyBody>? = null,
    val isNewAcquisition: Boolean? = null,
) {
    fun toDomain(): LibraryWork {
        val seen = mutableSetOf<String>()
        val branches = branches.orEmpty()
            .map { it.toDomain(campus = null) }
            .filter { seen.add(it.id) }
        return LibraryWork(
            id = id,
            rawTitle = title,
            callNumber = callNumber ?: "",
            // Books are the overwhelming majority, so an unmapped upstream
            // type reads as the neutral case rather than being dropped.
            type = LibraryWorkType.fromWire(type) ?: LibraryWorkType.Book,
            rawYear = year,
            authors = authors.orEmpty(),
            subjects = subjects.orEmpty(),
            branches = branches,
            rawIsbn = isbn,
            language = language,
            collection = collection,
            series = series,
            reference = reference,
            record = record.orEmpty().map { LibraryRecordField(label = it.label, value = it.value) },
            copies = copies.orEmpty().map { it.toDomain(workCallNumber = callNumber) },
            isNewAcquisition = isNewAcquisition ?: false,
            serverTitle = parsedTitle?.toDomain(),
        )
    }
}

@Serializable
internal data class LibraryRecentSearchBody(
    val query: String,
    val scope: String? = null,
    val resultCount: Int? = null,
)

@Serializable
internal data class LibraryOverviewBody(
    val recents: List<LibraryRecentSearchBody>? = null,
    val newAcquisitions: List<LibraryWorkBody>? = null,
) {
    fun toDomain(): LibraryOverview = LibraryOverview(
        recents = recents.orEmpty().map { recent ->
            LibraryRecentSearch(
                query = recent.query,
                scope = LibrarySearchScope.fromWire(recent.scope) ?: LibrarySearchScope.All,
                resultCount = recent.resultCount ?: 0,
            )
        },
        newAcquisitions = newAcquisitions.orEmpty().map { it.toDomain() },
    )
}

@Serializable
internal data class LibraryFacetValueBody(
    val key: String,
    val label: String,
    val count: Int,
)

@Serializable
internal data class LibrarySearchBody(
    val works: List<LibraryWorkBody>,
    val total: Int,
    val offset: Int,
    val facets: Map<String, List<LibraryFacetValueBody>>? = null,
    val servedFrom: String? = null,
    val upstreamAvailable: Boolean? = null,
) {
    fun toDomain(): LibrarySearchPage = LibrarySearchPage(
        works = works.map { it.toDomain() },
        total = total,
        offset = offset,
        // Unknown facet group keys are silently dropped so the backend can
        // ship new groups ahead of the clients.
        facets = facets.orEmpty().entries.mapNotNull { (raw, values) ->
            val group = LibraryFacetGroup.fromWire(raw) ?: return@mapNotNull null
            group to values.map { LibraryFacetValue(key = it.key, label = it.label, count = it.count) }
        }.toMap(),
    )
}

@Serializable
internal data class LibraryAvailabilityBody(
    val status: String,
    val checkedAt: String? = null,
    val copies: List<LibraryCopyBody>? = null,
) {
    fun toDomain(): LibraryAvailabilitySnapshot {
        val checkedAt = parseInstant(checkedAt) ?: Clock.System.now()
        return when (status) {
            "fresh" -> LibraryAvailabilitySnapshot(
                reading = LibraryReading.Fresh(checkedAt = checkedAt),
                copies = copies.orEmpty().map { it.toDomain(workCallNumber = null) },
            )
            "stale" -> LibraryAvailabilitySnapshot(
                reading = LibraryReading.Stale(checkedAt = checkedAt),
                copies = copies.orEmpty().map { it.toDomain(workCallNumber = null) },
            )
            else -> LibraryAvailabilitySnapshot(
                reading = LibraryReading.Unavailable,
                copies = emptyList(),
            )
        }
    }
}

// The backend stamps instants with `toISOString()`; `Instant.parse` accepts
// both the fractional and plain ISO-8601 forms.
private fun parseInstant(raw: String?): Instant? =
    raw?.let { runCatching { Instant.parse(it) }.getOrNull() }
