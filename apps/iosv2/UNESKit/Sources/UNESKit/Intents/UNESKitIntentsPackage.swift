import AppIntents

/// Anchor for App Intents metadata extraction inside this package. The UNES
/// app target's `AppIntentsPackage` lists it in `includedPackages`, which is
/// what lets the extractor find intents across the SPM boundary.
public struct UNESKitIntentsPackage: AppIntentsPackage {}
