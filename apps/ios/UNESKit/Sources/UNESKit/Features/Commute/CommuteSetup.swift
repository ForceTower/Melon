import SwiftUI

/// What the setup screens hand each other: the directions chosen on the
/// first screen, the legs saved so far, and the plan being replaced (only
/// read for prefill).
struct CommuteSetupDraft: Equatable, Sendable {
    var directions: [TransitDirection]
    var plan = CommutePlan()
    var existing: CommutePlan

    var totalSteps: Int {
        1 + (directions.contains(.toCampus) ? 2 : 0) + (directions.contains(.fromCampus) ? 1 : 0)
    }

    /// The step number of the lines screen for `direction`.
    func linesStep(for direction: TransitDirection) -> Int {
        direction == .toCampus ? 3 : totalSteps
    }

    /// The direction still waiting for its lines after `direction`, if any.
    func direction(after direction: TransitDirection) -> TransitDirection? {
        guard let index = directions.firstIndex(of: direction) else { return nil }
        return directions.dropFirst(index + 1).first
    }
}

/// The setup screens' shared shape: step counter, large question, content,
/// and a pinned call to action over a fade.
struct CommuteSetupScaffold<Content: View, Footer: View>: View {
    var step: Int
    var total: Int
    var title: LocalizedStringResource
    var subtitle: LocalizedStringResource?
    @ViewBuilder var content: Content
    @ViewBuilder var footer: Footer

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                Text(.commuteSetupStep(step, total))
                    .font(.system(size: 11, weight: .semibold))
                    .tracking(0.66)
                    .monospacedDigit()
                    .foregroundStyle(UNESColor.ink4)
                    .textCase(.uppercase)
                Text(title)
                    .font(.system(size: 30, weight: .bold))
                    .tracking(-1.05)
                    .foregroundStyle(UNESColor.ink)
                    .padding(.top, 6)
                    .fixedSize(horizontal: false, vertical: true)
                if let subtitle {
                    Text(subtitle)
                        .font(.system(size: 14.5))
                        .foregroundStyle(UNESColor.ink3)
                        .lineSpacing(2)
                        .padding(.top, 8)
                        .fixedSize(horizontal: false, vertical: true)
                }
                content
                    .padding(.top, 18)
            }
            .padding(EdgeInsets(top: 6, leading: 20, bottom: 24, trailing: 20))
        }
        .scrollIndicators(.hidden)
        .scrollDismissesKeyboard(.interactively)
        .background(UNESColor.surface.ignoresSafeArea())
        .safeAreaInset(edge: .bottom) {
            VStack(spacing: 10) {
                footer
            }
            .padding(EdgeInsets(top: 14, leading: 16, bottom: 8, trailing: 16))
            .background {
                LinearGradient(
                    stops: [.init(color: UNESColor.surface.opacity(0), location: 0), .init(color: UNESColor.surface, location: 0.3)],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .ignoresSafeArea()
            }
        }
        .inlineNavigationBar()
        .hiddenTabBar()
    }
}

/// The round check the setup lists use for selection.
struct CommuteCheckmark: View {
    var isOn: Bool

    var body: some View {
        ZStack {
            Circle()
                .strokeBorder(isOn ? UNESColor.ink : UNESColor.ink4, lineWidth: 1.5)
                .background(Circle().fill(isOn ? UNESColor.ink : .clear))
            if isOn {
                Image(systemName: "checkmark")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundStyle(UNESColor.surface)
            }
        }
        .frame(width: 24, height: 24)
        .animation(UNESMotion.ease(0.2, overshoot: 1.4), value: isOn)
    }
}
