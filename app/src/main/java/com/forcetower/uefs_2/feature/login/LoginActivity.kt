package com.forcetower.uefs_2.feature.login

import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import com.forcetower.uefs_2.R
import com.forcetower.uefs_2.feature.shared.UActivity
import com.forcetower.uefs_2.databinding.ActivityLoginBinding
import com.forcetower.uefs_2.feature.shared.config
import com.google.android.material.snackbar.Snackbar

class LoginActivity : UActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login)
    }

    override fun navigateUpTo(upIntent: Intent?): Boolean = findNavController(R.id.login_nav_host).navigateUp()

    override fun showSnack(string: String) {
        val snack = Snackbar.make(binding.root, string, Snackbar.LENGTH_SHORT)
        snack.config()
        snack.show()
    }
}
