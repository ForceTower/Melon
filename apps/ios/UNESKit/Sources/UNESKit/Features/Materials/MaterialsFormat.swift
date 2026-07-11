import Foundation

enum MaterialsFormat {
    /// Vote / download counts — "128", "1,2 mil" (pt-BR) / "1.2K" (en).
    static func count(_ value: Int, locale: Locale = .autoupdatingCurrent) -> String {
        guard value >= 1000 else { return String(value) }
        return Double(value).formatted(
            .number.notation(.compactName).precision(.fractionLength(0...1)).locale(locale)
        )
    }

    /// File size label — "620 KB", "1,8 MB".
    static func byteCount(_ value: Int, locale: Locale = .autoupdatingCurrent) -> String {
        var style = ByteCountFormatStyle(style: .file)
        style.locale = locale
        return Int64(value).formatted(style)
    }

    /// A stable palette index for a discipline reached outside the hub list
    /// (whose entries are colored by position): same code, same color, on
    /// every launch.
    static func stableColorIndex(for code: String) -> Int {
        code.unicodeScalars.reduce(0) { ($0 &+ Int($1.value)) % 997 }
    }

    /// The five semesters an upload can be tagged with, newest first,
    /// starting at the current one — "2025.2" → ["2025.2", "2025.1", …].
    static func uploadSemesters(from current: String) -> [String] {
        let parts = current.split(separator: ".")
        guard parts.count == 2, var year = Int(parts[0]), var term = Int(parts[1]), (1...2).contains(term) else {
            return [current]
        }
        var semesters: [String] = []
        for _ in 0..<5 {
            semesters.append("\(year).\(term)")
            if term == 1 {
                year -= 1
                term = 2
            } else {
                term = 1
            }
        }
        return semesters
    }
}
