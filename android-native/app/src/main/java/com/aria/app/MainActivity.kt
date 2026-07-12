package com.aria.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
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
import com.aria.app.ui.MainRoot

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    /** Re-sync whenever the app comes to the foreground (e.g. after completing a
     *  task from the widget) so the UI reflects the latest data. */
    override fun onResume() {
        super.onResume()
        vm.refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
                .launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val vm: AppViewModel = viewModel()
            val mode by vm.themeMode.collectAsState()
            AriaTheme(mode = mode) {
                val status by vm.status.collectAsState()
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    when (status) {
                        AuthStatus.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                        AuthStatus.SignedOut -> AuthScreen(vm)
                        AuthStatus.SignedIn -> MainRoot(vm)
                    }
                }
            }
        }
    }
}
