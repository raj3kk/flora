package com.getmaya.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

// Skeleton only — tabs Home / Scan / Mic / Memories / Chat are placeholders.
// Real screens will be re-implemented from behaviour spec + jadx-verified structure.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MayaHomePlaceholder() }
    }
}

@Composable
fun MayaHomePlaceholder() {
    Text("Maya rebuild — skeleton. Screens pending jadx-verified re-implementation.")
}
