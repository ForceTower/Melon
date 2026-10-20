import Charts
import SwiftUI

/// "Como calculamos": plain language, the example's own numbers, and a tiny
/// distribution of past trips with the shown band highlighted.
struct CommuteHowSheet: View {
    var arrival: TransitArrival?
    var direction: TransitDirection

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            TimelineView(.periodic(from: .now, by: 1)) { context in
                ScrollView {
                    VStack(alignment: .leading, spacing: 0) {
                        if let arrival {
                            summary(arrival, now: context.date)
                            if let basis = arrival.basis, basis.sampleCount >= 2, basis.dayCount >= 2 {
                                explanation(basis)
                                distribution(basis, arrival: arrival)
                                    .padding(.top, 18)
                            }
                        }
                        reasons
                            .padding(.top, 16)
                        Text(.commuteHowFootnote)
                            .font(.system(size: 12))
                            .foregroundStyle(UNESColor.ink4)
                            .padding(.top, 16)
                    }
                    .padding(EdgeInsets(top: 4, leading: 20, bottom: 32, trailing: 20))
                }
            }
            .background(UNESColor.surface.ignoresSafeArea())
            .navigationTitle(Text(.commuteHowTitle))
            .inlineNavigationBar()
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button {
                        dismiss()
                    } label: {
                        Image(systemName: "xmark")
                    }
                    .accessibilityLabel(Text(.commonClose))
                }
            }
        }
        .presentationDetents([.medium, .large])
        .presentationDragIndicator(.visible)
    }

    private func summary(_ arrival: TransitArrival, now: Date) -> some View {
        HStack(alignment: .center, spacing: 10) {
            TransitLineBadge(number: arrival.lineNumber)
            TransitEstimateText(estimate: CommuteFormat.estimate(for: arrival, now: now), size: 26)
            TransitChip(kind: TransitChip.Kind(arrival), small: true)
        }
        .padding(.bottom, 14)
    }

    private func explanation(_ basis: TransitEstimateBasis) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(.commuteHowBasis(basis.sampleCount, basis.dayCount))
            Text(.commuteHowHitRate(Int((basis.hitRate * 10).rounded())))
        }
        .font(.system(size: 15))
        .lineSpacing(3)
        .foregroundStyle(UNESColor.ink)
        .fixedSize(horizontal: false, vertical: true)
    }

    private func distribution(_ basis: TransitEstimateBasis, arrival: TransitArrival) -> some View {
        let color = UNESColor.transitLine(arrival.lineNumber)
        let bins = basis.counts.enumerated().map { index, count in
            (start: basis.histogramStart.addingTimeInterval(Double(index * basis.binMinutes) * 60), count: count)
        }
        let binLength = Double(basis.binMinutes) * 60
        let end = basis.histogramStart.addingTimeInterval(Double(basis.counts.count) * binLength)
        let inBand = { (start: Date) in
            start.addingTimeInterval(binLength) > arrival.earliest && start < arrival.latest
        }
        return VStack(spacing: 8) {
            Chart {
                RectangleMark(
                    xStart: .value("start", arrival.earliest),
                    xEnd: .value("end", arrival.latest)
                )
                .foregroundStyle(color.opacity(0.12))
                .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                ForEach(bins, id: \.start) { bin in
                    BarMark(
                        x: .value("minute", bin.start, unit: .minute),
                        y: .value("trips", bin.count)
                    )
                    .foregroundStyle(inBand(bin.start) ? color : UNESColor.surface3)
                    .clipShape(RoundedRectangle(cornerRadius: 3, style: .continuous))
                }
            }
            .chartXScale(domain: basis.histogramStart...end)
            .chartXAxis(.hidden)
            .chartYAxis(.hidden)
            .frame(height: 64)
            HStack {
                Text(verbatim: CommuteFormat.clock(basis.histogramStart))
                Spacer()
                Text(verbatim: CommuteFormat.clockRange(arrival.earliest, arrival.latest))
                    .fontWeight(.bold)
                    .foregroundStyle(UNESColor.ink)
                Spacer()
                Text(verbatim: CommuteFormat.clock(end))
            }
            .font(.system(size: 10.5, weight: .medium))
            .monospacedDigit()
            .foregroundStyle(UNESColor.ink4)
        }
        .transitCard(padding: EdgeInsets(top: 14, leading: 14, bottom: 10, trailing: 14))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text(.commuteHowChartAccessibility(CommuteFormat.clockRange(arrival.earliest, arrival.latest))))
    }

    private var reasons: some View {
        VStack(alignment: .leading, spacing: 10) {
            reason("dot.radiowaves.left.and.right", .commuteHowLiveTitle, .commuteHowLiveBody)
            reason("clock", .commuteHowHistoryTitle, .commuteHowHistoryBody)
            reason("xmark", .commuteHowIgnoredTitle, .commuteHowIgnoredBody)
        }
    }

    private func reason(_ symbol: String, _ title: LocalizedStringResource, _ body: LocalizedStringResource) -> some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: symbol)
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(UNESColor.ink2)
                .frame(width: 28, height: 28)
                .background(UNESColor.surface2, in: RoundedRectangle(cornerRadius: 9, style: .continuous))
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(UNESColor.ink)
                Text(body)
                    .font(.system(size: 12.5))
                    .foregroundStyle(UNESColor.ink3)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }
}

#Preview {
    Color.clear.sheet(isPresented: .constant(true)) {
        CommuteHowSheet(arrival: TransitFixtures.board(now: .now).arrivals[0], direction: .toCampus)
    }
}
