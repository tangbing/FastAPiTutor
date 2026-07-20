package com.edy.rbaclab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edy.rbaclab.ui.RbacApp
import com.edy.rbaclab.ui.theme.RbacLabTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RbacLabTheme {
                val viewModel: RbacViewModel = viewModel(factory = RbacViewModel.factory(this))
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                RbacApp(state = state, viewModel = viewModel)
            }
        }
    }
}
