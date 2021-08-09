/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2020. João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.forcetower.uefs.feature.document

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.fragment.app.activityViewModels
import com.forcetower.core.lifecycle.EventObserver
import com.forcetower.uefs.BuildConfig
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.SagresDocument
import com.forcetower.uefs.databinding.FragmentDocumentsBinding
import com.forcetower.uefs.feature.captcha.CaptchaResolverFragment
import com.forcetower.uefs.feature.shared.UFragment
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.io.File

@AndroidEntryPoint
class DocumentsFragment : UFragment() {
    private lateinit var binding: FragmentDocumentsBinding
    private val viewModel: DocumentsViewModel by activityViewModels()
    private lateinit var adapter: DocumentsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentDocumentsBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = DocumentsAdapter(this@DocumentsFragment, viewModel)
        binding.apply {
            recyclerDocuments.adapter = adapter
            incToolbar.textToolbarTitle.text = getString(R.string.label_documents)
        }

        viewModel.documents.observe(viewLifecycleOwner, { adapter.documents = it ?: emptyList() })
        viewModel.openDocumentAction.observe(viewLifecycleOwner, EventObserver { openDocument(it) })
        viewModel.snackMessages.observe(viewLifecycleOwner, EventObserver { showSnack(it) })
        viewModel.onRequestDownload.observe(viewLifecycleOwner, EventObserver { requestCaptchaForDocument(it) })
    }

    private fun requestCaptchaForDocument(document: SagresDocument) {
        val fragment = CaptchaResolverFragment()
        fragment.setCallback(
            object : CaptchaResolverFragment.CaptchaResolvedCallback {
                override fun onCaptchaResolved(token: String) {
                    Timber.d("Token received $token")
                    viewModel.onDownload(document, token)
                }
            }
        )
        fragment.show(childFragmentManager, "captcha_resolver")
    }

    private fun openDocument(document: File) {
        val intent = Intent(Intent.ACTION_VIEW)
        val uri = FileProvider.getUriForFile(requireContext(), BuildConfig.APPLICATION_ID + ".fileprovider", document)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.setDataAndType(uri, "application/pdf")
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        val choose = Intent.createChooser(intent, getString(R.string.open_file))
        try {
            startActivity(choose)
        } catch (e: ActivityNotFoundException) {
            showSnack(getString(R.string.no_pdf_reader))
        }
    }
}
