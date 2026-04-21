package com.speakpro.features.auth

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

object AuthRoutes {
    const val GRAPH = "auth_graph"
    const val LOGIN = "auth/login"
    const val EMAIL = "auth/email"
    const val OTP = "auth/otp/{flow}"
    const val REGISTER = "auth/register"
    const val FORGOT = "auth/forgot"
    const val NEW_PASSWORD = "auth/new_password"

    // OTP flow 参数
    const val FLOW_LOGIN = "login"
    const val FLOW_REGISTER = "register"
    const val FLOW_RESET = "reset"

    fun otp(flow: String) = "auth/otp/$flow"
}

/**
 * Auth 流程的嵌套导航图 —— Login → OTP → Register / NewPassword。
 * PhoneAuthViewModel 通过 hiltViewModel() 注入一次，所有子屏共享同一实例
 * （NavController 级别的 ViewModelStore 保证生命周期）。
 *
 * @param onAuthenticated 登录/注册成功时调用，外层切到 onboarding 或 main
 */
@Composable
fun AuthNavGraph(onAuthenticated: () -> Unit) {
    val navController = rememberNavController()
    val ctx = LocalContext.current

    NavHost(navController = navController, startDestination = AuthRoutes.LOGIN) {

        composable(AuthRoutes.LOGIN) {
            val phoneVM: PhoneAuthViewModel = hiltViewModel()
            val loginVM: LoginViewModel = hiltViewModel()
            val isLoggedIn by loginVM.isLoggedIn.collectAsState()
            LaunchedEffect(isLoggedIn) { if (isLoggedIn) onAuthenticated() }

            LoginScreen(
                phoneVM = phoneVM,
                onRequestOtp = {
                    phoneVM.sendOtp {
                        navController.navigate(AuthRoutes.otp(AuthRoutes.FLOW_LOGIN))
                    }
                },
                onGoRegister = { navController.navigate(AuthRoutes.REGISTER) },
                onGoForgot = { navController.navigate(AuthRoutes.FORGOT) },
                onAppleSignIn = {
                    // PR2c 未接 Apple SDK（Android 端 Apple 登录用 web OAuth 流，属 follow-up）
                    Toast.makeText(ctx, "Apple 登录敬请期待", Toast.LENGTH_SHORT).show()
                },
                onWechatSignIn = {
                    Toast.makeText(ctx, "微信登录敬请期待", Toast.LENGTH_SHORT).show()
                },
                onEmailLogin = { navController.navigate(AuthRoutes.EMAIL) },
            )
        }

        composable(AuthRoutes.EMAIL) {
            // 共享 LOGIN entry 的 LoginViewModel —— 登录成功后上游 isLoggedIn 状态一致
            val loginVM: LoginViewModel = hiltViewModel(
                navController.getBackStackEntry(AuthRoutes.LOGIN),
            )
            val isLoggedIn by loginVM.isLoggedIn.collectAsState()
            LaunchedEffect(isLoggedIn) { if (isLoggedIn) onAuthenticated() }

            EmailLoginScreen(
                vm = loginVM,
                onBack = { navController.popBackStack() },
                onGoForgot = { navController.navigate(AuthRoutes.FORGOT) },
            )
        }

        composable(
            route = AuthRoutes.OTP,
            arguments = listOf(navArgument("flow") { type = NavType.StringType }),
        ) { entry ->
            val flow = entry.arguments?.getString("flow") ?: AuthRoutes.FLOW_LOGIN
            val phoneVM: PhoneAuthViewModel = hiltViewModel(
                navController.getBackStackEntry(AuthRoutes.LOGIN),
            )

            val label = when (flow) {
                AuthRoutes.FLOW_RESET -> "验证并重置密码"
                else -> "验证并继续"
            }

            OTPScreen(
                phoneVM = phoneVM,
                primaryLabel = label,
                onBack = { navController.popBackStack() },
                onResend = {
                    if (flow == AuthRoutes.FLOW_RESET) phoneVM.sendResetOtp()
                    else phoneVM.sendOtp()
                },
                onVerified = {
                    // 一期：登录/注册都先去补姓名（register-phone 完成真正注册）；reset 去设新密码
                    when (flow) {
                        AuthRoutes.FLOW_RESET -> navController.navigate(AuthRoutes.NEW_PASSWORD)
                        else -> navController.navigate(AuthRoutes.REGISTER)
                    }
                },
            )
        }

        composable(AuthRoutes.REGISTER) {
            // 从 LOGIN 入口分享同一个 PhoneAuthViewModel
            val phoneVM: PhoneAuthViewModel = hiltViewModel(
                navController.getBackStackEntry(AuthRoutes.LOGIN),
            )
            val regVM: RegisterViewModel = hiltViewModel()
            RegisterScreen(
                phoneVM = phoneVM,
                viewModel = regVM,
                onBack = { navController.popBackStack() },
                onRegistered = onAuthenticated,
            )
        }

        composable(AuthRoutes.FORGOT) {
            // 忘记密码独立起一份 PhoneAuthViewModel
            val phoneVM: PhoneAuthViewModel = hiltViewModel()
            ForgotScreen(
                phoneVM = phoneVM,
                onBack = { navController.popBackStack() },
                onOtpSent = {
                    navController.navigate(AuthRoutes.otp(AuthRoutes.FLOW_RESET))
                },
            )
        }

        composable(AuthRoutes.NEW_PASSWORD) {
            val phoneVM: PhoneAuthViewModel = hiltViewModel(
                navController.getBackStackEntry(AuthRoutes.FORGOT),
            )
            val resetVM: ResetPasswordViewModel = hiltViewModel()
            NewPasswordScreen(
                phoneVM = phoneVM,
                viewModel = resetVM,
                onBack = { navController.popBackStack() },
                onSuccess = {
                    // 重置成功 —— 回到 Login 让用户重新登录
                    navController.popBackStack(AuthRoutes.LOGIN, inclusive = false)
                },
            )
        }
    }
}
