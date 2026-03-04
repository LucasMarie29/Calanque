package com.example.calanque

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.calanque.navigation.AppNavigation
import com.example.calanque.ui.theme.CalanqueTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalanqueTheme {
                AppNavigation()
            }
        }
    }
}