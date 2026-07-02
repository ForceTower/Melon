import SwiftUI

/// Live status card for today: the class in progress (with a progress bar
/// and time remaining) or the next one coming up, over a drifting mesh; once
/// the day is over, a quiet "aulas encerradas" card.
struct ScheduleStatusHero: View {
    let classes: [ScheduleClass]
    let now: Date

    var body: some View {
        let nowMinutes = minutes(of: now)
        let current = classes.first { $0.state(isToday: true, nowMinutes: nowMinutes) == .now }
        let upcoming = classes.first { [.next, .later].contains($0.state(isToday: true, nowMinutes: nowMinutes)) }

        if let target = current ?? upcoming {
            liveCard(target, isNow: current != nil, nowMinutes: nowMinutes)
        } else {
            dayOverCard
        }
    }

    private func minutes(of date: Date) -> Int {
        let calendar = Calendar.current
        return calendar.component(.hour, from: date) * 60 + calendar.component(.minute, from: date)
    }

    // MARK: Live card

    private func liveCard(_ cls: ScheduleClass, isNow: Bool, nowMinutes: Int) -> some View {
        ZStack {
            Color(hex: 0x141019)
            MeshView(variant: isNow ? .cool : .warm)
            LinearGradient.css(
                stops: [
                    .init(color: UNESColor.scrim.opacity(0.18), location: 0),
                    .init(color: UNESColor.scrim.opacity(0.66), location: 1),
                ],
                angle: 155
            )

            VStack(alignment: .leading, spacing: 0) {
                statusRow(cls, isNow: isNow)

                HStack(alignment: .bottom, spacing: 16) {
                    VStack(alignment: .leading, spacing: 8) {
                        Text(cls.title)
                            .font(.system(size: 26, weight: .bold))
                            .tracking(-0.78)
                            .lineSpacing(1)
                            .minimumScaleFactor(0.8)
                        ScheduleLocationRow(cls: cls, inverted: true)
                            .opacity(0.9)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)

                    countdown(cls, isNow: isNow, nowMinutes: nowMinutes)
                }
                .padding(.top, 14)

                if isNow, let progress = progress(cls, nowMinutes: nowMinutes) {
                    progressBar(progress)
                        .padding(.top, 14)
                }
            }
            .foregroundStyle(.white)
            .padding(EdgeInsets(top: 16, leading: 18, bottom: 18, trailing: 18))
        }
        .environment(\.colorScheme, .dark)
        .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
        .shadow(color: UNESColor.disciplineColor(cls.colorIndex).opacity(0.12), radius: 11, y: 8)
    }

    private func statusRow(_ cls: ScheduleClass, isNow: Bool) -> some View {
        HStack {
            HStack(spacing: 7) {
                if isNow {
                    LiveDot()
                } else {
                    Circle().fill(.white).frame(width: 7, height: 7)
                }
                Text(isNow ? "Em aula agora" : "Próxima aula")
                    .textCase(.uppercase)
                    .font(.system(size: 12, weight: .bold))
                    .tracking(0.3)
            }

            Spacer()

            Text(timeRange(cls))
                .font(.system(size: 13, weight: .semibold))
                .monospacedDigit()
                .opacity(0.7)
        }
    }

    private func countdown(_ cls: ScheduleClass, isNow: Bool, nowMinutes: Int) -> some View {
        let remaining = isNow
            ? max(0, (cls.endMinute ?? cls.startMinute) - nowMinutes)
            : max(0, cls.startMinute - nowMinutes)
        return VStack(alignment: .trailing, spacing: 3) {
            Text(ScheduleFormat.durationLabel(remaining))
                .font(.system(size: 30, weight: .bold))
                .tracking(-1.2)
                .monospacedDigit()
            Text(isNow ? "restam" : "até começar")
                .font(.system(size: 11, weight: .semibold))
                .tracking(0.3)
                .opacity(0.65)
        }
    }

    private func progress(_ cls: ScheduleClass, nowMinutes: Int) -> Double? {
        guard let end = cls.endMinute, end > cls.startMinute else { return nil }
        return min(1, max(0, Double(nowMinutes - cls.startMinute) / Double(end - cls.startMinute)))
    }

    private func progressBar(_ progress: Double) -> some View {
        GeometryReader { proxy in
            ZStack(alignment: .leading) {
                RoundedRectangle(cornerRadius: 3)
                    .fill(.white.opacity(0.2))
                RoundedRectangle(cornerRadius: 3)
                    .fill(.white)
                    .frame(width: proxy.size.width * progress)
                    .animation(.linear(duration: 0.9), value: progress)
            }
        }
        .frame(height: 5)
    }

    private func timeRange(_ cls: ScheduleClass) -> String {
        var range = ScheduleFormat.timeLabel(cls.startMinute)
        if let end = cls.endMinute {
            range += " – \(ScheduleFormat.timeLabel(end))"
        }
        return range
    }

    // MARK: Day over

    private var dayOverCard: some View {
        HStack(spacing: 14) {
            ZStack {
                Circle().fill(UNESColor.surface2)
                Image(systemName: "checkmark")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(UNESColor.successGreen)
            }
            .frame(width: 40, height: 40)

            VStack(alignment: .leading, spacing: 1) {
                Text("Aulas encerradas")
                    .font(.system(size: 16, weight: .bold))
                    .tracking(-0.32)
                    .foregroundStyle(UNESColor.ink)
                Text("Você concluiu o dia. Bom descanso.")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(UNESColor.ink3)
            }

            Spacer(minLength: 0)
        }
        .padding(EdgeInsets(top: 18, leading: 20, bottom: 18, trailing: 20))
        .background(UNESColor.card)
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .strokeBorder(UNESColor.cardLine)
        }
        .shadow(color: Color(hex: 0x141020, opacity: 0.05), radius: 9, y: 6)
    }
}

#Preview {
    let classes = ScheduleOverview.preview().days[3].classes
    VStack(spacing: 20) {
        ScheduleStatusHero(
            classes: classes,
            now: Calendar.current.date(bySettingHour: 10, minute: 52, second: 0, of: .now)!
        )
        ScheduleStatusHero(
            classes: classes,
            now: Calendar.current.date(bySettingHour: 13, minute: 10, second: 0, of: .now)!
        )
        ScheduleStatusHero(
            classes: classes,
            now: Calendar.current.date(bySettingHour: 19, minute: 0, second: 0, of: .now)!
        )
    }
    .padding(16)
    .background(UNESColor.surface)
}
