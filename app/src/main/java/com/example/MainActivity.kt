package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import com.example.ui.MainViewModel
import com.example.ui.screens.AppNavigation
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize the master MVVM controller
        val viewModel = ViewModelProvider(
            this, 
            MainViewModel.Factory(applicationContext)
        )[MainViewModel::class.java]
        
        enableEdgeToEdge()
        
        setContent {
            val selectedTheme by viewModel.selectedTheme.collectAsState()
            MyApplicationTheme(appTheme = selectedTheme) {
                AppNavigation(viewModel = viewModel)
            }
        }
    }
}
