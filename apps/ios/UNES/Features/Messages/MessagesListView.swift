import SwiftUI

/// Messages ("Recados") inbox — grouped by date bucket, with filter chips at
/// the top. Tapping a row navigates to the full message detail.
///
/// Mirrors `MessagesScreen` in `screens-messages.jsx`.
struct MessagesListView: View {
    @State private var viewModel: MessagesListViewModel
    // `MessagesFactory` is surfaced in `.task` closures — hold it on the view
    // so detail screens can grab their own VM off the same use cases.
    private let factory: MessagesFactory?

    init(factory: MessagesFactory) {
        self._viewModel = State(initialValue: factory.makeListViewModel())
        self.factory = factory
    }

    // Factory-less init — retained for `#Preview` so the canvas keeps
    // rendering against `MessageFixtures` without wiring a full graph.
    init() {
        self._viewModel = State(initialValue: MessagesListViewModel())
        self.factory = nil
    }

    private var counts: [MessageFilter: Int] {
        var out: [MessageFilter: Int] = [:]
        for f in MessageFilter.allCases {
            out[f] = viewModel.messages.filter { f.matches($0) }.count
        }
        return out
    }

    private var filtered: [Message] {
        viewModel.messages.filter { viewModel.filter.matches($0) }
    }

    private struct Bucket: Identifiable {
        let id: MessageDate.Bucket
        var label: String { id.rawValue }
        let items: [Message]
    }

    private var buckets: [Bucket] {
        var map: [MessageDate.Bucket: [Message]] = [:]
        for m in filtered {
            map[MessageDate.bucket(for: m.receivedAt), default: []].append(m)
        }
        return MessageDate.Bucket.allCases.compactMap { key in
            guard let items = map[key], !items.isEmpty else { return nil }
            return Bucket(id: key, items: items)
        }
    }

    private var unreadCount: Int {
        viewModel.messages.filter(\.unread).count
    }

    var body: some View {
        NavigationStack {
            screenBody
                .navigationTitle("Mensagens")
                .toolbar(.hidden, for: .navigationBar)
                .task { await viewModel.observe() }
        }
    }

    private var screenBody: some View {
        ZStack(alignment: .top) {
            UNESColor.surface.ignoresSafeArea()

            // Ambient mesh pinned to the top, fading into the surface.
            VStack(spacing: 0) {
                ZStack {
                    MeshGradientView(variant: .warm, intensity: 0.2)
                    LinearGradient(
                        stops: [
                            .init(color: .clear, location: 0),
                            .init(color: UNESColor.surface, location: 0.95),
                        ],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                }
                .frame(height: 280)
                Spacer(minLength: 0)
            }
            .ignoresSafeArea(edges: .top)
            .allowsHitTesting(false)

            ScrollView(.vertical, showsIndicators: false) {
                VStack(alignment: .leading, spacing: 0) {
                    header
                        .fadeUpOnAppear(delay: 0.02, distance: 10, duration: 0.55)

                    FilterChipRow(active: $viewModel.filter, counts: counts)
                        .padding(.top, 10)
                        .padding(.bottom, 4)
                        .fadeUpOnAppear(delay: 0.1, distance: 10, duration: 0.55)

                    if buckets.isEmpty {
                        emptyState
                    } else {
                        ForEach(Array(buckets.enumerated()), id: \.element.id) { idx, b in
                            bucketCard(b, index: idx)
                                .fadeUpOnAppear(delay: 0.18 + Double(idx) * 0.06,
                                                distance: 10, duration: 0.55)
                        }
                    }
                }
                .containerRelativeFrame(.horizontal, alignment: .leading)
                .padding(.bottom, 32)
            }
        }
    }

    // MARK: - Header

    private var header: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 10) {
                Text("◦ CAIXA DE ENTRADA")
                    .font(UNESFont.mono(10, weight: .medium))
                    .tracking(1.2)
                    .foregroundStyle(UNESColor.ink3)
                    .fixedSize()

                if unreadCount > 0 {
                    unreadBadge
                }
            }

            Text("Mensagens")
                .font(UNESFont.serif(32))
                .tracking(-0.64)
                .foregroundStyle(UNESColor.ink)
        }
        .padding(.horizontal, 20)
        .padding(.top, 60)
        .padding(.bottom, 14)
    }

    private var unreadBadge: some View {
        HStack(spacing: 6) {
            Circle()
                .fill(UNESColor.coral)
                .frame(width: 4, height: 4)
            Text("\(unreadCount) NOVAS")
                .font(UNESFont.mono(10, weight: .semibold))
                .tracking(0.8)
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 2)
        .background(
            RoundedRectangle(cornerRadius: 9, style: .continuous)
                .fill(UNESColor.coral.opacity(0.095))
        )
        .foregroundStyle(UNESColor.coral)
        .fixedSize()
    }

    // MARK: - Bucket card

    private func bucketCard(_ b: Bucket, index: Int) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            bucketHeader(label: b.label, count: b.items.count)

            VStack(spacing: 0) {
                ForEach(Array(b.items.enumerated()), id: \.element.id) { i, m in
                    NavigationLink {
                        detailView(for: m)
                    } label: {
                        MessageRow(message: m)
                    }
                    .buttonStyle(.plain)

                    if i < b.items.count - 1 {
                        Rectangle()
                            .fill(UNESColor.line)
                            .frame(height: 1)
                    }
                }
            }
            .background(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .fill(UNESColor.card)
                    .overlay(
                        RoundedRectangle(cornerRadius: 18, style: .continuous)
                            .strokeBorder(UNESColor.cardLine, lineWidth: 1)
                    )
            )
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            .padding(.horizontal, 12)
        }
    }

    private func bucketHeader(label: String, count: Int) -> some View {
        HStack(spacing: 8) {
            Text(label.uppercased())
                .font(UNESFont.mono(10, weight: .semibold))
                .tracking(1.4)
                .foregroundStyle(UNESColor.ink4)
            Text("\(count)")
                .font(UNESFont.mono(10))
                .foregroundStyle(UNESColor.ink4)
                .opacity(0.55)
            Rectangle()
                .fill(UNESColor.line)
                .frame(height: 1)
                .opacity(0.6)
                .padding(.leading, 4)
        }
        .padding(.horizontal, 20)
        .padding(.top, 18)
        .padding(.bottom, 6)
    }

    private var emptyState: some View {
        Text("Nenhuma mensagem neste filtro.")
            .font(UNESFont.sans(14))
            .foregroundStyle(UNESColor.ink3)
            .frame(maxWidth: .infinity)
            .padding(.horizontal, 40)
            .padding(.vertical, 80)
    }

    // MARK: - Navigation

    @ViewBuilder
    private func detailView(for message: Message) -> some View {
        if let factory {
            MessageDetailView(factory: factory, seed: message)
        } else {
            MessageDetailView(seed: message)
        }
    }
}

#Preview {
    MessagesListView()
}
