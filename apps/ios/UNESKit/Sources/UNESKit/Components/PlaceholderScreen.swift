import SwiftUI

struct PlaceholderScreen: View {
    let title: LocalizedStringResource
    let systemImage: String
    var message: LocalizedStringResource = .commonUnderConstruction

    var body: some View {
        ContentUnavailableView {
            Label {
                Text(title)
            } icon: {
                Image(systemName: systemImage)
            }
        } description: {
            Text(message)
        }
    }
}

#Preview {
    PlaceholderScreen(title: .commonToday, systemImage: "square.grid.2x2")
}
