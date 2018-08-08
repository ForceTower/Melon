package com.forcetower.uefs.feature.home

import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.forcetower.uefs.R
import com.forcetower.uefs.databinding.ActivityHomeBinding
import com.forcetower.uefs.feature.shared.UActivity

class HomeActivity : UActivity() {
    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DataBindingUtil.setContentView<ActivityHomeBinding>(this, R.layout.activity_home).also { it ->
            binding = it
            setSupportActionBar(it.bottomAppBar)
            supportActionBar?.setDisplayShowTitleEnabled(false)
            it.bottomAppBar.setNavigationOnClickListener {

            }
        }
    }

}