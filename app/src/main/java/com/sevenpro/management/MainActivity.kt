package com.sevenpro.management

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sevenpro.management.data.local.UserPreferences
import com.sevenpro.management.ui.navigation.SevenProNavHost
import com.sevenpro.management.ui.theme.SevenProTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Locale
import androidx.compose.ui.res.LocaleList

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val userPrefs = UserPreferences(this)

        setContent {
            val darkMode by userPrefs.darkMode.collectAsState(initial = null)
            val language by userPrefs.language.collectAsState(initial = null)

            val isDark = darkMode ?: isSystemInDarkTheme()
            val locale = if (language == "ar") Locale("ar") else Locale("en")
            val layoutDirection = if (language == "ar") {
                androidx.compose.ui.unit.LayoutDirection.Rtl
            } else {
                androidx.compose.ui.unit.LayoutDirection.Ltr
            }

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                SevenProTheme(darkTheme = isDark) {
                    SevenProNavHost(
                        userPreferences = userPrefs,
                        supabaseClient = (application as SevenProApp).supabaseClient
                    )
                }
            }
        }
    }
}
