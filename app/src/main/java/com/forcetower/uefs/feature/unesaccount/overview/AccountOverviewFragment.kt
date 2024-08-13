package com.forcetower.uefs.feature.unesaccount.overview

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.forcetower.core.adapters.imageUri
import com.forcetower.core.utils.ColorUtils
import com.forcetower.uefs.BuildConfig
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.edge.RegisterPasskeyStart
import com.forcetower.uefs.databinding.FragmentServiceAccountOverviewBinding
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.getPixelsFromDp
import com.forcetower.uefs.feature.unesaccount.overview.vm.AccountOverviewEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class AccountOverviewFragment : UFragment() {
    private lateinit var binding: FragmentServiceAccountOverviewBinding
    private val viewModel by viewModels<AccountOverviewViewModel>()

    private val pickImageContract = registerForActivityResult(ActivityResultContracts.GetContent()) {
        onContentSelected(it)
    }

    private val cropImage = registerForActivityResult(CropImageContract()) {
        onCropResults(it)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel.fetch()
        return FragmentServiceAccountOverviewBinding.inflate(inflater, container, false).also {
            binding = it
            binding.viewModel = viewModel
            binding.lifecycleOwner = viewLifecycleOwner
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.event.observe(viewLifecycleOwner, ::onEvent)

        binding.btnLogin.setOnClickListener {
            onLoginStart()
        }

        binding.btnAddEmail.setOnClickListener {
            onLinkEmail()
        }

        binding.btnCreatePasskey.setOnClickListener {
            viewModel.registerPasskeyStart()
        }

        binding.profileImage.setOnClickListener {
            pickImage()
        }

        binding.profileEmpty.setOnClickListener {
            pickImage()
        }

        binding.btnCreatePasskey.isVisible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    }

    private fun onEvent(event: AccountOverviewEvent) {
        when (event) {
            is AccountOverviewEvent.PasskeyRegister -> onPasskeyRegisterStart(event)
            AccountOverviewEvent.PasskeyRegisterConnectionFailed -> onConnectionFailed()
            AccountOverviewEvent.ImageUpdateFailed -> onImageUpdateFailed()
        }
    }

    private fun onImageUpdateFailed() {
        showSnack(getString(R.string.service_account_update_image_failed))
        imageUri(binding.profileImage, imageUrl = viewModel.user.value?.imageUrl, clipCircle = true)
    }

    private fun onPasskeyRegisterStart(event: AccountOverviewEvent.PasskeyRegister) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return

        val manager = CredentialManager.create(requireContext())

        Timber.d("Register challenge: ${event.json}")

        val request = CreatePublicKeyCredentialRequest(
            requestJson = event.json,
            preferImmediatelyAvailableCredentials = false,
        )

        lifecycleScope.launch {
            try {
                val result = manager.createCredential(
                    context = requireActivity(),
                    request = request,
                )
                handlePasskeyRegistrationResult(result, event)
            } catch (e: CreateCredentialCancellationException) {
                showSnack(getString(R.string.service_account_register_passkey_credential_create_canceled))
                viewModel.onPasskeyRegistrationFinished()
            } catch (e: CreateCredentialException) {
                Timber.e(e, "Failed to create passkey")
                showSnack(getString(R.string.service_account_register_passkey_credential_create_failed))
                viewModel.onPasskeyRegistrationFinished()
            }
        }
    }

    private fun handlePasskeyRegistrationResult(
        result: CreateCredentialResponse,
        event: AccountOverviewEvent.PasskeyRegister
    ) {
        if (result is CreatePublicKeyCredentialResponse) {
            Timber.d("Register Response: ${result.registrationResponseJson}")
            viewModel.registerPasskeyFinish(event.flowId, result.registrationResponseJson)
        } else {
            showSnack(getString(R.string.service_account_register_passkey_credential_not_public))
            viewModel.onPasskeyRegistrationFinished()
        }
    }

    private fun onConnectionFailed() {
        showSnack(getString(R.string.service_account_register_passkey_connect_failed))
    }

    private fun onLoginStart() {
        val directions = AccountOverviewFragmentDirections.actionUnesAccountOverviewToUnesAccountStart()
        findNavController().navigate(directions)
    }

    private fun onLinkEmail() {
        val directions = AccountOverviewFragmentDirections.actionUnesAccountOverviewToUnesAccountLinkEmail()
        findNavController().navigate(directions)
    }

    private fun onImagePicked(uri: Uri) {
        Glide.with(requireContext())
            .load(uri)
            .fallback(com.forcetower.core.R.mipmap.ic_unes_large_image_512)
            .placeholder(com.forcetower.core.R.mipmap.ic_unes_large_image_512)
            .circleCrop()
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(binding.profileImage)

        viewModel.uploadProfilePicture(uri)
    }

    private fun pickImage() {
        pickImageContract.launch("image/*")
    }

    private fun onContentSelected(uri: Uri?) {
        uri ?: return
        val bg = ColorUtils.modifyAlpha(ContextCompat.getColor(requireContext(), R.color.colorPrimary), 120)
        val ac = ContextCompat.getColor(requireContext(), R.color.colorAccent)

        val options = CropImageContractOptions(
            uri,
            CropImageOptions(
                fixAspectRatio = true,
                aspectRatioX = 1,
                aspectRatioY = 1,
                cropShape = CropImageView.CropShape.OVAL,
                backgroundColor = bg,
                borderLineColor = ac,
                borderCornerColor = ac,
                activityMenuIconColor = ac,
                borderLineThickness = getPixelsFromDp(requireContext(), 2),
                activityTitle = getString(R.string.cut_profile_image),
                guidelines = CropImageView.Guidelines.OFF
            )
        )

        cropImage.launch(options)
    }

    private fun onCropResults(result: CropImageView.CropResult) {
        val imageUri = result.uriContent ?: return
        onImagePicked(imageUri)
    }
}