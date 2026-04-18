import SwiftUI

/// Full message detail — sender card, optional serif subject, formatted body
/// with linkified URLs, image gallery, and the attachments list.
///
/// Mirrors `MessageDetailScreen` in `screens-message-detail.jsx`.
struct MessageDetailView: View {
    let message: Message
    var onMarkRead: (Message) -> Void = { _ in }

    private var images: [MessageAttachment] {
        message.attachments.filter { $0.kind == .image }
    }

    private var nonImages: [MessageAttachment] {
        message.attachments.filter { $0.kind != .image }
    }

    var body: some View {
        ZStack(alignment: .top) {
            UNESColor.surface.ignoresSafeArea()

            // Origin-tinted ambient glow near the top of the screen. A linear
            // fade to `UNESColor.surface` rides on top of the radial so the
            // bottom of the frame ends on the exact surface color that's
            // painted below — without it, the radial still carries a trace
            // of tint at the cutoff and reads as a visible seam.
            VStack(spacing: 0) {
                ZStack {
                    RadialGradient(
                        colors: [message.meta.color.opacity(0.165), .clear],
                        center: .top,
                        startRadius: 0,
                        endRadius: 320
                    )
                    LinearGradient(
                        stops: [
                            .init(color: .clear, location: 0),
                            .init(color: UNESColor.surface, location: 1.0),
                        ],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                }
                .frame(height: 260)
                Spacer(minLength: 0)
            }
            .ignoresSafeArea(edges: .top)
            .allowsHitTesting(false)

            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 0) {
                    senderCard
                        .padding(.horizontal, 16)
                        .padding(.top, 8)
                        .padding(.bottom, 18)
                        .fadeUpOnAppear(delay: 0.04, distance: 10, duration: 0.55)

                    subjectAndTimestamp
                        .padding(.horizontal, 20)
                        .padding(.bottom, 14)
                        .fadeUpOnAppear(delay: 0.12, distance: 10, duration: 0.55)

                    body(for: message)
                        .padding(.horizontal, 20)
                        .padding(.bottom, 20)
                        .fadeUpOnAppear(delay: 0.2, distance: 10, duration: 0.55)

                    if !images.isEmpty {
                        imagesGallery
                            .padding(.horizontal, 16)
                            .padding(.bottom, 16)
                            .fadeUpOnAppear(delay: 0.28, distance: 10, duration: 0.55)
                    }

                    if !nonImages.isEmpty {
                        attachmentsList
                            .padding(.horizontal, 16)
                            .padding(.bottom, 20)
                            .fadeUpOnAppear(delay: 0.34, distance: 10, duration: 0.55)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.bottom, 32)
            }
        }
        // Keep the system nav bar (so the back chevron + interactive swipe-
        // back gesture both work), but let the origin-tinted wash show
        // through — mirrors DisciplineDetailView.
        .toolbarBackground(.hidden, for: .navigationBar)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .principal) { EmptyView() }
        }
        .onAppear { onMarkRead(message) }
    }

    // MARK: - Sender card

    private var senderCard: some View {
        let meta = message.meta
        return HStack(alignment: .top, spacing: 12) {
            OriginSwatch(message: message, size: 44)

            VStack(alignment: .leading, spacing: 2) {
                Text(senderKindLabel(meta: meta))
                    .font(UNESFont.mono(9, weight: .semibold))
                    .tracking(1.1)
                    .textCase(.uppercase)
                    .foregroundStyle(meta.color)
                    .padding(.bottom, 2)

                Text(message.sender.name)
                    .font(UNESFont.sans(15, weight: .semibold))
                    .tracking(-0.15)
                    .foregroundStyle(UNESColor.ink)

                Text(message.sender.role)
                    .font(UNESFont.sans(12))
                    .foregroundStyle(UNESColor.ink3)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            starButton
        }
        .padding(14)
        .background(UNESColor.card)
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(UNESColor.cardLine, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
    }

    private func senderKindLabel(meta: MessageOriginMeta) -> String {
        if message.origin == .direct { return "\(meta.kind) · PARA VOCÊ" }
        return meta.kind
    }

    private var starButton: some View {
        let starred = message.starred
        return Image(systemName: starred ? "star.fill" : "star")
            .font(.system(size: 13))
            .foregroundStyle(starred
                             ? Color(red: 0xD9/255, green: 0x85/255, blue: 0x2E/255)
                             : UNESColor.ink3)
            .frame(width: 32, height: 32)
            .overlay(
                RoundedRectangle(cornerRadius: 8, style: .continuous)
                    .stroke(UNESColor.cardLine, lineWidth: 1)
            )
    }

    // MARK: - Subject + timestamp

    private var subjectAndTimestamp: some View {
        VStack(alignment: .leading, spacing: 10) {
            if let subject = message.subject, !subject.isEmpty {
                Text(subject)
                    .font(UNESFont.serif(26))
                    .tracking(-0.52)
                    .foregroundStyle(UNESColor.ink)
                    .fixedSize(horizontal: false, vertical: true)
            }

            Text(MessageDate.fullTime(for: message.receivedAt))
                .font(UNESFont.mono(10))
                .tracking(0.4)
                .foregroundStyle(UNESColor.ink4)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    // MARK: - Body

    private func body(for message: Message) -> some View {
        let hasSubject = message.subject != nil
        let accent = message.meta.color
        return linkifiedBody(message.body, accent: accent)
            .font(UNESFont.sans(hasSubject ? 14 : 15))
            .foregroundStyle(hasSubject ? UNESColor.ink2 : UNESColor.ink)
            .lineSpacing(hasSubject ? 14 * 0.55 : 15 * 0.6)
            .frame(maxWidth: .infinity, alignment: .leading)
            .fixedSize(horizontal: false, vertical: true)
    }

    /// Builds an `AttributedString` where URL-like tokens are tinted in the
    /// origin accent color. Matches the `linkify()` helper from the prototype.
    private func linkifiedBody(_ text: String, accent: Color) -> Text {
        var out = AttributedString()
        let pattern = #"(https?://[^\s]+|www\.[^\s]+|[a-z0-9.-]+\.(?:br|com|org|edu|net|io)/[^\s]*)"#
        guard let regex = try? NSRegularExpression(pattern: pattern, options: [.caseInsensitive]) else {
            return Text(text)
        }
        let ns = text as NSString
        var cursor = 0
        let matches = regex.matches(in: text, range: NSRange(location: 0, length: ns.length))
        for m in matches {
            let r = m.range
            if r.location > cursor {
                let before = ns.substring(with: NSRange(location: cursor, length: r.location - cursor))
                out += AttributedString(before)
            }
            var link = AttributedString(ns.substring(with: r))
            link.foregroundColor = accent
            link.underlineStyle = .single
            out += link
            cursor = r.location + r.length
        }
        if cursor < ns.length {
            out += AttributedString(ns.substring(from: cursor))
        }
        return Text(out)
    }

    // MARK: - Images gallery

    private var imagesGallery: some View {
        let columns: [GridItem] = images.count == 1
            ? [GridItem(.flexible(), spacing: 8)]
            : [GridItem(.flexible(), spacing: 8), GridItem(.flexible(), spacing: 8)]

        return LazyVGrid(columns: columns, spacing: 8) {
            ForEach(images) { a in
                AttachmentTile(attachment: a, accent: message.meta.color)
            }
        }
    }

    // MARK: - Attachments list

    private var attachmentsList: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("ANEXOS · \(nonImages.count)")
                .font(UNESFont.mono(10, weight: .semibold))
                .tracking(1.4)
                .foregroundStyle(UNESColor.ink4)
                .padding(.leading, 4)
                .padding(.bottom, 0)

            VStack(spacing: 8) {
                ForEach(nonImages) { a in
                    AttachmentTile(attachment: a, accent: message.meta.color)
                }
            }
        }
    }
}

#Preview {
    NavigationStack {
        MessageDetailView(message: MessageFixtures.messages[1])
    }
}
