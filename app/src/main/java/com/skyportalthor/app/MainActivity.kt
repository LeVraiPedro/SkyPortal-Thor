package com.skyportalthor.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.skyportalthor.app.display.DisplayRouter
import com.skyportalthor.app.ui.PortalColorScheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val router = DisplayRouter(this)
        val lowerDisplay = router.lowerDisplay()
        if (
            router.supportsSecondaryActivities() &&
            lowerDisplay != null &&
            router.launchOnDisplay(this, Intent(this, PortalActivity::class.java), lowerDisplay)
        ) {
            // MainActivity is only a trampoline. Closing it immediately leaves the upper panel
            // available for Dolphin instead of displaying a permanent companion landing page.
            finish()
            return
        }

        setContent {
            MaterialTheme(colorScheme = PortalColorScheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var status by remember {
                        mutableStateOf(
                            if (!router.supportsSecondaryActivities()) "Activités multi-écrans non prises en charge"
                            else "Écran inférieur non détecté"
                        )
                    }
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("SkyPortal Thor", style = MaterialTheme.typography.headlineMedium)
                        Text(status)
                        Text("Écrans : ${router.allDisplays().joinToString { "#${it.displayId} ${it.name}" }}")
                        Button(onClick = {
                            val target = router.lowerDisplay()
                            if (target != null && router.launchOnDisplay(
                                    this@MainActivity,
                                    Intent(this@MainActivity, PortalActivity::class.java),
                                    target
                                )
                            ) {
                                finish()
                            } else {
                                status = "Impossible de lancer SkyPortal sur l'écran inférieur"
                            }
                        }) {
                            Text("Réessayer sur l'écran inférieur")
                        }
                        Text("Allume les deux écrans de la Thor, puis réessaie.")
                    }
                }
            }
        }
    }
}
