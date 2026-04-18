package com.speakpro.features.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

object OnbRoutes {
    const val GRAPH = "onboarding_graph"
    const val WELCOME = "onb/welcome"
    const val EXAM_TYPE = "onb/exam_type"
    const val TARGET = "onb/target"
    const val DATE = "onb/date"
    const val LEVEL = "onb/level"
    const val BASELINE_INTRO = "onb/baseline_intro"
    const val BASELINE_RECORDING = "onb/baseline_recording"
    const val PLAN = "onb/plan"
}

/**
 * Onboarding 8 屏导航容器。完成后调 [onCompleted]。
 * VM 绑定在 graph 根，8 屏共享同一实例。
 */
@Composable
fun OnboardingNavGraph(
    onCompleted: () -> Unit,
    onGoLogin: () -> Unit = {},
) {
    val navController = rememberNavController()
    val vm: OnboardingViewModel = hiltViewModel()

    LaunchedEffect(Unit) { vm.hydrate() }

    val completed by vm.completed.collectAsState()
    LaunchedEffect(completed) { if (completed) onCompleted() }

    NavHost(
        navController = navController,
        startDestination = OnbRoutes.WELCOME,
    ) {
        composable(OnbRoutes.WELCOME) {
            OnbWelcomeScreen(
                onContinue = { navController.navigate(OnbRoutes.EXAM_TYPE) },
                onGoLogin = onGoLogin,
            )
        }
        composable(OnbRoutes.EXAM_TYPE) {
            OnbExamTypeScreen(
                vm = vm,
                onBack = { navController.popBackStack() },
                onContinue = { navController.navigate(OnbRoutes.TARGET) },
            )
        }
        composable(OnbRoutes.TARGET) {
            OnbTargetScreen(
                vm = vm,
                onBack = { navController.popBackStack() },
                onContinue = { navController.navigate(OnbRoutes.DATE) },
            )
        }
        composable(OnbRoutes.DATE) {
            OnbDateScreen(
                vm = vm,
                onBack = { navController.popBackStack() },
                onContinue = { navController.navigate(OnbRoutes.LEVEL) },
            )
        }
        composable(OnbRoutes.LEVEL) {
            OnbLevelScreen(
                vm = vm,
                onBack = { navController.popBackStack() },
                onContinue = { navController.navigate(OnbRoutes.BASELINE_INTRO) },
            )
        }
        composable(OnbRoutes.BASELINE_INTRO) {
            OnbBaselineIntroScreen(
                onBack = { navController.popBackStack() },
                onStart = { navController.navigate(OnbRoutes.BASELINE_RECORDING) },
            )
        }
        composable(OnbRoutes.BASELINE_RECORDING) {
            OnbBaselineRecordingScreen(
                vm = vm,
                onBack = { navController.popBackStack() },
                onFinished = { navController.navigate(OnbRoutes.PLAN) },
            )
        }
        composable(OnbRoutes.PLAN) {
            OnbPlanScreen(vm = vm, onDone = onCompleted)
        }
    }
}
