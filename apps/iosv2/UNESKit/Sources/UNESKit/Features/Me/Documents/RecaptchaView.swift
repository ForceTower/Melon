#if os(iOS)
import SwiftUI
import WebKit

/// Google reCAPTCHA v2 in a WKWebView. The widget is rendered explicitly
/// with the site key remote config delivered, on a page anchored to the API
/// origin so the key's domain allow-list matches.
struct RecaptchaView: UIViewRepresentable {
    var siteKey: String
    var onToken: (String) -> Void

    @Environment(\.colorScheme) private var colorScheme

    /// The reCAPTCHA key must allow-list this host.
    private static let origin = URL(string: "https://melon.forcetower.dev")!

    func makeUIView(context: Context) -> WKWebView {
        let configuration = WKWebViewConfiguration()
        configuration.userContentController.add(context.coordinator, name: "recaptcha")
        let webView = WKWebView(frame: .zero, configuration: configuration)
        webView.isOpaque = false
        webView.backgroundColor = .clear
        webView.scrollView.isScrollEnabled = false
        webView.loadHTMLString(html, baseURL: Self.origin)
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {
        context.coordinator.onToken = onToken
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(onToken: onToken)
    }

    static func dismantleUIView(_ webView: WKWebView, coordinator: Coordinator) {
        webView.configuration.userContentController.removeScriptMessageHandler(forName: "recaptcha")
    }

    private var html: String {
        """
        <!doctype html>
        <html>
        <head>
        <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
        <style>html, body { margin: 0; background: transparent; } #captcha { display: flex; justify-content: center; padding-top: 8px; }</style>
        </head>
        <body>
        <div id="captcha"></div>
        <script>
          function captchaLoaded() {
            grecaptcha.render('captcha', {
              sitekey: '\(siteKey)',
              theme: '\(colorScheme == .dark ? "dark" : "light")',
              callback: function (token) { window.webkit.messageHandlers.recaptcha.postMessage(token); },
              'expired-callback': function () { grecaptcha.reset(); }
            });
          }
        </script>
        <script src="https://www.google.com/recaptcha/api.js?onload=captchaLoaded&render=explicit" async defer></script>
        </body>
        </html>
        """
    }

    @MainActor
    final class Coordinator: NSObject, WKScriptMessageHandler {
        var onToken: (String) -> Void

        init(onToken: @escaping (String) -> Void) {
            self.onToken = onToken
        }

        nonisolated func userContentController(
            _ userContentController: WKUserContentController,
            didReceive message: WKScriptMessage
        ) {
            // WebKit delivers script messages on the main thread.
            MainActor.assumeIsolated {
                guard let token = message.body as? String, !token.isEmpty else { return }
                onToken(token)
            }
        }
    }
}

#Preview {
    RecaptchaView(siteKey: "preview-site-key", onToken: { _ in })
        .frame(height: 480)
}
#endif
