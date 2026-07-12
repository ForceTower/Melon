import ComposableArchitecture
import SwiftUI

/// Chaves de acesso — the passkey manager: a dark-free cool-mesh header over a
/// card list of enrolled keys, an add affordance, and the create / rename /
/// delete flows. Authentication itself is the system's job, so "Continuar"
/// hands off to the platform passkey sheet.
struct PasskeysView: View {
    let store: StoreOf<PasskeysFeature>
    @State private var titleProgress: CGFloat = 0

    var body: some View {
        ZStack(alignment: .top) {
            UNESColor.surface.ignoresSafeArea()
            ambientWash
            content
        }
        .toolbar {
            ToolbarItem(placement: .principalCompat) {
                Text(.passkeysTitle)
                    .font(.system(size: 16, weight: .semibold))
                    .tracking(-0.32)
                    .foregroundStyle(UNESColor.ink)
                    .opacity(titleProgress)
                    .offset(y: (1 - titleProgress) * 6)
            }
        }
        .inlineNavigationBar()
        .overlay(alignment: .bottom) {
            if let toast = store.toast {
                toastPill(toast)
            }
        }
        .animation(UNESMotion.ease(0.24), value: store.toast)
        .task { await store.send(.task).finish() }
        .sheet(isPresented: addBinding) {
            PasskeyAddSheet(
                accountName: store.accountName ?? String.localized(.settingsDefaultName),
                avatarInitial: store.avatarInitial,
                target: store.addTarget,
                step: store.addStep,
                isCreating: store.isCreating,
                error: store.createError,
                onSelect: { store.send(.addTargetSelected($0)) },
                onContinue: { store.send(.addContinueTapped) },
                onCancel: { store.send(.addDismissed) }
            )
        }
        .sheet(isPresented: detailBinding) {
            detailSheet
        }
    }

    private var content: some View {
        ScrollView {
            VStack(spacing: 0) {
                header
                    .fadeUp(delay: 0.02)

                VStack(spacing: 0) {
                    listCard
                        .fadeUp(delay: 0.1)
                        .padding(.bottom, 14)

                    addButton
                        .fadeUp(delay: 0.18)

                    footnote
                        .fadeUp(delay: 0.26)
                }
                .padding(.horizontal, 16)
            }
            .padding(.top, 8)
            .padding(.bottom, 12)
        }
        .scrollIndicators(.hidden)
        .onScrollGeometryChange(for: CGFloat.self) { geometry in
            geometry.contentOffset.y + geometry.contentInsets.top
        } action: { _, offset in
            titleProgress = min(max((offset - 40) / 44, 0), 1)
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 5) {
            Text(.passkeysTitle)
                .font(.system(size: 34, weight: .bold))
                .tracking(-1.36)
                .foregroundStyle(UNESColor.ink)
            Text(.passkeysIntro)
                .font(.system(size: 14, weight: .medium))
                .tracking(-0.14)
                .lineSpacing(2)
                .foregroundStyle(UNESColor.ink3)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(EdgeInsets(top: 2, leading: 20, bottom: 18, trailing: 20))
    }

    @ViewBuilder
    private var listCard: some View {
        VStack(spacing: 0) {
            if store.credentials.isEmpty {
                if store.hasLoaded {
                    emptyState
                }
            } else {
                ForEach(Array(store.credentials.enumerated()), id: \.element.id) { index, credential in
                    Button {
                        store.send(.rowTapped(credential))
                    } label: {
                        PasskeyRow(credential: credential, isNew: credential.id == store.newCredentialID)
                    }
                    .buttonStyle(TilePressStyle())

                    if index < store.credentials.count - 1 {
                        Rectangle()
                            .fill(UNESColor.line)
                            .frame(height: 0.5)
                            .padding(.leading, 66)
                    }
                }
            }
        }
        .frame(maxWidth: .infinity)
        .background(UNESColor.card)
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .strokeBorder(UNESColor.cardLine)
        }
        .shadow(color: Color(hex: 0x141020, opacity: 0.05), radius: 9, y: 6)
        .animation(.snappy, value: store.newCredentialID)
    }

    private var addButton: some View {
        Button {
            store.send(.addTapped)
        } label: {
            HStack(spacing: 9) {
                Image(systemName: "plus")
                    .font(.system(size: 15, weight: .bold))
                Text(.passkeysAdd)
                    .font(.system(size: 16, weight: .semibold))
                    .tracking(-0.16)
            }
            .foregroundStyle(UNESColor.accent)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 15)
            .background(UNESColor.card)
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .strokeBorder(UNESColor.cardLine)
            }
            .shadow(color: Color(hex: 0x141020, opacity: 0.05), radius: 9, y: 6)
        }
        .buttonStyle(TilePressStyle())
    }

    private var footnote: some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: "checkmark.shield")
                .font(.system(size: 15, weight: .medium))
            Text(.passkeysListFooter)
                .font(.system(size: 12, weight: .medium))
                .lineSpacing(2)
        }
        .foregroundStyle(UNESColor.ink4)
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(EdgeInsets(top: 16, leading: 14, bottom: 4, trailing: 14))
    }

    private var emptyState: some View {
        VStack(spacing: 5) {
            Image(systemName: "key.fill")
                .font(.system(size: 26, weight: .medium))
                .foregroundStyle(UNESColor.ink4)
                .frame(width: 58, height: 58)
                .background(UNESColor.surface2, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                .padding(.bottom, 9)
            Text(.passkeysEmptyTitle)
                .font(.system(size: 16, weight: .semibold))
                .tracking(-0.32)
                .foregroundStyle(UNESColor.ink)
            Text(.passkeysEmptySubtitle)
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(UNESColor.ink4)
                .multilineTextAlignment(.center)
                .frame(maxWidth: 260)
        }
        .frame(maxWidth: .infinity)
        .padding(EdgeInsets(top: 30, leading: 24, bottom: 26, trailing: 24))
    }

    @ViewBuilder
    private var detailSheet: some View {
        if let credential = store.detail {
            PasskeyDetailSheet(
                credential: credential,
                isEditingName: store.isEditingName,
                renameText: renameTextBinding,
                isMutating: store.isMutating,
                error: store.mutationError,
                onRename: { store.send(.renameTapped) },
                onRenameCancel: { store.send(.renameCancelled) },
                onRenameSave: { store.send(.renameSubmitted) },
                onDelete: { store.send(.deleteTapped) }
            )
            .alert(
                Text(.passkeysDeleteTitle),
                isPresented: deleteBinding,
                presenting: store.pendingDelete
            ) { _ in
                Button(String.localized(.passkeysDeleteConfirm), role: .destructive) {
                    store.send(.deleteConfirmed)
                }
                Button(String.localized(.commonCancel), role: .cancel) {
                    store.send(.deleteDismissed)
                }
            } message: { credential in
                Text(credential.isSynced ? .passkeysDeleteMessageSynced : .passkeysDeleteMessageLocal)
            }
        }
    }

    private func toastPill(_ toast: String) -> some View {
        Text(toast)
            .font(.system(size: 13, weight: .semibold))
            .tracking(-0.13)
            .foregroundStyle(.white)
            .lineLimit(1)
            .padding(EdgeInsets(top: 10, leading: 18, bottom: 10, trailing: 18))
            .background(Color(hex: 0x14101A, opacity: 0.92), in: Capsule())
            .overlay {
                Capsule().strokeBorder(.white.opacity(0.14), lineWidth: 0.5)
            }
            .shadow(color: .black.opacity(0.35), radius: 17, y: 12)
            .padding(.bottom, 12)
            .transition(.offset(y: 12).combined(with: .opacity))
    }

    /// Faint cool mesh washing down from behind the large title.
    private var ambientWash: some View {
        MeshView(variant: .cool, intensity: 0.5)
            .frame(height: 320)
            .padding(.horizontal, -50)
            .mask {
                LinearGradient(
                    stops: [
                        .init(color: .white, location: 0),
                        .init(color: .clear, location: 0.92),
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
            }
            .opacity(0.3)
            .offset(y: -80)
            .ignoresSafeArea()
    }

    // MARK: Bindings

    private var addBinding: Binding<Bool> {
        Binding(
            get: { store.isAddPresented },
            set: { if !$0, store.isAddPresented { store.send(.addDismissed) } }
        )
    }

    private var detailBinding: Binding<Bool> {
        Binding(
            get: { store.detail != nil },
            set: { if !$0, store.detail != nil { store.send(.detailDismissed) } }
        )
    }

    private var deleteBinding: Binding<Bool> {
        Binding(
            get: { store.pendingDelete != nil },
            set: { if !$0, store.pendingDelete != nil { store.send(.deleteDismissed) } }
        )
    }

    private var renameTextBinding: Binding<String> {
        Binding(
            get: { store.renameText },
            set: { store.send(.renameTextChanged($0)) }
        )
    }
}

/// A tinted, rounded device tile — the visual anchor of every passkey.
struct PasskeyTile: View {
    let credential: PasskeyCredential
    var size: CGFloat = 38
    var radius: CGFloat = 10

    var body: some View {
        let tint = credential.isSynced ? UNESColor.violet : UNESColor.tangerine
        Image(systemName: credential.isSynced ? "key.icloud.fill" : "key.fill")
            .font(.system(size: size * 0.5, weight: .semibold))
            .foregroundStyle(.white)
            .frame(width: size, height: size)
            .background(tint, in: RoundedRectangle(cornerRadius: radius, style: .continuous))
            .shadow(color: tint.opacity(0.35), radius: size * 0.26, y: size * 0.1)
    }
}

/// One enrolled passkey: device tile, name, sync + creation line, chevron.
private struct PasskeyRow: View {
    let credential: PasskeyCredential
    let isNew: Bool

    var body: some View {
        HStack(spacing: 13) {
            PasskeyTile(credential: credential)

            VStack(alignment: .leading, spacing: 3) {
                Text(credential.deviceName ?? String.localized(.passkeysUnnamed))
                    .font(.system(size: 15.5, weight: .semibold))
                    .tracking(-0.15)
                    .foregroundStyle(UNESColor.ink)
                    .lineLimit(1)

                HStack(spacing: 6) {
                    Image(systemName: credential.isSynced ? "icloud" : "key")
                        .font(.system(size: 11, weight: .medium))
                    Text(credential.isSynced ? .passkeysSynced : .passkeysDeviceBound)
                    Text(verbatim: "·").opacity(0.5)
                    Text(verbatim: created)
                }
                .font(.system(size: 12, weight: .medium))
                .foregroundStyle(UNESColor.ink4)
                .lineLimit(1)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            if isNew {
                Image(systemName: "checkmark.circle.fill")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(UNESColor.successGreen)
                    .transition(.scale.combined(with: .opacity))
            } else {
                Image(systemName: "chevron.forward")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(UNESColor.ink4)
            }
        }
        .padding(EdgeInsets(top: 12, leading: 15, bottom: 12, trailing: 15))
        .contentShape(Rectangle())
    }

    private var created: String {
        credential.createdAt.formatted(date: .abbreviated, time: .omitted)
    }
}

#Preview {
    NavigationStack {
        PasskeysView(
            store: Store(
                initialState: PasskeysFeature.State(
                    accountName: "Mariana Nogueira",
                    credentials: [.preview],
                    hasLoaded: true
                )
            ) {
                PasskeysFeature()
            } withDependencies: {
                $0.passkeyRepository = .previewValue
            }
        )
    }
}
