/*
 * Copyright (c) 2019.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.forcetower.sagres.operation.document

import com.forcetower.sagres.Utils
import com.forcetower.sagres.operation.Operation
import com.forcetower.sagres.operation.Status
import com.forcetower.sagres.parsers.SagresLinkFinder
import com.forcetower.sagres.request.SagresCalls
import okio.Okio
import org.jsoup.nodes.Document
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.concurrent.Executor

class DocumentOperation(
    private val file: File,
    private val url: String,
    executor: Executor?
) : Operation<DocumentCallback>(executor) {

    init {
        this.perform()
    }

    override fun execute() {
        val call = SagresCalls.getPageCall(url)
        publishProgress(DocumentCallback(Status.LOADING))
        try {
            val response = call.execute()
            if (response.isSuccessful) {
                val string = response.body()!!.string()
                val document = Utils.createDocument(string)
                onFirstResponse(document)
            } else {
                publishProgress(DocumentCallback(Status.RESPONSE_FAILED).message("Load error").code(500))
            }
        } catch (e: IOException) {
            publishProgress(DocumentCallback(Status.NETWORK_ERROR).message(e.message).throwable(e))
        }
    }

    private fun onFirstResponse(document: Document) {
        val link = SagresLinkFinder.findLink(document)
        if (link == null) {
            Timber.d("Link is null")
            publishProgress(DocumentCallback(Status.RESPONSE_FAILED).code(600).message("Link not found"))
        } else {
            Timber.d("Link found: $link")
            downloadDocument(link)
        }
    }

    private fun downloadDocument(link: String) {
        try {
            val call = SagresCalls.getPageCall(link)
            val response = call.execute()
            if (response.isSuccessful) {
                Timber.d("Will save document as: ${file.absolutePath}")
                if (file.exists()) file.delete()
                file.createNewFile()

                val sink = Okio.buffer(Okio.sink(file))
                sink.writeAll(response.body()!!.source())
                sink.close()
                Timber.d("Document downloaded")
                publishProgress(DocumentCallback(Status.SUCCESS))
            } else {
                Timber.d("Response failed with code ${response.code()}.")
                publishProgress(DocumentCallback(Status.RESPONSE_FAILED).code(response.code()).message("Error..."))
            }
        } catch (e: IOException) {
            Timber.d(e.message)
            publishProgress(DocumentCallback(Status.NETWORK_ERROR).message(e.message).throwable(e))
        }
    }
}