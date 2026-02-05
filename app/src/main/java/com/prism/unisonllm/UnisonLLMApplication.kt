package com.prism.unisonllm

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for UnisonLLM.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 */
@HiltAndroidApp
class UnisonLLMApplication : Application()
