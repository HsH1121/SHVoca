package com.baekseok.shvoca

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.baekseok.shvoca.ui.KanjiCardScreen
import com.baekseok.shvoca.ui.LanguageSelectScreen
import com.baekseok.shvoca.ui.TestScreen
import com.baekseok.shvoca.ui.WordListScreen
import com.baekseok.shvoca.ui.theme.SHVOCATheme
import com.baekseok.shvoca.ui.theme.Paper

private enum class Screen { Language, WordList, Cards, Test }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SHVOCATheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Paper) {
                    var screen         by remember { mutableStateOf(Screen.Language) }
                    var language       by remember { mutableStateOf("") }
                    var cardStartIndex by remember { mutableStateOf(0) }
                    var cardShuffled   by remember { mutableStateOf(false) }
                    var cardWordIds    by remember { mutableStateOf<List<Int>?>(null) }

                    when (screen) {
                        Screen.Language -> LanguageSelectScreen(
                            onLanguageSelected = {
                                language = it
                                screen = Screen.WordList
                            }
                        )
                        Screen.WordList -> WordListScreen(
                            language = language,
                            onBack = { screen = Screen.Language },
                            onWordSelected = { index, wordIds ->
                                cardStartIndex = index
                                cardShuffled   = false
                                cardWordIds    = wordIds
                                screen         = Screen.Cards
                            },
                            onShuffle = { wordIds ->
                                cardStartIndex = 0
                                cardShuffled   = true
                                cardWordIds    = wordIds
                                screen         = Screen.Cards
                            },
                            onTest = { screen = Screen.Test },
                        )
                        Screen.Cards -> KanjiCardScreen(
                            language   = language,
                            startIndex = cardStartIndex,
                            shuffled   = cardShuffled,
                            wordIds    = cardWordIds,
                            onBack     = { screen = Screen.WordList }
                        )
                        Screen.Test -> TestScreen(
                            language = language,
                            onBack   = { screen = Screen.WordList }
                        )
                    }
                }
            }
        }
    }
}
