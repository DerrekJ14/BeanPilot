package com.beanpilot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.hilt.navigation.compose.hiltViewModel
import com.beanpilot.ui.dashboard.DashboardScreen
import com.beanpilot.ui.dashboard.DashboardViewModel
import com.beanpilot.ui.theme.BeanPilotTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BeanPilotTheme {
                val viewModel: DashboardViewModel = hiltViewModel()
                DashboardScreen(viewModel = viewModel)
            }
        }
    }
}
