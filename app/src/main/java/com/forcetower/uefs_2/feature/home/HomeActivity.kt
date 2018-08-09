package com.forcetower.uefs_2.feature.home

import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import com.forcetower.uefs_2.R
import com.forcetower.uefs_2.databinding.ActivityHomeBinding
import com.forcetower.uefs_2.feature.shared.UActivity

class HomeActivity : UActivity() {
    private lateinit var binding: ActivityHomeBinding
    private val bottomFragment: HomeBottomFragment by lazy { HomeBottomFragment() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DataBindingUtil.setContentView<ActivityHomeBinding>(this, R.layout.activity_home).also { it ->
            binding = it
            setSupportActionBar(it.bottomAppBar)
            supportActionBar?.setDisplayShowTitleEnabled(false)
            it.bottomAppBar.setNavigationOnClickListener {
                if (!bottomFragment.isAdded) bottomFragment.show(supportFragmentManager, bottomFragment.tag)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean = findNavController(R.id.home_nav_host).navigateUp()

}