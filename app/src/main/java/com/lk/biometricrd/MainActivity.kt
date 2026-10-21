package com.lk.biometricrd

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.lk.biometricrd.core.navigation.AppNavSetup
import com.lk.biometricrd.ui.theme.BiometricRDTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BiometricRDTheme {
                AppNavSetup()
            }
        }
    }
}
