import SwiftUI

/// One value-prop slide of the intro carousel.
struct IntroSlide: Identifiable {
    struct Segment {
        var text: String
        var accented = false
    }

    enum Illustration {
        case schedule, grades, messages, notifications
    }

    let id: Int
    let variant: MeshView.Variant
    let eyebrow: String
    let accent: Color
    /// Title lines; accented segments render in the slide accent color.
    let titleLines: [[Segment]]
    let body: String
    let illustration: Illustration

    static let all: [IntroSlide] = [
        IntroSlide(
            id: 0,
            variant: .cool,
            eyebrow: String.localized(.onboardingIntroScheduleEyebrow),
            accent: UNESColor.teal,
            titleLines: [
                [Segment(text: String.localized(.onboardingIntroScheduleTitleLine1))],
                [Segment(text: String.localized(.onboardingIntroScheduleTitleLine2), accented: true)],
            ],
            body: String.localized(.onboardingIntroScheduleBody),
            illustration: .schedule
        ),
        IntroSlide(
            id: 1,
            variant: .sun,
            eyebrow: String.localized(.onboardingIntroGradesEyebrow),
            accent: UNESColor.tangerine,
            titleLines: [
                [Segment(text: String.localized(.onboardingIntroGradesTitleLine1))],
                [Segment(text: String.localized(.onboardingIntroGradesTitleLine2), accented: true)],
            ],
            body: String.localized(.onboardingIntroGradesBody),
            illustration: .grades
        ),
        IntroSlide(
            id: 2,
            variant: .rose,
            eyebrow: String.localized(.onboardingIntroMessagesEyebrow),
            accent: UNESColor.magenta,
            titleLines: [
                [Segment(text: String.localized(.onboardingIntroMessagesTitleLine1))],
                [Segment(text: String.localized(.onboardingIntroMessagesTitleLine2), accented: true)],
            ],
            body: String.localized(.onboardingIntroMessagesBody),
            illustration: .messages
        ),
        IntroSlide(
            id: 3,
            variant: .warm,
            eyebrow: String.localized(.onboardingIntroNotificationsEyebrow),
            accent: UNESColor.accent,
            titleLines: [
                [Segment(text: String.localized(.onboardingIntroNotificationsTitleLine1))],
                [
                    Segment(text: String.localized(.onboardingIntroNotificationsTitleLine2A)),
                    Segment(text: String.localized(.onboardingIntroNotificationsTitleLine2B), accented: true),
                ],
            ],
            body: String.localized(.onboardingIntroNotificationsBody),
            illustration: .notifications
        ),
    ]
}
