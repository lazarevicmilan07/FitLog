package com.workoutlog.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.workoutlog.ui.screens.goals.GoalsScreen
import com.workoutlog.ui.screens.home.HomeScreen
import com.workoutlog.ui.screens.onboarding.OnboardingScreen
import com.workoutlog.ui.screens.reports.ReportsScreen
import com.workoutlog.ui.screens.premium.PremiumScreen
import com.workoutlog.ui.screens.settings.SettingsScreen
import com.workoutlog.ui.screens.workouttype.WorkoutTypesScreen

private const val ANIM_DURATION = 300

private fun getNavIndex(route: String?): Int = when (route) {
    Screen.Home.route -> 0
    Screen.Stats.route, Screen.StatsMonthly.route, Screen.StatsYearly.route -> 1
    Screen.Goals.route -> 2
    Screen.WorkoutTypes.route -> 3
    Screen.Settings.route -> 4
    else -> -1
}

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            val from = getNavIndex(initialState.destination.route)
            val to = getNavIndex(targetState.destination.route)
            if (from >= 0 && to >= 0 && from != to) {
                val dir = if (to > from) 1 else -1
                slideInHorizontally(tween(ANIM_DURATION)) { it / 3 * dir } + fadeIn(tween(ANIM_DURATION))
            } else {
                fadeIn(tween(ANIM_DURATION))
            }
        },
        exitTransition = {
            val from = getNavIndex(initialState.destination.route)
            val to = getNavIndex(targetState.destination.route)
            if (from >= 0 && to >= 0 && from != to) {
                val dir = if (to > from) -1 else 1
                slideOutHorizontally(tween(ANIM_DURATION)) { it / 3 * dir } + fadeOut(tween(ANIM_DURATION))
            } else {
                fadeOut(tween(ANIM_DURATION))
            }
        },
        popEnterTransition = { slideInHorizontally(tween(ANIM_DURATION)) { -it / 3 } + fadeIn(tween(ANIM_DURATION)) },
        popExitTransition = { slideOutHorizontally(tween(ANIM_DURATION)) { it / 3 } + fadeOut(tween(ANIM_DURATION)) }
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen()
        }

        composable(Screen.Stats.route) {
            ReportsScreen()
        }

        composable(Screen.Goals.route) {
            GoalsScreen()
        }

        composable(Screen.WorkoutTypes.route) {
            WorkoutTypesScreen()
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onShowPremium = { navController.navigate(Screen.Premium.route) }
            )
        }

        composable(Screen.Premium.route) {
            PremiumScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
