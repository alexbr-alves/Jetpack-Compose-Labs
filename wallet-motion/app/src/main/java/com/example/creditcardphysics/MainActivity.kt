package com.example.creditcardphysics

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.creditcardphysics.theme.CreditCardPhysicsTheme
import com.example.creditcardphysics.ui.CreditCardPhysicsScreen

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    setContent {
      CreditCardPhysicsTheme(dynamicColor = false) {
        CreditCardPhysicsScreen()
      }
    }
  }
}
