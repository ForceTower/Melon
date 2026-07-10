import ComposableArchitecture
import Foundation

/// The document-request sheet. Every successful fetch lands in the on-device
/// offline slot, so reopening the sheet offers the saved copy immediately and
/// the portal is only contacted to refresh it — through the reCAPTCHA gate
/// when remote config delivers a site key.
@Reducer
struct MeDocumentFeature {
    @ObservableState
    struct State: Equatable {
        let document: AcademicDocument
        var studentName: String?
        var course: String?
        /// The CR feeding the histórico summary row.
        var score: Double?
        /// The offline copy on this device — loaded by MeFeature on open.
        var stored: StoredAcademicDocument?
        var stage: Stage
        @Shared(.appStorage(FeatureFlags.documentCaptchaSiteKeyKey)) var captchaSiteKey = ""
        @Shared(.appStorage(FeatureFlags.documentCaptchaBaseURLKey)) var captchaBaseURL = ""

        init(
            document: AcademicDocument,
            studentName: String? = nil,
            course: String? = nil,
            score: Double? = nil,
            stored: StoredAcademicDocument? = nil
        ) {
            self.document = document
            self.studentName = studentName
            self.course = course
            self.score = score
            self.stored = stored
            self.stage = stored == nil ? .intro : .saved
        }

        enum Stage: Equatable {
            /// Nothing saved yet — just the download CTA.
            case intro
            /// The offline copy, offered straight away on open.
            case saved
            case captcha
            case generating
            /// Refresh landed — the offline copy was just replaced.
            case fresh
            /// Refresh failed — showing the offline copy. The date is the
            /// badge's: the server copy's generation date when its fallback
            /// answered, or the local save date when nothing did.
            case stale(savedAt: Date)
            /// Nothing to show, and no saved copy to fall back on.
            case failed(FailureReason)
        }

        enum FailureReason: Equatable, Sendable {
            /// Request never landed, or the server errored.
            case connection
            /// The backend answered 404: the portal did not issue this
            /// document for the student (and no stored version exists).
            case unavailable
        }

        var needsCaptcha: Bool { !captchaSiteKey.isEmpty }
    }

    enum Action: Equatable {
        case downloadTapped
        case captchaSolved(token: String)
        case captchaCanceled
        case documentReady(FetchedAcademicDocument)
        case documentFailed(State.FailureReason)
        case closeTapped
    }

    @Dependency(\.documentsRepository) var documentsRepository
    @Dependency(\.localDocuments) var localDocuments
    @Dependency(\.dismiss) var dismiss

    private let log = Log.scoped("MeDocumentFeature")

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .downloadTapped:
                if state.needsCaptcha {
                    log.info("document request gated by captcha kind=\(state.document.rawValue)")
                    state.stage = .captcha
                    return .none
                }
                state.stage = .generating
                return fetch(state.document, token: nil)

            case let .captchaSolved(token):
                log.info("document captcha solved kind=\(state.document.rawValue)")
                state.stage = .generating
                return fetch(state.document, token: token)

            case .captchaCanceled:
                state.stage = state.stored == nil ? .intro : .saved
                return .none

            case let .documentReady(fetched):
                do {
                    state.stored = try localDocuments.save(state.document, fetched.fileURL)
                } catch {
                    log.warn("offline save failed; using the downloaded file directly", error: error)
                    state.stored = StoredAcademicDocument(
                        fileURL: fetched.fileURL,
                        version: state.stored?.version ?? 1,
                        savedAt: fetched.generatedAt
                    )
                }
                state.stage = fetched.isFresh ? .fresh : .stale(savedAt: fetched.generatedAt)
                return .none

            case let .documentFailed(reason):
                if let stored = state.stored {
                    state.stage = .stale(savedAt: stored.savedAt)
                } else {
                    state.stage = .failed(reason)
                }
                return .none

            case .closeTapped:
                return .run { _ in await dismiss() }
            }
        }
    }

    private func fetch(_ document: AcademicDocument, token: String?) -> Effect<Action> {
        .run { send in
            do {
                let fetched = try await documentsRepository.fetch(document, token)
                await send(.documentReady(fetched))
            } catch {
                await send(.documentFailed(failureReason(for: error)))
            }
        }
    }

    private func failureReason(for error: any Error) -> State.FailureReason {
        if case APIError.server(status: 404, _) = error { return .unavailable }
        return .connection
    }
}
