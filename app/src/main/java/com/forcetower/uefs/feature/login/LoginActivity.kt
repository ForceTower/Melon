package com.forcetower.uefs.feature.login

import android.content.Intent
import android.os.Bundle
import androidx.navigation.findNavController
import com.forcetower.uefs.R
import com.forcetower.uefs.feature.shared.UActivity

class LoginActivity : UActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
    }

    override fun navigateUpTo(upIntent: Intent?): Boolean = findNavController(R.id.login_nav_host).navigateUp()
}
