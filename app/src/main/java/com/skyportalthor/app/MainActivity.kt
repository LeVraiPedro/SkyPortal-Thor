package com.skyportalthor.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Display
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
        val secondary = router.secondaryDisplay(currentDisplayId())
        if (router.supportsSecondaryActivities() && secondary != null) {
            router.launchOnDisplay(this, Intent(this, PortalActivity::class.java), secondary)
        }

        setContent {
            MaterialTheme(colorScheme = PortalColorScheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var status by remember {
                        mutableStateOf(
                            if (secondary != null) "SkyPortal lancé sur l'écran #${secondary.displayId}"
                            else "Second écran non détecté"
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
                            val target = router.secondaryDisplay(currentDisplayId())
                            if (target != null) {
                                router.launchOnDisplay(this@MainActivity, Intent(this@MainActivity, PortalActivity::class.java), target)
                                status = "SkyPortal lancé sur #${target.displayId}"
                            } else status = "Impossible de trouver l'écran inférieur"
                        }) {
                            Text("Relancer SkyPortal en bas")
                        }
                        Text("Tu peux maintenant revenir à l'accueil sur l'écran du haut et lancer Dolphin.")
                    }
                }
            }
        }
    }

    private fun currentDisplayId(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        display?.displayId ?: Display.DEFAULT_DISPLAY
    } else {
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.displayId
    }
}
