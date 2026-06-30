package com.alexbralves.walletmotion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.alexbralves.walletmotion.theme.WalletMotionTheme
import com.alexbralves.walletmotion.ui.WalletMotionScreen

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    setContent {
      WalletMotionTheme(dynamicColor = false) {
        WalletMotionScreen()
      }
    }
  }
}
