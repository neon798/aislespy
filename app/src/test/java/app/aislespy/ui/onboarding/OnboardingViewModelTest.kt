package app.aislespy.ui.onboarding

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import app.aislespy.data.prefs.UserPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Completing onboarding sets the DataStore first-launch flag (T-510).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var userPrefs: UserPrefs

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        dataStoreScope = CoroutineScope(testDispatcher + Job())
        dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { tmpFolder.newFile("user_prefs.preferences_pb") },
        )
        userPrefs = UserPrefs(dataStore)
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun onStartScanning_setsFirstLaunchDoneFlag() = runTest {
        assertFalse(userPrefs.firstLaunchDone.first())

        val vm = OnboardingViewModel(userPrefs = userPrefs)
        vm.onStartScanning()
        advanceUntilIdle()

        assertTrue(
            "completing onboarding should persist firstLaunchDone",
            userPrefs.firstLaunchDone.first(),
        )
    }

    @Test
    fun onStartScanning_emitsNavigateToScanEvent() = runTest {
        val vm = OnboardingViewModel(userPrefs = userPrefs)
        var received: OnboardingEvent? = null
        val collectJob = launch {
            vm.events.collect { received = it }
        }

        vm.onStartScanning()
        advanceUntilIdle()

        assertTrue(received is OnboardingEvent.NavigateToScan)
        collectJob.cancel()
    }
}
