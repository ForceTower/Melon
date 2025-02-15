package com.forcetower.uefs.feature.allownotification

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.databinding.DataBindingUtil
import com.forcetower.uefs.R
import com.forcetower.uefs.databinding.ActivityAllowNotificationBinding
import com.forcetower.uefs.feature.shared.UActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AllowNotificationActivity : UActivity() {
    @Inject lateinit var preferences: SharedPreferences

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val requestPostNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            finish()
        } else {
            onPermissionsDenied()
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = DataBindingUtil.setContentView<ActivityAllowNotificationBinding>(this, R.layout.activity_allow_notification)
        val shown = preferences.getBoolean("notification_request_already_show", false)
        if (shown) binding.btnNotNow.setText(R.string.allow_notification_never)
        binding.btnNotNow.setOnClickListener {
            val key = if (!shown) "notification_request_already_show" else "notification_request_do_not_ask"
            preferences.edit { putBoolean(key, true) }
            finish()
        }
        binding.btnAllow.setOnClickListener { requestNotificationPermission() }
    }

    override fun onStart() {
        super.onStart()
        checkNotificationPermission()
    }

    private fun checkNotificationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            finish()
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestNotificationPermission() {
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
        if (result == PackageManager.PERMISSION_GRANTED) {
            finish()
        } else {
            requestPostNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun onPermissionsDenied() {
        if (!shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            onPermissionPermanentlyDenied()
        }
    }

    private fun onPermissionPermanentlyDenied() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }
}
