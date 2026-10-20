package dev.forcetower.melon.feature.transit.domain.model

// One saved direction of "Meu trajeto": where the student boards and which
// lines they actually take there.
data class CommuteLeg(
    val stopId: String,
    val stopName: String,
    val stopArea: String?,
    val lineNumbers: List<String>,
    // From home (or class) to the stop.
    val walkMinutes: Int,
)

// The student's saved commute. The server owns it (`api/transit/plan`); the
// device mirrors whatever the server last answered.
data class CommutePlan(
    val toCampus: CommuteLeg? = null,
    val fromCampus: CommuteLeg? = null,
) {
    val isEmpty: Boolean get() = toCampus == null && fromCampus == null

    val directions: List<TransitDirection>
        get() = TransitDirection.entries.filter { leg(it) != null }

    fun leg(direction: TransitDirection): CommuteLeg? =
        when (direction) {
            TransitDirection.ToCampus -> toCampus
            TransitDirection.FromCampus -> fromCampus
        }

    fun withLeg(
        direction: TransitDirection,
        leg: CommuteLeg?,
    ): CommutePlan =
        when (direction) {
            TransitDirection.ToCampus -> copy(toCampus = leg)
            TransitDirection.FromCampus -> copy(fromCampus = leg)
        }
}
