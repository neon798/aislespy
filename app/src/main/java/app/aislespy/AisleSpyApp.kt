package app.aislespy

import android.app.Application
import app.aislespy.di.AppContainer

/**
 * Application entry point. Owns the [AppContainer] composition root for the process lifetime.
 */
class AisleSpyApp : Application() {

    /** Lazily created manual DI container; first access is typically from [MainActivity]. */
    val container: AppContainer by lazy { AppContainer(this) }
}
