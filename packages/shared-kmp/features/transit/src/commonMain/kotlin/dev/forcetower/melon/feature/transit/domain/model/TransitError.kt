package dev.forcetower.melon.feature.transit.domain.model

enum class TransitError {
    Connection,

    // The account that asked signed out before the request could go; the
    // bearer by now may be another student's.
    SignedOut,
}
