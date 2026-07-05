package com.alexbralves.musicplay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.alexbralves.musicplay.theme.MusicPlayTheme
import com.alexbralves.musicplay.ui.MusicPlayerShowcaseScreen

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    setContent {
      MusicPlayTheme(dynamicColor = false) {
        MusicPlayerShowcaseScreen()
      }
    }
  }
}
