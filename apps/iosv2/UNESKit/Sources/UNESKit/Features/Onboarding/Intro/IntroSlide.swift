import SwiftUI

/// One value-prop slide of the intro carousel.
struct IntroSlide: Identifiable {
    struct Segment {
        var text: String
        var accented = false
    }

    enum Illustration {
        case schedule, grades, messages, notifications
    }

    let id: Int
    let variant: MeshView.Variant
    let eyebrow: String
    let accent: Color
    /// Title lines; accented segments render in the slide accent color.
    let titleLines: [[Segment]]
    let body: String
    let illustration: Illustration

    static let all: [IntroSlide] = [
        IntroSlide(
            id: 0,
            variant: .cool,
            eyebrow: "Horário",
            accent: UNESColor.teal,
            titleLines: [
                [Segment(text: "Sua semana,")],
                [Segment(text: "organizada.", accented: true)],
            ],
            body: "A grade da UEFS puxada direto do SAGRES. Aulas canceladas, salas trocadas e provas em tempo real.",
            illustration: .schedule
        ),
        IntroSlide(
            id: 1,
            variant: .sun,
            eyebrow: "Notas",
            accent: UNESColor.tangerine,
            titleLines: [
                [Segment(text: "Acompanhe seu")],
                [Segment(text: "desempenho.", accented: true)],
            ],
            body: "Notas parciais, coeficiente e histórico. Sem precisar entrar no SAGRES pelo navegador toda semana.",
            illustration: .grades
        ),
        IntroSlide(
            id: 2,
            variant: .rose,
            eyebrow: "Recados",
            accent: UNESColor.magenta,
            titleLines: [
                [Segment(text: "Tudo o que você")],
                [Segment(text: "precisa saber.", accented: true)],
            ],
            body: "Recados de professores, coordenação e DCE. Sem perder prazos nem assembleias importantes.",
            illustration: .messages
        ),
        IntroSlide(
            id: 3,
            variant: .warm,
            eyebrow: "Notificações",
            accent: UNESColor.accent,
            titleLines: [
                [Segment(text: "Avisa no instante")],
                [Segment(text: "que "), Segment(text: "acontece.", accented: true)],
            ],
            body: "Nota nova, recado de professor, material publicado, sala trocada. Um toque no bolso antes de você abrir o app.",
            illustration: .notifications
        ),
    ]
}
