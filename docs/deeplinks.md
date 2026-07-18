# `unes://` deeplinks

Registry of every deeplink the apps understand. URIs are composed by the
backend (push payloads carry one in the FCM `data.url` key) and parsed by one
parser per platform: Android `ui/feature/connected/DeepLinks.kt`, iOS
`UNESKit/Sources/UNESKit/Intents/Deeplinks.swift` — each with a mirrored
table test.

Scheme and host are case-insensitive, ids are backend UUIDs, query params and
fragments are ignored, and anything unparseable degrades to a plain app open.
Evolve by addition only — never repurpose an existing URI.

| URI | Destination |
| --- | --- |
| `unes://home` | Home / Hoje tab |
| `unes://schedule` | Horário tab |
| `unes://classes` | Disciplinas / Turmas tab |
| `unes://messages` | Mensagens tab (inbox) |
| `unes://me` | Eu tab |
| `unes://messages/{messageId}` | Message detail |
| `unes://materials/{materialId}` | Material detail |
| `unes://materials/discipline/{disciplineId}` | Materials shelf of one discipline |
