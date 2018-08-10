package com.forcetower.unes.feature.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import com.forcetower.sagres.operation.LoginCallback
import com.forcetower.sagres.operation.Status
import com.forcetower.unes.R
import com.forcetower.unes.core.injection.Injectable
import com.forcetower.unes.core.vm.LoginViewModel
import com.forcetower.unes.core.vm.UViewModelFactory
import com.forcetower.unes.databinding.FragmentSigningInBinding
import com.forcetower.unes.feature.shared.UFragment
import com.forcetower.unes.feature.shared.provideViewModel
import kotlinx.android.synthetic.main.activity_home.*
import timber.log.Timber
import javax.inject.Inject

class SigningInFragment : UFragment(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory

    private lateinit var binding: FragmentSigningInBinding
    private lateinit var viewModel: LoginViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentSigningInBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = provideViewModel(factory)
        viewModel.getLogin().observe(this, Observer<LoginCallback?>(this::onLoginProgress))
        doLogin()
    }

    private fun doLogin() {
        val username = arguments?.getString("username")
        val password = arguments?.getString("password")

        if (username.isNullOrBlank() || password.isNullOrBlank()) {
            showSnack(getString(R.string.error_invalid_credentials))
            view?.findNavController()?.popBackStack()
        } else {
            viewModel.login(username!!, password!!)
        }
    }

    private fun onLoginProgress(callback: LoginCallback?) {
        when(callback?.status) {
            Status.STARTED -> Timber.d("Status: Started")
            Status.LOADING -> Timber.d("Status: Loading")
            Status.INVALID_LOGIN -> snackAndBack(getString(R.string.error_invalid_credentials))
            Status.APPROVING -> Timber.d("Status: Approving")
            Status.NETWORK_ERROR -> snackAndBack(getString(R.string.error_network_error))
            Status.RESPONSE_FAILED -> snackAndBack(getString(R.string.error_unexpected_response))
            Status.SUCCESS -> showSnack(getString(R.string.login_connected))
            Status.APPROVAL_ERROR -> snackAndBack(getString(R.string.error_network_error))
        }
    }

    private fun snackAndBack(string: String) {
        showSnack(string)
        view?.findNavController()?.popBackStack()
    }
}