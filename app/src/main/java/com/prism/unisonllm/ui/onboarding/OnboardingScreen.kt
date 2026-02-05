package com.prism.unisonllm.ui.onboarding

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prism.unisonllm.data.remote.provider.ProviderType
import com.prism.unisonllm.ui.theme.ProviderColors
import com.prism.unisonllm.ui.theme.ThemeMode
import kotlinx.coroutines.launch

/**
 * Number of pages in the onboarding flow.
 */
private const val ONBOARDING_PAGE_COUNT = 3

/**
 * Main onboarding screen with HorizontalPager for the setup wizard.
 */
@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    onIntent: (OnboardingIntent) -> Unit,
    onOnboardingComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(
        initialPage = uiState.currentPage,
        pageCount = { ONBOARDING_PAGE_COUNT }
    )
    val scope = rememberCoroutineScope()

    // Sync pager state with ViewModel
    LaunchedEffect(pagerState.currentPage) {
        onIntent(OnboardingIntent.SetPage(pagerState.currentPage))
    }

    // Navigate when onboarding completes
    LaunchedEffect(uiState.isOnboardingComplete) {
        if (uiState.isOnboardingComplete) {
            onOnboardingComplete()
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Page indicator
            PageIndicator(
                pageCount = ONBOARDING_PAGE_COUNT,
                currentPage = pagerState.currentPage,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, bottom = 16.dp)
            )

            // Pager content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 24.dp),
                pageSpacing = 16.dp
            ) { page ->
                AnimatedContent(
                    targetState = page,
                    transitionSpec = {
                        (fadeIn() + slideInHorizontally { it / 2 }) togetherWith
                                (fadeOut() + slideOutHorizontally { -it / 2 })
                    },
                    label = "page_content"
                ) { targetPage ->
                    when (targetPage) {
                        0 -> WelcomePage()
                        1 -> ThemeSelectionPage(
                            themeSettings = uiState.themeSettings,
                            onThemeModeChange = { onIntent(OnboardingIntent.SetThemeMode(it)) },
                            onDynamicColorChange = { onIntent(OnboardingIntent.SetDynamicColor(it)) }
                        )
                        2 -> ProviderSetupPage(
                            uiState = uiState,
                            onIntent = onIntent
                        )
                    }
                }
            }

            // Navigation buttons
            NavigationButtons(
                currentPage = pagerState.currentPage,
                canProceed = canProceedFromPage(pagerState.currentPage, uiState),
                isLastPage = pagerState.currentPage == ONBOARDING_PAGE_COUNT - 1,
                onBack = {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    }
                },
                onNext = {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                },
                onComplete = { onIntent(OnboardingIntent.CompleteOnboarding) },
                modifier = Modifier.padding(24.dp)
            )
        }
    }
}

/**
 * Determines if the user can proceed from the current page.
 */
private fun canProceedFromPage(page: Int, state: OnboardingUiState): Boolean {
    return when (page) {
        0 -> true // Welcome page always allows proceeding
        1 -> true // Theme page always allows proceeding
        2 -> {
            // Provider page requires valid key
            state.selectedProvider != null &&
                    state.keyValidationResult is KeyValidationResult.Success
        }
        else -> true
    }
}

/**
 * Page indicator dots.
 */
@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            val width by animateDpAsState(
                targetValue = if (isSelected) 24.dp else 8.dp,
                label = "indicator_width"
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .height(8.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
            )
        }
    }
}

/**
 * Navigation buttons for the onboarding flow.
 */
@Composable
private fun NavigationButtons(
    currentPage: Int,
    canProceed: Boolean,
    isLastPage: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back button (hidden on first page)
        AnimatedVisibility(visible = currentPage > 0) {
            OutlinedButton(onClick = onBack) {
                Text("Back")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Next/Complete button
        Button(
            onClick = if (isLastPage) onComplete else onNext,
            enabled = canProceed
        ) {
            Text(if (isLastPage) "Get Started" else "Next")
            if (!isLastPage) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Step 1: Welcome page with branding.
 */
@Composable
private fun WelcomePage(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App icon placeholder
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Welcome to UnisonLLM",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Your universal bridge to AI.\nConnect to OpenAI, Anthropic, Google, and more.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Feature highlights
        FeatureHighlight(
            icon = Icons.Default.Palette,
            title = "Material You",
            description = "Beautiful dynamic theming"
        )
        Spacer(modifier = Modifier.height(16.dp))
        FeatureHighlight(
            icon = Icons.Default.Settings,
            title = "Multi-Provider",
            description = "One app, multiple AI services"
        )
    }
}

@Composable
private fun FeatureHighlight(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Step 2: Theme and Material You preference selection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSelectionPage(
    themeSettings: com.prism.unisonllm.ui.theme.ThemeSettings,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 24.dp)
    ) {
        item {
            Text(
                text = "Customize Your Theme",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Choose how UnisonLLM looks and feels",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Theme mode selection
        item {
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ThemeModeCard(
                    title = "Light",
                    icon = Icons.Default.LightMode,
                    isSelected = themeSettings.themeMode == ThemeMode.LIGHT,
                    onClick = { onThemeModeChange(ThemeMode.LIGHT) },
                    modifier = Modifier.weight(1f)
                )
                ThemeModeCard(
                    title = "Dark",
                    icon = Icons.Default.DarkMode,
                    isSelected = themeSettings.themeMode == ThemeMode.DARK,
                    onClick = { onThemeModeChange(ThemeMode.DARK) },
                    modifier = Modifier.weight(1f)
                )
                ThemeModeCard(
                    title = "System",
                    icon = Icons.Default.Settings,
                    isSelected = themeSettings.themeMode == ThemeMode.SYSTEM,
                    onClick = { onThemeModeChange(ThemeMode.SYSTEM) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Material You toggle
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Material You",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Match colors to your wallpaper",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                            Text(
                                text = "Requires Android 12+",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    Switch(
                        checked = themeSettings.useDynamicColor,
                        onCheckedChange = onDynamicColorChange,
                        enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeModeCard(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .then(
                if (isSelected) Modifier.border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(16.dp)
                ) else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Step 3: Provider selection and API key validation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderSetupPage(
    uiState: OnboardingUiState,
    onIntent: (OnboardingIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    var isKeyVisible by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 24.dp)
    ) {
        item {
            Text(
                text = "Connect Your AI Provider",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Select a provider and enter your API key",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Provider selection chips
        item {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Provider",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProviderChip(
                        label = "OpenAI",
                        isSelected = uiState.selectedProvider == ProviderType.OPENAI,
                        color = ProviderColors.OpenAI,
                        onClick = { onIntent(OnboardingIntent.SelectProvider(ProviderType.OPENAI)) }
                    )
                    ProviderChip(
                        label = "Anthropic",
                        isSelected = uiState.selectedProvider == ProviderType.ANTHROPIC,
                        color = ProviderColors.Anthropic,
                        onClick = { onIntent(OnboardingIntent.SelectProvider(ProviderType.ANTHROPIC)) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProviderChip(
                        label = "Google",
                        isSelected = uiState.selectedProvider == ProviderType.GOOGLE,
                        color = ProviderColors.Google,
                        onClick = { onIntent(OnboardingIntent.SelectProvider(ProviderType.GOOGLE)) }
                    )
                    ProviderChip(
                        label = "OpenRouter",
                        isSelected = uiState.selectedProvider == ProviderType.OPENROUTER,
                        color = ProviderColors.OpenRouter,
                        onClick = { onIntent(OnboardingIntent.SelectProvider(ProviderType.OPENROUTER)) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                ProviderChip(
                    label = "Custom",
                    isSelected = uiState.selectedProvider == ProviderType.CUSTOM,
                    color = ProviderColors.Custom,
                    onClick = { onIntent(OnboardingIntent.SelectProvider(ProviderType.CUSTOM)) }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // API Key input
        item {
            AnimatedVisibility(visible = uiState.selectedProvider != null) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Custom base URL field (only for Custom provider)
                    AnimatedVisibility(visible = uiState.selectedProvider == ProviderType.CUSTOM) {
                        Column {
                            OutlinedTextField(
                                value = uiState.customBaseUrl,
                                onValueChange = { onIntent(OnboardingIntent.SetCustomBaseUrl(it)) },
                                label = { Text("Base URL") },
                                placeholder = { Text("https://api.example.com/v1") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // API Key field
                    OutlinedTextField(
                        value = uiState.apiKey,
                        onValueChange = { onIntent(OnboardingIntent.SetApiKey(it)) },
                        label = { Text("API Key") },
                        placeholder = { Text(getApiKeyPlaceholder(uiState.selectedProvider)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (isKeyVisible)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                Icon(
                                    imageVector = if (isKeyVisible)
                                        Icons.Default.VisibilityOff
                                    else
                                        Icons.Default.Visibility,
                                    contentDescription = if (isKeyVisible)
                                        "Hide API key"
                                    else
                                        "Show API key"
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        isError = uiState.keyValidationResult is KeyValidationResult.Error
                    )

                    // Validation result message
                    AnimatedVisibility(visible = uiState.keyValidationResult != null) {
                        val result = uiState.keyValidationResult
                        Row(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            when (result) {
                                is KeyValidationResult.Success -> {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "API key validated successfully",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                is KeyValidationResult.Error -> {
                                    Text(
                                        text = result.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                null -> {}
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Validate button
                    Button(
                        onClick = { onIntent(OnboardingIntent.ValidateApiKey) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isValidatingKey &&
                                uiState.apiKey.isNotBlank() &&
                                (uiState.selectedProvider != ProviderType.CUSTOM ||
                                        uiState.customBaseUrl.isNotBlank())
                    ) {
                        if (uiState.isValidatingKey) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Validate API Key")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Security note
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "🔒 Your API key is stored securely using Android's EncryptedSharedPreferences and never leaves your device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderChip(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = color.copy(alpha = 0.2f),
            selectedLabelColor = color
        ),
        leadingIcon = if (isSelected) {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = color
                )
            }
        } else null
    )
}

private fun getApiKeyPlaceholder(provider: ProviderType?): String {
    return when (provider) {
        ProviderType.OPENAI -> "sk-..."
        ProviderType.ANTHROPIC -> "sk-ant-..."
        ProviderType.GOOGLE -> "AIza..."
        ProviderType.OPENROUTER -> "sk-or-..."
        ProviderType.CUSTOM -> "Your API key"
        null -> ""
    }
}
