package com.example.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.AnswerReviewScreen
import com.example.ui.screens.ExamInstructionsScreen
import com.example.ui.screens.ExamScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PackageSelectionScreen
import com.example.ui.screens.ResultScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.viewmodel.ExamViewModel

object Routes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val PACKAGES = "packages"
    const val INSTRUCTIONS = "instructions"
    const val EXAM = "exam"
    const val RESULT = "result"
    const val REVIEW = "review"
}

@Composable
fun AppNavigation(
    viewModel: ExamViewModel,
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        modifier = modifier.fillMaxSize(),
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) },
        popEnterTransition = { fadeIn(animationSpec = tween(300)) },
        popExitTransition = { fadeOut(animationSpec = tween(300)) }
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onNavigateToHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToPackages = {
                    navController.navigate(Routes.PACKAGES)
                },
                onSelectPackage = { pkg ->
                    viewModel.selectPackageForInstruction(pkg)
                    navController.navigate(Routes.INSTRUCTIONS)
                },
                onResumeExam = { pkg ->
                    viewModel.startExam(pkg, resume = true)
                    navController.navigate(Routes.EXAM)
                }
            )
        }

        composable(Routes.PACKAGES) {
            PackageSelectionScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onSelectPackage = { pkg ->
                    viewModel.selectPackageForInstruction(pkg)
                    navController.navigate(Routes.INSTRUCTIONS)
                }
            )
        }

        composable(Routes.INSTRUCTIONS) {
            ExamInstructionsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onStartExam = {
                    navController.navigate(Routes.EXAM) {
                        popUpTo(Routes.INSTRUCTIONS) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.EXAM) {
            ExamScreen(
                viewModel = viewModel,
                onExamFinished = {
                    navController.navigate(Routes.RESULT) {
                        popUpTo(Routes.EXAM) { inclusive = true }
                    }
                },
                onExitExam = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.RESULT) {
            ResultScreen(
                viewModel = viewModel,
                onReviewAnswers = {
                    navController.navigate(Routes.REVIEW)
                },
                onRetakeExam = {
                    navController.navigate(Routes.EXAM) {
                        popUpTo(Routes.RESULT) { inclusive = true }
                    }
                },
                onBackHome = {
                    viewModel.resetExam()
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.REVIEW) {
            AnswerReviewScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onBackHome = {
                    viewModel.resetExam()
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }
    }
}
