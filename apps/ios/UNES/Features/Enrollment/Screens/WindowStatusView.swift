import SwiftUI

// UNES — enrollment entry. A mesh hero announcing the window state, the
// student strip, the running workload + proposal tallies, and the call to
// action. Ported from `WindowStatusScreen` in `screens-matricula-screens.jsx`.
struct WindowStatusView: View {
    let enroll: EnrollmentState
    let window: EnrollmentWindow
    let windowState: EnrollmentWindowState
    let student: EnrollmentStudent
    let onStart: () -> Void
    let onReview: () -> Void

    private let heroBg = Color(red: 0x16 / 255, green: 0x0E / 255, blue: 0x22 / 255)

    private var isOpen: Bool { windowState == .open }
    private var isUpcoming: Bool { windowState == .upcoming }
    private var isClosed: Bool { windowState == .closed }

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(alignment: .leading, spacing: 14) {
                header
                    .fadeUpOnAppear(delay: 0.02, distance: 12, duration: 0.5)
                hero
                    .fadeUpOnAppear(delay: 0.06, distance: 14, duration: 0.55)
                studentStrip
                    .fadeUpOnAppear(delay: 0.12, distance: 12, duration: 0.55)
                WorkloadMeter(total: enroll.totalHours, min: window.minHours, max: window.maxHours, compact: true)
                    .fadeUpOnAppear(delay: 0.18, distance: 12, duration: 0.55)
                statTiles
                    .fadeUpOnAppear(delay: 0.24, distance: 12, duration: 0.55)
                cta
                    .fadeUpOnAppear(delay: 0.3, distance: 12, duration: 0.55)
                footer
                    .frame(maxWidth: .infinity)
                    .fadeUpOnAppear(delay: 0.36, distance: 10, duration: 0.5)
            }
            .padding(.horizontal, 18)
            .padding(.top, 4)
            .padding(.bottom, 28)
        }
        .background(UNESColor.surface.ignoresSafeArea())
        .navigationTitle("Matrícula")
        .navigationBarTitleDisplayMode(.inline)
    }

    // MARK: Header

    private var header: some View {
        EnrollmentEyebrow(text: "◦ período de seleção · \(window.semester)")
            .padding(.top, 4)
    }

    // MARK: Hero

    private var heroTitle: String {
        if isUpcoming { return "Abre em breve" }
        if isClosed { return "Proposta enviada" }
        return "Matrícula aberta"
    }

    private var heroEyebrow: String {
        isClosed ? "protocolo \(window.semester) · #48201" : "período de seleção · \(window.semester)"
    }

    private var heroDotColor: Color {
        if isUpcoming { return UNESColor.amber }
        if isClosed { return Color(red: 0x7B / 255, green: 0xD3 / 255, blue: 0xA6 / 255) }
        return Color(red: 0x88 / 255, green: 0xD4 / 255, blue: 0xC1 / 255)
    }

    private var heroBody: Text {
        switch windowState {
        case .upcoming:
            return Text("A janela de matrícula abre em ")
                + Text(window.startLabel).bold() + Text(". Prepare suas escolhas com antecedência.")
        case .open:
            return Text("Selecione suas turmas até ")
                + Text(window.endLabel).bold() + Text(". Você pode salvar e ajustar quantas vezes quiser.")
        case .closed:
            return Text("Sua proposta foi enviada e está em processamento. Para alterar, é preciso reabrir a matrícula.")
        }
    }

    private var hero: some View {
        ZStack(alignment: .topLeading) {
            MeshGradientView(variant: isClosed ? .fresh : .cool, intensity: 0.95)
            LinearGradient(
                colors: [heroBg.opacity(0.05), heroBg.opacity(0.6)],
                startPoint: .top, endPoint: .bottom
            )
            VStack(alignment: .leading, spacing: 0) {
                HStack(spacing: 7) {
                    Circle().fill(heroDotColor).frame(width: 6, height: 6).pulseForever()
                    Text(heroEyebrow.uppercased())
                        .font(UNESFont.mono(10, weight: .medium))
                        .tracking(1.6)
                        .foregroundStyle(.white.opacity(0.72))
                }
                Text(heroTitle)
                    .font(UNESFont.serif(38))
                    .tracking(-0.76)
                    .foregroundStyle(.white)
                    .padding(.top, 16)
                heroBody
                    .font(UNESFont.sans(13))
                    .foregroundStyle(.white.opacity(0.8))
                    .lineSpacing(2)
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(.top, 8)
                timeline
                    .padding(.top, 18)
            }
            .padding(20)
        }
        .frame(minHeight: 200)
        .background(heroBg)
        .clipShape(RoundedRectangle(cornerRadius: 26, style: .continuous))
        .shadow(color: Color(red: 0x1A / 255, green: 0x14 / 255, blue: 0x20 / 255).opacity(0.14), radius: 22, y: 14)
    }

    private var timeline: some View {
        HStack(spacing: 18) {
            timelineColumn(label: "abertura", value: window.startLabel)
            Rectangle().fill(.white.opacity(0.18)).frame(width: 1, height: 34)
            timelineColumn(label: "encerramento", value: window.endLabel)
        }
        .padding(.top, 14)
        .overlay(alignment: .top) {
            Rectangle().fill(.white.opacity(0.15)).frame(height: 1)
        }
    }

    private func timelineColumn(label: String, value: String) -> some View {
        VStack(alignment: .leading, spacing: 3) {
            Text(label.uppercased())
                .font(UNESFont.mono(9, weight: .medium))
                .tracking(1.08)
                .foregroundStyle(.white.opacity(0.6))
            Text(value)
                .font(UNESFont.sans(16, weight: .semibold))
                .tracking(-0.16)
                .foregroundStyle(.white)
        }
    }

    // MARK: Student strip

    private var studentStrip: some View {
        HStack(spacing: 12) {
            Text(student.avatarInitial)
                .font(UNESFont.serif(18))
                .foregroundStyle(.white)
                .frame(width: 40, height: 40)
                .background(
                    LinearGradient(
                        colors: [EnrollmentTone.teal.color, UNESColor.plum],
                        startPoint: .topLeading, endPoint: .bottomTrailing
                    ),
                    in: Circle()
                )
            VStack(alignment: .leading, spacing: 2) {
                Text(student.name)
                    .font(UNESFont.sans(14, weight: .semibold))
                    .foregroundStyle(UNESColor.ink)
                Text("\(student.course) · \(student.period)")
                    .font(UNESFont.mono(10))
                    .foregroundStyle(UNESColor.ink3)
            }
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .cardSurface(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    // MARK: Stat tiles

    private var statTiles: some View {
        let conflicts = enroll.conflicts.count
        let waitlisted = enroll.picks.filter { $0.waitlist }.count
        return HStack(spacing: 10) {
            StatTile(label: "disciplinas", value: enroll.picks.count,
                     hint: enroll.picks.count == 1 ? "turma" : "turmas")
            StatTile(label: "conflitos", value: conflicts,
                     hint: conflicts > 0 ? "resolver" : "tudo certo",
                     tone: conflicts > 0 ? EnrollmentPalette.danger : EnrollmentPalette.okSolid)
            StatTile(label: "fila", value: waitlisted, hint: "espera")
        }
    }

    // MARK: CTA

    @ViewBuilder
    private var cta: some View {
        if isUpcoming {
            Text("Você será avisado na abertura")
                .font(UNESFont.sans(16, weight: .medium))
                .foregroundStyle(UNESColor.ink3)
                .frame(maxWidth: .infinity)
                .frame(height: 54)
                .overlay(Capsule(style: .continuous).strokeBorder(UNESColor.line, lineWidth: 1.5))
        } else if isClosed {
            VStack(spacing: 10) {
                PrimaryButton(title: "Ver comprovante", showsArrow: false, action: onReview)
                GhostButton(title: "Reabrir matrícula", action: onStart)
            }
        } else {
            PrimaryButton(
                title: enroll.picks.isEmpty ? "Montar minha matrícula" : "Continuar montagem",
                action: onStart
            )
        }
    }

    private var footer: some View {
        Text("◦ carga mínima \(window.minHours)h · máxima \(window.maxHours)h ◦")
            .font(UNESFont.mono(9, weight: .medium))
            .tracking(1.08)
            .foregroundStyle(UNESColor.ink4)
            .padding(.top, 8)
    }
}

/// Small labelled tally tile used in the window-status summary.
private struct StatTile: View {
    let label: String
    let value: Int
    let hint: String
    var tone: Color = UNESColor.ink

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            EnrollmentEyebrow(text: label, size: 9)
            Text("\(value)")
                .font(UNESFont.sans(28, weight: .bold))
                .tracking(-0.98)
                .foregroundStyle(tone)
                .padding(.top, 6)
            Text(hint)
                .font(UNESFont.sans(10.5))
                .foregroundStyle(UNESColor.ink3)
                .padding(.top, 3)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 12)
        .padding(.vertical, 12)
        .cardSurface(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

#Preview("Aberta") {
    NavigationStack {
        WindowStatusView(
            enroll: .previewSeeded,
            window: EnrollmentFixtures.window,
            windowState: .open,
            student: EnrollmentFixtures.student,
            onStart: {}, onReview: {}
        )
    }
}

#Preview("Encerrada") {
    NavigationStack {
        WindowStatusView(
            enroll: .previewSeeded,
            window: EnrollmentFixtures.window,
            windowState: .closed,
            student: EnrollmentFixtures.student,
            onStart: {}, onReview: {}
        )
    }
}
