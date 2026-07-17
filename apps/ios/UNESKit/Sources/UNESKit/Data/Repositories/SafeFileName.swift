/// File names round-trip from whoever uploaded the file, so they are untrusted
/// input rather than a path: a directory component would let a download escape
/// the directory it is meant to land in.
func safeFileName(_ value: String, fallback: String = "arquivo") -> String {
    let name = value.split(separator: "/").last.map(String.init) ?? ""
    guard !name.isEmpty, name != ".", name != ".." else { return fallback }
    return name
}
