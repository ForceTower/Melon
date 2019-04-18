package com.forcetower.uefs.core.adapters

import com.google.android.gms.ads.reward.RewardItem
import com.google.android.gms.ads.reward.RewardedVideoAdListener

interface RewardedVideoAdListenerAdapter : RewardedVideoAdListener {
    override fun onRewardedVideoAdClosed() = Unit
    override fun onRewardedVideoAdLeftApplication() = Unit
    override fun onRewardedVideoAdLoaded() = Unit
    override fun onRewardedVideoAdOpened() = Unit
    override fun onRewardedVideoCompleted() = Unit
    override fun onRewarded(item: RewardItem?) = Unit
    override fun onRewardedVideoStarted() = Unit
    override fun onRewardedVideoAdFailedToLoad(reason: Int) = Unit
}