package app.aislespy.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.aislespy.ui.history.HistoryScreen
import app.aislespy.ui.ingredient.IngredientDetailScreen
import app.aislespy.ui.onboarding.OnboardingScreen
import app.aislespy.ui.result.CategoryChooserScreen
import app.aislespy.ui.result.ResultScreen
import app.aislespy.ui.result.ResultViewModel
import app.aislespy.ui.scan.ManualEntryScreen
import app.aislespy.ui.scan.ScanScreen
import app.aislespy.ui.settings.LicensesScreen
import app.aislespy.ui.settings.MethodologyScreen
import app.aislespy.ui.settings.PrivacyScreen
import app.aislespy.ui.settings.SettingsScreen

/**
 * Route patterns from docs/UI_UX.md (route table).
 * Keep in sync; do not invent routes without updating the doc.
 */
object Routes {
    const val ONBOARDING = "onboarding"
    const val SCAN = "scan"
    const val MANUAL = "manual"
    const val RESULT = "result/{barcode}?source={source}"
    const val CHOOSE = "choose/{barcode}"
    const val INGREDIENT = "ingredient/{concernId}"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val METHODOLOGY = "settings/methodology"
    const val PRIVACY = "settings/privacy"
    const val LICENSES = "settings/licenses"

    const val ARG_BARCODE = "barcode"
    const val ARG_SOURCE = "source"
    const val ARG_CONCERN_ID = "concernId"

    const val SOURCE_DEFAULT = "auto"

    fun result(barcode: String, source: String = SOURCE_DEFAULT): String =
        "result/$barcode?source=$source"

    fun choose(barcode: String): String = "choose/$barcode"

    fun ingredient(concernId: String): String = "ingredient/$concernId"
}

private data class TopLevelDestination(
    val route: String,
    val label: String,
    val iconGlyph: String,
)

private val topLevelDestinations = listOf(
    TopLevelDestination(Routes.SCAN, "Scan", "S"),
    TopLevelDestination(Routes.HISTORY, "History", "H"),
    TopLevelDestination(Routes.SETTINGS, "Settings", "⚙"),
)

private val bottomBarRoutes = topLevelDestinations.map { it.route }.toSet()

@Composable
fun AisleSpyNavGraph(
    firstLaunchDone: Boolean,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = currentDestination?.route in bottomBarRoutes
    val startDestination = if (firstLaunchDone) Routes.SCAN else Routes.ONBOARDING

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    topLevelDestinations.forEach { dest ->
                        val selected = currentDestination
                            ?.hierarchy
                            ?.any { it.route == dest.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Text(dest.iconGlyph) },
                            label = { Text(dest.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onFinished = {
                        navController.navigate(Routes.SCAN) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.SCAN) {
                ScanScreen(
                    onManualEntry = {
                        navController.navigate(Routes.MANUAL)
                    },
                    onBarcodeDecoded = { barcode ->
                        navController.navigate(Routes.result(barcode)) {
                            // Avoid stacking duplicate result destinations from a double-fire.
                            launchSingleTop = true
                        }
                    },
                    onSettings = {
                        navController.navigate(Routes.SETTINGS) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onHistory = {
                        navController.navigate(Routes.HISTORY) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onRecentClick = { barcode, source ->
                        navController.navigate(Routes.result(barcode, source)) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.MANUAL) {
                ManualEntryScreen(
                    onLookup = { barcode ->
                        navController.navigate(Routes.result(barcode))
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = Routes.RESULT,
                arguments = listOf(
                    navArgument(Routes.ARG_BARCODE) { type = NavType.StringType },
                    navArgument(Routes.ARG_SOURCE) {
                        type = NavType.StringType
                        defaultValue = Routes.SOURCE_DEFAULT
                    },
                ),
            ) { entry ->
                val barcode = entry.arguments?.getString(Routes.ARG_BARCODE).orEmpty()
                val source = entry.arguments?.getString(Routes.ARG_SOURCE)
                    ?: Routes.SOURCE_DEFAULT
                ResultScreen(
                    barcode = barcode,
                    source = source,
                    onBack = { navController.popBackStack() },
                    onScanAnother = {
                        navController.popBackStack(Routes.SCAN, inclusive = false)
                    },
                    onConcernClick = { concernId ->
                        navController.navigate(Routes.ingredient(concernId))
                    },
                    onMethodology = {
                        navController.navigate(Routes.METHODOLOGY)
                    },
                    onNavigateToCategoryChooser = { chooseBarcode ->
                        // Replace this result entry so config change cannot re-fire
                        // navigation, and back from chooser returns to scan (not loading).
                        navController.navigate(Routes.choose(chooseBarcode)) {
                            popUpTo(Routes.RESULT) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(
                route = Routes.CHOOSE,
                arguments = listOf(
                    navArgument(Routes.ARG_BARCODE) { type = NavType.StringType },
                ),
            ) { entry ->
                val barcode = entry.arguments?.getString(Routes.ARG_BARCODE).orEmpty()
                CategoryChooserScreen(
                    barcode = barcode,
                    onChooseFood = {
                        navController.navigate(
                            Routes.result(barcode, source = ResultViewModel.SOURCE_FOOD),
                        ) {
                            popUpTo(Routes.CHOOSE) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onChooseBeauty = {
                        navController.navigate(
                            Routes.result(barcode, source = ResultViewModel.SOURCE_BEAUTY),
                        ) {
                            popUpTo(Routes.CHOOSE) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onCancel = { navController.popBackStack() },
                )
            }
            composable(
                route = Routes.INGREDIENT,
                arguments = listOf(
                    navArgument(Routes.ARG_CONCERN_ID) { type = NavType.StringType },
                ),
            ) { entry ->
                val concernId = entry.arguments?.getString(Routes.ARG_CONCERN_ID).orEmpty()
                IngredientDetailScreen(
                    concernId = concernId,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.HISTORY) {
                HistoryScreen(
                    onOpenResult = { barcode, source ->
                        navController.navigate(Routes.result(barcode, source)) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onMethodology = { navController.navigate(Routes.METHODOLOGY) },
                    onPrivacy = { navController.navigate(Routes.PRIVACY) },
                    onLicenses = { navController.navigate(Routes.LICENSES) },
                )
            }
            composable(Routes.METHODOLOGY) {
                MethodologyScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.PRIVACY) {
                PrivacyScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.LICENSES) {
                LicensesScreen(
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
