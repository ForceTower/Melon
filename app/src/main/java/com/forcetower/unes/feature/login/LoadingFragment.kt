package com.forcetower.unes.feature.login

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import com.forcetower.sagres.database.model.SagresAccess
import com.forcetower.unes.R
import com.forcetower.unes.core.injection.Injectable
import com.forcetower.unes.core.vm.LoginViewModel
import com.forcetower.unes.core.vm.UViewModelFactory
import com.forcetower.unes.databinding.FragmentLoadingBinding
import com.forcetower.unes.feature.home.HomeActivity
import com.forcetower.unes.feature.shared.UFragment
import com.forcetower.unes.feature.shared.fadeIn
import com.forcetower.unes.feature.shared.fadeOut
import com.forcetower.unes.feature.shared.provideViewModel
import timber.log.Timber
import javax.inject.Inject

class LoadingFragment : UFragment(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory

    private lateinit var binding: FragmentLoadingBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentLoadingBinding.inflate(inflater, container, false).also {
            binding = it
            binding.btnFirstSteps.setOnClickListener { v ->
                v.findNavController().navigate(R.id.action_login_loading_to_login_form) }
        }.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        provideViewModel<LoginViewModel>(factory).getAccess()?.observe(this, Observer { access -> onReceiveToken(access) })
    }

    private fun onReceiveToken(access: SagresAccess?) {
        if (access == null) {
            binding.btnFirstSteps.fadeIn()
            binding.contentLoading.fadeOut()
        } else {
            Timber.d("Connected already")
            val intent = Intent(context, HomeActivity::class.java)
            startActivity(intent)
        }
    }
}