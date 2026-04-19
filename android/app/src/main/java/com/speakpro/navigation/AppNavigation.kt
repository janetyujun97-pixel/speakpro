package com.speakpro.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.speakpro.designsystem.theme.SpAccent
import com.speakpro.designsystem.theme.SpTextSecondary
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.speakpro.core.network.ApiService
import com.speakpro.core.network.NetworkMonitor
import com.speakpro.core.storage.TokenManager
import com.speakpro.designsystem.components.states.OfflineBanner
import com.speakpro.features.auth.AuthNavGraph
import com.speakpro.features.auth.LoginViewModel
import com.speakpro.features.auth.SplashScreen
import com.speakpro.features.onboarding.OnboardingNavGraph
import com.speakpro.features.home.HomeScreen
import com.speakpro.features.homework.HomeworkDetailScreen
import com.speakpro.features.homework.HomeworkListScreen
import com.speakpro.features.practice.PracticeListScreen
import com.speakpro.features.practice.conversation.ConversationScreen
import com.speakpro.features.practice.followread.FollowReadScreen
import com.speakpro.features.practice.mockexam.MockExamScreen
import com.speakpro.features.practice.readaloud.ReadAloudScreen
import com.speakpro.features.profile.ProfileScreen
import com.speakpro.features.progress.ProgressScreen

// ── 路由常量 ────────────────────────────────────

object Routes {
    const val LOGIN = "login"
    const val MAIN = "main"
    const val HOME = "home"
    const val PRACTICE = "practice"
    const val PRACTICE_CONVERSATION = "practice/conversation"
    const val PRACTICE_READALOUD = "practice/readaloud"
    const val PRACTICE_FOLLOWREAD = "practice/followread"
    const val PRACTICE_MOCKEXAM = "practice/mockexam"
    const val HOMEWORK = "homework"
    const val HOMEWORK_DETAIL = "homework/{id}"
    const val PROGRESS = "progress"
    const val PROFILE = "profile"

    // Review 系统（PR3c）
    const val REVIEW_HISTORY = "review/history"
    const val REVIEW_NOTEBOOK = "review/notebook"
    const val REVIEW_NOTIFICATIONS = "review/notifications"
    const val REVIEW_NOTIFICATION_PREFS = "review/notifications/prefs"

    fun homeworkDetail(id: String) = "homework/$id"
}

// ── Tab 枚举 ────────────────────────────────────

enum class BottomTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    Home(
        route = Routes.HOME,
        label = "首页",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
    ),
    Practice(
        route = Routes.PRACTICE,
        label = "练习",
        selectedIcon = Icons.Filled.Mic,
        unselectedIcon = Icons.Outlined.Mic,
    ),
    Homework(
        route = Routes.HOMEWORK,
        label = "作业",
        selectedIcon = Icons.Filled.MenuBook,
        unselectedIcon = Icons.Outlined.MenuBook,
    ),
    Progress(
        route = Routes.PROGRESS,
        label = "进度",
        selectedIcon = Icons.Filled.ShowChart,
        unselectedIcon = Icons.Outlined.ShowChart,
    ),
    Profile(
        route = Routes.PROFILE,
        label = "我的",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
    ),
}

// ── 顶层导航 ────────────────────────────────────

/**
 * App 根导航状态机：splash → auth / onboarding / main。
 *
 * 路由决策：
 *   - 初次进入：展示 Splash；完成后根据 token 决定去 auth 还是 onboarding 检查
 *   - 未登录：展示 AuthNavGraph
 *   - 已登录：拉 /onboarding/status；completed=true → MainScreen；否则进 OnboardingNavGraph
 */
@Composable
fun AppNavigation() {
    val loginViewModel: LoginViewModel = hiltViewModel()
    val isLoggedIn by loginViewModel.isLoggedIn.collectAsState()

    // 全局 NetworkMonitor —— 单例，靠 EntryPoint 从 Application-scoped graph 拿
    val appCtx = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val networkMonitor = remember(appCtx) {
        dagger.hilt.android.EntryPointAccessors.fromApplication(
            appCtx, NetworkMonitorEntryPoint::class.java,
        ).networkMonitor()
    }

    var route by remember {
        androidx.compose.runtime.mutableStateOf(AppRoute.Splash)
    }

    // Splash 完成后切到合适的路由
    val splashFinished: () -> Unit = {
        route = if (TokenManager.isLoggedIn) AppRoute.Onboarding else AppRoute.Auth
    }

    // 登录态变化：登出时回到 auth；登录时进 onboarding gate
    LaunchedEffect(isLoggedIn) {
        route = if (!isLoggedIn) {
            if (route == AppRoute.Splash) return@LaunchedEffect
            AppRoute.Auth
        } else if (route == AppRoute.Auth) {
            AppRoute.Onboarding
        } else route
    }

    Column {
        // 全局 offline 横幅：非 connected 时滑入顶部
        OfflineBanner(monitor = networkMonitor)

        when (route) {
            AppRoute.Splash -> SplashScreen(onFinished = splashFinished)

            AppRoute.Auth -> AuthNavGraph(onAuthenticated = {
                route = AppRoute.Onboarding
            })

            AppRoute.Onboarding -> OnboardingCheck(
                onGoMain = { route = AppRoute.Main },
                onStartOnboarding = { route = AppRoute.OnboardingGraph },
            )

            AppRoute.OnboardingGraph -> OnboardingNavGraph(
                onCompleted = { route = AppRoute.Main },
            )

            AppRoute.Main -> MainScreen(onLogout = {
                loginViewModel.logout()
                route = AppRoute.Auth
            })
        }
    }
}

private enum class AppRoute {
    Splash, Auth, Onboarding, OnboardingGraph, Main
}

/**
 * 登录后先拉一次 /onboarding/status 决定去主界面还是 onboarding 流程。
 * 网络失败时默认进 onboarding（用户可正常完成流程）。
 */
@Composable
private fun OnboardingCheck(
    onGoMain: () -> Unit,
    onStartOnboarding: () -> Unit,
) {
    val apiService: ApiService =
        dagger.hilt.android.EntryPointAccessors.fromApplication(
            androidx.compose.ui.platform.LocalContext.current.applicationContext,
            com.speakpro.navigation.AppApiEntryPoint::class.java,
        ).apiService()

    LaunchedEffect(Unit) {
        try {
            val resp = apiService.getOnboardingStatus()
            if (resp.code == 0 && resp.data?.completed == true) onGoMain()
            else onStartOnboarding()
        } catch (_: Exception) {
            onStartOnboarding()
        }
    }

    // 检查期间展示一个中性的 splash（避免 UI 闪空白）
    SplashScreen(onFinished = {})
}

// ── 主界面（带底部导航栏） ──────────────────────

@Composable
private fun MainScreen(
    onLogout: () -> Unit,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // 仅在 Tab 根路由时显示底部栏
    val tabRoutes = BottomTab.entries.map { it.route }
    val showBottomBar = currentRoute in tabRoutes

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
            ) {
                BottomNavigationBar(
                    navController = navController,
                    currentRoute = currentRoute,
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // ── Tab 根页面 ──
            composable(Routes.HOME) {
                HomeScreen(
                    onNavigateToPractice = { mode ->
                        navController.navigate(mode)
                    },
                    onNavigateToHomework = {
                        navController.navigate(Routes.HOMEWORK) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }

            composable(Routes.PRACTICE) {
                PracticeListScreen(
                    onNavigate = { route -> navController.navigate(route) },
                )
            }

            composable(Routes.HOMEWORK) {
                HomeworkListScreen(
                    onNavigateToDetail = { id ->
                        navController.navigate(Routes.homeworkDetail(id))
                    },
                )
            }

            composable(Routes.PROGRESS) {
                ProgressScreen(
                    onOpenHistory = { navController.navigate(Routes.REVIEW_HISTORY) },
                    onOpenNotebook = { navController.navigate(Routes.REVIEW_NOTEBOOK) },
                    onOpenNotifications = {
                        navController.navigate(Routes.REVIEW_NOTIFICATIONS)
                    },
                )
            }

            // Review 子路由（PR3c）
            composable(Routes.REVIEW_HISTORY) {
                com.speakpro.features.review.HistoryTimelineScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.REVIEW_NOTEBOOK) {
                com.speakpro.features.review.NotebookScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.REVIEW_NOTIFICATIONS) {
                com.speakpro.features.review.NotificationsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenPrefs = {
                        navController.navigate(Routes.REVIEW_NOTIFICATION_PREFS)
                    },
                )
            }
            composable(Routes.REVIEW_NOTIFICATION_PREFS) {
                com.speakpro.features.review.NotificationPrefsScreen(
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.PROFILE) {
                ProfileScreen(onLogout = onLogout)
            }

            // ── 练习子路由（嵌套详情页） ──

            composable(Routes.PRACTICE_CONVERSATION) {
                ConversationScreen(
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.PRACTICE_READALOUD) {
                ReadAloudScreen(
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.PRACTICE_FOLLOWREAD) {
                FollowReadScreen(
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.PRACTICE_MOCKEXAM) {
                MockExamScreen(
                    onBack = { navController.popBackStack() },
                )
            }

            // ── 作业详情 ──

            composable(
                route = Routes.HOMEWORK_DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) {
                val homeworkId = it.arguments?.getString("id") ?: ""
                HomeworkDetailScreen(
                    homeworkId = homeworkId,
                    onBack = { navController.popBackStack() },
                    onStartPractice = { questionId ->
                        navController.navigate(Routes.PRACTICE_CONVERSATION)
                    },
                )
            }
        }
    }
}

// ── 底部导航栏 ──────────────────────────────────

@Composable
private fun BottomNavigationBar(
    navController: NavHostController,
    currentRoute: String?,
) {
    NavigationBar {
        BottomTab.entries.forEach { tab ->
            val selected = currentRoute == tab.route

            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.label,
                    )
                },
                label = { Text(text = tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = SpAccent,
                    selectedTextColor = SpAccent,
                    unselectedIconColor = SpTextSecondary,
                    unselectedTextColor = SpTextSecondary,
                ),
            )
        }
    }
}

// ── 占位页面（供尚未实现的练习详情使用） ─────────

@Composable
private fun PlaceholderScreen(
    title: String,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = com.speakpro.designsystem.theme.SpTitleMedium,
                color = com.speakpro.designsystem.theme.SpTextPrimary,
            )
            Spacer(modifier = Modifier.padding(vertical = 8.dp))
            TextButton(onClick = onBack) {
                Text("返回")
            }
        }
    }
}
