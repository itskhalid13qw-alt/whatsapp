package com.messagestudio.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.messagestudio.app.ui.StudioApp
import com.messagestudio.app.ui.StudioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StudioTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    StudioApp()
                }
            }
        }
    }
}
