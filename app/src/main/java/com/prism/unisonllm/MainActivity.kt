package com.prism.unisonllm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.prism.unisonllm.ui.onboarding.OnboardingScreen
import com.prism.unisonllm.ui.onboarding.OnboardingViewModel
import com.prism.unisonllm.ui.theme.UnisonLLMTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity for UnisonLLM.
 * Entry point for the application with Compose UI.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Handle splash screen
        installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            val viewModel: OnboardingViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            UnisonLLMTheme(themeSettings = uiState.themeSettings) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    OnboardingScreen(
                        uiState = uiState,
                        onIntent = viewModel::processIntent,
                        onOnboardingComplete = {
                            // TODO: Navigate to main chat screen
                            // For now, the onboarding complete state is handled in the ViewModel
                        }
                    )
                }
            }
        }
    }
}
