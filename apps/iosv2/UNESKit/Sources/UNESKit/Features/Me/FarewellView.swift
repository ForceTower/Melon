import ComposableArchitecture
import SwiftUI

/// The full-screen goodbye after a logout: a brief "Encerrando sessão…"
/// flash, then the farewell with the way back to the login.
struct FarewellView: View {
    let store: StoreOf<FarewellFeature>

    var body: some View {
        ZStack {
            UNESColor.surface.ignoresSafeArea()
            if store.isFlashing {
                flash
                    .transition(.opacity)
            } else {
                farewell
                    .transition(.opacity)
            }
        }
        .task { await store.send(.task).finish() }
    }

    // MARK: Flash

    private var flash: some View {
        VStack(spacing: 14) {
            Image(systemName: "rectangle.portrait.and.arrow.right")
                .font(.system(size: 19, weight: .medium))
                .foregroundStyle(UNESColor.readable(0xE85D4E))
                .frame(width: 46, height: 46)
                .background(UNESColor.coral.opacity(0.14), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                .overlay {
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .strokeBorder(UNESColor.coral.opacity(0.3))
                }
            Text("Encerrando sessão…")
                .font(.system(size: 12.5, weight: .semibold))
                .tracking(0.3)
                .foregroundStyle(UNESColor.ink3)
        }
    }

    // MARK: Farewell

    private var farewell: some View {
        ZStack(alignment: .top) {
            mesh
            VStack(alignment: .leading, spacing: 0) {
                Text("Sessão encerrada")
                    .textCase(.uppercase)
                    .font(.system(size: 13, weight: .semibold))
                    .tracking(0.3)
                    .foregroundStyle(UNESColor.accent)
                    .padding(.bottom, 10)

                title
                    .padding(.bottom, 14)

                Text("Seus dados foram removidos deste aparelho. Quando quiser voltar, o idUEFS te espera.")
                    .font(.system(size: 15, weight: .medium))
                    .lineSpacing(5)
                    .foregroundStyle(UNESColor.ink2)
                    .frame(maxWidth: 280, alignment: .leading)
                    .padding(.bottom, 26)

                signInButton

                Text(MeFormat.versionLabel)
                    .font(.system(size: 11, weight: .medium))
                    .tracking(0.4)
                    .foregroundStyle(UNESColor.ink4)
                    .frame(maxWidth: .infinity)
                    .padding(.top, 14)
            }
            .padding(.horizontal, 28)
            .frame(maxHeight: .infinity, alignment: .center)
        }
        .fadeUp(delay: 0, duration: 0.5)
    }

    private var title: some View {
        (
            Text("Até logo,\n")
                + Text("\(store.firstName ?? "estudante").").foregroundStyle(UNESColor.accent)
        )
        .font(.system(size: 38, weight: .bold))
        .tracking(-1.52)
        .foregroundStyle(UNESColor.ink)
    }

    private var signInButton: some View {
        Button {
            store.send(.signInTapped)
        } label: {
            HStack(spacing: 8) {
                Text("Entrar com idUEFS")
                    .font(.system(size: 15, weight: .semibold))
                Image(systemName: "arrow.right")
                    .font(.system(size: 12, weight: .semibold))
            }
            .foregroundStyle(UNESColor.surface)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 15)
            .background(UNESColor.ink, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            .shadow(color: .black.opacity(0.2), radius: 14, y: 12)
        }
        .buttonStyle(TilePressStyle())
    }

    /// The rose wash bleeding down from the top edge.
    private var mesh: some View {
        MeshView(variant: .rose, intensity: 0.7)
            .frame(height: 360)
            .padding(.horizontal, -60)
            .mask {
                LinearGradient(
                    stops: [
                        .init(color: .white, location: 0),
                        .init(color: .clear, location: 0.94),
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
            }
            .opacity(0.5)
            .offset(y: -40)
            .ignoresSafeArea()
    }
}

#Preview("Despedida") {
    FarewellView(
        store: Store(
            initialState: FarewellFeature.State(firstName: "Mariana", isFlashing: false)
        ) {
            FarewellFeature()
        }
    )
}

#Preview("Encerrando") {
    FarewellView(
        store: Store(initialState: FarewellFeature.State(firstName: "Mariana")) {
            FarewellFeature()
        }
    )
}
