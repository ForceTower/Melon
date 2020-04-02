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

package com.forcetower.uefs.feature.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.forcetower.core.injection.Injectable
import com.forcetower.sagres.SagresNavigator
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.FragmentTechNopeCaptchaBinding
import com.forcetower.uefs.feature.shared.UFragment
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_event_editor_index.password
import timber.log.Timber
import javax.inject.Inject

class TechNopeCaptchaFragment : UFragment(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory

    private lateinit var binding: FragmentTechNopeCaptchaBinding
    private val args by navArgs<TechNopeCaptchaFragmentArgs>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // create a layout that is basically a webview, monitor page progress and redirect.
        // catch the cookies and set to the navigator
        // import okhttp3.Cookie;
        // SagresNavigator.instance.setCookies(List<Cookie>) ...... this method will be available on next version of navigator
        return FragmentTechNopeCaptchaBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        showSnack("Eu odeio essa parte...", Snackbar.LENGTH_LONG)
        binding.webView.apply {
            loadUrl("http://academico2.uefs.br/Portal/Acesso.aspx")
            settings.apply {
                javaScriptEnabled = true
                userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.100 Safari/537.36"
                loadWithOverviewMode = true
                useWideViewPort = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
            }
            scrollBarStyle = WebView.SCROLLBARS_OUTSIDE_OVERLAY
            isScrollbarFadingEnabled = false
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    val cookies = CookieManager.getInstance().getCookie(url)
                    Timber.d("The URL: $url")
                    Timber.d("The cookies $cookies")

                    // It's hardcoded... yes
                    if (url == "http://academico2.uefs.br/Portal/Modules/Portal/Default.aspx") {
                        onCompleteSteps(cookies)
                    }
                }
            }
        }
    }

    private fun onCompleteSteps(cookies: String) {
        // set cookies on navigator!
        SagresNavigator.instance.setCookiesOnClient(cookies)
        // Change to livedata event this is temp
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            binding.webView.destroy()
            val direction = TechNopeCaptchaFragmentDirections.actionLoginTechNopeToLoginSigningIn(args.username, args.password).apply {
                skipLogin = true
            }
            findNavController().navigate(direction)
        }
    }
}