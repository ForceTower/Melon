import SwiftUI

/// The 2×2 iOS-widget-style grid under the hero.
struct HomeWidgetGrid: View {
    let overview: HomeOverview
    var onMessagesTap: () -> Void

    var body: some View {
        LazyVGrid(
            columns: [GridItem(.flexible(), spacing: 14), GridItem(.flexible(), spacing: 14)],
            spacing: 14
        ) {
            CoefficientHomeWidget(summary: overview.coefficient)
            AttendanceHomeWidget(summary: overview.attendance)
            ExamHomeWidget(summary: overview.nextExam)
            MessagesHomeWidget(summary: overview.messages, onTap: onMessagesTap)
        }
    }
}

// MARK: - Shared chrome

private struct HomeWidget<Content: View>: View {
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            content
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .frame(height: 158)
        .background(UNESColor.card)
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .strokeBorder(UNESColor.cardLine)
        }
        .shadow(color: Color(hex: 0x141020, opacity: 0.06), radius: 9, y: 6)
    }
}

private struct WidgetHead: View {
    var icon: String
    var label: String
    var color: Color

    var body: some View {
        HStack(spacing: 7) {
            Image(systemName: icon)
                .font(.system(size: 13, weight: .semibold))
            Text(label)
                .font(.system(size: 13, weight: .semibold))
                .tracking(-0.13)
        }
        .foregroundStyle(color)
    }
}

// MARK: - Coeficiente

private struct CoefficientHomeWidget: View {
    let summary: CoefficientSummary?

    var body: some View {
        HomeWidget {
            WidgetHead(icon: "chart.line.uptrend.xyaxis", label: "Coeficiente", color: UNESColor.tangerine)

            Spacer(minLength: 8)

            HStack(alignment: .firstTextBaseline, spacing: 8) {
                Text(formatGrade(summary?.value))
                    .font(.system(size: 46, weight: .bold))
                    .tracking(-1.84)
                    .monospacedDigit()
                    .foregroundStyle(summary == nil ? UNESColor.ink4 : UNESColor.ink)

                if let delta = deltaLabel {
                    HStack(spacing: 2) {
                        Image(systemName: delta.rising ? "chevron.up" : "chevron.down")
                            .font(.system(size: 9, weight: .bold))
                        Text(delta.text)
                            .font(.system(size: 12, weight: .semibold))
                            .monospacedDigit()
                    }
                    .foregroundStyle(delta.rising ? UNESColor.successGreen : UNESColor.alertRed)
                }
            }

            if let spark = summary?.spark, spark.count > 1 {
                Sparkline(values: spark, size: CGSize(width: 118, height: 34))
                    .padding(.top, 8)
            }
        }
    }

    private var deltaLabel: (text: String, rising: Bool)? {
        guard let delta = summary?.delta, abs(delta) >= 0.1 else { return nil }
        let rising = delta >= 0
        return ("\(rising ? "+" : "−")\(formatGrade(abs(delta)))", rising)
    }
}

// MARK: - Frequência

private struct AttendanceHomeWidget: View {
    let summary: AttendanceSummary?

    var body: some View {
        HomeWidget {
            WidgetHead(icon: "flame", label: "Frequência", color: UNESColor.teal)

            Spacer(minLength: 8)

            HStack(alignment: .bottom) {
                VStack(alignment: .leading, spacing: 4) {
                    percentLabel
                    Text(remainingLabel)
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(UNESColor.ink3)
                }

                Spacer(minLength: 8)

                ring
            }
        }
    }

    private var percentLabel: some View {
        Group {
            if let percent = summary?.percent {
                Text("\(percent)")
                    .font(.system(size: 40, weight: .bold))
                    .tracking(-1.6)
                    .monospacedDigit()
                    .foregroundStyle(UNESColor.ink)
                    + Text("%")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(UNESColor.ink3)
            } else {
                Text("—")
                    .font(.system(size: 40, weight: .bold))
                    .foregroundStyle(UNESColor.ink4)
            }
        }
    }

    private var remainingLabel: String {
        guard let remaining = summary?.remainingAbsences else { return "Aguardando aulas" }
        return remaining == 1 ? "1 falta restante" : "\(remaining) faltas restantes"
    }

    private var ring: some View {
        ZStack {
            Circle()
                .stroke(Color(hex: 0x787880, opacity: 0.28), lineWidth: 7)
            Circle()
                .trim(from: 0, to: Double(summary?.percent ?? 0) / 100)
                .stroke(UNESColor.teal, style: StrokeStyle(lineWidth: 7, lineCap: .round))
                .rotationEffect(.degrees(-90))
        }
        .frame(width: 58, height: 58)
    }
}

// MARK: - Próxima prova

private struct ExamHomeWidget: View {
    let summary: ExamSummary?

    var body: some View {
        HomeWidget {
            WidgetHead(icon: "calendar", label: "Próxima prova", color: UNESColor.magenta)

            Spacer(minLength: 8)

            if let summary {
                HStack(alignment: .firstTextBaseline, spacing: 5) {
                    Text(summary.daysUntil == 0 ? "Hoje" : "\(summary.daysUntil)")
                        .font(.system(size: summary.daysUntil == 0 ? 34 : 46, weight: .bold))
                        .tracking(-1.84)
                        .monospacedDigit()
                        .foregroundStyle(UNESColor.ink)
                    if summary.daysUntil > 0 {
                        Text(summary.daysUntil == 1 ? "dia" : "dias")
                            .font(.system(size: 18, weight: .semibold))
                            .foregroundStyle(UNESColor.ink3)
                    }
                }

                Text("\(summary.label) · \(summary.disciplineName)")
                    .font(.system(size: 14, weight: .semibold))
                    .tracking(-0.14)
                    .foregroundStyle(UNESColor.ink)
                    .lineLimit(1)
                    .padding(.top, 6)

                Text(dateLine(summary))
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(UNESColor.ink3)
                    .padding(.top, 1)
            } else {
                Text("—")
                    .font(.system(size: 46, weight: .bold))
                    .foregroundStyle(UNESColor.ink4)
                Text("Sem provas marcadas")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(UNESColor.ink3)
                    .padding(.top, 6)
            }
        }
    }

    private func dateLine(_ summary: ExamSummary) -> String {
        let date = HomeFormat.shortDate(fromDayStamp: summary.date) ?? summary.date
        return [date, summary.time].compactMap(\.self).joined(separator: " · ")
    }
}

// MARK: - Mensagens

private struct MessagesHomeWidget: View {
    let summary: MessagesSummary?
    var onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 0) {
                HStack {
                    WidgetHead(icon: "envelope", label: "Mensagens", color: .white.opacity(0.85))
                    Spacer()
                    if let unread = summary?.unreadCount, unread > 0 {
                        Text("\(unread)")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundStyle(.white)
                            .padding(.horizontal, 6)
                            .frame(minWidth: 20)
                            .frame(height: 20)
                            .background(UNESColor.alertRed, in: Capsule())
                    }
                }

                Spacer(minLength: 8)

                if let summary, let sender = summary.latestSenderName {
                    HStack(spacing: 8) {
                        Text(sender.prefix(1).uppercased())
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundStyle(.white)
                            .frame(width: 26, height: 26)
                            .background(
                                LinearGradient.css(
                                    stops: [
                                        .init(color: UNESColor.magenta, location: 0),
                                        .init(color: UNESColor.coral, location: 1),
                                    ],
                                    angle: 135
                                ),
                                in: Circle()
                            )
                        Text(sender)
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(.white)
                            .lineLimit(1)
                    }

                    Text(summary.latestPreview ?? "")
                        .font(.system(size: 12.5))
                        .lineSpacing(2.5)
                        .foregroundStyle(.white.opacity(0.78))
                        .lineLimit(2, reservesSpace: true)
                        .multilineTextAlignment(.leading)
                        .padding(.top, 6)
                } else {
                    Text("Sem mensagens por aqui")
                        .font(.system(size: 13, weight: .medium))
                        .foregroundStyle(.white.opacity(0.78))
                }
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .frame(height: 158)
            .background {
                ZStack {
                    UNESColor.roseBg
                    MeshView(variant: .rose, intensity: 0.8)
                    LinearGradient.css(
                        stops: [
                            .init(color: UNESColor.roseScrim.opacity(0.12), location: 0),
                            .init(color: UNESColor.roseScrim.opacity(0.5), location: 1),
                        ],
                        angle: 160
                    )
                }
            }
            .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
            .shadow(color: Color(hex: 0x141020, opacity: 0.06), radius: 9, y: 6)
            .environment(\.colorScheme, .dark)
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    ScrollView {
        HomeWidgetGrid(overview: .preview(), onMessagesTap: {})
            .padding(16)
    }
    .background(UNESColor.surface)
}

#Preview("Empty") {
    ScrollView {
        HomeWidgetGrid(overview: .empty, onMessagesTap: {})
            .padding(16)
    }
    .background(UNESColor.surface)
}
