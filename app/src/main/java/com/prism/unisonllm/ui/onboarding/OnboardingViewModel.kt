package com.prism.unisonllm.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prism.unisonllm.data.remote.provider.ChatProviderFactory
import com.prism.unisonllm.data.remote.provider.ProviderConfig
import com.prism.unisonllm.data.remote.provider.ProviderResult
import com.prism.unisonllm.data.remote.provider.ProviderType
import com.prism.unisonllm.ui.theme.ThemeMode
import com.prism.unisonllm.ui.theme.ThemeSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * State for the onboarding flow.
 */
data class OnboardingUiState(
    val currentPage: Int = 0,
    val themeSettings: ThemeSettings = ThemeSettings(),
    val selectedProvider: ProviderType? = null,
    val apiKey: String = "",
    val customBaseUrl: String = "",
    val isValidatingKey: Boolean = false,
    val keyValidationResult: KeyValidationResult? = null,
    val isOnboardingComplete: Boolean = false
)

/**
 * Result of API key validation.
 */
sealed class KeyValidationResult {
    data object Success : KeyValidationResult()
    data class Error(val message: String) : KeyValidationResult()
}

/**
 * Intent actions for onboarding.
 */
sealed class OnboardingIntent {
    data class SetPage(val page: Int) : OnboardingIntent()
    data class SetThemeMode(val mode: ThemeMode) : OnboardingIntent()
    data class SetDynamicColor(val enabled: Boolean) : OnboardingIntent()
    data class SelectProvider(val provider: ProviderType) : OnboardingIntent()
    data class SetApiKey(val key: String) : OnboardingIntent()
    data class SetCustomBaseUrl(val url: String) : OnboardingIntent()
    data object ValidateApiKey : OnboardingIntent()
    data object CompleteOnboarding : OnboardingIntent()
    data object ClearValidationResult : OnboardingIntent()
}

/**
 * ViewModel for the onboarding flow.
 * Manages state for the setup wizard including theme preferences and API key validation.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val providerFactory: ChatProviderFactory
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    /**
     * Process user intents/actions.
     */
    fun processIntent(intent: OnboardingIntent) {
        when (intent) {
            is OnboardingIntent.SetPage -> setPage(intent.page)
            is OnboardingIntent.SetThemeMode -> setThemeMode(intent.mode)
            is OnboardingIntent.SetDynamicColor -> setDynamicColor(intent.enabled)
            is OnboardingIntent.SelectProvider -> selectProvider(intent.provider)
            is OnboardingIntent.SetApiKey -> setApiKey(intent.key)
            is OnboardingIntent.SetCustomBaseUrl -> setCustomBaseUrl(intent.url)
            is OnboardingIntent.ValidateApiKey -> validateApiKey()
            is OnboardingIntent.CompleteOnboarding -> completeOnboarding()
            is OnboardingIntent.ClearValidationResult -> clearValidationResult()
        }
    }

    private fun setPage(page: Int) {
        _uiState.update { it.copy(currentPage = page) }
    }

    private fun setThemeMode(mode: ThemeMode) {
        _uiState.update { currentState ->
            currentState.copy(
                themeSettings = currentState.themeSettings.copy(themeMode = mode)
            )
        }
    }

    private fun setDynamicColor(enabled: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(
                themeSettings = currentState.themeSettings.copy(useDynamicColor = enabled)
            )
        }
    }

    private fun selectProvider(provider: ProviderType) {
        _uiState.update { it.copy(selectedProvider = provider, keyValidationResult = null) }
    }

    private fun setApiKey(key: String) {
        _uiState.update { it.copy(apiKey = key, keyValidationResult = null) }
    }

    private fun setCustomBaseUrl(url: String) {
        _uiState.update { it.copy(customBaseUrl = url, keyValidationResult = null) }
    }

    private fun clearValidationResult() {
        _uiState.update { it.copy(keyValidationResult = null) }
    }

    private fun validateApiKey() {
        val currentState = _uiState.value
        val provider = currentState.selectedProvider ?: return

        if (currentState.apiKey.isBlank() && provider != ProviderType.CUSTOM) {
            _uiState.update { it.copy(keyValidationResult = KeyValidationResult.Error("API key is required")) }
            return
        }

        if (provider == ProviderType.CUSTOM && currentState.customBaseUrl.isBlank()) {
            _uiState.update { it.copy(keyValidationResult = KeyValidationResult.Error("Base URL is required for custom providers")) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isValidatingKey = true, keyValidationResult = null) }

            val config = ProviderConfig(
                type = provider,
                apiKey = currentState.apiKey,
                baseUrl = currentState.customBaseUrl
            )

            val chatProvider = providerFactory.createProvider(config)
            val result = chatProvider.validateApiKey()

            _uiState.update { state ->
                state.copy(
                    isValidatingKey = false,
                    keyValidationResult = when (result) {
                        is ProviderResult.Success -> KeyValidationResult.Success
                        is ProviderResult.Error -> KeyValidationResult.Error(result.message)
                    }
                )
            }
        }
    }

    private fun completeOnboarding() {
        _uiState.update { it.copy(isOnboardingComplete = true) }
        // TODO: Save settings to DataStore/EncryptedSharedPreferences
    }
}
