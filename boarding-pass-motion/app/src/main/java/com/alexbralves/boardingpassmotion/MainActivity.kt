package com.alexbralves.boardingpassmotion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.alexbralves.boardingpassmotion.theme.BoardingPassMotionTheme
import com.alexbralves.boardingpassmotion.ui.screen.BoardingPassMotionScreen

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      BoardingPassMotionTheme {
        BoardingPassMotionScreen(autoExpand = intent?.getBooleanExtra("auto_expand", false) == true)
      }
    }
  }
}
