import ComposableArchitecture
import Foundation

/// The document-request sheet: a summary of what the PDF will say, then a
/// reCAPTCHA gate when remote config delivers a site key, then the download.
@Reducer
struct MeDocumentFeature {
    @ObservableState
    struct State: Equatable {
        let document: AcademicDocument
        var studentName: String?
        var course: String?
        /// The CR feeding the histórico summary row.
        var score: Double?
        var stage: Stage = .summary
        @Shared(.appStorage(FeatureFlags.documentCaptchaSiteKeyKey)) var captchaSiteKey = ""

        enum Stage: Equatable {
            case summary
            case captcha
            case generating
            case ready(URL)
            case failed
        }

        var needsCaptcha: Bool { !captchaSiteKey.isEmpty }
    }

    enum Action: Equatable {
        case downloadTapped
        case captchaSolved(token: String)
        case documentReady(URL)
        case documentFailed
        case retryTapped
        case closeTapped
    }

    @Dependency(\.documentsRepository) var documentsRepository
    @Dependency(\.dismiss) var dismiss

    private let log = Log.scoped("MeDocumentFeature")

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .downloadTapped, .retryTapped:
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

            case let .documentReady(url):
                state.stage = .ready(url)
                return .none

            case .documentFailed:
                state.stage = .failed
                return .none

            case .closeTapped:
                return .run { _ in await dismiss() }
            }
        }
    }

    private func fetch(_ document: AcademicDocument, token: String?) -> Effect<Action> {
        .run { send in
            do {
                let url = try await documentsRepository.fetch(document, token)
                await send(.documentReady(url))
            } catch {
                await send(.documentFailed)
            }
        }
    }
}
