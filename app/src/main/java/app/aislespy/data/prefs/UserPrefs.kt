package app.aislespy.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPrefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = UserPrefs.DATA_STORE_NAME,
)

/**
 * Lightweight DataStore wrapper for user preferences (T-510).
 *
 * Theme preference is intentionally omitted — the app follows the system theme.
 * Key name [KEY_FIRST_LAUNCH_DONE] maps to UI_UX `onboarding_done`.
 */
class UserPrefs(
    private val dataStore: DataStore<Preferences>,
) {
    /** `true` after the user completes first-launch onboarding. */
    val firstLaunchDone: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_FIRST_LAUNCH_DONE] == true
    }

    /** Marks onboarding complete so subsequent launches go straight to scan. */
    suspend fun setFirstLaunchDone() {
        dataStore.edit { prefs ->
            prefs[KEY_FIRST_LAUNCH_DONE] = true
        }
    }

    companion object {
        const val DATA_STORE_NAME: String = "user_prefs"

        /** Preference key for completed first-launch onboarding (UI_UX: onboarding_done). */
        val KEY_FIRST_LAUNCH_DONE = booleanPreferencesKey("onboarding_done")

        fun create(context: Context): UserPrefs =
            UserPrefs(context.applicationContext.userPrefsDataStore)
    }
}
