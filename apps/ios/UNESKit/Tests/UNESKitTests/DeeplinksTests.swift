import Testing

@testable import UNESKit

/// The `unes://` grammar, mirrored from the Android parser table — the URL
/// is a wire contract composed by the backend, so both parsers must agree.
struct DeeplinksTests {
    @Test
    func tabHostsResolveToTheirTabs() {
        #expect(Deeplinks.parse("unes://home") == .tab(.home))
        #expect(Deeplinks.parse("unes://schedule") == .tab(.schedule))
        #expect(Deeplinks.parse("unes://classes") == .tab(.classes))
        #expect(Deeplinks.parse("unes://messages") == .tab(.messages))
        #expect(Deeplinks.parse("unes://me") == .tab(.me))
    }

    @Test
    func schemeAndHostAreCaseInsensitiveIdsAreNot() {
        #expect(Deeplinks.parse("UNES://Messages") == .tab(.messages))
        #expect(Deeplinks.parse("unes://messages/AbC") == .message(id: "AbC"))
    }

    @Test
    func trailingSlashStillResolvesTheTab() {
        #expect(Deeplinks.parse("unes://messages/") == .tab(.messages))
    }

    @Test
    func messageDetailCarriesTheId() {
        #expect(
            Deeplinks.parse("unes://messages/0d4e9a52-77aa-4e2e-9646-c7a5599e6d2b")
                == .message(id: "0d4e9a52-77aa-4e2e-9646-c7a5599e6d2b")
        )
    }

    @Test
    func materialsDisciplineShelfCarriesTheDisciplineId() {
        #expect(
            Deeplinks.parse("unes://materials/discipline/6f1a2b3c-4d5e-6f70-8192-a3b4c5d6e7f8")
                == .materialsDiscipline(disciplineId: "6f1a2b3c-4d5e-6f70-8192-a3b4c5d6e7f8")
        )
    }

    @Test
    func materialDetailCarriesTheMaterialId() {
        #expect(
            Deeplinks.parse("unes://materials/6f1a2b3c-4d5e-6f70-8192-a3b4c5d6e7f8")
                == .material(id: "6f1a2b3c-4d5e-6f70-8192-a3b4c5d6e7f8")
        )
    }

    @Test
    func queryParamsAreIgnored() {
        #expect(Deeplinks.parse("unes://materials/mat-1?utm=push") == .material(id: "mat-1"))
    }

    @Test
    func fragmentIsIgnored() {
        #expect(Deeplinks.parse("unes://messages/abc#section") == .message(id: "abc"))
    }

    @Test
    func unknownShapesAreDropped() {
        #expect(Deeplinks.parse("unes://materials") == nil)
        #expect(Deeplinks.parse("unes://settings") == nil)
        #expect(Deeplinks.parse("unes://messages/a/b") == nil)
        #expect(Deeplinks.parse("unes://materials/discipline/a/b") == nil)
        #expect(Deeplinks.parse("unes://") == nil)
        #expect(Deeplinks.parse("") == nil)
    }

    @Test
    func otherSchemesAreNotOurs() {
        #expect(Deeplinks.parse("https://messages/abc") == nil)
        #expect(Deeplinks.parse("unes:messages") == nil)
    }
}
