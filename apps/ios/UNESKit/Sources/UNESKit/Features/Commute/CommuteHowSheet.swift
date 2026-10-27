import Charts
import SwiftUI

/// "Como calculamos": plain language, the example's own numbers, and a tiny
/// distribution of past trips with the shown band highlighted.
struct CommuteHowSheet: View {
    var arrival: TransitArrival?
    var direction: TransitDirection

    @Environment(\.dismiss) private var dismiss
    /// Measured content height so the sheet hugs it — same pattern as
    /// `MeDocumentSheet`, including the post-presentation re-sync (a detent
    /// write issued mid-animation is dropped).
    @State private var height: CGFloat = 560

    var body: some View {
        ScrollView {
            TimelineView(.periodic(from: .now, by: 1)) { context in
                VStack(alignment: .leading, spacing: 0) {
                    header
                        .padding(.bottom, 18)
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
                .padding(EdgeInsets(top: 24, leading: 20, bottom: 26, trailing: 20))
            }
            .onGeometryChange(for: CGFloat.self) { proxy in
                proxy.size.height
            } action: { measured in
                fit(measured)
            }
        }
        .scrollIndicators(.hidden)
        .presentationBackground(UNESColor.surface)
        .presentationDetents([.height(height)])
        .presentationDragIndicator(.visible)
    }

    private func fit(_ measured: CGFloat) {
        height = measured
        Task { @MainActor in
            try? await Task.sleep(for: .milliseconds(700))
            if height == measured {
                height = measured + 0.001
            }
        }
    }

    private var header: some View {
        HStack(spacing: 12) {
            Text(.commuteHowTitle)
                .font(.system(size: 22, weight: .bold))
                .tracking(-0.66)
                .foregroundStyle(UNESColor.ink)
                .frame(maxWidth: .infinity, alignment: .leading)
            Button {
                dismiss()
            } label: {
                Image(systemName: "xmark")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(UNESColor.ink3)
                    .frame(width: 30, height: 30)
                    .background(UNESColor.surface2, in: Circle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(Text(.commonClose))
        }
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
            // Verbatim labels: the axes are hidden and the chart reads as one
            // accessibility label, so these never reach the String Catalog.
            Chart {
                RectangleMark(
                    xStart: .value(Text(verbatim: "start"), arrival.earliest),
                    xEnd: .value(Text(verbatim: "end"), arrival.latest)
                )
                .foregroundStyle(color.opacity(0.12))
                .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                ForEach(bins, id: \.start) { bin in
                    BarMark(
                        x: .value(Text(verbatim: "minute"), bin.start, unit: .minute),
                        y: .value(Text(verbatim: "trips"), bin.count)
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
            reason("calendar", .commuteHowIgnoredTitle, .commuteHowIgnoredBody)
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
