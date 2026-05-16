import SwiftUI

struct ReadyView: View {
    let userName: String
    let onEnter: () -> Void
    let viewModel: ReadyViewModel

    var body: some View {
        ZStack(alignment: .top) {
            UNESColor.surface.ignoresSafeArea()

            // hero mesh — top-half fully visible, fades to surface over the
            // bottom half (matches the web `linear-gradient(180deg,
            // transparent 50%, var(--surface) 100%)`).
            ZStack {
                MeshGradientView(variant: .fresh, intensity: 0.9)
                LinearGradient(
                    stops: [
                        .init(color: .clear, location: 0.5),
                        .init(color: UNESColor.surface, location: 1.0)
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
            }
            .frame(maxWidth: .infinity)
            .frame(height: 420)
            .ignoresSafeArea(edges: .top)
            .allowsHitTesting(false)

            VStack(spacing: 0) {
                Spacer().frame(height: 73)

                checkBadge
                    .scaleInOnAppear(delay: 0.1)

                Text("◦ conectado")
                    .font(UNESFont.sans(12, weight: .medium))
                    .tracking(1.4)
                    .textCase(.uppercase)
                    .foregroundStyle(Color.white.opacity(0.7))
                    .padding(.top, 20)
                    .fadeUpOnAppear(delay: 1.1)

                Spacer().frame(height: 60)

                titleText
                    .font(UNESFont.serif(44))
                    .tracking(-1.1)
                    .multilineTextAlignment(.center)
                    .fadeUpOnAppear(delay: 1.2)

                Text(viewModel.semesterLine)
                    .font(UNESFont.sans(15))
                    .lineSpacing(3)
                    .foregroundStyle(UNESColor.ink3)
                    .padding(.top, 10)
                    .fadeUpOnAppear(delay: 1.3)

                Spacer()

                if let next = viewModel.nextClass {
                    previewCard(for: next)
                        .padding(.horizontal, 28)
                        .fadeUpOnAppear(delay: 1.4)
                }

                Spacer()

                PrimaryButton(title: "Ver meu semestre", action: onEnter)
                    .padding(.horizontal, 28)
                    .fadeUpOnAppear(delay: 1.5)
                    .padding(.bottom, 6)
            }
        }
        .task { await viewModel.load() }
    }

    @ViewBuilder
    private var checkBadge: some View {
        ZStack {
            Circle()
                .fill(Color.white.opacity(0.15))
                .background(.ultraThinMaterial, in: Circle())
                .overlay(Circle().stroke(Color.white.opacity(0.25), lineWidth: 1))
                .frame(width: 96, height: 96)

            DrawingCheckmark(size: 54, strokeColor: UNESColor.surfaceLight, drawCircle: true)
        }
    }

    private var titleText: Text {
        Text("\(Text("Prontinho,\n").foregroundStyle(UNESColor.ink))\(Text("\(userName).").italic().foregroundStyle(UNESColor.accent))")
    }

    @ViewBuilder
    private func previewCard(for next: ReadyNextClass) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("Próxima aula")
                    .font(UNESFont.mono(10))
                    .tracking(1.5)
                    .textCase(.uppercase)
                    .foregroundStyle(UNESColor.ink3)
                Spacer()
                Text(next.startsInLabel)
                    .font(UNESFont.mono(11))
                    .foregroundStyle(UNESColor.accent)
            }

            HStack(spacing: 12) {
                MeshChip(variant: .cool, size: 48, radius: 14)

                VStack(alignment: .leading, spacing: 2) {
                    Text(next.disciplineName)
                        .font(UNESFont.serif(20))
                        .tracking(-0.2)
                        .foregroundStyle(UNESColor.ink)
                    Text(secondaryLine(for: next))
                        .font(UNESFont.sans(13))
                        .foregroundStyle(UNESColor.ink3)
                }
                Spacer()
            }
        }
        .padding(18)
        .cardSurface(RoundedRectangle(cornerRadius: 22, style: .continuous))
        .shadow(color: Color.black.opacity(0.06), radius: 20, y: 10)
    }

    private func secondaryLine(for next: ReadyNextClass) -> String {
        var parts: [String] = [next.startTime]
        if let room = next.spaceLocation, !room.isEmpty {
            parts.append("sala \(room)")
        }
        if let teacher = next.teacherName, !teacher.isEmpty {
            parts.append(shortTeacherName(teacher))
        }
        return parts.joined(separator: " · ")
    }

    // "Prof. Adriana" from a full name like "Adriana Silva Souza".
    private func shortTeacherName(_ full: String) -> String {
        let first = full.split(separator: " ").first.map(String.init) ?? full
        return "Prof. \(first)"
    }
}

#Preview {
    ReadyView(userName: "Mariana", onEnter: {}, viewModel: ReadyViewModel(useCase: nil))
}
