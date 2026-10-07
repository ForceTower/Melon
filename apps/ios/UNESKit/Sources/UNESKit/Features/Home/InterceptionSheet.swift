import ComposableArchitecture
import SwiftUI

/// Opened from the Hoje banner while the network intercepts the API's TLS.
/// Informational: swiping it away leaves the app fully usable on saved data.
struct InterceptionSheet: View {
    let store: StoreOf<InterceptionFeature>

    @State private var height: CGFloat = 340

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            header
            Text(.interceptionSheetHint)
                .font(.system(size: 13.5, weight: .semibold))
                .lineSpacing(3)
                .foregroundStyle(UNESColor.ink)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.top, 14)
            if store.isStillBlocked {
                Text(.interceptionSheetStillBlocked)
                    .font(.system(size: 12.5, weight: .medium))
                    .foregroundStyle(UNESBannerTone.danger)
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(.top, 10)
            }
            actions
                .padding(.top, 18)
        }
        .padding(EdgeInsets(top: 24, leading: 18, bottom: 16, trailing: 18))
        .onGeometryChange(for: CGFloat.self) { proxy in
            proxy.size.height
        } action: { measured in
            height = measured
            // Same detent nudge as `ReauthSheet`: the "still blocked" line
            // appears mid-presentation, and those updates can get dropped.
            Task { @MainActor in
                try? await Task.sleep(for: .milliseconds(700))
                if height == measured {
                    height = measured + 0.001
                }
            }
        }
        .presentationBackground(UNESColor.surface)
        .presentationDetents([.height(height)])
        .presentationDragIndicator(.visible)
    }

    private var header: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: "exclamationmark.shield.fill")
                .font(.system(size: 14, weight: .bold))
                .foregroundStyle(.white)
                .frame(width: 34, height: 34)
                .background(UNESBannerTone.warn, in: RoundedRectangle(cornerRadius: 11, style: .continuous))

            VStack(alignment: .leading, spacing: 3) {
                Text(.interceptionSheetTitle)
                    .font(.system(size: 19, weight: .bold))
                    .tracking(-0.4)
                    .foregroundStyle(UNESColor.ink)
                Group {
                    if let issuerName = store.interception?.issuerName {
                        Text(.interceptionSheetBodyNamed(issuerName))
                    } else {
                        Text(.interceptionSheetBodyUnnamed)
                    }
                }
                .font(.system(size: 13, weight: .medium))
                .lineSpacing(2)
                .foregroundStyle(UNESColor.ink3)
                .fixedSize(horizontal: false, vertical: true)
            }
        }
    }

    private var actions: some View {
        VStack(spacing: 12) {
            Button {
                store.send(.retryTapped)
            } label: {
                if store.isRetrying {
                    SpinnerRing(
                        size: 20,
                        color: UNESColor.surface,
                        trackColor: UNESColor.paper.opacity(0.3),
                        speed: 0.7
                    )
                } else {
                    UNESButtonLabel(text: .commonTryAgain)
                }
            }
            .buttonStyle(.unesDark)
            .disabled(store.isRetrying)

            Button {
                store.send(.closeTapped)
            } label: {
                Text(.interceptionSheetCancel)
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(UNESColor.ink3)
            }
            .buttonStyle(.plain)
        }
    }
}

#Preview {
    let _ = Shared<TLSInterception?>(.tlsInterception).withLock { $0 = TLSInterception(issuerName: "Fortinet") }
    InterceptionSheet(
        store: Store(initialState: InterceptionFeature.State()) {
            InterceptionFeature()
        }
    )
}
