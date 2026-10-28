package com.swasthai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.swasthai.app.ui.chat.ChatScreen
import com.swasthai.app.ui.chat.ChatViewModel
import com.swasthai.app.ui.components.AppBottomNav
import com.swasthai.app.ui.history.HistoryScreen
import com.swasthai.app.ui.history.HistoryViewModel
import com.swasthai.app.ui.integrations.IntegrationsScreen
import com.swasthai.app.ui.home.HomeScreen
import com.swasthai.app.ui.home.HomeViewModel
import com.swasthai.app.ui.navigation.Screen
import com.swasthai.app.ui.settings.SettingsScreen
import com.swasthai.app.ui.settings.SettingsViewModel
import com.swasthai.app.ui.theme.Slate950
import com.swasthai.app.ui.theme.SwasthAITheme
import com.swasthai.app.ui.vault.VaultScreen
import com.swasthai.app.ui.vault.VaultViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as SwasthApplication

        setContent {
            SwasthAITheme {
                MainAppContent(app)
            }
        }
    }
}

@Composable
fun MainAppContent(app: SwasthApplication) {
    val navController = rememberNavController()

    // ViewModels with factories
    val homeViewModel: HomeViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return HomeViewModel(
                    app.chatRepository,
                    app.documentRepository,
                    app.historyRepository,
                    app.gemma4ToolCallingModel,
                    app.personProfileRepository,
                    app.medicationReminderRepository,
                    app.cycleTrackingRepository
                ) as T
            }
        }
    )

    val chatViewModel: ChatViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ChatViewModel(
                    app.chatRepository,
                    app.documentRepository,
                    app.gemma4ToolCallingModel
                ) as T
            }
        }
    )

    val vaultViewModel: VaultViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return VaultViewModel(app.documentRepository) as T
            }
        }
    )

    val historyViewModel: HistoryViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return HistoryViewModel(app.historyRepository) as T
            }
        }
    )

    val settingsViewModel: SettingsViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return SettingsViewModel(
                    app.embeddingEngine,
                    app.documentRepository,
                    app.gemma4ToolCallingModel,
                    app.gemmaModelArtifactManager,
                    app.accountFamilyRepository,
                    app.deviceKeyManager
                ) as T
            }
        }
    )

    Scaffold(
        bottomBar = {
            AppBottomNav(navController = navController)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        viewModel = homeViewModel,
                        onNavigateToChat = { convoId ->
                            navController.navigate(Screen.Chat.createRoute(convoId))
                        },
                        onNavigateToHistory = {
                            navController.navigate(Screen.History.route)
                        },
                        onNavigateToVault = {
                            navController.navigate(Screen.Vault.route)
                        },
                        onNavigateToSettings = {
                            navController.navigate(Screen.Settings.route)
                        }
                    )
                }

                composable(
                    route = Screen.Chat.route,
                    arguments = listOf(
                        navArgument("conversationId") {
                            type = NavType.LongType
                            defaultValue = -1L
                        }
                    )
                ) { backStackEntry ->
                    val convoId = backStackEntry.arguments?.getLong("conversationId")
                    ChatScreen(
                        viewModel = chatViewModel,
                        initialConversationId = if (convoId != null && convoId > 0) convoId else null,
                        onNavigateToSettings = {
                            navController.navigate(Screen.Settings.route)
                        }
                    )
                }

                composable(Screen.Vault.route) {
                    VaultScreen(
                        viewModel = vaultViewModel,
                        onNavigateToChatWithDoc = { docId ->
                            chatViewModel.setActiveDocument(docId)
                            navController.navigate(Screen.Chat.createRoute())
                        }
                    )
                }

                composable(Screen.Integrations.route) {
                    val documents by app.documentRepository.getAllDocuments().collectAsState(initial = emptyList())
                    val aiModelStatus by app.gemma4ToolCallingModel.statusFlow.collectAsState()
                    IntegrationsScreen(
                        localRecordCount = documents.size,
                        modelStatus = aiModelStatus,
                        onOpenRecords = { navController.navigate(Screen.Vault.route) }
                    )
                }

                composable(Screen.History.route) {
                    HistoryScreen(viewModel = historyViewModel)
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}
