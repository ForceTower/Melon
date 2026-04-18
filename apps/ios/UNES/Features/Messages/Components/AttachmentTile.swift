import SwiftUI

/// Attachment tile used on the message detail screen. Three layouts:
///
/// * `.image` — a 16:10 gradient panel with a picture glyph in the accent.
/// * `.link`  — title + host pair with a link-chain icon.
/// * default  — file name + size, with a download arrow.
struct AttachmentTile: View {
    let attachment: MessageAttachment
    let accent: Color

    var body: some View {
        switch attachment.kind {
        case .image: imageTile
        case .link:  linkTile
        default:     fileTile
        }
    }

    // MARK: - Image

    private var imageTile: some View {
        ZStack {
            LinearGradient(
                colors: [accent.opacity(0.2), accent.opacity(0.07)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            Image(systemName: "photo")
                .font(.system(size: 28, weight: .light))
                .foregroundStyle(accent)
        }
        .aspectRatio(16.0/10.0, contentMode: .fit)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .stroke(UNESColor.cardLine, lineWidth: 1)
        )
    }

    // MARK: - Link

    private var linkTile: some View {
        HStack(spacing: 12) {
            badge(systemName: "link")

            VStack(alignment: .leading, spacing: 2) {
                Text(attachment.title ?? "")
                    .font(UNESFont.sans(13, weight: .medium))
                    .foregroundStyle(UNESColor.ink)
                    .lineLimit(1)
                    .truncationMode(.tail)
                Text(attachment.host ?? "")
                    .font(UNESFont.mono(10))
                    .tracking(0.3)
                    .foregroundStyle(UNESColor.ink4)
                    .lineLimit(1)
                    .truncationMode(.tail)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(12)
        .background(UNESColor.card)
        .overlay(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .stroke(UNESColor.cardLine, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    // MARK: - File (pdf/slides/other)

    private var fileTile: some View {
        HStack(spacing: 12) {
            badge(systemName: attachment.kind == .slides ? "rectangle.on.rectangle" : "doc")

            VStack(alignment: .leading, spacing: 2) {
                Text(attachment.name ?? "")
                    .font(UNESFont.sans(13, weight: .medium))
                    .foregroundStyle(UNESColor.ink)
                    .lineLimit(1)
                    .truncationMode(.middle)
                Text(metaLine)
                    .font(UNESFont.mono(10))
                    .tracking(0.3)
                    .foregroundStyle(UNESColor.ink4)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Image(systemName: "arrow.down")
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(UNESColor.ink3)
        }
        .padding(12)
        .background(UNESColor.card)
        .overlay(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .stroke(UNESColor.cardLine, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private var metaLine: String {
        var parts: [String] = [attachment.kind.label]
        if let size = attachment.size { parts.append(size) }
        return parts.joined(separator: " · ")
    }

    private func badge(systemName: String) -> some View {
        ZStack {
            RoundedRectangle(cornerRadius: 10, style: .continuous)
                .fill(accent.opacity(0.13))
            Image(systemName: systemName)
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(accent)
        }
        .frame(width: 40, height: 40)
    }
}
