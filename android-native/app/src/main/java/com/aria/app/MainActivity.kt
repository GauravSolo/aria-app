package com.aria.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aria.app.data.AppViewModel
import com.aria.app.data.AuthStatus
import com.aria.app.ui.AriaTheme
import com.aria.app.ui.AuthScreen
import com.aria.app.ui.MainScaffold

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AriaTheme {
                val vm: AppViewModel = viewModel()
                val status by vm.status.collectAsState()
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    when (status) {
                        AuthStatus.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                        AuthStatus.SignedOut -> AuthScreen(vm)
                        AuthStatus.SignedIn -> MainScaffold(vm)
                    }
                }
            }
        }
    }
}
