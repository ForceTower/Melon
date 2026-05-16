import Foundation

// KMP emits ISO "yyyy-MM-dd"; the Disciplines UI reads "dd/MM/yyyy" strings
// (consumed e.g. by `DisciplineDate.daysUntil`). Centralized here so both
// view models — and any future consumer — hit the same conversion.
enum DisciplineDateFormatting {
    static func ddMmYyyy(iso: String?) -> String? {
        guard let iso, iso.count >= 10 else { return nil }
        let year = iso.prefix(4)
        let month = iso.dropFirst(5).prefix(2)
        let day = iso.dropFirst(8).prefix(2)
        return "\(day)/\(month)/\(year)"
    }

    // Shorter "dd/MM" for Attachment.added — matches the prototype's "24/03".
    static func ddMm(iso: String?) -> String? {
        guard let iso, iso.count >= 10 else { return nil }
        let month = iso.dropFirst(5).prefix(2)
        let day = iso.dropFirst(8).prefix(2)
        return "\(day)/\(month)"
    }
}
