package com.cortex.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cortex.app.core.navigation.CortexNavHost
import com.cortex.app.core.ui.theme.CortexColors
import com.cortex.app.core.ui.theme.CortexTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { CortexApp() }
    }
}

@Composable
private fun CortexApp() {
    CortexTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(CortexColors.Paper),
            color = CortexColors.Paper,
        ) {
            CortexNavHost()
        }
    }
}
