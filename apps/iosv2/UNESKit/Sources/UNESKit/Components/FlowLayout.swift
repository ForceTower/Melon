import SwiftUI

/// Left-aligned wrapping rows — CSS `flex-wrap: wrap` for chip clusters.
/// Subviews wider than the container are clamped to it, so oversized chips
/// truncate instead of overflowing.
struct FlowLayout: Layout {
    var spacing: CGFloat = 7
    var lineSpacing: CGFloat = 7

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        arrange(subviews, in: proposal.width ?? .infinity).size
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let slots = arrange(subviews, in: bounds.width).slots
        for (subview, slot) in zip(subviews, slots) {
            subview.place(
                at: CGPoint(x: bounds.minX + slot.origin.x, y: bounds.minY + slot.origin.y),
                // Re-proposing the measured size squeezes chips whose content
                // is flexible — an HStack of two Texts offered exactly its
                // ideal width splits it evenly and truncates the wider Text.
                // Unclamped chips re-take their ideal size instead.
                proposal: slot.clamped ? ProposedViewSize(slot.size) : .unspecified
            )
        }
    }

    private func arrange(
        _ subviews: Subviews,
        in width: CGFloat
    ) -> (slots: [(origin: CGPoint, size: CGSize, clamped: Bool)], size: CGSize) {
        var slots: [(origin: CGPoint, size: CGSize, clamped: Bool)] = []
        var cursor = CGPoint.zero
        var lineHeight: CGFloat = 0
        var maxX: CGFloat = 0

        for subview in subviews {
            var size = subview.sizeThatFits(.unspecified)
            let clamped = size.width > width
            if clamped {
                size = subview.sizeThatFits(ProposedViewSize(width: width, height: nil))
                size.width = min(size.width, width)
            }
            if cursor.x > 0, cursor.x + size.width > width {
                cursor.x = 0
                cursor.y += lineHeight + lineSpacing
                lineHeight = 0
            }
            slots.append((cursor, size, clamped))
            cursor.x += size.width + spacing
            lineHeight = max(lineHeight, size.height)
            maxX = max(maxX, cursor.x - spacing)
        }
        return (slots, CGSize(width: maxX, height: cursor.y + lineHeight))
    }
}
