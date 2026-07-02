import ComposableArchitecture

/// The matrícula endpoints. Offers and vacancy counts change by the second
/// upstream, so nothing is cached — every call goes straight to the API.
@DependencyClient
struct EnrollmentRepository: Sendable {
    /// The current enrollment window, or nil while no step is published.
    var window: @Sendable () async throws -> EnrollmentWindow?
    /// The full offered-disciplines tree for the current step. Heavy — only
    /// fetched once a window exists.
    var offers: @Sendable () async throws -> [EnrollmentDiscipline]
    /// Submits the complete desired set — the backend reopens the step if
    /// needed, replaces the saved proposal wholesale, and finalizes.
    var submit: @Sendable (_ selections: [EnrollmentSelection]) async throws -> Void
}

extension EnrollmentRepository: TestDependencyKey {
    static let testValue = EnrollmentRepository()

    static let previewValue = EnrollmentRepository(
        window: { .preview },
        offers: { .previewCatalogue },
        submit: { _ in }
    )
}

extension DependencyValues {
    var enrollmentRepository: EnrollmentRepository {
        get { self[EnrollmentRepository.self] }
        set { self[EnrollmentRepository.self] = newValue }
    }
}
