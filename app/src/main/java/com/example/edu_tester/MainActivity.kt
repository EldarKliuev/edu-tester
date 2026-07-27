package com.example.edu_tester

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.edu_tester.presentation.navigation.EduTesterNavHost
import com.example.edu_tester.ui.theme.EdutesterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EdutesterTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    EduTesterNavHost()
                }
            }
        }
    }
}
