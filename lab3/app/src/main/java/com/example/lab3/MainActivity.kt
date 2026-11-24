package com.example.lab3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.lab3.data.FinanceSampleData
import com.example.lab3.data.FinanceUiState
import com.example.lab3.data.ThemeMode
import com.example.lab3.data.ThemePreferences
import com.example.lab3.data.TransactionItem
import com.example.lab3.ui.screens.FinanceOverviewScreen
import com.example.lab3.ui.screens.TransactionDetailScreen
import com.example.lab3.ui.theme.FinanceTheme

/**
 * Точка входа приложения на Jetpack Compose.
 * Храню здесь верхнеуровневое состояние (режим темы, Material You, текущее состояние данных
 * и выбранную операцию) и передаю всё вниз в экранные функции.
 */
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val themePrefs = ThemePreferences(this)

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            var themeMode by remember { mutableStateOf(themePrefs.loadThemeMode()) }
            var dynamicColor by remember { mutableStateOf(themePrefs.loadDynamicColor()) }
            var uiState by remember { mutableStateOf<FinanceUiState>(FinanceSampleData.sampleContent) }
            var selectedTx by remember { mutableStateOf<TransactionItem?>(null) }

            val isWide = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

            // На телефоне кнопка «Назад» закрывает экран деталей и возвращает на обзор
            BackHandler(enabled = !isWide && selectedTx != null) {
                selectedTx = null
            }

            FinanceTheme(
                themeMode = themeMode,
                dynamicColor = dynamicColor
            ) {
                val showSeparateDetailScreen = !isWide && selectedTx != null

                // Делаю плавный переход со сдвигом между списком и экраном деталей
                AnimatedContent(
                    targetState = showSeparateDetailScreen,
                    transitionSpec = {
                        if (targetState) {
                            (slideInHorizontally { it / 3 } + fadeIn()) togetherWith fadeOut()
                        } else {
                            fadeIn() togetherWith (slideOutHorizontally { it / 3 } + fadeOut())
                        }
                    },
                    label = "screen_navigation"
                ) { showingDetail ->
                    val currentTx = selectedTx
                    if (showingDetail && currentTx != null) {
                        TransactionDetailScreen(
                            item = currentTx,
                            onBack = { selectedTx = null }
                        )
                    } else {
                        FinanceOverviewScreen(
                            uiState = uiState,
                            windowWidthSizeClass = windowSizeClass.widthSizeClass,
                            themeMode = themeMode,
                            dynamicColor = dynamicColor,
                            selectedTransaction = selectedTx,
                            onCycleThemeMode = {
                                val next = when (themeMode) {
                                    ThemeMode.SYSTEM -> ThemeMode.DARK
                                    ThemeMode.DARK -> ThemeMode.LIGHT
                                    ThemeMode.LIGHT -> ThemeMode.SYSTEM
                                }
                                themeMode = next
                                themePrefs.saveThemeMode(next)
                            },
                            onToggleDynamicColor = {
                                val next = !dynamicColor
                                dynamicColor = next
                                themePrefs.saveDynamicColor(next)
                            },
                            onSelectStateTab = { tab ->
                                uiState = when (tab) {
                                    "loading" -> FinanceUiState.Loading
                                    "empty" -> FinanceUiState.Empty
                                    else -> FinanceSampleData.sampleContent
                                }
                                if (tab != "content") selectedTx = null
                            },
                            onTransactionClick = { tx ->
                                selectedTx = tx
                            }
                        )
                    }
                }
            }
        }
    }
}
