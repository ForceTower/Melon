import ComposableArchitecture
import SwiftUI

/// The Final Countdown calculator: discipline context, the verdict hero,
/// composition breakdown, and the editable evaluation sandbox. The ambient
/// wash and hero mesh retint as the verdict changes.
struct FinalCountdownView: View {
    @Bindable var store: StoreOf<FinalCountdownFeature>
    @State private var titleProgress: CGFloat = 0

    var body: some View {
        let verdict = store.verdict
        let style = verdict.style(nextSemesterLabel: store.discipline?.nextSemesterLabel)

        ZStack(alignment: .top) {
            UNESColor.surface.ignoresSafeArea()
            ambientWash(style.mesh)
            content(verdict: verdict, style: style)
        }
        .toolbar {
            ToolbarItem(placement: .principal) {
                Text(.finalCountdownTitle)
                    .font(.system(size: 16, weight: .semibold))
                    .tracking(-0.32)
                    .foregroundStyle(UNESColor.ink)
                    .opacity(titleProgress)
                    .offset(y: (1 - titleProgress) * 6)
            }
        }
        .inlineNavigationBar()
        .task { await store.send(.task).finish() }
        .sheet(isPresented: pickerBinding) {
            FCDisciplinePicker(
                choices: store.choices,
                selectedId: store.discipline?.id,
                onPick: { store.send(.disciplinePicked($0)) },
                onClose: { store.send(.pickerDismissed) }
            )
        }
    }

    // MARK: Content

    private func content(verdict: FCVerdict, style: FCVerdictStyle) -> some View {
        ScrollView {
            VStack(spacing: 0) {
                header
                    .fadeUp(delay: 0.02)

                VStack(spacing: 0) {
                    FCDisciplineRow(discipline: store.discipline, canChange: canChange) {
                        store.send(.changeTapped)
                    }
                    .fadeUp(delay: 0.08)
                    .padding(.bottom, 12)

                    FCVerdictHero(verdict: verdict, style: style, weighted: store.weighted)
                        .scaleIn(delay: 0.14, duration: 0.62)
                        .padding(.bottom, 22)

                    FCBreakdownCard(rows: Array(store.rows), weighted: store.weighted)
                        .fadeUp(delay: 0.24)
                        .padding(.bottom, 12)

                    evaluationsSection
                        .fadeUp(delay: 0.32)

                    infoCard
                        .fadeUp(delay: 0.4)

                    footerQuip
                        .fadeUp(delay: 0.46)
                }
                .padding(.horizontal, 16)
            }
            .padding(.top, 16)
            .padding(.bottom, 12)
        }
        .scrollIndicators(.hidden)
        .scrollDismissesKeyboard(.interactively)
        .onScrollGeometryChange(for: CGFloat.self) { geometry in
            geometry.contentOffset.y + geometry.contentInsets.top
        } action: { _, offset in
            titleProgress = min(max((offset - 40) / 44, 0), 1)
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 5) {
            Text(.finalCountdownTitle)
                .font(.system(size: 40, weight: .bold))
                .tracking(-1.6)
                .foregroundStyle(UNESColor.ink)
            Text(.finalCountdownSubtitle)
                .font(.system(size: 14, weight: .medium))
                .tracking(-0.14)
                .foregroundStyle(UNESColor.ink3)
                .frame(maxWidth: 300, alignment: .leading)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(EdgeInsets(top: 2, leading: 20, bottom: 16, trailing: 20))
    }

    // MARK: Evaluations

    private var evaluationsSection: some View {
        VStack(spacing: 0) {
            HStack(alignment: .lastTextBaseline) {
                Text(.finalCountdownEvaluationsTitle)
                    .font(.system(size: 22, weight: .bold))
                    .tracking(-0.66)
                    .foregroundStyle(UNESColor.ink)
                Spacer()
                Text(.finalCountdownEvaluationsFilledCount(filledCount, store.rows.count))
                    .font(.system(size: 13, weight: .semibold))
                    .monospacedDigit()
                    .foregroundStyle(UNESColor.ink3)
            }
            .padding(EdgeInsets(top: 10, leading: 4, bottom: 12, trailing: 4))

            FCWeightedRow(weighted: store.weighted) { store.send(.weightedToggled($0)) }
                .padding(.bottom, 12)

            FCGradeList(
                rows: Array(store.rows),
                weighted: store.weighted,
                onScore: { store.send(.scoreEdited(id: $0, text: $1)) },
                onLabel: { store.send(.labelEdited(id: $0, text: $1)) },
                onWeight: { store.send(.weightStepped(id: $0, delta: $1)) },
                onRemove: { store.send(.rowRemoved(id: $0)) },
                onAdd: { store.send(.addRowTapped) }
            )
            .padding(.bottom, 12)

            clearButton
                .padding(.bottom, 12)
        }
    }

    private var clearButton: some View {
        Button {
            store.send(.clearTapped)
        } label: {
            HStack(spacing: 8) {
                Image(systemName: "arrow.counterclockwise")
                    .font(.system(size: 13, weight: .semibold))
                Text(.finalCountdownEvaluationsClearAll)
                    .font(.system(size: 15, weight: .semibold))
                    .tracking(-0.15)
            }
            .foregroundStyle(UNESColor.ink2)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 13)
            .background(UNESColor.card)
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .strokeBorder(UNESColor.cardLine)
            }
            .shadow(color: Color(hex: 0x141020, opacity: 0.05), radius: 9, y: 6)
        }
        .buttonStyle(TilePressStyle())
    }

    // MARK: Footnotes

    private var infoCard: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: "info.circle")
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(UNESColor.tangerine)
                .frame(width: 28, height: 28)
                .background(
                    UNESColor.tangerine.opacity(0.15),
                    in: RoundedRectangle(cornerRadius: 8, style: .continuous)
                )

            (
                Text(.finalCountdownInfoIntro)
                    + Text(verbatim: "≥ 7,0").fontWeight(.semibold).foregroundColor(UNESColor.ink)
                    + Text(.finalCountdownInfoFinalRange)
                    + Text(verbatim: "3,0 ≤ m < 7,0").fontWeight(.semibold).foregroundColor(UNESColor.ink)
                    + Text(.finalCountdownInfoFailRange)
                    + Text(verbatim: "m < 3,0").fontWeight(.semibold).foregroundColor(UNESColor.ink)
                    + Text(.finalCountdownInfoOutro)
            )
            .font(.system(size: 12.5, weight: .medium))
            .lineSpacing(4)
            .foregroundStyle(UNESColor.ink3)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(EdgeInsets(top: 13, leading: 15, bottom: 13, trailing: 15))
        .background(UNESColor.card)
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .strokeBorder(UNESColor.cardLine)
        }
        .shadow(color: Color(hex: 0x141020, opacity: 0.05), radius: 9, y: 6)
    }

    private var footerQuip: some View {
        Text(.finalCountdownFooterQuip)
            .font(.system(size: 11.5, weight: .medium))
            .foregroundStyle(UNESColor.ink4)
            .frame(maxWidth: .infinity)
            .padding(EdgeInsets(top: 16, leading: 16, bottom: 4, trailing: 16))
    }

    /// Ambient wash tinted to the verdict, bleeding down from the top.
    private func ambientWash(_ variant: MeshView.Variant) -> some View {
        MeshView(variant: variant, intensity: 0.5)
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
            .allowsHitTesting(false)
    }

    // MARK: Derived

    private var filledCount: Int {
        store.rows.count { $0.score != nil }
    }

    /// "Trocar" only shows when there's somewhere else to go — another
    /// discipline, or modo livre while one is attached.
    private var canChange: Bool {
        store.discipline != nil || !store.choices.isEmpty
    }

    private var pickerBinding: Binding<Bool> {
        Binding(
            get: { store.isPickerPresented },
            set: { value in
                if !value { store.send(.pickerDismissed) }
            }
        )
    }
}

#Preview {
    NavigationStack {
        FinalCountdownView(
            store: Store(initialState: FinalCountdownFeature.State()) {
                FinalCountdownFeature()
            }
        )
    }
}
