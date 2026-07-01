import SwiftUI

/// Motion curves from the v2 design language.
enum UNESMotion {
    /// The v2 entrance family — `cubic-bezier(.2, .9, .3, overshoot)`.
    /// Raise `overshoot` past 1 for spring-like pops (1.2 buttons, 1.5 checkmarks, 1.6 dots).
    static func ease(_ duration: Double = 0.6, overshoot: Double = 1) -> Animation {
        .timingCurve(0.2, 0.9, 0.3, overshoot, duration: duration)
    }

    /// Soft settle for progress fills — `cubic-bezier(.2, .8, .2, 1)`.
    static func settle(_ duration: Double = 0.6) -> Animation {
        .timingCurve(0.2, 0.8, 0.2, 1, duration: duration)
    }

    /// Stroke draws (checkmarks) — `cubic-bezier(.4, 0, .2, 1)`.
    static func draw(_ duration: Double = 0.45) -> Animation {
        .timingCurve(0.4, 0, 0.2, 1, duration: duration)
    }
}
