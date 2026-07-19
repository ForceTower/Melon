import ComposableArchitecture

/// Keeps the evening-before evaluation reminders honest: observes the
/// mirror's pending-evaluation projection and reconciles the scheduled
/// local notifications against it.
@DependencyClient
struct EvaluationReminderClient: Sendable {
    /// Runs for the app's whole lifetime; cancelling the task stops it.
    var run: @Sendable () async -> Void
    /// One-shot reconcile — for gate flips and returns from background,
    /// which don't write to the mirror and so never re-emit the projection.
    var reconcile: @Sendable () async -> Void
}

extension EvaluationReminderClient: TestDependencyKey {
    static let testValue = EvaluationReminderClient()
    static let previewValue = EvaluationReminderClient(run: {}, reconcile: {})
}

extension DependencyValues {
    var evaluationReminders: EvaluationReminderClient {
        get { self[EvaluationReminderClient.self] }
        set { self[EvaluationReminderClient.self] = newValue }
    }
}
