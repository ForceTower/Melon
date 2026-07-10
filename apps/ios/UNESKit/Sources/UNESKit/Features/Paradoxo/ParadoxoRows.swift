import SwiftUI

/// A grouped card of rows separated by inset hairlines — the Paradoxo list
/// container.
struct ParadoxoRowGroup<Row: Identifiable, Content: View>: View {
    var rows: [Row]
    @ViewBuilder var content: (Row) -> Content

    var body: some View {
        VStack(spacing: 0) {
            ForEach(Array(rows.enumerated()), id: \.element.id) { index, row in
                content(row)
                if index < rows.count - 1 {
                    Divider()
                        .overlay(UNESColor.line)
                        .padding(.leading, 73)
                }
            }
        }
        .paradoxoCard()
    }
}

/// Score tile + title + metadata + trailing accessory, shared by every
/// Paradoxo list row.
struct ParadoxoRow<Subtitle: View, Accessory: View>: View {
    var score: Double
    var title: String
    var onTap: () -> Void
    @ViewBuilder var subtitle: Subtitle
    @ViewBuilder var accessory: Accessory

    var body: some View {
        Button {
            onTap()
        } label: {
            HStack(spacing: 13) {
                ParadoxoScoreTile(score: score)
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.system(size: 15.5, weight: .semibold))
                        .tracking(-0.31)
                        .foregroundStyle(UNESColor.ink)
                        .lineLimit(1)
                    HStack(spacing: 6) {
                        subtitle
                    }
                    .font(.system(size: 12.5, weight: .medium))
                    .foregroundStyle(UNESColor.ink3)
                    .lineLimit(1)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                accessory
            }
            .padding(EdgeInsets(top: 10, leading: 14, bottom: 10, trailing: 14))
            .frame(minHeight: 64)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

/// Trailing sparkline + chevron.
struct ParadoxoRowAccessory: View {
    var spark: [Double]
    var sparkColor: Color = UNESColor.ink4

    var body: some View {
        HStack(spacing: 10) {
            if spark.count > 1 {
                Sparkline(values: spark, color: sparkColor, size: CGSize(width: 40, height: 16), lineWidth: 1.75)
            }
            Image(systemName: "chevron.right")
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(UNESColor.ink4)
        }
    }
}

/// Interpunct separator for row metadata.
struct ParadoxoDot: View {
    var body: some View {
        Text(verbatim: "·")
            .foregroundStyle(UNESColor.ink4.opacity(0.6))
    }
}

// MARK: - Concrete rows

struct ParadoxoDisciplineSummaryRow: View {
    var summary: ParadoxoDisciplineSummary
    var onTap: () -> Void

    var body: some View {
        ParadoxoRow(score: summary.mean, title: summary.name, onTap: onTap) {
            Text(.paradoxoSamples(ParadoxoFormat.count(summary.sampleCount)))
            if let percentile = summary.myPercentile {
                ParadoxoDot()
                Text(.paradoxoTopPercent(ParadoxoFormat.percent(100 - percentile)))
                    .fontWeight(.bold)
                    .foregroundStyle(UNESColor.successGreen)
            }
        } accessory: {
            ParadoxoRowAccessory(spark: summary.spark)
        }
    }
}

struct ParadoxoIndexEntryRow: View {
    var entry: ParadoxoIndexEntry
    var onTap: () -> Void

    var body: some View {
        ParadoxoRow(score: entry.mean, title: entry.name, onTap: onTap) {
            if let code = entry.code {
                Text(code)
                    .foregroundStyle(UNESColor.ink4)
                ParadoxoDot()
            }
            switch entry.ref.kind {
            case .discipline:
                Text(.paradoxoSamples(ParadoxoFormat.count(entry.studentCount)))
            case .teacher:
                Text(.paradoxoStudents(ParadoxoFormat.count(entry.studentCount)))
                ParadoxoDot()
                Text(ParadoxoTier(mean: entry.mean).label)
                    .fontWeight(.bold)
                    .foregroundStyle(ParadoxoTier(mean: entry.mean).tone)
            }
        } accessory: {
            Image(systemName: "chevron.right")
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(UNESColor.ink4)
        }
    }
}

struct ParadoxoRankedEntryRow: View {
    var entry: ParadoxoRankedEntry
    var onTap: () -> Void

    var body: some View {
        ParadoxoRow(score: entry.mean, title: entry.name, onTap: onTap) {
            if let code = entry.code {
                Text(code)
                    .foregroundStyle(UNESColor.ink4)
                ParadoxoDot()
            }
            Text(.paradoxoStudents(ParadoxoFormat.count(entry.studentCount)))
            if let delta = entry.delta {
                ParadoxoDot()
                Text(ParadoxoFormat.signedGrade(delta))
                    .fontWeight(.bold)
                    .foregroundStyle(delta >= 0 ? UNESColor.successGreen : UNESColor.coral)
            }
        } accessory: {
            Image(systemName: "chevron.right")
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(UNESColor.ink4)
        }
    }
}

#Preview("Linhas") {
    ScrollView {
        VStack(spacing: 16) {
            ParadoxoRowGroup(rows: ParadoxoOverview.preview().myDisciplines) { summary in
                ParadoxoDisciplineSummaryRow(summary: summary, onTap: {})
            }
            ParadoxoRowGroup(rows: ParadoxoIndexEntry.preview()) { entry in
                ParadoxoIndexEntryRow(entry: entry, onTap: {})
            }
        }
        .padding(16)
    }
    .background(UNESColor.surface)
}
