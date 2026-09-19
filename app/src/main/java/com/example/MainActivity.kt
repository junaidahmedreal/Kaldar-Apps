package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.locale.AppStrings
import com.example.ui.navigation.Screen
import com.example.ui.screens.BudgetScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.RoomScreen
import com.example.ui.screens.ScanReceiptScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.EnExpenseTheme
import com.example.ui.viewmodel.BudgetViewModel
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.ReportsViewModel
import com.example.ui.viewmodel.RoomViewModel
import com.example.ui.viewmodel.ScanViewModel
import com.example.ui.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val scanViewModel: ScanViewModel by viewModels()
    private val budgetViewModel: BudgetViewModel by viewModels()
    private val reportsViewModel: ReportsViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val roomViewModel: RoomViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by settingsViewModel.themeMode.collectAsState()
            val themeColor by settingsViewModel.themeColor.collectAsState()
            val themeFont by settingsViewModel.themeFont.collectAsState()

            EnExpenseTheme(
                themeMode = themeMode,
                themeColor = themeColor,
                themeFont = themeFont
            ) {
                EnExpenseApp(
                    mainViewModel = mainViewModel,
                    scanViewModel = scanViewModel,
                    budgetViewModel = budgetViewModel,
                    reportsViewModel = reportsViewModel,
                    settingsViewModel = settingsViewModel,
                    roomViewModel = roomViewModel
                )
            }
        }
    }
}

@Composable
fun EnExpenseApp(
    mainViewModel: MainViewModel,
    scanViewModel: ScanViewModel,
    budgetViewModel: BudgetViewModel,
    reportsViewModel: ReportsViewModel,
    settingsViewModel: SettingsViewModel,
    roomViewModel: RoomViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val language by mainViewModel.appLanguage.collectAsState()
    val accountMode by mainViewModel.accountMode.collectAsState()

    // Sync mode changes with other viewmodels
    budgetViewModel.setAccountMode(accountMode)
    reportsViewModel.setAccountMode(accountMode)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier.testTag("bottom_navigation_bar")
            ) {
                Screen.bottomNavItems.forEach { screen ->
                    val selected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = AppStrings.get(screen.stringKey, language)
                            )
                        },
                        label = {
                            Text(
                                text = AppStrings.get(screen.stringKey, language),
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.testTag("nav_item_${screen.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = mainViewModel,
                    onNavigateToScan = {
                        scanViewModel.reset()
                        navController.navigate(Screen.Scan.route)
                    },
                    onNavigateToTransactions = {
                        navController.navigate(Screen.Transactions.route)
                    },
                    onNavigateToRooms = {
                        navController.navigate(Screen.Rooms.route)
                    }
                )
            }

            composable(Screen.Scan.route) {
                ScanReceiptScreen(
                    viewModel = scanViewModel,
                    accountMode = accountMode,
                    onModeChanged = { mainViewModel.setAccountMode(it) },
                    language = language,
                    onNavigateBack = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                        }
                    }
                )
            }

            composable(Screen.Transactions.route) {
                TransactionsScreen(
                    viewModel = mainViewModel
                )
            }

            composable(Screen.Rooms.route) {
                RoomScreen(
                    viewModel = roomViewModel,
                    language = language,
                    onNavigateBack = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Screen.Budget.route) {
                BudgetScreen(
                    viewModel = budgetViewModel,
                    language = language
                )
            }

            composable(Screen.Reports.route) {
                ReportsScreen(
                    viewModel = reportsViewModel,
                    language = language
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onLanguageChanged = { mainViewModel.setAppLanguage(it) }
                )
            }
        }
    }
}
