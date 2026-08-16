import SwiftUI

/// Pick a format, preview it, hand it to the system share sheet — which
/// already owns saving and copying, so neither is duplicated here.
struct MessageShareSheet: View {
    var message: MessageItem

    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss

    @State private var format = Format.image
    @State private var rendered: Image?

    enum Format: CaseIterable, Hashable {
        case image, text

        var label: String {
            switch self {
            case .image: .localized(.messagesShareFormatImage)
            case .text: .localized(.messagesShareFormatText)
            }
        }

        var caption: String {
            switch self {
            case .image: .localized(.messagesShareCaptionImage)
            case .text: .localized(.messagesShareCaptionText)
            }
        }
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    Picker(String.localized(.messagesShareFormat), selection: $format) {
                        ForEach(Format.allCases, id: \.self) { format in
                            Text(format.label).tag(format)
                        }
                    }
                    .segmentedPickerCompat()

                    Text(format.caption)
                        .font(.footnote)
                        .foregroundStyle(UNESColor.ink3)
                        .frame(maxWidth: .infinity, alignment: .leading)

                    preview
                        .frame(maxWidth: .infinity)
                }
                .padding(EdgeInsets(top: 12, leading: 20, bottom: 24, trailing: 20))
            }
            .background(UNESColor.surface)
            .safeAreaInset(edge: .bottom) { shareBar }
            .navigationTitle(Text(.messagesShareTitle))
            .inlineNavigationBar()
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(role: .cancel) { dismiss() } label: {
                        Text(.commonCancel)
                    }
                }
            }
        }
        .presentationDetents([.large])
        .presentationDragIndicator(.hidden)
        // The palette is baked in, so an appearance flip needs a re-render.
        .task(id: colorScheme) { render() }
    }

    // MARK: Preview

    @ViewBuilder
    private var preview: some View {
        switch format {
        case .image:
            MessageShareCard(message: message, width: 248, scheme: colorScheme)
                .shadow(color: UNESColor.shareCardShadow, radius: 18, y: 10)
                .padding(.vertical, 6)

        case .text:
            Text(MessageShareText.build(for: message))
                .font(.system(size: 12.5, design: .monospaced))
                .lineSpacing(3)
                .foregroundStyle(UNESColor.ink2)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(16)
                .background(UNESColor.card)
                .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                .overlay {
                    RoundedRectangle(cornerRadius: 18, style: .continuous)
                        .strokeBorder(UNESColor.cardLine)
                }
        }
    }

    // MARK: Handoff

    @ViewBuilder
    private var shareBar: some View {
        Group {
            switch format {
            case .image:
                if let rendered {
                    ShareLink(
                        item: rendered,
                        preview: SharePreview(previewTitle, image: rendered)
                    ) {
                        shareLabel
                    }
                } else {
                    // Holds the button's height so the preview doesn't shift.
                    ProgressView()
                        .frame(maxWidth: .infinity)
                        .frame(height: 54)
                }

            case .text:
                ShareLink(
                    item: MessageShareText.build(for: message),
                    preview: SharePreview(previewTitle)
                ) {
                    shareLabel
                }
            }
        }
        .buttonStyle(.unesAccent)
        .padding(EdgeInsets(top: 10, leading: 20, bottom: 8, trailing: 20))
        .background(UNESColor.surface)
    }

    private var shareLabel: some View {
        HStack(spacing: 8) {
            Image(systemName: "square.and.arrow.up")
                .font(.system(size: 15, weight: .semibold))
            Text(.messagesShareAction).tracking(-0.17)
        }
    }

    private var previewTitle: String {
        message.subject ?? message.senderName
    }

    @MainActor
    private func render() {
        let renderer = ImageRenderer(
            content: MessageShareCard(message: message, scheme: colorScheme)
        )
        renderer.scale = 3
        renderer.proposedSize = ProposedViewSize(width: MessageShareCard.exportWidth, height: nil)
        guard let image = renderer.cgImage else { return }
        rendered = Image(decorative: image, scale: 3)
    }
}

#Preview {
    MessageShareSheet(message: MessagesOverview.preview().messages[1])
}
