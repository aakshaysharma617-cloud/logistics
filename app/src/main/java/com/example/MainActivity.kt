package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MainAppContainer
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.LogisticsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Prompt for notification permission on Android 13+ (Tiramisu)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val requestPermissionLauncher = registerForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                // Permission outcome handled gracefully
            }
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val viewModel = remember { LogisticsViewModel(application) }
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            MyApplicationTheme(darkTheme = isDarkMode) {
                val currentUser by viewModel.currentUser.collectAsState()

                AnimatedContent(
                    targetState = currentUser != null,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "LoginTransition"
                ) { isLoggedIn ->
                    if (isLoggedIn) {
                        MainAppContainer(
                            viewModel = viewModel,
                            onLogout = {}
                        )
                    } else {
                        LoginScreen(
                            viewModel = viewModel,
                            onLoginSuccess = {}
                        )
                    }
                }
            }
        }
    }
}
