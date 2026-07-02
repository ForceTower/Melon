import Foundation

/// The catalog rendered as a minimal CycloneDX 1.5 document — what the
/// "Baixar SBOM" row shares. Deterministic (no timestamp, sorted keys) so
/// the same build always produces the same file.
enum LicensesSBOM {
    static let data: Data = {
        let document = Document(components: LicenseCatalog.packages.map { package in
            Component(
                name: package.name,
                version: package.version,
                supplier: Supplier(name: package.author),
                licenses: [LicenseRef(license: License(id: package.family.rawValue))],
                externalReferences: [Reference(url: "https://\(package.homepage)")]
            )
        })
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        // The document is static and encodes only strings — this cannot throw.
        return (try? encoder.encode(document)) ?? Data()
    }()

    /// "CycloneDX · JSON · 4 KB" — the share row subtitle.
    static let sizeLabel = "CycloneDX · JSON · \(max(1, data.count / 1024)) KB"

    private struct Document: Encodable {
        let bomFormat = "CycloneDX"
        let specVersion = "1.5"
        let version = 1
        let components: [Component]
    }

    private struct Component: Encodable {
        let type = "library"
        let name: String
        let version: String
        let supplier: Supplier
        let licenses: [LicenseRef]
        let externalReferences: [Reference]
    }

    private struct Supplier: Encodable {
        let name: String
    }

    private struct LicenseRef: Encodable {
        let license: License
    }

    private struct License: Encodable {
        let id: String
    }

    private struct Reference: Encodable {
        let type = "website"
        let url: String
    }
}
