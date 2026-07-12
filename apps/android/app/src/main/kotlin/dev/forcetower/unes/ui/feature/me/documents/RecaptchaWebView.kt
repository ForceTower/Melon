package dev.forcetower.unes.ui.feature.me.documents

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

// Google reCAPTCHA v2 in a WebView. The widget is rendered explicitly with
// the site key remote config delivered, on a page anchored to the portal's
// login origin — the key belongs to the portal, so Google checks its domain
// allow-list against that origin. Mirrors iOS `RecaptchaView`.
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun RecaptchaWebView(
    siteKey: String,
    baseUrl: String,
    onToken: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnToken by rememberUpdatedState(onToken)
    val theme = if (isSystemInDarkTheme()) "dark" else "light"

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                // AndroidView doesn't impose layout params — without explicit
                // MATCH_PARENT ones the WebView measures itself 0×0 and the
                // captcha never renders.
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                settings.javaScriptEnabled = true
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun postToken(token: String) {
                            if (token.isEmpty()) return
                            // The JS bridge calls in on a WebView-internal
                            // thread — hop to main before touching state.
                            post { currentOnToken(token) }
                        }
                    },
                    "recaptcha",
                )
                loadDataWithBaseURL(
                    baseUrl.ifEmpty { null },
                    captchaHtml(siteKey, theme),
                    "text/html",
                    "utf-8",
                    null,
                )
            }
        },
    )
}

private fun captchaHtml(siteKey: String, theme: String): String = """
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
          sitekey: '$siteKey',
          theme: '$theme',
          callback: function (token) { window.recaptcha.postToken(token); },
          'expired-callback': function () { grecaptcha.reset(); }
        });
      }
    </script>
    <script src="https://www.google.com/recaptcha/api.js?onload=captchaLoaded&render=explicit" async defer></script>
    </body>
    </html>
""".trimIndent()
