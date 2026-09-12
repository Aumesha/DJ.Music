package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import com.example.ads.AdMobManager
import com.example.data.model.VideoItem
import com.example.data.repository.VideoRepository
import com.example.service.NotificationHelper
import com.example.ui.components.AdOverlayDialog
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.MainVideoScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.VideoPlayerScreen
import com.example.ui.theme.MyApplicationTheme

enum class Screen {
    SPLASH,
    MAIN,
    PLAYER,
    ADMIN
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize AdMob and Notification channels early
        AdMobManager.initialize(applicationContext)
        NotificationHelper.createNotificationChannel(applicationContext)

        val repository = VideoRepository(applicationContext)

        setContent {
            MyApplicationTheme {
                // Request notification permission for Android 13+ (POST_NOTIFICATIONS)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val notificationPermissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { isGranted ->
                        // Permission result handled
                    }

                    LaunchedEffect(Unit) {
                        val currentPermission = ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                        if (currentPermission != PackageManager.PERMISSION_GRANTED) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                var currentScreen by remember { mutableStateOf(Screen.SPLASH) }
                var activeVideo by remember { mutableStateOf<VideoItem?>(null) }
                var authenticatedAdminEmail by remember { mutableStateOf("") }

                Box(modifier = Modifier.fillMaxSize()) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color(0xFF070A10)
                    ) {
                        when (currentScreen) {
                            Screen.SPLASH -> {
                                SplashScreen(
                                    onSplashFinished = {
                                        currentScreen = Screen.MAIN
                                    }
                                )
                            }

                            Screen.MAIN -> {
                                MainVideoScreen(
                                    videoRepository = repository,
                                    onOpenAdmin = { email ->
                                        authenticatedAdminEmail = email
                                        currentScreen = Screen.ADMIN
                                    },
                                    onPlayVideo = { video ->
                                        activeVideo = video
                                        currentScreen = Screen.PLAYER
                                    }
                                )
                            }

                            Screen.PLAYER -> {
                                val video = activeVideo
                                if (video != null) {
                                    BackHandler {
                                        currentScreen = Screen.MAIN
                                    }

                                    VideoPlayerScreen(
                                        video = video,
                                        onBack = {
                                            currentScreen = Screen.MAIN
                                        }
                                    )
                                } else {
                                    currentScreen = Screen.MAIN
                                }
                            }

                            Screen.ADMIN -> {
                                BackHandler {
                                    currentScreen = Screen.MAIN
                                }

                                AdminScreen(
                                    videoRepository = repository,
                                    adminEmail = authenticatedAdminEmail,
                                    onBack = {
                                        currentScreen = Screen.MAIN
                                    },
                                    onPreviewVideo = { video ->
                                        activeVideo = video
                                        currentScreen = Screen.PLAYER
                                    }
                                )
                            }
                        }
                    }

                    // Interactive AdOverlayDialog (renders whenever an ad is displayed)
                    AdOverlayDialog()
                }
            }
        }
    }
}
