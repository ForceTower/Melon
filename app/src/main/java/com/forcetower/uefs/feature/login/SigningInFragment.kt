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
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity.CENTER
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.navigation.ActivityNavigator
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.forcetower.sagres.operation.Callback
import com.forcetower.sagres.operation.Status
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.Profile
import com.forcetower.uefs.core.storage.repository.LoginSagresRepository
import com.forcetower.uefs.core.util.fromJson
import com.forcetower.uefs.databinding.FragmentSigningInBinding
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.fadeIn
import com.forcetower.uefs.feature.shared.fadeOut
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.nio.charset.Charset
import javax.inject.Inject

@AndroidEntryPoint
class SigningInFragment : UFragment() {
    @Inject
    lateinit var firebaseAuth: FirebaseAuth
    @Inject
    lateinit var firebaseStorage: FirebaseStorage

    private lateinit var binding: FragmentSigningInBinding
    private val viewModel: LoginViewModel by viewModels()
    private lateinit var messages: Array<String>

    private val args by navArgs<SigningInFragmentArgs>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAuth = FirebaseAuth.getInstance()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentSigningInBinding.inflate(inflater, container, false).also {
            binding = it
            prepareSwitcher()
            prepareMessages()
        }.apply {
            firebaseStorage = this@SigningInFragment.firebaseStorage
            firebaseUser = this@SigningInFragment.firebaseAuth.currentUser
            executePendingBindings()
        }.root
    }

    private fun prepareMessages() {
        try {
            val stream = context?.assets?.open("login_messages.json")
            if (stream != null) {
                val size = stream.available()
                val buffer = ByteArray(size)
                stream.read(buffer)
                stream.close()
                val json = String(buffer, Charset.forName("UTF-8"))
                messages = json.fromJson()
            }
        } catch (e: Exception) {
            messages = arrayOf("Hum... Algo de estranho aconteceu", "O aplicativo não tem mensagens...", "Corre!!!", "Me avisa que isso aconteceu!")
        }
    }

    private fun prepareSwitcher() {
        val font = ResourcesCompat.getFont(requireContext(), R.font.product_sans_regular)

        binding.textStatus.setFactory {
            val textView = TextView(requireContext())
            textView.textSize = 16f
            textView.gravity = CENTER
            textView.typeface = font
            val typedValue = TypedValue()
            val theme = requireContext().theme
            theme.resolveAttribute(R.attr.colorOnSurface, typedValue, true)
            val colorOnSurface = typedValue.data
            textView.setTextColor(colorOnSurface)
            textView
        }

        binding.textTips.setFactory {
            val textView = TextView(requireContext())
            textView.textSize = 13f
            textView.gravity = CENTER
            textView.typeface = font
            textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
            textView
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.getLogin().observe(viewLifecycleOwner, { onLoginProgress(it) })
        viewModel.getProfile().observe(viewLifecycleOwner, Observer(this::onProfileUpdate))
        viewModel.getStep(args.snowpiercer).observe(viewLifecycleOwner, Observer(this::onStep))
        doLogin()
    }

    override fun onStart() {
        super.onStart()
        displayRandomText()
        firebaseAuthListener()
    }

    private fun displayRandomText() {
        val position = (Math.random() * (messages.size - 1)).toInt()
        val message = messages[position]
        binding.textStatus.setText(message)
        Handler(Looper.getMainLooper()).postDelayed(
            {
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
                    displayRandomText()
            },
            3000
        )
    }

    private fun doLogin() {
        val username = args.username
        val password = args.password
        val captcha = args.captchaToken
        val snowpiercer = args.snowpiercer

        if (username.isBlank() || password.isBlank()) {
            showSnack(getString(R.string.error_invalid_credentials))
            view?.findNavController()?.popBackStack()
        } else {
            viewModel.login(username, password, captcha, snowpiercer, true)
            if (username.contains("@")) {
                binding.textTips.setText(getString(R.string.enter_using_username_instead))
                binding.textTips.fadeIn()
            } else {
                binding.textTips.fadeOut()
            }
        }
    }

    private fun onStep(step: LoginSagresRepository.Step) {
        binding.contentLoading.setProgressWithAnimation(step.step.toFloat() * 100 / step.count)
    }

    private fun onLoginProgress(callback: Callback) {
        Timber.d("${callback.status}, ${callback.message}")
        when (callback.status) {
            Status.STARTED -> Timber.d("Status: Started")
            Status.LOADING -> Timber.d("Status: Loading")
            Status.INVALID_LOGIN -> snackAndBack(getString(R.string.error_invalid_credentials))
            Status.APPROVING -> Timber.d("Status: Approving")
            Status.NETWORK_ERROR -> snackAndBack(getString(R.string.error_network_error))
            Status.RESPONSE_FAILED -> snackAndBack(getString(R.string.error_unexpected_response_joke))
            Status.SUCCESS -> completeLogin()
            Status.APPROVAL_ERROR -> snackAndBack(getString(R.string.error_network_error))
            Status.GRADES_FAILED -> completeLogin()
            Status.UNKNOWN_FAILURE -> snackAndBack(getString(R.string.unknown_error))
            Status.COMPLETED -> completeLogin()
        }

//        val document = callback.document
//        if (document != null) {
//            val html = document.html()
//            binding.testingWebview.run {
//                loadDataWithBaseURL("", html, "text/html", "ISO8859-1", "")
//            }
//        }
    }

    private fun onProfileUpdate(profile: Profile?) {
        if (profile != null) {
            val username = arguments?.getString("username")
            if (username != null && username.contains("@")) {
                binding.textHelloUser.text = getString(R.string.attention_user_login_with_email, profile.name)
            } else {
                binding.textHelloUser.text = getString(R.string.login_hello_user, profile.name)
            }
            binding.textHelloUser.fadeIn()
        }
    }

    private fun completeLogin() {
        if (viewModel.isConnected()) return
        viewModel.setConnected()
        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), binding.imageCenter, getString(R.string.user_image_transition))
        val extras = ActivityNavigator.Extras.Builder()
            .setActivityOptions(options)
            .build()

        binding.textHelloUser.fadeOut()

        Handler(Looper.getMainLooper()).postDelayed(
            {
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    binding.textHelloUser.text = ""
                    findNavController().navigate(R.id.action_login_to_setup, null, null, extras)
                    activity?.finishAfterTransition()
                }
            },
            1000
        )
    }

    private fun firebaseAuthListener() {
        firebaseAuth.addAuthStateListener {
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                val current = it.currentUser
                Timber.d("Auth State changed... User is ${current?.email}")
                binding.firebaseUser = current
                binding.executePendingBindings()
            }
        }
    }

    private fun snackAndBack(string: String) {
        showSnack(string)
        firebaseAuth.signOut()
        binding.textHelloUser.text = ""
        binding.textHelloUser.fadeOut()
        binding.textTips.fadeOut()
        findNavController().popBackStack(R.id.login_form, false)
    }
}
