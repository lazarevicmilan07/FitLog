package com.workoutlog

import android.animation.AnimatorSet
import android.content.Intent
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AnticipateOvershootInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.workoutlog.BuildConfig
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.workoutlog.data.datastore.ThemeMode
import com.workoutlog.ui.navigation.BottomNavItem
import com.workoutlog.ui.navigation.NavGraph
import com.workoutlog.ui.navigation.Screen
import com.workoutlog.ui.navigation.bottomNavItems
import com.workoutlog.ui.theme.NavBarAccentDark
import com.workoutlog.ui.theme.NavBarAccentLight
import com.workoutlog.ui.theme.NavBarBgDark
import com.workoutlog.ui.theme.NavBarBgLight
import com.workoutlog.ui.theme.NavBarUnselectedDark
import com.workoutlog.ui.theme.NavBarUnselectedLight
import com.workoutlog.ui.theme.WorkoutLogTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.ripple
import androidx.activity.viewModels
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { mainViewModel.onboardingCompleted.value == null }

        splashScreen.setOnExitAnimationListener { splashScreenView ->
            // When launched from a notification the splash screen has no icon view.
            // Accessing splashScreenView.iconView in that case throws a NPE inside
            // the AndroidX SplashScreen library, so we guard with a try-catch and
            // fall back to an instant dismiss.
            val iconView = try { splashScreenView.iconView } catch (_: Exception) { null }

            if (iconView == null) {
                splashScreenView.remove()
                return@setOnExitAnimationListener
            }

            val iconScaleX = ObjectAnimator.ofFloat(iconView, View.SCALE_X, 1f, 1.5f)
            val iconScaleY = ObjectAnimator.ofFloat(iconView, View.SCALE_Y, 1f, 1.5f)
            val iconFade = ObjectAnimator.ofFloat(iconView, View.ALPHA, 1f, 0f)

            val backgroundSlide = ObjectAnimator.ofFloat(
                splashScreenView.view,
                View.TRANSLATION_Y,
                0f,
                -splashScreenView.view.height.toFloat() * 0.3f
            )
            val backgroundFade = ObjectAnimator.ofFloat(splashScreenView.view, View.ALPHA, 1f, 0f)

            val iconAnimator = AnimatorSet().apply {
                playTogether(iconScaleX, iconScaleY, iconFade)
                duration = 400L
                interpolator = DecelerateInterpolator(1.5f)
            }

            val backgroundAnimator = AnimatorSet().apply {
                playTogether(backgroundSlide, backgroundFade)
                duration = 350L
                startDelay = 100L
                interpolator = AnticipateOvershootInterpolator(0.5f)
            }

            AnimatorSet().apply {
                playTogether(iconAnimator, backgroundAnimator)
                doOnEnd { splashScreenView.remove() }
                start()
            }
        }
        enableEdgeToEdge()

        MobileAds.initialize(this) {}

        setContent {
            val themeMode by mainViewModel.themeMode.collectAsStateWithLifecycle()
            val onboardingCompleted by mainViewModel.onboardingCompleted.collectAsStateWithLifecycle()
            val isPremium by mainViewModel.isPremium.collectAsStateWithLifecycle()

            WorkoutLogTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (onboardingCompleted != null) {
                        MainContent(
                            activity = this@MainActivity,
                            startDestination = if (onboardingCompleted == true)
                                Screen.Home.route else Screen.Onboarding.route,
                            isPremium = isPremium
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Dismiss the daily reminder notification whenever the app comes to the foreground,
        // regardless of how it was opened (body tap, action button, or launcher).
        NotificationManagerCompat.from(this)
            .cancel(com.workoutlog.notifications.ReminderNotificationHelper.NOTIFICATION_ID)
    }

    /** Handles re-delivery when the activity is already running (singleTop). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
    }
}

@Composable
fun MainContent(
    activity: MainActivity,
    startDestination: String,
    isPremium: Boolean
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomBarRoutes = setOf(
        Screen.Home.route,
        Screen.StatsMonthly.route,
        Screen.StatsYearly.route,
        Screen.WorkoutTypes.route,
        Screen.Settings.route
    )
    val showBottomBar = currentRoute in bottomBarRoutes

    var navBarVisible by remember { mutableStateOf(showBottomBar) }
    var statsSubmenuVisible by remember { mutableStateOf(false) }

    // Close submenu when leaving stats screens
    LaunchedEffect(currentRoute) {
        if (currentRoute != Screen.StatsMonthly.route && currentRoute != Screen.StatsYearly.route) {
            statsSubmenuVisible = false
        }
    }

    // Delayed show to avoid nav bar jumping during screen slide animations
    LaunchedEffect(showBottomBar) {
        if (showBottomBar) {
            delay(300L)
            navBarVisible = true
        } else {
            navBarVisible = false
        }
    }

    val isStatsSelected = currentRoute == Screen.StatsMonthly.route || currentRoute == Screen.StatsYearly.route
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val navBarBg = if (isDark) NavBarBgDark else NavBarBgLight
    val navBarAccent = if (isDark) NavBarAccentDark else NavBarAccentLight
    val navBarUnselected = if (isDark) NavBarUnselectedDark else NavBarUnselectedLight

    val adRoutes = setOf(
        Screen.WorkoutTypes.route,
        Screen.Settings.route,
        Screen.StatsMonthly.route
    )
    val showAd = !isPremium && currentRoute in adRoutes

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val adView = remember {
        AdView(activity).apply {
            setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, screenWidthDp))
            adUnitId = BuildConfig.ADMOB_BANNER_ID
            loadAd(AdRequest.Builder().build())
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> adView.pause()
                Lifecycle.Event.ON_RESUME -> adView.resume()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            adView.destroy()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                AnimatedVisibility(
                    visible = navBarVisible,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = ExitTransition.None
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(navBarBg)
                            .navigationBarsPadding()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            bottomNavItems.forEach { item ->
                                val isSelected = when (item.screen) {
                                    Screen.StatsMonthly -> isStatsSelected
                                    else -> currentRoute == item.screen.route
                                }
                                NavBarItem(
                                    item = item,
                                    selected = isSelected,
                                    selectedColor = navBarAccent,
                                    unselectedColor = navBarUnselected,
                                    onClick = {
                                        if (item.screen == Screen.StatsMonthly) {
                                            statsSubmenuVisible = !statsSubmenuVisible
                                        } else {
                                            statsSubmenuVisible = false
                                            navController.navigate(item.screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                NavGraph(
                    navController = navController,
                    startDestination = startDestination
                )
            }
        }

        // Ad banner overlay — sits above the nav bar, visibility toggled per route
        AndroidView(
            factory = { adView },
            update = { view ->
                view.visibility = if (showAd) View.VISIBLE else View.GONE
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 56.dp)
                .fillMaxWidth()
        )

        // Stats submenu — floating overlay outside Scaffold so it never affects nav bar height
        AnimatedVisibility(
            visible = navBarVisible && statsSubmenuVisible,
            enter = fadeIn(tween(150)) + slideInVertically(tween(150)) { it },
            exit = fadeOut(tween(120)) + slideOutVertically(tween(120)) { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .offset(x = (-60).dp, y = (-70).dp)
        ) {
            StatsSubmenu(
                navBarAccent = navBarAccent,
                navBarUnselected = navBarUnselected,
                navBarBg = navBarBg,
                currentRoute = currentRoute,
                onMonthly = {
                    statsSubmenuVisible = false
                    if (currentRoute != Screen.StatsMonthly.route) {
                        navController.navigate(Screen.StatsMonthly.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                onYearly = {
                    statsSubmenuVisible = false
                    if (currentRoute != Screen.StatsYearly.route) {
                        navController.navigate(Screen.StatsYearly.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun NavBarItem(
    item: BottomNavItem,
    selected: Boolean,
    selectedColor: Color,
    unselectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.82f else 1f,
        animationSpec = tween(100),
        label = "nav_scale"
    )
    val itemColor = if (selected) selectedColor else unselectedColor

    Column(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = itemColor)
            ) { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
            contentDescription = item.label,
            tint = itemColor,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = item.label,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = itemColor
        )
    }
}

@Composable
private fun StatsSubmenu(
    navBarAccent: Color,
    navBarUnselected: Color,
    navBarBg: Color,
    currentRoute: String?,
    onMonthly: () -> Unit,
    onYearly: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = navBarBg)
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            StatsSubmenuItem(
                icon = Icons.Filled.BarChart,
                label = "Monthly",
                color = if (currentRoute == Screen.StatsMonthly.route) navBarAccent else navBarUnselected,
                onClick = onMonthly
            )
            StatsSubmenuItem(
                icon = Icons.Filled.CalendarMonth,
                label = "Yearly",
                color = if (currentRoute == Screen.StatsYearly.route) navBarAccent else navBarUnselected,
                onClick = onYearly
            )
        }
    }
}

@Composable
private fun StatsSubmenuItem(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .width(84.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}
