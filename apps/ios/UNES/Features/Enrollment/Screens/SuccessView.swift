import SwiftUI

// UNES — submission confirmation. A full-bleed mesh celebration with the
// protocol, a recap of the registered load, and the per-pick tallies. Ported
// from `SuccessScreen` in `screens-matricula-review.jsx`.
struct SuccessView: View {
    let enroll: EnrollmentState
    let window: EnrollmentWindow
    let onDone: () -> Void

    private let darkBg = Color(red: 0x0F / 255, green: 0x1A / 255, blue: 0x14 / 255)
    private let cream = Color(red: 0xF2 / 255, green: 0xEF / 255, blue: 0xE8 / 255)

    private var waitCount: Int { enroll.picks.filter(\.waitlist).count }
    private var allowCount: Int { enroll.picks.filter(\.allowsOther).count }

    var body: some View {
        ZStack {
            darkBg.ignoresSafeArea()
            MeshGradientView(variant: .fresh, intensity: 0.85).opacity(0.9).ignoresSafeArea()
            LinearGradient(
                colors: [darkBg.opacity(0.2), darkBg.opacity(0.75)],
                startPoint: .top, endPoint: .bottom
            )
            .ignoresSafeArea()

            VStack(spacing: 0) {
                checkBadge
                    .padding(.bottom, 26)
                Text("protocolo #48201 · \(window.semester)".uppercased())
                    .font(UNESFont.mono(10, weight: .medium))
                    .tracking(1.8)
                    .foregroundStyle(cream.opacity(0.7))
                    .padding(.bottom, 12)
                Text("Proposta enviada")
                    .font(UNESFont.serif(40))
                    .tracking(-0.8)
                    .foregroundStyle(cream)
                    .padding(.bottom, 12)
                recapStyled
                    .frame(maxWidth: 300)
                    .padding(.bottom, 28)
                stats
                    .frame(maxWidth: 320)
                    .padding(.bottom, 30)
                doneButton
                    .frame(maxWidth: 320)
            }
            .multilineTextAlignment(.center)
            .padding(.horizontal, 26)
        }
        .toolbar(.hidden, for: .navigationBar)
    }

    private var checkBadge: some View {
        ZStack {
            Circle()
                .fill(.white.opacity(0.14))
                .overlay(Circle().strokeBorder(.white.opacity(0.25), lineWidth: 1))
            DrawingCheckmark(size: 42, strokeColor: .white, drawCircle: false)
        }
        .frame(width: 88, height: 88)
        .scaleInOnAppear(delay: 0.1)
    }

    private var recap: Text {
        Text("Sua matrícula de ")
            + Text("\(enroll.totalHours)h").foregroundColor(cream).bold()
            + Text(" em ")
            + Text("\(enroll.picks.count) disciplinas").foregroundColor(cream).bold()
            + Text(" foi registrada. Você receberá a confirmação das vagas por mensagem.")
    }

    private var recapStyled: some View {
        recap
            .font(UNESFont.sans(14))
            .foregroundStyle(cream.opacity(0.82))
            .lineSpacing(3)
    }

    private var stats: some View {
        HStack(spacing: 10) {
            SuccessStat(value: enroll.picks.count, label: "disciplinas")
            SuccessStat(value: waitCount, label: "em fila")
            SuccessStat(value: allowCount, label: "aceitam troca")
        }
    }

    private var doneButton: some View {
        Button(action: onDone) {
            Text("Concluir")
                .font(UNESFont.sans(15, weight: .semibold))
                .foregroundStyle(darkBg)
                .frame(maxWidth: .infinity)
                .frame(height: 52)
                .background(cream, in: Capsule(style: .continuous))
        }
        .buttonStyle(PressScaleStyle())
    }
}

/// One translucent tally tile on the success screen.
private struct SuccessStat: View {
    let value: Int
    let label: String

    var body: some View {
        VStack(spacing: 5) {
            Text("\(value)")
                .font(UNESFont.sans(24, weight: .bold))
                .tracking(-0.72)
                .foregroundStyle(.white)
            Text(label.uppercased())
                .font(UNESFont.mono(8.5))
                .tracking(0.68)
                .foregroundStyle(.white.opacity(0.7))
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
        .padding(.horizontal, 8)
        .background(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .fill(.white.opacity(0.10))
                .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous).strokeBorder(.white.opacity(0.16), lineWidth: 1))
        )
    }
}

#Preview {
    SuccessView(enroll: .previewSeeded, window: EnrollmentFixtures.window, onDone: {})
}
