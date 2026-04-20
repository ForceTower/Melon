import SwiftUI

struct OverviewNowClass {
    let code: String
    let title: String
    let prof: String
    let room: String
    let startsIn: Int // minutes
    let time: String
    let topic: String?
    let color: Color
    let meshVariant: MeshVariant
}

enum OverviewClassState {
    case done, now, next, later
}

struct OverviewTodayItem: Identifiable {
    let id = UUID()
    let time: String
    let code: String
    let title: String
    let room: String
    let color: Color
    let state: OverviewClassState
    let topic: String?
}

struct OverviewDiscipline: Identifiable {
    let id = UUID()
    let code: String
    let title: String
    let grade: String
    let color: Color
    var statusLabel: String = "PARCIAL"
}

enum OverviewFixtures {
    static let success = Color(red: 0x2F / 255, green: 0x6B / 255, blue: 0x48 / 255)
    static let successIcon = Color(red: 0x4A / 255, green: 0xA6 / 255, blue: 0x79 / 255)

    static let nowClass = OverviewNowClass(
        code: "CALC II",
        title: "Cálculo Diferencial II",
        prof: "Prof. Adriana Matos",
        room: "MT-14",
        startsIn: 72,
        time: "10:20 – 12:00",
        topic: "Integrais por partes — continuação do exercício 4.2",
        color: ColorFor.teal,
        meshVariant: .cool
    )

    static let today: [OverviewTodayItem] = [
        .init(time: "08:00", code: "ALGI", title: "Algoritmos I",
              room: "LC-03", color: ColorFor.coral,   state: .done,  topic: nil),
        .init(time: "10:20", code: "CALC", title: "Cálculo II",
              room: "MT-14", color: ColorFor.teal,    state: .now,   topic: "Integrais por partes"),
        .init(time: "14:00", code: "LPOO", title: "Prog. Orientada a Obj.",
              room: "LC-01", color: ColorFor.magenta, state: .next,  topic: "Herança vs composição"),
        .init(time: "16:20", code: "FIS2", title: "Física II",
              room: "PV-22", color: ColorFor.plum,    state: .later, topic: nil),
    ]

    static let disciplines: [OverviewDiscipline] = [
        .init(code: "ALGI", title: "Algoritmos I",        grade: "8,8", color: ColorFor.coral),
        .init(code: "CALC", title: "Cálculo II",          grade: "7,5", color: ColorFor.teal),
        .init(code: "LPOO", title: "POO",                 grade: "9,4", color: ColorFor.magenta),
        .init(code: "FIS2", title: "Física II",           grade: "—",   color: ColorFor.plum),
        .init(code: "PROJ", title: "Projeto de Software", grade: "8,1", color: ColorFor.amber),
    ]
}
