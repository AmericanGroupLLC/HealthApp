package com.myhealth.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import androidx.compose.animation.*
import com.myhealth.app.ui.activity.ActivityListScreen
import com.myhealth.app.ui.anatomy.AnatomyScreen
import com.myhealth.app.ui.articles.ArticlesScreen
import com.myhealth.app.ui.care.CareHomeScreen
import com.myhealth.app.ui.care.DoctorDetailScreen
import com.myhealth.app.ui.care.DoctorFinderScreen
import com.myhealth.app.ui.care.InsuranceCardScreen
import com.myhealth.app.ui.care.LabReportScreen
import com.myhealth.app.ui.care.MyChartConnectScreen
import com.myhealth.app.ui.care.MyChartDataScreen
import com.myhealth.app.ui.care.AnnualReportsScreen
import com.myhealth.app.ui.care.MoodTrackingScreen
import com.myhealth.app.ui.care.SymptomsLogScreen
import com.myhealth.app.ui.diary.DiaryScreen
import com.myhealth.app.ui.diet.DietHomeScreen
import com.myhealth.app.ui.diet.DietSuggestionsScreen
import com.myhealth.app.ui.diet.FoodDiaryScreen
import com.myhealth.app.ui.diet.MealPlanDetailScreen
import com.myhealth.app.ui.diet.OrderCheckoutScreen
import com.myhealth.app.ui.diet.VendorBrowseScreen
import com.myhealth.app.ui.diet.WaterTrackerScreen
import com.myhealth.app.ui.home.HomeScreen
import com.myhealth.app.ui.medicine.MedicineListScreen
import com.myhealth.app.ui.more.MoreScreen
import com.myhealth.app.ui.news.NewsDrawerSheet
import com.myhealth.app.ui.onboarding.OnboardingScreen
import com.myhealth.app.ui.profile.ProfileScreen
import com.myhealth.app.ui.run.LiveRunScreen
import com.myhealth.app.ui.run.RunDetailScreen
import com.myhealth.app.ui.run.RunTrackerScreen
import com.myhealth.app.ui.settings.SettingsScreen
import com.myhealth.app.ui.sleep.SleepScreen
import com.myhealth.app.ui.social.ChallengeDetailScreen
import com.myhealth.app.ui.social.CommunityHubScreen
import com.myhealth.app.ui.theme.CareTab
import com.myhealth.app.ui.train.ExerciseDetailScreen
import com.myhealth.app.ui.train.ExerciseLibraryScreen
import com.myhealth.app.ui.train.ProgressReportScreen
import com.myhealth.app.ui.train.RecoveryDayScreen
import com.myhealth.app.ui.train.StandupTimerScreen
import com.myhealth.app.ui.train.TodayPlanScreen
import com.myhealth.app.ui.train.TrainHomeScreen
import com.myhealth.app.ui.train.TrainScreen
import com.myhealth.app.ui.train.WorkoutLoggerScreen
import com.myhealth.app.ui.vitals.BiologicalAgeScreen
import com.myhealth.app.ui.vitals.VitalsScreen
import com.myhealth.app.ui.workout.WellnessInsightsScreen
import com.myhealth.app.ui.workout.WorkoutHomeScreen

const val ONBOARDING_KEY = "did_onboard"
val ONBOARDING_PREF = booleanPreferencesKey(ONBOARDING_KEY)

object Routes {
    const val ONBOARD = "onboard"

    // Care+ primary tabs
    const val CARE = "care"
    const val DIET = "diet"
    const val TRAIN = "train"
    const val WORKOUT = "workout"

    // Global header destinations
    const val PROFILE = "profile"
    const val NEWS_DRAWER = "news_drawer"
    const val SETTINGS = "settings"

    // Care sub-routes
    const val MYCHART_CONNECT = "mychart_connect"
    const val MYCHART_DATA = "mychart_data"
    const val INSURANCE_CARD = "insurance_card"
    const val LAB_REPORT = "lab_report"
    const val DOCTOR_FINDER = "doctor_finder"
    const val DOCTOR_DETAIL = "doctor_detail" // doctor_detail/{npi}
    const val ANNUAL_REPORTS = "annual_reports"
    const val SYMPTOMS_LOG = "symptoms_log"
    const val MOOD_TRACKING = "mood_tracking"

    // Diet sub-routes
    const val VENDOR_BROWSE = "vendor_browse"
    const val MEAL_PLAN_DETAIL = "meal_plan_detail"
    const val ORDER_CHECKOUT = "order_checkout"
    const val MEAL_LOG_ENTRY = "meal_log_entry"
    const val WATER_TRACKER = "water_tracker"
    const val DIET_SUGGESTIONS = "diet_suggestions"
    const val FOOD_DIARY = "food_diary"

    // Train sub-routes
    const val STANDUP_TIMER = "standup_timer"
    const val TODAY_PLAN = "today_plan"
    const val EXERCISE_DETAIL = "exercise_detail"
    const val WORKOUT_LIBRARY = "workout_library"
    const val PROGRESS_REPORT = "progress_report"
    const val RECOVERY_DAY = "recovery_day"

    // Workout sub-routes
    const val RUN_TRACKER = "run_tracker"
    const val WORKOUT_LOGGER = "workout_logger"
    const val SLEEP = "sleep"
    const val WELLNESS_INSIGHTS = "wellness_insights"
    const val COMMUNITY_HUB = "community_hub"
    const val CHALLENGE_DETAIL = "challenge_detail"

    // Existing destinations kept reachable via Profile / inner nav
    const val MEDICINE = "medicine"
    const val ACTIVITY = "activity"
    const val ARTICLES = "articles"
    const val VITALS = "vitals"
    const val BIO_AGE = "bio_age"
    const val ANATOMY = "anatomy"
    const val MORE = "more"
    const val HOME = "home"
}

private data class TabItem(val route: String, val tab: CareTab,
                           val icon: @Composable () -> Unit, val label: String)

@Composable
fun MyHealthRoot(rootViewModel: RootViewModel = hiltViewModel()) {
    val didOnboard by rootViewModel.didOnboard.collectAsStateWithLifecycle()
    if (!didOnboard) {
        OnboardingScreen(onComplete = { rootViewModel.completeOnboarding() })
        return
    }
    val nav = rememberNavController()
    val backStackEntry by nav.currentBackStackEntryAsState()

    val tabs = listOf(
        TabItem(Routes.CARE, CareTab.Care,
            { Icon(Icons.Filled.Favorite, null) }, "Care"),
        TabItem(Routes.DIET, CareTab.Diet,
            { Icon(Icons.Filled.Restaurant, null) }, "Diet"),
        TabItem(Routes.TRAIN, CareTab.Train,
            { Icon(Icons.Filled.FitnessCenter, null) }, "Train"),
        TabItem(Routes.WORKOUT, CareTab.Workout,
            { Icon(Icons.Filled.DirectionsRun, null) }, "Workout"),
    )
    var newsOpen by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    val selected = backStackEntry?.destination?.hierarchy
                        ?.any { it.route == tab.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            nav.navigate(tab.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = tab.icon,
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.CARE,
            modifier = Modifier.padding(padding),
            enterTransition = { fadeIn() + slideInHorizontally { it / 4 } },
            exitTransition = { fadeOut() + slideOutHorizontally { -it / 4 } },
            popEnterTransition = { fadeIn() + slideInHorizontally { -it / 4 } },
            popExitTransition = { fadeOut() + slideOutHorizontally { it / 4 } }
        ) {
            // ─── Primary tabs ─────────────────────────────────────────────
            composable(Routes.CARE) { CareHomeScreen(nav) }
            composable(Routes.DIET) { DietHomeScreen(nav) }
            composable(Routes.TRAIN) { TrainHomeScreen(nav) }
            composable(Routes.WORKOUT) { WorkoutHomeScreen(nav) }

            // ─── Header destinations ──────────────────────────────────────
            composable(
                Routes.PROFILE,
                deepLinks = listOf(navDeepLink { uriPattern = "myhealth://profile" })
            ) { ProfileScreen(nav = nav, onNavigateToBioAge = { nav.navigate(Routes.BIO_AGE) }) }
            composable(
                Routes.SETTINGS,
                deepLinks = listOf(navDeepLink { uriPattern = "myhealth://settings" })
            ) { SettingsScreen(nav = nav) }
            composable(Routes.NEWS_DRAWER) { NewsDrawerSheet(onDismiss = { nav.popBackStack() }) }

            // ─── Care ─────────────────────────────────────────────────────
            composable(Routes.MYCHART_CONNECT) { MyChartConnectScreen(nav) }
            composable(Routes.MYCHART_DATA)    { MyChartDataScreen() }
            composable(Routes.INSURANCE_CARD)  { InsuranceCardScreen() }
            composable(Routes.LAB_REPORT)      { LabReportScreen() }
            composable(Routes.DOCTOR_FINDER)   { DoctorFinderScreen(nav) }
            composable(
                "${Routes.DOCTOR_DETAIL}/{npi}",
                arguments = listOf(navArgument("npi") { type = NavType.StringType })
            ) { back ->
                DoctorDetailScreen(npi = back.arguments?.getString("npi") ?: "")
            }
            composable(Routes.ANNUAL_REPORTS) { AnnualReportsScreen(nav) }
            composable(Routes.SYMPTOMS_LOG)   { SymptomsLogScreen(nav) }
            composable(
                Routes.MOOD_TRACKING,
                deepLinks = listOf(navDeepLink { uriPattern = "myhealth://mood" })
            ) { MoodTrackingScreen(nav) }

            // ─── Diet ─────────────────────────────────────────────────────
            composable(Routes.VENDOR_BROWSE)    { VendorBrowseScreen() }
            composable(Routes.MEAL_PLAN_DETAIL) { MealPlanDetailScreen(nav) }
            composable(Routes.ORDER_CHECKOUT)   { OrderCheckoutScreen(nav) }
            composable(Routes.MEAL_LOG_ENTRY)   { FoodDiaryScreen() }
            composable(Routes.DIET_SUGGESTIONS) { DietSuggestionsScreen() }
            composable(Routes.FOOD_DIARY)       { FoodDiaryScreen() }
            composable(Routes.WATER_TRACKER)    { WaterTrackerScreen(nav) }

            // ─── Train ────────────────────────────────────────────────────
            composable(Routes.STANDUP_TIMER)   { StandupTimerScreen() }
            composable(Routes.TODAY_PLAN)      { TodayPlanScreen(nav) }
            composable(
                "${Routes.EXERCISE_DETAIL}/{exerciseId}",
                arguments = listOf(navArgument("exerciseId") { type = NavType.StringType })
            ) { back ->
                ExerciseDetailScreen(exerciseId = back.arguments?.getString("exerciseId") ?: "", nav = nav)
            }
            composable(Routes.EXERCISE_DETAIL) { ExerciseDetailScreen(exerciseId = "", nav = nav) }
            composable(Routes.WORKOUT_LIBRARY) { ExerciseLibraryScreen(nav) }
            composable(Routes.PROGRESS_REPORT) { ProgressReportScreen(nav) }
            composable(Routes.RECOVERY_DAY)    { RecoveryDayScreen(nav) }

            // ─── Workout ──────────────────────────────────────────────────
            composable(Routes.RUN_TRACKER)       { RunTrackerScreen(nav) }
            composable(Routes.WORKOUT_LOGGER)    { WorkoutLoggerScreen(nav) }
            composable(Routes.SLEEP)             { SleepScreen(nav) }
            composable(Routes.WELLNESS_INSIGHTS) { WellnessInsightsScreen(nav) }
            composable(Routes.COMMUNITY_HUB)     { CommunityHubScreen(nav) }
            composable(
                "challenge_detail/{challengeId}",
                arguments = listOf(navArgument("challengeId") { type = NavType.IntType })
            ) { back ->
                ChallengeDetailScreen(
                    challengeId = back.arguments?.getInt("challengeId") ?: 0,
                    nav = nav,
                )
            }
            composable(
                "live_run",
                deepLinks = listOf(navDeepLink { uriPattern = "myhealth://run" })
            ) { LiveRunScreen(nav) }
            composable(
                "run_detail/{sessionId}",
                arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
            ) { back ->
                RunDetailScreen(
                    sessionId = back.arguments?.getString("sessionId") ?: "",
                    nav = nav,
                )
            }

            // ─── Existing kept reachable ──────────────────────────────────
            composable(Routes.HOME)     { HomeScreen(nav) }
            composable(Routes.MEDICINE) { MedicineListScreen() }
            composable(Routes.ACTIVITY) { ActivityListScreen() }
            composable(Routes.ARTICLES) { ArticlesScreen() }
            composable(Routes.VITALS)   { VitalsScreen(nav) }
            composable(Routes.BIO_AGE)  { BiologicalAgeScreen() }
            composable(Routes.ANATOMY)  { AnatomyScreen() }
            composable(Routes.MORE)     { MoreScreen(nav) }
        }
    }
}
