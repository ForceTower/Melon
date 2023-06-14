package com.forcetower.uefs.feature.allownotification

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.forcetower.uefs.R
import com.forcetower.uefs.databinding.ActivityAllowNotificationBinding
import com.forcetower.uefs.feature.shared.UActivity

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class AllowNotificationActivity : UActivity() {
    private val requestPostNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) finish()
        else onPermissionsDenied()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = DataBindingUtil.setContentView<ActivityAllowNotificationBinding>(this, R.layout.activity_allow_notification)
        binding.btnNotNow.setOnClickListener { finish() }
        binding.btnAllow.setOnClickListener { requestNotificationPermission() }
    }

    override fun onStart() {
        super.onStart()
        checkNotificationPermission()
    }

    private fun checkNotificationPermission() {
        if (ContextCompat.checkSelfPermission(this, PERMISSION) == PackageManager.PERMISSION_GRANTED)
            finish()
    }

    private fun requestNotificationPermission() {
        val result = ContextCompat.checkSelfPermission(this, PERMISSION)
        if (result == PackageManager.PERMISSION_GRANTED) {
            finish()
        } else {
            requestPostNotificationPermission.launch(PERMISSION)
        }
    }

    private fun onPermissionsDenied() {
        if (!shouldShowRequestPermissionRationale(PERMISSION)) {
            onPermissionPermanentlyDenied()
        }
    }

    private fun onPermissionPermanentlyDenied() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    companion object {
        private const val PERMISSION = Manifest.permission.POST_NOTIFICATIONS
    }
}
