package com.ticketcheck.offline.ui.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ticketcheck.offline.TicketCheckApp
import com.ticketcheck.offline.ui.ViewModelFactory
import com.ticketcheck.offline.ui.screens.backup.BackupScreen
import com.ticketcheck.offline.ui.screens.backup.BackupViewModel
import com.ticketcheck.offline.ui.screens.history.ScanHistoryScreen
import com.ticketcheck.offline.ui.screens.history.ScanHistoryViewModel
import com.ticketcheck.offline.ui.screens.home.HomeScreen
import com.ticketcheck.offline.ui.screens.home.HomeViewModel
import com.ticketcheck.offline.ui.screens.manage.ManageTicketsScreen
import com.ticketcheck.offline.ui.screens.manage.ManageTicketsViewModel
import com.ticketcheck.offline.ui.screens.onboarding.OnboardingScreen
import com.ticketcheck.offline.ui.screens.qrgen.QrGenerateScreen
import com.ticketcheck.offline.ui.screens.qrgen.QrGenerateViewModel
import com.ticketcheck.offline.ui.screens.scanner.ScannerScreen
import com.ticketcheck.offline.ui.screens.scanner.ScannerViewModel
import com.ticketcheck.offline.ui.screens.settings.SettingsScreen
import com.ticketcheck.offline.ui.screens.ticketdetail.TicketDetailScreen
import com.ticketcheck.offline.ui.screens.ticketdetail.TicketDetailViewModel
import com.ticketcheck.offline.ui.screens.ticketlist.TicketListScreen
import com.ticketcheck.offline.ui.screens.ticketlist.TicketListViewModel

@Composable
fun TicketCheckNavGraph(app: TicketCheckApp) {
    val navController = rememberNavController()
    val factory = ViewModelFactory(app)
    val event by app.repository.observeCurrentEvent().collectAsState(initial = null)

    val startDestination = Routes.HOME // Onboarding gate is handled inline below via event == null check.

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        enterTransition = {
            fadeIn(tween(280)) + slideInVertically(
                tween(340, easing = FastOutSlowInEasing)
            ) { it / 14 }
        },
        exitTransition = { fadeOut(tween(180)) },
        popEnterTransition = { fadeIn(tween(240)) },
        popExitTransition = {
            fadeOut(tween(220)) + slideOutVertically(
                tween(300, easing = FastOutSlowInEasing)
            ) { it / 14 }
        }
    ) {
        composable(Routes.HOME) {
            if (event == null) {
                OnboardingScreen(app = app, onEventCreated = { })
            } else {
                val vm: HomeViewModel = viewModel(factory = factory)
                HomeScreen(
                    app = app,
                    viewModel = vm,
                    onScan = { navController.navigate(Routes.SCANNER) },
                    onManage = { navController.navigate(Routes.MANAGE) },
                    onTicketList = { navController.navigate(Routes.TICKET_LIST) },
                    onGenerateQr = { navController.navigate(Routes.QR_GENERATE) },
                    onBackup = { navController.navigate(Routes.BACKUP) },
                    onSettings = { navController.navigate(Routes.SETTINGS) },
                    onHistory = { navController.navigate(Routes.SCAN_HISTORY) }
                )
            }
        }

        composable(
            Routes.SCANNER,
            enterTransition = {
                scaleIn(initialScale = 0.85f, animationSpec = tween(320, easing = FastOutSlowInEasing)) +
                    fadeIn(tween(240))
            },
            popExitTransition = {
                scaleOut(targetScale = 0.92f, animationSpec = tween(260)) + fadeOut(tween(220))
            }
        ) {
            val vm: ScannerViewModel = viewModel(factory = factory)
            ScannerScreen(viewModel = vm, onExit = { navController.popBackStack() })
        }

        composable(Routes.MANAGE) {
            val vm: ManageTicketsViewModel = viewModel(factory = factory)
            ManageTicketsScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }

        composable(Routes.TICKET_LIST) {
            val vm: TicketListViewModel = viewModel(factory = factory)
            TicketListScreen(
                navController = navController,
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onTicketClick = { id -> navController.navigate(Routes.ticketDetail(id)) }
            )
        }

        composable(
            route = Routes.TICKET_DETAIL,
            arguments = listOf(navArgument("ticketId") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("ticketId") ?: -1L
            val vm: TicketDetailViewModel = viewModel(factory = factory)
            TicketDetailScreen(
                viewModel = vm,
                ticketId = id,
                onBack = { navController.popBackStack() },
                onGenerateQr = { code -> navController.navigate(Routes.qrGenerateFor(code)) }
            )
        }

        composable(Routes.QR_GENERATE) {
            val vm: QrGenerateViewModel = viewModel(factory = factory)
            QrGenerateScreen(viewModel = vm, repository = app.repository, initialTicketCode = null, onBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.QR_GENERATE_FOR,
            arguments = listOf(navArgument("ticketCode") { type = NavType.StringType })
        ) { backStackEntry ->
            val code = backStackEntry.arguments?.getString("ticketCode")
            val vm: QrGenerateViewModel = viewModel(factory = factory)
            QrGenerateScreen(viewModel = vm, repository = app.repository, initialTicketCode = code, onBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.QR_GENERATE_BULK,
            arguments = listOf(navArgument("codes") { type = NavType.StringType })
        ) { backStackEntry ->
            val codesStr = backStackEntry.arguments?.getString("codes")
            val codes = codesStr?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
            val vm: QrGenerateViewModel = viewModel(factory = factory)
            QrGenerateScreen(viewModel = vm, repository = app.repository, initialTicketCode = null, initialTicketCodes = codes, onBack = { navController.popBackStack() })
        }

        composable(Routes.SCAN_HISTORY) {
            val vm: ScanHistoryViewModel = viewModel(factory = factory)
            ScanHistoryScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }

        composable(Routes.BACKUP) {
            val vm: BackupViewModel = viewModel(factory = factory)
            BackupScreen(viewModel = vm, backupManager = app.backupManager, onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(settings = app.settings, repository = app.repository, onBack = { navController.popBackStack() })
        }
    }
}
