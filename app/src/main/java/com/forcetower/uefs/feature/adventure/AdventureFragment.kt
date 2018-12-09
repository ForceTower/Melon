package com.forcetower.uefs.feature.adventure

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.forcetower.uefs.GameConnectionStatus
import com.forcetower.uefs.R
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.vm.EventObserver
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.FragmentAdventureBeginsBinding
import com.forcetower.uefs.feature.profile.ProfileViewModel
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.UGameActivity
import com.forcetower.uefs.feature.shared.provideActivityViewModel
import com.forcetower.uefs.feature.shared.provideViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import timber.log.Timber
import javax.inject.Inject

class AdventureFragment : UFragment(), Injectable {
    @Inject
    lateinit var firebaseAuth: FirebaseAuth
    @Inject
    lateinit var firebaseStorage: FirebaseStorage
    @Inject
    lateinit var preferences: SharedPreferences
    @Inject
    lateinit var factory: UViewModelFactory

    private lateinit var viewModel: AdventureViewModel
    private lateinit var profileViewModel: ProfileViewModel
    private var activity: UGameActivity? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = context as? UGameActivity
        activity ?: Timber.e("Adventure Fragment must be attached to a UGameActivity for it to work")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = provideActivityViewModel(factory)
        profileViewModel = provideViewModel(factory)
        return FragmentAdventureBeginsBinding.inflate(inflater, container, false).apply {
            interactor = viewModel
            profile = profileViewModel
            storage = firebaseStorage
            firebaseUser = this@AdventureFragment.firebaseAuth.currentUser
            setLifecycleOwner(this@AdventureFragment)
            executePendingBindings()
        }.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        profileViewModel.getMeProfile().observe(this, Observer {
            profileViewModel.setProfileId(it.uuid)
        })

        viewModel.run {
            achievements.observe(this@AdventureFragment, EventObserver { activity?.openAchievements() })
            start.observe(this@AdventureFragment, EventObserver { activity?.signIn() })
        }

        if (activity?.isConnectedToPlayGames() == false && savedInstanceState == null) {
            openStartupDialog()
        }

        activity?.mGamesInstance?.connectionStatus?.observe(this, EventObserver {
            when (it) {
                GameConnectionStatus.DISCONNECTED -> openStartupDialog()
                GameConnectionStatus.CONNECTED -> showSnack(getString(R.string.connected_to_play_games))
                GameConnectionStatus.LOADING -> Unit
            }
        })
    }

    private fun openStartupDialog() {
        val dialog = AdventureSignInDialog()
        dialog.show(childFragmentManager, "adventure_sign_in")
    }
}