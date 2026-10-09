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
| `unes://calendar` | Calendário, pushed on the Eu tab (personal-event reminders) |
| `unes://reauth` | Home / Hoje tab, which opens the portal-password sheet (credentials-invalid push) |
| `unes://messages/{messageId}` | Message detail, or the inbox when the message can't be found (below) |
| `unes://materials/{materialId}` | Material detail |
| `unes://materials/discipline/{disciplineId}` | Materials shelf of one discipline |

A message push can be tapped before the device has mirrored the message. Both
apps then refresh the inbox once and open the message only if it has arrived;
otherwise they open the inbox, never an endless loading screen. Android covers
this with `ConnectedViewModelDeepLinkTest` and the `notification.*` journeys in
[scenarios](scenarios.md).

## watchOS

The watch app consumes the same URIs via mirrored notification taps (parser
shared with iOS; table in `UNESKit/Sources/UNESKit/Watch/WatchDeeplinkResolver.swift`),
projected onto the watch's smaller destination set:

| URI | Watch destination |
| --- | --- |
| `unes://schedule` | Week view |
| `unes://classes` + push `data.disciplineCode` | That discipline's grades screen (grade pushes carry the code in push data, not the URL; no match → Hoje) |
| `unes://messages` | Inbox |
| `unes://messages/{messageId}` | Message detail when mirrored; otherwise inbox + a phone-wake refresh that upgrades to the detail if the message arrives in time |
| everything else | Hoje (plain open) |
