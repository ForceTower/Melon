/*
 * Copyright (c) 2018.
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

package com.forcetower.unes.feature.web

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle

import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession

class CustomTabActivityHelper {
    private var mCustomTabsSession: CustomTabsSession? = null
    private var mClient: CustomTabsClient? = null
    private var mConnection: CustomTabsServiceConnection? = null
    private var mConnectionCallback: ConnectionCallback? = null

    /**
     * Creates or retrieves an exiting CustomTabsSession
     *
     * @return a CustomTabsSession
     */
    val session: CustomTabsSession?
        get() {
            if (mClient == null) {
                mCustomTabsSession = null
            } else if (mCustomTabsSession == null) {
                mCustomTabsSession = mClient!!.newSession(null)
            }
            return mCustomTabsSession
        }

    /**
     * Binds the Activity to the Custom Tabs Service
     * @param activity the activity to be bound to the service
     */
    fun bindCustomTabsService(activity: Activity) {
        if (mClient != null) return

        val packageName = CustomTabsHelper.getPackageNameToUse(activity) ?: return
        mConnection = object : CustomTabsServiceConnection() {
            override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
                mClient = client
                mClient!!.warmup(0L)
                if (mConnectionCallback != null) mConnectionCallback!!.onCustomTabsConnected()
                //Initialize a session as soon as possible.
                session
            }

            override fun onServiceDisconnected(name: ComponentName) {
                mClient = null
                if (mConnectionCallback != null) mConnectionCallback!!.onCustomTabsDisconnected()
            }
        }
        CustomTabsClient.bindCustomTabsService(activity, packageName, mConnection)
    }

    /**
     * Unbinds the Activity from the Custom Tabs Service
     * @param activity the activity that is bound to the service
     */
    fun unbindCustomTabsService(activity: Activity) {
        if (mConnection == null) return
        activity.unbindService(mConnection!!)
        mClient = null
        mCustomTabsSession = null
    }

    /**
     * Register a Callback to be called when connected or disconnected from the Custom Tabs Service
     * @param connectionCallback
     */
    fun setConnectionCallback(connectionCallback: ConnectionCallback) {
        this.mConnectionCallback = connectionCallback
    }

    /**
     * @see {@link CustomTabsSession.mayLaunchUrl
     * @return true if call to mayLaunchUrl was accepted
     */
    fun mayLaunchUrl(uri: Uri, extras: Bundle, otherLikelyBundles: List<Bundle>): Boolean {
        if (mClient == null) return false

        val session = session ?: return false

        return session.mayLaunchUrl(uri, extras, otherLikelyBundles)
    }

    /**
     * A Callback for when the service is connected or disconnected. Use those callbacks to
     * handle UI changes when the service is connected or disconnected
     */
    interface ConnectionCallback {
        /**
         * Called when the service is connected
         */
        fun onCustomTabsConnected()

        /**
         * Called when the service is disconnected
         */
        fun onCustomTabsDisconnected()
    }

    companion object {

        /**
         * Opens the URL on a Custom Tab if possible; otherwise falls back to opening it via
         * `Intent.ACTION_VIEW`
         *
         * @param activity The host activity
         * @param customTabsIntent a CustomTabsIntent to be used if Custom Tabs is available
         * @param uri the Uri to be opened
         */
        fun openCustomTab(activity: Activity, customTabsIntent: CustomTabsIntent, uri: Uri) {
            val packageName = CustomTabsHelper.getPackageNameToUse(activity)

            // if we cant find a package name, it means there's no browser that supports
            // Custom Tabs installed. So, we fallback to a view intent
            if (packageName != null) {
                customTabsIntent.intent.setPackage(packageName)
                customTabsIntent.launchUrl(activity, uri)
            } else {
                activity.startActivity(Intent(Intent.ACTION_VIEW, uri))
            }
        }
    }

}