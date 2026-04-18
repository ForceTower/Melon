import SwiftUI

struct HomeNowClass {
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

enum HomeClassState {
    case done, now, next, later
}

struct HomeTodayItem: Identifiable {
    let id = UUID()
    let time: String
    let code: String
    let title: String
    let room: String
    let color: Color
    let state: HomeClassState
    let topic: String?
}

enum HomeTabKey: String, CaseIterable {
    case home, schedule, classes, messages, me

    var label: String {
        switch self {
        case .home:     return "Hoje"
        case .schedule: return "Horário"
        case .classes:  return "Turmas"
        case .messages: return "Recados"
        case .me:       return "Eu"
        }
    }

    /// SF Symbols mapping (closest to the custom SVGs in the design).
    var icon: String {
        switch self {
        case .home:     return "house"
        case .schedule: return "square.grid.2x2"
        case .classes:  return "square.stack.3d.up"
        case .messages: return "bubble.left"
        case .me:       return "person"
        }
    }

    var badge: Int? {
        switch self {
        case .messages: return 2
        default:        return nil
        }
    }
}

struct HomeDiscipline: Identifiable {
    let id = UUID()
    let code: String
    let title: String
    let grade: String
    let color: Color
}

enum HomeFixtures {
    // Match design hex values that aren't tokens in UNESColor.
    static let teal    = Color(red: 0x3B / 255, green: 0x9E / 255, blue: 0xAE / 255)
    static let success = Color(red: 0x2F / 255, green: 0x6B / 255, blue: 0x48 / 255)
    static let successIcon = Color(red: 0x4A / 255, green: 0xA6 / 255, blue: 0x79 / 255)

    static let nowClass = HomeNowClass(
        code: "CALC II",
        title: "Cálculo Diferencial II",
        prof: "Prof. Adriana Matos",
        room: "MT-14",
        startsIn: 72,
        time: "10:20 – 12:00",
        topic: "Integrais por partes — continuação do exercício 4.2",
        color: teal,
        meshVariant: .cool
    )

    static let today: [HomeTodayItem] = [
        .init(time: "08:00", code: "ALGI", title: "Algoritmos I",
              room: "LC-03", color: UNESColor.coral, state: .done,  topic: nil),
        .init(time: "10:20", code: "CALC", title: "Cálculo II",
              room: "MT-14", color: teal,            state: .now,   topic: "Integrais por partes"),
        .init(time: "14:00", code: "LPOO", title: "Prog. Orientada a Obj.",
              room: "LC-01", color: UNESColor.magenta, state: .next, topic: "Herança vs composição"),
        .init(time: "16:20", code: "FIS2", title: "Física II",
              room: "PV-22", color: UNESColor.plum,   state: .later, topic: nil),
    ]

    static let disciplines: [HomeDiscipline] = [
        .init(code: "ALGI", title: "Algoritmos I",        grade: "8,8", color: UNESColor.coral),
        .init(code: "CALC", title: "Cálculo II",          grade: "7,5", color: teal),
        .init(code: "LPOO", title: "POO",                 grade: "9,4", color: UNESColor.magenta),
        .init(code: "FIS2", title: "Física II",           grade: "—",   color: UNESColor.plum),
        .init(code: "PROJ", title: "Projeto de Software", grade: "8,1", color: UNESColor.amber),
    ]
}
