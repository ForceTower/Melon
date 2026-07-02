import SwiftUI

/// One notification group (Mensagens / Notas / Aulas): a titled card with a
/// live "n/3 ativas" counter and one switch per push kind.
struct SettingsNotificationGroupCard: View {
    var group: SettingsNotificationGroup
    var settings: UserSettings
    var onToggle: (NotificationToggle) -> Void

    var body: some View {
        VStack(spacing: 0) {
            header
            ForEach(Array(group.rows.enumerated()), id: \.element.toggle) { position, row in
                self.row(row)
                    .overlay(alignment: .bottom) {
                        if position < group.rows.count - 1 {
                            Rectangle()
                                .fill(UNESColor.line)
                                .frame(height: 0.5)
                        }
                    }
            }
        }
        .background(UNESColor.card)
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .strokeBorder(UNESColor.cardLine)
        }
        .shadow(color: Color(hex: 0x141020, opacity: 0.05), radius: 9, y: 6)
    }

    private var header: some View {
        let activeCount = group.rows.count { settings[keyPath: $0.toggle.keyPath] }
        return HStack {
            Text(group.title)
                .font(.system(size: 15, weight: .bold))
                .tracking(-0.3)
                .foregroundStyle(UNESColor.ink)
            Spacer()
            Text("\(activeCount)/\(group.rows.count) ativas")
                .font(.system(size: 11.5, weight: .semibold))
                .monospacedDigit()
                .foregroundStyle(UNESColor.ink4)
                .padding(.horizontal, 9)
                .padding(.vertical, 3)
                .background(UNESColor.surface2, in: Capsule())
        }
        .padding(EdgeInsets(top: 13, leading: 16, bottom: 11, trailing: 16))
        .overlay(alignment: .bottom) {
            Rectangle()
                .fill(UNESColor.line)
                .frame(height: 0.5)
        }
    }

    private func row(_ row: SettingsNotificationGroup.Row) -> some View {
        HStack(spacing: 13) {
            Image(systemName: row.icon)
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(.white)
                .frame(width: 30, height: 30)
                .background(row.tone, in: RoundedRectangle(cornerRadius: 8, style: .continuous))
                .shadow(color: row.tone.opacity(0.27), radius: 5, y: 4)

            VStack(alignment: .leading, spacing: 1) {
                Text(row.label)
                    .font(.system(size: 15, weight: .medium))
                    .tracking(-0.15)
                    .foregroundStyle(UNESColor.ink)
                Text(row.hint)
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(UNESColor.ink4)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Toggle(
                row.label,
                isOn: Binding(
                    get: { settings[keyPath: row.toggle.keyPath] },
                    set: { _ in onToggle(row.toggle) }
                )
            )
            .labelsHidden()
        }
        .padding(EdgeInsets(top: 11, leading: 14, bottom: 11, trailing: 14))
    }
}

/// The three groups, mirroring the wire fields on `PATCH api/me/settings`.
struct SettingsNotificationGroup: Identifiable {
    let id: String
    let title: String
    let rows: [Row]

    struct Row {
        let toggle: NotificationToggle
        let icon: String
        let tone: Color
        let label: String
        let hint: String
    }

    static let all: [SettingsNotificationGroup] = [
        SettingsNotificationGroup(id: "messages", title: "Mensagens", rows: [
            Row(toggle: .messageBroadcast, icon: "megaphone", tone: Tone.amber,
                label: "Broadcasts", hint: "Avisos da universidade"),
            Row(toggle: .messageClass, icon: "person.2", tone: Tone.teal,
                label: "Da turma", hint: "Mensagens enviadas à classe"),
            Row(toggle: .messageDirect, icon: "envelope", tone: Tone.plum,
                label: "Diretas", hint: "Professor ou secretaria"),
        ]),
        SettingsNotificationGroup(id: "grades", title: "Notas", rows: [
            Row(toggle: .gradePosted, icon: "sparkle", tone: Tone.coral,
                label: "Publicada", hint: "Uma nota nova apareceu"),
            Row(toggle: .gradeChanged, icon: "pencil", tone: Tone.magenta,
                label: "Alterada", hint: "O valor foi corrigido"),
            Row(toggle: .gradeDateChanged, icon: "calendar", tone: Tone.plum,
                label: "Data alterada", hint: "Prazo da avaliação mudou"),
        ]),
        SettingsNotificationGroup(id: "classes", title: "Aulas", rows: [
            Row(toggle: .classLocation, icon: "mappin.and.ellipse", tone: Tone.teal,
                label: "Sala alterada", hint: "Mudança de localização"),
            Row(toggle: .classMaterial, icon: "book", tone: Tone.amber,
                label: "Material publicado", hint: "Slides, enunciados, listas"),
            Row(toggle: .classSubject, icon: "tag", tone: Tone.coral,
                label: "Assunto da aula", hint: "O tópico previsto mudou"),
        ]),
    ]

    /// The design's icon-tile palette, lifted for dark-mode legibility.
    private enum Tone {
        static let plum = UNESColor.readable(0x7A5AD0)
        static let magenta = UNESColor.readable(0xB23A7A)
        static let teal = UNESColor.readable(0x2AA5B8)
        static let coral = UNESColor.readable(0xE85D4E)
        static let amber = UNESColor.readable(0xE8894E)
    }
}

#Preview {
    ScrollView {
        VStack(spacing: 12) {
            ForEach(SettingsNotificationGroup.all) { group in
                SettingsNotificationGroupCard(
                    group: group,
                    settings: UserSettings(),
                    onToggle: { _ in }
                )
            }
        }
        .padding(16)
    }
    .background(UNESColor.surface)
}
