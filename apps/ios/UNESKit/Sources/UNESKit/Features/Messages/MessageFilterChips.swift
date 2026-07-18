import SwiftUI

/// The horizontally scrolling filter pills, each carrying its match count.
struct MessageFilterChips: View {
    var active: MessageFilter
    var messages: [MessageItem]
    var onChange: (MessageFilter) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 7) {
                ForEach(MessageFilter.allCases, id: \.self) { filter in
                    chip(filter)
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 4)
        }
        // The active chip's shadow reaches ~10pt below the row, so it clips
        // at the scroll bounds without this.
        .scrollClipDisabled()
        .animation(.easeOut(duration: 0.15), value: active)
    }

    private func chip(_ filter: MessageFilter) -> some View {
        let on = filter == active
        let count = messages.count { filter.matches($0) }
        return Button {
            onChange(filter)
        } label: {
            HStack(spacing: 6) {
                Text(filter.label)
                    .font(.system(size: 13, weight: .semibold))
                    .tracking(-0.13)
                    .foregroundStyle(on ? .white : UNESColor.ink2)
                if count > 0 {
                    Text(verbatim: "\(count)")
                        .font(.system(size: 11.5, weight: .bold))
                        .monospacedDigit()
                        .foregroundStyle(on ? .white.opacity(0.85) : UNESColor.ink4)
                }
            }
            .padding(EdgeInsets(top: 7, leading: 13, bottom: 7, trailing: 13))
            .background(
                on ? UNESColor.accent : UNESColor.card,
                in: RoundedRectangle(cornerRadius: 15, style: .continuous)
            )
            .overlay {
                RoundedRectangle(cornerRadius: 15, style: .continuous)
                    .strokeBorder(on ? UNESColor.accent : UNESColor.cardLine)
            }
            .shadow(
                color: on ? .black.opacity(0.12) : Color(hex: 0x141020, opacity: 0.04),
                radius: on ? 6 : 3,
                y: on ? 4 : 2
            )
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    VStack {
        MessageFilterChips(active: .all, messages: MessagesOverview.preview().messages, onChange: { _ in })
        MessageFilterChips(active: .unread, messages: MessagesOverview.preview().messages, onChange: { _ in })
    }
    .padding(.vertical, 16)
    .background(UNESColor.surface)
}
