import SwiftUI

/// The Horário picker card: miniature previews of the two layouts over a
/// segmented control, with the subtitle narrating the current choice.
struct SettingsScheduleViewCard: View {
    var mode: ScheduleViewMode
    var onSelect: (ScheduleViewMode) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            VStack(alignment: .leading, spacing: 1) {
                Text(.navSchedule)
                    .font(.system(size: 15, weight: .bold))
                    .tracking(-0.3)
                    .foregroundStyle(UNESColor.ink)
                Text(mode == .grid ? .settingsScheduleViewSubtitleGrid : .settingsScheduleViewSubtitleList)
                    .font(.system(size: 12.5, weight: .medium))
                    .foregroundStyle(UNESColor.ink4)
            }

            HStack(spacing: 10) {
                thumb(for: .grid) { gridThumb }
                thumb(for: .list) { listThumb }
            }

            SettingsSegmented(
                options: [
                    (ScheduleViewMode.grid, String.localized(.settingsScheduleViewGrid)),
                    (ScheduleViewMode.list, String.localized(.settingsScheduleViewList)),
                ],
                selected: mode,
                onSelect: onSelect
            )
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(UNESColor.card)
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .strokeBorder(UNESColor.cardLine)
        }
        .shadow(color: Color(hex: 0x141020, opacity: 0.05), radius: 9, y: 6)
    }

    // MARK: Thumbnails

    private func thumb(for value: ScheduleViewMode, @ViewBuilder content: () -> some View) -> some View {
        Button {
            withAnimation(.easeOut(duration: 0.15)) {
                onSelect(value)
            }
        } label: {
            content()
                .padding(9)
                .frame(maxWidth: .infinity)
                .frame(height: 74)
                .background(UNESColor.surface2)
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                .overlay {
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .strokeBorder(mode == value ? UNESColor.accent : .clear, lineWidth: 1.5)
                }
                .contentShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        }
        .buttonStyle(.pressableCard)
    }

    /// Five columns of abstract class blocks — the week grid at a squint.
    private var gridThumb: some View {
        let opacities: [Double] = [
            0.9, 0, 0.5, 0, 0.7,
            0, 0.8, 0.45, 0, 0.6,
            0.55, 0, 0.9, 0.5, 0,
        ]
        return Grid(horizontalSpacing: 3, verticalSpacing: 3) {
            ForEach(0..<3, id: \.self) { row in
                GridRow {
                    ForEach(0..<5, id: \.self) { columnIndex in
                        let opacity = opacities[row * 5 + columnIndex]
                        RoundedRectangle(cornerRadius: 2)
                            .fill(opacity > 0 ? UNESColor.accent.opacity(opacity) : UNESColor.line)
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                    }
                }
            }
        }
    }

    /// Time-rail-and-row bars — the day list at a squint.
    private var listThumb: some View {
        VStack(spacing: 5) {
            ForEach([0.9, 0.65, 0.45], id: \.self) { opacity in
                HStack(spacing: 5) {
                    RoundedRectangle(cornerRadius: 2)
                        .fill(UNESColor.line)
                        .frame(width: 12, height: 6)
                    RoundedRectangle(cornerRadius: 2)
                        .fill(UNESColor.accent.opacity(opacity))
                        .frame(maxWidth: .infinity)
                        .frame(height: 6)
                }
            }
        }
        .frame(maxHeight: .infinity)
    }
}

#Preview {
    VStack(spacing: 12) {
        SettingsScheduleViewCard(mode: .grid, onSelect: { _ in })
        SettingsScheduleViewCard(mode: .list, onSelect: { _ in })
    }
    .padding(16)
    .frame(maxHeight: .infinity)
    .background(UNESColor.surface)
}
