package dev.forcetower.melon.feature.me.domain.model

// Failure modes of a profile-customization save. `Rejected` is a 400 — the
// payload failed validation (name over the limit, picture bytes not matching
// the declared type, over 5 MB); everything the client builds should already
// respect those bounds, so the UI folds it into the generic error copy.
enum class ProfileUpdateError { Connection, Rejected, Unexpected }
