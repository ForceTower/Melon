/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2020. João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.forcetower.uefs.feature.captcha

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.annotation.Keep
import com.forcetower.sagres.Constants
import com.forcetower.uefs.databinding.FragmentTechNopeCaptchaBinding
import com.google.android.material.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import timber.log.Timber

class CaptchaResolverFragment : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentTechNopeCaptchaBinding
    private lateinit var callback: CaptchaResolvedCallback

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val sheetDialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        try {
            sheetDialog.setOnShowListener {
                val bottomSheet = sheetDialog.findViewById<FrameLayout>(R.id.design_bottom_sheet)!!
                val behavior = BottomSheetBehavior.from(bottomSheet)
                behavior.skipCollapsed = true
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        } catch (t: Throwable) {
            Timber.d(t, "Hum...")
        }
        return sheetDialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentTechNopeCaptchaBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.webView.apply {
            loadDataWithBaseURL(Constants.getParameter("CAPTCHA_BASE"), data, "text/html; charset=utf-8", "UTF-8", null)
            settings.apply {
                javaScriptEnabled = true
                userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.100 Safari/537.36"
                loadWithOverviewMode = true
                // useWideViewPort = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
            }
            scrollBarStyle = WebView.SCROLLBARS_OUTSIDE_OVERLAY
            isScrollbarFadingEnabled = false
            addJavascriptInterface(this@CaptchaResolverFragment, "BridgeWebView")
        }
    }

    @Keep
    @JavascriptInterface
    fun reCaptchaCallback(token: String) {
        Timber.d("reCaptcha token $token")
        dismiss()
        callback.onCaptchaResolved(token)
    }

    fun setCallback(callback: CaptchaResolvedCallback) {
        this.callback = callback
    }

    interface CaptchaResolvedCallback {
        fun onCaptchaResolved(token: String)
    }

    companion object {
        private val data = "<html>\n" +
            "    <head>\n" +
            "      <title>Shit</title>\n" +
            "      <meta content='width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=0' name='viewport' />\n" +
            "      <script src=\"https://www.google.com/recaptcha/api.js\" async defer></script>\n" +
            "<script type=\"text/javascript\">\n" +
            "    function captchaResponse(token){\n" +
            "        BridgeWebView.reCaptchaCallback(token);\n" +
            "    }\n" +
            "</script>" +
            "      <script>\n" +
            "        function onSubmit() {\n" +
            "          var response = grecaptcha.getResponse();\n" +
            "          console.log(response)\n" +
            "          window.postMessage(response);\n" +
            "        }\n" +
            "      </script>\n" +
            "      <script>\n" +
            "        window.onload = function (e) {\n" +
            "          grecaptcha.execute()\n" +
            "        }\n" +
            "      </script>\n" +
            "      <style>\n" +
            "        html, body {\n" +
            "          max-width: 100%;\n" +
            "          overflow-x: hidden;\n" +
            "        }\n" +
            "        .status-message {\n" +
            "          background-color: #00000000;\n" +
            "          margin-bottom: 10px;\n" +
            "          text-align: center;\n" +
            "        }\n" +
            "        textarea {\n" +
            "          margin: 10px 0;\n" +
            "          resize: none;\n" +
            "        }\n" +
            "        .g-recaptcha {\n" +
            "          transform:scale(1.2);\n" +
            "          transform-origin:0 0;\n" +
            "        }\n" +
            "        .hide-all {\n" +
            "          display: none;\n" +
            "        }\n" +
            "      </style>\n" +
            "    </head>\n" +
            "    <body>\n" +
            "        <div class=\"g-recaptcha\"\n" +
            "          data-sitekey=\"" + Constants.getParameter("CAPTCHA_SITE_KEY") + "\"\n" +
            "          data-callback=\"captchaResponse\">\n" +
            "        </div>\n" +
            "    </body>\n" +
            "</html>"
    }
}
