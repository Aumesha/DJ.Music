package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

enum class AdType {
    INTERSTITIAL,
    REWARDED
}

data class ActiveSimulatedAd(
    val type: AdType,
    val title: String,
    val sponsorName: String,
    val durationSeconds: Int = 5,
    val onComplete: (Boolean) -> Unit
)

object AdMobManager {
    private const val TAG = "AdMobManager"

    // Official Google AdMob Test Ad Unit IDs
    const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var isInitializing = false

    // State for interactive ad display when Google Play Services is pending
    var activeSimulatedAd by mutableStateOf<ActiveSimulatedAd?>(null)
        private set

    fun initialize(context: Context) {
        if (isInitializing) return
        isInitializing = true
        try {
            MobileAds.initialize(context) { status ->
                Log.d(TAG, "AdMob MobileAds initialized: $status")
                loadInterstitial(context)
                loadRewarded(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MobileAds", e)
        }
    }

    fun loadInterstitial(context: Context) {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial Ad loaded successfully")
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Interstitial Ad failed to load: ${error.message}")
                    interstitialAd = null
                }
            }
        )
    }

    fun loadRewarded(context: Context) {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Rewarded Ad loaded successfully")
                    rewardedAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Rewarded Ad failed to load: ${error.message}")
                    rewardedAd = null
                }
            }
        )
    }

    fun showInterstitial(activity: Activity, onDismissed: () -> Unit) {
        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Interstitial Ad dismissed")
                    interstitialAd = null
                    loadInterstitial(activity)
                    onDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    Log.w(TAG, "Interstitial failed to show: ${error.message}")
                    interstitialAd = null
                    loadInterstitial(activity)
                    // Fallback to simulated ad if failed
                    showSimulatedAd(AdType.INTERSTITIAL, "Sponsored Interstitial Ad", "Google AdMob Partner") {
                        onDismissed()
                    }
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad displayed")
                }
            }
            ad.show(activity)
        } else {
            // If ad is not ready or in environment without play services, show simulated full-screen ad
            Log.d(TAG, "AdMob interstitial not ready; displaying interactive fallback ad")
            loadInterstitial(activity)
            showSimulatedAd(AdType.INTERSTITIAL, "AdMob Full-Screen Showcase", "Test AdMob Network") {
                onDismissed()
            }
        }
    }

    fun showRewarded(activity: Activity, onRewardEarned: () -> Unit, onDismissed: () -> Unit) {
        val ad = rewardedAd
        if (ad != null) {
            var rewardGranted = false
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Rewarded Ad dismissed")
                    rewardedAd = null
                    loadRewarded(activity)
                    if (rewardGranted) {
                        onRewardEarned()
                    }
                    onDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    Log.w(TAG, "Rewarded ad failed to show: ${error.message}")
                    rewardedAd = null
                    loadRewarded(activity)
                    showSimulatedAd(AdType.REWARDED, "Watch Ad to Unlock Video", "Premium Ad Network") { earned ->
                        if (earned) onRewardEarned()
                        onDismissed()
                    }
                }
            }
            ad.show(activity) { rewardItem ->
                Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                rewardGranted = true
            }
        } else {
            Log.d(TAG, "AdMob rewarded ad not ready; displaying interactive fallback ad")
            loadRewarded(activity)
            showSimulatedAd(AdType.REWARDED, "Watch Ad to Unlock Video", "Google AdMob Premium Ad") { earned ->
                if (earned) onRewardEarned()
                onDismissed()
            }
        }
    }

    fun showSimulatedAd(
        type: AdType,
        title: String,
        sponsorName: String,
        onComplete: (Boolean) -> Unit
    ) {
        activeSimulatedAd = ActiveSimulatedAd(
            type = type,
            title = title,
            sponsorName = sponsorName,
            durationSeconds = if (type == AdType.REWARDED) 5 else 4,
            onComplete = { success ->
                activeSimulatedAd = null
                onComplete(success)
            }
        )
    }

    fun dismissSimulatedAd(completed: Boolean) {
        val current = activeSimulatedAd
        activeSimulatedAd = null
        current?.onComplete?.invoke(completed)
    }
}
