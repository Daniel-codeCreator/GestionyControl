package com.universidad.sigelab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.universidad.sigelab.core.session.SessionManager
import com.universidad.sigelab.ui.navigation.SigelabNavHost
import com.universidad.sigelab.ui.theme.SigelabTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SigelabTheme {
                SigelabNavHost(sessionManager)
            }
        }
    }
}
