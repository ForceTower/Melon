package dev.forcetower.melon.feature.library.domain.model

import kotlin.time.Instant

// Biblioteca — the university library catalogue (Pergamum). Ports the iOS
// domain in `UNESKit/Domain/Models/Library.swift`, which defined the model
// client-first. The catalogue's raw strings are dirty (titles carry editions
// and trailing junk, the year is text, the ISBN field mixes notes with the
// number), so parsing happens at the edge and the UI stays honest about what
// the catalogue actually holds.

enum class LibraryError { Connection }

/// Work types as the catalogue classifies them. Books are the overwhelming
/// majority, so an unmapped upstream type reads as the neutral case.
enum class LibraryWorkType(val wire: String) {
    Book("livro"),
    Pamphlet("folheto"),
    Cordel("cordel"),
    EducationalProduct("produto"),
    Dissertation("dissertacao"),
    Thesis("tese"),
    Article("artigo"),
    Periodical("periodico"),
    ;

    companion object {
        fun fromWire(raw: String?): LibraryWorkType? = entries.firstOrNull { it.wire == raw }
    }
}

/// A campus library. The backend maps upstream branches to the known slugs
/// where it can; anything it cannot map still arrives with its code and name,
/// so this is a data class rather than a closed enum.
data class LibraryBranch(
    /** Stable identity — the app slug when known, else the upstream code. */
    val id: String,
    val sigla: String,
    val name: String,
    val campus: String?,
    /**
     * On the student's own campus — an available copy here means "walk over
     * and pick it up today".
     */
    val isNear: Boolean,
) {
    /** Compact label for refine rows. */
    val shortName: String get() = shortNames[id] ?: name

    companion object {
        val central = LibraryBranch(
            id = "bcjc", sigla = "BCJC", name = "Biblioteca Central Julieta Carteado",
            campus = "Feira de Santana", isNear = true,
        )
        val health = LibraryBranch(
            id = "saude", sigla = "BSS", name = "Biblioteca Setorial de Saúde",
            campus = "Feira de Santana", isNear = true,
        )
        val lencois = LibraryBranch(
            id = "lencois", sigla = "BAL", name = "Biblioteca do Campus Avançado",
            campus = "Lençóis", isNear = false,
        )
        val santoAntonio = LibraryBranch(
            id = "saj", sigla = "BSAJ", name = "Biblioteca do Campus Avançado",
            campus = "Santo Antônio de Jesus", isNear = false,
        )

        /** The mapped campus libraries, in display order. */
        val known = listOf(central, health, lencois, santoAntonio)

        private val shortNames = mapOf(
            "bcjc" to "Central Julieta Carteado",
            "saude" to "Setorial de Saúde",
            "lencois" to "Campus de Lençóis",
            "saj" to "Campus de Sto. Antônio de Jesus",
        )
    }
}

/// One physical copy's circulation status.
sealed interface LibraryCopyStatus {
    data object Available : LibraryCopyStatus

    /**
     * [due] is null when the loan record carries no credible return date —
     * the backend nulls decades-old dues rather than forecasting the past.
     */
    data class OnLoan(val due: Instant?) : LibraryCopyStatus

    /** In the catalogue but not on the shelf — excluded from every count. */
    data object Missing : LibraryCopyStatus

    /** Reference-only material that never leaves the building. */
    data class LocalUse(val note: String) : LibraryCopyStatus
}

data class LibraryCopy(
    val branch: LibraryBranch,
    /** Shelf area — "Coleção Geral", "Coleção Cordel". */
    val area: String,
    /** Full per-copy call number, including edition/volume suffixes. */
    val callNumber: String,
    val status: LibraryCopyStatus,
)

/// One label/value row of the catalogue record ("ficha"). Labels come from
/// the catalogue itself — an ordered listing, not a schema.
data class LibraryRecordField(
    val label: String,
    val value: String,
)

/// The `obra` string decomposed: title, subtitle, inline edition, and the
/// trailing "/ 0000" junk the catalogue appends.
data class LibraryWorkTitle(
    val title: String,
    val subtitle: String? = null,
    val edition: String? = null,
    /** The trailing year-or-zeros fragment, kept so the UI can call it noise. */
    val junkYear: String? = null,
)

/// `ano_publicacao` is text in the source; sometimes it isn't a year at all.
sealed interface LibraryYear {
    data class Year(val text: String) : LibraryYear
    data class Illegible(val text: String) : LibraryYear
    data object None : LibraryYear
}

data class LibraryIsbn(
    /** Digits only, 10 or 13 — enough to look up covers or copy. */
    val value: String?,
    /** As printed in the record, hyphens kept. */
    val pretty: String?,
    /** Whatever else the field carried — "Broch." and friends. */
    val note: String?,
)

/// One catalogue work, keeping the record's dirty raw strings.
data class LibraryWork(
    /** The catalogue's `cod_acervo`. */
    val id: String,
    /** The raw `obra` string — "Cálculo : um novo horizonte - 6. ed / 0000". */
    val rawTitle: String,
    /** The classification / call number — "515 A638c". */
    val callNumber: String,
    val type: LibraryWorkType,
    /** Raw `ano_publicacao` — TEXT in the source, not always a year. */
    val rawYear: String?,
    /** From the reliable facet array; empty when only the degraded shape came. */
    val authors: List<String>,
    val subjects: List<String>,
    val branches: List<LibraryBranch>,
    /** Raw `isbn_issn` — dirty: "85-7307-655-3: (Broch.)", "(Broch.)", null. */
    val rawIsbn: String?,
    val language: String?,
    val collection: String?,
    val series: String?,
    /** Ready ABNT citation as inline markdown, when the listing carried one. */
    val reference: String?,
    val record: List<LibraryRecordField>,
    val copies: List<LibraryCopy>,
    /** Came from the "novas aquisições" listing — the degraded record shape. */
    val isNewAcquisition: Boolean = false,
    /**
     * The backend's decomposition of [rawTitle], parsed once at scrape time.
     * Null only for fixture data, which falls back to the local parser.
     */
    val serverTitle: LibraryWorkTitle? = null,
) {
    val parsedTitle: LibraryWorkTitle get() = serverTitle ?: parseTitle(rawTitle)

    val year: LibraryYear
        get() {
            val raw = rawYear?.takeIf { it.isNotEmpty() } ?: return LibraryYear.None
            val match = yearRegex.find(raw) ?: return LibraryYear.Illegible(raw)
            return LibraryYear.Year(match.value)
        }

    val isbn: LibraryIsbn?
        get() = rawIsbn?.takeIf { it.isNotEmpty() }?.let(::parseIsbn)

    /**
     * Edition for the meta line — the one parsed out of the title wins over
     * the (rarer) dedicated field.
     */
    val edition: String? get() = parsedTitle.edition

    /** Numeric year for sorting; unparseable years read as 0. */
    val sortYear: Int
        get() = (year as? LibraryYear.Year)?.text?.toIntOrNull() ?: 0

    /** `now` decides which due dates still count as "coming back". */
    fun availability(now: Instant): LibraryAvailability {
        var available = 0
        var onLoan = 0
        var missing = 0
        var localUse = 0
        var nextDue: Instant? = null
        val order = mutableListOf<LibraryBranch>()
        val byBranch = mutableMapOf<LibraryBranch, BranchTally>()
        for (copy in copies) {
            val tally = byBranch.getOrPut(copy.branch) {
                order.add(copy.branch)
                BranchTally()
            }
            when (val status = copy.status) {
                is LibraryCopyStatus.Available -> {
                    available++
                    tally.available++
                    tally.total++
                }
                is LibraryCopyStatus.OnLoan -> {
                    onLoan++
                    tally.onLoan++
                    tally.total++
                    val due = status.due
                    val earliest = nextDue
                    if (due != null && due > now && (earliest == null || due < earliest)) {
                        nextDue = due
                    }
                }
                is LibraryCopyStatus.Missing -> {
                    missing++
                    tally.missing++
                }
                is LibraryCopyStatus.LocalUse -> {
                    localUse++
                    tally.localUse++
                    tally.total++
                }
            }
            if (copy.area !in tally.areas) tally.areas.add(copy.area)
        }
        return LibraryAvailability(
            available = available,
            onLoan = onLoan,
            missing = missing,
            localUse = localUse,
            total = copies.size - missing,
            nextDue = nextDue,
            branches = order.map { branch ->
                val tally = byBranch.getValue(branch)
                LibraryAvailability.Branch(
                    branch = branch,
                    available = tally.available,
                    onLoan = tally.onLoan,
                    missing = tally.missing,
                    localUse = tally.localUse,
                    total = tally.total,
                    areas = tally.areas.toList(),
                )
            },
        )
    }

    private class BranchTally {
        var available = 0
        var onLoan = 0
        var missing = 0
        var localUse = 0
        var total = 0
        val areas = mutableListOf<String>()
    }

    companion object {
        private val yearRegex = Regex("""\d{4}""")
        private val junkYearRegex = Regex("""0+|\d{4}""")
        private val editionRegex = Regex("""\s[-–]\s(\d+\.?\s?ed[^/]*)$""", RegexOption.IGNORE_CASE)
        private val subtitleSplitRegex = Regex("""\s+:\s+""")
        private val isbnRegex = Regex("""[\dX][\dX\- ]{8,}[\dX]""", RegexOption.IGNORE_CASE)
        private val whitespaceRegex = Regex("""\s+""")
        private val isbnNoiseRegex = Regex("""[():]""")

        fun parseTitle(raw: String): LibraryWorkTitle {
            var text = raw.trim()
            var junkYear: String? = null
            val slash = text.lastIndexOf(" / ")
            if (slash >= 0) {
                val trailing = text.substring(slash + 3).trim()
                if (junkYearRegex.matches(trailing)) junkYear = trailing
                text = text.substring(0, slash).trim()
            }
            var edition: String? = null
            val editionMatch = editionRegex.find(text)
            if (editionMatch != null) {
                edition = editionMatch.groupValues[1].trim().replace(whitespaceRegex, " ")
                text = text.substring(0, editionMatch.range.first).trim()
            }
            val parts = text.split(subtitleSplitRegex, limit = 2)
            return LibraryWorkTitle(
                title = parts.firstOrNull() ?: text,
                subtitle = parts.getOrNull(1),
                edition = edition,
                junkYear = junkYear,
            )
        }

        fun parseIsbn(raw: String): LibraryIsbn {
            val match = isbnRegex.find(raw)
            if (match == null) {
                val note = raw.replace(isbnNoiseRegex, "").trim()
                return LibraryIsbn(value = null, pretty = null, note = note.ifEmpty { null })
            }
            val pretty = match.value.trim()
            val digits = match.value.filter { it.isDigit() || it == 'X' || it == 'x' }
            val note = raw.replace(match.value, "")
                .replace(isbnNoiseRegex, "")
                .trim()
                .ifEmpty { null }
            if (digits.length != 10 && digits.length != 13) {
                return LibraryIsbn(value = null, pretty = null, note = note)
            }
            return LibraryIsbn(value = digits, pretty = pretty, note = note)
        }
    }
}

/// Copy counts with missing copies excluded from every denominator: a title
/// with 122 catalogued copies and 22 missing reads "51 de 100", never
/// "51 de 122".
data class LibraryAvailability(
    val available: Int,
    val onLoan: Int,
    val missing: Int,
    val localUse: Int,
    /** Copies that count — everything but missing. */
    val total: Int,
    /** Earliest future due date across loans; past dues are stale records. */
    val nextDue: Instant?,
    val branches: List<Branch>,
) {
    data class Branch(
        val branch: LibraryBranch,
        val available: Int,
        val onLoan: Int,
        val missing: Int,
        val localUse: Int,
        /** Copies that count — everything but missing. */
        val total: Int,
        val areas: List<String>,
    )

    /** Copies that circulate at all — local-use ones never leave the building. */
    val lendable: Int get() = total - localUse

    enum class Verdict {
        /** At least one copy free on a shelf. */
        Available,

        /** Every circulating copy is out. */
        AllOnLoan,

        /** Nothing circulates — reference-only material. */
        LocalUseOnly,
    }

    val verdict: Verdict
        get() = when {
            lendable == 0 && localUse > 0 -> Verdict.LocalUseOnly
            available > 0 -> Verdict.Available
            else -> Verdict.AllOnLoan
        }

    /** A free copy on the student's own campus. */
    val hasNearAvailable: Boolean
        get() = branches.any { it.branch.isNear && it.available > 0 }
}

/// One consultation of the circulation system. The catalogue itself is
/// reliable; the copy counts are only as good as the last time Pergamum
/// answered — the UI always says when that was.
sealed interface LibraryReading {
    data class Fresh(val checkedAt: Instant) : LibraryReading

    /** Pergamum stopped answering; this is the last known reading. */
    data class Stale(val checkedAt: Instant) : LibraryReading

    /** Pergamum is down and there is no reading to fall back to. */
    data object Unavailable : LibraryReading
}

/// The availability endpoint's answer: the reading plus the live copies.
data class LibraryAvailabilitySnapshot(
    val reading: LibraryReading,
    val copies: List<LibraryCopy>,
)

enum class LibrarySearchScope(val wire: String) {
    All("all"),
    Title("titulo"),
    Author("autor"),
    Subject("assunto"),
    Isbn("isbn"),
    CallNumber("chamada"),
    ;

    companion object {
        fun fromWire(raw: String?): LibrarySearchScope? = entries.firstOrNull { it.wire == raw }
    }
}

/// How an advanced-search term joins the one before it — E / OU / NÃO.
/// Wire values are the contract.
enum class LibrarySearchOperator(val wire: String) {
    And("and"),
    Or("or"),
    Not("not"),
}

/// One term of a (possibly boolean) catalogue search. A plain search is a
/// single term; the advanced sheet sends up to three.
data class LibrarySearchTerm(
    val query: String,
    val scope: LibrarySearchScope,
    /** Ignored on the first term. */
    val op: LibrarySearchOperator = LibrarySearchOperator.And,
)

/// Compact one-line rendering — "cálculo + guidorizzi − geometria".
fun List<LibrarySearchTerm>.displayQuery(): String {
    val first = firstOrNull() ?: return ""
    return drop(1).fold(first.query) { text, term ->
        val joiner = when (term.op) {
            LibrarySearchOperator.And -> " + "
            LibrarySearchOperator.Or -> " / "
            LibrarySearchOperator.Not -> " − "
        }
        text + joiner + term.query
    }
}

enum class LibrarySort(val wire: String) {
    Relevance("relevance"),
    Newest("newest"),
    Oldest("oldest"),
    TitleAZ("title"),
}

enum class LibraryFacetGroup(val wire: String) {
    Type("type"),
    Branch("branch"),
    Subject("subject"),
    Author("author"),
    Language("language"),
    Year("year"),
    ;

    companion object {
        fun fromWire(raw: String?): LibraryFacetGroup? = entries.firstOrNull { it.wire == raw }
    }
}

/// One refine option with its count over the unfiltered result set.
data class LibraryFacetValue(
    val key: String,
    val label: String,
    val count: Int,
)

/// Selected facet keys per group. Within a group selections add up (OR);
/// across groups they narrow (AND).
typealias LibraryFacetSelection = Map<LibraryFacetGroup, Set<String>>

/// Publication-decade buckets for the year facet.
enum class LibraryYearBucket(val wire: String) {
    From2020("2020"),
    Decade2010("2010"),
    Decade2000("2000"),
    Decade1990("1990"),
    Before1990("old"),
    Unknown("none"),
    ;

    companion object {
        fun fromWire(raw: String?): LibraryYearBucket? = entries.firstOrNull { it.wire == raw }
    }
}

/// One page of a search response. Sorting, faceting and pagination run
/// server-side over the full result set; [facets] counts the unfaceted set
/// and is identical on every page of the same query.
data class LibrarySearchPage(
    val works: List<LibraryWork>,
    val total: Int,
    val offset: Int,
    val facets: Map<LibraryFacetGroup, List<LibraryFacetValue>>,
)

/// One search call: the terms plus the server-side page/sort/facet options.
data class LibrarySearchRequest(
    val terms: List<LibrarySearchTerm>,
    val sort: LibrarySort = LibrarySort.Relevance,
    val facets: LibraryFacetSelection = emptyMap(),
    val offset: Int = 0,
    val limit: Int = 25,
)

data class LibraryRecentSearch(
    val query: String,
    val scope: LibrarySearchScope,
    val resultCount: Int,
) {
    val id: String get() = "${scope.wire}|$query"
}

/// The search-entry payload: recent searches plus the "novas no acervo"
/// shelf (which arrives in the degraded record shape).
data class LibraryOverview(
    val recents: List<LibraryRecentSearch>,
    val newAcquisitions: List<LibraryWork>,
)
