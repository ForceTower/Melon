import SwiftUI

/// Two facing pages for a screen that isn't a navigation shell. The leading
/// page ends on a spine fold when there is one, and takes half otherwise.
struct FacingPages<Leading: View, Trailing: View>: View {
    @Environment(\.deviceFold) private var fold

    @ViewBuilder var leading: Leading
    @ViewBuilder var trailing: Trailing

    var body: some View {
        let spine = fold?.spineX
        HStack(spacing: 0) {
            leading
                .frame(width: spine)
                .frame(maxWidth: spine == nil ? .infinity : nil)
            trailing
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
    }
}
