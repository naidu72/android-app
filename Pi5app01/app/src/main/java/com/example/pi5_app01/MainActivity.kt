package com.example.pi5_app01

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pi5_app01.ui.theme.Pi5app01Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Pi5app01Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CounterApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

/**
 * A simple counter app - perfect for learning Android basics!
 * 
 * This app demonstrates:
 * - State management (remember and mutableIntStateOf)
 * - User interface components (Text, Button)
 * - Layout (Column, Row, Spacer)
 * - User interactions (onClick)
 */
@Composable
fun CounterApp(modifier: Modifier = Modifier) {
    // This is called "state" - it remembers the counter value
    // When it changes, the UI automatically updates!
    var count by remember { mutableIntStateOf(0) }

    // Column arranges items vertically (top to bottom)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Title text
        Text(
            text = "Simple Counter App",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Display the current count
        Text(
            text = count.toString(),
            fontSize = 72.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Row arranges items horizontally (left to right)
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Decrease button
            Button(
                onClick = { count-- } // Decrease count by 1
            ) {
                Text("-", fontSize = 24.sp)
            }

            // Reset button
            Button(
                onClick = { count = 0 } // Reset to 0
            ) {
                Text("Reset", fontSize = 16.sp)
            }

            // Increase button
            Button(
                onClick = { count++ } // Increase count by 1
            ) {
                Text("+", fontSize = 24.sp)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Helpful hint text
        Text(
            text = "Tap the buttons to change the number!",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CounterAppPreview() {
    Pi5app01Theme {
        CounterApp()
    }
}