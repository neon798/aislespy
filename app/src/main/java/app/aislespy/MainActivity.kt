package app.aislespy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.aislespy.di.AppContainer
import app.aislespy.ui.navigation.AisleSpyNavGraph
import app.aislespy.ui.theme.AisleSpyTheme
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {

    /** Application-scoped composition root; wiring grows as features land. */
    lateinit var container: AppContainer
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        container = (application as AisleSpyApp).container
        enableEdgeToEdge()
        setContent {
            AisleSpyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    // Wait for first DataStore emission so we don't flash onboarding for
                    // returning users (or scan for first-time users).
                    val firstLaunchDone by produceState<Boolean?>(initialValue = null) {
                        value = container.userPrefs.firstLaunchDone.first()
                    }
                    when (val done = firstLaunchDone) {
                        null -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        else -> {
                            AisleSpyNavGraph(firstLaunchDone = done)
                        }
                    }
                }
            }
        }
    }
}
