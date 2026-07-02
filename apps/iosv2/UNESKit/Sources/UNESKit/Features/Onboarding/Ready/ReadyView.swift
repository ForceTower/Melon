import ComposableArchitecture
import SwiftUI

struct ReadyView: View {
    let store: StoreOf<ReadyFeature>

    var body: some View {
        ZStack(alignment: .top) {
            UNESColor.surface.ignoresSafeArea()
            successWash

            ScrollView {
                VStack(spacing: 0) {
                    checkBadge
                        .popIn(delay: 0.1, duration: 0.6, from: 0.5, overshoot: 1.5)

                    Eyebrow(text: String.localized(.onboardingReadyEyebrow), color: UNESColor.successGreen, live: true)
                        .padding(.top, 14)
                        .fadeUp(delay: 0.85, duration: 0.5)

                    title
                        .padding(.top, 12)
                        .fadeUp(delay: 0.95, duration: 0.6)

                    Text(subtitle)
                        .font(.system(size: 15, weight: .medium))
                        .monospacedDigit()
                        .foregroundStyle(UNESColor.ink3)
                        .padding(.top, 6)
                        .fadeUp(delay: 1.02, duration: 0.6)

                    if let nextClass = store.overview.nextClass {
                        NextClassHero(nextClass: nextClass)
                            .padding(.top, 22)
                            .scaleIn(delay: 1.12, duration: 0.6)
                    }

                    HStack(spacing: 12) {
                        CoefficientWidget(
                            coefficient: store.overview.coefficient,
                            spark: store.overview.gradeSpark
                        )
                        AttendanceWidget(percent: store.overview.attendancePercent)
                    }
                    .padding(.top, 12)
                    .fadeUp(delay: 1.22, duration: 0.6)
                }
                .padding(.horizontal, 20)
                .padding(.top, 24)
            }
            .scrollBounceBehavior(.basedOnSize)
        }
        .safeAreaInset(edge: .bottom) { enterButton }
        .navigationBarBackButtonHidden()
        .bareNavigationBar()
    }

    private var successWash: some View {
        // Fading the mesh's own alpha (instead of painting surface over it)
        // guarantees a seamless blend into the surface background.
        MeshView(variant: .fresh, intensity: 0.85)
            .frame(height: 300)
            .padding(.horizontal, -50)
            .mask {
                LinearGradient(
                    stops: [
                        .init(color: .white, location: 0),
                        .init(color: .clear, location: 0.92),
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
            }
            .opacity(0.5)
            .offset(y: -80)
            .ignoresSafeArea()
    }

    private var checkBadge: some View {
        ZStack {
            Circle()
                .fill(UNESColor.accent)
            DelayedCheck(delay: 0.5, duration: 0.45)
        }
        .frame(width: 68, height: 68)
        .shadow(color: UNESColor.accent.opacity(0.32), radius: 15, y: 12)
    }

    private var title: some View {
        (
            Text(.onboardingReadyTitle).foregroundStyle(UNESColor.ink)
                + Text(verbatim: "\(store.userName).").foregroundStyle(UNESColor.accent)
        )
        .font(.system(size: 38, weight: .heavy))
        .tracking(-1.52)
        .multilineTextAlignment(.center)
    }

    private var subtitle: String {
        var parts: [String] = []
        if store.overview.classCount > 0 {
            parts.append(String.localized(.onboardingReadyClassCount(store.overview.classCount)))
        }
        if store.overview.totalCredits > 0 {
            parts.append(String.localized(.onboardingReadyCreditCount(store.overview.totalCredits)))
        }
        if let code = store.overview.semesterCode {
            parts.append(String.localized(.onboardingReadySemester(Self.formatSemesterCode(code))))
        }
        return parts.isEmpty ? String.localized(.onboardingReadyAllSynced) : parts.joined(separator: " · ")
    }

    /// Upstream semester codes are "20192"-style; render as "2019.2".
    static func formatSemesterCode(_ code: String) -> String {
        guard code.count >= 5, code.allSatisfy(\.isNumber) else { return code }
        return "\(code.prefix(4)).\(code.dropFirst(4))"
    }

    private var enterButton: some View {
        Button {
            store.send(.enterTapped)
        } label: {
            UNESButtonLabel(text: .onboardingReadyEnter)
        }
        .buttonStyle(.unesDark)
        .padding(.horizontal, 20)
        .padding(.top, 8)
        .padding(.bottom, 12)
        .background(UNESColor.surface)
        .fadeUp(delay: 1.34, duration: 0.6)
    }
}

/// Check stroke that draws itself after `delay`, mirroring the CSS keyframes.
private struct DelayedCheck: View {
    let delay: Double
    let duration: Double

    @State private var drawn = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        CheckShape()
            .trim(from: 0, to: drawn ? 1 : 0)
            .stroke(.white, style: StrokeStyle(lineWidth: 3.2, lineCap: .round, lineJoin: .round))
            .frame(width: 34, height: 34)
            .onAppear {
                guard !drawn else { return }
                if reduceMotion {
                    drawn = true
                } else {
                    withAnimation(UNESMotion.draw(duration).delay(delay)) {
                        drawn = true
                    }
                }
            }
    }
}

// MARK: - Live hero (compact Home v2 preview)

private struct NextClassHero: View {
    let nextClass: NextClassInfo

    @State private var startedAt = Date()

    private var totalSeconds: Double {
        Double(max(1, nextClass.startsInMinutes)) * 60
    }

    var body: some View {
        ZStack {
            UNESColor.darkBg
            MeshView(variant: .cool)
            LinearGradient.css(
                stops: [
                    .init(color: UNESColor.scrim.opacity(0.15), location: 0),
                    .init(color: UNESColor.scrim.opacity(0.62), location: 1),
                ],
                angle: 155
            )

            VStack(spacing: 13) {
                HStack {
                    HStack(spacing: 7) {
                        LiveDot(size: 6.5)
                        Text(.onboardingReadyNextClass)
                            .textCase(.uppercase)
                            .font(.system(size: 11.5, weight: .bold))
                            .tracking(0.23)
                            .foregroundStyle(.white.opacity(0.9))
                    }
                    Spacer()
                    Text(timeRange)
                        .font(.system(size: 12.5, weight: .medium))
                        .monospacedDigit()
                        .foregroundStyle(.white.opacity(0.6))
                }

                HStack(alignment: .bottom, spacing: 14) {
                    VStack(alignment: .leading, spacing: 6) {
                        Text(nextClass.disciplineName)
                            .font(.system(size: 26, weight: .heavy))
                            .tracking(-0.78)
                            .foregroundStyle(.white)
                            .lineLimit(2)
                            .minimumScaleFactor(0.85)
                        if !detail.isEmpty {
                            Text(detail)
                                .font(.system(size: 13))
                                .foregroundStyle(.white.opacity(0.8))
                                .lineLimit(1)
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)

                    countdownRing
                }
            }
            .padding(EdgeInsets(top: 16, leading: 18, bottom: 17, trailing: 18))
        }
        .clipShape(RoundedRectangle(cornerRadius: 26, style: .continuous))
        .shadow(color: Color(hex: 0x141020, opacity: 0.28), radius: 18, y: 16)
    }

    private var timeRange: String {
        [nextClass.startTime, nextClass.endTime]
            .compactMap { $0?.isEmpty == false ? $0 : nil }
            .joined(separator: " – ")
    }

    private var detail: String {
        var parts: [String] = []
        if let location = nextClass.location, !location.isEmpty {
            parts.append(String.localized(.commonRoom(location)))
        }
        if let teacher = nextClass.teacherName, !teacher.isEmpty {
            // Full SAGRES names run long — first two names read naturally.
            parts.append(teacher.split(separator: " ").prefix(2).joined(separator: " "))
        }
        return parts.joined(separator: " · ")
    }

    private var countdownRing: some View {
        TimelineView(.periodic(from: .now, by: 1)) { context in
            let left = max(0, totalSeconds - context.date.timeIntervalSince(startedAt))
            let minutesLeft = Int(left) / 60
            ZStack {
                Circle()
                    .stroke(.white.opacity(0.16), lineWidth: 5)
                Circle()
                    .trim(from: 0, to: left / totalSeconds)
                    .stroke(.white, style: StrokeStyle(lineWidth: 5, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                    .animation(.linear(duration: 0.9), value: Int(left))

                VStack(spacing: 0) {
                    Text(minutesLeft < 100 ? "\(minutesLeft)" : "\(minutesLeft / 60)")
                        .font(.system(size: 21, weight: .heavy))
                        .tracking(-0.63)
                        .monospacedDigit()
                        .foregroundStyle(.white)
                    Text(minutesLeft < 100 ? .commonUnitMinuteShort : .commonUnitHourShort)
                        .font(.system(size: 8.5, weight: .bold))
                        .tracking(0.34)
                        .foregroundStyle(.white.opacity(0.65))
                }
            }
            .frame(width: 66, height: 66)
        }
    }
}

// MARK: - Mini widgets

private struct MiniWidget<Content: View>: View {
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            content
        }
        .padding(14)
        .frame(maxWidth: .infinity, minHeight: 118, alignment: .topLeading)
        .background(UNESColor.card)
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .strokeBorder(UNESColor.cardLine)
        }
        .shadow(color: Color(hex: 0x141020, opacity: 0.05), radius: 9, y: 6)
    }
}

private struct CoefficientWidget: View {
    let coefficient: Double?
    let spark: [Double]

    var body: some View {
        MiniWidget {
            HStack(spacing: 6) {
                Image(systemName: "chart.line.uptrend.xyaxis")
                    .font(.system(size: 11, weight: .semibold))
                Text(.onboardingReadyCurrentAverage)
                    .font(.system(size: 12, weight: .semibold))
            }
            .foregroundStyle(UNESColor.tangerine)

            Spacer(minLength: 8)

            Text(display)
                .font(.system(size: 30, weight: .heavy))
                .tracking(-1.2)
                .monospacedDigit()
                .foregroundStyle(UNESColor.ink)

            if spark.count > 1 {
                Sparkline(values: spark)
                    .padding(.top, 4)
            }
        }
    }

    private var display: String {
        formatGrade(coefficient)
    }
}

private struct AttendanceWidget: View {
    let percent: Int?

    var body: some View {
        MiniWidget {
            HStack(spacing: 6) {
                Image(systemName: "flame")
                    .font(.system(size: 11, weight: .semibold))
                Text(.onboardingReadyAttendance)
                    .font(.system(size: 12, weight: .semibold))
            }
            .foregroundStyle(UNESColor.teal)

            Spacer(minLength: 8)

            HStack(alignment: .bottom) {
                percentLabel
                    .font(.system(size: 30, weight: .heavy))
                    .tracking(-1.2)
                    .monospacedDigit()
                    .foregroundStyle(UNESColor.ink)

                Spacer(minLength: 8)

                ring
            }
        }
    }

    private var percentLabel: Text {
        guard let percent else { return Text("—") }
        return Text("\(percent)")
            + Text("%").font(.system(size: 15, weight: .heavy)).foregroundStyle(UNESColor.ink3)
    }

    private var ring: some View {
        ZStack {
            Circle()
                .stroke(UNESColor.surface3, lineWidth: 6)
            Circle()
                .trim(from: 0, to: Double(percent ?? 0) / 100)
                .stroke(UNESColor.teal, style: StrokeStyle(lineWidth: 6, lineCap: .round))
                .rotationEffect(.degrees(-90))
        }
        .frame(width: 46, height: 46)
    }
}

#Preview {
    NavigationStack {
        ReadyView(
            store: Store(
                initialState: ReadyFeature.State(userName: "Mariana", overview: .preview)
            ) {
                ReadyFeature()
            }
        )
    }
}
