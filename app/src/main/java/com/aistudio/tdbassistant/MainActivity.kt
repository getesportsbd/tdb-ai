package com.aistudio.tdbassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aistudio.tdbassistant.ui.TdbViewModel
import com.aistudio.tdbassistant.ui.navigation.TdbAppRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Configure Firebase App Check with launch intent before any UI, ViewModel, or AI call
        configureAppCheck(this, intent)

        setContent {
            val viewModel: TdbViewModel = viewModel()
            TdbAppRoot(viewModel = viewModel)
        }
    }
}
