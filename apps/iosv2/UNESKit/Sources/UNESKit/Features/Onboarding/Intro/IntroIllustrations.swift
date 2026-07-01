import SwiftUI

// The four 260×260 line-art illustrations of the intro carousel.

// MARK: - Schedule (animated timetable)

struct ScheduleIllustration: View {
    private struct Block {
        let col: Int
        let row: Int
        let height: Int
        let color: Color
        let label: String
        let room: Int
    }

    private let blocks = [
        Block(col: 0, row: 0, height: 2, color: UNESColor.coral, label: "ALGI", room: 112),
        Block(col: 1, row: 1, height: 1, color: UNESColor.amber, label: "CALC", room: 108),
        Block(col: 2, row: 0, height: 1, color: UNESColor.magenta, label: "LPOO", room: 121),
        Block(col: 2, row: 2, height: 2, color: UNESColor.plum, label: "FIS2", room: 137),
        Block(col: 0, row: 3, height: 1, color: UNESColor.amber, label: "PROJ", room: 104),
    ]

    var body: some View {
        ZStack(alignment: .topLeading) {
            grid

            ForEach(["SEG", "TER", "QUA"].indices, id: \.self) { index in
                Text(["SEG", "TER", "QUA"][index])
                    .font(.system(size: 9, weight: .medium))
                    .tracking(1)
                    .foregroundStyle(UNESColor.ink3)
                    .position(x: 20 + CGFloat(index) * 73 + 36, y: 17)
            }

            ForEach(["08", "10", "12", "14", "16"].indices, id: \.self) { index in
                Text(["08", "10", "12", "14", "16"][index])
                    .font(.system(size: 9))
                    .foregroundStyle(UNESColor.ink4)
                    .position(x: 4, y: 32 + CGFloat(index) * 40)
            }

            ForEach(blocks.indices, id: \.self) { index in
                blockView(blocks[index])
                    .offset(
                        x: 21 + CGFloat(blocks[index].col) * 73,
                        y: 31 + CGFloat(blocks[index].row) * 40
                    )
                    .popIn(
                        delay: 0.1 + Double(index) * 0.12,
                        duration: 0.5,
                        from: 1,
                        offsetY: 12,
                        overshoot: 1.2
                    )
            }

            NowLine()
        }
        .frame(width: 260, height: 260)
    }

    private var grid: some View {
        Path { path in
            for index in 0...5 {
                path.move(to: CGPoint(x: 20, y: 30 + index * 40))
                path.addLine(to: CGPoint(x: 240, y: 30 + index * 40))
            }
            for index in 0...3 {
                path.move(to: CGPoint(x: 20 + index * 73, y: 30))
                path.addLine(to: CGPoint(x: 20 + index * 73, y: 230))
            }
        }
        .stroke(UNESColor.ink.opacity(0.1), lineWidth: 1)
    }

    private func blockView(_ block: Block) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(block.label)
                .font(.system(size: 9, weight: .semibold))
                .tracking(0.45)
            Text("sala \(block.room)")
                .font(.system(size: 7))
                .opacity(0.8)
        }
        .foregroundStyle(UNESColor.paper)
        .padding(8)
        .frame(width: 71, height: CGFloat(block.height) * 40 - 2, alignment: .topLeading)
        .background(block.color)
        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
        .shadow(color: .black.opacity(0.08), radius: 5, y: 4)
    }

    /// Dashed "now" marker marching across the grid with a pulsing origin dot.
    private struct NowLine: View {
        @State private var dashPhase: CGFloat = 0
        @State private var pulsing = false
        @Environment(\.accessibilityReduceMotion) private var reduceMotion

        var body: some View {
            ZStack(alignment: .topLeading) {
                Path { path in
                    path.move(to: CGPoint(x: 20, y: 110))
                    path.addLine(to: CGPoint(x: 240, y: 110))
                }
                .stroke(UNESColor.coral, style: StrokeStyle(lineWidth: 1.5, dash: [3, 2], dashPhase: dashPhase))

                Circle()
                    .fill(UNESColor.coral)
                    .frame(width: 6, height: 6)
                    .scaleEffect(pulsing ? 5 / 3 : 1)
                    .position(x: 20, y: 110)
            }
            .onAppear {
                guard !reduceMotion else { return }
                withAnimation(.linear(duration: 1).repeatForever(autoreverses: false)) {
                    dashPhase = -10
                }
                withAnimation(.easeInOut(duration: 0.8).repeatForever(autoreverses: true)) {
                    pulsing = true
                }
            }
        }
    }
}

// MARK: - Grades (rising bars + headline number)

struct GradesIllustration: View {
    private struct Bar {
        let height: CGFloat
        let color: Color
        let label: String
    }

    private let bars = [
        Bar(height: 55, color: UNESColor.amber, label: "7.5"),
        Bar(height: 72, color: UNESColor.coral, label: "8.8"),
        Bar(height: 88, color: UNESColor.magenta, label: "9.4"),
        Bar(height: 65, color: UNESColor.amber, label: "8.1"),
        Bar(height: 80, color: UNESColor.coral, label: "9.0"),
    ]

    var body: some View {
        VStack(spacing: 0) {
            headline
                .padding(.top, 16)
                .fadeUp(delay: 0.1, duration: 0.6)

            Text("coeficiente · 2026.1")
                .textCase(.uppercase)
                .font(.system(size: 10))
                .tracking(2)
                .foregroundStyle(UNESColor.ink3)
                .padding(.top, 4)
                .fadeIn(delay: 0.3, duration: 0.8)

            HStack(alignment: .bottom, spacing: 10) {
                ForEach(bars.indices, id: \.self) { index in
                    barColumn(bars[index], index: index)
                }
            }
            .frame(height: 100, alignment: .bottom)
            .padding(.top, 28)
        }
        .frame(width: 260, height: 260, alignment: .top)
    }

    private var headline: some View {
        (
            Text("8").foregroundStyle(UNESColor.ink)
                + Text(",").foregroundStyle(UNESColor.accent)
                + Text("5").foregroundStyle(UNESColor.ink)
                + Text("/10").font(.system(size: 28, weight: .bold)).italic().foregroundStyle(UNESColor.ink3)
        )
        .font(.system(size: 92, weight: .bold))
        .tracking(-3.68)
    }

    private func barColumn(_ bar: Bar, index: Int) -> some View {
        VStack(spacing: 6) {
            Text(bar.label)
                .font(.system(size: 9))
                .foregroundStyle(UNESColor.ink3)
                .fadeIn(delay: 0.6 + Double(index) * 0.1, duration: 0.4)

            GrowingBar(color: bar.color, height: bar.height, delay: 0.3 + Double(index) * 0.08)
        }
    }

    private struct GrowingBar: View {
        let color: Color
        let height: CGFloat
        let delay: Double

        @State private var grown = false
        @Environment(\.accessibilityReduceMotion) private var reduceMotion

        var body: some View {
            UnevenRoundedRectangle(
                topLeadingRadius: 6,
                bottomLeadingRadius: 2,
                bottomTrailingRadius: 2,
                topTrailingRadius: 6
            )
            .fill(color)
            .frame(width: 28, height: grown ? height : 0)
            .onAppear {
                guard !grown else { return }
                if reduceMotion {
                    grown = true
                } else {
                    withAnimation(UNESMotion.ease(0.7, overshoot: 1.2).delay(delay)) {
                        grown = true
                    }
                }
            }
        }
    }
}

// MARK: - Messages (tilted cards sliding in)

struct MessagesIllustration: View {
    private struct Message {
        let from: String
        let preview: String
        let color: Color
        let time: String
    }

    private let messages = [
        Message(from: "Prof. Adriana", preview: "Gabarito da P1 liberado", color: UNESColor.magenta, time: "ag."),
        Message(from: "Coordenação CC", preview: "Matrícula em optativas", color: UNESColor.coral, time: "09:14"),
        Message(from: "DCE UEFS", preview: "Assembleia geral quinta…", color: UNESColor.amber, time: "ont."),
    ]

    var body: some View {
        VStack(spacing: 10) {
            ForEach(messages.indices, id: \.self) { index in
                TiltingCard(angle: index.isMultiple(of: 2) ? -1.5 : 1.5, delay: 0.1 + Double(index) * 0.15) {
                    card(messages[index], unread: index == 0)
                }
            }
            Spacer(minLength: 0)
        }
        .padding(.top, 16)
        .frame(width: 260, height: 260)
    }

    private func card(_ message: Message, unread: Bool) -> some View {
        HStack(spacing: 12) {
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(message.color)
                .frame(width: 40, height: 40)
                .overlay {
                    Text(String(message.from.prefix(1)))
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(UNESColor.paper)
                }

            VStack(alignment: .leading, spacing: 2) {
                HStack(alignment: .firstTextBaseline) {
                    Text(message.from)
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(UNESColor.ink)
                    Spacer(minLength: 4)
                    Text(message.time)
                        .font(.system(size: 10))
                        .foregroundStyle(UNESColor.ink3)
                }
                Text(message.preview)
                    .font(.system(size: 13))
                    .foregroundStyle(UNESColor.ink3)
                    .lineLimit(1)
            }

            if unread {
                UnreadDot()
            }
        }
        .padding(.vertical, 12)
        .padding(.horizontal, 14)
        .background(UNESColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .strokeBorder(UNESColor.line)
        }
        .shadow(color: Color(hex: 0x1A1420, opacity: 0.05), radius: 8, y: 4)
    }

    private struct UnreadDot: View {
        @State private var pulsing = false

        var body: some View {
            Circle()
                .fill(UNESColor.accent)
                .frame(width: 8, height: 8)
                .opacity(pulsing ? 0.6 : 1)
                .scaleEffect(pulsing ? 0.95 : 1)
                .onAppear {
                    withAnimation(.easeInOut(duration: 1).repeatForever(autoreverses: true)) {
                        pulsing = true
                    }
                }
        }
    }

    /// Slides up while settling into its resting tilt.
    private struct TiltingCard<Content: View>: View {
        let angle: Double
        let delay: Double
        @ViewBuilder let content: Content

        @State private var shown = false
        @Environment(\.accessibilityReduceMotion) private var reduceMotion

        var body: some View {
            content
                .rotationEffect(.degrees(shown ? angle : 0))
                .opacity(shown ? 1 : 0)
                .offset(y: shown ? 0 : 16)
                .onAppear {
                    guard !shown else { return }
                    if reduceMotion {
                        shown = true
                    } else {
                        withAnimation(UNESMotion.ease(0.6, overshoot: 1.2).delay(delay)) {
                            shown = true
                        }
                    }
                }
        }
    }
}

// MARK: - Notifications (lockscreen push stack)

struct NotificationsIllustration: View {
    private struct Push {
        let app: String
        let title: String
        let body: String
        let chip: Color
        let glyph: Glyph

        enum Glyph {
            case grade, message, clock, material
        }
    }

    private let pushes = [
        Push(
            app: "Nota publicada",
            title: "CÁLCULO II · P2",
            body: "8,7 lançado por Prof. Ribamar",
            chip: UNESColor.amber,
            glyph: .grade
        ),
        Push(
            app: "Novo recado",
            title: "Prof. Adriana Souza",
            body: "Gabarito da P1 está disponível no mural.",
            chip: UNESColor.magenta,
            glyph: .message
        ),
        Push(
            app: "Mudança de horário",
            title: "ALGI II · quinta",
            body: "Remanejada: sala 204 → sala 312",
            chip: UNESColor.coral,
            glyph: .clock
        ),
        Push(
            app: "Material novo",
            title: "FÍSICA II",
            body: "Lista de exercícios · cap. 7",
            chip: UNESColor.plum,
            glyph: .material
        ),
    ]

    var body: some View {
        VStack(spacing: 0) {
            clock
                .fadeIn(delay: 0.05, duration: 0.5)

            VStack(spacing: 7) {
                ForEach(pushes.indices, id: \.self) { index in
                    pushCard(pushes[index])
                        .popIn(
                            delay: 0.15 + Double(index) * 0.11,
                            duration: 0.55,
                            from: 1,
                            offsetY: 18,
                            overshoot: 1.2
                        )
                }
            }
            .padding(.top, 14)

            Spacer(minLength: 0)
        }
        .padding(.top, 4)
        .frame(width: 260, height: 260)
    }

    private var clock: some View {
        VStack(spacing: 3) {
            (
                Text("9")
                    + Text(":").foregroundStyle(UNESColor.ink.opacity(0.5))
                    + Text("14")
            )
            .font(.system(size: 22, weight: .bold))
            .tracking(-0.44)
            .foregroundStyle(UNESColor.ink)

            Text("qui · 23 abr")
                .textCase(.uppercase)
                .font(.system(size: 8))
                .tracking(1.6)
                .foregroundStyle(UNESColor.ink3)
        }
    }

    private func pushCard(_ push: Push) -> some View {
        HStack(spacing: 10) {
            RoundedRectangle(cornerRadius: 7, style: .continuous)
                .fill(push.chip)
                .frame(width: 28, height: 28)
                .overlay { glyph(push.glyph) }

            VStack(alignment: .leading, spacing: 1) {
                HStack(alignment: .firstTextBaseline, spacing: 6) {
                    Text(push.app)
                        .textCase(.uppercase)
                        .tracking(1.19)
                        .foregroundStyle(UNESColor.ink3)
                    Spacer(minLength: 0)
                    Text("agora")
                        .foregroundStyle(UNESColor.ink4)
                }
                .font(.system(size: 8.5))

                Text(push.title)
                    .font(.system(size: 12.5, weight: .semibold))
                    .foregroundStyle(UNESColor.ink)
                    .lineLimit(1)

                Text(push.body)
                    .font(.system(size: 11.5))
                    .foregroundStyle(UNESColor.ink3)
                    .lineLimit(1)
            }
        }
        .padding(.vertical, 9)
        .padding(.horizontal, 11)
        .background(UNESColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .strokeBorder(UNESColor.line)
        }
        .shadow(color: Color(hex: 0x1A1420, opacity: 0.08), radius: 9, y: 6)
    }

    @ViewBuilder
    private func glyph(_ glyph: Push.Glyph) -> some View {
        switch glyph {
        case .grade:
            Text("9")
                .font(.system(size: 14, weight: .semibold))
                .italic()
                .foregroundStyle(UNESColor.paper)
        case .message:
            Image(systemName: "bubble.left")
                .font(.system(size: 12, weight: .medium))
                .foregroundStyle(UNESColor.paper)
        case .clock:
            Image(systemName: "clock")
                .font(.system(size: 12, weight: .medium))
                .foregroundStyle(UNESColor.paper)
        case .material:
            Image(systemName: "doc")
                .font(.system(size: 12, weight: .medium))
                .foregroundStyle(UNESColor.paper)
        }
    }
}

#Preview("Schedule") {
    ScheduleIllustration()
}

#Preview("Grades") {
    GradesIllustration()
}

#Preview("Messages") {
    MessagesIllustration()
}

#Preview("Notifications") {
    NotificationsIllustration()
}
