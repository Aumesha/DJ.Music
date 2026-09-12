package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ads.AdMobManager
import com.example.ads.AdType
import kotlinx.coroutines.delay

@Composable
fun AdOverlayDialog() {
    val activeAd = AdMobManager.activeSimulatedAd ?: return

    var secondsLeft by remember(activeAd) { mutableIntStateOf(activeAd.durationSeconds) }
    var rewardEarned by remember(activeAd) { mutableIntStateOf(0) } // 0: watching, 1: earned

    LaunchedEffect(activeAd) {
        while (secondsLeft > 0) {
            delay(1000L)
            secondsLeft -= 1
        }
        rewardEarned = 1
    }

    Dialog(
        onDismissRequest = {
            if (secondsLeft == 0 || activeAd.type == AdType.INTERSTITIAL) {
                AdMobManager.dismissSimulatedAd(rewardEarned == 1)
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = (secondsLeft == 0),
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF070A10))
                .testTag("ad_overlay_container")
        ) {
            // Background subtle gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF131B2E),
                                Color(0xFF090D16),
                                Color(0xFF030712)
                            )
                        )
                    )
            )

            // Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFFFB703))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (activeAd.type == AdType.REWARDED) "REWARDED AD" else "INTERSTITIAL AD",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = activeAd.sponsorName,
                        color = Color(0xFFCBD5E1),
                        fontSize = 12.sp
                    )
                }

                if (secondsLeft > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x33FFFFFF))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Reward in ${secondsLeft}s",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    IconButton(
                        onClick = { AdMobManager.dismissSimulatedAd(rewardEarned == 1) },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x55FFFFFF))
                            .testTag("ad_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Ad",
                            tint = Color.White
                        )
                    }
                }
            }

            // Central Ad Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF00E5FF), Color(0xFF7C3AED))
                            )
                        )
                        .border(3.dp, Color(0x66FFFFFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (activeAd.type == AdType.REWARDED) Icons.Default.CardGiftcard else Icons.Default.PlayArrow,
                        contentDescription = "Ad Sponsor",
                        tint = Color.White,
                        modifier = Modifier.size(52.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = activeAd.title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (activeAd.type == AdType.REWARDED)
                        "Watch this short sponsored message completely to unlock uninterrupted access to your chosen video!"
                    else
                        "Enjoy premium streaming on Video Stream. AdMob interstitial sponsored experience.",
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                val progress = (activeAd.durationSeconds - secondsLeft).toFloat() / activeAd.durationSeconds.toFloat()
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFF00E5FF),
                    trackColor = Color(0xFF1E293B),
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (secondsLeft == 0) {
                    Button(
                        onClick = { AdMobManager.dismissSimulatedAd(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(50.dp)
                            .testTag("ad_claim_reward_button")
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (activeAd.type == AdType.REWARDED) "Reward Granted! Watch Video" else "Continue to App",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    Text(
                        text = "Playing Ad (${secondsLeft}s remaining)...",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp
                    )
                }
            }

            // Bottom brand indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Powered by Google Mobile Ads SDK",
                    color = Color(0xFF475569),
                    fontSize = 11.sp
                )
            }
        }
    }
}
