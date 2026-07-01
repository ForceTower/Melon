import SwiftUI

struct PlaceholderScreen: View {
    let title: String
    let systemImage: String
    var message: String = "Em construção"

    var body: some View {
        ContentUnavailableView {
            Label(title, systemImage: systemImage)
        } description: {
            Text(message)
        }
    }
}

#Preview {
    PlaceholderScreen(title: "Horário", systemImage: "square.grid.2x2")
}
