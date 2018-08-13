package com.forcetower.unes.feature.home

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.forcetower.unes.GlideApp
import com.forcetower.unes.R
import com.forcetower.unes.core.model.Access
import com.forcetower.unes.core.model.Profile
import com.forcetower.unes.core.vm.HomeViewModel
import com.forcetower.unes.core.vm.LoginViewModel
import com.forcetower.unes.core.vm.UViewModelFactory
import com.forcetower.unes.databinding.ActivityHomeBinding
import com.forcetower.unes.feature.login.LoginActivity
import com.forcetower.unes.feature.shared.ToolbarActivity
import com.forcetower.unes.feature.shared.UActivity
import com.forcetower.unes.feature.shared.config
import com.forcetower.unes.feature.shared.provideViewModel
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import timber.log.Timber
import javax.inject.Inject

class HomeActivity : UActivity(), ToolbarActivity, HasSupportFragmentInjector {
    @Inject
    lateinit var fragmentInjector: DispatchingAndroidInjector<Fragment>
    @Inject
    lateinit var vmFactory: UViewModelFactory

    private lateinit var viewModel: HomeViewModel
    private lateinit var binding: ActivityHomeBinding
    private val bottomFragment: HomeBottomFragment by lazy { HomeBottomFragment() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        setupViewModel()
        DataBindingUtil.setContentView<ActivityHomeBinding>(this, R.layout.activity_home).also { it ->
            binding = it
            setSupportActionBar(it.bottomAppBar)
            supportActionBar?.setDisplayShowTitleEnabled(false)
            it.bottomAppBar.setNavigationOnClickListener {
                if (!bottomFragment.isAdded) bottomFragment.show(supportFragmentManager, bottomFragment.tag)
            }
        }
        setupUserData()
    }

    private fun setupViewModel() {
        viewModel = provideViewModel(vmFactory)
    }

    private fun setupUserData() {
        viewModel.access.observe(this, Observer { onAccessUpdate(it) })
        viewModel.profile.observe(this, Observer { onProfileUpdate(it) })
    }

    private fun onAccessUpdate(access: Access?) {
        if (access == null) {
            Timber.d("Access Invalidated")
            val intent = Intent(this, LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        }
    }

    private fun onProfileUpdate(profile: Profile?) {
        if (profile == null) return

        GlideApp.with(this)
                .load(profile.imageUrl)
                .fallback(R.mipmap.ic_unes_large_image_512)
                .placeholder(R.mipmap.ic_unes_large_image_512)
                .circleCrop()
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(binding.imageUserPicture)
    }

    override fun onSupportNavigateUp(): Boolean = findNavController(R.id.home_nav_host).navigateUp()

    override fun getToolbar(): Toolbar = binding.toolbar

    override fun getTabLayout(): TabLayout = binding.tabLayout

    override fun getAppBar(): AppBarLayout = binding.appBar

    override fun showSnack(string: String) {
        val snack = Snackbar.make(binding.snack, string, Snackbar.LENGTH_SHORT)
        snack.config()
        snack.show()
    }

    override fun supportFragmentInjector(): AndroidInjector<Fragment> = fragmentInjector
}