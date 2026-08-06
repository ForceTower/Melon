import SwiftUI

/// The class detail sheet: code chip and weekday over the title, an inset
/// table of facts, and the discipline shortcut.
struct ScheduleGridClassSheet: View {
    let item: ScheduleGridFeature.SheetItem
    var onViewDiscipline: () -> Void
    var onClose: () -> Void

    @State private var height: CGFloat = 420

    private var scheduleClass: ScheduleClass { item.scheduleClass }
    private var readable: Color { UNESColor.disciplineReadableColor(scheduleClass.colorIndex) }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            header
            factsCard
                .padding(.top, 16)
            actions
                .padding(.top, 14)
        }
        .padding(EdgeInsets(top: 20, leading: 16, bottom: 16, trailing: 16))
        .frame(maxWidth: .infinity, alignment: .leading)
        .onGeometryChange(for: CGFloat.self) { proxy in
            proxy.size.height
        } action: { measured in
            height = measured
            // Detent updates issued mid-presentation get dropped, and a
            // same-value write is a no-op, so re-send with a hidden nudge.
            Task { @MainActor in
                try? await Task.sleep(for: .milliseconds(700))
                if height == measured {
                    height = measured + 0.001
                }
            }
        }
        .presentationBackground(UNESColor.surface)
        .presentationDetents([.height(height)])
        .presentationDragIndicator(.visible)
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 8) {
                Text(scheduleClass.code)
                    .font(.system(size: 11, weight: .bold))
                    .tracking(0.3)
                    .foregroundStyle(readable)
                    .padding(EdgeInsets(top: 3, leading: 7, bottom: 3, trailing: 7))
                    .background(readable.opacity(0.14), in: RoundedRectangle(cornerRadius: 6, style: .continuous))

                Text(verbatim: "\(ScheduleFormat.dayNames[item.dayIndex]) · \(ScheduleGridFormat.timeRange(scheduleClass))")
                    .font(.system(size: 13, weight: .medium))
                    .monospacedDigit()
                    .foregroundStyle(UNESColor.ink3)
            }

            Text(scheduleClass.title)
                .font(.system(size: 24, weight: .bold))
                .tracking(-0.72)
                .lineSpacing(1)
                .foregroundStyle(UNESColor.ink)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(.horizontal, 4)
    }

    private var factsCard: some View {
        VStack(spacing: 0) {
            if let duration = scheduleClass.durationMinutes {
                fact(.scheduleGridSheetDuration, value: ScheduleFormat.durationLabel(duration))
            }
            fact(.scheduleGridSheetLocation, value: ScheduleGridFormat.sheetLocation(scheduleClass))
            if let campus = scheduleClass.campus, !scheduleClass.isOnline {
                fact(.scheduleGridSheetCampus, value: campus)
            }
            if let topic = scheduleClass.topic {
                fact(.scheduleGridSheetTopic, value: topic)
            }
            if let teacher = scheduleClass.teacherName {
                fact(.scheduleGridSheetTeacher, value: teacher, isLast: true)
            }
        }
        .background(UNESColor.card)
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .strokeBorder(UNESColor.cardLine, lineWidth: 0.5)
        }
    }

    private func fact(_ label: LocalizedStringResource, value: String, isLast: Bool = false) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: 16) {
            Text(label)
                .font(.system(size: 15, weight: .medium))
                .foregroundStyle(UNESColor.ink3)
                .fixedSize()

            Spacer(minLength: 0)

            Text(value)
                .font(.system(size: 15, weight: .medium))
                .foregroundStyle(UNESColor.ink)
                .multilineTextAlignment(.trailing)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(EdgeInsets(top: 11, leading: 16, bottom: 11, trailing: 16))
        .overlay(alignment: .bottom) {
            if !isLast {
                Rectangle()
                    .fill(UNESColor.line)
                    .frame(height: 0.5)
                    .padding(.leading, 16)
            }
        }
    }

    private var actions: some View {
        HStack(spacing: 10) {
            Button(action: onViewDiscipline) {
                Text(.scheduleGridSheetViewDiscipline)
            }
            .buttonStyle(UNESButtonStyle(tone: .accent))

            Button(action: onClose) {
                Text(.scheduleGridSheetClose)
            }
            .buttonStyle(UNESButtonStyle(tone: .neutral))
        }
    }
}

#Preview {
    Color.clear
        .sheet(isPresented: .constant(true)) {
            ScheduleGridClassSheet(
                item: ScheduleGridFeature.SheetItem(
                    scheduleClass: ScheduleOverview.preview().days[3].classes[1],
                    dayIndex: 3
                ),
                onViewDiscipline: {},
                onClose: {}
            )
        }
}
