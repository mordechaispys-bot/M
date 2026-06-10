package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.CalculatorScreen
import com.example.ui.MediaViewerScreen
import com.example.ui.VaultScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.UnlockState
import com.example.viewmodel.VaultViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Initialize modern ViewModel
                val viewModel: VaultViewModel = viewModel()
                val unlockState by viewModel.unlockState.collectAsState()
                val selectedItem by viewModel.selectedItem.collectAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    BoxWithContent(
                        unlockState = unlockState,
                        selectedItem = selectedItem,
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun BoxWithContent(
    unlockState: UnlockState,
    selectedItem: com.example.data.VaultItem?,
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier
) {
    if (selectedItem != null) {
        // Full-screen media view overlay
        MediaViewerScreen(
            item = selectedItem,
            viewModel = viewModel,
            modifier = modifier
        )
    } else {
        // Crossfade animations between Decoy and Vault panels
        AnimatedContent(
            targetState = unlockState,
            transitionSpec = {
                fadeIn().togetherWith(fadeOut())
            },
            label = "ScreenTransition",
            modifier = modifier
        ) { state ->
            when (state) {
                UnlockState.UNLOCKED -> {
                    VaultScreen(viewModel = viewModel)
                }
                else -> {
                    // LOCKED, SETUP_PIN, CONFIRM_PIN
                    CalculatorScreen(viewModel = viewModel)
                }
            }
        }
    }
}
