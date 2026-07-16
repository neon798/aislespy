package app.aislespy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import app.aislespy.di.AppContainer
import app.aislespy.ui.navigation.AisleSpyNavGraph
import app.aislespy.ui.theme.AisleSpyTheme

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
                    AisleSpyNavGraph()
                }
            }
        }
    }
}
